package com.keyboardr.dancedj;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.player.MonitorPlayer;
import com.keyboardr.dancedj.util.CachedLoader;

import java.util.ArrayList;
import java.util.Arrays;

public class PlaylistControlsFragment extends Fragment {

    private AudioManager audioManager;
    private AudioDeviceCallback audioDeviceCallback = new AudioDeviceCallback() {
        @Override
        public void onAudioDevicesAdded(AudioDeviceInfo[] addedDevices) {
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
        public void onAudioDevicesRemoved(AudioDeviceInfo[] removedDevices) {
            int originalSize = devices == null ? 0 : devices.length;
            ArrayList<AudioDeviceInfo> newDevices = (devices == null)
                    ? new ArrayList<AudioDeviceInfo>() : new ArrayList<>(Arrays.asList(devices));
            boolean currentRemoved = false;
            AudioDeviceInfo audioOutput = player.getAudioOutput();
            for (AudioDeviceInfo device : removedDevices) {
                for (int i = newDevices.size() - 1; i >= 0; i--) {
                    if (device.getId() == newDevices.get(i).getId()) {
                        newDevices.remove(i);
                        if (audioOutput != null && audioOutput.getId() == device.getId()) {
                            currentRemoved = true;
                        }
                    }
                }
            }
            if (newDevices.size() != originalSize) {
                setDevices(newDevices.toArray(new AudioDeviceInfo[newDevices.size()]));
            }
            if (audioDeviceSpinner != null && currentRemoved) {
                player.setAudioOutput(null);
                audioDeviceSpinner.setSelection(0);
            }
        }
    };
    private Handler handler;

    @SuppressWarnings("unused")
    public static MonitorControlsFragment newInstance() {
        return new MonitorControlsFragment();
    }

    private PlayerControlsUpdater uiUpdater;
    private MonitorPlayer player;
    private AudioOutputAdapter audioOutputAdapter;
    private
    @Nullable
    AudioDeviceInfo[] devices;
    private Spinner audioDeviceSpinner;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        handler = new Handler();
        audioManager = getContext().getSystemService(AudioManager.class);
        player = new MonitorPlayer(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_controls, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        audioDeviceSpinner = (Spinner) view.findViewById(R.id.controls_spinner);
        audioOutputAdapter = new AudioOutputAdapter();
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, handler);
        setDevices(devices);
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
        uiUpdater = new PlayerControlsUpdater(view, player, getLoaderManager());
    }

    @Override
    public void onDestroyView() {
        uiUpdater.detach();
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
        super.onDestroyView();
    }

    public void playMedia(MediaItem mediaItem) {
        player.play(mediaItem, true);
        uiUpdater.onMetaData(mediaItem);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.release();
    }

    private void setDevices(@Nullable AudioDeviceInfo[] devices) {
        this.devices = devices;
        if (audioOutputAdapter != null) {
            audioOutputAdapter.notifyDataSetChanged();
        }
    }

    private class AudioOutputAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return devices == null ? 1 : devices.length + 1;
        }

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
                textView.setText(getTextForDevice(item));
            }
            return convertView;
        }

        private CharSequence getTextForDevice(AudioDeviceInfo item) {
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
                return getText(res);
            }
            return item.getProductName();
        }


    }

    public static class AlbumArtLoader extends CachedLoader<Bitmap> {

        private final MediaItem mediaItem;

        public AlbumArtLoader(Context context, MediaItem item) {
            super(context);
            this.mediaItem = item;
        }

        @Override
        public Bitmap loadInBackground() {
            if (mediaItem == null) {
                return null;
            }
            return mediaItem.getAlbumArt(getContext());
        }
    }
}
