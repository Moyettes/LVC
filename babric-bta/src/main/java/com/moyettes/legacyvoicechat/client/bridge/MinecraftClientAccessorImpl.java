package com.moyettes.legacyvoicechat.client.bridge;

import com.moyettes.legacyvoicechat.client.platform.IMinecraftClientAccessor;
import com.moyettes.legacyvoicechat.client.platform.PlayerRef;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.core.entity.player.Player;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public final class MinecraftClientAccessorImpl implements IMinecraftClientAccessor {

	@Override
	public double getPlayerX() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.thePlayer != null ? mc.thePlayer.x : 0.0;
	}

	@Override
	public double getPlayerY() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.thePlayer != null ? mc.thePlayer.y : 0.0;
	}

	@Override
	public double getPlayerZ() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.thePlayer != null ? mc.thePlayer.z : 0.0;
	}

	@Override
	public float getYaw() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.thePlayer != null ? mc.thePlayer.yRot : 0.0F;
	}

	@Override
	public float getPitch() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.thePlayer != null ? mc.thePlayer.xRot : 0.0F;
	}

	@Override
	public String getLocalPlayerName() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null || mc.thePlayer == null) return null;
		return mc.thePlayer.username;
	}

	@Override
	public Integer getLocalPlayerNetworkId() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null || mc.thePlayer == null) return null;
		return mc.thePlayer.id;
	}

	@Override
	public float getOptionsMasterVolume() {
		return 1.0F;
	}

	@Override
	public Collection<PlayerRef> getOnlinePlayers() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null || mc.currentWorld == null) return Collections.emptyList();

		List<PlayerRef> out = new ArrayList<PlayerRef>();
		for (Player p : mc.currentWorld.players) {
			if (p != null && p.username != null) {
				out.add(new PlayerRef(p.id, p.username));
			}
		}
		return out;
	}

	@Override
	public void openScreen(Object screenRef) {
	}
}
