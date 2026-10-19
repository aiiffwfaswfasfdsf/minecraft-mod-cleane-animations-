package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/** Pure helper turning timeline progress into a scale/offset around the block centre. */
public final class PlacementTransform {
	/** Scale used for the final pose so the copy never z-fights with the real block. */
	public static final float MAX_FINAL_SCALE = 0.999f;

	private PlacementTransform() {
	}

	/**
	 * Computes the transform of the placed block.
	 *
	 * @param style     one of {@code AnimMath.PLACE_*}
	 * @param progress  [0,1]
	 * @param nx,ny,nz  normal of the clicked face (points from the support block to the new block)
	 * @param intensity combined intensity (1 = default motion)
	 * @param out       7 floats: scaleX/Y/Z, offsetX/Y/Z (blocks), alpha
	 */
	public static void compute(AnimMath math, int style, float progress, float nx, float ny, float nz,
							   float intensity, float[] out) {
		// SETTLE and SLIDE start on the open side of the clicked face and move into the support.
		// AnimMath's SETTLE offsets by -dir, its SLIDE offsets by +dir.
		float sign = style == AnimMath.PLACE_SETTLE ? -1.0f : 1.0f;
		math.placeTransform(style, progress, sign * nx, sign * ny, sign * nz, out);
		for (int i = 0; i < 3; i++) {
			out[i] = 1.0f + (out[i] - 1.0f) * intensity;
			out[3 + i] *= intensity;
		}
		if (progress >= 1.0f) {
			// final pose: strictly inside the real block volume
			for (int i = 0; i < 3; i++) {
				out[i] = Math.min(out[i], MAX_FINAL_SCALE);
			}
		}
		for (int i = 0; i < 3; i++) {
			if (out[i] < 0.02f) {
				out[i] = 0.02f;
			}
		}
	}
}
