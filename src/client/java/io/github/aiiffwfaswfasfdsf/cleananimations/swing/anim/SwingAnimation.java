package io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim;

import java.util.List;

/**
 * All animation data of one tool kind.
 *
 * @param attack  main swing clip (also used as combo variant 0 when {@code combo} is empty)
 * @param combo   optional list of swing variants cycled by consecutive attacks
 * @param mining  optional looping clip played while a block is being broken
 * @param shake   optional impact shake, or null
 */
public record SwingAnimation(Clip attack, List<Clip> combo, Clip mining, Shake shake) {

	/** Screen-space shake that starts at normalised swing progress {@code at}. */
	public record Shake(float at, float amplitude, float ticks) {
	}

	public int comboSize() {
		return combo.isEmpty() ? 1 : combo.size();
	}

	/** The clip for a combo step (wraps around). */
	public Clip clip(int step) {
		if (combo.isEmpty()) {
			return attack;
		}
		return combo.get(Math.floorMod(step, combo.size()));
	}
}
