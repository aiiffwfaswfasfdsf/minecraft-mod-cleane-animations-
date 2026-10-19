package io.github.aiiffwfaswfasfdsf.cleananimations.hud.mixin;

import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudRuntime;
import net.minecraft.client.Camera;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Zoom FOV, sprint FOV ease, strafe roll and landing dip. */
@Mixin(Camera.class)
public abstract class CameraMixin {
	@Shadow
	private Entity entity;
	@Shadow
	@org.spongepowered.asm.mixin.Final
	private Minecraft minecraft;

	@Inject(method = "update", at = @At("HEAD"))
	private void cleananimations$frame(DeltaTracker deltaTracker, CallbackInfo ci) {
		HudRuntime.frame();
	}

	@Inject(method = "calculateFov", at = @At("RETURN"), cancellable = true)
	private void cleananimations$fov(float partialTick, CallbackInfoReturnable<Float> cir) {
		HudConfig cfg = HudConfig.get();
		float mul = 1f;
		if (cfg.zoomOn() && HudRuntime.ZOOM.isActive()) {
			mul *= HudRuntime.ZOOM.fovMultiplier();
		}
		if (cfg.cameraOn() && cfg.cameraSprintFov) {
			double effectScale = minecraft.options.fovEffectScale().get();
			mul *= 1f + (HudRuntime.CAMERA.fovMultiplier() - 1f) * (float) effectScale;
		}
		if (mul != 1f) {
			cir.setReturnValue(cir.getReturnValueF() * mul);
		}
	}

	@Unique
	private boolean cleananimations$isLocalFirstPerson() {
		return entity != null && minecraft.player == entity && minecraft.options.getCameraType().isFirstPerson();
	}

	/** Landing dip: extra pitch (positive = down). Args of setRotation: this, yRot (1), xRot (2). */
	@ModifyVariable(method = "setRotation", at = @At("HEAD"), argsOnly = true, index = 2)
	private float cleananimations$pitch(float xRot) {
		HudConfig cfg = HudConfig.get();
		if (cfg.cameraOn() && cfg.cameraLandingDip && cleananimations$isLocalFirstPerson()) {
			return xRot + HudRuntime.CAMERA.dipDegrees();
		}
		return xRot;
	}

	/** Strafe roll: the third (roll) argument of {@code Quaternionf.rotationYXZ}, in radians. */
	@ModifyArg(method = "setRotation",
			at = @At(value = "INVOKE", target = "Lorg/joml/Quaternionf;rotationYXZ(FFF)Lorg/joml/Quaternionf;"),
			index = 2)
	private float cleananimations$roll(float roll) {
		HudConfig cfg = HudConfig.get();
		if (cfg.cameraOn() && cfg.cameraStrafeRoll && cleananimations$isLocalFirstPerson()) {
			return roll - (float) Math.toRadians(HudRuntime.CAMERA.rollDegrees());
		}
		return roll;
	}
}
