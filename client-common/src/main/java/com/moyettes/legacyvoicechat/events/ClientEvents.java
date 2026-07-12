package com.moyettes.legacyvoicechat.events;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class ClientEvents {

	public static ClientEvents INSTANCE;

	private final List<Runnable> inputEvents;
	private final List<Runnable> renderHUDEvents;
	private final List<RenderPlayerIconCallback> renderPlayerIconEvents;

	@FunctionalInterface
	public interface RenderPlayerIconCallback {
		// Integer networkId rather than the standalone's PlayerEntity so
		// client-common stays loader-agnostic. Per-loader RenderEvents
		// translates: cb.render(player.networkId, d, e, f).
		void render(int playerNetworkId, double d, double e, double f);
	}

	public ClientEvents(){
		INSTANCE = this;
		inputEvents = new ArrayList<>();
		renderHUDEvents = new ArrayList<>();
		renderPlayerIconEvents = new ArrayList<>();
	}

	public void onInput() {
		inputEvents.forEach(Runnable::run);
	}

	public void registerKeyBindsEvent(Runnable onHandleKeyBinds) {
		inputEvents.add(onHandleKeyBinds);
	}

	// Back-compat shim for callers still using the older method name.
	public void onHandleKeyBinds(Runnable onHandleKeyBinds) {
		registerKeyBindsEvent(onHandleKeyBinds);
	}

	public void onRenderHUD() {
		renderHUDEvents.forEach(Runnable::run);
	}

	// Back-compat: older callers passed a tickDelta. Ignored by the standalone
	// pipeline which fires HUD callbacks per tick rather than per frame.
	public void onRenderHUD(float tickDelta) {
		onRenderHUD();
	}

	public void registerRenderHUDEvent(Runnable renderHUDEvent) {
		renderHUDEvents.add(renderHUDEvent);
	}

	public void registerRenderHUDEvent(Consumer<Float> renderHUDEvent) {
		renderHUDEvents.add(() -> renderHUDEvent.accept(0.0f));
	}

	public void onRenderPlayerIcons(int playerNetworkId, double d, double e, double f) {
		renderPlayerIconEvents.forEach(cb -> cb.render(playerNetworkId, d, e, f));
	}

	public void registerRenderPlayerIconsEvent(RenderPlayerIconCallback callback) {
		renderPlayerIconEvents.add(callback);
	}
}
