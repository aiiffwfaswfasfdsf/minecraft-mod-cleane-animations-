package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

/**
 * Pure timeline of one placement animation, measured in game ticks (fractional, i.e. {@code gameTime + partialTick}).
 * <ol>
 *   <li>{@code [start, start + duration)}: the real block is hidden from the chunk mesh and the copy is animated.</li>
 *   <li>The first tick at or after the end the real block is un-hidden ({@link #markUnhidden}); the copy keeps being drawn
 *       for {@link #HOLD_TICKS} more ticks so the chunk has time to rebuild (no flicker).</li>
 * </ol>
 */
public final class PlacementTimeline {
	/** Ticks the copy is still drawn after the real block was un-hidden. */
	public static final int HOLD_TICKS = 3;

	private final float start;
	private final float duration;
	private float unhiddenAt = Float.NaN;

	public PlacementTimeline(float start, float duration) {
		this.start = start;
		this.duration = Math.max(0.5f, duration);
	}

	/** Duration in ticks for a base duration in ms and a combined speed multiplier. */
	public static float durationTicks(float durationMs, float speed) {
		return Math.max(0.5f, durationMs / 50.0f / Math.max(0.05f, speed));
	}

	/** Animation progress in [0,1]. */
	public float progress(float now) {
		float p = (now - start) / duration;
		return p < 0 ? 0 : Math.min(p, 1.0f);
	}

	/** True while the real block must be hidden from the chunk mesh. */
	public boolean shouldBeHidden(float now) {
		return Float.isNaN(unhiddenAt) && now < start + duration;
	}

	public boolean isUnhidden() {
		return !Float.isNaN(unhiddenAt);
	}

	public void markUnhidden(float now) {
		if (Float.isNaN(unhiddenAt)) {
			unhiddenAt = now;
		}
	}

	/** True while the copy should still be drawn. */
	public boolean isDrawing(float now) {
		return Float.isNaN(unhiddenAt) || now < unhiddenAt + HOLD_TICKS;
	}

	public boolean isFinished(float now) {
		return !isDrawing(now);
	}

	public float start() {
		return start;
	}

	public float duration() {
		return duration;
	}
}
