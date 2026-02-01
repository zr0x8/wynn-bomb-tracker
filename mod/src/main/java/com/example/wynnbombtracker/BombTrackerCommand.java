package com.example.wynnbombtracker;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.minecraft.text.Text;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

import net.minecraft.command.CommandSource;

public class BombTrackerCommand {

        public static void register() {
                ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
                        dispatcher.register(literal("wbt")
                                        .then(literal("webhook")
                                                        .then(argument("url", StringArgumentType.greedyString())
                                                                        .executes(context -> {
                                                                                String url = StringArgumentType
                                                                                                .getString(context,
                                                                                                                "url");
                                                                                if (!url.startsWith(
                                                                                                "https://discord.com/api/webhooks/")) {
                                                                                        context.getSource()
                                                                                                        .sendFeedback(Text
                                                                                                                        .of("§cinvalid webhook url! make sure you are using a discord webhook url."));
                                                                                        return 0;
                                                                                }
                                                                                ModConfig.get().webhook_url = url;
                                                                                ModConfig.get().webhook_enabled = true;

                                                                                ModConfig.get().save();
                                                                                context.getSource().sendFeedback(Text
                                                                                                .of("§awebhook url saved and relay enabled!"));
                                                                                return 1;
                                                                        })))
                                        .then(literal("guildrelay")
                                                        .then(argument("enabled",
                                                                        com.mojang.brigadier.arguments.BoolArgumentType
                                                                                        .bool())
                                                                        .executes(context -> {
                                                                                boolean enabled = com.mojang.brigadier.arguments.BoolArgumentType
                                                                                                .getBool(context,
                                                                                                                "enabled");
                                                                                ModConfig.get().auto_relay_enabled = enabled;
                                                                                ModConfig.get().save();
                                                                                context.getSource().sendFeedback(
                                                                                                Text.of("§aguild relay "
                                                                                                                + (enabled ? "enabled"
                                                                                                                                : "disabled")
                                                                                                                + "!"));
                                                                                return 1;
                                                                        })))
                                        .then(literal("alias")
                                                        .then(literal("set")
                                                                        .then(argument("bombType",
                                                                                        StringArgumentType.string())
                                                                                        .suggests((context,
                                                                                                        builder) -> CommandSource
                                                                                                                        .suggestMatching(
                                                                                                                                        java.util.List.of(
                                                                                                                                                        "Combat_XP",
                                                                                                                                                        "Profession_Speed",
                                                                                                                                                        "Profession_XP",
                                                                                                                                                        "Dungeon",
                                                                                                                                                        "Loot",
                                                                                                                                                        "Loot_Chest"),
                                                                                                                                        builder))
                                                                                        .then(argument("alias",
                                                                                                        StringArgumentType
                                                                                                                        .greedyString())
                                                                                                        .executes(context -> {
                                                                                                                String bombType = StringArgumentType
                                                                                                                                .getString(context,
                                                                                                                                                "bombType")
                                                                                                                                .replace("_",
                                                                                                                                                " ");
                                                                                                                String alias = StringArgumentType
                                                                                                                                .getString(context,
                                                                                                                                                "alias");

                                                                                                                if (!ModConfig.get().bomb_aliases
                                                                                                                                .containsKey(bombType)) {
                                                                                                                        context.getSource()
                                                                                                                                        .sendFeedback(Text
                                                                                                                                                        .of("§cinvalid bomb type! available: Combat_XP, Profession_Speed, Profession_XP, Dungeon, Loot, Loot_Chest"));
                                                                                                                        return 0;
                                                                                                                }

                                                                                                                ModConfig.get().bomb_aliases
                                                                                                                                .get(bombType)
                                                                                                                                .add(alias);
                                                                                                                ModConfig.get().save();
                                                                                                                context.getSource()
                                                                                                                                .sendFeedback(Text
                                                                                                                                                .of("§aalias added: "
                                                                                                                                                                + alias
                                                                                                                                                                + " -> "
                                                                                                                                                                + bombType));
                                                                                                                return 1;
                                                                                                        }))))
                                                        .then(literal("remove")
                                                                        .then(argument("bombType",
                                                                                        StringArgumentType.string())
                                                                                        .suggests((context,
                                                                                                        builder) -> CommandSource
                                                                                                                        .suggestMatching(
                                                                                                                                        java.util.List.of(
                                                                                                                                                        "Combat_XP",
                                                                                                                                                        "Profession_Speed",
                                                                                                                                                        "Profession_XP",
                                                                                                                                                        "Dungeon",
                                                                                                                                                        "Loot",
                                                                                                                                                        "Loot_Chest"),
                                                                                                                                        builder))
                                                                                        .then(argument("alias",
                                                                                                        StringArgumentType
                                                                                                                        .greedyString())
                                                                                                        .executes(context -> {
                                                                                                                String bombType = StringArgumentType
                                                                                                                                .getString(context,
                                                                                                                                                "bombType")
                                                                                                                                .replace("_",
                                                                                                                                                " ");
                                                                                                                String alias = StringArgumentType
                                                                                                                                .getString(context,
                                                                                                                                                "alias");

                                                                                                                if (ModConfig.get().bomb_aliases
                                                                                                                                .containsKey(bombType)) {
                                                                                                                        if (ModConfig.get().bomb_aliases
                                                                                                                                        .get(bombType)
                                                                                                                                        .remove(alias)) {
                                                                                                                                ModConfig.get()
                                                                                                                                                .save();
                                                                                                                                context.getSource()
                                                                                                                                                .sendFeedback(Text
                                                                                                                                                                .of("§aalias removed: "
                                                                                                                                                                                + alias
                                                                                                                                                                                + " from "
                                                                                                                                                                                + bombType));
                                                                                                                                return 1;
                                                                                                                        }
                                                                                                                }
                                                                                                                context.getSource()
                                                                                                                                .sendFeedback(Text
                                                                                                                                                .of("§calias or bomb type not found."));
                                                                                                                return 0;
                                                                                                        }))))
                                                        .then(literal("list")
                                                                        .executes(context -> {
                                                                                context.getSource().sendFeedback(
                                                                                                Text.of("§e--- bomb aliases ---"));
                                                                                ModConfig.get().bomb_aliases
                                                                                                .forEach((type,
                                                                                                                aliases) -> {
                                                                                                        if (!aliases.isEmpty()) {
                                                                                                                context.getSource()
                                                                                                                                .sendFeedback(Text
                                                                                                                                                .of("§b" + type
                                                                                                                                                                + ": §f"
                                                                                                                                                                + String.join(", ",
                                                                                                                                                                                aliases)));
                                                                                                        }
                                                                                                });
                                                                                return 1;
                                                                        }))));
                });
        }
}
