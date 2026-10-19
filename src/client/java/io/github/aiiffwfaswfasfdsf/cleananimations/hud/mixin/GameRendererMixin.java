package io.github.aiiffwfaswfasfdsf.cleananimations.hud.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudRuntime;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** View-bob smoothing: low-pass the bob amplitude before vanilla applies it. */
@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
	@Inject(method = "bobView", at = @At("HEAD"))
	private void cleananimations$smoothBob(CameraRenderState camera, PoseStack poseStack, CallbackInfo ci) {
		HudConfig cfg = HudConfig.get();
		if (cfg.cameraOn() && cfg.cameraBobSmoothing && camera.entityRenderState.isPlayer) {
			camera.entityRenderState.bob = HudRuntime.CAMERA.smoothBob(camera.entityRenderState.bob,
					HudRuntime.bobFrameDt(), cfg.cameraSpeedEff(), Math.min(1f, cfg.cameraIntensityEff()));
		}
	}
}
