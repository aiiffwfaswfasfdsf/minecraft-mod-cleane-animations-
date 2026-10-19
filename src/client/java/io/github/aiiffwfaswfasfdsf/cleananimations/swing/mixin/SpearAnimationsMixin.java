package io.github.aiiffwfaswfasfdsf.cleananimations.swing.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.SwingRuntime;
import net.minecraft.client.model.effects.SpearAnimations;
import net.minecraft.world.entity.HumanoidArm;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Vanilla spears use a STAB swing; our own spear thrust replaces it. */
@Mixin(SpearAnimations.class)
public abstract class SpearAnimationsMixin {
	@Inject(method = "firstPersonAttack", at = @At("HEAD"), cancellable = true)
	private static void cleananimations$thrust(float progress, PoseStack poseStack, int side, HumanoidArm arm,
			CallbackInfo ci) {
		if (SwingRuntime.replacesVanillaSwing()) {
			ci.cancel();
		}
	}
}
