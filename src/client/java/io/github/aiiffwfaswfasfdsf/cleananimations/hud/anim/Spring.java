package io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/** Tiny damped spring around a target, stepped with {@link AnimMath#springStep}. */
public final class Spring {
	private final AnimMath math;
	private final float[] out = new float[2];
	public float pos;
	public float vel;

	public Spring(AnimMath math) {
		this.math = math;
	}

	public Spring(AnimMath math, float start) {
		this(math);
		this.pos = start;
	}

	/**
	 * Advances by {@code dt} seconds. {@code speed} scales the time axis (stiffness by speed squared, damping by speed)
	 * so the damping ratio stays the same.
	 */
	public void step(float target, float stiffness, float damping, float speed, float dt) {
		float s = Math.max(0.05f, speed);
		math.springStep(pos, vel, target, stiffness * s * s, damping * s, dt, out);
		pos = out[0];
		vel = out[1];
	}

	public void snap(float value) {
		pos = value;
		vel = 0f;
	}

	public boolean isSettled(float target, float eps) {
		return Math.abs(pos - target) < eps && Math.abs(vel) < eps * 10f;
	}
}
