package io.github.aiiffwfaswfasfdsf.cleananimations.core.config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/** Reflection view of one annotated config field. */
public final class ConfigField {
	public enum Kind { TOGGLE, SLIDER, CHOICE }

	private final String sectionId;
	private final ConfigSection section;
	private final Field field;
	private final Kind kind;
	private final double min;
	private final double max;
	private final double step;
	private final List<String> choices;

	private ConfigField(String sectionId, ConfigSection section, Field field, Kind kind,
			double min, double max, double step, List<String> choices) {
		this.sectionId = sectionId;
		this.section = section;
		this.field = field;
		this.kind = kind;
		this.min = min;
		this.max = max;
		this.step = step;
		this.choices = choices;
		field.setAccessible(true);
	}

	/** Lists all annotated fields of a section in declaration order. */
	public static List<ConfigField> of(String sectionId, ConfigSection section) {
		List<ConfigField> out = new ArrayList<>();
		for (Field f : section.getClass().getDeclaredFields()) {
			Toggle t = f.getAnnotation(Toggle.class);
			Slider s = f.getAnnotation(Slider.class);
			Choice c = f.getAnnotation(Choice.class);
			if (t != null && (f.getType() == boolean.class)) {
				out.add(new ConfigField(sectionId, section, f, Kind.TOGGLE, 0, 1, 1, List.of()));
			} else if (s != null && isNumber(f.getType())) {
				out.add(new ConfigField(sectionId, section, f, Kind.SLIDER, s.min(), s.max(), s.step(), List.of()));
			} else if (c != null) {
				List<String> values = new ArrayList<>();
				if (f.getType().isEnum()) {
					for (Object o : f.getType().getEnumConstants()) {
						values.add(((Enum<?>) o).name());
					}
				} else if (f.getType() == String.class) {
					values.addAll(List.of(c.values()));
				}
				if (!values.isEmpty()) {
					out.add(new ConfigField(sectionId, section, f, Kind.CHOICE, 0, 0, 0, List.copyOf(values)));
				}
			}
		}
		return out;
	}

	private static boolean isNumber(Class<?> type) {
		return type == float.class || type == double.class || type == int.class;
	}

	public String sectionId() { return sectionId; }

	public String name() { return field.getName(); }

	public Kind kind() { return kind; }

	public double min() { return min; }

	public double max() { return max; }

	public double step() { return step; }

	public List<String> choices() { return choices; }

	/** Translation key of the label. */
	public String labelKey() { return "cleananimations.config." + sectionId + "." + field.getName(); }

	public String tooltipKey() { return labelKey() + ".tooltip"; }

	public String choiceKey(String value) { return labelKey() + "." + value.toLowerCase(java.util.Locale.ROOT); }

	public boolean getBoolean() {
		try {
			return field.getBoolean(section);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public void setBoolean(boolean v) {
		try {
			field.setBoolean(section, v);
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	public double getNumber() {
		try {
			return ((Number) field.get(section)).doubleValue();
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Clamps, snaps to the step and stores the value, converting to the field's numeric type. */
	public void setNumber(double v) {
		v = normalize(v);
		try {
			Class<?> t = field.getType();
			if (t == float.class) {
				field.setFloat(section, (float) v);
			} else if (t == int.class) {
				field.setInt(section, (int) Math.round(v));
			} else {
				field.setDouble(section, v);
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	private double normalize(double v) {
		if (Double.isNaN(v)) {
			v = min;
		}
		if (step > 0) {
			v = min + Math.round((v - min) / step) * step;
		}
		return Math.max(min, Math.min(max, v));
	}

	/** Current choice value as its constant name / string. */
	public String getChoice() {
		try {
			Object o = field.get(section);
			return o == null ? choices.get(0) : (o instanceof Enum<?> e ? e.name() : o.toString());
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public void setChoice(String value) {
		if (!choices.contains(value)) {
			return;
		}
		try {
			if (field.getType().isEnum()) {
				field.set(section, Enum.valueOf((Class<Enum>) field.getType(), value));
			} else {
				field.set(section, value);
			}
		} catch (IllegalAccessException e) {
			throw new IllegalStateException(e);
		}
	}

	/** Repairs an out-of-range or invalid value loaded from disk. */
	void sanitize() {
		switch (kind) {
			case SLIDER -> setNumber(getNumber());
			case CHOICE -> {
				try {
					Object o = field.get(section);
					String cur = o == null ? null : (o instanceof Enum<?> e ? e.name() : o.toString());
					if (cur == null || !choices.contains(cur)) {
						setChoice(choices.get(0));
					}
				} catch (IllegalAccessException e) {
					throw new IllegalStateException(e);
				}
			}
			default -> { }
		}
	}
}
