package io.github.aiiffwfaswfasfdsf.cleananimations.hud;

import static org.junit.jupiter.api.Assertions.*;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.JavaAnimMath;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.CameraFeel;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.CrosshairAnimator;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.HotbarAnimator;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.ScreenOpenAnimator;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.ZoomController;
import org.junit.jupiter.api.Test;

class HudAnimTest {
	private static final AnimMath MATH = new JavaAnimMath();
	private static final float DT = 1f / 60f;

	@Test
	void hotbarGlidesAndSettlesOnTarget() {
		HotbarAnimator h = new HotbarAnimator(MATH);
		h.update(DT, 0, 1f, false);
		h.update(DT, 3, 1f, false);
		float p = h.selectionPosition();
		assertTrue(p > 0f && p < 3f, "mid-glide position " + p);
		for (int i = 0; i < 240; i++) {
			h.update(DT, 3, 1f, false);
		}
		assertEquals(3f, h.selectionPosition(), 1e-2f);
		assertEquals(1f, h.selectedness(3), 1e-2f);
		assertEquals(0f, h.selectionOffsetPixels(), 0.3f);
	}

	@Test
	void hotbarNeverSweepsOverWholeBarWhenWrapping() {
		HotbarAnimator h = new HotbarAnimator(MATH);
		h.update(DT, 8, 1f, false);
		h.update(DT, 0, 1f, false);
		assertEquals(0f, h.selectionPosition(), 0.5f);
	}

	@Test
	void bouncyOvershootsAndPlainDoesNot() {
		float plainMax = maxPosition(false);
		float bouncyMax = maxPosition(true);
		assertTrue(plainMax <= 4.05f, "plain should hardly overshoot: " + plainMax);
		assertTrue(bouncyMax > 4.05f, "bouncy should overshoot: " + bouncyMax);
	}

	private static float maxPosition(boolean bouncy) {
		HotbarAnimator h = new HotbarAnimator(MATH);
		h.update(DT, 0, 1f, bouncy);
		float max = 0;
		for (int i = 0; i < 200; i++) {
			h.update(DT, 4, 1f, bouncy);
			max = Math.max(max, h.selectionPosition());
		}
		return max;
	}

	@Test
	void slotLiftAndBounce() {
		HotbarAnimator h = new HotbarAnimator(MATH);
		h.update(DT, 2, 1f, false);
		assertEquals(-HotbarAnimator.LIFT_PIXELS, h.slotOffsetY(2, 1f), 1e-3f);
		assertEquals(0f, h.slotOffsetY(5, 1f), 1e-3f);
		assertEquals(0f, h.slotOffsetY(2, 0f), 1e-3f, "intensity 0 disables motion");
		h.kick(5, 1f);
		float min = 0;
		for (int i = 0; i < 20; i++) {
			h.update(DT, 2, 1f, false);
			min = Math.min(min, h.slotOffsetY(5, 1f));
		}
		assertTrue(min < -1f, "bounce rises: " + min);
		for (int i = 0; i < 300; i++) {
			h.update(DT, 2, 1f, false);
		}
		assertEquals(0f, h.slotOffsetY(5, 1f), 1e-2f);
	}

	@Test
	void observeSlotKicksOnPickupUseAndIgnoresFirstSnapshot() {
		HotbarAnimator h = new HotbarAnimator(MATH);
		Object stone = new Object();
		assertFalse(h.observeSlot(0, stone, 4, 1f), "before init nothing bounces");
		h.update(DT, 0, 1f, false);
		assertFalse(h.observeSlot(0, stone, 4, 1f), "unchanged");
		assertTrue(h.observeSlot(0, stone, 5, 1f), "pickup");
		assertTrue(h.observeSlot(0, stone, 4, 1f), "use");
		assertFalse(h.observeSlot(0, null, 0, 1f), "emptied slot does not bounce");
		assertTrue(h.observeSlot(0, new Object(), 1, 1f), "new item");
	}

	@Test
	void zoomEasesInAndOutAndRespectsLevel() {
		ZoomController z = new ZoomController(MATH);
		z.setLevel(4f, 8f);
		assertEquals(1f, z.fovMultiplier(), 1e-6f);
		assertFalse(z.isActive());
		z.setHeld(true);
		z.update(DT, 1f, 0.2f, 8f);
		float first = z.fovMultiplier();
		assertTrue(first < 1f && first > 0.9f, "starts gently: " + first);
		for (int i = 0; i < 60; i++) {
			z.update(DT, 1f, 0.2f, 8f);
		}
		assertEquals(0.25f, z.fovMultiplier(), 1e-3f);
		z.setHeld(false);
		float prev = z.fovMultiplier();
		for (int i = 0; i < 60; i++) {
			z.update(DT, 1f, 0.2f, 8f);
			assertTrue(z.fovMultiplier() >= prev - 1e-6f, "monotone release");
			prev = z.fovMultiplier();
		}
		assertEquals(1f, z.fovMultiplier(), 1e-4f);
		assertFalse(z.isActive());
	}

	@Test
	void zoomScrollClampsToRange() {
		ZoomController z = new ZoomController(MATH);
		z.setLevel(3f, 6f);
		z.scroll(10, 6f);
		z.scroll(10, 6f);
		assertEquals(6f, z.level(), 1e-4f);
		z.scroll(-100, 6f);
		z.scroll(-100, 6f);
		z.scroll(-100, 6f);
		assertEquals(ZoomController.MIN_LEVEL, z.level(), 1e-4f);
		float before = z.level();
		z.scroll(0, 6f);
		assertEquals(before, z.level());
	}

	@Test
	void cinematicSmoothingLagsButConverges() {
		ZoomController z = new ZoomController(MATH);
		double[] out = new double[2];
		z.smoothTurn(10, -10, 0.5f, out);
		assertEquals(5, out[0], 1e-6);
		assertEquals(-5, out[1], 1e-6);
		for (int i = 0; i < 30; i++) {
			z.smoothTurn(10, -10, 0.5f, out);
		}
		assertEquals(10, out[0], 1e-3);
	}

	@Test
	void crosshairPulsesAndSettles() {
		CrosshairAnimator c = new CrosshairAnimator(MATH);
		assertEquals(1f, c.scale(), 1e-6f);
		c.attack(1f);
		float max = 1f;
		for (int i = 0; i < 30; i++) {
			c.update(DT, 1f, 1f);
			max = Math.max(max, c.scale());
		}
		assertTrue(max > 1.05f && max < 1.6f, "pulse " + max);
		for (int i = 0; i < 300; i++) {
			c.update(DT, 1f, 1f);
		}
		assertEquals(1f, c.scale(), 1e-3f);
		c.use(1f);
		float min = 1f;
		for (int i = 0; i < 30; i++) {
			c.update(DT, 1f, 1f);
			min = Math.min(min, c.scale());
		}
		assertTrue(min < 0.97f);
	}

	@Test
	void crosshairMiningHoldsAndHitMarkerFades() {
		CrosshairAnimator c = new CrosshairAnimator(MATH);
		c.setMining(true);
		for (int i = 0; i < 120; i++) {
			c.update(DT, 1f, 1f);
		}
		assertEquals(1.10f, c.scale(), 0.01f);
		c.setMining(false);
		assertEquals(0f, c.hitMarkerAlpha());
		c.hit();
		assertEquals(1f, c.hitMarkerAlpha(), 1e-6f);
		float prev = 1f;
		for (int i = 0; i < 10; i++) {
			c.update(DT, 1f, 1f);
			assertTrue(c.hitMarkerAlpha() < prev);
			prev = c.hitMarkerAlpha();
		}
		for (int i = 0; i < 60; i++) {
			c.update(DT, 1f, 1f);
		}
		assertEquals(0f, c.hitMarkerAlpha());
	}

	@Test
	void strafeRollIsCappedAndLeansIntoMovement() {
		CameraFeel f = new CameraFeel(MATH);
		f.setInput(5f, false); // absurdly fast strafe is clamped
		for (int i = 0; i < 200; i++) {
			f.update(DT, 1f, true, 10f, true, true, 0.04f); // asks for 10 degrees, hard cap must hold
		}
		assertEquals(CameraFeel.MAX_ROLL_DEGREES, f.rollDegrees(), 1e-4f);
		f.setInput(-5f, false);
		for (int i = 0; i < 300; i++) {
			f.update(DT, 1f, true, 10f, true, true, 0.04f);
		}
		assertEquals(-CameraFeel.MAX_ROLL_DEGREES, f.rollDegrees(), 1e-4f);
		f.setInput(0f, false);
		for (int i = 0; i < 300; i++) {
			f.update(DT, 1f, true, 1f, true, true, 0.04f);
		}
		assertEquals(0f, f.rollDegrees(), 1e-2f);
	}

	@Test
	void landingDipNodsDownThenRecoversAndScalesWithImpact() {
		CameraFeel soft = new CameraFeel(MATH);
		CameraFeel hard = new CameraFeel(MATH);
		soft.land(0.3f, 1f);
		hard.land(0.9f, 1f);
		float softMax = 0;
		float hardMax = 0;
		for (int i = 0; i < 60; i++) {
			soft.update(DT, 1f, true, 1f, true, true, 0.03f);
			hard.update(DT, 1f, true, 1f, true, true, 0.03f);
			softMax = Math.max(softMax, soft.dipDegrees());
			hardMax = Math.max(hardMax, hard.dipDegrees());
		}
		assertTrue(softMax > 0f);
		assertTrue(hardMax > softMax);
		assertTrue(hardMax <= CameraFeel.MAX_DIP_DEGREES);
		for (int i = 0; i < 240; i++) {
			hard.update(DT, 1f, true, 1f, true, true, 0.03f);
		}
		assertEquals(0f, hard.dipDegrees(), 1e-2f);
		CameraFeel tiny = new CameraFeel(MATH);
		tiny.land(0.05f, 1f);
		tiny.update(DT, 1f, true, 1f, true, true, 0.03f);
		assertEquals(0f, tiny.dipDegrees(), 1e-4f, "no dip for a tiny fall");
	}

	@Test
	void sprintFovEasesInAndOut() {
		CameraFeel f = new CameraFeel(MATH);
		f.setInput(0f, true);
		f.update(DT, 1f, true, 1f, true, true, 0.04f);
		assertTrue(f.fovMultiplier() > 1f && f.fovMultiplier() < 1.01f, "gentle start " + f.fovMultiplier());
		for (int i = 0; i < 300; i++) {
			f.update(DT, 1f, true, 1f, true, true, 0.04f);
		}
		assertEquals(1.04f, f.fovMultiplier(), 2e-3f);
		f.setInput(0f, false);
		for (int i = 0; i < 300; i++) {
			f.update(DT, 1f, true, 1f, true, true, 0.04f);
		}
		assertEquals(1f, f.fovMultiplier(), 2e-3f);
	}

	@Test
	void bobSmoothingLagsAndPassesThroughWhenOff() {
		CameraFeel f = new CameraFeel(MATH);
		assertEquals(0.1f, f.smoothBob(0.1f, DT, 1f, 0f), 1e-6f);
		f.reset();
		float v = f.smoothBob(0.1f, DT, 1f, 1f);
		assertTrue(v > 0f && v < 0.1f);
		for (int i = 0; i < 300; i++) {
			v = f.smoothBob(0.1f, DT, 1f, 1f);
		}
		assertEquals(0.1f, v, 1e-3f);
	}

	@Test
	void screenOpenScalesFromStartToOneAndRestartsOnNewScreen() {
		ScreenOpenAnimator a = new ScreenOpenAnimator(MATH);
		Object s1 = new Object();
		float first = a.frame(s1, 0f, 1f, 1f);
		assertEquals(ScreenOpenAnimator.START_SCALE, first, 1e-4f);
		float prev = first;
		for (int i = 0; i < 9; i++) {
			float s = a.frame(s1, DT, 1f, 1f);
			assertTrue(s >= prev);
			prev = s;
		}
		assertEquals(1f, a.frame(s1, DT, 1f, 1f), 1e-4f);
		assertEquals(ScreenOpenAnimator.START_SCALE, a.frame(new Object(), 0f, 1f, 1f), 1e-4f);
		assertEquals(1f, a.frame(new Object(), 0f, 1f, 0f), 1e-6f, "intensity 0 = no scaling");
	}
}
