package com.keyboardr.dancedj;

import android.content.ContentResolver;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelFileDescriptor;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.util.MathUtil;

import java.io.IOException;

import static android.content.ContentValues.TAG;

public class MonitorControlsFragment extends Fragment implements MediaPlayer.OnPreparedListener {

    @SuppressWarnings("unused")
    public static MonitorControlsFragment newInstance() {
        return new MonitorControlsFragment();
    }

    private MediaPlayer mediaPlayer;
    private UiUpdater uiUpdater = new UiUpdater();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        mediaPlayer.setOnPreparedListener(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitor_controls, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        uiUpdater.attach(mediaPlayer);
    }

    @Override
    public void onDestroyView() {
        uiUpdater.detach();
        super.onDestroyView();
    }

    public void playMedia(MediaItem mediaItem) {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
        }
        mediaPlayer.reset();
        uiUpdater.onMetaData(mediaItem);
        try {
            Uri uri = new Uri.Builder().scheme(ContentResolver.SCHEME_FILE).path(mediaItem.path).build();

            ParcelFileDescriptor r = getContext().getContentResolver().openFileDescriptor(uri, "r");
            if (r == null) {
                Log.w(TAG, "Unable to get ParcelFileDescriptor for path: " + mediaItem.path);
                return;
            }
            mediaPlayer.setDataSource(r.getFileDescriptor());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        mediaPlayer.prepareAsync();
    }

    @Override
    public void onPrepared(MediaPlayer mediaPlayer) {
        mediaPlayer.start();
        uiUpdater.updatePlayState();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mediaPlayer.release();
    }

    private class UiUpdater implements MediaPlayer.OnSeekCompleteListener, MediaPlayer.OnCompletionListener {

        private Handler seekHandler = new Handler();

        private MediaPlayer mediaPlayer;
        @Nullable
        private MediaItem mediaItem;

        private ImageView playPause;
        private SeekBar seekBar;
        private TextView title;
        private TextView artist;
        private Runnable seekRunnable;

        void onMetaData(@Nullable MediaItem item) {
            this.mediaItem = item;
            if (getView() != null) {
                title.setText(mediaItem == null ? "" : mediaItem.title);
                artist.setText(mediaItem == null ? "" : mediaItem.artist);
            }
        }

        @Override
        public void onSeekComplete(MediaPlayer mediaPlayer) {
            float currentPosition = mediaPlayer.getCurrentPosition();
            float duration = mediaPlayer.getDuration();
            float max = seekBar.getMax();
            int progress = MathUtil.clamp(
                    (int) ((currentPosition / duration) * max + .5f),
                    0, seekBar.getMax());
            //Log.d(TAG, "currentPosition: " + currentPosition + ", duration: " + duration + ", progress: " + progress);
            seekBar.setProgress(progress);
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {
            updatePlayState();
        }

        void attach(final MediaPlayer mediaPlayer) {
            mediaPlayer.setOnSeekCompleteListener(this);
            mediaPlayer.setOnCompletionListener(this);
            this.mediaPlayer = mediaPlayer;

            if (getView() == null) {
                throw new IllegalStateException();
            }

            title = ((TextView) getView().findViewById(R.id.monitor_title));
            artist = ((TextView) getView().findViewById(R.id.monitor_artist));
            seekBar = (SeekBar) getView().findViewById(R.id.monitor_seek);
            playPause = ((ImageView) getView().findViewById(R.id.monitor_play_pause));

            onMetaData(mediaItem);

            playPause.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (mediaItem != null) {
                        togglePlayPause();
                    }
                }
            });
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override
                public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && mediaPlayer.getDuration() > 0) {
                        seekTo((int) (((float) progress) * ((float) mediaPlayer.getDuration())
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
            mediaPlayer.setOnCompletionListener(null);
            mediaPlayer.setOnSeekCompleteListener(null);
            mediaPlayer = null;
            seekHandler.removeCallbacks(seekRunnable);
        }

        void updatePlayState() {
            playPause.setImageResource(mediaPlayer.isPlaying() ?
                    R.drawable.ic_pause : R.drawable.ic_play_arrow);
            if (mediaPlayer.isPlaying()) {
                seekRunnable = new Runnable() {
                    @Override
                    public void run() {
                        onSeekComplete(mediaPlayer);
                        seekHandler.postDelayed(this, mediaPlayer.getDuration() / seekBar.getMax());
                    }
                };
                seekHandler.post(seekRunnable);
            } else {
                seekHandler.removeCallbacks(seekRunnable);
            }
        }
    }

    private void togglePlayPause() {
        if (mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        } else {
            mediaPlayer.start();
        }
        uiUpdater.updatePlayState();
    }

    private void seekTo(int duration) {
        mediaPlayer.seekTo(duration);
    }
}
