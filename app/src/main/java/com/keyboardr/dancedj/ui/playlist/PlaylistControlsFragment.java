package com.keyboardr.dancedj.ui.playlist;

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

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.model.MediaItem;
import com.keyboardr.dancedj.player.PlaylistPlayer;
import com.keyboardr.dancedj.service.PlaylistServiceClient;
import com.keyboardr.dancedj.ui.AudioSelectionManager;
import com.keyboardr.dancedj.ui.PlayerControlsUpdater;
import com.keyboardr.dancedj.util.FragmentUtils;

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
        void onMediaListLoaded();
    }

    @Nullable
    private PlaylistServiceClient player;
    @Nullable
    private PlayerControlsUpdater uiUpdater;
    private AudioSelectionManager audioSelectionManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_playlist_controls, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        updateView();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        updateView();
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
        if (view != null && player != null) {
            if (uiUpdater == null) {
                uiUpdater = new PlaylistControlsUpdater(view, player, getLoaderManager());
            }
            if (audioSelectionManager == null) {
                audioSelectionManager = new AudioSelectionManager(getContext(), (Spinner) view.findViewById(R.id.controls_spinner), player, this);
            }
        } else {
            if (uiUpdater != null) {
                uiUpdater.detach();
                uiUpdater = null;
            }
            if (audioSelectionManager != null) {
                audioSelectionManager.detach();
                audioSelectionManager = null;
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

    @NonNull
    public List<PlaylistPlayer.PlaylistItem> getPlaylist() {
        return player != null ? player.getMediaList() : Collections.<PlaylistPlayer.PlaylistItem>emptyList();
    }

    public int getCurrentTrackIndex() {
        return player != null ? player.getCurrentMediaIndex() : 0;
    }

    private Holder getParent() {
        return FragmentUtils.getParent(this, Holder.class);
    }

}
