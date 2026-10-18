package io.github.aiiffwfaswfasfdsf.cleananimations.core.math;

import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.file.Path;

/**
 * Rust implementation called through the Java FFM API (no JNI, no preview flags).
 * The constructor throws on any problem (missing symbol, wrong ABI version, restricted access denied);
 * {@link NativeLoader} then falls back to {@link JavaAnimMath}.
 */
public final class NativeAnimMath implements AnimMath {
	private static final int ABI_VERSION = 1;
	/** Max keyframes handled natively per call; larger tracks use the Java path. */
	private static final int MAX_KEYS = 256;
	private static final long SCRATCH_BYTES = 4L * (8 + MAX_KEYS * 3);

	private static final ValueLayout.OfFloat F = ValueLayout.JAVA_FLOAT;
	private static final ValueLayout.OfInt I = ValueLayout.JAVA_INT;

	private final MethodHandle ease;
	private final MethodHandle spring;
	private final MethodHandle track;
	private final MethodHandle place;
	private final JavaAnimMath fallback = new JavaAnimMath();
	private final Arena arena = Arena.ofAuto();
	private final ThreadLocal<MemorySegment> scratch = ThreadLocal.withInitial(
			() -> arena.allocate(SCRATCH_BYTES, 8));

	public NativeAnimMath(Path library) {
		Linker linker = Linker.nativeLinker();
		SymbolLookup lookup = SymbolLookup.libraryLookup(library, arena);
		MethodHandle version = linker.downcallHandle(
				lookup.find("ca_version").orElseThrow(),
				FunctionDescriptor.of(I));
		try {
			int v = (int) version.invokeExact();
			if (v != ABI_VERSION) {
				throw new IllegalStateException("native ABI version " + v + " != " + ABI_VERSION);
			}
		} catch (RuntimeException | Error e) {
			throw e;
		} catch (Throwable t) {
			throw new IllegalStateException(t);
		}
		ease = linker.downcallHandle(lookup.find("ca_ease").orElseThrow(),
				FunctionDescriptor.of(F, I, F));
		spring = linker.downcallHandle(lookup.find("ca_spring_step").orElseThrow(),
				FunctionDescriptor.ofVoid(F, F, F, F, F, F, ValueLayout.ADDRESS));
		track = linker.downcallHandle(lookup.find("ca_sample_track").orElseThrow(),
				FunctionDescriptor.of(F, ValueLayout.ADDRESS, ValueLayout.ADDRESS, ValueLayout.ADDRESS, I, F));
		place = linker.downcallHandle(lookup.find("ca_place_transform").orElseThrow(),
				FunctionDescriptor.ofVoid(I, F, F, F, F, ValueLayout.ADDRESS));
		// Smoke test so that a broken binding fails here and not in the render loop.
		ease(EASE_LINEAR, 0.5f);
	}

	@Override
	public String name() {
		return "native";
	}

	@Override
	public boolean isNative() {
		return true;
	}

	@Override
	public float ease(int kind, float t) {
		try {
			return (float) ease.invokeExact(kind, t);
		} catch (Throwable e) {
			throw rethrow(e);
		}
	}

	@Override
	public void springStep(float pos, float vel, float target, float stiffness, float damping, float dt, float[] out) {
		MemorySegment s = scratch.get();
		try {
			spring.invokeExact(pos, vel, target, stiffness, damping, dt, s);
		} catch (Throwable e) {
			throw rethrow(e);
		}
		out[0] = s.getAtIndex(F, 0);
		out[1] = s.getAtIndex(F, 1);
	}

	@Override
	public float sampleTrack(float[] times, float[] values, int[] easing, int count, float t) {
		int n = Math.min(count, Math.min(times.length, values.length));
		if (n <= 0) {
			return 0.0f;
		}
		if (n > MAX_KEYS) {
			return fallback.sampleTrack(times, values, easing, count, t);
		}
		MemorySegment s = scratch.get();
		long timesOff = 0;
		long valuesOff = 4L * MAX_KEYS;
		long easingOff = 8L * MAX_KEYS;
		MemorySegment.copy(times, 0, s.asSlice(timesOff), F, 0, n);
		MemorySegment.copy(values, 0, s.asSlice(valuesOff), F, 0, n);
		for (int i = 0; i < n - 1; i++) {
			s.set(I, easingOff + 4L * i, (easing != null && i < easing.length) ? easing[i] : 0);
		}
		try {
			return (float) track.invokeExact(s.asSlice(timesOff), s.asSlice(valuesOff), s.asSlice(easingOff), n, t);
		} catch (Throwable e) {
			throw rethrow(e);
		}
	}

	@Override
	public void placeTransform(int style, float progress, float dirX, float dirY, float dirZ, float[] out) {
		MemorySegment s = scratch.get();
		try {
			place.invokeExact(style, progress, dirX, dirY, dirZ, s);
		} catch (Throwable e) {
			throw rethrow(e);
		}
		for (int i = 0; i < PLACE_TRANSFORM_SIZE; i++) {
			out[i] = s.getAtIndex(F, i);
		}
	}

	private static RuntimeException rethrow(Throwable t) {
		if (t instanceof RuntimeException r) {
			return r;
		}
		if (t instanceof Error e) {
			throw e;
		}
		return new IllegalStateException(t);
	}
}
