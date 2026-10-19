package io.github.aiiffwfaswfasfdsf.cleananimations.placement.mixin;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(Minecraft.class)
public interface MinecraftAccessor {
	@Accessor("rightClickDelay")
	int cleananimations$getRightClickDelay();

	@Accessor("rightClickDelay")
	void cleananimations$setRightClickDelay(int value);
}
