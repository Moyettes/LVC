# Risugami's ModLoader b1.7.3 module

Risugami's ModLoader sits on top of the same RetroMCP toolchain as the `rawretromcp-b1.7.3` module — `RetroMCP-GUI-all.jar` for compile/reobf/repack. The extra wrinkle is `Risugami's modloader.jar` at the project root and `ModLoader.java` patched into `net.minecraft.src.*`. The voice mod entry point is `mod_Voice extends BaseMod`; Risugami auto-registers any `mod_*` class on the classpath.

So this module produces a **source bundle** instead of a compiled jar — same approach as `rawretromcp-b1.7.3`.

## What's in the bundle

The zip extracts directly over a Risugami RetroMCP project root:

```
LegacyVoiceChat-0.1.0-RisugamiModloader-source.zip
├── patches/
│   └── client.patch                    ← MC class modifications + initial voice snapshot
└── minecraft/
    └── src/
        └── com/moyettes/voice/...      ← Latest voice source (overlay)
```

`patches/client.patch` is **regenerated on every build** by the `regeneratePatch` Gradle task — it uses [`codechicken:DiffPatch`](https://github.com/MCPHackers/DiffPatch), the exact same library RetroMCP-GUI uses for its "Create Patch" button. The task diffs the standalone's pristine `minecraft/src_original/` tree against the modified `minecraft/src/` tree and writes a single `client.patch`. The patch contains MC class modifications (`KeyBinding` replaced by Risugami's, `RenderPlayer`, `GuiIngame`, `NetworkManager`, `Packet`, `NetClientHandler`, plus Risugami's own `BaseMod`/`ModLoader` and `mod_Voice`) plus a voice-source snapshot.

**To add a new MC modification**: edit the file in `All Versions/Risugami's ModLoader-b1.7.3/minecraft/src/net/minecraft/src/X.java` directly, then re-run `./gradlew :risugami-b1.7.3:bundleSource`. The patch regenerates automatically.

`minecraft/src/com/moyettes/voice/...` is regenerated every build from the shared modules — when `:common`, `:client-common`, or `:server-common` change, the next `bundleSource` invocation picks up the latest source automatically. Files are package-rewritten from `com.moyettes.legacyvoicechat.*` to `com.moyettes.voice.*` to match the standalone's existing layout.

## Building

From the parent `LegacyVoiceChat Compatibility/` directory:

```
./gradlew :risugami-b1.7.3:bundleSource
```

Output: `risugami-b1.7.3/build/libs/LegacyVoiceChat-0.1.0-RisugamiModloader-source.zip`

## Installing — fresh Risugami project

1. Set up a clean RetroMCP project for b1.7.3 with Risugami's modloader.jar dropped into the root (matching the standalone layout under `All Versions/Risugami's ModLoader-b1.7.3/`).
2. Extract the zip at the project root. Patch lands at `patches/client.patch`; voice source lands at `minecraft/src/com/moyettes/voice/...`.
3. In `RetroMCP-GUI-all.jar`, apply `patches/client.patch` (writes MC class modifications, ModLoader, and `mod_Voice` into `minecraft/src/net/minecraft/src/...`, plus a baseline voice source).
4. The voice source from step 3 is overwritten by the overlay extracted in step 2 — that's intentional, you get the latest version of the shared layers.
5. Run the GUI's compile + reobfuscate + repackage workflow.
6. Output `minecraft.jar` now has the voice mod baked in. Risugami picks up `mod_Voice` automatically on launch.

## Installing — updating an existing project

If your project already has the patch applied:

1. Extract just the `minecraft/` directory from the zip (skip `patches/`).
2. Recompile via the GUI.

## Per-loader bridge files

Files specific to Risugami (the `mod_Voice` entry class under `net/minecraft/src/`, `api/client/ClientPlayNetworking`, `api/KeyBindingRegistry`, `api/KeyBindingEvents`, etc.) come from `patches/client.patch` and are NOT re-shipped by the overlay. The standalone at `All Versions/Risugami's ModLoader-b1.7.3/minecraft/src/` is the authoritative reference for editing them — keep editing there and regenerate the patch via RetroMCP-GUI when they need an update.
