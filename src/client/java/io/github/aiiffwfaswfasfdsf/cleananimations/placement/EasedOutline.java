package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import net.fabricmc.fabric.api.client.rendering.v1.level.LevelRenderContext;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.BlockOutlineRenderState;
import net.minecraft.util.ARGB;
import net.minecraft.world.phys.Vec3;

/**
 * Replaces the vanilla block selection box by one that glides between blocks. It draws through the same submit API and
 * with the same render types and colours as vanilla ({@code LevelRenderer#submitBlockOutline}).
 */
public final class EasedOutline {
	private static final OutlineEase EASE = new OutlineEase();
	private static long lastNanos;

	private EasedOutline() {
	}

	/** {@code LevelRenderEvents.BEFORE_BLOCK_OUTLINE}: returns false when this class drew the outline itself. */
	public static boolean beforeBlockOutline(LevelRenderContext ctx, BlockOutlineRenderState state) {
		CleanConfig cfg = CleanConfig.get();
		if (!cfg.core.enabled || !cfg.placement.easedOutline || state.shape().isEmpty()) {
			return true;
		}
		long nowN = System.nanoTime();
		double dt = lastNanos == 0 ? 1.0 : (nowN - lastNanos) / 1.0e9;
		lastNanos = nowN;
		double speed = cfg.placement.outlineSpeed * cfg.core.animationSpeed;
		EASE.update(state.pos().getX(), state.pos().getY(), state.pos().getZ(), dt, speed);

		Vec3 cam = ctx.levelState().cameraRenderState.pos;
		PoseStack ps = ctx.poseStack();
		SubmitNodeCollector collector = ctx.submitNodeCollector();
		ps.pushPose();
		ps.translate(EASE.x() - cam.x, EASE.y() - cam.y, EASE.z() - cam.z);
		float lineWidth = ctx.gameRenderer().gameRenderState().windowRenderState.appropriateLineWidth;
		boolean high = state.highContrast();
		if (high) {
			collector.submitShapeOutline(ps, state.shape(), RenderTypes.secondaryBlockOutline(), -16777216, 7.0f,
					state.isTranslucent());
		}
		int color = high ? -11010079 : ARGB.black(102);
		RenderType type = high ? RenderTypes.linesDepthBias()
				: (ctx.gameRenderer().useImprovedTransparency() ? RenderTypes.linesTranslucentNoDepthWrite()
				: RenderTypes.linesTranslucent());
		collector.submitShapeOutline(ps, state.shape(), type, color, lineWidth, state.isTranslucent());
		ps.popPose();
		return false;
	}
}
