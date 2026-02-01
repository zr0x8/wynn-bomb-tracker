package com.example.wynnbombtracker;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ModConfig {
    private static final File CONFIG_FILE = FabricLoader.getInstance().getConfigDir().resolve("wynn-bomb-tracker.json")
            .toFile();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public boolean webhook_enabled = false;
    public String webhook_url = "";

    public boolean auto_reply_enabled = false;
    public boolean auto_relay_enabled = true;
    public boolean debug_chat = false;
    public Map<String, List<String>> bomb_aliases = new HashMap<>();

    private static ModConfig instance;

    public static ModConfig get() {
        if (instance == null) {
            instance = load();
        }
        return instance;
    }

    public ModConfig() {
        bomb_aliases.put("Combat XP", new java.util.ArrayList<>());
        bomb_aliases.put("Profession Speed", new java.util.ArrayList<>());
        bomb_aliases.put("Profession XP", new java.util.ArrayList<>());
        bomb_aliases.put("Dungeon", new java.util.ArrayList<>());
        bomb_aliases.put("Loot", new java.util.ArrayList<>());
        bomb_aliases.put("Loot Chest", new java.util.ArrayList<>());
    }

    public static ModConfig load() {
        if (CONFIG_FILE.exists()) {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                ModConfig config = GSON.fromJson(reader, ModConfig.class);
                if (config.bomb_aliases == null) {
                    config.bomb_aliases = new HashMap<>();
                }
                String[] keys = { "Combat XP", "Profession Speed", "Profession XP", "Dungeon", "Loot", "Loot Chest" };
                for (String key : keys) {
                    config.bomb_aliases.putIfAbsent(key, new java.util.ArrayList<>());
                }
                return config;
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return new ModConfig();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
