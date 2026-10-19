package io.github.aiiffwfaswfasfdsf.cleananimations.placement.mixin;

import io.github.aiiffwfaswfasfdsf.cleananimations.placement.HiddenBlocks;
import net.minecraft.client.renderer.chunk.RenderSectionRegion;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Vanilla chunk compiler: positions whose placement animation is running are seen as air. */
@Mixin(RenderSectionRegion.class)
public abstract class RenderSectionRegionMixin {
	@Inject(method = "getBlockState", at = @At("HEAD"), cancellable = true)
	private void cleananimations$hideAnimatedBlock(BlockPos pos, CallbackInfoReturnable<BlockState> cir) {
		if (HiddenBlocks.isHidden(pos)) {
			cir.setReturnValue(Blocks.AIR.defaultBlockState());
		}
	}
}
