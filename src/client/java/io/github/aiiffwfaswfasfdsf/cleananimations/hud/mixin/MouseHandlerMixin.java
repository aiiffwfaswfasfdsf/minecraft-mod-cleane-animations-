package io.github.aiiffwfaswfasfdsf.cleananimations.hud.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Zoom: scroll wheel adjusts the level (only while zooming) and look speed follows the zoom. */
@Mixin(MouseHandler.class)
public abstract class MouseHandlerMixin {
	@Shadow
	@org.spongepowered.asm.mixin.Final
	private Minecraft minecraft;

	@Unique
	private final double[] cleananimations$smoothed = new double[2];

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void cleananimations$scroll(long window, double xOffset, double yOffset, CallbackInfo ci) {
		HudConfig cfg = HudConfig.get();
		if (window == minecraft.getWindow().handle() && cfg.zoomOn() && cfg.zoomScroll
				&& HudRuntime.ZOOM.isHeld() && minecraft.gui.screen() == null && minecraft.gui.overlay() == null) {
			HudRuntime.ZOOM.scroll(yOffset, cfg.zoomMaxLevel);
			ci.cancel(); // swallow the wheel only while zooming
		}
	}

	@WrapOperation(method = "turnPlayer",
			at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;turn(DD)V"))
	private void cleananimations$turn(LocalPlayer player, double dx, double dy, Operation<Void> original) {
		HudConfig cfg = HudConfig.get();
		if (cfg.zoomOn() && HudRuntime.ZOOM.isActive()) {
			float m = HudRuntime.turnMultiplier(cfg);
			dx *= m;
			dy *= m;
			if (cfg.zoomCinematic) {
				HudRuntime.ZOOM.smoothTurn(dx, dy, 0.55f, cleananimations$smoothed);
				dx = cleananimations$smoothed[0];
				dy = cleananimations$smoothed[1];
			}
		} else {
			HudRuntime.ZOOM.resetSmoothing();
		}
		original.call(player, dx, dy);
	}
}
