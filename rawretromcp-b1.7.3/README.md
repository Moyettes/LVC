# RawRetroMCP b1.7.3 module

Authoring is identical to `:ornithe-b1.7.3:client` — SpongePowered Mixin classes against Feather-named Minecraft plus voice-mod source. The difference is the build output:

- Ornithe players get a standard Fabric mod jar; Fabric loader applies the mixins at runtime.
- RetroMCP players have no Fabric loader in their `minecraft.jar`, so the build bakes the mixins into the MC bytecode at **build time** and emits MCP-named `.class` files for drag-and-drop installation.

## Source layout

```
rawretromcp-b1.7.3/
├── src/main/java/com/moyettes/legacyvoicechat/
│   ├── mixin/                    ← SpongePowered Mixin classes (@Mixin(KeyBinding.class) etc.)
│   ├── api/                      ← KeyBindingEvents, KeyBindingRegistry
│   ├── events/ClientEvents.java  ← runtime event bus
│   ├── utils/MinecraftAccessor.java
│   └── Voice.java                ← ModInitializer entrypoint
├── src/main/resources/
│   ├── fabric.mod.json
│   └── legacyvoicechat.mixins.json
└── mappings/                     ← vendored Calamus + Feather + RetroMCP MCP tiny v2 files
```

No vanilla Minecraft source lives in this folder — Loom + Ploceus fetch and decompile Feather-named MC at build time from the user's Gradle cache. Clone the repo, run `./gradlew :rawretromcp-b1.7.3:build`, done.

## Build pipeline

1. **Compile + shadow** — same as Ornithe. `shadowJar` produces a Feather-named dev jar containing our mixins + voice mod body + opus/rnnoise natives. Log4j and JetBrains/IntelliJ annotation jars are excluded from the shadow output (see below).
2. **Merge mappings** — `mergeMappings` builds a 5-column Tiny v2 `(official, intermediary, feather, mcp, server)` from the three vendored mapping files.
3. **Offline mixin apply** — `applyMixinsOffline` runs a custom ASM transformer (`build-tools/mixin-applier.gradle`) over the Feather MC jar, emitting **only the classes a mixin actually touched** (not a pass-through copy of every MC class). Supported features: `@Mixin(class)` target selection, `@Accessor` (explicit or inferred), `@Shadow` (compile-time only), `@Inject @At("TAIL"|"HEAD"|"INVOKE")`, added fields, added methods with owner rewriting, added interfaces. Callback bodies are copied onto the target class as private `lvc$<Mixin>$<method>` methods. The applier **strips the trailing `CallbackInfo` parameter** from @Inject callback descriptors and omits the `new CallbackInfo(...)` construction at the injection site, so the shipping drop-in has zero `org.spongepowered.*` references — mixins are a build-time concept only. Callbacks that actually consume the `ci` parameter (read isCancelled, call cancel()) are rejected with a clear error; extend the applier if that pattern becomes necessary.
4. **Remap to Mojang obfuscated names** — `remapDropInToObf` runs tiny-remapper feather→official. Modified MC classes land at the exact paths vanilla b1.7.3 `minecraft.jar` uses (`qb.class`, `kv.class`, etc.), so a user drags them in and they replace the original classes one-for-one. Voice-mod-added fields/methods keep their human names (they're not in the mapping).
5. **Drop-in zip** — `buildDropIn` strips `META-INF/` and zips the result.

## What ships in the drop-in

- 5 modified MC classes at root: `kv.class` (GameOptions), `nb.class` (ClientNetworkHandler), `pf.class` (Connection), `qb.class` (KeyBinding), `uq.class` (GameGui)
- 1 modified class in a package: `net/minecraft/client/Minecraft.class` (Mojang left this readable)
- 47 voice-mod classes under `com/moyettes/legacyvoicechat/**`
- 8 opus/rnnoise Java wrappers under `com/plasmoverse/**` plus their 14 native `.so`/`.dll`/`.dylib` binaries under `natives/`
- 2 log4j stubs at `org/apache/logging/log4j/{LogManager,Logger}.class` (tiny replacements; real log4j is ~1400 classes and not needed for the handful of LOGGER.info/warn/error calls we make)
- `assets/legacyvoicechat/**` textures and the mixin refmap

Total 63 classes + 14 native libs + assets ≈ 5 MB. No SpongePowered runtime classes, no vanilla MC pass-through, no transitive dependency bloat.

## Install

1. Download `LegacyVoiceChat-0.1.0-RawRetroMCP-dropin.zip` (or `./gradlew :rawretromcp-b1.7.3:build` locally).
2. Merge the zip contents into a vanilla or modded b1.7.3 `minecraft.jar` (e.g. `cd extracted && jar uf /path/to/minecraft.jar .`). Because the class names match vanilla Mojang obfuscation, the modified classes simply overwrite the unmodified originals.
3. Launch — no Fabric loader required; mixins were applied at build time.

## Limitations of the offline mixin applier

The applier in `build-tools/mixin-applier.gradle` is a targeted implementation, not a drop-in replacement for SpongePowered Mixin. It supports exactly the features this module's mixins use today. When a new mixin feature is introduced (say `@ModifyArg`, `@Redirect`, `@Overwrite`), the applier needs to be extended. Features currently supported:

- `@Mixin(class)` target selection (reads `invisibleAnnotations`)
- `@Shadow` (skipped; assumes Loom's compile already resolved the reference)
- `@Accessor` with explicit `value=` or inferred getter-name conversion
- `@Inject @At("HEAD" | "TAIL" | "INVOKE" target=…)`
- Added instance fields, added instance methods with owner-reference rewriting
- `implements <Interface>` on the mixin propagated to the target class

The `ClassWriter.getCommonSuperClass` override returns `java/lang/Object` unconditionally — fine for the shape of changes we make (method appends, field adds) but would produce suboptimal stack map frames on complex control-flow rewrites.
