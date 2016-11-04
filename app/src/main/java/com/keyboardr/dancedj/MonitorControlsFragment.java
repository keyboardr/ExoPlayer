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
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.player.MonitorPlayer;
import com.keyboardr.dancedj.player.Player;
import com.keyboardr.dancedj.util.CachedLoader;
import com.keyboardr.dancedj.util.MathUtil;

import java.util.ArrayList;
import java.util.Arrays;

public class MonitorControlsFragment extends Fragment {

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

    private UiUpdater uiUpdater = new UiUpdater();
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
        return inflater.inflate(R.layout.fragment_monitor_controls, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        audioDeviceSpinner = (Spinner) view.findViewById(R.id.spinner);
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
        uiUpdater.attach(player);
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

    private class UiUpdater implements Player.PlaybackListener {

        private static final String ARG_MEDIA_ITEM = "mediaItem";
        private Handler seekHandler = new Handler();

        @Nullable
        private MediaItem mediaItem;

        private ImageView playPause;
        private ImageView albumArt;
        private SeekBar seekBar;
        private TextView title;
        private TextView artist;
        private Runnable seekRunnable;

        private Bitmap albumArtData;

        private LoaderManager.LoaderCallbacks<Bitmap> albumArtCalbacks = new LoaderManager.LoaderCallbacks<Bitmap>() {
            @Override
            public Loader<Bitmap> onCreateLoader(int id, Bundle args) {
                return new AlbumArtLoader(getContext(), (MediaItem) args.getParcelable(ARG_MEDIA_ITEM));
            }

            @Override
            public void onLoadFinished(Loader<Bitmap> loader, Bitmap data) {
                albumArtData = data;
                if (albumArt != null) {
                    albumArt.setImageBitmap(albumArtData);
                }
            }

            @Override
            public void onLoaderReset(Loader<Bitmap> loader) {
                albumArtData = null;
            }
        };

        void onMetaData(@Nullable MediaItem item) {
            boolean itemChanged = mediaItem != item;
            this.mediaItem = item;
            if (getView() != null) {
                title.setText(mediaItem == null ? "" : mediaItem.title);
                artist.setText(mediaItem == null ? "" : mediaItem.artist);

                playPause.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);
                seekBar.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);
                title.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);
                artist.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);
                albumArt.setVisibility(item == null ? View.INVISIBLE : View.VISIBLE);

                albumArt.setImageBitmap(albumArtData);

                Bundle loaderArgs = new Bundle();
                loaderArgs.putParcelable(ARG_MEDIA_ITEM, item);

                if (!itemChanged) {
                    getLoaderManager().initLoader(0, loaderArgs, albumArtCalbacks);
                } else {
                    getLoaderManager().restartLoader(0, loaderArgs, albumArtCalbacks);
                }
            }
        }

        @Override
        public void onSeekComplete(Player player) {
            float currentPosition = player.getCurrentPosition();
            float duration = player.getDuration();
            float max = seekBar.getMax();
            int progress = duration == 0 ? 0 : MathUtil.clamp(
                    (int) ((currentPosition / duration) * max + .5f),
                    0, seekBar.getMax());
            seekBar.setProgress(progress);
        }

        @Override
        public void onPlayStateChanged(Player player) {
            updatePlayState();
        }

        void attach(final MonitorPlayer player) {
            player.setPlaybackListener(this);

            if (getView() == null) {
                throw new IllegalStateException();
            }

            title = ((TextView) getView().findViewById(R.id.monitor_title));
            artist = ((TextView) getView().findViewById(R.id.monitor_artist));
            seekBar = (SeekBar) getView().findViewById(R.id.monitor_seek);
            playPause = ((ImageView) getView().findViewById(R.id.monitor_play_pause));
            albumArt = (ImageView) getView().findViewById(R.id.monitor_album_art);

            onMetaData(mediaItem);

            playPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mediaItem != null) {
                        player.togglePlayPause();
                    }
                }
            });
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && player.getDuration() > 0) {
                        seekTo((int) (((float) progress) * ((float) player.getDuration())
                                / (float) seekBar.getMax()));
                    }
                }

                @Override
                public void onStartTrackingTouch(SeekBar seekBar) {

                }

                @Override
                public void onStopTrackingTouch(SeekBar seekBar) {

                }
            });
        }

        void detach() {
            player.setPlaybackListener(null);
            seekHandler.removeCallbacks(seekRunnable);
        }

        void updatePlayState() {
            playPause.setImageResource(player.isPlaying() ?
                    R.drawable.ic_pause : R.drawable.ic_play_arrow);
            if (player.isPlaying()) {
                seekRunnable = new Runnable() {
                    @Override
                    public void run() {
                        onSeekComplete(player);
                        seekHandler.postDelayed(this, player.getDuration() / seekBar.getMax());
                    }
                };
                seekHandler.post(seekRunnable);
            } else {
                seekHandler.removeCallbacks(seekRunnable);
            }
        }
    }

    private void seekTo(int duration) {
        player.seekTo(duration);
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
