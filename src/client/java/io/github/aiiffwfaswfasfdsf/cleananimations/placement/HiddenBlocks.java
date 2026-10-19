package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;

/**
 * Positions that the chunk mesher must treat as air while their placement animation runs. Read from chunk build
 * worker threads (vanilla and Sodium), written on the main thread, hence the concurrent set.
 */
public final class HiddenBlocks {
	private static final Set<Long> HIDDEN = ConcurrentHashMap.newKeySet();

	private HiddenBlocks() {
	}

	/** Cheap check used by the mixins before they box a position. */
	public static boolean any() {
		return !HIDDEN.isEmpty();
	}

	public static boolean isHidden(BlockPos pos) {
		return !HIDDEN.isEmpty() && HIDDEN.contains(pos.asLong());
	}

	public static boolean isHidden(int x, int y, int z) {
		return !HIDDEN.isEmpty() && HIDDEN.contains(BlockPos.asLong(x, y, z));
	}

	static void hide(long pos) {
		HIDDEN.add(pos);
	}

	static void show(long pos) {
		HIDDEN.remove(pos);
	}

	static void clear() {
		HIDDEN.clear();
	}

	/** Number of currently hidden positions (used by the game test: must be 0 once animations are over). */
	public static int size() {
		return HIDDEN.size();
	}
}
