package io.github.aiiffwfaswfasfdsf.cleananimations.gametest;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.placement.BreakAnimations;
import io.github.aiiffwfaswfasfdsf.cleananimations.placement.BridgingAssist;
import io.github.aiiffwfaswfasfdsf.cleananimations.placement.HiddenBlocks;
import io.github.aiiffwfaswfasfdsf.cleananimations.placement.PlacementAnimations;
import io.github.aiiffwfaswfasfdsf.cleananimations.placement.PlacementConfig;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screens.worldselection.WorldCreationUiState;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;

/**
 * End-to-end test of the placement module. Game time is frozen with {@code /tick freeze} and advanced with
 * {@code /tick step} so every screenshot shows a well defined animation progress.
 */
@SuppressWarnings("UnstableApiUsage")
public class PlacementClientGameTest implements FabricClientGameTest {
	private static final String[] ITEMS = {"stone", "cobblestone_slab", "oak_stairs", "glass", "chest"};
	private static final Block[] BLOCKS = {Blocks.STONE, Blocks.COBBLESTONE_SLAB, Blocks.OAK_STAIRS, Blocks.GLASS, Blocks.CHEST};
	private static final int[] XS = {-2, -1, 0, 1, 2};

	@Override
	public void runTest(ClientGameTestContext ctx) {
		boolean sodium = FabricLoader.getInstance().isModLoaded("sodium");
		String prefix = sodium ? "sodium-" : "";
		System.out.println("[placement-test] sodium loaded: " + sodium);
		PlacementConfig cfg = CleanConfig.get().placement;
		try (TestSingleplayerContext world = ctx.worldBuilder()
				.adjustSettings(state -> state.setGameMode(WorldCreationUiState.SelectedGameMode.CREATIVE))
				.create()) {
			world.getConnection().waitForChunksRender();
			ctx.waitTicks(5);

			// slow animation: 500 ms / 0.5 speed = 20 ticks, so a few ticks of stepping give distinct progress values
			cfg.durationMs = 500f;
			cfg.speed = 0.5f;
			cfg.intensity = 1.0f;
			cfg.style = PlacementConfig.Style.SETTLE;

			int gy = ctx.computeOnClient(mc -> mc.player.blockPosition().getY()) - 1; // grass layer
			int by = gy + 1;
			System.out.println("[placement-test] ground y=" + gy);
			world.getServer().runCommand("tp @p 0.5 " + by + " 1.5 180 38");
			for (int i = 0; i < ITEMS.length; i++) {
				world.getServer().runCommand("item replace entity @p hotbar." + i + " with minecraft:" + ITEMS[i]);
			}
			ctx.waitTicks(10);
			world.getServer().runCommand("gamerule send_command_feedback false");
			// let the recipe toasts from the creative inventory changes disappear before the screenshots
			ctx.waitTicks(150);
			world.getServer().runCommand("tick freeze");
			ctx.waitTicks(5);

			// clear the stage in front of the player
			for (int x : XS) {
				assertTrue(ctx.computeOnClient(mc -> mc.level.getBlockState(new BlockPos(x, by, -3)).isAir()), "stage not empty");
			}

			// place the five blocks, a few game ticks apart, so they are at different stages of the animation
			for (int i = 0; i < ITEMS.length; i++) {
				final int idx = i;
				ctx.runOnClient(mc -> {
					mc.player.getInventory().setSelectedSlot(idx);
					BlockPos support = new BlockPos(XS[idx], gy, -3);
					BlockHitResult hit = new BlockHitResult(new Vec3(XS[idx] + 0.5, by, -2.5), Direction.UP, support, false);
					mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
				});
				ctx.waitTicks(2);
				step(ctx, world, 1);
			}

			// 4 animated blocks (stone, slab, stairs, glass); the chest is a block entity and is skipped
			int hidden = HiddenBlocks.size();
			int active = PlacementAnimations.activeCount();
			System.out.println("[placement-test] hidden=" + hidden + " active=" + active);
			assertTrue(hidden == 4 && active == 4, "expected 4 hidden/active, got " + hidden + "/" + active);
			for (int i = 0; i < ITEMS.length; i++) {
				final int idx = i;
				ctx.runOnClient(mc -> {
					BlockPos p = new BlockPos(XS[idx], by, -3);
					assertTrue(mc.level.getBlockState(p).is(BLOCKS[idx]), "real block missing at " + p + " during animation");
					System.out.println("[placement-test] " + ITEMS[idx] + " progress=" + PlacementAnimations.progressOf(p));
				});
			}
			ctx.takeScreenshot(prefix + "placement-mid");

			// later in the animation: the stone is at ~65 % where the easeOutBack overshoot peaks
			step(ctx, world, 9);
			assertTrue(HiddenBlocks.size() >= 1, "stone should still be animating");
			ctx.takeScreenshot(prefix + "placement-overshoot");

			// let everything finish: duration 20 ticks + hold
			step(ctx, world, 30);
			ctx.waitTicks(10);
			assertTrue(HiddenBlocks.size() == 0, "hide set not empty after animation: " + HiddenBlocks.size());
			assertTrue(PlacementAnimations.activeCount() == 0, "animations still active");
			for (int i = 0; i < ITEMS.length; i++) {
				final int idx = i;
				ctx.runOnClient(mc -> assertTrue(mc.level.getBlockState(new BlockPos(XS[idx], by, -3)).is(BLOCKS[idx]),
						"block state changed for " + ITEMS[idx]));
			}
			ctx.takeScreenshot(prefix + "placement-end");

			// break the stone: instant in creative
			ctx.runOnClient(mc -> {
				mc.gameMode.startDestroyBlock(new BlockPos(-2, by, -3), Direction.UP);
			});
			ctx.waitTicks(2);
			assertTrue(ctx.computeOnClient(mc -> mc.level.getBlockState(new BlockPos(-2, by, -3)).isAir()), "block not broken");
			assertTrue(BreakAnimations.activeCount() == 1, "break animation missing");
			step(ctx, world, 2);
			ctx.takeScreenshot(prefix + "break-mid");
			step(ctx, world, 12);
			ctx.waitTicks(2);
			assertTrue(BreakAnimations.activeCount() == 0, "break animation did not end");
			assertTrue(HiddenBlocks.size() == 0, "break animation must never hide blocks");

			// a rejected placement must not leave anything hidden: place and remove within the animation
			ctx.runOnClient(mc -> {
				mc.player.getInventory().setSelectedSlot(0);
				BlockHitResult hit = new BlockHitResult(new Vec3(-1.5, by, -2.5), Direction.UP, new BlockPos(-2, gy, -3), false);
				mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
			});
			ctx.waitTicks(2);
			assertTrue(HiddenBlocks.size() == 1, "stone placed again should be hidden");
			world.getServer().runCommand("setblock -2 " + by + " -3 air");
			step(ctx, world, 3);
			ctx.waitTicks(3);
			assertTrue(HiddenBlocks.size() == 0, "removed block stayed hidden");
			assertTrue(PlacementAnimations.activeCount() == 0, "animation of removed block still active");

			world.getServer().runCommand("tick unfreeze");
			ctx.waitTicks(5);

			// bridging assist: platform high in the air so vanilla's ray misses
			cfg.bridgingAssist = true;
			cfg.durationMs = 250f;
			cfg.speed = 1.0f;
			world.getServer().runCommand("fill 0 99 0 6 99 6 stone");
			world.getServer().runCommand("tp @p 3.5 100 6.5 0 65");
			ctx.waitTicks(30);
			ctx.runOnClient(mc -> mc.player.getInventory().setSelectedSlot(0));
			ctx.waitTicks(5);
			HitResult.Type type = ctx.computeOnClient(mc -> mc.hitResult == null ? HitResult.Type.MISS : mc.hitResult.getType());
			System.out.println("[placement-test] vanilla hit type: " + type);
			assertTrue(type == HitResult.Type.MISS, "vanilla ray should miss over the void");
			BlockPos target = ctx.computeOnClient(mc -> {
				var t = BridgingAssist.current();
				return t == null ? null : new BlockPos(t.cx(), t.cy(), t.cz());
			});
			System.out.println("[placement-test] bridging target: " + target);
			assertTrue(target != null, "bridging assist found no target");
			ctx.takeScreenshot(prefix + "bridging-indicator");
			ctx.getInput().pressKey(options -> options.keyUse);
			ctx.waitTicks(6);
			assertTrue(ctx.computeOnClient(mc -> mc.level.getBlockState(target).is(Blocks.STONE)),
					"bridging placed nothing at " + target);
			ctx.takeScreenshot(prefix + "bridging-placed");
			ctx.waitTicks(20);
			assertTrue(HiddenBlocks.size() == 0, "hide set not empty after bridging placement");
			cfg.bridgingAssist = false;
		} finally {
			cfg.durationMs = 240f;
			cfg.speed = 1.0f;
		}
	}

	private static void step(ClientGameTestContext ctx, TestSingleplayerContext world, int ticks) {
		long before = ctx.computeOnClient(mc -> mc.level.getGameTime());
		world.getServer().runCommand("tick step " + ticks);
		ctx.waitFor(mc -> mc.level.getGameTime() >= before + ticks, 400);
		ctx.waitTicks(2);
	}

	private static void assertTrue(boolean cond, String message) {
		if (!cond) {
			throw new AssertionError(message);
		}
	}
}
