package com.keyboardr.bluejay.ui.monitor;

import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.player.MonitorPlayer;
import com.keyboardr.bluejay.ui.AudioSelectionManager;
import com.keyboardr.bluejay.ui.PlayerControlsUpdater;
import com.keyboardr.bluejay.util.CachedLoader;

public class MonitorControlsFragment extends Fragment {

    @SuppressWarnings("unused")
    public static MonitorControlsFragment newInstance() {
        return new MonitorControlsFragment();
    }

    private PlayerControlsUpdater uiUpdater;
    private MonitorPlayer player;
    private AudioSelectionManager audioSelectionManager;

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
        audioSelectionManager = new AudioSelectionManager(getContext(),
                (Spinner) view.findViewById(R.id.controls_spinner), player, null);
        uiUpdater = new MonitorControlsUpdater(view, player, getLoaderManager());
    }

    @Override
    public void onDestroyView() {
        uiUpdater.detach();
        audioSelectionManager.detach();
        super.onDestroyView();
    }

    public void playMedia(MediaItem mediaItem) {
        player.play(mediaItem, true);
        uiUpdater.onMetaData();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.release();
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
