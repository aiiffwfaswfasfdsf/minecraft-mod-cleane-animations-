package io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/** Pure logic of the crosshair feedback: a springy scale and a short hit-marker flash. */
public final class CrosshairAnimator {
	/** Seconds the hit marker stays visible at speed 1. */
	public static final float HIT_MARKER_SECONDS = 0.28f;

	private final Spring scale;
	private float hitTimer = 1f; // 0..1, 1 = finished
	private boolean mining;

	public CrosshairAnimator(AnimMath math) {
		this.scale = new Spring(math);
	}

	public void update(float dt, float speed, float intensity) {
		float target = mining ? 0.10f * intensity : 0f;
		scale.step(target, 520f, 30f, speed, dt);
		if (hitTimer < 1f) {
			hitTimer = Math.min(1f, hitTimer + dt * Math.max(0.05f, speed) / HIT_MARKER_SECONDS);
		}
	}

	/** Crosshair expands (attack swing). */
	public void attack(float intensity) {
		scale.vel += 5.5f * intensity;
	}

	/** Crosshair squeezes briefly (item use / block placement). */
	public void use(float intensity) {
		scale.vel -= 3.2f * intensity;
	}

	public void hit() {
		hitTimer = 0f;
	}

	public void setMining(boolean mining) {
		this.mining = mining;
	}

	/** Multiplier applied to the crosshair size around its centre. */
	public float scale() {
		return Math.max(0.5f, Math.min(1.8f, 1f + scale.pos));
	}

	/** 0 when no hit marker is visible, up to 1 right after a hit. */
	public float hitMarkerAlpha() {
		if (hitTimer >= 1f) {
			return 0f;
		}
		float inv = 1f - hitTimer;
		return inv * inv;
	}

	/** Progress of the hit marker (0 = just hit, 1 = gone); the marker's arms spread outwards with it. */
	public float hitMarkerProgress() {
		return hitTimer;
	}
}
