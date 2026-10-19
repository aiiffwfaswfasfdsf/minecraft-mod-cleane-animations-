package io.github.aiiffwfaswfasfdsf.cleananimations.swing;

import static org.junit.jupiter.api.Assertions.*;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim.Channel;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim.Clip;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim.SwingAnimation;
import io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim.SwingAnimationParser;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class SwingAnimationTest {
	private final AnimMath math = AnimMath.get();

	private static final String SAMPLE = """
			{ "duration": 10,
			  "channels": {
			    "rotX": [ {"t":1,"v":0}, {"t":0,"v":0,"ease":"linear"}, {"t":0.5,"v":-40,"ease":"out_cubic"} ],
			    "posY": [ {"t":0,"v":0}, {"t":1,"v":0.2} ],
			    "bogus": [ {"t":0,"v":1} ] },
			  "combo": [ { "channels": { "rotZ": [ {"t":0,"v":0}, {"t":1,"v":30} ] } },
			             { "duration": 6, "channels": { "rotZ": [ {"t":0,"v":0}, {"t":1,"v":-30} ] } } ],
			  "mining": { "duration": 12, "channels": { "posZ": [ {"t":0,"v":0}, {"t":0.5,"v":-0.1}, {"t":1,"v":0} ] } },
			  "shake": { "at": 0.6, "amplitude": 0.05, "ticks": 4 } }
			""";

	@Test
	void parsesStructure() {
		SwingAnimation a = SwingAnimationParser.parse(SAMPLE);
		assertEquals(10f, a.attack().durationTicks());
		assertEquals(2, a.combo().size());
		assertEquals(10f, a.combo().get(0).durationTicks(), "combo variants inherit the duration");
		assertEquals(6f, a.combo().get(1).durationTicks());
		assertEquals(12f, a.mining().durationTicks());
		assertEquals(0.6f, a.shake().at());
		assertEquals(2, a.comboSize());
		assertSame(a.combo().get(1), a.clip(3), "combo step wraps");
	}

	@Test
	void keysAreSortedAndSampled() {
		SwingAnimation a = SwingAnimationParser.parse(SAMPLE);
		float[] out = new float[Channel.COUNT];
		a.attack().sample(math, 0f, out);
		assertEquals(0f, out[Channel.ROT_X.ordinal()], 1e-5f);
		a.attack().sample(math, 0.5f, out);
		assertEquals(-40f, out[Channel.ROT_X.ordinal()], 1e-4f);
		assertEquals(0.1f, out[Channel.POS_Y.ordinal()], 1e-5f);
		a.attack().sample(math, 1f, out);
		assertEquals(0f, out[Channel.ROT_X.ordinal()], 1e-5f);
		assertEquals(1f, out[Channel.SCALE.ordinal()], "unanimated scale stays at rest");
		a.attack().sample(math, 7f, out);
		assertEquals(0.2f, out[Channel.POS_Y.ordinal()], 1e-5f, "progress is clamped");
	}

	@Test
	void inEasingIsExpandedAndSlowStart() {
		SwingAnimation a = SwingAnimationParser.parse(
				"{\"channels\":{\"posY\":[{\"t\":0,\"v\":0,\"ease\":\"in_cubic\"},{\"t\":1,\"v\":1}]}}");
		float[] out = new float[Channel.COUNT];
		a.attack().sample(math, 0.5f, out);
		assertEquals(0.125f, out[Channel.POS_Y.ordinal()], 0.01f);
		a.attack().sample(math, 1f, out);
		assertEquals(1f, out[Channel.POS_Y.ordinal()], 1e-5f);
		float prev = 0;
		for (int i = 1; i <= 20; i++) {
			a.attack().sample(math, i / 20f, out);
			assertTrue(out[Channel.POS_Y.ordinal()] >= prev);
			prev = out[Channel.POS_Y.ordinal()];
		}
	}

	@Test
	void rejectsBrokenDocuments() {
		assertThrows(IllegalArgumentException.class, () -> SwingAnimationParser.parse("[1,2]"));
		assertThrows(IllegalArgumentException.class, () -> SwingAnimationParser.parse("{ nope"));
		assertThrows(IllegalArgumentException.class, () -> SwingAnimationParser.parse("{\"duration\":8}"));
		assertThrows(IllegalArgumentException.class,
				() -> SwingAnimationParser.parse("{\"duration\":0,\"channels\":{}}"));
	}

	@Test
	void restPoseIsIdentity() {
		float[] out = new float[Channel.COUNT];
		Clip.rest(out);
		assertEquals(1f, out[Channel.SCALE.ordinal()]);
		assertEquals(0f, out[Channel.ROT_Z.ordinal()]);
	}

	@Test
	void everyBundledDefaultParsesAndEndsAtRest() throws Exception {
		for (ToolKind kind : ToolKind.animated()) {
			SwingAnimation a = load(kind.animation);
			assertNotNull(a.attack(), kind.name());
			for (int step = 0; step < a.comboSize(); step++) {
				assertEndsAtRest(a.clip(step), kind + " step " + step);
			}
			if (a.mining() != null) {
				assertEndsAtRest(a.mining(), kind + " mining loop");
			}
		}
		SwingAnimation inspect = load(SwingAnimations.INSPECT);
		assertEndsAtRest(inspect.attack(), "inspect");
		assertEquals(3, load("sword").comboSize(), "sword has a 3-hit combo");
		assertNotNull(load("mace").shake());
	}

	private void assertEndsAtRest(Clip clip, String what) {
		float[] start = new float[Channel.COUNT];
		float[] end = new float[Channel.COUNT];
		clip.sample(math, 0f, start);
		clip.sample(math, 1f, end);
		for (int i = 0; i < Channel.COUNT; i++) {
			float rest = Channel.values()[i].rest;
			assertEquals(rest, start[i], 1e-4f, what + " start " + Channel.values()[i]);
			assertEquals(rest, end[i], 1e-4f, what + " end " + Channel.values()[i]);
		}
	}

	private static SwingAnimation load(String name) throws Exception {
		String path = "/assets/cleananimations/animations/swing/" + name + ".json";
		try (InputStream in = SwingAnimationTest.class.getResourceAsStream(path)) {
			assertNotNull(in, "missing " + path);
			try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
				return SwingAnimationParser.parse(r);
			}
		}
	}

	@Test
	void configKindToggles() {
		SwingConfig cfg = new SwingConfig();
		assertTrue(cfg.swingEnabled(ToolKind.SWORD));
		assertFalse(cfg.swingEnabled(ToolKind.BOW), "bows keep the vanilla swing");
		cfg.swordSwing = false;
		assertFalse(cfg.swingEnabled(ToolKind.SWORD));
		cfg.keepVanillaSwingOnUnknownItems = true;
		assertFalse(cfg.swingEnabled(ToolKind.GENERIC));
		assertFalse(cfg.swingEnabled(ToolKind.BLOCK));
		assertTrue(cfg.swingEnabled(ToolKind.AXE));
	}
}
