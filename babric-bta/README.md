# Babric-BTA module

This module is **not** part of the parent multi-project Gradle build. BTA requires `fabric-loom 1.10.0-bta` which conflicts with the Ornithe modules' `fabric-loom 1.11-SNAPSHOT` on Gradle's plugin classpath.

## Building

From the `LegacyVoiceChat Compatibility/` parent directory:

```
# 1. Publish shared modules to local maven so BTA module can consume them
./gradlew :common:publishToMavenLocal :client-common:publishToMavenLocal :server-common:publishToMavenLocal

# 2. Build the BTA module (uses mavenLocal-published shared modules)
./gradlew -p babric-bta build

# 3. Collect every jar (including the BTA one) into build/dist/
./gradlew collectJars
```

Output: `babric-bta/build/libs/LegacyVoiceChat-b1.7.3-0.1.0-BTA.jar` and a copy at `build/dist/LegacyVoiceChat-b1.7.3-0.1.0-BTA.jar`.

## Why standalone?

Gradle's plugin classpath model doesn't allow two versions of the same plugin id (`fabric-loom`) on the same build. The Ornithe modules need `1.11-SNAPSHOT` for Ornithe Feather support; BTA needs the `1.10.0-bta` fork for `noIntermediateMappings()` and `customMinecraftMetadata`. The two cannot coexist in one composite build.

Once you've built the shared modules into your local maven, this module's `mavenLocal()` repo declaration picks them up. The output is byte-identical to what a fully-included module would produce.
