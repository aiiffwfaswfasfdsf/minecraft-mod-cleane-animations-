package io.github.aiiffwfaswfasfdsf.cleananimations.gametest;

import com.mojang.blaze3d.platform.InputConstants;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.SwingRuntime;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.world.InteractionHand;

/**
 * Swing module end-to-end test: gives the player each tool, triggers a real swing (asserting the mixin path ran),
 * then freezes the animation at several progress values and takes screenshots named {@code swing-<kind>} (mid-swing)
 * and {@code swing-<kind>-<nn>} (frames of the whole motion), plus mining, combo, inspect and the vanilla fallback.
 */
@SuppressWarnings("UnstableApiUsage")
public class SwingClientGameTest implements FabricClientGameTest {
	private record Tool(String kind, String item) {
	}

	private static final Tool[] TOOLS = {
			new Tool("sword", "diamond_sword"),
			new Tool("axe", "diamond_axe"),
			new Tool("pickaxe", "diamond_pickaxe"),
			new Tool("shovel", "diamond_shovel"),
			new Tool("hoe", "diamond_hoe"),
			new Tool("spear", "diamond_spear"),
			new Tool("trident", "trident"),
			new Tool("mace", "mace"),
			new Tool("shears", "shears"),
			new Tool("brush", "brush"),
			new Tool("block", "stone"),
			new Tool("generic", "stick"),
	};
	private static final float[] FRAMES = {0.15f, 0.3f, 0.45f, 0.6f, 0.8f};

	@Override
	public void runTest(ClientGameTestContext context) {
		try (TestSingleplayerContext world = context.worldBuilder()
				.adjustSettings(state -> state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			world.getConnection().waitForChunksRender();
			world.getServer().runCommand("gamerule send_command_feedback false");
			world.getServer().runCommand("tp @p ~ ~ ~ 0 22");
			context.waitTicks(5);
			String only = String.valueOf(System.getenv("CLEANANIM_SWING_ONLY") == null ? "" : System.getenv("CLEANANIM_SWING_ONLY"));

			for (Tool tool : TOOLS) {
				if (!only.isEmpty() && !only.contains(tool.kind())) {
					continue;
				}
				world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:" + tool.item());
				context.waitTicks(12); // let the equip animation finish
				context.takeScreenshot("rest-" + tool.kind());

				// 1) real swing through the vanilla pipeline: the runtime must pick it up
				long before = SwingRuntime.swingsStarted;
				long appliedBefore = SwingRuntime.appliedCount;
				context.runOnClient(mc -> mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false));
				context.waitTicks(2);
				if (SwingRuntime.appliedCount <= appliedBefore) {
					throw new AssertionError("swing mixin did not run for " + tool.kind());
				}
				boolean customKind = !tool.kind().equals("bow");
				if (customKind && SwingRuntime.swingsStarted <= before) {
					throw new AssertionError("swing was not detected for " + tool.kind());
				}
				context.waitTicks(14);

				// 2) frozen frames of the motion
				for (int i = 0; i < FRAMES.length; i++) {
					SwingRuntime.debugProgress = FRAMES[i];
					context.waitTicks(2);
					context.takeScreenshot("swing-" + tool.kind() + "-" + (i + 1));
					if (i == 2) {
						context.takeScreenshot("swing-" + tool.kind());
					}
				}
				SwingRuntime.debugProgress = Float.NaN;

				// 3) mining loop
				SwingRuntime.debugMining = true;
				for (int i = 0; i < 3; i++) {
					SwingRuntime.debugProgress = Float.NaN;
					SwingRuntime.debugMining = true;
					context.waitTicks(1);
				}
				SwingRuntime.debugMining = false;
			}

			if (only.isEmpty() || only.contains("sword")) {
				world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:diamond_sword");
				context.waitTicks(12);
				for (int step = 1; step <= 2; step++) {
					SwingRuntime.debugComboStep = step;
					for (int i : new int[] {1, 2, 3}) {
						SwingRuntime.debugProgress = FRAMES[i];
						context.waitTicks(2);
						context.takeScreenshot("swing-sword-combo" + (step + 1) + "-" + (i + 1));
					}
				}
				SwingRuntime.debugComboStep = 0;
				SwingRuntime.debugProgress = Float.NaN;
			}

			// mining poses (looping clip sampled at fixed phases)
			for (Tool tool : TOOLS) {
				if (!only.isEmpty() && !only.contains(tool.kind())) {
					continue;
				}
				if (!tool.kind().matches("pickaxe|axe|shovel|hoe|sword")) {
					continue;
				}
				world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:" + tool.item());
				context.waitTicks(12);
				SwingRuntime.debugMining = true;
				for (int i = 0; i < 3; i++) {
					SwingRuntime.debugProgress = 0.15f + 0.35f * i;
					context.waitTicks(2);
					context.takeScreenshot("mine-" + tool.kind() + "-" + (i + 1));
				}
				SwingRuntime.debugMining = false;
				SwingRuntime.debugProgress = Float.NaN;
			}

			// inspect
			world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:diamond_sword");
			context.waitTicks(12);
			for (int i = 0; i < 4; i++) {
				SwingRuntime.debugInspect = 0.2f + 0.2f * i;
				context.waitTicks(2);
				context.takeScreenshot("inspect-" + (i + 1));
			}
			SwingRuntime.debugInspect = Float.NaN;

			// the real inspect key: R
			context.getInput().pressKey(InputConstants.KEY_R);
			context.waitTicks(10);
			context.takeScreenshot("inspect-key");
			context.waitTicks(40);

			// equip: swap items and capture the animation
			world.getServer().runCommand("item replace entity @p weapon.mainhand with minecraft:diamond_axe");
			context.waitTicks(1);
			context.takeScreenshot("equip-1");
			context.waitTicks(1);
			context.takeScreenshot("equip-2");
			context.waitTicks(1);
			context.takeScreenshot("equip-3");
			context.waitTicks(10);

			// vanilla fallback: module off must leave the vanilla swing alone
			boolean prev = CleanConfig.get().swing.enabled;
			CleanConfig.get().swing.enabled = false;
			long applied = SwingRuntime.appliedCount;
			context.runOnClient(mc -> mc.player.swing(InteractionHand.MAIN_HAND, mc.player.getMainHandItem().getAttackAnimation(), false));
			context.waitTicks(2);
			context.takeScreenshot("swing-vanilla-axe");
			if (SwingRuntime.appliedCount != applied) {
				throw new AssertionError("module applied transforms while disabled");
			}
			CleanConfig.get().swing.enabled = prev;
			context.waitTicks(10);
		}
	}
}
