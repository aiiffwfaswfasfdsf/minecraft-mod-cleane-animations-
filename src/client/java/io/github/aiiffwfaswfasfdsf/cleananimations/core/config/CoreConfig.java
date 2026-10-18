package io.github.aiiffwfaswfasfdsf.cleananimations.core.config;

/** Settings shared by every feature module. */
public class CoreConfig implements ConfigSection {
	@Toggle
	public boolean enabled = true;

	@Slider(min = 0.5, max = 2.0, step = 0.05)
	public float animationSpeed = 1.0f;

	@Slider(min = 0.0, max = 2.0, step = 0.05)
	public float intensity = 1.0f;
}
