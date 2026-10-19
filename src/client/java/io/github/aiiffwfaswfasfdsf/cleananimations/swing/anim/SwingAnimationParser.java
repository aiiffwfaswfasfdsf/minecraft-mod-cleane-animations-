package io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.math.AnimMath;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Parses the animation JSON format:
 * <pre>
 * { "duration": 8,
 *   "channels": { "rotX": [ {"t":0,"v":0,"ease":"out_cubic"}, {"t":1,"v":0} ] },
 *   "combo":  [ { "duration": 8, "channels": {...} }, ... ],
 *   "mining": { "duration": 10, "channels": {...} },
 *   "shake":  { "at": 0.6, "amplitude": 0.02, "ticks": 5 } }
 * </pre>
 * {@code t} is normalised clip progress, {@code ease} shapes the segment that STARTS at that key.
 * The parser is tolerant: bad keys are skipped, but a structurally broken document throws {@link IllegalArgumentException}.
 */
public final class SwingAnimationParser {
	/** Number of linear pieces an expanded in-curve is cut into. */
	private static final int EXPAND_STEPS = 8;

	private SwingAnimationParser() {
	}

	public static SwingAnimation parse(String json) {
		return parse(new StringReader(json));
	}

	public static SwingAnimation parse(Reader reader) {
		JsonElement root;
		try {
			root = JsonParser.parseReader(reader);
		} catch (RuntimeException e) {
			throw new IllegalArgumentException("invalid JSON: " + e.getMessage(), e);
		}
		if (root == null || !root.isJsonObject()) {
			throw new IllegalArgumentException("animation must be a JSON object");
		}
		JsonObject obj = root.getAsJsonObject();

		Clip attack = obj.has("channels") ? parseClip(obj, 8f) : null;
		List<Clip> combo = new ArrayList<>();
		if (obj.has("combo") && obj.get("combo").isJsonArray()) {
			float inherited = attack != null ? attack.durationTicks() : floatOf(obj, "duration", 8f);
			for (JsonElement e : obj.getAsJsonArray("combo")) {
				if (e.isJsonObject()) {
					combo.add(parseClip(e.getAsJsonObject(), inherited));
				}
			}
		}
		if (attack == null) {
			if (combo.isEmpty()) {
				throw new IllegalArgumentException("animation needs 'channels' or a non-empty 'combo'");
			}
			attack = combo.get(0);
		}
		Clip mining = null;
		if (obj.has("mining") && obj.get("mining").isJsonObject()) {
			mining = parseClip(obj.getAsJsonObject("mining"), 10f);
		}
		SwingAnimation.Shake shake = null;
		if (obj.has("shake") && obj.get("shake").isJsonObject()) {
			JsonObject s = obj.getAsJsonObject("shake");
			shake = new SwingAnimation.Shake(floatOf(s, "at", 0.6f), floatOf(s, "amplitude", 0.02f),
					Math.max(1f, floatOf(s, "ticks", 5f)));
		}
		return new SwingAnimation(attack, List.copyOf(combo), mining, shake);
	}

	private static Clip parseClip(JsonObject obj, float defaultDuration) {
		float duration = floatOf(obj, "duration", defaultDuration);
		if (!(duration > 0f)) {
			throw new IllegalArgumentException("duration must be > 0");
		}
		Track[] tracks = new Track[Channel.COUNT];
		if (obj.has("channels") && obj.get("channels").isJsonObject()) {
			JsonObject ch = obj.getAsJsonObject("channels");
			for (String name : ch.keySet()) {
				Channel c = Channel.byName(name);
				if (c == null || !ch.get(name).isJsonArray()) {
					continue; // unknown channels are ignored so packs can carry extra data
				}
				Track t = parseTrack(ch.getAsJsonArray(name));
				if (t != null) {
					tracks[c.ordinal()] = t;
				}
			}
		}
		return new Clip(duration, tracks);
	}

	private record Key(float t, float v, String ease) {
	}

	private static Track parseTrack(JsonArray array) {
		List<Key> keys = new ArrayList<>();
		for (JsonElement e : array) {
			if (!e.isJsonObject()) {
				continue;
			}
			JsonObject k = e.getAsJsonObject();
			if (!k.has("t") || !k.has("v")) {
				continue;
			}
			float t = Math.max(0f, Math.min(1f, floatOf(k, "t", 0f)));
			float v = floatOf(k, "v", 0f);
			String ease = k.has("ease") && k.get("ease").isJsonPrimitive() ? k.get("ease").getAsString() : "linear";
			keys.add(new Key(t, v, ease));
		}
		if (keys.isEmpty()) {
			return null;
		}
		keys.sort(Comparator.comparingDouble(Key::t)); // stable: equal times keep file order
		List<Float> times = new ArrayList<>();
		List<Float> values = new ArrayList<>();
		List<Integer> easing = new ArrayList<>();
		for (int i = 0; i < keys.size(); i++) {
			Key k = keys.get(i);
			int core = Ease.coreId(k.ease());
			boolean hasNext = i + 1 < keys.size();
			if (core == Ease.EXPANDED && hasNext) {
				Key n = keys.get(i + 1);
				times.add(k.t());
				values.add(k.v());
				easing.add(AnimMath.EASE_LINEAR);
				for (int s = 1; s < EXPAND_STEPS; s++) {
					float u = s / (float) EXPAND_STEPS;
					times.add(k.t() + (n.t() - k.t()) * u);
					values.add(k.v() + (n.v() - k.v()) * Ease.inCurve(k.ease(), u));
					easing.add(AnimMath.EASE_LINEAR);
				}
			} else {
				times.add(k.t());
				values.add(k.v());
				easing.add(core >= 0 ? core : AnimMath.EASE_LINEAR);
			}
		}
		float[] ts = new float[times.size()];
		float[] vs = new float[times.size()];
		int[] es = new int[times.size()];
		for (int i = 0; i < ts.length; i++) {
			ts[i] = times.get(i);
			vs[i] = values.get(i);
			es[i] = easing.get(i);
		}
		return new Track(ts, vs, es);
	}

	private static float floatOf(JsonObject o, String key, float def) {
		if (!o.has(key) || !o.get(key).isJsonPrimitive()) {
			return def;
		}
		try {
			return o.get(key).getAsFloat();
		} catch (RuntimeException e) {
			return def;
		}
	}
}
