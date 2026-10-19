package io.github.aiiffwfaswfasfdsf.cleananimations.placement.mixin;

import io.github.aiiffwfaswfasfdsf.cleananimations.placement.PlacementAnimations;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientLevel.class)
public abstract class ClientLevelMixin {
	/** Runs before the new state is applied, so the block is already hidden when the section gets re-meshed. */
	@Inject(method = "setBlock(Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;II)Z", at = @At("HEAD"))
	private void cleananimations$beforeSetBlock(BlockPos pos, BlockState state, int flags, int depth, CallbackInfoReturnable<Boolean> cir) {
		PlacementAnimations.onSetBlock((ClientLevel) (Object) this, pos, state);
	}
}
