package io.github.aiiffwfaswfasfdsf.cleananimations.swing;

import java.util.IdentityHashMap;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.BrushItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.MaceItem;
import net.minecraft.world.item.ShearsItem;
import net.minecraft.world.item.TridentItem;

/**
 * What kind of tool a held item is. {@link #animation} names the animation file
 * ({@code assets/cleananimations/animations/swing/<animation>.json}); kinds without one keep the vanilla swing
 * and only get the equip and idle animations.
 */
public enum ToolKind {
	SWORD("sword", true),
	AXE("axe", true),
	PICKAXE("pickaxe", true),
	SHOVEL("shovel", true),
	HOE("hoe", true),
	SPEAR("spear", true),
	MACE("mace", true),
	SHEARS("shears", true),
	BRUSH("brush", true),
	BLOCK("block", true),
	GENERIC("generic", true),
	FISHING_ROD(null, false),
	BOW(null, false),
	CROSSBOW(null, false),
	FOOD(null, false),
	POTION(null, false);

	/** Name of the animation file, or null when the vanilla swing is kept. */
	public final String animation;
	public final boolean customSwing;

	ToolKind(String animation, boolean customSwing) {
		this.animation = animation;
		this.customSwing = customSwing;
	}

	/** Kinds that have an animation file, in loading order. */
	public static ToolKind[] animated() {
		return java.util.Arrays.stream(values()).filter(k -> k.animation != null).toArray(ToolKind[]::new);
	}

	private static final Map<Item, ToolKind> CACHE = new IdentityHashMap<>();

	/** Drops cached classifications (tags may change on reload). */
	public static void clearCache() {
		CACHE.clear();
	}

	public static ToolKind of(ItemStack stack) {
		if (stack.isEmpty()) {
			return GENERIC;
		}
		return CACHE.computeIfAbsent(stack.getItem(), item -> classify(stack));
	}

	private static ToolKind classify(ItemStack stack) {
		Item item = stack.getItem();
		if (stack.is(ItemTags.SWORDS)) {
			return SWORD;
		}
		if (stack.is(ItemTags.AXES)) {
			return AXE;
		}
		if (stack.is(ItemTags.PICKAXES)) {
			return PICKAXE;
		}
		if (stack.is(ItemTags.SHOVELS)) {
			return SHOVEL;
		}
		if (stack.is(ItemTags.HOES)) {
			return HOE;
		}
		if (stack.is(ItemTags.SPEARS) || item instanceof TridentItem) {
			return SPEAR;
		}
		if (item instanceof MaceItem) {
			return MACE;
		}
		if (item instanceof ShearsItem) {
			return SHEARS;
		}
		if (item instanceof BrushItem) {
			return BRUSH;
		}
		if (item instanceof FishingRodItem) {
			return FISHING_ROD;
		}
		if (item instanceof BowItem) {
			return BOW;
		}
		if (item instanceof CrossbowItem) {
			return CROSSBOW;
		}
		if (stack.has(DataComponents.POTION_CONTENTS)) {
			return POTION;
		}
		if (stack.has(DataComponents.FOOD)) {
			return FOOD;
		}
		if (item instanceof BlockItem) {
			return BLOCK;
		}
		return GENERIC;
	}
}
