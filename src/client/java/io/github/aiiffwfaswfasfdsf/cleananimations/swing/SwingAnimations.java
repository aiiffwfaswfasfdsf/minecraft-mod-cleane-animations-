package io.github.aiiffwfaswfasfdsf.cleananimations.swing;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.CleanAnimationsClient;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim.SwingAnimation;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim.SwingAnimationParser;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

/**
 * Loads {@code assets/cleananimations/animations/swing/<name>.json} on every resource reload, so resource packs can
 * override any default. A file that fails to parse is logged and the previous/default entry for that name is kept.
 */
public final class SwingAnimations implements ResourceManagerReloadListener {
	public static final String INSPECT = "inspect";
	private static final Identifier ID = Identifier.fromNamespaceAndPath(CleanAnimationsClient.MOD_ID, "swing_animations");

	private static volatile Map<String, SwingAnimation> byName = Map.of();

	private SwingAnimations() {
	}

	public static void register() {
		ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(ID, new SwingAnimations());
	}

	/** Animation for a kind, or null when the kind keeps vanilla. Falls back to the generic animation. */
	public static SwingAnimation of(ToolKind kind) {
		if (kind.animation == null) {
			return null;
		}
		Map<String, SwingAnimation> m = byName;
		SwingAnimation a = m.get(kind.animation);
		return a != null ? a : m.get(ToolKind.GENERIC.animation);
	}

	public static SwingAnimation named(String name) {
		return byName.get(name);
	}

	@Override
	public void onResourceManagerReload(ResourceManager manager) {
		Map<String, SwingAnimation> loaded = new java.util.HashMap<>();
		for (ToolKind kind : ToolKind.animated()) {
			read(manager, kind.animation, loaded);
		}
		read(manager, INSPECT, loaded);
		byName = Map.copyOf(loaded);
		ToolKind.clearCache();
		SwingRuntime.reset();
	}

	private static void read(ResourceManager manager, String name, Map<String, SwingAnimation> into) {
		Identifier id = Identifier.fromNamespaceAndPath(CleanAnimationsClient.MOD_ID, "animations/swing/" + name + ".json");
		Optional<Resource> res = manager.getResource(id);
		if (res.isEmpty()) {
			CleanAnimationsClient.LOGGER.warn("Missing swing animation {}", id);
			return;
		}
		try (BufferedReader reader = res.get().openAsReader()) {
			into.put(name, SwingAnimationParser.parse(reader));
		} catch (IOException | RuntimeException e) {
			CleanAnimationsClient.LOGGER.warn("Could not load swing animation {} from pack {}: {}", id,
					res.get().sourcePackId(), e.getMessage());
			SwingAnimation previous = byName.get(name);
			if (previous != null) {
				into.put(name, previous);
			}
		}
	}

}
