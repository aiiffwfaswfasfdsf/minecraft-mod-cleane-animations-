package io.github.aiiffwfaswfasfdsf.cleananimations.hud;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.CameraFeel;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.CrosshairAnimator;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.FrameClock;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.HotbarAnimator;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.ScreenOpenAnimator;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.anim.ZoomController;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Holds the live animation state of the hud module and feeds it from the client tick. Render thread only. */
public final class HudRuntime {
	public static final HotbarAnimator HOTBAR = new HotbarAnimator(AnimMath.get());
	public static final ZoomController ZOOM = new ZoomController(AnimMath.get());
	public static final CrosshairAnimator CROSSHAIR = new CrosshairAnimator(AnimMath.get());
	public static final CameraFeel CAMERA = new CameraFeel(AnimMath.get());
	public static final ScreenOpenAnimator SCREEN = new ScreenOpenAnimator(AnimMath.get());

	private static final FrameClock FRAME_CLOCK = new FrameClock();
	private static final FrameClock HOTBAR_CLOCK = new FrameClock();
	private static final FrameClock CROSSHAIR_CLOCK = new FrameClock();
	private static final FrameClock SCREEN_CLOCK = new FrameClock();
	private static final FrameClock BOB_CLOCK = new FrameClock();

	private static boolean wasAttackDown;
	private static boolean wasUseDown;
	private static boolean wasOnGround = true;
	private static float lastVerticalSpeed;
	private static boolean zoomLevelInitialised;

	private HudRuntime() {
	}

	// ------------------------------------------------------------------ client tick

	public static void tick(Minecraft mc) {
		HudConfig cfg = HudConfig.get();
		LocalPlayer player = mc.player;
		if (player == null || mc.level == null) {
			HOTBAR.reset();
			CAMERA.reset();
			ZOOM.setHeld(false);
			wasOnGround = true;
			lastVerticalSpeed = 0f;
			return;
		}
		boolean inGame = mc.gui.screen() == null;

		tickHotbar(mc, player, cfg);
		tickZoom(player, cfg, inGame);
		tickCrosshair(mc, cfg, inGame);
		tickCamera(player, cfg);
	}

	private static void tickHotbar(Minecraft mc, LocalPlayer player, HudConfig cfg) {
		Inventory inv = player.getInventory();
		boolean bounce = cfg.hotbarOn() && cfg.hotbarItemBounce;
		for (int i = 0; i < HotbarAnimator.SLOTS; i++) {
			ItemStack s = inv.getItem(i);
			HOTBAR.observeSlot(i, s.isEmpty() ? null : s.getItem(), s.getCount(), 1f);
		}
		boolean useDown = mc.options.keyUse.isDown() && mc.gui.screen() == null;
		if (useDown && !wasUseDown && !inv.getItem(inv.getSelectedSlot()).isEmpty() && bounce) {
			HOTBAR.kick(inv.getSelectedSlot(), 0.55f);
		}
		if (!bounce) {
			HOTBAR.clearBounces();
		}
	}

	private static void tickZoom(LocalPlayer player, HudConfig cfg, boolean inGame) {
		if (!zoomLevelInitialised) {
			ZOOM.setLevel(cfg.zoomLevel, cfg.zoomMaxLevel);
			zoomLevelInitialised = true;
		}
		boolean want = cfg.zoomOn() && inGame && !player.isScoping() && HudModule.ZOOM_KEY.isDown();
		if (want && !ZOOM.isHeld()) {
			ZOOM.setLevel(cfg.zoomLevel, cfg.zoomMaxLevel);
			ZOOM.resetSmoothing();
		}
		ZOOM.setHeld(want);
	}

	private static void tickCrosshair(Minecraft mc, HudConfig cfg, boolean inGame) {
		boolean attackDown = inGame && mc.options.keyAttack.isDown();
		boolean useDown = inGame && mc.options.keyUse.isDown();
		float intensity = cfg.crosshairIntensityEff();
		if (cfg.crosshairOn()) {
			if (attackDown && !wasAttackDown) {
				CROSSHAIR.attack(intensity);
			}
			if (useDown && !wasUseDown) {
				CROSSHAIR.use(intensity);
			}
			HitResult hit = mc.hitResult;
			CROSSHAIR.setMining(attackDown && hit != null && hit.getType() == HitResult.Type.BLOCK);
		} else {
			CROSSHAIR.setMining(false);
		}
		wasAttackDown = attackDown;
		wasUseDown = useDown;
	}

	private static void tickCamera(LocalPlayer player, HudConfig cfg) {
		Vec3 v = player.getDeltaMovement();
		double yaw = Math.toRadians(player.getYRot());
		// Right-hand vector of the view in the x/z plane (facing +z = yaw 0, right = -x).
		double strafe = v.x * -Math.cos(yaw) + v.z * -Math.sin(yaw);
		boolean onGround = player.onGround();
		CAMERA.setInput(onGround ? (float) strafe : (float) strafe * 0.5f, player.isSprinting());
		if (onGround && !wasOnGround && cfg.cameraOn() && cfg.cameraLandingDip) {
			CAMERA.land(-lastVerticalSpeed, cfg.cameraIntensityEff());
		}
		wasOnGround = onGround;
		lastVerticalSpeed = (float) v.y;
	}

	// ------------------------------------------------------------------ per frame

	/** Called once per rendered frame from {@code Camera.update}: steps zoom and camera feel. */
	public static void frame() {
		HudConfig cfg = HudConfig.get();
		float dt = FRAME_CLOCK.tick();
		ZOOM.update(dt, cfg.zoomSpeedEff(), 0.22f, cfg.zoomMaxLevel);
		float ci = cfg.cameraIntensityEff();
		CAMERA.update(dt, cfg.cameraSpeedEff(),
				cfg.cameraOn() && cfg.cameraStrafeRoll, CameraFeel.MAX_ROLL_DEGREES * 0.8f * ci,
				cfg.cameraOn() && cfg.cameraLandingDip,
				cfg.cameraOn() && cfg.cameraSprintFov, 0.035f * ci);
	}

	public static void hotbarFrame(int selectedSlot) {
		HudConfig cfg = HudConfig.get();
		HOTBAR.update(HOTBAR_CLOCK.tick(), selectedSlot, cfg.hotbarSpeedEff(), cfg.hotbarBouncy);
	}

	public static void crosshairFrame() {
		HudConfig cfg = HudConfig.get();
		CROSSHAIR.update(CROSSHAIR_CLOCK.tick(), cfg.crosshairSpeedEff(), cfg.crosshairIntensityEff());
	}

	public static float screenFrame(Object screen) {
		HudConfig cfg = HudConfig.get();
		return SCREEN.frame(screen, SCREEN_CLOCK.tick(), cfg.screenOpenSpeedEff(), cfg.screenOpenIntensityEff());
	}

	public static float bobFrameDt() {
		return BOB_CLOCK.tick();
	}

	/** Sensitivity multiplier while zooming (1 when not). */
	public static float turnMultiplier(HudConfig cfg) {
		if (!ZOOM.isActive() || !cfg.zoomScaleSensitivity) {
			return 1f;
		}
		return Math.max(0.12f, ZOOM.fovMultiplier());
	}
}
