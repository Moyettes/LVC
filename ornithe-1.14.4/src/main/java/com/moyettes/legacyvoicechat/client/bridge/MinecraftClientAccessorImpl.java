package com.moyettes.legacyvoicechat.client.bridge;

import com.moyettes.legacyvoicechat.client.platform.IMinecraftClientAccessor;
import com.moyettes.legacyvoicechat.client.platform.PlayerRef;
import com.moyettes.legacyvoicechat.utils.MinecraftAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.living.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class MinecraftClientAccessorImpl implements IMinecraftClientAccessor {

	@Override
	public double getPlayerX() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.player != null ? mc.player.x : 0.0;
	}

	@Override
	public double getPlayerY() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.player != null ? mc.player.y : 0.0;
	}

	@Override
	public double getPlayerZ() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.player != null ? mc.player.z : 0.0;
	}

	@Override
	public float getYaw() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.player != null ? mc.player.yaw : 0.0F;
	}

	@Override
	public float getPitch() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		return mc != null && mc.player != null ? mc.player.pitch : 0.0F;
	}

	@Override
	public String getLocalPlayerName() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null || mc.player == null) return null;
		try {
			return mc.player.getName().getString();
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	public Integer getLocalPlayerNetworkId() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null || mc.player == null) return null;
		return mc.player.getNetworkId();
	}

	@Override
	public float getOptionsMasterVolume() {
		return 1.0F;
	}

	@Override
	public Collection<PlayerRef> getOnlinePlayers() {
		Minecraft mc = MinecraftAccessor.getMinecraft();
		if (mc == null || mc.world == null) return Collections.emptyList();

		java.util.List<PlayerRef> out = new ArrayList<PlayerRef>();
		for (Object obj : mc.world.m_2450655()) {
			if (obj instanceof PlayerEntity) {
				PlayerEntity p = (PlayerEntity) obj;
				String name;
				try {
					name = p.getName().getString();
				} catch (Exception e) {
					continue;
				}
				out.add(new PlayerRef(p.getNetworkId(), name));
			}
		}
		return out;
	}

	@Override
	public void openScreen(Object screenRef) {
	}
}
