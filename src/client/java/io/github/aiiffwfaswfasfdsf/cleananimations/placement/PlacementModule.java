package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderEvents;
import net.fabricmc.fabric.api.event.client.player.ClientPlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.resources.Identifier;

/**
 * Entry point of the placement feature module: block placement animation, break animation, precise placement,
 * bridging assist and the eased block outline.
 */
public final class PlacementModule {
	private PlacementModule() {
	}

	public static void init() {
		UseBlockCallback.EVENT.register(PlacementAnimations::onUseBlock);
		ClientPlayerBlockBreakEvents.AFTER.register(BreakAnimations::onBreak);

		ClientTickEvents.START_CLIENT_TICK.register(mc -> {
			PrecisePlacement.tick(mc);
			BridgingAssist.tick(mc);
		});
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			PlacementAnimations.tick(mc);
			BreakAnimations.tick(mc);
		});
		ClientPlayConnectionEvents.DISCONNECT.register((handler, mc) -> {
			PlacementAnimations.reset();
			BreakAnimations.reset();
			PrecisePlacement.reset();
		});

		LevelRenderEvents.COLLECT_SUBMITS.register(ctx -> {
			PlacementAnimations.render(ctx);
			BreakAnimations.render(ctx);
		});
		LevelRenderEvents.BEFORE_BLOCK_OUTLINE.register(EasedOutline::beforeBlockOutline);

		HudElementRegistry.attachElementAfter(VanillaHudElements.CROSSHAIR,
				Identifier.fromNamespaceAndPath("cleananimations", "bridging_indicator"), BridgingAssist.INDICATOR);
	}
}
