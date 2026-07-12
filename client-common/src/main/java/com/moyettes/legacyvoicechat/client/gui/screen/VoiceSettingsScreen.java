package com.moyettes.legacyvoicechat.client.gui.screen;

import com.moyettes.legacyvoicechat.client.VoiceClient;
import com.moyettes.legacyvoicechat.client.VoiceSession;
import com.moyettes.legacyvoicechat.client.gui.widget.UiButton;
import com.moyettes.legacyvoicechat.client.gui.widget.UiSlider;
import com.moyettes.legacyvoicechat.client.platform.IRenderApi;
import com.moyettes.legacyvoicechat.client.platform.ResourceLike;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Line;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.TargetDataLine;
import java.util.ArrayList;
import java.util.List;

public class VoiceSettingsScreen extends BaseVoiceScreen {

	private static final ResourceLike MICROPHONE_ICON = ResourceLike.of("textures/icons/microphone.png");
	private static final ResourceLike MICROPHONE_MUTE_ICON = ResourceLike.of("textures/icons/microphone_off.png");
	private static final ResourceLike PLAYER_TALKING = ResourceLike.of("textures/icons/player_talking.png");
	private static final ResourceLike PLAYER_MUTED = ResourceLike.of("textures/icons/player_muted.png");

	private boolean isMuted;
	private boolean isDeafened;
	private double threshold;
	private float micGain = 1.0f;
	private float voiceVolume = 1.0f;
	private boolean pushToTalk;
	private boolean testingMic;

	private List<String> inputDevices;
	private int selectedDeviceIndex;
	private List<String> outputDevices;
	private int selectedOutputDeviceIndex;

	private UiSlider voiceVolumeSlider;
	private UiSlider micGainSlider;
	private UiSlider thresholdSlider;
	private UiButton activationButton;
	private UiButton deviceButton;
	private UiButton outputDeviceButton;
	private UiButton testButton;

	private static double thresholdToDb(double raw) {
		return -60.0 + (raw / 1000.0) * 60.0;
	}

	@Override
	protected void initContent() {
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) {
			threshold = vc.getSilenceThreshold();
			micGain = vc.getMicrophoneGain();
			voiceVolume = vc.getMasterVolume();
			testingMic = vc.isMicrophoneTesting();
			pushToTalk = vc.isPushToTalk();
			isMuted = vc.isMicMuted();
			isDeafened = vc.isDeafened();
		}

		int backgroundX = backgroundX();
		int backgroundY = backgroundY();
		int controlX = backgroundX + 8;
		int controlY = backgroundY + 22;
		int controlWidth = backgroundWidth - 16;

		voiceVolumeSlider = new UiSlider(controlX, controlY, controlWidth, 20,
			"Voice Chat Volume: " + Math.round(voiceVolume * 100) + "%",
			clamp01(voiceVolume / 2.0f),
			slider -> onVoiceVolumeChanged(slider));
		widgets.add(voiceVolumeSlider);

		float gain = vc != null ? vc.getMicrophoneGain() : micGain;
		micGainSlider = new UiSlider(controlX, controlY + 24, controlWidth, 20,
			"Microphone Volume: " + Math.round(gain * 100) + "%",
			clamp01(gain / 2.0f),
			slider -> onMicGainChanged(slider));
		widgets.add(micGainSlider);

		activationButton = new UiButton(controlX, controlY + 48, controlWidth, 20,
			"Activation Type: " + (pushToTalk ? "Push To Talk" : "Voice"),
			this::onActivationToggled);
		widgets.add(activationButton);

		thresholdSlider = new UiSlider(controlX, controlY + 72, controlWidth, 20,
			"Activation Threshold: " + (int) Math.round(thresholdToDb(threshold)) + " dB",
			(float) (Math.max(0.0, Math.min(1000.0, threshold)) / 1000.0),
			slider -> onThresholdChanged(slider));
		widgets.add(thresholdSlider);

		buildInputDevices();
		String currentName = vc != null ? vc.getInputDeviceName() : null;
		if (currentName != null) {
			selectedDeviceIndex = Math.max(0, inputDevices.indexOf(currentName));
		} else {
			selectedDeviceIndex = 0;
		}
		deviceButton = new UiButton(controlX, controlY + 96, controlWidth, 20,
			"Microphone: " + inputDevices.get(selectedDeviceIndex),
			this::onDeviceCycled);
		widgets.add(deviceButton);

		buildOutputDevices();
		String currentOutputName = vc != null ? vc.getOutputDeviceName() : null;
		if (currentOutputName != null) {
			selectedOutputDeviceIndex = Math.max(0, outputDevices.indexOf(currentOutputName));
		} else {
			selectedOutputDeviceIndex = 0;
		}
		outputDeviceButton = new UiButton(controlX, controlY + 120, controlWidth, 20,
			"Speakers: " + outputDevices.get(selectedOutputDeviceIndex),
			this::onOutputDeviceCycled);
		widgets.add(outputDeviceButton);

		testButton = new UiButton(controlX, controlY + 144, controlWidth, 20,
			testingMic ? "Disable Microphone Testing" : "Test Microphone",
			this::onTestMicToggled);
		widgets.add(testButton);

		UiButton muteButton = new UiButton(backgroundX + 8, backgroundY + backgroundHeight - 28, 20, 20, "",
			this::onMuteToggled);
		widgets.add(muteButton);

		UiButton deafenButton = new UiButton(backgroundX + 32, backgroundY + backgroundHeight - 28, 20, 20, "",
			this::onDeafenToggled);
		widgets.add(deafenButton);
	}

	private static float clamp01(float v) {
		if (v < 0.0f) return 0.0f;
		if (v > 1.0f) return 1.0f;
		return v;
	}

	private void onVoiceVolumeChanged(UiSlider slider) {
		voiceVolume = Math.max(0.0f, Math.min(2.0f, slider.value * 2.0f));
		slider.setMessage("Voice Chat Volume: " + Math.round(voiceVolume * 100) + "%");
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) vc.setMasterVolume(voiceVolume);
	}

	private void onMicGainChanged(UiSlider slider) {
		micGain = Math.max(0.0f, Math.min(2.0f, slider.value * 2.0f));
		slider.setMessage("Microphone Volume: " + (int) (micGain * 100) + "%");
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) vc.setMicrophoneGain(micGain);
	}

	private void onThresholdChanged(UiSlider slider) {
		threshold = Math.max(0.0, Math.min(1000.0, slider.value * 1000.0));
		slider.setMessage("Activation Threshold: " + (int) Math.round(thresholdToDb(threshold)) + " dB");
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) vc.setSilenceThreshold(threshold);
	}

	private void onActivationToggled() {
		pushToTalk = !pushToTalk;
		activationButton.setMessage("Activation Type: " + (pushToTalk ? "Push To Talk" : "Voice"));
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) vc.setPushToTalk(pushToTalk);
	}

	private void onDeviceCycled() {
		selectedDeviceIndex = (selectedDeviceIndex + 1) % inputDevices.size();
		String name = inputDevices.get(selectedDeviceIndex);
		deviceButton.setMessage("Microphone: " + name);

		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) {
			vc.setInputDeviceName("Default".equals(name) ? null : name);
		}
	}

	private void onOutputDeviceCycled() {
		selectedOutputDeviceIndex = (selectedOutputDeviceIndex + 1) % outputDevices.size();
		String name = outputDevices.get(selectedOutputDeviceIndex);
		outputDeviceButton.setMessage("Speakers: " + name);

		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) {
			vc.setOutputDeviceName("Default".equals(name) ? null : name);
		}
	}

	private void onTestMicToggled() {
		testingMic = !testingMic;
		testButton.setMessage(testingMic ? "Disable Microphone Testing" : "Test Microphone");
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) {
			if (testingMic) {
				if (!vc.startMicrophoneTesting()) {
					testingMic = false;
					testButton.setMessage("Test Microphone");
				}
			} else {
				vc.stopMicrophoneTesting();
			}
		}
	}

	private void onMuteToggled() {
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) {
			vc.toggleMute();
			isMuted = vc.isMicMuted();
		}
	}

	private void onDeafenToggled() {
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null) {
			vc.toggleDeafened();
			isDeafened = vc.isDeafened();
			isMuted = vc.isMicMuted();
		}
	}

	@Override
	protected void renderContent(IRenderApi api, int mouseX, int mouseY, float partialTick) {
	}

	@Override
	protected void drawForeground(IRenderApi api) {
		super.drawForeground(api);
		drawIconAt(api, 8, isMuted ? MICROPHONE_MUTE_ICON : MICROPHONE_ICON);
		drawIconAt(api, 32, isDeafened ? PLAYER_MUTED : PLAYER_TALKING);
	}

	private void drawIconAt(IRenderApi api, int buttonOffsetX, ResourceLike icon) {
		int backgroundX = backgroundX();
		int backgroundY = backgroundY();
		int buttonX = backgroundX + buttonOffsetX;
		int buttonY = backgroundY + backgroundHeight - 28;
		int buttonSize = 20;
		int iconSize = 12;
		int iconX = buttonX + (buttonSize - iconSize) / 2;
		int iconY = buttonY + (buttonSize - iconSize) / 2;

		api.setColor(1.0F, 1.0F, 1.0F, 1.0F);
		api.enableBlend();
		api.drawTextureScaled(icon, iconX, iconY, iconSize, iconSize);
		api.disableBlend();
	}

	@Override
	protected ResourceLike backgroundTexture() {
		return BACKGROUND_MAIN;
	}

	@Override
	protected String title() {
		return "Voice Settings";
	}

	@Override
	public void close() {
		VoiceClient vc = VoiceSession.getActive();
		if (vc != null && testingMic) {
			vc.stopMicrophoneTesting();
		}
	}

	private void buildInputDevices() {
		inputDevices = buildDevices(TargetDataLine.class);
	}

	private void buildOutputDevices() {
		outputDevices = buildDevices(javax.sound.sampled.SourceDataLine.class);
	}

	private static List<String> buildDevices(Class<?> lineClass) {
		List<String> devices = new ArrayList<String>();
		devices.add("Default");
		try {
			Mixer.Info[] mixers = AudioSystem.getMixerInfo();
			for (Mixer.Info mi : mixers) {
				try {
					Mixer m = AudioSystem.getMixer(mi);
					Line.Info lineInfo = new Line.Info(lineClass);
					if (m.isLineSupported(lineInfo)) {
						String name = mi.getName();
						if (!devices.contains(name)) {
							devices.add(name);
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		if (devices.isEmpty()) {
			devices.add("Default");
		}
		return devices;
	}
}
