package io.github.aiiffwfaswfasfdsf.cleananimations.core.math;

/** Pure-Java implementation; formulas mirror native/src/lib.rs exactly. */
public final class JavaAnimMath implements AnimMath {
	private static final float BACK_OVERSHOOT = 1.70158f;
	private static final float PI = (float) Math.PI;
	private static final float MAX_SPRING_STEP = 1.0f / 120.0f;

	@Override
	public String name() {
		return "java";
	}

	@Override
	public boolean isNative() {
		return false;
	}

	private static float clamp01(float t) {
		if (Float.isNaN(t)) {
			return 0.0f;
		}
		return Math.max(0.0f, Math.min(1.0f, t));
	}

	@Override
	public float ease(int kind, float t) {
		t = clamp01(t);
		switch (kind) {
			case EASE_OUT_CUBIC: {
				float u = 1.0f - t;
				return 1.0f - u * u * u;
			}
			case EASE_IN_OUT_CUBIC: {
				if (t < 0.5f) {
					return 4.0f * t * t * t;
				}
				float u = -2.0f * t + 2.0f;
				return 1.0f - u * u * u / 2.0f;
			}
			case EASE_OUT_BACK: {
				float c1 = BACK_OVERSHOOT;
				float c3 = c1 + 1.0f;
				float u = t - 1.0f;
				return 1.0f + c3 * u * u * u + c1 * u * u;
			}
			case EASE_OUT_ELASTIC: {
				if (t <= 0.0f) {
					return 0.0f;
				}
				if (t >= 1.0f) {
					return 1.0f;
				}
				float c4 = (2.0f * PI) / 3.0f;
				return (float) (Math.pow(2.0, -10.0f * t) * Math.sin((t * 10.0f - 0.75f) * c4)) + 1.0f;
			}
			case EASE_OUT_QUINT: {
				float u = 1.0f - t;
				return 1.0f - u * u * u * u * u;
			}
			case EASE_IN_OUT_SINE:
				return -((float) Math.cos(PI * t) - 1.0f) / 2.0f;
			default:
				return t;
		}
	}

	@Override
	public void springStep(float pos, float vel, float target, float stiffness, float damping, float dt, float[] out) {
		if (!(dt > 0.0f)) {
			out[0] = pos;
			out[1] = vel;
			return;
		}
		int steps = Math.max(1, Math.min(1000, (int) Math.ceil(dt / MAX_SPRING_STEP)));
		float h = dt / steps;
		for (int i = 0; i < steps; i++) {
			float accel = stiffness * (target - pos) - damping * vel;
			vel += accel * h;
			pos += vel * h;
		}
		out[0] = pos;
		out[1] = vel;
	}

	@Override
	public float sampleTrack(float[] times, float[] values, int[] easing, int count, float t) {
		int n = Math.min(count, Math.min(times.length, values.length));
		if (n <= 0) {
			return 0.0f;
		}
		if (n == 1 || t <= times[0]) {
			return values[0];
		}
		if (t >= times[n - 1]) {
			return values[n - 1];
		}
		int i = 0;
		while (i + 2 < n && t >= times[i + 1]) {
			i++;
		}
		float span = times[i + 1] - times[i];
		float local = span > 0.0f ? (t - times[i]) / span : 1.0f;
		int kind = (easing != null && i < easing.length && i < n - 1) ? easing[i] : 0;
		float e = ease(kind, local);
		return values[i] + (values[i + 1] - values[i]) * e;
	}

	@Override
	public void placeTransform(int style, float progress, float dx, float dy, float dz, float[] out) {
		float p = clamp01(progress);
		float sx = 1, sy = 1, sz = 1, ox = 0, oy = 0, oz = 0;
		switch (style) {
			case PLACE_SETTLE: {
				float s = 0.6f + 0.4f * ease(EASE_OUT_BACK, p);
				float k = 0.35f * (1.0f - ease(EASE_OUT_CUBIC, p));
				sx = s;
				sy = s;
				sz = s;
				ox = -dx * k;
				oy = -dy * k;
				oz = -dz * k;
				break;
			}
			case PLACE_POP: {
				float s = ease(EASE_OUT_BACK, p);
				sx = s;
				sy = s;
				sz = s;
				break;
			}
			case PLACE_SLIDE: {
				float k = 1.0f - ease(EASE_OUT_CUBIC, p);
				ox = dx * k;
				oy = dy * k;
				oz = dz * k;
				break;
			}
			case PLACE_DROP: {
				final float fallEnd = 0.7f;
				if (p < fallEnd) {
					float f = p / fallEnd;
					oy = 0.5f * (1.0f - f * f);
				} else {
					float q = (p - fallEnd) / (1.0f - fallEnd);
					float squash = 0.08f * (float) Math.sin(PI * q);
					sx = 1.0f + squash * 0.5f;
					sy = 1.0f - squash;
					sz = 1.0f + squash * 0.5f;
				}
				break;
			}
			default:
				break;
		}
		out[0] = sx;
		out[1] = sy;
		out[2] = sz;
		out[3] = ox;
		out[4] = oy;
		out[5] = oz;
		out[6] = 1.0f;
	}
}
