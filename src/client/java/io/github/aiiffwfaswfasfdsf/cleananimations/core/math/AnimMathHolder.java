package io.github.aiiffwfaswfasfdsf.cleananimations.core.math;

/** Holds the active {@link AnimMath}. Defaults to Java until {@link NativeLoader#init()} succeeds. */
final class AnimMathHolder {
	private static volatile AnimMath active = new JavaAnimMath();

	private AnimMathHolder() {
	}

	static AnimMath get() {
		return active;
	}

	static void set(AnimMath math) {
		active = math;
	}
}
