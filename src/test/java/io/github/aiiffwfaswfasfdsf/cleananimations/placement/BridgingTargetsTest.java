package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import static org.junit.jupiter.api.Assertions.*;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BridgingTargetsTest {
	/** Tiny test world: a set of solid cells, everything else is replaceable air. */
	private static final class TestWorld implements BridgingTargets.World {
		final Set<String> solid = new HashSet<>();

		TestWorld fill(int x1, int y1, int z1, int x2, int y2, int z2) {
			for (int x = x1; x <= x2; x++) {
				for (int y = y1; y <= y2; y++) {
					for (int z = z1; z <= z2; z++) {
						solid.add(x + "," + y + "," + z);
					}
				}
			}
			return this;
		}

		@Override
		public boolean solid(int x, int y, int z) {
			return solid.contains(x + "," + y + "," + z);
		}

		@Override
		public boolean replaceable(int x, int y, int z) {
			return !solid(x, y, z);
		}
	}

	private static BridgingTargets.Target aim(TestWorld w, double px, double py, double pz, double tx, double ty, double tz,
											  int feetY) {
		double ex = px, ey = py + 1.62, ez = pz;
		return BridgingTargets.select(ex, ey, ez, tx - ex, ty - ey, tz - ez, 4.5, feetY, w);
	}

	@Test
	void bridgesOutwardFromThePlatformEdge() {
		// platform x 0..3 at y 63, player stands on its east edge looking down at the empty cell (4,63,0)
		TestWorld w = new TestWorld().fill(0, 63, -3, 3, 63, 3);
		BridgingTargets.Target t = aim(w, 3.5, 64, 0.5, 4.5, 63.5, 0.5, 64);
		assertNotNull(t);
		assertEquals(4, t.cx());
		assertEquals(63, t.cy());
		assertEquals(0, t.cz());
		assertEquals(3, t.sx());
		assertEquals(63, t.sy());
		assertEquals(5, t.face(), "clicks the EAST face of the platform block");
		assertEquals(4.0, t.hx(), 1e-9);
	}

	@Test
	void nothingToDoWhenThereIsNoSupportNearby() {
		TestWorld w = new TestWorld();
		assertNull(aim(w, 0.5, 64, 0.5, 2.5, 60.5, 0.5, 64));
	}

	@Test
	void doesNotBuildInMidAirAboveFeetLevel() {
		TestWorld w = new TestWorld().fill(0, 63, -3, 3, 63, 3).fill(4, 63, 0, 4, 66, 0);
		// aiming at the sky next to a pillar: layers above the feet are never considered
		BridgingTargets.Target t = aim(w, 3.5, 64, 0.5, 3.5, 68.5, 0.5, 64);
		assertNull(t);
	}

	@Test
	void stairBridgingUpwardsUsesTheStepInFront() {
		// player stands on the platform edge, aims at the cell one block ahead at feet level; support is the block below it
		TestWorld w = new TestWorld().fill(0, 63, -3, 4, 63, 3);
		BridgingTargets.Target t = aim(w, 2.5, 64, 0.5, 4.5, 64.5, 0.5, 64);
		assertNotNull(t);
		assertEquals(64, t.cy());
		assertEquals(1, t.face(), "supported from below");
		assertEquals(63, t.sy());
	}

	@Test
	void prefersTheCellClosestToTheRay() {
		TestWorld w = new TestWorld().fill(0, 63, -3, 3, 63, 3);
		BridgingTargets.Target left = aim(w, 3.5, 64, 0.5, 4.5, 63.5, -1.5, 64);
		BridgingTargets.Target right = aim(w, 3.5, 64, 0.5, 4.5, 63.5, 2.5, 64);
		assertNotNull(left);
		assertNotNull(right);
		assertEquals(-2, left.cz());
		assertEquals(2, right.cz());
	}

	@Test
	void respectsReach() {
		TestWorld w = new TestWorld().fill(0, 63, -3, 3, 63, 3);
		// a gap cell far out of reach is not selected
		assertNull(BridgingTargets.select(3.5, 65.62, 0.5, 8, -2, 0, 4.5, 64, w));
	}

	@Test
	void degenerateLookVectorIsRejected() {
		assertNull(BridgingTargets.select(0, 0, 0, 0, 0, 0, 4.5, 0, new TestWorld()));
	}
}
