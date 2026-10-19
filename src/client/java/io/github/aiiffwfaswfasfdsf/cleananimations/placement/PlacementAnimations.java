package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.block.MovingBlockRenderState;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Client side state of the placement animation: matches local placements to {@code ClientLevel#setBlock}, keeps the
 * chunk-mesh hide set in sync, and draws the animated copy every frame.
 */
public final class PlacementAnimations {
	/** Placements older than this many ticks are not matched any more. */
	private static final int PENDING_TIMEOUT_TICKS = 20;
	private static final double MAX_DISTANCE = 32.0;

	private record Pending(BlockPos clicked, Direction face, Block block, long gameTime) {
	}

	private static final class Anim {
		final BlockPos pos;
		final Block block;
		final Direction face;
		final int style;
		final PlacementTimeline timeline;

		Anim(BlockPos pos, Block block, Direction face, int style, PlacementTimeline timeline) {
			this.pos = pos;
			this.block = block;
			this.face = face;
			this.style = style;
			this.timeline = timeline;
		}
	}

	private static final Map<Long, Anim> ACTIVE = new HashMap<>();
	private static Pending pending;
	private static ClientLevel trackedLevel;
	private static final float[] OUT = new float[AnimMath.PLACE_TRANSFORM_SIZE];

	private PlacementAnimations() {
	}

	/** Number of running animations (game test). */
	public static int activeCount() {
		return ACTIVE.size();
	}

	/** Progress [0,1] of the animation running at {@code pos}, or NaN if there is none (debug / game test). */
	public static float progressOf(BlockPos pos) {
		Anim a = ACTIVE.get(pos.asLong());
		Minecraft mc = Minecraft.getInstance();
		return a == null ? Float.NaN : a.timeline.progress(now(mc));
	}

	/** Game time in ticks including the fractional part, used for every timeline. */
	static float now(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level == null) {
			return 0;
		}
		return level.getGameTime() + mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
	}

	private static PlacementConfig cfg() {
		return CleanConfig.get().placement;
	}

	private static boolean enabled() {
		return CleanConfig.get().core.enabled && cfg().placeAnimation;
	}

	/** Whether the state may be drawn as a plain copy of its block model (no block entity, no fluid, no special shape). */
	static boolean eligible(BlockState state) {
		return !state.isAir() && state.getRenderShape() == RenderShape.MODEL && !state.hasBlockEntity()
				&& state.getFluidState().isEmpty();
	}

	/** Fabric {@code UseBlockCallback}: remember what the local player is about to place. */
	public static InteractionResult onUseBlock(Player player, Level level, InteractionHand hand, BlockHitResult hit) {
		if (!level.isClientSide()) {
			return InteractionResult.PASS;
		}
		ItemStack stack = player.getItemInHand(hand);
		if (stack.getItem() instanceof BlockItem blockItem) {
			pending = new Pending(hit.getBlockPos(), hit.getDirection(), blockItem.getBlock(), level.getGameTime());
		} else {
			pending = null;
		}
		return InteractionResult.PASS;
	}

	/** Called from {@code ClientLevel#setBlock} before the state is applied. */
	public static void onSetBlock(ClientLevel level, BlockPos pos, BlockState newState) {
		if (!enabled()) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (level != mc.level || mc.player == null || newState.isAir() || !eligible(newState)) {
			return;
		}
		long key = pos.asLong();
		if (ACTIVE.containsKey(key)) {
			return;
		}
		BlockState old = level.getBlockState(pos);
		if (old == newState || old.getBlock() == newState.getBlock() && !old.isAir()) {
			return;
		}
		Direction face = null;
		Pending p = pending;
		if (p != null && level.getGameTime() - p.gameTime() <= PENDING_TIMEOUT_TICKS && p.block() == newState.getBlock()) {
			if (pos.equals(p.clicked().relative(p.face())) || pos.equals(p.clicked())) {
				face = p.face();
				pending = null;
			}
		}
		if (face == null) {
			if (cfg().onlyOwnPlacement || !(old.isAir() || old.canBeReplaced())) {
				return;
			}
			face = guessFace(level, pos);
		}
		if (mc.player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > MAX_DISTANCE * MAX_DISTANCE) {
			return;
		}
		float combinedSpeed = cfg().speed * CleanConfig.get().core.animationSpeed;
		float duration = PlacementTimeline.durationTicks(cfg().durationMs, combinedSpeed);
		PlacementTimeline timeline = new PlacementTimeline(level.getGameTime(), duration);
		int style = cfg().style == null ? AnimMath.PLACE_SETTLE : cfg().style.ordinal();
		ACTIVE.put(key, new Anim(pos.immutable(), newState.getBlock(), face, style, timeline));
		HiddenBlocks.hide(key);
	}

	/** Face on whose side a block placed by someone else most likely rests (normal points away from the support). */
	private static Direction guessFace(ClientLevel level, BlockPos pos) {
		Direction[] order = {Direction.DOWN, Direction.NORTH, Direction.SOUTH, Direction.WEST, Direction.EAST, Direction.UP};
		for (Direction d : order) {
			BlockPos n = pos.relative(d);
			if (level.getBlockState(n).isSolidRender()) {
				return d.getOpposite();
			}
		}
		return Direction.UP;
	}

	/** End of client tick: un-hide finished animations, drop stale ones. */
	public static void tick(Minecraft mc) {
		ClientLevel level = mc.level;
		if (level != trackedLevel) {
			reset();
			trackedLevel = level;
		}
		if (level == null || ACTIVE.isEmpty()) {
			return;
		}
		float now = level.getGameTime();
		Iterator<Map.Entry<Long, Anim>> it = ACTIVE.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<Long, Anim> e = it.next();
			Anim a = e.getValue();
			BlockState cur = level.getBlockState(a.pos);
			if (cur.isAir() || cur.getBlock() != a.block) {
				// removed, replaced or never arrived (rejected placement): give the block back to the mesher
				if (!a.timeline.isUnhidden()) {
					unhide(mc, a, e.getKey());
				}
				it.remove();
				continue;
			}
			if (!a.timeline.isUnhidden() && !a.timeline.shouldBeHidden(now)) {
				a.timeline.markUnhidden(now);
				unhide(mc, a, e.getKey());
			}
			if (a.timeline.isFinished(now)) {
				it.remove();
			}
		}
		if (ACTIVE.isEmpty()) {
			// belt and braces: nothing may stay hidden without an owner
			HiddenBlocks.clear();
		}
	}

	private static void unhide(Minecraft mc, Anim a, long key) {
		HiddenBlocks.show(key);
		markDirty(mc, a.pos);
	}

	private static void markDirty(Minecraft mc, BlockPos pos) {
		// flag 8 = "important": rebuilt with priority, so the hand-off happens within a frame or two
		mc.levelExtractor.blockChanged(pos, 8);
	}

	/** Forgets everything, e.g. on world change or disconnect. */
	public static void reset() {
		ACTIVE.clear();
		pending = null;
		HiddenBlocks.clear();
	}

	/** Draws every running copy. Called from {@code LevelRenderEvents.COLLECT_SUBMITS}. */
	public static void render(LevelRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel level = mc.level;
		if (level == null || ACTIVE.isEmpty()) {
			return;
		}
		float now = now(mc);
		float intensity = cfg().intensity * CleanConfig.get().core.intensity;
		Vec3 cam = ctx.levelState().cameraRenderState.pos;
		OrderedSubmitNodeCollector collector = ctx.submitNodeCollector().order(0);
		PoseStack ps = ctx.poseStack();
		AnimMath math = AnimMath.get();
		List<Anim> snapshot = new ArrayList<>(ACTIVE.values());
		for (Anim a : snapshot) {
			if (!a.timeline.isDrawing(now)) {
				continue;
			}
			BlockState state = level.getBlockState(a.pos);
			if (state.isAir() || state.getBlock() != a.block) {
				continue;
			}
			float progress = a.timeline.progress(now);
			Direction f = a.face;
			PlacementTransform.compute(math, a.style, progress, f.getStepX(), f.getStepY(), f.getStepZ(), intensity, OUT);
			if (a.timeline.isUnhidden()) {
				// hand-off phase: exactly the final pose, slightly inside the real block
				PlacementTransform.compute(math, a.style, 1.0f, f.getStepX(), f.getStepY(), f.getStepZ(), intensity, OUT);
			}
			ps.pushPose();
			ps.translate(a.pos.getX() - cam.x + 0.5 + OUT[3], a.pos.getY() - cam.y + 0.5 + OUT[4],
					a.pos.getZ() - cam.z + 0.5 + OUT[5]);
			ps.scale(OUT[0], OUT[1], OUT[2]);
			ps.translate(-0.5f, -0.5f, -0.5f);
			collector.submitMovingBlock(ps, movingBlock(level, a.pos, state), 0);
			ps.popPose();
		}
	}

	static MovingBlockRenderState movingBlock(ClientLevel level, BlockPos pos, BlockState state) {
		MovingBlockRenderState s = new MovingBlockRenderState();
		s.randomSeedPos = pos;
		s.blockPos = pos;
		s.blockState = state;
		s.biome = level.getBiome(pos);
		s.cardinalLighting = level.cardinalLighting();
		s.lightEngine = level.getLightEngine();
		return s;
	}
}
