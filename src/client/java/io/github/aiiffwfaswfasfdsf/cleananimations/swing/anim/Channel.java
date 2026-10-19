package io.github.aiiffwfaswfasfdsf.cleananimations.swing.anim;

/** The animated channels of a clip. Positions are blocks, rotations degrees (view-space of the hand). */
public enum Channel {
	POS_X("posX", 0f),
	POS_Y("posY", 0f),
	POS_Z("posZ", 0f),
	ROT_X("rotX", 0f),
	ROT_Y("rotY", 0f),
	ROT_Z("rotZ", 0f),
	SCALE("scale", 1f),
	/** Roll of the whole first-person view of the hand in degrees ("body lean"). */
	LEAN("lean", 0f);

	public static final int COUNT = values().length;

	public final String json;
	/** Value of the channel when a clip does not animate it. */
	public final float rest;

	Channel(String json, float rest) {
		this.json = json;
		this.rest = rest;
	}

	public static Channel byName(String name) {
		for (Channel c : values()) {
			if (c.json.equals(name)) {
				return c;
			}
		}
		return null;
	}
}
