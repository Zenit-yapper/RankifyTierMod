package com.rankify.client.render;

import com.rankify.client.RankifyClientEntrypoint;
import com.rankify.client.tier.TierData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix4f;

public class TierDisplayRenderer {
    private static final int RENDER_DISTANCE = 64; // Only render within 64 blocks
    
    public static void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, Camera camera) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) return;
        
        // Get client's username for self-check
        String clientUsername = client.player.getName().getString();
        
        for (PlayerEntity player : client.world.getPlayers()) {
            // Skip self in first person
            if (player == client.player && client.options.getPerspective().isFirstPerson()) continue;
            
            // Check distance
            double distance = client.player.squaredDistanceTo(player);
            if (distance > RENDER_DISTANCE * RENDER_DISTANCE) continue;
            
            // LOOKUP BY USERNAME (not UUID) for cracked servers
            String playerName = player.getName().getString();
            TierData tier = RankifyClientEntrypoint.getTierCache().get(playerName);
            
            if (tier == null) {
                // Trigger fetch if not in cache (async, will render next frame)
                continue;
            }
            
            renderTierAbovePlayer(matrices, vertexConsumers, camera, player, tier, client.textRenderer);
        }
    }
    
    private static void renderTierAbovePlayer(MatrixStack matrices, VertexConsumerProvider vertexConsumers, 
                                              Camera camera, PlayerEntity player, TierData tier, 
                                              TextRenderer textRenderer) {
        
        matrices.push();
        
        // Calculate position with interpolation for smooth movement
        double x = MathHelper.lerp(1.0f, player.lastRenderX, player.getX()) - camera.getPos().x;
        double y = MathHelper.lerp(1.0f, player.lastRenderY, player.getY()) - camera.getPos().y + player.getHeight() + 0.75; // Slightly higher
        double z = MathHelper.lerp(1.0f, player.lastRenderZ, player.getZ()) - camera.getPos().z;
        
        matrices.translate(x, y, z);
        
        // Billboard effect - always face camera
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        
        // Scale based on distance (smaller when far)
        float distance = (float) Math.sqrt(player.squaredDistanceTo(camera.getPos().x, camera.getPos().y, camera.getPos().z));
        float scale = 0.025f * Math.max(0.5f, Math.min(1.0f, 8.0f / distance));
        matrices.scale(-scale, -scale, scale);
        
        // Prepare text
        String tierText = "[" + tier.getTierName() + "]";
        int color = tier.getColor();
        int textWidth = textRenderer.getWidth(tierText);
        float centerX = -textWidth / 2f;
        
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        
        // Draw background with tier color (subtle tint)
        float alpha = 0.6f;
        int bgColor = (color & 0x00FFFFFF) | ((int)(alpha * 255) << 24);
        
        VertexConsumer bgConsumer = vertexConsumers.getBuffer(RenderLayer.getTextBackgroundSeeThrough());
        float padding = 2;
        float height = textRenderer.fontHeight;
        
        // Background quad
        bgConsumer.vertex(matrix, centerX - padding, -padding, 0.01f).color(bgColor).light(0xF000F0).next();
        bgConsumer.vertex(matrix, centerX - padding, height + padding, 0.01f).color(bgColor).light(0xF000F0).next();
        bgConsumer.vertex(matrix, centerX + textWidth + padding, height + padding, 0.01f).color(bgColor).light(0xF000F0).next();
        bgConsumer.vertex(matrix, centerX + textWidth + padding, -padding, 0.01f).color(bgColor).light(0xF000F0).next();
        
        // Draw text with shadow for readability
        textRenderer.draw(tierText, centerX, 0, color, true, matrix, vertexConsumers, 
                         TextRenderer.TextLayerType.SEE_THROUGH, 0, 0xF000F0);
        
        matrices.pop();
    }
}
