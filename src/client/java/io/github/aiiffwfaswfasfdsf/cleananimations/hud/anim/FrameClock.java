package io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim;

/** Measures wall-clock seconds between successive calls (clamped so a hitch never explodes a spring). */
public final class FrameClock {
	private static final float MAX_DT = 0.1f;
	private long last = Long.MIN_VALUE;

	/** Seconds since the previous call; 0 on the first call. */
	public float tick() {
		long now = System.nanoTime();
		float dt = last == Long.MIN_VALUE ? 0f : (now - last) / 1.0e9f;
		last = now;
		return Math.max(0f, Math.min(MAX_DT, dt));
	}

	public void reset() {
		last = Long.MIN_VALUE;
	}
}
