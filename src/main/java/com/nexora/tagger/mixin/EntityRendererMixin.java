package com.nexora.tagger.mixin;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.state.EntityRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EntityRenderer.class)
public abstract class EntityRendererMixin<S extends EntityRenderState> {
    @Inject(method = "render", at = @At("HEAD"))
    private void onRender(S state, org.minecraft.client.util.math.MatrixStack matrices, net.minecraft.client.render.VertexConsumerProvider v, int l, CallbackInfo ci) {
        // Ready for 1.21.10
    }
}
