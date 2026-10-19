package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Bridging assist glue: when vanilla's hit result is a miss and the player holds a block, the use action is redirected
 * to the support block next to the gap that is being aimed at (see {@link BridgingTargets}).
 */
public final class BridgingAssist {
	private static BridgingTargets.Target current;

	private BridgingAssist() {
	}

	private static boolean active() {
		CleanConfig c = CleanConfig.get();
		return c.core.enabled && c.placement.bridgingAssist;
	}

	/** The target found by the last tick, for the crosshair indicator (may be null). */
	public static BridgingTargets.Target current() {
		return current;
	}

	/** Computes the current target without touching the game. Returns null when the assist does not apply. */
	static BridgingTargets.Target find(Minecraft mc) {
		LocalPlayer player = mc.player;
		ClientLevel level = mc.level;
		if (!active() || player == null || level == null || mc.gui.screen() != null) {
			return null;
		}
		if (!(player.getMainHandItem().getItem() instanceof BlockItem)
				&& !(player.getOffhandItem().getItem() instanceof BlockItem)) {
			return null;
		}
		HitResult hit = mc.hitResult;
		if (hit != null && hit.getType() != HitResult.Type.MISS) {
			return null;
		}
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
		BridgingTargets.World world = new BridgingTargets.World() {
			@Override
			public boolean solid(int x, int y, int z) {
				BlockState s = level.getBlockState(m.set(x, y, z));
				return !s.isAir() && s.isCollisionShapeFullBlock(level, m);
			}

			@Override
			public boolean replaceable(int x, int y, int z) {
				BlockState s = level.getBlockState(m.set(x, y, z));
				return s.isAir() || s.canBeReplaced();
			}
		};
		return BridgingTargets.select(eye.x, eye.y, eye.z, look.x, look.y, look.z, player.blockInteractionRange(),
				player.blockPosition().getY(), world);
	}

	/** Per-tick update of the indicator target. */
	public static void tick(Minecraft mc) {
		current = find(mc);
	}

	/**
	 * Called at the start of {@code Minecraft#startUseItem}: returns a hit result to use instead of the vanilla miss,
	 * or null to leave the action alone.
	 */
	public static BlockHitResult overrideHit(Minecraft mc) {
		BridgingTargets.Target t = find(mc);
		if (t == null) {
			return null;
		}
		Direction face = Direction.values()[t.face()];
		return new BlockHitResult(new Vec3(t.hx(), t.hy(), t.hz()), face, new BlockPos(t.sx(), t.sy(), t.sz()), false);
	}

	/** Small bracket frame around the crosshair, shown while the assist has a target. */
	public static final HudElement INDICATOR = (GuiGraphicsExtractor g, DeltaTracker delta) -> {
		if (current == null || !CleanConfig.get().placement.bridgingIndicator) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		int cx = mc.getWindow().getGuiScaledWidth() / 2;
		int cy = mc.getWindow().getGuiScaledHeight() / 2;
		int color = 0xE06CF0A0;
		int r = 7, l = 3;
		// four corner brackets
		for (int sx = -1; sx <= 1; sx += 2) {
			for (int sy = -1; sy <= 1; sy += 2) {
				int x = cx + sx * r, y = cy + sy * r;
				g.fill(Math.min(x, x - sx * l), y, Math.max(x, x - sx * l) + 1, y + 1, color);
				g.fill(x, Math.min(y, y - sy * l), x + 1, Math.max(y, y - sy * l) + 1, color);
			}
		}
	};
}
