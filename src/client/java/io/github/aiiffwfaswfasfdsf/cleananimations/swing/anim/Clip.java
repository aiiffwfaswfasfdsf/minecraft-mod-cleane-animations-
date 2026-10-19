package io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/** A set of channel tracks plus a duration in ticks. Missing channels stay at their rest value. */
public final class Clip {
	private final float durationTicks;
	private final Track[] tracks;

	public Clip(float durationTicks, Track[] tracks) {
		if (tracks.length != Channel.COUNT) {
			throw new IllegalArgumentException("expected " + Channel.COUNT + " tracks");
		}
		this.durationTicks = durationTicks;
		this.tracks = tracks;
	}

	public float durationTicks() {
		return durationTicks;
	}

	public Track track(Channel c) {
		return tracks[c.ordinal()];
	}

	/** Samples every channel at normalised progress {@code p} (clamped) into {@code out[0..Channel.COUNT)}. */
	public void sample(AnimMath math, float p, float[] out) {
		float t = Math.max(0f, Math.min(1f, p));
		for (int i = 0; i < Channel.COUNT; i++) {
			Track tr = tracks[i];
			out[i] = tr == null ? Channel.values()[i].rest : tr.sample(math, t);
		}
	}

	/** Writes the rest pose (identity) into {@code out}. */
	public static void rest(float[] out) {
		for (int i = 0; i < Channel.COUNT; i++) {
			out[i] = Channel.values()[i].rest;
		}
	}
}
