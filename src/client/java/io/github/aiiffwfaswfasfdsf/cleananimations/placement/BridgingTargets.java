package io.github.aiiffwfaswfasfdsf.cleananimations.placement;

/**
 * Pure target selection of the bridging assist. Given where the player looks and which cells are solid / free, it
 * finds the free cell next to a solid block (the "gap") that the view ray passes closest to, restricted to the layers
 * around the player's feet so it only ever helps bridging, never building in mid air.
 */
public final class BridgingTargets {
	/** Face index order equals {@code Direction} ordinals: DOWN, UP, NORTH, SOUTH, WEST, EAST. */
	public static final int[][] NORMALS = {{0, -1, 0}, {0, 1, 0}, {0, 0, -1}, {0, 0, 1}, {-1, 0, 0}, {1, 0, 0}};
	/** Maximum distance between the view ray and the centre of the chosen cell. */
	public static final double MAX_RAY_DISTANCE = 0.8;

	private BridgingTargets() {
	}

	/** Read access to the world used by the selection. */
	public interface World {
		boolean solid(int x, int y, int z);

		boolean replaceable(int x, int y, int z);
	}

	/**
	 * @param cx,cy,cz cell the block ends up in
	 * @param sx,sy,sz the solid support block that gets clicked
	 * @param face     face index of the support that is clicked (points from the support into the cell)
	 * @param hx,hy,hz exact click position (centre of the support face)
	 */
	public record Target(int cx, int cy, int cz, int sx, int sy, int sz, int face, double hx, double hy, double hz) {
	}

	/**
	 * @param ex,ey,ez eye position
	 * @param lx,ly,lz look direction (need not be normalised)
	 * @param reach    block interaction range
	 * @param feetY    block Y of the player's feet
	 * @return the best target or {@code null}
	 */
	public static Target select(double ex, double ey, double ez, double lx, double ly, double lz, double reach,
								int feetY, World world) {
		double len = Math.sqrt(lx * lx + ly * ly + lz * lz);
		if (len < 1e-6) {
			return null;
		}
		lx /= len;
		ly /= len;
		lz /= len;
		int r = (int) Math.ceil(reach);
		int bx = (int) Math.floor(ex), bz = (int) Math.floor(ez);
		Target best = null;
		double bestScore = Double.MAX_VALUE;
		for (int y = feetY - 2; y <= feetY; y++) {
			for (int x = bx - r; x <= bx + r; x++) {
				for (int z = bz - r; z <= bz + r; z++) {
					if (!world.replaceable(x, y, z) || world.solid(x, y, z)) {
						continue;
					}
					double vx = x + 0.5 - ex, vy = y + 0.5 - ey, vz = z + 0.5 - ez;
					double t = vx * lx + vy * ly + vz * lz;
					if (t <= 0.0 || t > reach + 0.87) {
						continue;
					}
					double px = vx - t * lx, py = vy - t * ly, pz = vz - t * lz;
					double d = Math.sqrt(px * px + py * py + pz * pz);
					if (d > MAX_RAY_DISTANCE) {
						continue;
					}
					int face = chooseFace(x, y, z, world);
					if (face < 0) {
						continue;
					}
					int[] n = NORMALS[face];
					double hx = x + 0.5 - n[0] * 0.5, hy = y + 0.5 - n[1] * 0.5, hz = z + 0.5 - n[2] * 0.5;
					double hdx = hx - ex, hdy = hy - ey, hdz = hz - ez;
					if (hdx * hdx + hdy * hdy + hdz * hdz > reach * reach) {
						continue;
					}
					// prefer cells close to the ray, then closer ones, and a support below the cell over side supports
					double score = d + 0.04 * t + (face == 1 ? 0.0 : 0.12);
					if (score < bestScore) {
						bestScore = score;
						best = new Target(x, y, z, x - n[0], y - n[1], z - n[2], face, hx, hy, hz);
					}
				}
			}
		}
		return best;
	}

	/** Face index (pointing from the support into the cell) of the preferred solid neighbour, or -1. */
	private static int chooseFace(int x, int y, int z, World w) {
		// 1 = UP: support below the cell. Then the four sides. DOWN (support above) is the last resort.
		int[] order = {1, 2, 3, 4, 5, 0};
		for (int face : order) {
			int[] n = NORMALS[face];
			if (w.solid(x - n[0], y - n[1], z - n[2])) {
				return face;
			}
		}
		return -1;
	}
}
