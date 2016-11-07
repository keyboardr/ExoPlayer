package com.keyboardr.dancedj.ui.playlist;

import android.media.AudioDeviceCallback;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
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

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.player.PlaylistPlayer;
import com.keyboardr.dancedj.service.PlaylistServiceClient;
import com.keyboardr.dancedj.ui.PlayerControlsUpdater;
import com.keyboardr.dancedj.util.FragmentUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class PlaylistControlsFragment extends Fragment {

    public interface Holder extends PlaylistPlayer.PlaylistChangedListener {
        void onMediaListLoaded();
    }

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
            int audioOutput = player != null ? player.getAudioOutputId() : -1;
            for (AudioDeviceInfo device : removedDevices) {
                for (int i = newDevices.size() - 1; i >= 0; i--) {
                    if (device.getId() == newDevices.get(i).getId()) {
                        newDevices.remove(i);
                        if (audioOutput != -1 && audioOutput == device.getId()) {
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
    @Nullable
    private PlaylistServiceClient player;
    @Nullable
    private PlayerControlsUpdater uiUpdater;
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
                if (player != null) {
                    player.setAudioOutput((AudioDeviceInfo) adapterView.getItemAtPosition(position));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
                if (player != null) {
                    player.setAudioOutput(null);
                }
            }
        });
        updateView();
    }

    @Override
    public void onDestroyView() {
        if (uiUpdater != null) {
            uiUpdater.detach();
        }
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback);
        super.onDestroyView();
    }


    public void serviceConnected(IBinder playlistServiceBinder) {
        player = new PlaylistServiceClient(playlistServiceBinder) {
            @Override
            public void onTrackAdded(int index) {
                getParent().onTrackAdded(index);
            }

            @Override
            public void onIndexChanged(int oldIndex, int newIndex) {
                getParent().onIndexChanged(oldIndex, newIndex);
            }

            @Override
            public void onMediaListLoaded() {
                getParent().onMediaListLoaded();
            }
        };
        updateView();
    }

    public void serviceDisconnected() {
        player = null;
        updateView();
    }

    private void updateView() {
        View view = getView();
        if (view != null) {
            if (player != null) {
                audioDeviceSpinner.setVisibility(View.VISIBLE);
                if (uiUpdater == null) {
                    uiUpdater = new PlaylistControlsUpdater(view, player, getLoaderManager());
                }
            } else {
                audioDeviceSpinner.setVisibility(View.INVISIBLE);
                if (uiUpdater != null) {
                    uiUpdater.detach();
                }
                uiUpdater = null;
            }
        }

    }

    public void addToQueue(@NonNull MediaItem mediaItem) {
        if (player != null) {
            player.addToQueue(mediaItem);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
        }
    }

    private void setDevices(@Nullable AudioDeviceInfo[] devices) {
        this.devices = devices;
        if (audioOutputAdapter != null) {
            audioOutputAdapter.notifyDataSetChanged();
        }
    }

    public List<PlaylistPlayer.PlaylistItem> getPlaylist() {
        return player != null ? player.getMediaList() : Collections.<PlaylistPlayer.PlaylistItem>emptyList();
    }

    public int getCurrentTrackIndex() {
        return player != null ? player.getCurrentMediaIndex() : 0;
    }

    private Holder getParent() {
        return FragmentUtils.getParent(this, Holder.class);
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
}
