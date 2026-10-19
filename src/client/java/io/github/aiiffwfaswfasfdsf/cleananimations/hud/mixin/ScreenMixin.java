package io.github.aiiffwfaswfasfdsf.cleananimations.hud.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudRuntime;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Screen-open ease: the screen's foreground (not its background) scales 0.96 to 1 when it appears. */
@Mixin(Screen.class)
public abstract class ScreenMixin {
	@WrapOperation(method = "extractRenderStateWithTooltipAndSubtitles",
			at = @At(value = "INVOKE",
					target = "Lnet/minecraft/client/gui/screens/Screen;extractRenderState(Lnet/minecraft/client/gui/GuiGraphicsExtractor;IIF)V"))
	private void cleananimations$open(Screen self, GuiGraphicsExtractor g, int mx, int my, float partial, Operation<Void> original) {
		if (!HudConfig.get().screenOpenOn() || Minecraft.getInstance().level == null) {
			HudRuntime.SCREEN.forget();
			original.call(self, g, mx, my, partial);
			return;
		}
		float s = HudRuntime.screenFrame(self);
		if (s >= 0.9999f) {
			original.call(self, g, mx, my, partial);
			return;
		}
		float cx = g.guiWidth() / 2f;
		float cy = g.guiHeight() / 2f;
		g.pose().pushMatrix();
		g.pose().translate(cx, cy);
		g.pose().scale(s, s);
		g.pose().translate(-cx, -cy);
		original.call(self, g, mx, my, partial);
		g.pose().popMatrix();
	}
}
