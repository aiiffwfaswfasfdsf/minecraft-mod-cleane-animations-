package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.placement.mixin.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Precise placement glue: skips vanilla's 4 tick use delay when a new block face is targeted (see {@link PreciseTargeting}). */
public final class PrecisePlacement {
	private static final PreciseTargeting TARGETING = new PreciseTargeting();

	private PrecisePlacement() {
	}

	/** Runs at the start of every client tick, before vanilla handles the use key. */
	public static void tick(Minecraft mc) {
		CleanConfig c = CleanConfig.get();
		if (!c.core.enabled || !c.placement.precisePlacement || mc.player == null || mc.gui.screen() != null) {
			TARGETING.reset();
			return;
		}
		boolean holdsBlock = mc.player.getMainHandItem().getItem() instanceof BlockItem
				|| mc.player.getOffhandItem().getItem() instanceof BlockItem;
		if (!holdsBlock) {
			TARGETING.reset();
			return;
		}
		PreciseTargeting.Target target = null;
		if (mc.hitResult instanceof BlockHitResult bhr && bhr.getType() == HitResult.Type.BLOCK) {
			target = new PreciseTargeting.Target(bhr.getBlockPos().asLong(), bhr.getDirection().ordinal());
		}
		MinecraftAccessor access = (MinecraftAccessor) mc;
		switch (TARGETING.update(mc.options.keyUse.isDown(), target)) {
			case PLACE_NOW -> access.cleananimations$setRightClickDelay(0);
			case HOLD_OFF -> access.cleananimations$setRightClickDelay(Math.max(access.cleananimations$getRightClickDelay(), 2));
			case NONE -> {
			}
		}
	}

	public static void reset() {
		TARGETING.reset();
	}
}
