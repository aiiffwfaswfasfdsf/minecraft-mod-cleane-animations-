package io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/** Pure logic of the smooth zoom: eased in/out progress plus a scroll-adjustable zoom level. */
public final class ZoomController {
	public static final float MIN_LEVEL = 1.25f;
	public static final float SCROLL_STEP = 1.14f;

	private final AnimMath math;
	private boolean held;
	private float progress; // 0 = not zoomed, 1 = fully zoomed
	private float level = 3f; // target zoom factor
	private float shownLevel = 3f; // smoothed toward level
	private float cinematicX;
	private float cinematicY;

	public ZoomController(AnimMath math) {
		this.math = math;
	}

	public void setHeld(boolean held) {
		this.held = held;
	}

	public boolean isHeld() {
		return held;
	}

	/** @param maxLevel upper bound of the zoom factor; @param seconds time of a full transition at speed 1 */
	public void update(float dt, float speed, float seconds, float maxLevel) {
		float step = dt * Math.max(0.05f, speed) / Math.max(0.02f, seconds);
		progress = held ? Math.min(1f, progress + step) : Math.max(0f, progress - step * 1.15f);
		level = clampLevel(level, maxLevel);
		float k = 1f - (float) Math.exp(-14f * Math.max(0.05f, speed) * dt);
		shownLevel += (level - shownLevel) * k;
	}

	/** Applies a scroll amount (positive = wheel up = zoom in). */
	public void scroll(double amount, float maxLevel) {
		if (amount == 0) {
			return;
		}
		float factor = (float) Math.pow(SCROLL_STEP, Math.max(-4, Math.min(4, amount)));
		level = clampLevel(level * factor, maxLevel);
	}

	public void setLevel(float newLevel, float maxLevel) {
		level = clampLevel(newLevel, maxLevel);
		shownLevel = level;
	}

	public float level() {
		return level;
	}

	private static float clampLevel(float v, float max) {
		return Math.max(MIN_LEVEL, Math.min(Math.max(MIN_LEVEL, max), v));
	}

	/** Eased transition value in [0,1]. */
	public float eased() {
		return math.ease(AnimMath.EASE_IN_OUT_CUBIC, progress);
	}

	public boolean isActive() {
		return progress > 0.0005f;
	}

	/** Current zoom factor (1 = none). */
	public float zoomFactor() {
		return 1f + (shownLevel - 1f) * eased();
	}

	/** Multiplier for the field of view (1 = none). */
	public float fovMultiplier() {
		return 1f / zoomFactor();
	}

	/**
	 * Cinematic smoothing of one mouse delta pair; {@code amount} in [0,1) is the weight of the previous frame.
	 * Writes the result to {@code out[0..2]}.
	 */
	public void smoothTurn(double dx, double dy, float amount, double[] out) {
		cinematicX += (float) ((dx - cinematicX) * (1f - amount));
		cinematicY += (float) ((dy - cinematicY) * (1f - amount));
		out[0] = cinematicX;
		out[1] = cinematicY;
	}

	public void resetSmoothing() {
		cinematicX = 0;
		cinematicY = 0;
	}
}
