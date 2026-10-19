# Clean Animations

A client-side Fabric mod for Minecraft **26.3** that adds smooth, configurable animations:
sword and tool swings, block placement effects and small HUD touches. It is client-only, so it can be
used on any server.

> Status: skeleton. The build, configuration screen, native math core and test setup are in place;
> the actual features land in the next commits.

This is a **clean-room implementation**. It is inspired by the idea behind mods like Punchy,
Accurate Block Placement and Bridging Mod, but it shares no code with them.

## Install

1. Install [Fabric Loader](https://fabricmc.net/use/installer/) **0.19.5 or newer** for Minecraft 26.3.
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) **0.163.0+26.3**.
3. Use **Java 25** (required by Minecraft 26.x).
4. Drop `cleananimations-<version>.jar` into your `.minecraft/mods` folder.

[Mod Menu](https://modrinth.com/mod/modmenu) is optional. Without it, press **K** (rebindable under
Controls > Clean Animations) to open the config screen.

The jar bundles Rust native libraries for Linux, Windows and macOS (x86_64 and aarch64 where available).
If a native library cannot be loaded, the mod silently falls back to equivalent pure-Java math and logs a
single INFO line. Depending on your Java version you may see a one-time warning about native access; it is harmless.

## Planned features

All three sections are **coming in the next commits**.

### Swing / Animations
- Smooth first-person swing and item-use animations with adjustable speed and intensity.

### Placement
- Block placement animations (Settle, Pop, Slide, Drop).

### HUD
- Small animated HUD polish (hotbar and item feedback).

Everything respects the global master toggle, animation speed (0.5x - 2.0x) and intensity (0 - 2.0).

## Building

```
./gradlew build
```

The jar is written to `build/libs/cleananimations-0.1.0.jar`. Java 25 is needed; if it is not installed,
Gradle can download it automatically via the toolchain resolver.

Rust is **optional**. If `cargo` is on the PATH, `./gradlew build` compiles `native/` and bundles the library
for your host platform; otherwise the jar is built without it and uses the Java fallback.

```
cd native && cargo test          # unit tests of the animation math
./gradlew test                   # JUnit tests, including native-vs-Java parity (skipped without the library)
./gradlew runClientGameTest      # end-to-end client game test, screenshots in build/run/clientGameTest/screenshots
```

Headless Linux (no GPU): Minecraft 26.x requests an sRGB-capable framebuffer, which Xvfb's GLX does not offer.
Use SDL's offscreen driver with Mesa software rendering instead:

```
SDL_VIDEODRIVER=offscreen LIBGL_ALWAYS_SOFTWARE=1 XDG_RUNTIME_DIR=/tmp/xdg ./gradlew runClientGameTest
```

CI (`.github/workflows/build.yml`) builds the native libraries on every platform, bundles them into the jar
and attaches the jar to a GitHub Release for tags named `v*`.

## Adding a feature module

Each feature owns its own package, mixin config, config section and lang keys, so features never touch the
same files. See `src/client/java/io/github/aiiffwfaswfasfdsf/cleananimations/{swing,placement,hud}`.

## License

[Eclipse Public License 2.0](LICENSE) (EPL-2.0).
