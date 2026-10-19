package io.github.aiiffwfaswfasfdsf.cleananimations.core.math;

/**
 * Animation math used by every feature. Two interchangeable implementations exist:
 * {@link NativeAnimMath} (Rust, via the Java FFM API) and {@link JavaAnimMath} (pure Java, same formulas).
 */
public interface AnimMath {
	int EASE_LINEAR = 0;
	int EASE_OUT_CUBIC = 1;
	int EASE_IN_OUT_CUBIC = 2;
	int EASE_OUT_BACK = 3;
	int EASE_OUT_ELASTIC = 4;
	int EASE_OUT_QUINT = 5;
	int EASE_IN_OUT_SINE = 6;

	int PLACE_SETTLE = 0;
	int PLACE_POP = 1;
	int PLACE_SLIDE = 2;
	int PLACE_DROP = 3;

	/** Number of floats written by {@link #placeTransform}. */
	int PLACE_TRANSFORM_SIZE = 7;

	/** The active implementation (native when it could be loaded, otherwise Java). */
	static AnimMath get() {
		return AnimMathHolder.get();
	}

	/** Human readable name of the implementation, e.g. "native" or "java". */
	String name();

	boolean isNative();

	/** Easing curve; {@code t} is clamped to [0,1]. */
	float ease(int kind, float t);

	/** Damped spring step. Writes new position and velocity to {@code out[0..2]}. */
	void springStep(float pos, float vel, float target, float stiffness, float damping, float dt, float[] out);

	/**
	 * Samples a keyframe track. The value between key i and i+1 is eased with {@code easing[i]};
	 * clamps outside the time range.
	 */
	float sampleTrack(float[] times, float[] values, int[] easing, int count, float t);

	/**
	 * Block placement transform. Writes 7 floats to {@code out}:
	 * scaleX, scaleY, scaleZ, offX, offY, offZ, alpha (currently always 1).
	 */
	void placeTransform(int style, float progress, float dirX, float dirY, float dirZ, float[] out);
}
