package io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/** One keyframed channel. Times are normalised clip progress in [0,1]; sampling is done by {@link AnimMath}. */
public final class Track {
	private final float[] times;
	private final float[] values;
	private final int[] easing;

	public Track(float[] times, float[] values, int[] easing) {
		if (times.length != values.length || times.length == 0) {
			throw new IllegalArgumentException("track needs matching, non-empty time/value arrays");
		}
		this.times = times;
		this.values = values;
		this.easing = easing;
	}

	public int size() {
		return times.length;
	}

	public float time(int i) {
		return times[i];
	}

	public float value(int i) {
		return values[i];
	}

	public int easing(int i) {
		return easing[i];
	}

	public float sample(AnimMath math, float t) {
		return math.sampleTrack(times, values, easing, times.length, t);
	}
}
