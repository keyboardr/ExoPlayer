package com.keyboardr.dancedj;

import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.player.MonitorPlayer;
import com.keyboardr.dancedj.util.MathUtil;

public class MonitorControlsFragment extends Fragment {

    @SuppressWarnings("unused")
    public static MonitorControlsFragment newInstance() {
        return new MonitorControlsFragment();
    }

    private UiUpdater uiUpdater = new UiUpdater();
    private MonitorPlayer player;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        uiUpdater.attach(player);
    }

    @Override
    public void onDestroyView() {
        uiUpdater.detach();
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

    private class UiUpdater implements MonitorPlayer.PlaybackListener {

        private Handler seekHandler = new Handler();

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
        public void onSeekComplete(MonitorPlayer player) {
            float currentPosition = player.getCurrentPosition();
            float duration = player.getDuration();
            float max = seekBar.getMax();
            int progress = duration == 0 ? 0 : MathUtil.clamp(
                    (int) ((currentPosition / duration) * max + .5f),
                    0, seekBar.getMax());
            seekBar.setProgress(progress);
        }

        @Override
        public void onPlayStateChanged(MonitorPlayer player) {
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
}
