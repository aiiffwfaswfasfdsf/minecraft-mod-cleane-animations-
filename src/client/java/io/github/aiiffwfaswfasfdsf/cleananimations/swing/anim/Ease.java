package io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/**
 * Easing names usable in animation JSON. The "out" and "in_out" curves map straight to {@link AnimMath};
 * the "in_*" curves are not part of the core math, so the parser expands them into a handful of linear sub-keys.
 */
public final class Ease {
	/** Marker id for curves expanded by the parser; never passed to {@link AnimMath}. */
	static final int EXPANDED = -1;

	private Ease() {
	}

	/** Core easing id, {@link #EXPANDED} for in-curves, or -2 for an unknown name. */
	static int coreId(String name) {
		switch (name) {
			case "linear":
				return AnimMath.EASE_LINEAR;
			case "out_cubic":
				return AnimMath.EASE_OUT_CUBIC;
			case "in_out_cubic":
				return AnimMath.EASE_IN_OUT_CUBIC;
			case "out_back":
				return AnimMath.EASE_OUT_BACK;
			case "out_elastic":
				return AnimMath.EASE_OUT_ELASTIC;
			case "out_quint":
				return AnimMath.EASE_OUT_QUINT;
			case "in_out_sine":
				return AnimMath.EASE_IN_OUT_SINE;
			case "in_quad":
			case "in_cubic":
			case "in_quint":
			case "in_sine":
				return EXPANDED;
			default:
				return -2;
		}
	}

	/** Shape of an in-curve at u in [0,1]. */
	static float inCurve(String name, float u) {
		switch (name) {
			case "in_quad":
				return u * u;
			case "in_cubic":
				return u * u * u;
			case "in_quint":
				return u * u * u * u * u;
			case "in_sine":
				return 1f - (float) Math.cos(u * Math.PI / 2.0);
			default:
				return u;
		}
	}
}
