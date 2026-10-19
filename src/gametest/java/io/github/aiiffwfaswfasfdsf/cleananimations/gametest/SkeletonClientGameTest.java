package io.github.aiiffwfaswfasfdsf.cleananimations.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.screen.CleanConfigScreen;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

/**
 * Skeleton end-to-end test: boot, create a creative world (the builder defaults to a flat world), screenshot, open the config screen
 * through the real key binding, screenshot, close. Feature agents add their own test classes next to this one
 * and register them in src/gametest/resources/fabric.mod.json.
 */
@SuppressWarnings("UnstableApiUsage")
public class SkeletonClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder()
				.adjustSettings(state -> state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			world.getConnection().waitForChunksRender();
			context.waitTicks(5);
			context.takeScreenshot("skeleton");

			// Open the config screen with the registered key (default K).
			context.getInput().pressKey(InputConstants.KEY_K);
			context.waitForScreen(CleanConfigScreen.class);
			context.waitTicks(2);
			context.takeScreenshot("config-screen");

			context.setScreen(() -> null);
			context.waitTick();
		}
	}
}
