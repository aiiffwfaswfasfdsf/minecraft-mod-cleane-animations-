package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import java.util.Objects;

/**
 * Pure freshness rules of precise placement: while use is held, a block is placed immediately whenever the targeted
 * block face differs from the face we last tried to place on. The same face is never used twice in a row
 * (no double placing), and releasing use forgets the memory.
 */
public final class PreciseTargeting {
	/** A targeted block face. {@code face} is a {@code Direction} ordinal. */
	public record Target(long pos, int face) {
	}

	private Target last;

	/**
	 * Decides what to do this tick.
	 *
	 * @param useHeld whether the use key is held
	 * @param target  currently targeted face, or {@code null} for a miss
	 * @return {@link Action#PLACE_NOW} to skip the vanilla delay, {@link Action#HOLD_OFF} to keep vanilla from repeating
	 * on the same face, {@link Action#NONE} to leave vanilla alone
	 */
	public Action update(boolean useHeld, Target target) {
		if (!useHeld) {
			last = null;
			return Action.NONE;
		}
		if (target == null) {
			// aiming into the void for a moment (walking backwards) keeps the memory
			return Action.NONE;
		}
		if (Objects.equals(target, last)) {
			return Action.HOLD_OFF;
		}
		last = target;
		return Action.PLACE_NOW;
	}

	public Target last() {
		return last;
	}

	public void reset() {
		last = null;
	}

	public enum Action {
		NONE, PLACE_NOW, HOLD_OFF
	}
}
