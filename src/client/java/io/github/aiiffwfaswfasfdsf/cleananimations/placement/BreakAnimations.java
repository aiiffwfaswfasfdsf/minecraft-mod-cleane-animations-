package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;
import java.util.ArrayList;
import java.util.List;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;

/**
 * Short shrink + tilt of a block that the local player just broke. Purely a visual copy of the old state; the world
 * and the chunk mesh are not touched.
 */
public final class BreakAnimations {
	private static final float DURATION_TICKS = 4.5f;
	private static final int MAX_ACTIVE = 64;

	private record Anim(BlockPos pos, BlockState state, float start, float duration, float tiltDeg, float ax, float az) {
	}

	private static final List<Anim> ACTIVE = new ArrayList<>();
	private static ClientLevel level;

	private BreakAnimations() {
	}

	public static int activeCount() {
		return ACTIVE.size();
	}

	/** Fabric {@code ClientPlayerBlockBreakEvents.AFTER}. */
	public static void onBreak(ClientLevel lvl, LocalPlayer player, BlockPos pos, BlockState state) {
		if (!CleanConfig.get().core.enabled || !CleanConfig.get().placement.breakAnimation || !PlacementAnimations.eligible(state)) {
			return;
		}
		Minecraft mc = Minecraft.getInstance();
		if (lvl != mc.level || player.distanceToSqr(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5) > 32 * 32) {
			return;
		}
		if (level != lvl) {
			ACTIVE.clear();
			level = lvl;
		}
		if (ACTIVE.size() >= MAX_ACTIVE) {
			ACTIVE.remove(0);
		}
		PlacementConfig cfg = CleanConfig.get().placement;
		float speed = cfg.speed * CleanConfig.get().core.animationSpeed;
		float duration = Math.max(1.0f, DURATION_TICKS / Math.max(0.05f, speed));
		int h = (int) (pos.asLong() * 0x9E3779B97F4A7C15L >>> 40);
		float angle = ((h & 1) == 0 ? 1 : -1) * (14 + (h >>> 1 & 7));
		double dir = (h >>> 4 & 255) / 255.0 * Math.PI * 2;
		ACTIVE.add(new Anim(pos.immutable(), state, lvl.getGameTime(), duration, angle,
				(float) Math.cos(dir), (float) Math.sin(dir)));
	}

	public static void tick(Minecraft mc) {
		if (mc.level != level) {
			ACTIVE.clear();
			level = mc.level;
		}
		if (ACTIVE.isEmpty() || mc.level == null) {
			return;
		}
		float now = mc.level.getGameTime();
		ACTIVE.removeIf(a -> now >= a.start() + a.duration() + 1);
	}

	public static void reset() {
		ACTIVE.clear();
	}

	public static void render(LevelRenderContext ctx) {
		Minecraft mc = Minecraft.getInstance();
		ClientLevel lvl = mc.level;
		if (lvl == null || ACTIVE.isEmpty()) {
			return;
		}
		float now = PlacementAnimations.now(mc);
		float intensity = CleanConfig.get().placement.intensity * CleanConfig.get().core.intensity;
		Vec3 cam = ctx.levelState().cameraRenderState.pos;
		OrderedSubmitNodeCollector collector = ctx.submitNodeCollector().order(0);
		PoseStack ps = ctx.poseStack();
		AnimMath math = AnimMath.get();
		for (Anim a : new ArrayList<>(ACTIVE)) {
			float p = Math.min(1.0f, Math.max(0.0f, (now - a.start()) / a.duration()));
			if (p >= 1.0f) {
				continue;
			}
			// shrink: slow start, fast end; tilt and sink follow the same curve
			float shrink = math.ease(AnimMath.EASE_IN_OUT_CUBIC, p);
			float scale = Math.max(0.02f, 1.0f - 0.95f * Math.min(1.0f, shrink * intensity));
			float tilt = (float) Math.toRadians(a.tiltDeg() * intensity) * math.ease(AnimMath.EASE_OUT_CUBIC, p);
			float sink = -0.18f * p * p * intensity;
			ps.pushPose();
			ps.translate(a.pos().getX() - cam.x + 0.5, a.pos().getY() - cam.y + 0.5 + sink, a.pos().getZ() - cam.z + 0.5);
			ps.rotate(new Quaternionf().rotateAxis(tilt, a.ax(), 0.0f, a.az()));
			ps.scale(scale, scale, scale);
			ps.translate(-0.5f, -0.5f, -0.5f);
			collector.submitMovingBlock(ps, PlacementAnimations.movingBlock(lvl, a.pos(), a.state()), 0);
			ps.popPose();
		}
	}
}
