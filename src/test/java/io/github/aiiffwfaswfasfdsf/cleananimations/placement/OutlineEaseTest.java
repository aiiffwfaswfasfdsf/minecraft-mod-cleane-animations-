package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class OutlineEaseTest {
	@Test
	void firstUpdateSnaps() {
		OutlineEase e = new OutlineEase();
		assertTrue(e.update(5, 6, 7, 0.016, 1));
		assertEquals(5, e.x());
		assertEquals(7, e.z());
	}

	@Test
	void glidesTowardsTargetWithoutOvershoot() {
		OutlineEase e = new OutlineEase();
		e.update(0, 0, 0, 0.016, 1);
		double prev = 0;
		for (int i = 0; i < 120; i++) {
			assertFalse(e.update(3, 0, 0, 0.016, 1));
			assertTrue(e.x() >= prev - 1e-12);
			assertTrue(e.x() <= 3 + 1e-9);
			prev = e.x();
		}
		assertEquals(3, e.x(), 1e-3);
	}

	@Test
	void firstFrameAfterTargetChangeMovesOnlyPartOfTheWay() {
		OutlineEase e = new OutlineEase();
		e.update(0, 0, 0, 0.016, 1);
		e.update(1, 0, 0, 0.016, 1);
		assertTrue(e.x() > 0.05 && e.x() < 0.9, "x=" + e.x());
	}

	@Test
	void farJumpsAndStaleUpdatesSnap() {
		OutlineEase e = new OutlineEase();
		e.update(0, 0, 0, 0.016, 1);
		assertTrue(e.update(100, 0, 0, 0.016, 1));
		assertEquals(100, e.x());
		e.update(101, 0, 0, 0.016, 1);
		assertTrue(e.update(102, 0, 0, 1.0, 1), "stale update snaps");
		assertEquals(102, e.x());
	}

	@Test
	void frameRateIndependent() {
		OutlineEase a = new OutlineEase(), b = new OutlineEase();
		a.update(0, 0, 0, 0.01, 1);
		b.update(0, 0, 0, 0.01, 1);
		for (int i = 0; i < 10; i++) {
			a.update(2, 0, 0, 0.01, 1);
		}
		b.update(2, 0, 0, 0.1, 1);
		assertEquals(a.x(), b.x(), 1e-9);
	}
}
