package com.keyboardr.bluejay.ui;

import android.content.Context;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.android.exoplayer2.BuildConfig;
import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.player.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;

public class AudioSelectionManager {
    @SuppressWarnings("PointlessBooleanExpression")
    public static final boolean SPINNER_ENABLED = BuildConfig.DEBUG && true;

    public interface DefaultDeviceSelector {
        boolean canBeDefault(AudioDeviceInfo deviceInfo);

        void onNoDeviceFound();
    }

    @Nullable
    private AudioDeviceInfo[] devices;
    @SuppressWarnings("FieldCanBeLocal")
    @NonNull
    private final AudioOutputAdapter audioOutputAdapter;
    @NonNull
    private final Player player;
    @NonNull
    private final Spinner audioDeviceSpinner;
    @NonNull
    private final AudioManager audioManager;
    @SuppressWarnings("FieldCanBeLocal")
    @Nullable
    private final DefaultDeviceSelector defaultDeviceSelector;

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
            boolean currentRemoved = false;
            int audioOutput = player.getAudioOutputType();
            for (AudioDeviceInfo device : removedDevices) {
                for (int i = newDevices.size() - 1; i >= 0; i--) {
                    if (device.getId() == newDevices.get(i).getId()) {
                        newDevices.remove(i);
                        if (audioOutput != -1 && audioOutput == device.getType()) {
                            currentRemoved = true;
                        }
                    }
                }
            }
            if (newDevices.size() != originalSize) {
                setDevices(newDevices.toArray(new AudioDeviceInfo[newDevices.size()]));
            }
            if (currentRemoved && SPINNER_ENABLED) {
                player.setAudioOutput(null);
                audioDeviceSpinner.setSelection(0);
            }
        }
    };

    public AudioSelectionManager(@NonNull Context context, @NonNull Spinner spinner,
                                 final @NonNull Player player, @Nullable DefaultDeviceSelector defaultDeviceSelector) {
        audioManager = context.getSystemService(AudioManager.class);
        audioDeviceSpinner = spinner;
        this.player = player;
        this.defaultDeviceSelector = defaultDeviceSelector;

        audioOutputAdapter = new AudioOutputAdapter();
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null);
        audioDeviceSpinner.setAdapter(audioOutputAdapter);
        audioDeviceSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                player.setAudioOutput((AudioDeviceInfo) adapterView.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                player.setAudioOutput(null);
            }
        });
        //noinspection ConstantConditions
        spinner.setVisibility(SPINNER_ENABLED ? View.VISIBLE : View.GONE);
    }

    public void detach() {
        //noinspection ConstantConditions
        audioDeviceSpinner.setVisibility(SPINNER_ENABLED ? View.INVISIBLE : View.GONE);
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
    }

    private void setDevices(@Nullable AudioDeviceInfo[] devices) {
        this.devices = devices;
        AudioDeviceInfo selectedDefault = null;
        if (defaultDeviceSelector == null) {
            player.setAudioOutput(null);
        } else {
            if (devices != null) {
              Arrays.sort(devices, new Comparator<AudioDeviceInfo>() {
                @Override
                public int compare(AudioDeviceInfo left, AudioDeviceInfo right) {
                  return right.getType() - left.getType();
                }
              });
                for (AudioDeviceInfo deviceInfo : devices) {
                    if (defaultDeviceSelector.canBeDefault(deviceInfo)) {
                        player.setAudioOutput(deviceInfo);
                        selectedDefault = deviceInfo;
                        break;
                    }
                }
            }
            if (selectedDefault == null) {
                defaultDeviceSelector.onNoDeviceFound();
            }
        }

        if (SPINNER_ENABLED) {
            audioOutputAdapter.notifyDataSetChanged();
            boolean setSelection = false;
            if (selectedDefault != null) {
                for (int i = 0; i < audioOutputAdapter.getCount(); i++) {
                    AudioDeviceInfo item = (AudioDeviceInfo) audioOutputAdapter.getItem(i);
                    if (item != null && item.getId() == selectedDefault.getId()) {
                        audioDeviceSpinner.setSelection(i);
                        setSelection = true;
                        break;
                    }
                }
            }

            if (!setSelection) {
                audioDeviceSpinner.setSelection(0);
            }
        }
    }

    private class AudioOutputAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return devices == null ? 1 : devices.length + 1;
        }

        @Nullable
        @Override
        public Object getItem(int position) {
            if (position == 0) {
                return null;
            }
            return devices == null ? null : devices[position - 1];
        }

        @Override
        public long getItemId(int position) {
            AudioDeviceInfo item = (AudioDeviceInfo) getItem(position);
            return item == null ? 0 : item.getId();
        }

        @Override
        public View getView(int position, View convertView, ViewGroup viewGroup) {
            return applyItemToView(position, convertView, viewGroup, android.R.layout.simple_spinner_item);
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return applyItemToView(position, convertView, parent, android.R.layout.simple_spinner_dropdown_item);
        }

        @NonNull
        private View applyItemToView(int position, View convertView, ViewGroup viewGroup, @LayoutRes int layout) {
            if (convertView == null) {
                convertView = LayoutInflater.from(viewGroup.getContext()).inflate(layout, viewGroup, false);
            }

            AudioDeviceInfo item = (AudioDeviceInfo) getItem(position);

            TextView textView = ((TextView) convertView.findViewById(android.R.id.text1));
            if (item == null) {
                textView.setText(R.string.audio_output_default);
            } else {
                textView.setText(getTextForDevice(textView.getContext(), item));
            }
            return convertView;
        }

        private CharSequence getTextForDevice(Context context, AudioDeviceInfo item) {
            int res = -1;
            switch (item.getType()) {
                case AudioDeviceInfo.TYPE_WIRED_HEADPHONES:
                    res = R.string.audio_output_wired_headphones;
                    break;
                case AudioDeviceInfo.TYPE_AUX_LINE:
                    res = R.string.audio_output_aux_line;
                    break;
                case AudioDeviceInfo.TYPE_BUILTIN_SPEAKER:
                    res = R.string.audio_output_builtin_speaker;
                    break;
                case AudioDeviceInfo.TYPE_WIRED_HEADSET:
                    res = R.string.audio_output_wired_headset;
                    break;
                case AudioDeviceInfo.TYPE_BUILTIN_EARPIECE:
                    res = R.string.audio_output_builtin_earpiece;
                    break;
            }
            if (res != -1) {
                return context.getText(res);
            }
            return item.getProductName();
        }
    }
}
