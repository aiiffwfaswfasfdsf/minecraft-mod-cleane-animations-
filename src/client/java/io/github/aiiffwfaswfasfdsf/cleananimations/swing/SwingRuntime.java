package io.github.aiiffwfaswfasfdsf.cleananimations.swing;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.CleanAnimationsClient;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim.Channel;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim.Clip;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim.SwingAnimation;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwingAnimationType;

/**
 * Per-frame state machine behind the first-person animations. The mixins only forward to this class.
 * All time is measured in game ticks plus the render partial tick, so animations are smooth per frame,
 * freeze while the game is paused and never depend on the frame rate.
 */
public final class SwingRuntime {
	/** Window in ticks (600 ms) in which consecutive attacks continue a combo. */
	public static final float COMBO_WINDOW_TICKS = 12f;
	private static final float BLEND_TICKS = 2.5f;
	private static final int C = Channel.COUNT;

	// --- test / debug hooks (see SwingClientGameTest). NaN = off. ---
	public static volatile float debugProgress = Float.NaN;
	public static volatile int debugComboStep = 0;
	public static volatile boolean debugMining = false;
	public static volatile float debugInspect = Float.NaN;
	/** Raw pose (one value per {@link Channel}) forced onto the main hand; null = off. */
	public static volatile float[] debugPose = null;
	/** Number of hand transforms that went through the custom path (lets tests assert the mixin ran). */
	public static volatile long appliedCount = 0;
	/** Number of swings detected (and started) by the runtime. */
	public static volatile long swingsStarted = 0;

	private static boolean broken;

	// --- per frame ---
	private static boolean frameActive;
	private static float tickTime;
	private static float dt;
	private static long lastNanos;
	private static float swayYaw, swayYawVel, swayPitch, swayPitchVel;
	private static float lastViewYaw, lastViewPitch;
	private static boolean haveView;
	private static float walkPos, walkSpeed;
	private static final float[] SPRING = new float[2];

	// --- inspect ---
	private static float inspectStart = -1e9f;
	private static net.minecraft.world.item.Item inspectItem;

	// --- per hand ---
	private static final HandState[] HANDS = {new HandState(), new HandState()};

	private static final class HandState {
		boolean wasSwinging;
		float lastP;
		float startTick = -1e9f;
		float lastStartTick = -1e9f;
		int comboStep;
		boolean swingActive;
		Clip clip;
		float clipTicks;
		final float[] blendFrom = new float[C];
		final float[] lastPose = new float[C];
		float blendStart = -1e9f;
		float mineW;
		float mineStart;
		float lastEquip;
		boolean raising;
		float settle, settleVel;
		net.minecraft.world.item.Item lastItem;
		final float[] pose = new float[C];
		final float[] tmp = new float[C];

		HandState() {
			Clip.rest(lastPose);
		}
	}

	// --- current hand context (valid between beginHand and endHand) ---
	private static HandState cur;
	private static boolean ctxActive;
	private static boolean ctxCustom;
	private static boolean ctxUsing;
	private static ToolKind ctxKind = ToolKind.GENERIC;
	private static SwingAnimation ctxAnim;
	private static boolean ctxSwingPose;
	private static float ctxSpeed = 1f;
	private static float ctxIntensity = 1f;

	private SwingRuntime() {
	}

	public static void reset() {
		for (int i = 0; i < HANDS.length; i++) {
			HANDS[i] = new HandState();
		}
		inspectStart = -1e9f;
		haveView = false;
		swayYaw = swayYawVel = swayPitch = swayPitchVel = 0f;
	}

	// ------------------------------------------------------------------ config helpers

	private static boolean moduleOn() {
		CleanConfig cfg = CleanConfig.get();
		return !broken && cfg.core.enabled && cfg.swing.enabled;
	}

	private static float speed() {
		CleanConfig cfg = CleanConfig.get();
		return Math.max(0.1f, cfg.core.animationSpeed * cfg.swing.speed);
	}

	private static float intensity() {
		CleanConfig cfg = CleanConfig.get();
		return Math.max(0f, cfg.core.intensity * cfg.swing.intensity);
	}

	private static void fail(Throwable t) {
		if (!broken) {
			broken = true;
			CleanAnimationsClient.LOGGER.error("Swing animations disabled after an error, falling back to vanilla", t);
		}
		frameActive = false;
		ctxActive = false;
		ctxCustom = false;
	}

	// ------------------------------------------------------------------ frame hooks

	public static void beginFrame(float partial, PlayerRenderState player, FirstPersonHandsAndItemsRenderState state) {
		try {
			frameActive = false;
			if (!moduleOn()) {
				return;
			}
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) {
				return;
			}
			frameActive = true;
			tickTime = mc.level.getGameTime() + partial;

			long now = System.nanoTime();
			dt = lastNanos == 0 ? 0f : Mth.clamp((now - lastNanos) / 1.0e9f, 0f, 0.05f);
			lastNanos = now;

			updateSway(state);
			AvatarRenderState avatar = player.avatarRenderState;
			if (avatar != null) {
				walkPos = avatar.walkAnimationPos;
				walkSpeed = Math.min(1f, avatar.walkAnimationSpeed);
			}
		} catch (Throwable t) {
			fail(t);
		}
	}

	public static void endFrame() {
		frameActive = false;
	}

	private static void updateSway(FirstPersonHandsAndItemsRenderState state) {
		AnimMath math = AnimMath.get();
		CleanConfig cfg = CleanConfig.get();
		float strength = cfg.swing.inertiaStrength * intensity();
		float yaw = state.viewYRot;
		float pitch = state.viewXRot;
		if (haveView) {
			float dy = Mth.clamp(Mth.wrapDegrees(yaw - lastViewYaw), -30f, 30f);
			float dp = Mth.clamp(pitch - lastViewPitch, -30f, 30f);
			// The item lags behind the camera: kick it opposite to the camera motion, the spring pulls it home.
			swayYaw += dy * 0.45f;
			swayPitch += dp * 0.45f;
		}
		haveView = true;
		lastViewYaw = yaw;
		lastViewPitch = pitch;
		swayYaw = Mth.clamp(swayYaw, -16f, 16f);
		swayPitch = Mth.clamp(swayPitch, -16f, 16f);
		math.springStep(swayYaw, swayYawVel, 0f, 110f, 13f, dt, SPRING);
		swayYaw = SPRING[0];
		swayYawVel = SPRING[1];
		math.springStep(swayPitch, swayPitchVel, 0f, 110f, 13f, dt, SPRING);
		swayPitch = SPRING[0];
		swayPitchVel = SPRING[1];
		if (strength <= 0f) {
			swayYaw = swayPitch = swayYawVel = swayPitchVel = 0f;
		}
	}

	// ------------------------------------------------------------------ hand hooks

	public static void beginHand(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, float partial,
			InteractionHand hand, float vanillaSwing, ItemStack item, float equip) {
		ctxActive = false;
		ctxCustom = false;
		try {
			if (!frameActive || item.isEmpty() || item.has(DataComponents.MAP_ID)) {
				return;
			}
			AvatarRenderState avatar = player.avatarRenderState;
			if (avatar == null) {
				return;
			}
			CleanConfig cfg = CleanConfig.get();
			HandState h = HANDS[hand == InteractionHand.MAIN_HAND ? 0 : 1];
			cur = h;
			ctxActive = true;
			ctxKind = ToolKind.of(item);
			ctxAnim = SwingAnimations.of(ctxKind);
			ctxSpeed = speed();
			ctxIntensity = intensity();
			ctxUsing = avatar.isUsingItem && state.useItemRemainingTicks > 0 && avatar.useItemHand == hand;
			LivingEntity.SwingDescription swing = avatar.currentSwing;
			boolean vanillaSwinging = swing != null && swing.hand() == hand && vanillaSwing > 0.0001f
					&& swing.animation().type() != SwingAnimationType.NONE;
			ctxCustom = ctxAnim != null && cfg.swing.swingEnabled(ctxKind) && !ctxUsing;
			if (h.lastItem != item.getItem()) {
				h.lastItem = item.getItem();
				h.swingActive = false;
				h.mineW = 0f;
				if (inspectItem != item.getItem()) {
					inspectStart = -1e9f;
				}
			}
			if (ctxCustom) {
				updateSwing(h, hand, vanillaSwinging, vanillaSwing, swing, avatar);
			} else {
				h.swingActive = false;
				h.mineW = 0f;
				Clip.rest(h.pose);
			}
			ctxSwingPose = ctxCustom;
		} catch (Throwable t) {
			fail(t);
		}
	}

	public static void endHand() {
		ctxActive = false;
		ctxCustom = false;
		cur = null;
	}

	/** True when the vanilla swing transform must be skipped for the hand being rendered. */
	public static boolean replacesVanillaSwing() {
		return ctxActive && ctxCustom;
	}

	// ------------------------------------------------------------------ swing / mining / inspect

	private static void updateSwing(HandState h, InteractionHand hand, boolean vanillaSwinging, float vanillaP,
			LivingEntity.SwingDescription swing, AvatarRenderState avatar) {
		CleanConfig cfg = CleanConfig.get();
		SwingAnimation anim = ctxAnim;
		AnimMath math = AnimMath.get();
		float speed = ctxSpeed;
		boolean main = hand == InteractionHand.MAIN_HAND;

		float dbg = debugProgress;
		boolean debugging = main && !Float.isNaN(dbg);
		boolean dbgMining = main && debugMining;

		// --- detect the start of a swing ---
		if (debugging) {
			h.clip = anim.clip(debugComboStep);
			h.swingActive = true;
			h.comboStep = debugComboStep;
		} else {
			if (vanillaSwinging && (!h.wasSwinging || vanillaP < h.lastP - 0.25f)) {
				startSwing(h, anim, cfg);
				h.startTick = tickTime - vanillaP * (swing != null ? swing.durationTicks() : 6f);
				inspectStart = -1e9f;
			}
			h.wasSwinging = vanillaSwinging;
			h.lastP = vanillaSwinging ? vanillaP : 0f;
		}

		// --- attack clip pose ---
		float p = 0f;
		float[] pose = h.pose;
		Clip.rest(pose);
		if (h.swingActive && h.clip != null) {
			h.clipTicks = h.clip.durationTicks() / speed;
			p = debugging ? dbg : (tickTime - h.startTick) / h.clipTicks;
			if (p >= 1f && !debugging) {
				h.swingActive = false;
				p = 1f;
			}
			if (p < 0f) {
				p = 0f;
			}
			h.clip.sample(math, p, pose);
			float since = tickTime - h.blendStart;
			if (!debugging && since < BLEND_TICKS) {
				float k = Mth.clamp(since / BLEND_TICKS, 0f, 1f);
				k = k * k * (3f - 2f * k);
				for (int i = 0; i < C; i++) {
					pose[i] = Mth.lerp(k, h.blendFrom[i], pose[i]);
				}
			}
			if (anim.shake() != null && p > anim.shake().at()) {
				float tt = (p - anim.shake().at()) * h.clipTicks;
				float amp = anim.shake().amplitude() * (float) Math.exp(-3.0 * tt / anim.shake().ticks());
				pose[Channel.POS_X.ordinal()] += Mth.sin(tt * 9.1f) * amp;
				pose[Channel.POS_Y.ordinal()] += Mth.cos(tt * 7.3f) * amp;
			}
		}

		// --- mining loop (start -> loop -> end by blend weight) ---
		Clip mining = anim.mining();
		boolean mineTarget = false;
		if (mining != null && cfg.swing.miningAnimation && main) {
			Minecraft mc = Minecraft.getInstance();
			mineTarget = dbgMining || (mc.gameMode != null && mc.gameMode.isDestroying());
		}
		if (mineTarget && h.mineW < 0.02f) {
			h.mineStart = tickTime;
		}
		if (debugging || dbgMining) {
			h.mineW = dbgMining ? 1f : 0f;
		} else {
			h.mineW += ((mineTarget ? 1f : 0f) - h.mineW) * (1f - (float) Math.exp(-dt * 11f));
		}
		if (mining != null && h.mineW > 0.002f) {
			float loopTicks = mining.durationTicks() / speed;
			float x = dbgMining ? (Float.isNaN(dbg) ? 0f : dbg) : (tickTime - h.mineStart) / loopTicks;
			float lp = x - (float) Math.floor(x);
			mining.sample(math, lp, h.tmp);
			float w = h.mineW;
			for (int i = 0; i < C; i++) {
				pose[i] = Mth.lerp(w, pose[i], h.tmp[i]);
			}
			// A mining loop is a held state, not a hit: it cancels a pending swing blend.
		}

		System.arraycopy(pose, 0, h.lastPose, 0, C);

		float[] forced = debugPose;
		if (main && forced != null && forced.length >= C) {
			System.arraycopy(forced, 0, pose, 0, C);
		}

		// --- inspect (main hand only) ---
		if (main) {
			SwingAnimation inspect = SwingAnimations.named(SwingAnimations.INSPECT);
			float ip = Float.NaN;
			if (!Float.isNaN(debugInspect)) {
				ip = debugInspect;
			} else if (inspect != null && cfg.swing.inspectEnabled) {
				float elapsed = tickTime - inspectStart;
				float dur = inspect.attack().durationTicks() / speed;
				if (elapsed >= 0f && elapsed < dur) {
					ip = elapsed / dur;
				}
			}
			if (!Float.isNaN(ip) && inspect != null) {
				inspect.attack().sample(math, ip, h.tmp);
				for (int i = 0; i < C; i++) {
					if (i == Channel.SCALE.ordinal()) {
						pose[i] *= h.tmp[i];
					} else {
						pose[i] += h.tmp[i];
					}
				}
			}
		}
	}

	private static void startSwing(HandState h, SwingAnimation anim, CleanConfig cfg) {
		boolean combo = cfg.swing.comboEnabled && anim.comboSize() > 1;
		if (combo && tickTime - h.lastStartTick <= COMBO_WINDOW_TICKS && tickTime >= h.lastStartTick) {
			h.comboStep = (h.comboStep + 1) % anim.comboSize();
		} else {
			h.comboStep = 0;
		}
		h.lastStartTick = tickTime;
		swingsStarted++;
		h.clip = anim.clip(h.comboStep);
		h.swingActive = true;
		System.arraycopy(h.lastPose, 0, h.blendFrom, 0, C);
		h.blendStart = tickTime;
	}

	/** Called from the client tick when the inspect key was pressed. */
	public static void requestInspect(Minecraft mc) {
		if (mc.player == null || mc.level == null || !moduleOn() || !CleanConfig.get().swing.inspectEnabled) {
			return;
		}
		ItemStack held = mc.player.getMainHandItem();
		if (held.isEmpty() || mc.player.isUsingItem()) {
			return;
		}
		inspectStart = mc.level.getGameTime();
		inspectItem = held.getItem();
	}

	// ------------------------------------------------------------------ the arm transform

	/**
	 * Replacement for the vanilla "item arm" transform: equip animation, idle breathing, camera inertia,
	 * walking sway and the swing/mining/inspect pose. Returns false when vanilla must run instead.
	 */
	public static boolean applyArmTransform(PoseStack ps, HumanoidArm arm, float equip) {
		if (!ctxActive || cur == null) {
			return false;
		}
		try {
			CleanConfig cfg = CleanConfig.get();
			HandState h = cur;
			float sign = arm == HumanoidArm.RIGHT ? 1f : -1f;
			float I = ctxIntensity;
			float[] pose = h.pose;

			float lean = ctxSwingPose ? pose[Channel.LEAN.ordinal()] * I : 0f;
			if (lean != 0f) {
				ps.rotateDegrees(Axis.ZP, lean * sign);
			}

			// --- equip: easeOutBack on the way up with a little settle, tilt while the item is away ---
			float e = equip;
			float tilt = 0f;
			if (cfg.swing.equipAnimation) {
				e = remapEquip(h, equip);
				float t = Mth.clamp(e, 0f, 1.2f);
				tilt = t;
			} else {
				h.lastEquip = equip;
			}
			ps.translate(sign * 0.56f, -0.52f + e * -0.6f, -0.72f);
			if (tilt != 0f) {
				ps.rotateDegrees(Axis.XP, -32f * tilt * I);
				ps.rotateDegrees(Axis.ZP, sign * -14f * tilt * I);
				ps.rotateDegrees(Axis.YP, sign * 18f * tilt * I);
			}

			// --- idle ---
			float time = tickTime / 20f;
			boolean calm = !ctxUsing;
			if (cfg.swing.idleBreathing && calm) {
				float b = Mth.sin(time * 1.9f);
				ps.translate(0f, b * 0.0045f * I, b * 0.002f * I);
				ps.rotateDegrees(Axis.XP, b * 0.55f * I);
				ps.rotateDegrees(Axis.ZP, Mth.sin(time * 1.1f + 1f) * 0.35f * I * sign);
			}
			if (cfg.swing.idleSway && calm && walkSpeed > 0.01f) {
				float ph = walkPos * 0.6662f;
				float amp = Math.min(1f, walkSpeed * 1.3f) * I;
				ps.translate(Mth.cos(ph) * 0.010f * amp * sign, -Math.abs(Mth.sin(ph)) * 0.014f * amp, 0f);
				ps.rotateDegrees(Axis.ZP, Mth.cos(ph) * 1.6f * amp * sign);
				ps.rotateDegrees(Axis.XP, Math.abs(Mth.sin(ph)) * 1.1f * amp);
			}
			float strength = cfg.swing.inertiaStrength * I;
			if (strength > 0f && !ctxUsing) {
				ps.rotateDegrees(Axis.YP, swayYaw * strength);
				ps.rotateDegrees(Axis.XP, swayPitch * strength);
			}

			// --- swing / mining / inspect pose ---
			if (ctxSwingPose || !Float.isNaN(debugInspect)) {
				applyPose(ps, pose, sign, I);
			}
			appliedCount++;
			return true;
		} catch (Throwable t) {
			fail(t);
			return false;
		}
	}

	private static void applyPose(PoseStack ps, float[] pose, float sign, float I) {
		float px = pose[Channel.POS_X.ordinal()] * I;
		float py = pose[Channel.POS_Y.ordinal()] * I;
		float pz = pose[Channel.POS_Z.ordinal()] * I;
		float rx = pose[Channel.ROT_X.ordinal()] * I;
		float ry = pose[Channel.ROT_Y.ordinal()] * I;
		float rz = pose[Channel.ROT_Z.ordinal()] * I;
		float sc = 1f + (pose[Channel.SCALE.ordinal()] - 1f) * I;
		if (px == 0f && py == 0f && pz == 0f && rx == 0f && ry == 0f && rz == 0f && sc == 1f) {
			return;
		}
		// matrix order T * Rz * Ry * Rx: the stance (pitch/yaw) is applied to the item first, then the whole
		// stance rolls around the view axis, which gives clean slashing arcs in the screen plane.
		ps.translate(sign * px, py, pz);
		if (rz != 0f) {
			ps.rotateDegrees(Axis.ZP, sign * rz);
		}
		if (ry != 0f) {
			ps.rotateDegrees(Axis.YP, sign * ry);
		}
		if (rx != 0f) {
			ps.rotateDegrees(Axis.XP, rx);
		}
		if (sc != 1f) {
			ps.scale(sc, sc, sc);
		}
	}

	/**
	 * Remaps vanilla's linear equip progress (0 = raised, 1 = lowered): easeOutBack while raising, a soft ease-in
	 * while lowering, plus a damped settle bounce once the new item arrived.
	 */
	private static float remapEquip(HandState h, float e) {
		AnimMath math = AnimMath.get();
		boolean raisingNow = e < h.lastEquip - 1e-4f;
		boolean loweringNow = e > h.lastEquip + 1e-4f;
		if (raisingNow) {
			if (!h.raising && h.lastEquip > 0.5f) {
				h.settleVel = 0f;
			}
			h.raising = true;
		} else if (loweringNow) {
			h.raising = false;
		}
		boolean arrived = h.raising && e <= 0.001f && h.lastEquip > 0.001f;
		h.lastEquip = e;
		if (arrived) {
			h.settleVel = -1.0f; // small downward dip after the overshoot
		}
		math.springStep(h.settle, h.settleVel, 0f, 160f, 11f, dt, SPRING);
		h.settle = SPRING[0];
		h.settleVel = SPRING[1];
		float out = e;
		if (h.raising && e > 0f) {
			out = 1f - math.ease(AnimMath.EASE_OUT_BACK, 1f - e);
		} else if (loweringNow && e < 1f) {
			out = e * e * (3f - 2f * e) * 0.35f + e * 0.65f;
		}
		return out + h.settle * 0.05f;
	}
}
