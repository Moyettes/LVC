#!/bin/bash
# Merges a split ornithe-<v> (/client + /server) back into a single merged
# module for MC versions that ship a merged client+server jar (MC 1.3+).
#
# Shuffles:
#   /client/src/main/java/**           -> src/main/java/**
#   /server/src/main/java/**           -> src/main/java/**   (no package overlap)
#   /client/src/main/resources/assets  -> src/main/resources/assets
# Rewrites entrypoints + mixin config for merged use. Switches Voice.java
# (client) and VoiceBootstrap*.java (server) to the client-init / server-init
# OSL interfaces so one side's classes don't load on the other.
#
# Usage: merge-split-module.sh <version>
#   example: merge-split-module.sh 1.8

set -e

VERSION="$1"
if [ -z "$VERSION" ]; then
  echo "usage: $0 <version>"
  exit 1
fi

REPO_ROOT="P:/LegacyVoiceChat (Claude)/LegacyVoiceChat Compatibility"
MOD="$REPO_ROOT/ornithe-$VERSION"

if [ ! -d "$MOD/client" ] || [ ! -d "$MOD/server" ]; then
  echo "ornithe-$VERSION is not in the split layout; skipping."
  exit 0
fi

echo "=== Merging ornithe-$VERSION ==="

# 1. Combined java tree.
mkdir -p "$MOD/src/main/java"
mkdir -p "$MOD/src/main/resources"

# Use tar so we preserve nested directories (cp -r over existing dest is
# inconsistent across bsd/gnu; tar|tar is portable and predictable).
(cd "$MOD/client/src/main/java" && tar cf - .) | (cd "$MOD/src/main/java" && tar xf -)
(cd "$MOD/server/src/main/java" && tar cf - .) | (cd "$MOD/src/main/java" && tar xf -)

# 2. Assets live under client only; copy them up.
if [ -d "$MOD/client/src/main/resources/assets" ]; then
  mkdir -p "$MOD/src/main/resources/assets"
  (cd "$MOD/client/src/main/resources/assets" && tar cf - .) | (cd "$MOD/src/main/resources/assets" && tar xf -)
fi

# 3. Convert client Voice.java to ClientModInitializer and relocate it to the
#    .client subpackage so it does not collide with server-common's Voice
#    (same FQN otherwise; server-common owns com.moyettes.legacyvoicechat.Voice).
VOICE="$MOD/src/main/java/com/moyettes/legacyvoicechat/Voice.java"
if [ -f "$VOICE" ]; then
  mkdir -p "$MOD/src/main/java/com/moyettes/legacyvoicechat/client"
  mv "$VOICE" "$MOD/src/main/java/com/moyettes/legacyvoicechat/client/Voice.java"
  VOICE="$MOD/src/main/java/com/moyettes/legacyvoicechat/client/Voice.java"
  sed -i \
    -e 's|^package com\.moyettes\.legacyvoicechat;|package com.moyettes.legacyvoicechat.client;|' \
    -e 's|net\.ornithemc\.osl\.entrypoints\.api\.ModInitializer|net.ornithemc.osl.entrypoints.api.client.ClientModInitializer|' \
    -e 's|implements ModInitializer|implements ClientModInitializer|' \
    -e 's|public void init()|public void initClient()|' \
    "$VOICE"
fi

# 3a. Client-side files currently import com.moyettes.legacyvoicechat.Voice;
#     those references now need to point at the relocated client package.
#     Skip server-only files whose Voice import resolves to server-common's
#     class (compat/, VoiceBootstrap*, MinecraftServerMixin, MinecraftServerAccessor).
while IFS= read -r -d '' f; do
  case "$f" in
    */compat/*) continue ;;
    */VoiceBootstrap*.java) continue ;;
    */MinecraftServerMixin.java) continue ;;
    */MinecraftServerAccessor.java) continue ;;
  esac
  sed -i 's|import com.moyettes.legacyvoicechat.Voice;|import com.moyettes.legacyvoicechat.client.Voice;|' "$f"
done < <(grep -lrZ "import com.moyettes.legacyvoicechat.Voice;" "$MOD/src/main/java" 2>/dev/null || true)

# 4. Convert server bootstrap to ServerModInitializer.
BOOTSTRAP=$(find "$MOD/src/main/java/com/moyettes/legacyvoicechat" -maxdepth 1 -name "VoiceBootstrap*.java" -print -quit)
if [ -n "$BOOTSTRAP" ]; then
  sed -i \
    -e 's|net\.ornithemc\.osl\.entrypoints\.api\.ModInitializer|net.ornithemc.osl.entrypoints.api.server.ServerModInitializer|' \
    -e 's|implements ModInitializer|implements ServerModInitializer|' \
    -e 's|public void init()|public void initServer()|' \
    "$BOOTSTRAP"
fi
BOOTSTRAP_CLASS=""
if [ -n "$BOOTSTRAP" ]; then
  BOOTSTRAP_CLASS="com.moyettes.legacyvoicechat.$(basename "$BOOTSTRAP" .java)"
fi

# 5. Combined build.gradle — depends on all three shared modules; no
#    clientOnly/serverOnly hooks (merged minecraft jar for MC 1.3+).
cat > "$MOD/build.gradle" <<'GRADLE'
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
	id 'fabric-loom'
	id 'ploceus'
	id 'com.gradleup.shadow' version '8.3.6'
}

base {
	archivesName = "${archives_base_name}-Ornithe-${minecraft_version}"
}

repositories {
	maven {
		url = uri('https://repo.plo.su')
	}
}

dependencies {
	minecraft "com.mojang:minecraft:${minecraft_version}"
	mappings ploceus.featherMappings(feather_build)
	modImplementation "net.fabricmc:fabric-loader:${loader_version}"
	ploceus.dependOsl(project.osl_version)

	shadow(implementation('su.plo.voice:opus:1.1.2'))
	shadow(implementation('su.plo.voice:rnnoise:1.0.0'))
	shadow(implementation(project(':common')))
	shadow(implementation(project(':client-common')))
	shadow(implementation(project(':server-common')))
}

tasks.named('shadowJar', ShadowJar) {
	configurations = [project.configurations.shadow]
	archiveClassifier.set('dev-shadow')
}

tasks.named('remapJar') {
	dependsOn tasks.named('shadowJar')
	inputFile.set(tasks.named('shadowJar', ShadowJar).flatMap { it.archiveFile })
	archiveClassifier.set('')
}

jar {
	from(rootProject.file('LICENSE')) {
		rename { "${it}_${base.archivesName.get()}" }
	}
}
GRADLE

# 6. Combined gradle.properties — drop the environment = client marker.
cp "$MOD/server/gradle.properties" "$MOD/gradle.properties"
sed -i '/^environment\s*=/d' "$MOD/gradle.properties"

# 7. fabric.mod.json — client-init + server-init entrypoints.
DEPEND_MC=$(grep -E '"minecraft"\s*:' "$MOD/client/src/main/resources/fabric.mod.json" | sed 's/.*"minecraft":\s*"\([^"]*\)".*/\1/')
cat > "$MOD/src/main/resources/fabric.mod.json" <<EOF
{
  "schemaVersion": 1,
  "id": "legacyvoicechat",
  "version": "1.0.0",
  "name": "LegacyVoiceChat",
  "description": "Cross-loader voice chat for Minecraft.",
  "authors": [
    "Moyettes"
  ],
  "contact": {
    "homepage": "https://github.com/moyettes/legacyvoicechat",
    "sources": "https://github.com/moyettes/legacyvoicechat"
  },
  "license": "MIT",
  "entrypoints": {
    "client-init": [
      "com.moyettes.legacyvoicechat.client.Voice"
    ],
    "server-init": [
      "$BOOTSTRAP_CLASS"
    ]
  },
  "mixins": [
    "legacyvoicechat.mixins.json"
  ],
  "depends": {
    "fabricloader": ">=0.14.21",
    "minecraft": "$DEPEND_MC",
    "osl-entrypoints": ">=0.4.0"
  }
}
EOF

# 8. Combined mixins.json. Pull the shared/client arrays from the client
#    config, and fold the server-only MinecraftServerMixin under the server
#    array (the only per-module server mixin in this codebase).
CLIENT_MIX="$MOD/client/src/main/resources/legacyvoicechat.mixins.json"
if [ -f "$MOD/src/main/java/com/moyettes/legacyvoicechat/mixin/MinecraftServerMixin.java" ]; then
  HAS_SERVER_MIX=1
else
  HAS_SERVER_MIX=0
fi

if [ -f "$CLIENT_MIX" ]; then
  # Extract client array entries from the client config; leave shared list
  # identical to the split-era client config.
  perl -0pe '
    if ('"$HAS_SERVER_MIX"') {
      s/"server":\s*\[\s*\]/"server": [\n    "MinecraftServerMixin"\n  ]/;
    }
  ' "$CLIENT_MIX" > "$MOD/src/main/resources/legacyvoicechat.mixins.json"
fi

# 9. Remove the now-redundant split subprojects.
rm -rf "$MOD/client" "$MOD/server"

echo "=== ornithe-$VERSION merged (settings.gradle update still needed) ==="
