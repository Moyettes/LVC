#!/bin/bash
# Patches a scaffolded ornithe-<v>/client to bridge the Integer-networkId
# callback in client-common's ClientEvents down to per-loader PlayerEntity
# methods. Applied once per module after restructure-flat-module.sh runs.
#
# Usage: fix-render-bridge.sh <version>

set -e

VERSION="$1"
REPO_ROOT="P:/LegacyVoiceChat (Claude)/LegacyVoiceChat Compatibility"
SCRIPT_DIR="$(dirname "$0")"
OUR_JAVA="$REPO_ROOT/ornithe-$VERSION/client/src/main/java/com/moyettes/legacyvoicechat"

RE="$OUR_JAVA/events/RenderEvents.java"
PM="$OUR_JAVA/mixin/PlayerEntityRendererMixin.java"

if [ -f "$RE" ]; then
  perl "$SCRIPT_DIR/fix-render-bridge.pl" "$RE"
fi

if [ -f "$PM" ]; then
  sed -i 's|ClientEvents\.INSTANCE\.onRenderPlayerIcons(playerEntity, d, e, f);|ClientEvents.INSTANCE.onRenderPlayerIcons(playerEntity.getNetworkId(), d, e, f);|' "$PM"
fi

echo "  patched RenderEvents + PlayerEntityRendererMixin for ornithe-$VERSION"
