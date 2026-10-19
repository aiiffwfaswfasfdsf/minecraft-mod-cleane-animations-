package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

/** Pure exponential smoothing of the block outline position. */
public final class OutlineEase {
	/** Jumps further than this many blocks snap instead of gliding. */
	public static final double SNAP_DISTANCE = 8.0;
	/** Without updates for this long (seconds) the next update snaps. */
	public static final double STALE_SECONDS = 0.35;
	private static final double BASE_RATE = 24.0;

	private double x, y, z;
	private boolean valid;

	/**
	 * Moves towards the target.
	 *
	 * @param dt seconds since the last update
	 * @return true if the position snapped
	 */
	public boolean update(double tx, double ty, double tz, double dt, double speed) {
		double dx = tx - x, dy = ty - y, dz = tz - z;
		double dist2 = dx * dx + dy * dy + dz * dz;
		if (!valid || dt > STALE_SECONDS || dist2 > SNAP_DISTANCE * SNAP_DISTANCE || speed <= 0) {
			x = tx;
			y = ty;
			z = tz;
			valid = true;
			return true;
		}
		double a = 1.0 - Math.exp(-Math.max(0, dt) * BASE_RATE * speed);
		x += dx * a;
		y += dy * a;
		z += dz * a;
		if (dist2 < 1e-6) {
			x = tx;
			y = ty;
			z = tz;
		}
		return false;
	}

	public double x() {
		return x;
	}

	public double y() {
		return y;
	}

	public double z() {
		return z;
	}

	public boolean isValid() {
		return valid;
	}
}
