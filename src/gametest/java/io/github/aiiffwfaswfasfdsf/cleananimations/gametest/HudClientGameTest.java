package io.github.aiiffwfaswfasfdsf.cleananimations.gametest;


import com.mojang.blaze3d.platform.InputConstants;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudRuntime;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;

/** Exercises the hud module in a real world: hotbar glide, zoom (incl. scroll swallowing), crosshair feedback. */
@SuppressWarnings("UnstableApiUsage")
public class HudClientGameTest implements FabricClientGameTest {
	@Override
	public void runTest(ClientGameTestContext context) {
		CleanConfig.set(new CleanConfig());
		try (TestSingleplayerContext world = context.worldBuilder()
				.adjustSettings(state -> state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			world.getConnection().waitForChunksRender();
			buildScene(world);
			context.waitTicks(20);
			context.runOnClient(mc -> mc.gui.hud.getChat().clearMessages(true));
			context.waitTicks(2);

			hotbar(context, world);
			zoom(context);
			crosshair(context);
			disabledKeepsVanilla(context);
		}
	}

	private static void buildScene(TestSingleplayerContext world) {
		var server = world.getServer();
		server.runCommand("gamerule spawn_mob_spawning false");
		server.runCommand("time set noon");
		server.runCommand("tp @p 0 -60 0 0 0");
		server.runCommand("fill -6 -61 10 6 -61 10 minecraft:stone_bricks");
		server.runCommand("fill -4 -60 10 -3 -57 10 minecraft:red_concrete");
		server.runCommand("fill -1 -60 10 1 -58 10 minecraft:lime_concrete");
		server.runCommand("fill 3 -60 10 4 -56 10 minecraft:blue_concrete");
		server.runCommand("fill 0 -57 10 0 -55 10 minecraft:yellow_concrete");
		String[] items = {"diamond_sword", "diamond_pickaxe", "stone", "oak_planks", "apple", "torch", "bow", "shield", "compass"};
		for (int i = 0; i < items.length; i++) {
			server.runCommand("item replace entity @p hotbar." + i + " with minecraft:" + items[i] + (i == 2 || i == 3 ? " 32" : ""));
		}
	}

	private static void hotbar(ClientGameTestContext context, TestSingleplayerContext world) {
		context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
		context.waitTicks(30);
		// Slow the glide a little so the screenshot reliably lands mid-slot-glide.
		context.runOnClient(mc -> HudConfig.get().hotbarSpeed = 0.35f);
		context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(4));
		context.waitTick();
		float mid = context.computeOnClient(mc -> HudRuntime.HOTBAR.selectionPosition());
		System.out.println("[hud-test] hotbar glide position at screenshot time: " + mid);
		context.takeScreenshot("hud-hotbar");
		assertTrue(mid > 0f && mid < 4f, "selection should be gliding, was " + mid);
		context.waitTicks(60);
		float end = context.computeOnClient(mc -> HudRuntime.HOTBAR.selectionPosition());
		assertEquals(4f, end, 0.05f);
		context.runOnClient(mc -> HudConfig.get().hotbarSpeed = 1.0f);
	}

	private static void zoom(ClientGameTestContext context) {
		context.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(2));
		context.waitTicks(20);
		float baseFov = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());

		context.getInput().holdKey(InputConstants.KEY_C);
		context.waitTicks(25);
		float zoomedFov = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
		System.out.println("[hud-test] fov base=" + baseFov + " zoomed=" + zoomedFov);
		assertTrue(zoomedFov < baseFov * 0.5f, "zoom should narrow the FOV (" + baseFov + " -> " + zoomedFov + ")");

		// Scroll is swallowed while zooming: hotbar slot stays, zoom level changes.
		float levelBefore = context.computeOnClient(mc -> HudRuntime.ZOOM.level());
		context.getInput().scroll(1.0);
		context.waitTicks(2);
		int slotWhileZooming = context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
		float levelAfter = context.computeOnClient(mc -> HudRuntime.ZOOM.level());
		assertEquals(2, slotWhileZooming, "scroll must not change the hotbar slot while zooming");
		assertTrue(levelAfter > levelBefore, "scroll should zoom in further");
		context.waitTicks(20);
		context.takeScreenshot("hud-zoom");

		context.getInput().releaseKey(InputConstants.KEY_C);
		context.waitTicks(40);
		float afterFov = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
		assertEquals(baseFov, afterFov, 0.5f, "FOV must ease back to normal");
		context.getInput().scroll(1.0);
		context.waitTicks(2);
		int slotAfter = context.computeOnClient(mc -> mc.player.getInventory().getSelectedSlot());
		assertTrue(slotAfter != 2, "scroll must change the hotbar again after zooming");
	}

	private static void crosshair(ClientGameTestContext context) {
		context.runOnClient(mc -> HudConfig.get().crosshairSpeed = 0.5f);
		context.runOnClient(mc -> {
			HudRuntime.CROSSHAIR.attack(1.0f);
			HudRuntime.CROSSHAIR.hit();
		});
		context.waitTick();
		float scale = context.computeOnClient(mc -> HudRuntime.CROSSHAIR.scale());
		float alpha = context.computeOnClient(mc -> HudRuntime.CROSSHAIR.hitMarkerAlpha());
		System.out.println("[hud-test] crosshair scale=" + scale + " hit alpha=" + alpha);
		context.takeScreenshot("hud-crosshair");
		assertTrue(alpha > 0f && scale > 1f, "crosshair feedback should be visible (scale " + scale + ", alpha " + alpha + ")");
		context.waitTicks(80);
		assertEquals(1f, context.computeOnClient(mc -> HudRuntime.CROSSHAIR.scale()), 0.02f);
		context.runOnClient(mc -> HudConfig.get().crosshairSpeed = 1.0f);
	}

	private static void disabledKeepsVanilla(ClientGameTestContext context) {
		context.runOnClient(mc -> HudConfig.get().enabled = false);
		context.getInput().holdKey(InputConstants.KEY_C);
		context.waitTicks(15);
		float fov = context.computeOnClient(mc -> mc.gameRenderer.mainCamera().getFov());
		float expected = context.computeOnClient(mc -> mc.options.fov().get().floatValue());
		context.getInput().releaseKey(InputConstants.KEY_C);
		assertTrue(fov >= expected * 0.95f, "module disabled: FOV must not zoom (" + fov + " vs " + expected + ")");
		context.runOnClient(mc -> HudConfig.get().enabled = true);
		context.waitTicks(5);
	}

	private static void assertTrue(boolean condition, String message) {
		if (!condition) {
			throw new AssertionError(message);
		}
	}

	private static void assertEquals(float expected, float actual, float delta, String message) {
		assertTrue(Math.abs(expected - actual) <= delta, message + " (expected " + expected + ", got " + actual + ")");
	}

	private static void assertEquals(float expected, float actual, float delta) {
		assertEquals(expected, actual, delta, "values differ");
	}

	private static void assertEquals(int expected, int actual, String message) {
		assertTrue(expected == actual, message + " (expected " + expected + ", got " + actual + ")");
	}
}
