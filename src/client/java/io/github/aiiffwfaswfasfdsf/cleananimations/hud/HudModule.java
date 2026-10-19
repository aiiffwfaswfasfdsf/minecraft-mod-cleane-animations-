package io.github.aiiffwfaswfasfdsf.cleananimations.hud;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.ConfigKeybind;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;

/** Entry point of the hud feature module: smooth hotbar, smooth zoom, crosshair feedback, camera feel, screen open. */
public final class HudModule {
	/** Hold to zoom (default C), registered in the Clean Animations key category. */
	public static final KeyMapping ZOOM_KEY = new KeyMapping(
			"key.cleananimations.zoom", InputConstants.KEY_C, ConfigKeybind.CATEGORY);

	private HudModule() {
	}

	public static void init() {
		KeyMappingHelper.registerKeyMapping(ZOOM_KEY);
		ClientTickEvents.END_CLIENT_TICK.register(HudRuntime::tick);
		AttackEntityCallback.EVENT.register((player, level, hand, entity, hit) -> {
			HudConfig cfg = HudConfig.get();
			if (level.isClientSide() && cfg.crosshairOn() && cfg.crosshairHitMarker && entity instanceof LivingEntity) {
				HudRuntime.CROSSHAIR.hit();
			}
			return InteractionResult.PASS;
		});
	}
}
