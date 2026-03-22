package com.rankify.client.tier;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.rankify.RankifyTierMod;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class TierApiClient {
    private final HttpClient httpClient;
    private static final String API_URL = RankifyTierMod.API_BASE_URL + "/player/%s/tier";
    
    // Tier colors - customize these!
    private static final Map<String, Integer> TIER_COLORS = new HashMap<>() {{
        put("unranked", 0x808080);
        put("iron", 0xCFD8DC);
        put("bronze", 0xCD7F32);
        put("silver", 0xC0C0C0);
        put("gold", 0xFFD700);
        put("platinum", 0xE5E4E2);
        put("diamond", 0xB9F2FF);
        put("master", 0xFF6B6B);
        put("grandmaster", 0x9B59B6);
        put("challenger", 0x00FF00);
        put("legend", 0xFF8C00);
        put("mythic", 0xFF00FF);
    }};
    
    public TierApiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(5))
                .build();
    }
    
    // For cracked servers: fetch by USERNAME instead of UUID
    public void fetchTierByUsername(String username, Consumer<TierData> callback) {
        // Sanitize username for URL
        String safeUsername = username.replaceAll("[^a-zA-Z0-9_]", "");
        String url = String.format(API_URL, safeUsername);
        
        LOGGER.debug("Fetching tier for username: {} from {}", username, url);
        
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("User-Agent", "RankifyTierMod/1.0.0 (Cracked-Server)")
                .timeout(java.time.Duration.ofSeconds(5))
                .GET()
                .build();
        
        CompletableFuture.runAsync(() -> {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                
                if (response.statusCode() == 200) {
                    JsonObject json = JsonParser.parseString(response.body()).getAsJsonObject();
                    parseAndCallback(json, callback);
                } else if (response.statusCode() == 404) {
                    // Player not found in system - unranked
                    callback.accept(new TierData("UNRANKED", TIER_COLORS.get("unranked")));
                } else {
                    LOGGER.error("API error {} for user: {}", response.statusCode(), username);
                    callback.accept(new TierData("ERROR", 0xFF0000));
                }
            } catch (Exception e) {
                LOGGER.error("Failed to fetch tier for {}: {}", username, e.getMessage());
                // Don't cache errors - allow retry
                callback.accept(null);
            }
        });
    }
    
    private void parseAndCallback(JsonObject json, Consumer<TierData> callback) {
        try {
            String tier = "unranked";
            if (json.has("tier")) {
                tier = json.get("tier").getAsString();
            } else if (json.has("rank")) {
                tier = json.get("rank").getAsString();
            }
            
            // Support both "color" field or derive from tier name
            int color;
            if (json.has("color")) {
                String colorStr = json.get("color").getAsString();
                color = parseColor(colorStr);
            } else {
                color = TIER_COLORS.getOrDefault(tier.toLowerCase(), 0xFFFFFF);
            }
            
            String displayName = tier.toUpperCase();
            if (json.has("displayName")) {
                displayName = json.get("displayName").getAsString();
            }
            
            callback.accept(new TierData(displayName, color));
        } catch (Exception e) {
            LOGGER.error("Error parsing tier data: {}", e.getMessage());
            callback.accept(new TierData("UNRANKED", TIER_COLORS.get("unranked")));
        }
    }
    
    private int parseColor(String colorStr) {
        try {
            if (colorStr.startsWith("#")) {
                return Integer.parseInt(colorStr.substring(1), 16);
            }
            return Integer.parseInt(colorStr);
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
    
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(TierApiClient.class);
}
