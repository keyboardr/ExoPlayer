package com.keyboardr.bluejay.ui;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class AudioSelectionManager {
  public interface Callback {
    boolean canBeDefault(AudioDeviceInfo deviceInfo);

    void onNoDeviceFound();

    void onDeviceSelected(AudioDeviceInfo audioDeviceInfo);
  }

  @Nullable
  private AudioDeviceInfo[] devices;
  @NonNull
  private final AudioManager audioManager;
  @NonNull
  private final Callback callback;

  private AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
    @Override
    public void onAudioDevicesAdded(@NonNull AudioDeviceInfo[] addedDevices) {
      ArrayList<AudioDeviceInfo> newDevices = (devices == null)
          ? new ArrayList<AudioDeviceInfo>() : new ArrayList<>(Arrays.asList(devices));
      for (AudioDeviceInfo deviceInfo : addedDevices) {
        if (deviceInfo.isSink() && deviceInfo.getType() != AudioDeviceInfo.TYPE_TELEPHONY) {
          newDevices.add(deviceInfo);
        }
      }
      setDevices(newDevices.toArray(new AudioDeviceInfo[newDevices.size()]));
    }

    @Override
    public void onAudioDevicesRemoved(@NonNull AudioDeviceInfo[] removedDevices) {
      int originalSize = devices == null ? 0 : devices.length;
      ArrayList<AudioDeviceInfo> newDevices = (devices == null)
          ? new ArrayList<AudioDeviceInfo>() : new ArrayList<>(Arrays.asList(devices));
      for (AudioDeviceInfo device : removedDevices) {
        for (int i = newDevices.size() - 1; i >= 0; i--) {
          if (device.getId() == newDevices.get(i).getId()) {
            newDevices.remove(i);
          }
        }
      }
      if (newDevices.size() != originalSize) {
        setDevices(newDevices.toArray(new AudioDeviceInfo[newDevices.size()]));
      }
    }
  };

  public AudioSelectionManager(@NonNull Context context, @NonNull Callback callback) {
    audioManager = context.getSystemService(AudioManager.class);
    this.callback = callback;

    audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
  }

  public void detach() {
    audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
  }

  private void setDevices(@Nullable AudioDeviceInfo[] devices) {
    this.devices = devices;
    AudioDeviceInfo selectedDefault = null;
    if (devices != null) {
      Arrays.sort(devices, new Comparator<AudioDeviceInfo>() {
        @Override
        public int compare(AudioDeviceInfo left, AudioDeviceInfo right) {
          return right.getType() - left.getType();
        }
      });
      for (AudioDeviceInfo deviceInfo : devices) {
        if (callback.canBeDefault(deviceInfo)) {
          callback.onDeviceSelected(deviceInfo);
          selectedDefault = deviceInfo;
          break;
        }
      }
    }
    if (selectedDefault == null) {
      callback.onNoDeviceFound();
    }
  }
}
