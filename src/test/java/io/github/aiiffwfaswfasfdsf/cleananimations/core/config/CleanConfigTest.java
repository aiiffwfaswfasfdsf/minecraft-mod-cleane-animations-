package io.github.aiiffwfaswfasfdsf.cleananimations.core.config;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CleanConfigTest {
	@TempDir
	Path dir;

	@Test
	void roundTrip() {
		CleanConfig c = new CleanConfig();
		c.core.enabled = false;
		c.core.animationSpeed = 1.5f;
		c.core.intensity = 0.25f;
		Path f = dir.resolve("nested/cleananimations.json");
		c.save(f);
		assertTrue(Files.isRegularFile(f));
		assertFalse(Files.exists(dir.resolve("nested/cleananimations.json.tmp")));

		CleanConfig r = CleanConfig.read(f);
		assertFalse(r.core.enabled);
		assertEquals(1.5f, r.core.animationSpeed);
		assertEquals(0.25f, r.core.intensity);
	}

	@Test
	void missingFileGivesDefaults() {
		CleanConfig r = CleanConfig.read(dir.resolve("nope.json"));
		assertTrue(r.core.enabled);
		assertEquals(1.0f, r.core.animationSpeed);
	}

	@Test
	void brokenJsonGivesDefaults() throws Exception {
		Path f = dir.resolve("bad.json");
		Files.writeString(f, "{ this is not json");
		assertTrue(CleanConfig.read(f).core.enabled);
	}

	@Test
	void outOfRangeValuesAreClampedAndNullSectionsRepaired() throws Exception {
		Path f = dir.resolve("range.json");
		Files.writeString(f, "{\"core\":{\"animationSpeed\":99,\"intensity\":-4},\"swing\":null}");
		CleanConfig r = CleanConfig.read(f);
		assertEquals(2.0f, r.core.animationSpeed);
		assertEquals(0.0f, r.core.intensity);
		assertNotNull(r.swing);
	}

	@Test
	void fieldModelListsCoreFields() {
		var names = new CleanConfig().fields().stream().map(ConfigField::labelKey).toList();
		assertTrue(names.contains("cleananimations.config.core.enabled"));
		assertTrue(names.contains("cleananimations.config.core.animationSpeed"));
		assertTrue(names.contains("cleananimations.config.core.intensity"));
		assertEquals(java.util.List.of("core", "swing", "placement", "hud"),
				java.util.List.copyOf(new CleanConfig().sections().keySet()));
	}

	@Test
	void sliderSnapsToStep() {
		CleanConfig c = new CleanConfig();
		ConfigField f = c.fields().stream().filter(x -> x.name().equals("animationSpeed")).findFirst().orElseThrow();
		f.setNumber(1.013);
		assertEquals(1.0f, c.core.animationSpeed, 1e-4f);
	}
}
