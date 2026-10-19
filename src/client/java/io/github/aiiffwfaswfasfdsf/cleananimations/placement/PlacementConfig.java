package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.Choice;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.ConfigSection;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.Slider;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.Toggle;

/**
 * Config section of the placement module (translation keys: {@code cleananimations.config.placement.<field>}).
 * Precise placement and bridging assist are OFF by default because some servers treat them as unfair advantages.
 */
public class PlacementConfig implements ConfigSection {
	/** Animation style of a freshly placed block. Matches the {@code PLACE_*} constants of AnimMath. */
	public enum Style {
		SETTLE, POP, SLIDE, DROP
	}

	@Toggle
	public boolean placeAnimation = true;

	@Choice
	public Style style = Style.SETTLE;

	/** Base duration of the placement animation before the speed multipliers are applied. */
	@Slider(min = 100, max = 500, step = 10)
	public float durationMs = 240f;

	@Slider(min = 0.0, max = 2.0, step = 0.05)
	public float intensity = 1.0f;

	@Slider(min = 0.5, max = 2.0, step = 0.05)
	public float speed = 1.0f;

	/** When on, only blocks the local player places are animated (not blocks placed by others or by the world). */
	@Toggle
	public boolean onlyOwnPlacement = true;

	@Toggle
	public boolean breakAnimation = true;

	@Toggle
	public boolean easedOutline = true;

	@Slider(min = 0.5, max = 2.0, step = 0.05)
	public float outlineSpeed = 1.0f;

	/** Place on every newly targeted block face immediately while use is held. Default off (server rules!). */
	@Toggle
	public boolean precisePlacement = false;

	/** Reach-around placement into the gap you aim at. Default off (server rules!). */
	@Toggle
	public boolean bridgingAssist = false;

	@Toggle
	public boolean bridgingIndicator = true;
}
