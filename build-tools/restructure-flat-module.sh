#!/bin/bash
# Restructures a flat ornithe-<v> (server-only) into /client + /server split.
# Moves the flat src/build into /server, creates a /client subfolder wired to
# client-common, and syncs client-side code from the matching Ornithe-<v>
# standalone. Voice.java, RenderEvents.java, and PlayerEntityRendererMixin.java
# are left for hand-adjustment (per-version MC API differences).
#
# Usage: restructure-flat-module.sh <our-version> <standalone-dir-name> [mc-version-for-depend]
#   example: restructure-flat-module.sh 1.3.1 Ornithe-1.3.0 1.3.1

set -e

VERSION="$1"
STANDALONE="$2"
DEPEND_MC="${3:-$VERSION}"

if [ -z "$VERSION" ] || [ -z "$STANDALONE" ]; then
  echo "Usage: $0 <our-version> <standalone-dir-name> [mc-version-for-depend]"
  exit 1
fi

REPO_ROOT="P:/LegacyVoiceChat (Claude)/LegacyVoiceChat Compatibility"
STD_ROOT="P:/LegacyVoiceChat (Claude)/All Versions/$STANDALONE/src/main"
MOD="$REPO_ROOT/ornithe-$VERSION"

if [ ! -d "$STD_ROOT/java/com/moyettes/voice" ]; then
  echo "Standalone not found: $STD_ROOT/java/com/moyettes/voice"
  exit 1
fi

echo "=== Restructuring ornithe-$VERSION ==="

# 1. Flat -> /server.
if [ -d "$MOD/src" ] && [ ! -d "$MOD/server/src" ]; then
  mkdir -p "$MOD/server"
  mv "$MOD/src" "$MOD/server/src"
  mv "$MOD/build.gradle" "$MOD/server/build.gradle"
  mv "$MOD/gradle.properties" "$MOD/server/gradle.properties"
  rm -rf "$MOD/build" 2>/dev/null || true
fi

# 2. /server build.gradle — merged mapping (required for MC 1.3+).
cat > "$MOD/server/build.gradle" <<'GRADLE'
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
	shadow(implementation(project(':server-common')))
}

tasks.named('shadowJar', ShadowJar) {
	configurations = [project.configurations.shadow]
	archiveClassifier.set('dev-shadow')
}

tasks.named('remapJar') {
	dependsOn tasks.named('shadowJar')
	inputFile.set(tasks.named('shadowJar', ShadowJar).flatMap { it.archiveFile })
	archiveClassifier.set('server')
}

jar {
	from(rootProject.file('LICENSE')) {
		rename { "${it}_${base.archivesName.get()}" }
	}
}
GRADLE

# 3. /client skeleton.
mkdir -p "$MOD/client/src/main/java/com/moyettes/legacyvoicechat"
mkdir -p "$MOD/client/src/main/resources"

cat > "$MOD/client/build.gradle" <<'GRADLE'
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
	shadow(implementation(project(':client-common')))
	shadow(implementation(project(':common')))
}

tasks.named('shadowJar', ShadowJar) {
	configurations = [project.configurations.shadow]
	archiveClassifier.set('dev-shadow')
}

tasks.named('remapJar') {
	dependsOn tasks.named('shadowJar')
	inputFile.set(tasks.named('shadowJar', ShadowJar).flatMap { it.archiveFile })
	archiveClassifier.set('client')
}

jar {
	from(rootProject.file('LICENSE')) {
		rename { "${it}_${base.archivesName.get()}" }
	}
}
GRADLE

cp "$MOD/server/gradle.properties" "$MOD/client/gradle.properties"
if ! grep -q "^environment" "$MOD/client/gradle.properties"; then
  sed -i '1i environment = client\n' "$MOD/client/gradle.properties"
fi

# 4. Sync java tree.
STD_JAVA="$STD_ROOT/java/com/moyettes/voice"
OUR_JAVA="$MOD/client/src/main/java/com/moyettes/legacyvoicechat"

declare -A SKIP=(
  [events/ClientEvents.java]=1
  [utils/AudioUtils.java]=1
)

for subdir in api events extensions gui gui/widget mixin utils; do
  src_dir="$STD_JAVA/$subdir"
  dst_dir="$OUR_JAVA/$subdir"
  if [ ! -d "$src_dir" ]; then continue; fi
  mkdir -p "$dst_dir"
  for f in "$src_dir"/*.java; do
    [ -f "$f" ] || continue
    name=$(basename "$f")
    rel="$subdir/$name"
    if [ -n "${SKIP[$rel]}" ]; then continue; fi
    sed 's|com\.moyettes\.voice|com.moyettes.legacyvoicechat|g' "$f" > "$dst_dir/$name"
  done
done

# Server mixin doesn't belong on client side.
rm -f "$OUR_JAVA/mixin/MinecraftServerMixin.java"

# Assets.
if [ -d "$STD_ROOT/resources/assets" ]; then
  mkdir -p "$MOD/client/src/main/resources/assets"
  cp -r "$STD_ROOT/resources/assets"/* "$MOD/client/src/main/resources/assets/"
fi

# Rewrite standalone's client.init.Voice import to our root Voice import.
find "$OUR_JAVA" -name "*.java" -exec sed -i \
  's|com\.moyettes\.legacyvoicechat\.client\.init\.Voice|com.moyettes.legacyvoicechat.Voice|g' {} +

# 5. Template Voice.java — the 1.8 pattern (String channel keys + PacketByteBuf
#    OSL bridge). Use ornithe-1.8/client as the canonical template.
TEMPLATE_VOICE="$REPO_ROOT/ornithe-1.8/client/src/main/java/com/moyettes/legacyvoicechat/Voice.java"
if [ -f "$TEMPLATE_VOICE" ]; then
  cp "$TEMPLATE_VOICE" "$OUR_JAVA/Voice.java"
fi

# 6. fabric.mod.json.
cat > "$MOD/client/src/main/resources/fabric.mod.json" <<EOF
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
    "init": [
      "com.moyettes.legacyvoicechat.Voice"
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

# 7. legacyvoicechat.mixins.json.
cat > "$MOD/client/src/main/resources/legacyvoicechat.mixins.json" <<'EOF'
{
  "required": true,
  "minVersion": "0.8",
  "package": "com.moyettes.legacyvoicechat.mixin",
  "compatibilityLevel": "JAVA_8",
  "mixins": [
    "GameGuiMixin",
    "KeyBindingMixin"
  ],
  "client": [
    "ClientNetworkHandlerMixin",
    "ConnectionInterfaceMixin",
    "ConnectionMixin",
    "GameOptionsMixin",
    "MinecraftMixin",
    "PlayerEntityRendererMixin"
  ],
  "server": [],
  "injectors": {
    "defaultRequire": 1
  }
}
EOF

echo "=== ornithe-$VERSION scaffold ready (manual fixes likely needed in RenderEvents/PlayerEntityRendererMixin) ==="
