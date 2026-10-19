package io.github.aiiffwfaswfasfdsf.cleananimations.placement.mixin;

import io.github.aiiffwfaswfasfdsf.cleananimations.placement.BridgingAssist;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftMixin {
	@Shadow
	public HitResult hitResult;

	@Unique
	private HitResult cleananimations$savedHit;

	/** Bridging assist: while a use action is processed, a vanilla miss is replaced by the bridging target. */
	@Inject(method = "startUseItem", at = @At("HEAD"))
	private void cleananimations$bridgeBefore(CallbackInfo ci) {
		cleananimations$savedHit = null;
		BlockHitResult bridge = BridgingAssist.overrideHit((Minecraft) (Object) this);
		if (bridge != null) {
			cleananimations$savedHit = this.hitResult;
			this.hitResult = bridge;
		}
	}

	@Inject(method = "startUseItem", at = @At("RETURN"))
	private void cleananimations$bridgeAfter(CallbackInfo ci) {
		if (cleananimations$savedHit != null) {
			this.hitResult = cleananimations$savedHit;
			cleananimations$savedHit = null;
		}
	}
}
