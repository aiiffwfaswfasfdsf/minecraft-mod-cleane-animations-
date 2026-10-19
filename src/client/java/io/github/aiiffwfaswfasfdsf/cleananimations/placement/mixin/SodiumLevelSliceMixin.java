package io.github.aiiffwfaswfasfdsf.cleananimations.placement.mixin;

import io.github.aiiffwfaswfasfdsf.cleananimations.placement.HiddenBlocks;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Sodium's chunk build reads block states through {@code LevelSlice}. Applied only when Sodium is loaded
 * (see PlacementMixinPlugin); there is no compile time dependency on Sodium.
 */
@Pseudo
@Mixin(targets = "net.caffeinemc.mods.sodium.client.world.LevelSlice")
public abstract class SodiumLevelSliceMixin {
	@Inject(method = "getBlockState(III)Lnet/minecraft/world/level/block/state/BlockState;", at = @At("HEAD"), cancellable = true, remap = false)
	private void cleananimations$hideAnimatedBlock(int x, int y, int z, CallbackInfoReturnable<BlockState> cir) {
		if (HiddenBlocks.isHidden(x, y, z)) {
			cir.setReturnValue(Blocks.AIR.defaultBlockState());
		}
	}
}
