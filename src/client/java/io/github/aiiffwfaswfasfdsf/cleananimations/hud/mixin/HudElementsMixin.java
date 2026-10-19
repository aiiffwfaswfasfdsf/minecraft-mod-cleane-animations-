package io.github.aiiffwfaswfasfdsf.cleananimations.hud.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudRuntime;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.CrosshairAnimator;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.HotbarAnimator;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hotbar (glide + lift + bounce) and crosshair (scale + hit marker) of the in-game HUD. */
@Mixin(Hud.class)
public abstract class HudElementsMixin {
	@Shadow
	protected abstract net.minecraft.world.entity.player.Player getCameraPlayer();

	// ------------------------------------------------------------------ hotbar

	@Inject(method = "extractItemHotbar", at = @At("HEAD"))
	private void cleananimations$hotbarFrame(GuiGraphicsExtractor g, DeltaTracker dt, CallbackInfo ci) {
		if (HudConfig.get().hotbarOn() && getCameraPlayer() != null) {
			HudRuntime.hotbarFrame(getCameraPlayer().getInventory().getSelectedSlot());
		}
	}

	/** The selection frame: x offset is the (fractional) glide offset relative to the vanilla slot position. */
	@WrapOperation(method = "extractItemHotbar",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
					ordinal = 1))
	private void cleananimations$selection(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier sprite,
			int x, int y, int w, int h, Operation<Void> original) {
		HudConfig cfg = HudConfig.get();
		if (!cfg.hotbarOn()) {
			original.call(g, pipeline, sprite, x, y, w, h);
			return;
		}
		float dx = HudRuntime.HOTBAR.selectionOffsetPixels();
		g.pose().pushMatrix();
		g.pose().translate(dx, 0f);
		original.call(g, pipeline, sprite, x, y, w, h);
		g.pose().popMatrix();
	}

	/** Each item slot: lift for the selected slot and bounce. */
	@WrapOperation(method = "extractItemHotbar",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/Hud;extractSlot(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IILnet/minecraft/client/DeltaTracker;Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/item/ItemStack;I)V"))
	private void cleananimations$slot(Hud self, GuiGraphicsExtractor g, int x, int y, DeltaTracker dt,
			net.minecraft.world.entity.player.Player player, ItemStack stack, int seed, Operation<Void> original) {
		HudConfig cfg = HudConfig.get();
		int rel = x - (g.guiWidth() / 2 - 88);
		int slot = Math.floorDiv(rel, 20);
		if (!cfg.hotbarOn() || Math.floorMod(rel, 20) != 0 || slot < 0 || slot >= HotbarAnimator.SLOTS) {
			original.call(self, g, x, y, dt, player, stack, seed);
			return;
		}
		float offset = HudRuntime.HOTBAR.slotOffsetY(slot, cfg.hotbarIntensityEff());
		g.pose().pushMatrix();
		g.pose().translate(0f, offset);
		original.call(self, g, x, y, dt, player, stack, seed);
		g.pose().popMatrix();
	}

	// ------------------------------------------------------------------ crosshair

	/** The crosshair sprite (first blitSprite with 6 args in extractCrosshair): scale around the centre, then the hit marker. */
	@WrapOperation(method = "extractCrosshair",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/GuiGraphicsExtractor;blitSprite(Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;Lnet/minecraft/resources/Identifier;IIII)V",
					ordinal = 0))
	private void cleananimations$crosshair(GuiGraphicsExtractor g, RenderPipeline pipeline, Identifier sprite,
			int x, int y, int w, int h, Operation<Void> original) {
		HudConfig cfg = HudConfig.get();
		if (!cfg.crosshairOn()) {
			original.call(g, pipeline, sprite, x, y, w, h);
			return;
		}
		HudRuntime.crosshairFrame();
		CrosshairAnimator c = HudRuntime.CROSSHAIR;
		float cx = x + w / 2f;
		float cy = y + h / 2f;
		float s = c.scale();
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(s, s);
		g.pose().translate(-cx, -cy);
		original.call(g, pipeline, sprite, x, y, w, h);
		g.pose().popMatrix();

		float alpha = cfg.crosshairHitMarker ? c.hitMarkerAlpha() : 0f;
		if (alpha > 0.01f) {
			drawHitMarker(g, Math.round(cx), Math.round(cy), c.hitMarkerProgress(), alpha);
		}
	}

	@org.spongepowered.asm.mixin.Unique
	private static void drawHitMarker(GuiGraphicsExtractor g, int cx, int cy, float progress, float alpha) {
		int a = Math.min(255, Math.round(alpha * 230f));
		int color = (a << 24) | 0xFFFFFF;
		int gap = 4 + Math.round(progress * 3f);
		for (int i = 0; i < 3; i++) {
			int d = gap + i;
			g.fill(cx - d - 1, cy - d - 1, cx - d, cy - d, color);
			g.fill(cx + d, cy - d - 1, cx + d + 1, cy - d, color);
			g.fill(cx - d - 1, cy + d, cx - d, cy + d + 1, color);
			g.fill(cx + d, cy + d, cx + d + 1, cy + d + 1, color);
		}
	}
}
