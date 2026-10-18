package io.github.aiiffwfaswfasfdsf.cleananimations.core;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.NativeLoader;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudModule;
import io.github.aiiffwfaswfasfdsf.cleananimations.placement.PlacementModule;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.SwingModule;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class CleanAnimationsClient implements ClientModInitializer {
	public static final String MOD_ID = "cleananimations";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitializeClient() {
		CleanConfig.load(FabricLoader.getInstance().getConfigDir());
		CleanConfig.get().save(); // writes defaults / repaired values so the file always exists
		NativeLoader.init();
		ConfigKeybind.init();

		SwingModule.init();
		PlacementModule.init();
		HudModule.init();

		LOGGER.info("Clean Animations initialised (math: {})", AnimMath.get().name());
	}
}
