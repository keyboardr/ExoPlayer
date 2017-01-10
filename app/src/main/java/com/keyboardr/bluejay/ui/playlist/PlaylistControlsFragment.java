package com.keyboardr.bluejay.ui.playlist;

import android.media.AudioDeviceInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.player.PlaylistPlayer;
import com.keyboardr.bluejay.service.PlaylistServiceClient;
import com.keyboardr.bluejay.ui.AudioSelectionManager;
import com.keyboardr.bluejay.ui.PlayerControlsUpdater;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.Collections;
import java.util.List;

public class PlaylistControlsFragment extends Fragment implements AudioSelectionManager.DefaultDeviceSelector {

    @Override
    public boolean canBeDefault(AudioDeviceInfo deviceInfo) {
        return deviceInfo.getType() == AudioDeviceInfo.TYPE_USB_DEVICE;
    }

    @Override
    public void onNoDeviceFound() {
        // TODO: 11/6/2016 replace with error bar
        Toast.makeText(getContext(), "No USB Audio found", Toast.LENGTH_LONG).show();
    }

    public interface Holder extends PlaylistPlayer.PlaylistChangedListener {
        IBinder getPlaylistServiceBinder();

        void onMediaListLoaded();
    }

    private PlaylistServiceClient player;
    private PlayerControlsUpdater uiUpdater;
    private AudioSelectionManager audioSelectionManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        player = new PlaylistServiceClient(getParent().getPlaylistServiceBinder()) {
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
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_controls, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!AudioSelectionManager.SPINNER_ENABLED) {
            view.findViewById(R.id.controls_spinner).setVisibility(View.GONE);
        }
        uiUpdater = new PlaylistControlsUpdater(view, player, getLoaderManager());
        audioSelectionManager = new AudioSelectionManager(getContext(), (Spinner) view.findViewById(R.id.controls_spinner), player, this);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        uiUpdater.detach();
        uiUpdater = null;
        audioSelectionManager.detach();
        audioSelectionManager = null;
    }

    public void addToQueue(@NonNull MediaItem mediaItem) {
        player.addToQueue(mediaItem);
    }

    public void removeItem(int removeIndex) {
        player.removeItem(removeIndex);
    }

    public void moveItem(int oldIndex, int newIndex) {
        player.moveItem(oldIndex, newIndex);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        player.release();
    }

    @NonNull
    public List<PlaylistPlayer.PlaylistItem> getPlaylist() {
        return player != null ? player.getMediaList() : Collections.<PlaylistPlayer.PlaylistItem>emptyList();
    }

    public int getCurrentTrackIndex() {
        return player.getCurrentMediaIndex();
    }

    private Holder getParent() {
        return FragmentUtils.getParent(this, Holder.class);
    }

}
