package com.rankify;

import com.rankify.network.RankifyPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class RankifyTierMod implements ModInitializer {
    public static final String MOD_ID = "rankify-tier-mod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    
    // API Configuration - Update with your actual endpoint
    public static final String API_BASE_URL = "https://rankify-tier-flow.base44.app/api";
    
    // Track usernames for cracked servers (since UUIDs are fake/offline)
    public static final Map<String, String> usernameCache = new HashMap<>();
    
    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Rankify Tier Mod for CRACKED/OFFLINE servers (1.21.10)");
        
        // Register packet handlers
        RankifyPackets.registerC2S();
        
        // When player joins, broadcast their username for tier lookup
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            String username = player.getName().getString();
            
            LOGGER.info("[CRACKED SERVER] Player joined: {}", username);
            usernameCache.put(player.getUuidAsString(), username);
            
            // Broadcast to all clients that this player needs tier lookup
            RankifyPackets.syncPlayerTier(player);
        });
        
        // Handle disconnections
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            String uuid = handler.getPlayer().getUuidAsString();
            String username = usernameCache.remove(uuid);
            LOGGER.info("Player left: {}", username);
        });
        
        ServerLifecycleEvents.SERVER_STARTED.register(server -> {
            LOGGER.info("Rankify Tier Mod ready for CRACKED server");
            LOGGER.info("Using username-based identification (offline mode)");
        });
    }
}
