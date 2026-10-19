package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import static org.junit.jupiter.api.Assertions.*;

import io.github.aiiffwfaswfasfdsf.cleananimations.placement.PreciseTargeting.Action;
import io.github.aiiffwfaswfasfdsf.cleananimations.placement.PreciseTargeting.Target;
import org.junit.jupiter.api.Test;

class PreciseTargetingTest {
	private static final Target A = new Target(1L, 1);
	private static final Target B = new Target(2L, 1);
	private static final Target A_SIDE = new Target(1L, 2);

	@Test
	void firstTargetPlacesImmediately() {
		assertEquals(Action.PLACE_NOW, new PreciseTargeting().update(true, A));
	}

	@Test
	void sameFaceIsNeverPlacedTwice() {
		PreciseTargeting t = new PreciseTargeting();
		assertEquals(Action.PLACE_NOW, t.update(true, A));
		for (int i = 0; i < 10; i++) {
			assertEquals(Action.HOLD_OFF, t.update(true, A));
		}
	}

	@Test
	void newBlockOrNewFaceIsFresh() {
		PreciseTargeting t = new PreciseTargeting();
		t.update(true, A);
		assertEquals(Action.PLACE_NOW, t.update(true, B));
		assertEquals(Action.PLACE_NOW, t.update(true, A_SIDE));
		assertEquals(Action.PLACE_NOW, t.update(true, A));
	}

	@Test
	void releasingUseForgetsTheMemory() {
		PreciseTargeting t = new PreciseTargeting();
		t.update(true, A);
		assertEquals(Action.NONE, t.update(false, A));
		assertNull(t.last());
		assertEquals(Action.PLACE_NOW, t.update(true, A));
	}

	@Test
	void briefMissWhileWalkingBackwardsKeepsTheMemory() {
		PreciseTargeting t = new PreciseTargeting();
		t.update(true, A);
		assertEquals(Action.NONE, t.update(true, null));
		assertEquals(Action.HOLD_OFF, t.update(true, A));
		assertEquals(Action.PLACE_NOW, t.update(true, B));
	}

	@Test
	void straightLineWalkingBackwardsPlacesOnEveryNewBlock() {
		PreciseTargeting t = new PreciseTargeting();
		int placed = 0;
		for (long i = 0; i < 8; i++) {
			// every step back the crosshair rests on the side face of the block placed one step earlier
			if (t.update(true, new Target(100 + i, 3)) == Action.PLACE_NOW) {
				placed++;
			}
			// repeated ticks on that same face do nothing
			assertEquals(Action.HOLD_OFF, t.update(true, new Target(100 + i, 3)));
		}
		assertEquals(8, placed);
	}

	@Test
	void resetClears() {
		PreciseTargeting t = new PreciseTargeting();
		t.update(true, A);
		t.reset();
		assertEquals(Action.PLACE_NOW, t.update(true, A));
	}
}
