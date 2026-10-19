package io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/** Pure logic of the subtle camera effects: strafe roll, landing dip, sprint FOV ease and view-bob smoothing. */
public final class CameraFeel {
	/** Hard cap of the strafe roll in degrees, whatever the intensity. */
	public static final float MAX_ROLL_DEGREES = 1.5f;
	/** Hard cap of the landing dip in degrees. */
	public static final float MAX_DIP_DEGREES = 3.0f;
	/** Strafe speed (blocks/tick) that counts as full strafing. */
	public static final float FULL_STRAFE_SPEED = 0.22f;

	private final Spring roll;
	private final Spring dip;
	private final Spring sprint;
	private float smoothedBob;

	// Inputs set from the tick.
	private float strafe; // -1..1, positive = moving right
	private boolean sprinting;

	public CameraFeel(AnimMath math) {
		this.roll = new Spring(math);
		this.dip = new Spring(math);
		this.sprint = new Spring(math);
	}

	public void setInput(float strafeBlocksPerTick, boolean sprinting) {
		this.strafe = Math.max(-1f, Math.min(1f, strafeBlocksPerTick / FULL_STRAFE_SPEED));
		this.sprinting = sprinting;
	}

	/** Landing with a downward speed in blocks/tick (positive number = faster fall). */
	public void land(float fallSpeed, float intensity) {
		float impact = Math.max(0f, Math.min(1f, (fallSpeed - 0.15f) / 0.85f));
		dip.vel += 46f * impact * intensity;
	}

	public void update(float dt, float speed, boolean rollOn, float rollDeg, boolean dipOn, boolean sprintOn, float sprintFov) {
		float rollTarget = rollOn ? strafe * rollDeg : 0f;
		roll.step(rollTarget, 140f, 17f, speed, dt);
		dip.step(0f, 260f, 22f, speed, dt);
		sprint.step(sprintOn && sprinting ? sprintFov : 0f, 55f, 13f, speed, dt);
		if (!dipOn) {
			dip.snap(0f);
		}
	}

	/** Roll in degrees; positive leans the view to the right (clockwise), i.e. into a rightward strafe. */
	public float rollDegrees() {
		return Math.max(-MAX_ROLL_DEGREES, Math.min(MAX_ROLL_DEGREES, roll.pos));
	}

	/** Extra pitch in degrees (positive = looking down). */
	public float dipDegrees() {
		return Math.max(0f, Math.min(MAX_DIP_DEGREES, dip.pos));
	}

	/** Extra FOV multiplier from the sprint ease (1 = none). */
	public float fovMultiplier() {
		return 1f + sprint.pos;
	}

	/** Low-passes the vanilla view-bob amplitude so starting and stopping does not snap. */
	public float smoothBob(float rawBob, float dt, float speed, float amount) {
		if (amount <= 0f) {
			smoothedBob = rawBob;
			return rawBob;
		}
		float rate = 18f * Math.max(0.05f, speed) / (0.25f + amount * 3f);
		float k = 1f - (float) Math.exp(-rate * dt);
		smoothedBob += (rawBob - smoothedBob) * k;
		return smoothedBob;
	}

	public void reset() {
		roll.snap(0);
		dip.snap(0);
		sprint.snap(0);
		smoothedBob = 0;
	}
}
