package io.github.aiiffwfaswfasfdsf.cleananimations.core.math;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class EasingTest {
	private final AnimMath math = new JavaAnimMath();

	@Test
	void endpointsAndClamping() {
		for (int kind = 0; kind <= 6; kind++) {
			assertEquals(0f, math.ease(kind, 0f), 1e-5f, "kind " + kind);
			assertEquals(1f, math.ease(kind, 1f), 1e-5f, "kind " + kind);
			assertEquals(math.ease(kind, 0f), math.ease(kind, -5f));
			assertEquals(math.ease(kind, 1f), math.ease(kind, 5f));
		}
	}

	@Test
	void backOvershootsAndMonotoneOutCubic() {
		assertTrue(math.ease(AnimMath.EASE_OUT_BACK, 0.8f) > 1f);
		float prev = 0;
		for (int i = 1; i <= 100; i++) {
			float v = math.ease(AnimMath.EASE_OUT_CUBIC, i / 100f);
			assertTrue(v >= prev);
			prev = v;
		}
	}

	@Test
	void springConverges() {
		float[] s = {0, 0};
		for (int i = 0; i < 600; i++) {
			math.springStep(s[0], s[1], 1f, 120f, 18f, 1f / 60f, s);
		}
		assertEquals(1f, s[0], 1e-3f);
		assertEquals(0f, s[1], 1e-3f);
	}

	@Test
	void trackClampsAndInterpolates() {
		float[] t = {0, 1, 2};
		float[] v = {0, 10, 0};
		int[] e = {0, 0};
		assertEquals(0f, math.sampleTrack(t, v, e, 3, -1f));
		assertEquals(0f, math.sampleTrack(t, v, e, 3, 9f));
		assertEquals(5f, math.sampleTrack(t, v, e, 3, 0.5f), 1e-5f);
		assertEquals(5f, math.sampleTrack(t, v, e, 3, 1.5f), 1e-5f);
	}

	@Test
	void placeEndsAtIdentity() {
		float[] out = new float[AnimMath.PLACE_TRANSFORM_SIZE];
		for (int style = 0; style <= 3; style++) {
			math.placeTransform(style, 1f, 0, 1, 0, out);
			assertArrayEquals(new float[] {1, 1, 1, 0, 0, 0, 1}, out, 1e-4f, "style " + style);
		}
	}

	@Test
	void holderDefaultsToJava() {
		assertNotNull(AnimMath.get());
	}
}
