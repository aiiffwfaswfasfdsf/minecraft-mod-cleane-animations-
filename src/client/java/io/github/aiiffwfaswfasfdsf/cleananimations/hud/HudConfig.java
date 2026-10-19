package io.github.aiiffwfaswfasfdsf.cleananimations.hud;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.ConfigSection;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.Slider;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.Toggle;

/**
 * Config section for the hud module (translation keys: {@code cleananimations.config.hud.<field>}).
 * Every feature has a toggle plus intensity/speed; the core master values are applied on top via the
 * {@code *Intensity()} / {@code *Speed()} helpers.
 */
public class HudConfig implements ConfigSection {
	@Toggle
	public boolean enabled = true;

	// --- Smooth hotbar ---
	@Toggle
	public boolean hotbar = true;
	@Slider(min = 0.0, max = 2.0, step = 0.05)
	public float hotbarIntensity = 1.0f;
	@Slider(min = 0.5, max = 2.0, step = 0.05)
	public float hotbarSpeed = 1.0f;
	@Toggle
	public boolean hotbarBouncy = false;
	@Toggle
	public boolean hotbarItemBounce = true;

	// --- Smooth zoom ---
	@Toggle
	public boolean zoom = true;
	@Slider(min = 1.5, max = 10.0, step = 0.25)
	public float zoomLevel = 3.0f;
	@Slider(min = 2.0, max = 10.0, step = 0.5)
	public float zoomMaxLevel = 8.0f;
	@Slider(min = 0.5, max = 2.0, step = 0.05)
	public float zoomSpeed = 1.0f;
	@Toggle
	public boolean zoomScroll = true;
	@Toggle
	public boolean zoomScaleSensitivity = true;
	@Toggle
	public boolean zoomCinematic = false;

	// --- Crosshair feedback ---
	@Toggle
	public boolean crosshair = true;
	@Slider(min = 0.0, max = 2.0, step = 0.05)
	public float crosshairIntensity = 1.0f;
	@Slider(min = 0.5, max = 2.0, step = 0.05)
	public float crosshairSpeed = 1.0f;
	@Toggle
	public boolean crosshairHitMarker = true;

	// --- Camera feel ---
	@Toggle
	public boolean cameraStrafeRoll = true;
	@Toggle
	public boolean cameraLandingDip = true;
	@Toggle
	public boolean cameraSprintFov = true;
	@Toggle
	public boolean cameraBobSmoothing = true;
	@Slider(min = 0.0, max = 2.0, step = 0.05)
	public float cameraIntensity = 1.0f;
	@Slider(min = 0.5, max = 2.0, step = 0.05)
	public float cameraSpeed = 1.0f;

	// --- Screen open ---
	@Toggle
	public boolean screenOpen = true;
	@Slider(min = 0.0, max = 2.0, step = 0.05)
	public float screenOpenIntensity = 1.0f;
	@Slider(min = 0.5, max = 2.0, step = 0.05)
	public float screenOpenSpeed = 1.0f;

	/** The active hud section. */
	public static HudConfig get() {
		return CleanConfig.get().hud;
	}

	/** True when the mod and the hud module are on. */
	public boolean active() {
		return CleanConfig.get().core.enabled && enabled;
	}

	public float masterIntensity() {
		return CleanConfig.get().core.intensity;
	}

	public float masterSpeed() {
		return CleanConfig.get().core.animationSpeed;
	}

	public boolean hotbarOn() {
		return active() && hotbar;
	}

	public boolean zoomOn() {
		return active() && zoom;
	}

	public boolean crosshairOn() {
		return active() && crosshair;
	}

	public boolean cameraOn() {
		return active() && (cameraStrafeRoll || cameraLandingDip || cameraSprintFov || cameraBobSmoothing);
	}

	public boolean screenOpenOn() {
		return active() && screenOpen;
	}

	public float hotbarIntensityEff() {
		return hotbarIntensity * masterIntensity();
	}

	public float hotbarSpeedEff() {
		return hotbarSpeed * masterSpeed();
	}

	public float zoomSpeedEff() {
		return zoomSpeed * masterSpeed();
	}

	public float crosshairIntensityEff() {
		return crosshairIntensity * masterIntensity();
	}

	public float crosshairSpeedEff() {
		return crosshairSpeed * masterSpeed();
	}

	public float cameraIntensityEff() {
		return cameraIntensity * masterIntensity();
	}

	public float cameraSpeedEff() {
		return cameraSpeed * masterSpeed();
	}

	public float screenOpenIntensityEff() {
		return screenOpenIntensity * masterIntensity();
	}

	public float screenOpenSpeedEff() {
		return screenOpenSpeed * masterSpeed();
	}
}
