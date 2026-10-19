package io.github.aiiffwfaswfasfdsf.cleananimations.swing;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.ConfigKeybind;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;

/** Entry point of the swing feature module. Called once from the client initializer. */
public final class SwingModule {
	/** Default R: flips and spins the held item. */
	public static final KeyMapping INSPECT = new KeyMapping(
			"key.cleananimations.inspect", InputConstants.KEY_R, ConfigKeybind.CATEGORY);

	private SwingModule() {
	}

	public static void init() {
		SwingAnimations.register();
		KeyMappingHelper.registerKeyMapping(INSPECT);
		ClientTickEvents.END_CLIENT_TICK.register(mc -> {
			while (INSPECT.consumeClick()) {
				if (mc.gui.screen() == null) {
					SwingRuntime.requestInspect(mc);
				}
			}
		});
	}
}
