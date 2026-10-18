package io.github.aiiffwfaswfasfdsf.cleananimations.core.screen;

import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.CleanConfig;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.ConfigField;
import io.github.aiiffwfaswfasfdsf.cleananimations.core.config.ConfigSection;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.locale.Language;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Generic config screen built by reflection over {@link CleanConfig}: one tab per section, one row
 * per annotated field. Labels come from {@code cleananimations.config.<section>.<field>} (+ {@code .tooltip}).
 */
public class CleanConfigScreen extends Screen {
	private static final int ROW_W = 220;
	private static final int ROW_H = 20;
	private static final int ROW_GAP = 4;
	private static final int TOP = 56;
	private static final int BOTTOM_RESERVED = 34;

	private final Screen parent;
	private final CleanConfig config = CleanConfig.get();
	private String activeSection;
	private double scroll;
	private final List<AbstractWidget> rows = new ArrayList<>();

	public CleanConfigScreen(Screen parent) {
		super(Component.translatable("cleananimations.config.title"));
		this.parent = parent;
		this.activeSection = config.sections().keySet().iterator().next();
	}

	@Override
	protected void init() {
		rows.clear();
		int x = this.width / 2;

		// Section tabs.
		Map<String, ConfigSection> sections = config.sections();
		int tabW = 70;
		int tabsW = sections.size() * (tabW + 4) - 4;
		int tx = x - tabsW / 2;
		for (String id : sections.keySet()) {
			Button tab = Button.builder(Component.translatable("cleananimations.config.section." + id), b -> {
				activeSection = id;
				scroll = 0;
				rebuildWidgets();
			}).bounds(tx, 28, tabW, ROW_H).build();
			tab.active = !id.equals(activeSection);
			addRenderableWidget(tab);
			tx += tabW + 4;
		}

		// Field rows of the active section.
		for (ConfigField f : ConfigField.of(activeSection, sections.get(activeSection))) {
			AbstractWidget w = createWidget(f, x - ROW_W / 2);
			if (Language.getInstance().has(f.tooltipKey())) {
				w.setTooltip(Tooltip.create(Component.translatable(f.tooltipKey())));
			}
			rows.add(w);
			addRenderableWidget(w);
		}
		layoutRows();

		addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, b -> onClose())
				.bounds(x - 100, this.height - 28, 200, ROW_H).build());
	}

	private AbstractWidget createWidget(ConfigField f, int x) {
		Component label = Component.translatable(f.labelKey());
		switch (f.kind()) {
			case TOGGLE:
				return CycleButton.onOffBuilder(f.getBoolean())
						.create(x, 0, ROW_W, ROW_H, label, (btn, v) -> f.setBoolean(v));
			case CHOICE:
				return CycleButton.<String>builder(v -> Component.translatable(f.choiceKey(v)), f.getChoice())
						.withValues(f.choices())
						.create(x, 0, ROW_W, ROW_H, label, (btn, v) -> f.setChoice(v));
			default:
				return new FieldSlider(x, ROW_W, ROW_H, f, label);
		}
	}

	private int contentHeight() {
		return rows.size() * (ROW_H + ROW_GAP);
	}

	private int viewportHeight() {
		return this.height - TOP - BOTTOM_RESERVED;
	}

	private void layoutRows() {
		double max = Math.max(0, contentHeight() - viewportHeight());
		scroll = Math.max(0, Math.min(max, scroll));
		int y = TOP - (int) scroll;
		for (AbstractWidget w : rows) {
			w.setY(y);
			w.visible = y >= TOP - 2 && y + ROW_H <= TOP + viewportHeight() + 2;
			y += ROW_H + ROW_GAP;
		}
	}

	@Override
	public boolean mouseScrolled(double mx, double my, double sx, double sy) {
		scroll -= sy * (ROW_H + ROW_GAP);
		layoutRows();
		return true;
	}

	@Override
	public void extractRenderState(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		super.extractRenderState(g, mouseX, mouseY, partialTick);
		g.centeredText(this.font, this.title, this.width / 2, 10, 0xFFFFFFFF);
		if (rows.isEmpty()) {
			g.centeredText(this.font, Component.translatable("cleananimations.config.empty"),
					this.width / 2, TOP + 20, 0xFFAAAAAA);
		}
	}

	@Override
	public void onClose() {
		config.save();
		this.minecraft.gui.setScreen(parent);
	}

	/** Slider bound to a numeric config field. */
	private static final class FieldSlider extends AbstractSliderButton {
		private final ConfigField field;
		private final Component label;

		FieldSlider(int x, int w, int h, ConfigField field, Component label) {
			super(x, 0, w, h, Component.empty(), toUnit(field, field.getNumber()));
			this.field = field;
			this.label = label;
			updateMessage();
		}

		private static double toUnit(ConfigField f, double v) {
			double range = f.max() - f.min();
			return range <= 0 ? 0 : (v - f.min()) / range;
		}

		@Override
		protected void updateMessage() {
			double v = field.min() + this.value * (field.max() - field.min());
			String text = field.step() >= 1 || field.min() == Math.rint(field.min()) && field.step() == 0
					? Long.toString(Math.round(v)) : String.format(java.util.Locale.ROOT, "%.2f", v);
			setMessage(Component.empty().append(label).append(": " + text));
		}

		@Override
		protected void applyValue() {
			field.setNumber(field.min() + this.value * (field.max() - field.min()));
		}
	}
}
