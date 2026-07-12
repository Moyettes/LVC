package com.moyettes.legacyvoicechat;

import net.minecraft.server.Packet;
import org.bukkit.event.Listener;
import org.bukkit.plugin.java.JavaPlugin;

import java.lang.reflect.Method;

public class BukkitVoicechatMod extends JavaPlugin implements Listener {


	@Override
	public void onEnable() {

	}

	private void registerVoiceChatPackets() throws Exception {
		try {
			Method registerMethod = Packet.class.getMethod("registerPacket", int.class, boolean.class, boolean.class, Class.class);

			//registerMethod.invoke(null, 250, true, true, CustomPayloadPacket.class);

			System.out.println("Registered packets using public registerPacket method");
		} catch (NoSuchMethodException e) {
			System.out.println("Public registerPacket method not found, trying private method");
		}

	}

	@Override
	public void onDisable() {

	}

}
