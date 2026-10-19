package io.github.aiiffwfaswfasfdsf.cleananimations.core.config;

import io.github.aiiffwfaswfasfdsf.cleananimations.hud.HudConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.placement.PlacementConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.SwingConfig;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Root config object, stored as JSON in {@code <configdir>/cleananimations.json}.
 * Each feature owns one section (a field of this class); the field name is the section id.
 */
public class CleanConfig {
	private static final Logger LOGGER = LoggerFactory.getLogger("cleananimations");
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	public static final String FILE_NAME = "cleananimations.json";

	public CoreConfig core = new CoreConfig();
	public SwingConfig swing = new SwingConfig();
	public PlacementConfig placement = new PlacementConfig();
	public HudConfig hud = new HudConfig();

	private static volatile CleanConfig instance = new CleanConfig();
	private static volatile Path path;

	/** Active config. Never null. */
	public static CleanConfig get() {
		return instance;
	}

	/** Replaces the active config (used by tests). */
	public static void set(CleanConfig config) {
		instance = config;
	}

	/** Loads (or creates) the config at {@code configDir/cleananimations.json} and makes it active. */
	public static CleanConfig load(Path configDir) {
		path = configDir.resolve(FILE_NAME);
		CleanConfig loaded = read(path);
		instance = loaded;
		return loaded;
	}

	/** Tolerant read: missing file, broken JSON or bad values fall back to defaults. */
	public static CleanConfig read(Path file) {
		CleanConfig cfg = null;
		if (Files.isRegularFile(file)) {
			try {
				cfg = GSON.fromJson(Files.readString(file, StandardCharsets.UTF_8), CleanConfig.class);
			} catch (IOException | RuntimeException e) {
				LOGGER.warn("Could not read " + file + ", using defaults", e);
			}
		}
		if (cfg == null) {
			cfg = new CleanConfig();
		}
		cfg.repair();
		return cfg;
	}

	/** Saves to the path given to {@link #load(Path)}; no-op before loading. */
	public void save() {
		Path p = path;
		if (p != null) {
			save(p);
		}
	}

	/** Atomic save: write a temp file next to the target, then move it into place. */
	public void save(Path file) {
		try {
			Path parent = file.toAbsolutePath().getParent();
			if (parent != null) {
				Files.createDirectories(parent);
			}
			Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
			Files.writeString(tmp, GSON.toJson(this), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, file, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			LOGGER.warn("Could not save " + file, e);
		}
	}

	/** All sections by id, in declaration order. */
	public Map<String, ConfigSection> sections() {
		Map<String, ConfigSection> out = new LinkedHashMap<>();
		for (Field f : CleanConfig.class.getDeclaredFields()) {
			if (ConfigSection.class.isAssignableFrom(f.getType())) {
				try {
					out.put(f.getName(), (ConfigSection) f.get(this));
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		return out;
	}

	/** All annotated fields of all sections. */
	public List<ConfigField> fields() {
		List<ConfigField> out = new ArrayList<>();
		sections().forEach((id, s) -> out.addAll(ConfigField.of(id, s)));
		return out;
	}

	/** Replaces null sections with defaults and clamps invalid values. */
	private void repair() {
		for (Field f : CleanConfig.class.getDeclaredFields()) {
			if (ConfigSection.class.isAssignableFrom(f.getType())) {
				try {
					if (f.get(this) == null) {
						f.set(this, f.getType().getDeclaredConstructor().newInstance());
					}
				} catch (ReflectiveOperationException e) {
					throw new IllegalStateException(e);
				}
			}
		}
		fields().forEach(ConfigField::sanitize);
	}

	/** Convenience: master toggle. */
	public static boolean enabled() {
		return instance.core.enabled;
	}
}
