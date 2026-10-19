package io.github.aiiffwfaswfasfdsf.cleananimations.swing.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.SwingRuntime;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Thin hooks into the first-person hand renderer; all logic lives in {@link SwingRuntime}. */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsAndItemsRendererMixin {
	@Inject(method = "submitHandsWithItems", at = @At("HEAD"))
	private void cleananimations$beginFrame(float partial, PoseStack poseStack, SubmitNodeCollector collector,
			PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
		SwingRuntime.beginFrame(partial, player, state);
	}

	@Inject(method = "submitHandsWithItems", at = @At("RETURN"))
	private void cleananimations$endFrame(float partial, PoseStack poseStack, SubmitNodeCollector collector,
			PlayerRenderState player, FirstPersonHandsAndItemsRenderState state, CallbackInfo ci) {
		SwingRuntime.endFrame();
	}

	@Inject(method = "submitArmWithItem", at = @At("HEAD"))
	private void cleananimations$beginHand(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state,
			float partial, float pitch, InteractionHand hand, float swing, ItemStack item, float equip,
			PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		SwingRuntime.beginHand(player, state, partial, hand, swing, item, equip);
	}

	@Inject(method = "submitArmWithItem", at = @At("RETURN"))
	private void cleananimations$endHand(PlayerRenderState player, FirstPersonHandsAndItemsRenderState state,
			float partial, float pitch, InteractionHand hand, float swing, ItemStack item, float equip,
			PoseStack poseStack, SubmitNodeCollector collector, int light, CallbackInfo ci) {
		SwingRuntime.endHand();
	}

	@Inject(method = "applyItemArmTransform", at = @At("HEAD"), cancellable = true)
	private void cleananimations$armTransform(PoseStack poseStack, HumanoidArm arm, float equip, CallbackInfo ci) {
		if (SwingRuntime.applyArmTransform(poseStack, arm, equip)) {
			ci.cancel();
		}
	}

	@Inject(method = "swingArm", at = @At("HEAD"), cancellable = true)
	private void cleananimations$swingArm(float progress, PoseStack poseStack, int side, HumanoidArm arm,
			CallbackInfo ci) {
		if (SwingRuntime.replacesVanillaSwing()) {
			ci.cancel();
		}
	}
}
