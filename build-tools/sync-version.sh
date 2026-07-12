#!/bin/bash
# Syncs a per-version ornithe-<v>/client module from its standalone at
# P:/LegacyVoiceChat (Claude)/All Versions/Ornithe-<v>/client/.
#
# Rewrites package com.moyettes.voice -> com.moyettes.legacyvoicechat on copy.
# Copies every .java in events/, mixin/, extensions/, utils/, gui/ plus
# src/main/resources/assets/.
#
# Files the per-loader module legitimately owns (Voice.java, mixins.json,
# fabric.mod.json, gradle.properties, build.gradle) are left alone - each
# version's entry point has version-specific bootstrapping that shouldn't
# be clobbered by a blind sync.

set -e

VERSION="$1"
STANDALONE="$2"  # e.g. Ornithe-b1.4

if [ -z "$VERSION" ] || [ -z "$STANDALONE" ]; then
  echo "Usage: $0 <version-tag> <standalone-dir-name>"
  echo "  example: $0 b1.4 Ornithe-b1.4"
  exit 1
fi

REPO_ROOT="P:/LegacyVoiceChat (Claude)/LegacyVoiceChat Compatibility"
STD_ROOT="P:/LegacyVoiceChat (Claude)/All Versions/$STANDALONE/client/src/main"
OUR_ROOT="$REPO_ROOT/ornithe-$VERSION/client/src/main"

STD_JAVA="$STD_ROOT/java/com/moyettes/voice"
OUR_JAVA="$OUR_ROOT/java/com/moyettes/legacyvoicechat"

if [ ! -d "$STD_JAVA" ]; then
  echo "Standalone java tree not found: $STD_JAVA"
  exit 1
fi

echo "=== Syncing ornithe-$VERSION from $STANDALONE ==="

# Files that live in client-common (shared across loaders) - don't copy a
# per-loader duplicate or the two copies will conflict on the classpath.
declare -A SKIP=(
  [events/ClientEvents.java]=1
  [utils/AudioUtils.java]=1
)

# Sync subdirectories that contain loader-specific code but not the entry
# Voice.java (that one the per-loader version owns).
for subdir in events extensions mixin gui gui/widget utils api; do
  src_dir="$STD_JAVA/$subdir"
  dst_dir="$OUR_JAVA/$subdir"
  if [ ! -d "$src_dir" ]; then continue; fi
  mkdir -p "$dst_dir"
  for f in "$src_dir"/*.java; do
    [ -f "$f" ] || continue
    name=$(basename "$f")
    rel="$subdir/$name"
    if [ -n "${SKIP[$rel]}" ]; then
      echo "  (skip $rel - lives in client-common)"
      continue
    fi
    sed 's|com\.moyettes\.voice|com.moyettes.legacyvoicechat|g' "$f" > "$dst_dir/$name"
    echo "  $rel"
  done
done

# Assets - textures, sounds, anything under resources/assets/
if [ -d "$STD_ROOT/resources/assets" ]; then
  mkdir -p "$OUR_ROOT/resources/assets"
  cp -r "$STD_ROOT/resources/assets"/* "$OUR_ROOT/resources/assets/"
  echo "  assets/ copied"
fi

echo "=== ornithe-$VERSION sync done ==="
