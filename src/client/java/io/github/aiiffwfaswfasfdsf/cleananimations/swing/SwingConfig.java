package io.github.aiiffwfaswfasfdsf.cleananimations.swing;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.ConfigSection;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.Slider;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.Toggle;

/**
 * Config section of the swing module (translation keys: {@code cleananimations.config.swing.<field>}).
 * Everything is additionally scaled by the core master switch, speed and intensity.
 */
public class SwingConfig implements ConfigSection {
	@Toggle
	public boolean enabled = true;

	@Slider(min = 0.0, max = 2.0, step = 0.05)
	public float intensity = 1.0f;

	@Slider(min = 0.5, max = 2.0, step = 0.05)
	public float speed = 1.0f;

	@Toggle
	public boolean swordSwing = true;
	@Toggle
	public boolean axeSwing = true;
	@Toggle
	public boolean pickaxeSwing = true;
	@Toggle
	public boolean shovelSwing = true;
	@Toggle
	public boolean hoeSwing = true;
	@Toggle
	public boolean spearSwing = true;
	@Toggle
	public boolean maceSwing = true;
	@Toggle
	public boolean shearsSwing = true;
	@Toggle
	public boolean brushSwing = true;
	@Toggle
	public boolean blockSwing = true;
	@Toggle
	public boolean genericSwing = true;

	@Toggle
	public boolean comboEnabled = true;
	@Toggle
	public boolean miningAnimation = true;
	@Toggle
	public boolean equipAnimation = true;
	@Toggle
	public boolean idleBreathing = true;
	@Toggle
	public boolean idleSway = true;

	@Slider(min = 0.0, max = 2.0, step = 0.05)
	public float inertiaStrength = 1.0f;

	@Toggle
	public boolean inspectEnabled = true;

	/** When true, items without a dedicated animation (blocks, unknown items) keep the vanilla swing. */
	@Toggle
	public boolean keepVanillaSwingOnUnknownItems = false;

	/** Whether the custom swing of {@code kind} is switched on (vanilla swing is used when false). */
	public boolean swingEnabled(ToolKind kind) {
		if (!kind.customSwing) {
			return false;
		}
		switch (kind) {
			case SWORD:
				return swordSwing;
			case AXE:
				return axeSwing;
			case PICKAXE:
				return pickaxeSwing;
			case SHOVEL:
				return shovelSwing;
			case HOE:
				return hoeSwing;
			case SPEAR:
				return spearSwing;
			case MACE:
				return maceSwing;
			case SHEARS:
				return shearsSwing;
			case BRUSH:
				return brushSwing;
			case BLOCK:
				return blockSwing && !keepVanillaSwingOnUnknownItems;
			case GENERIC:
				return genericSwing && !keepVanillaSwingOnUnknownItems;
			default:
				return false;
		}
	}
}
