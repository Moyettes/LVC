#!/usr/bin/perl
# Rewrites a RenderEvents.java scaffolded from standalone so that it plugs
# into client-common's Integer-networkId callback signature. Call with the
# RenderEvents path as argv[0].

use strict;
use warnings;

die "usage: fix-render-bridge.pl <RenderEvents.java>\n" unless @ARGV == 1;

my $path = $ARGV[0];
open my $in, '<', $path or die "open $path: $!";
local $/;
my $src = <$in>;
close $in;

my $helper = <<'JAVA';
ClientEvents.INSTANCE.registerRenderPlayerIconsEvent((networkId, d, e, f) -> {
			PlayerEntity entity = lookupPlayer(networkId);
			if (entity != null) onRenderPlayerIcons(entity, d, e, f);
		});
	}

	private PlayerEntity lookupPlayer(int networkId) {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null || mc.world == null) return null;
		for (Object obj : mc.world.players) {
			if (obj instanceof PlayerEntity) {
				PlayerEntity player = (PlayerEntity) obj;
				if (player.getNetworkId() == networkId) return player;
			}
		}
		return null;
JAVA
chomp $helper;

# Replace the registration line and the closing brace of the constructor with
# the lambda bridge + lookupPlayer helper body. The helper reuses the same
# closing brace of the constructor after the lookup method.
$src =~ s{ClientEvents\.INSTANCE\.registerRenderPlayerIconsEvent\(this::onRenderPlayerIcons\);\n\t\}}{$helper\n\t\}}s;

# Swap the isPlayer* calls to pass Integer networkId.
$src =~ s{if \(voiceClient\.isPlayerVoiceSupported\(player\)\)}{Integer id = player.getNetworkId();\n\t\tif (voiceClient.isPlayerVoiceSupported(id))};
$src =~ s{voiceClient\.isPlayerDeafened\(player\)}{voiceClient.isPlayerDeafened(id)};
$src =~ s{voiceClient\.isPlayerTalking\(player\)}{voiceClient.isPlayerTalking(id)};

open my $out, '>', $path or die "write $path: $!";
print $out $src;
close $out;
