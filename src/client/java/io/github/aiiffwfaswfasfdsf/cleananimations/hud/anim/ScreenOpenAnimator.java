package io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/** Pure logic of the screen-open ease: scale 0.96 to 1 over 150 ms (at speed 1) whenever a new screen shows up. */
public final class ScreenOpenAnimator {
	public static final float START_SCALE = 0.96f;
	public static final float SECONDS = 0.15f;

	private final AnimMath math;
	private Object current;
	private float progress = 1f;

	public ScreenOpenAnimator(AnimMath math) {
		this.math = math;
	}

	/** Call once per rendered frame with the screen being drawn; returns the scale to use for this frame. */
	public float frame(Object screen, float dt, float speed, float intensity) {
		if (screen != current) {
			current = screen;
			progress = 0f;
		}
		progress = Math.min(1f, progress + dt * Math.max(0.05f, speed) / SECONDS);
		float e = math.ease(AnimMath.EASE_OUT_CUBIC, progress);
		float from = 1f - (1f - START_SCALE) * Math.max(0f, intensity);
		return from + (1f - from) * e;
	}

	public void forget() {
		current = null;
	}
}
