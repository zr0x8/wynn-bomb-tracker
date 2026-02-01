package com.example.wynnbombtracker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import java.util.Optional;

public class BombTrackerClient implements ClientModInitializer {

    private static final String WORKER_URL = "https://your-worker-name.your-subdomain.workers.dev"; // TODO: replace
                                                                                                    // with ur
                                                                                                    // worker URL

    private final HttpClient http_client = HttpClient.newHttpClient();
    private static BombTrackerClient INSTANCE;

    private static final String GUILD_CHAT_UNICODE_1 = "\uDAFF\uDFFC\uE006\uDAFF\uDFFF\uE002\uDAFF\uDFFE";
    private static final String GUILD_CHAT_UNICODE_2 = "\uDAFF\uDFFC\uE001\uDB00\uDC06";

    @Override
    public void onInitializeClient() {
        INSTANCE = this;
        System.out.println("BombTrackerClient initialized!");

        BombTrackerCommand.register();

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (overlay)
                return;

            String plain_message = message.getString();

            System.out.println("BombTracker Debug - Chat: " + plain_message);
            System.out.println("BombTracker Debug - Unicode: " + escapeUnicode(plain_message));
            System.out.println("BombTracker Debug - Is Guild Chat: " + isGuildChat(message));
            System.out.println("BombTracker Debug - AutoReply Enabled: " + ModConfig.get().auto_reply_enabled);

            if (ModConfig.get().debug_chat) {
                System.out.println("BombTracker Debug - Chat: " + plain_message);
                System.out.println("BombTracker Debug - Unicode: " + escapeUnicode(plain_message));
            }

            if (isGuildChat(message)) {

                if (MinecraftClient.getInstance().player != null) {
                    String local_player_name = MinecraftClient.getInstance().player.getName().getString();

                    java.util.regex.Matcher SENDER_MATCHER = Pattern.compile("(?:(?:\\[[^\\]]+\\])\\s*)?([^:]+):")
                            .matcher(plain_message);
                    if (SENDER_MATCHER.find()) {
                        String sender = SENDER_MATCHER.group(1);
                        if (sender.contains(local_player_name)) {
                            return;
                        }
                    }

                    if (plain_message.contains("You have thrown a") || plain_message.contains("You have purchased a")) {
                        return;
                    }

                    java.util.regex.Matcher THROWN_MATCHER = Pattern.compile("([\\w]+) has thrown a")
                            .matcher(plain_message);
                    if (THROWN_MATCHER.find()) {
                        String thrower = THROWN_MATCHER.group(1);
                        if (thrower.equals(local_player_name)) {
                            return;
                        }
                    }
                }

                if (ModConfig.get().auto_reply_enabled) {
                    ModConfig.get().bomb_aliases.forEach((BOMB_TYPE, aliases) -> {
                        for (String alias : aliases) {
                            if (Pattern.compile("(?i)\\b" + Pattern.quote(alias) + "\\b").matcher(plain_message)
                                    .find()) {
                                replyToQuery(BOMB_TYPE);
                                return;
                            }
                        }
                    });
                }
            }
        });
    }

    private String escapeUnicode(String input) {
        StringBuilder b = new StringBuilder();
        for (char c : input.toCharArray()) {
            if (c >= 128)
                b.append(String.format("\\u%04x", (int) c));
            else
                b.append(c);
        }
        return b.toString();
    }

    public static void onBombDetected(String user, String bomb, String server, Object bombInfo) {
        System.out.println("BombTracker: onBombDetected called for " + bomb + " on " + server);
        if (INSTANCE != null) {
            String final_thrower = user;
            if (final_thrower == null || final_thrower.isEmpty()) {
                System.out.println("BombTracker: User is null/empty. Attempting to extract from BombInfo...");
                if (bombInfo != null) {
                    try {
                        System.out.println("BombTracker: BombInfo class: " + bombInfo.getClass().getName());
                        java.lang.reflect.Method getThrowerMethod = bombInfo.getClass().getMethod("getThrower");
                        final_thrower = (String) getThrowerMethod.invoke(bombInfo);
                        System.out.println("BombTracker: Extracted thrower: " + final_thrower);
                    } catch (Exception e) {
                        System.out.println("BombTracker: Failed to get thrower from BombInfo: " + e);
                        e.printStackTrace();
                    }
                } else {
                    System.out.println("BombTracker: BombInfo is null.");
                }

                if (final_thrower == null || final_thrower.isEmpty()) {
                    final_thrower = "Unknown";
                }
            }

            System.out.println("Bomb Detected (via Mixin): " + bomb + " by " + final_thrower + " on " + server);
            if (ModConfig.get().auto_relay_enabled) {
                INSTANCE.relayToGuildChat(final_thrower, bomb, server);
            }
            if (ModConfig.get().webhook_enabled) {
                INSTANCE.sendBomb(final_thrower, bomb, server);
            }
        } else {
            System.out.println("BombTracker: INSTANCE is null!");
        }
    }

    public static void onBombDetected(String user, String bomb, String server) {
        onBombDetected(user, bomb, server, null);
    }

    private final java.util.Map<String, Long> last_reply_time = new java.util.HashMap<>();
    private static final long REPLY_COOLDOWN = 30000; // 30s
    private final java.util.Map<String, Long> last_webhook_times = new java.util.HashMap<>();
    private static final long WEBHOOK_COOLDOWN = 300000; // 5m

    private void replyToQuery(String BOMB_TYPE) {
        long now = System.currentTimeMillis();
        if (now - last_reply_time.getOrDefault(BOMB_TYPE, 0L) < REPLY_COOLDOWN) {
            return;
        }
        last_reply_time.put(BOMB_TYPE, now);

        System.out.println("BombTracker: Querying for " + BOMB_TYPE);

        try {
            java.util.Collection<Object> raw_bomb = getBombsFromModel();
            System.out.println("BombTracker: Found " + raw_bomb.size() + " raw bombs from model.");

            java.util.Map<String, java.util.List<String>> results = new java.util.TreeMap<>();

            for (Object b : raw_bomb) {
                try {
                    String rawName = getMainBombName(b);
                    if (!rawName.equalsIgnoreCase(BOMB_TYPE)
                            && !rawName.toLowerCase().contains(BOMB_TYPE.toLowerCase())) {
                        continue;
                    }

                    String sanitizedName = sanitizeBombName(rawName);
                    String server = getBombServer(b);
                    long remaining = getBombExpiry(b) - System.currentTimeMillis();

                    if (remaining > 0) {
                        String info = server + " (" + formatTime(remaining) + ")";
                        results.computeIfAbsent(sanitizedName, k -> new java.util.ArrayList<>()).add(info);
                    }
                } catch (Exception e) {
                    System.out.println("BombTracker: Error processing bomb for reply: " + e);
                }
            }

            String reply;
            if (!results.isEmpty()) {
                String combined = results.entrySet().stream()
                        .map(e -> e.getKey() + ": " + String.join(", ", e.getValue()))
                        .collect(Collectors.joining(" | "));
                reply = "/g " + combined;
            } else {
                reply = String.format("/g %s: No active bombs found.", BOMB_TYPE);
            }

            System.out.println("BombTracker: Reply prepared: " + reply);

            String finalReply = reply;

            queueChatCommand(finalReply.substring(1));

        } catch (Throwable t) {
            System.err.println("BombTracker: Error in replyToQuery");
            t.printStackTrace();
        }
    }

    private static CompletableFuture<Void> chatQueue = CompletableFuture.completedFuture(null);

    private void queueChatCommand(String command) {
        chatQueue = chatQueue.thenCompose(v -> CompletableFuture.runAsync(() -> {
            try {
                Thread.sleep(500);
                MinecraftClient.getInstance().execute(() -> {
                    if (MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.networkHandler.sendChatCommand(command);
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }));
    }

    private String getMainBombName(Object bombInfo) throws Exception {
        try {
            java.lang.reflect.Method bombMethod = bombInfo.getClass().getMethod("bomb");
            Object BOMB_TYPE = bombMethod.invoke(bombInfo);
            if (BOMB_TYPE != null) {
                java.lang.reflect.Method displayNameMethod = BOMB_TYPE.getClass().getMethod("getDisplayName");
                return (String) displayNameMethod.invoke(BOMB_TYPE);
            }
        } catch (Exception e) {
            System.out.println("BombTracker: getMainBombName failed: " + e);
        }
        return "??Bomb";
    }

    private String getBombServer(Object bombInfo) throws Exception {
        try {
            return (String) bombInfo.getClass().getMethod("server").invoke(bombInfo);
        } catch (Exception e) {
            System.out.println("BombTracker: getBombServer failed: " + e);
        }
        return "WC??";
    }

    private long getBombExpiry(Object bombInfo) throws Exception {
        try {
            return (long) bombInfo.getClass().getMethod("endTime").invoke(bombInfo);
        } catch (Exception e) {
            System.out.println("BombTracker: getBombExpiry failed: " + e);
        }
        return 0;
    }

    private String formatTime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        seconds = seconds % 60;
        return String.format("%02dm %02ds", minutes, seconds);
    }

    private String sanitizeBombName(String name) {
        if (name == null)
            return "";
        String s = name.replace("Experience", "XP");
        s = s.replaceAll("(?i)\\s+Bomb$", "");
        return s.trim();
    }

    @SuppressWarnings("unchecked")
    private java.util.Collection<Object> getBombsFromModel() {
        try {
            Class<?> BombModel_class = Class.forName("com.wynntils.models.worlds.BombModel");
            java.lang.reflect.Field BOMBS_field = BombModel_class.getDeclaredField("BOMBS");
            BOMBS_field.setAccessible(true);
            Object activeBomb_container = BOMBS_field.get(null);

            if (activeBomb_container != null) {
                java.lang.reflect.Method asSet_method = activeBomb_container.getClass().getDeclaredMethod("asSet");
                asSet_method.setAccessible(true);
                return (java.util.Collection<Object>) asSet_method.invoke(activeBomb_container);
            }
        } catch (Exception e) {
            System.out.println("BombTracker: getBombsFromModel Error: " + e);
            e.printStackTrace();
        }
        return java.util.Collections.emptyList();
    }

    private long getBombDuration(String bomb) {
        if (bomb.equalsIgnoreCase("Profession Speed Bomb") || bomb.equalsIgnoreCase("Dungeon Bomb") ||
                bomb.contains("Profession Speed") || bomb.contains("Dungeon")) {
            return 10 * 60 * 1000;
        }
        return 20 * 60 * 1000;
    }

    private void sendBomb(String user, String bomb, String server) {
        String webhook_url = ModConfig.get().webhook_url;
        if (webhook_url == null || webhook_url.isEmpty()) {
            return;
        }

        String dedupe_key = server + ":" + sanitizeBombName(bomb);
        long now = System.currentTimeMillis();
        if (now - last_webhook_times.getOrDefault(dedupe_key, 0L) < WEBHOOK_COOLDOWN) {
            System.out.println("BombTracker: Skipping webhook for " + dedupe_key + " due to cooldown.");
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                long duration = getBombDuration(bomb);
                long expiry_time = System.currentTimeMillis() + duration;

                String json_body = String.format(
                        "{\"user\": \"%s\", \"bomb\": \"%s\", \"server\": \"%s\", \"timestamp\": %d, \"expiry\": %d, \"webhook_url\": \"%s\"}",
                        user, bomb, server, System.currentTimeMillis(), expiry_time, webhook_url);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(WORKER_URL))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json_body))
                        .build();

                http_client.send(request, HttpResponse.BodyHandlers.ofString());

                last_webhook_times.put(dedupe_key, now);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private void relayToGuildChat(String user, String bomb, String server) {
        MinecraftClient.getInstance().execute(() -> {
            if (MinecraftClient.getInstance().player != null) {
                long duration = getBombDuration(bomb);

                long reduction = 2000 + (long) (Math.random() * 8001);
                duration -= reduction;

                String time_string = formatTime(duration);
                String clean_bomb = sanitizeBombName(bomb);
                String guild_message = String.format("/g %s bomb on %s with %s remaining", clean_bomb, server,
                        time_string);
                MinecraftClient.getInstance().player.networkHandler.sendChatCommand(guild_message.substring(1));
            }
        });
    }

    private boolean isGuildChat(Text message) {
        String content = message.getString();
        if (!content.contains(GUILD_CHAT_UNICODE_1) && !content.contains(GUILD_CHAT_UNICODE_2)) {
            return false;
        }

        return message.visit((style, as_string) -> {
            if (as_string.contains(GUILD_CHAT_UNICODE_1) || as_string.contains(GUILD_CHAT_UNICODE_2)) {
                if (style != null && style.getColor() != null) {
                    Formatting formatting = Formatting.byName(style.getColor().getName());
                    if (formatting != null) {
                        if (Formatting.YELLOW.equals(formatting) || Formatting.GOLD.equals(formatting)) {
                            return Optional.of(false);
                        }
                    }

                    int rgb = style.getColor().getRgb();
                    if (rgb == 0xFFFF55 || rgb == 0xFFAA00) {
                        return Optional.of(false);
                    }
                }
                return Optional.of(true);
            }
            return Optional.empty();
        }, Style.EMPTY).orElse(false);
    }
}
