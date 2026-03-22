package com.rankify.client;

import com.rankify.client.tier.TierApiClient;
import com.rankify.client.tier.TierCache;
import com.rankify.client.render.TierDisplayRenderer;
import com.rankify.network.RankifyPackets;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RankifyClientEntrypoint implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("rankify-client");
    private static TierCache tierCache;
    private static TierApiClient apiClient;
    
    @Override
    public void onInitializeClient() {
        LOGGER.info("Initializing Rankify Client for CRACKED servers (1.21.10)");
        
        tierCache = new TierCache();
        apiClient = new TierApiClient();
        
        registerPacketHandlers();
        
        // Render tiers above player heads
        WorldRenderEvents.AFTER_ENTITIES.register(context -> {
            TierDisplayRenderer.render(context.matrixStack(), context.consumers(), context.camera());
        });
        
        // Periodic cleanup and refresh
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.world != null && client.world.getTime() % 1200 == 0) {
                tierCache.cleanup();
                
                // Refresh all visible players (for cracked servers, names can change)
                if (client.world != null) {
                    for (var player : client.world.getPlayers()) {
                        if (player != client.player) {
                            String name = player.getName().getString();
                            apiClient.fetchTierByUsername(name, data -> {
                                if (data != null) tierCache.put(name.toLowerCase(), data);
                            });
                        }
                    }
                }
            }
        });
    }
    
    private void registerPacketHandlers() {
        // Receive request to fetch tier for a player (by USERNAME for cracked)
        ClientPlayNetworking.registerGlobalReceiver(RankifyPackets.REQUEST_TIER, (client, handler, buf, responseSender) -> {
            String username = buf.readString(); // Username is the key!
            String displayName = buf.readString();
            
            client.execute(() -> {
                LOGGER.debug("Fetching tier for cracked player: {}", username);
                
                apiClient.fetchTierByUsername(username, tierData -> {
                    if (tierData != null) {
                        // Store by lowercase username for consistency
                        tierCache.put(username.toLowerCase(), tierData);
                        
                        // Broadcast back to server
                        var responseBuf = net.fabricmc.fabric.api.networking.v1.PacketByteBufs.create();
                        responseBuf.writeString(username);
                        responseBuf.writeString(tierData.getTierName());
                        responseBuf.writeInt(tierData.getColor());
                        responseSender.sendPacket(RankifyPackets.SYNC_TIER, responseBuf);
                    }
                });
            });
        });
        
        // Receive tier data from other clients via server
        ClientPlayNetworking.registerGlobalReceiver(RankifyPackets.SYNC_TIER, (client, handler, buf, responseSender) -> {
            String username = buf.readString(); // Username, not UUID!
            String tierName = buf.readString();
            int color = buf.readInt();
            
            client.execute(() -> {
                tierCache.put(username.toLowerCase(), new com.rankify.client.tier.TierData(tierName, color));
                LOGGER.debug("Received tier for {}: {}", username, tierName);
            });
        });
        
        // Handle name changes (cracked servers)
        ClientPlayNetworking.registerGlobalReceiver(RankifyPackets.PLAYER_JOINED, (client, handler, buf, responseSender) -> {
            String oldName = buf.readString();
            String newName = buf.readString();
            
            client.execute(() -> {
                // Remove old cache entry, fetch new one
                tierCache.remove(oldName.toLowerCase());
                apiClient.fetchTierByUsername(newName, data -> {
                    if (data != null) tierCache.put(newName.toLowerCase(), data);
                });
            });
        });
    }
    
    public static TierCache getTierCache() {
        return tierCache;
    }
}
