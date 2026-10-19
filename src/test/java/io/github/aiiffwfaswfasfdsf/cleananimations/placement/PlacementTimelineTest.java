package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import static org.junit.jupiter.api.Assertions.*;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.JavaAnimMath;
import org.junit.jupiter.api.Test;

class PlacementTimelineTest {
	@Test
	void progressRunsFromZeroToOneAndClamps() {
		PlacementTimeline t = new PlacementTimeline(10f, 4f);
		assertEquals(0f, t.progress(5f));
		assertEquals(0f, t.progress(10f));
		assertEquals(0.5f, t.progress(12f), 1e-6f);
		assertEquals(1f, t.progress(14f));
		assertEquals(1f, t.progress(99f));
	}

	@Test
	void hiddenOnlyDuringAnimation() {
		PlacementTimeline t = new PlacementTimeline(0f, 4f);
		assertTrue(t.shouldBeHidden(0f));
		assertTrue(t.shouldBeHidden(3.99f));
		assertFalse(t.shouldBeHidden(4f));
	}

	@Test
	void copyIsDrawnForHoldTicksAfterUnhide() {
		PlacementTimeline t = new PlacementTimeline(0f, 4f);
		assertTrue(t.isDrawing(2f));
		t.markUnhidden(5f);
		assertTrue(t.isUnhidden());
		assertFalse(t.shouldBeHidden(5f));
		assertTrue(t.isDrawing(5f + PlacementTimeline.HOLD_TICKS - 0.1f));
		assertFalse(t.isDrawing(5f + PlacementTimeline.HOLD_TICKS));
		assertTrue(t.isFinished(5f + PlacementTimeline.HOLD_TICKS));
		// a second markUnhidden does not extend the hold
		t.markUnhidden(6f);
		assertTrue(t.isFinished(5f + PlacementTimeline.HOLD_TICKS));
	}

	@Test
	void neverFinishedBeforeUnhidden() {
		PlacementTimeline t = new PlacementTimeline(0f, 4f);
		assertFalse(t.isFinished(1000f));
	}

	@Test
	void durationTicksFollowsSpeed() {
		assertEquals(5f, PlacementTimeline.durationTicks(250f, 1f), 1e-6f);
		assertEquals(2.5f, PlacementTimeline.durationTicks(250f, 2f), 1e-6f);
		assertTrue(PlacementTimeline.durationTicks(1f, 100f) >= 0.5f);
	}

	@Test
	void settleOvershootsAndEndsInsideTheBlock() {
		AnimMath math = new JavaAnimMath();
		float[] out = new float[AnimMath.PLACE_TRANSFORM_SIZE];
		float max = 0;
		for (int i = 0; i <= 100; i++) {
			PlacementTransform.compute(math, AnimMath.PLACE_SETTLE, i / 100f, 0, 1, 0, 1f, out);
			max = Math.max(max, out[0]);
		}
		assertTrue(max > 1.0f && max < 1.1f, "overshoot " + max);
		PlacementTransform.compute(math, AnimMath.PLACE_SETTLE, 1f, 0, 1, 0, 1f, out);
		assertTrue(out[0] <= PlacementTransform.MAX_FINAL_SCALE);
		assertEquals(0f, out[3], 1e-5f);
		assertEquals(0f, out[4], 1e-5f);
	}

	@Test
	void settleStartsOnTheOpenSideOfTheClickedFace() {
		AnimMath math = new JavaAnimMath();
		float[] out = new float[AnimMath.PLACE_TRANSFORM_SIZE];
		PlacementTransform.compute(math, AnimMath.PLACE_SETTLE, 0f, 0, 1, 0, 1f, out);
		assertTrue(out[4] > 0.1f, "starts above the floor block, offset " + out[4]);
		PlacementTransform.compute(math, AnimMath.PLACE_SLIDE, 0f, 1, 0, 0, 1f, out);
		assertTrue(out[3] > 0.5f, "slides in from +x, offset " + out[3]);
	}

	@Test
	void zeroIntensityMeansNoMotion() {
		AnimMath math = new JavaAnimMath();
		float[] out = new float[AnimMath.PLACE_TRANSFORM_SIZE];
		for (int style = 0; style < 4; style++) {
			PlacementTransform.compute(math, style, 0.3f, 0, 1, 0, 0f, out);
			assertEquals(1f, out[0], 1e-6f);
			assertEquals(0f, out[3], 1e-6f);
			assertEquals(0f, out[4], 1e-6f);
		}
	}
}
