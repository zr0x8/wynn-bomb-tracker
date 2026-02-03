package com.example.wynnbombtracker;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.text.Text;

import java.util.ArrayList;

public class ModMenuIntegration implements ModMenuApi {

        @Override
        public ConfigScreenFactory<?> getModConfigScreenFactory() {
                return parent -> {
                        ConfigBuilder builder = ConfigBuilder.create()
                                .setParentScreen(parent)
                                .setTitle(Text.of("Wynn Bomb Tracker Config"));

                        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

                        ConfigCategory general = builder.getOrCreateCategory(Text.of("General"));

                        general.addEntry(
                                entryBuilder.startBooleanToggle(Text.of("Webhook Relay Enabled"),
                                                ModConfig.get().webhook_enabled)
                                        .setDefaultValue(false)
                                        .setTooltip(Text.of(
                                                "Enable relaying bomb reports to Discord Webhook."))
                                        .setSaveConsumer(newValue -> ModConfig
                                                .get().webhook_enabled = newValue)
                                        .build());

                        general.addEntry(entryBuilder.startStrField(Text.of("Webhook URL"), ModConfig.get().webhook_url)
                                .setDefaultValue("")
                                .setTooltip(Text.of("The Discord Webhook URL. Only used if Relay is enabled."))
                                .setSaveConsumer(newValue -> ModConfig.get().webhook_url = newValue)
                                .build());

                        general.addEntry(
                                entryBuilder.startBooleanToggle(Text.of("Auto-Reply Enabled"),
                                                ModConfig.get().auto_reply_enabled)
                                        .setDefaultValue(false)
                                        .setTooltip(Text.of(
                                                "Enable auto-replying to guild chat queries matching aliases."))
                                        .setSaveConsumer(newValue -> ModConfig
                                                .get().auto_reply_enabled = newValue)
                                        .build());

                        general.addEntry(
                                entryBuilder.startBooleanToggle(Text.of("Guild Relay Enabled"),
                                                ModConfig.get().auto_relay_enabled)
                                        .setDefaultValue(true)
                                        .setTooltip(Text.of(
                                                "Enable relaying bomb detections to guild chat."))
                                        .setSaveConsumer(newValue -> ModConfig
                                                .get().auto_relay_enabled = newValue)
                                        .build());

                        ModConfig.get().bomb_aliases.forEach((bombType, aliases) -> {
                                general.addEntry(entryBuilder.startStrList(Text.of(bombType + " Aliases"), aliases)
                                        .setDefaultValue(new ArrayList<>())
                                        .setTooltip(Text.of("Aliases to trigger auto-reply for " + bombType))
                                        .setSaveConsumer(newList -> ModConfig.get().bomb_aliases.put(bombType,
                                                newList))
                                        .build());
                        });

                        builder.setSavingRunnable(() -> {
                                ModConfig.get().save();
                        });

                        return builder.build();
                };
        }
}
