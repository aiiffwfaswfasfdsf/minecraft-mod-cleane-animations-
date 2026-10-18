package io.github.aiiffwfaswfasfdsf.cleananimations.core;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.screen.CleanConfigScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Registers the "open config" key (default K) in the Clean Animations key category. */
public final class ConfigKeybind {
	public static final KeyMapping.Category CATEGORY =
			KeyMapping.Category.register(Identifier.fromNamespaceAndPath(CleanAnimationsClient.MOD_ID, "main"));
	public static final KeyMapping OPEN_CONFIG = new KeyMapping(
			"key.cleananimations.open_config", InputConstants.KEY_K, CATEGORY);

	private ConfigKeybind() {
	}

	public static void init() {
		KeyMappingHelper.registerKeyMapping(OPEN_CONFIG);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (OPEN_CONFIG.consumeClick()) {
				openConfig(mc);
			}
		});
	}

	/** Opens the config screen on top of the current screen (if any). */
	public static void openConfig(Minecraft mc) {
		if (!(mc.gui.screen() instanceof CleanConfigScreen)) {
			mc.gui.setScreen(new CleanConfigScreen(mc.gui.screen()));
		}
	}
}
