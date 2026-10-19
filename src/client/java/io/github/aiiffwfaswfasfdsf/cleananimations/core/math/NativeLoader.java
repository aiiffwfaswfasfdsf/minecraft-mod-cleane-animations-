package io.github.aiiffwfaswfasfdsf.cleananimations.core.math;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Extracts the bundled Rust library from the jar and activates {@link NativeAnimMath}.
 * Any failure logs one INFO line and leaves {@link JavaAnimMath} active; it never throws.
 */
public final class NativeLoader {
	private static final Logger LOGGER = LoggerFactory.getLogger("cleananimations");
	/** Optional override (absolute path to a library file), used by tests and development. */
	public static final String OVERRIDE_PROPERTY = "cleananimations.nativeLib";

	private NativeLoader() {
	}

	/** Tries to load the native library and installs the result into {@link AnimMath#get()}. */
	public static void init() {
		try {
			AnimMath math = tryLoadNative();
			AnimMathHolder.set(math);
			LOGGER.info("Clean Animations: using native (Rust) animation math");
		} catch (Throwable t) {
			AnimMathHolder.set(new JavaAnimMath());
			LOGGER.info("Clean Animations: native math unavailable (" + describe(t) + "), using Java fallback");
		}
	}

	/** Attempts to create a native implementation; throws when impossible. For tests. */
	public static NativeAnimMath tryLoadNative() throws IOException {
		return new NativeAnimMath(resolveLibrary());
	}

	private static Path resolveLibrary() throws IOException {
		String override = System.getProperty(OVERRIDE_PROPERTY);
		if (override != null && !override.isBlank() && Files.isRegularFile(Path.of(override))) {
			return Path.of(override);
		}
		String dir = platformDir();
		String file = libraryFileName();
		String resource = "/natives/" + dir + "/" + file;
		try (InputStream in = NativeLoader.class.getResourceAsStream(resource)) {
			if (in == null) {
				throw new IOException("no bundled library at " + resource);
			}
			Path tmp = Files.createTempDirectory("cleananimations-native");
			Path target = tmp.resolve(file);
			Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
			target.toFile().deleteOnExit();
			tmp.toFile().deleteOnExit();
			return target;
		}
	}

	/** {@code <os>-<arch>}, e.g. {@code linux-x86_64}, {@code macos-aarch64}, {@code windows-x86_64}. */
	public static String platformDir() {
		String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
		String arch = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
		String o = os.contains("win") ? "windows" : os.contains("mac") ? "macos" : "linux";
		String a = (arch.equals("aarch64") || arch.equals("arm64")) ? "aarch64" : "x86_64";
		return o + "-" + a;
	}

	private static String libraryFileName() {
		return switch (platformDir().substring(0, platformDir().indexOf('-'))) {
			case "windows" -> "cleananimations_native.dll";
			case "macos" -> "libcleananimations_native.dylib";
			default -> "libcleananimations_native.so";
		};
	}

	private static String describe(Throwable t) {
		String m = t.getMessage();
		return t.getClass().getSimpleName() + (m == null ? "" : ": " + m);
	}
}
