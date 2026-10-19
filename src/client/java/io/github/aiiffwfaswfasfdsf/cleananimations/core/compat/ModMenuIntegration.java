package io.github.aiiffwfaswfasfdsf.cleananimations.core.compat;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.screen.CleanConfigScreen;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Only loaded when Mod Menu is installed (registered under the "modmenu" entrypoint). */
public final class ModMenuIntegration implements ModMenuApi {
	@Override
	public ConfigScreenFactory<?> getModConfigScreenFactory() {
		return CleanConfigScreen::new;
	}
}
