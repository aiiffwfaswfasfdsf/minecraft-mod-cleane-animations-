package io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;

/**
 * Pure logic of the smooth hotbar: a gliding selection position, a small lift for the selected slot and
 * a per-slot bounce that is kicked when an item is picked up, used or a block is placed.
 */
public final class HotbarAnimator {
	public static final int SLOTS = 9;
	/** Maximum lift of the selected slot in GUI pixels at intensity 1. */
	public static final float LIFT_PIXELS = 2.0f;

	private final AnimMath math;
	private final Spring selection;
	private final Spring[] bounce = new Spring[SLOTS];
	private final float[] lastCount = new float[SLOTS];
	private final Object[] lastItem = new Object[SLOTS];
	private boolean initialised;
	private int target;

	public HotbarAnimator(AnimMath math) {
		this.math = math;
		this.selection = new Spring(math);
		for (int i = 0; i < SLOTS; i++) {
			bounce[i] = new Spring(math);
		}
	}

	/** Advances all springs. {@code bouncy} switches the glide from well damped to a visible overshoot. */
	public void update(float dt, int selectedSlot, float speed, boolean bouncy) {
		if (!initialised) {
			selection.snap(selectedSlot);
			initialised = true;
		}
		target = selectedSlot;
		// Going around the bar (8 -> 0 or 0 -> 8): do not sweep over every slot, jump and let the bounce sell it.
		if (Math.abs(selectedSlot - selection.pos) > SLOTS / 2f) {
			selection.snap(selectedSlot);
			kick(selectedSlot, 1f);
		}
		float damping = bouncy ? 24f : 52f;
		selection.step(selectedSlot, 700f, damping, speed, dt);
		for (Spring b : bounce) {
			b.step(0f, 420f, 17f, speed, dt);
		}
	}

	/** Pushes a slot upwards; the spring brings it back with a small overshoot. */
	public void kick(int slot, float strength) {
		if (slot >= 0 && slot < SLOTS) {
			bounce[slot].vel -= 95f * strength;
		}
	}

	/**
	 * Compares the inventory contents with the previous tick and kicks changed slots.
	 * {@code item} identifies the stack's item (null or empty = {@code null}), {@code count} its size.
	 *
	 * @return true when a slot was kicked
	 */
	public boolean observeSlot(int slot, Object item, int count, float strength) {
		if (slot < 0 || slot >= SLOTS) {
			return false;
		}
		boolean kicked = false;
		if (initialised) {
			boolean sameItem = item == lastItem[slot];
			if (item != null && (!sameItem || count > lastCount[slot])) {
				kick(slot, strength);
				kicked = true;
			} else if (item != null && count < lastCount[slot]) {
				kick(slot, strength * 0.7f);
				kicked = true;
			}
		}
		lastItem[slot] = item;
		lastCount[slot] = count;
		return kicked;
	}

	/** Drops every running slot bounce (used when the bounce is disabled). */
	public void clearBounces() {
		for (Spring b : bounce) {
			b.snap(0f);
		}
	}

	/** Called when the world/player changes so a fresh inventory does not make every slot bounce. */
	public void reset() {
		initialised = false;
	}

	public boolean isInitialised() {
		return initialised;
	}

	/** Position of the selection frame in slot units (fractional while gliding). */
	public float selectionPosition() {
		return selection.pos;
	}

	/** Horizontal selection offset from the slot it is supposed to be on, in GUI pixels. */
	public float selectionOffsetPixels() {
		return (selection.pos - target) * 20f;
	}

	/** How strongly {@code slot} is "selected" in [0,1] (1 when the glide is exactly on it). */
	public float selectedness(int slot) {
		return Math.max(0f, 1f - Math.abs(selection.pos - slot));
	}

	/** Vertical item offset for {@code slot} in GUI pixels (negative = up). */
	public float slotOffsetY(int slot, float intensity) {
		float lift = -LIFT_PIXELS * selectedness(slot) * intensity;
		return lift + bounce[slot].pos * intensity;
	}

	public AnimMath math() {
		return math;
	}
}
