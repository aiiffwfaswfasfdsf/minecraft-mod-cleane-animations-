package io.github.aiiffwfaswfasfdsf.cleananimations.core.math;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;

class NativeParityTest {
	private static final float EPS = 1e-4f;

	private static NativeAnimMath nativeOrSkip() {
		try {
			return NativeLoader.tryLoadNative();
		} catch (Throwable t) {
			Assumptions.abort("native library unavailable: " + t);
			return null;
		}
	}

	@Test
	void easeParity() {
		NativeAnimMath n = nativeOrSkip();
		JavaAnimMath j = new JavaAnimMath();
		for (int kind = 0; kind <= 7; kind++) {
			for (int i = -10; i <= 1010; i++) {
				float t = i / 1000.0f;
				assertEquals(j.ease(kind, t), n.ease(kind, t), EPS, "kind " + kind + " t " + t);
			}
		}
	}

	@Test
	void springParity() {
		NativeAnimMath n = nativeOrSkip();
		JavaAnimMath j = new JavaAnimMath();
		float[] a = new float[2];
		float[] b = new float[2];
		for (float dt : new float[] {0.001f, 1f / 60f, 0.05f, 0.2f}) {
			for (float pos = -2; pos <= 2; pos += 0.5f) {
				for (float vel = -3; vel <= 3; vel += 1.5f) {
					j.springStep(pos, vel, 1.0f, 120f, 14f, dt, a);
					n.springStep(pos, vel, 1.0f, 120f, 14f, dt, b);
					assertEquals(a[0], b[0], EPS);
					assertEquals(a[1], b[1], EPS);
				}
			}
		}
	}

	@Test
	void trackParity() {
		NativeAnimMath n = nativeOrSkip();
		JavaAnimMath j = new JavaAnimMath();
		float[] times = {0f, 0.2f, 0.5f, 1.0f};
		float[] values = {0f, 1.2f, -0.4f, 1f};
		int[] easing = {3, 1, 6};
		for (int i = -20; i <= 1200; i++) {
			float t = i / 1000.0f;
			assertEquals(j.sampleTrack(times, values, easing, 4, t), n.sampleTrack(times, values, easing, 4, t), EPS, "t " + t);
		}
	}

	@Test
	void placeParity() {
		NativeAnimMath n = nativeOrSkip();
		JavaAnimMath j = new JavaAnimMath();
		float[] a = new float[AnimMath.PLACE_TRANSFORM_SIZE];
		float[] b = new float[AnimMath.PLACE_TRANSFORM_SIZE];
		float[][] dirs = {{0, 1, 0}, {1, 0, 0}, {0, 0, -1}, {0, -1, 0}};
		for (int style = 0; style <= 4; style++) {
			for (float[] d : dirs) {
				for (int i = 0; i <= 100; i++) {
					float p = i / 100f;
					j.placeTransform(style, p, d[0], d[1], d[2], a);
					n.placeTransform(style, p, d[0], d[1], d[2], b);
					for (int k = 0; k < a.length; k++) {
						assertEquals(a[k], b[k], EPS, "style " + style + " p " + p + " k " + k);
					}
				}
			}
		}
	}
}
