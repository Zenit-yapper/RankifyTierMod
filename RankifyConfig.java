package com.rankify.client.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class RankifyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("rankify-tier-mod.json");
    
    // Configurable values
    public String apiBaseUrl = "https://rankify-tier-flow.base44.app/api";
    public boolean showOwnTier = false;
    public float renderDistance = 64.0f;
    public int cacheMinutes = 10;
    public boolean showInTabList = true;
    public String displayFormat = "[%TIER%]"; // %TIER% gets replaced
    
    public static RankifyConfig load() {
        if (Files.exists(CONFIG_PATH)) {
            try {
                String json = Files.readString(CONFIG_PATH);
                return GSON.fromJson(json, RankifyConfig.class);
            } catch (IOException e) {
                System.err.println("Failed to load Rankify config: " + e.getMessage());
            }
        }
        return new RankifyConfig();
    }
    
    public void save() {
        try {
            Files.writeString(CONFIG_PATH, GSON.toJson(this));
        } catch (IOException e) {
            System.err.println("Failed to save Rankify config: " + e.getMessage());
        }
    }
}
