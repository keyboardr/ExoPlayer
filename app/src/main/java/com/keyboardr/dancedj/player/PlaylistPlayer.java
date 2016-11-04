package com.keyboardr.dancedj.player;

import android.content.Context;
import android.support.annotation.NonNull;

import com.google.android.exoplayer2.source.ConcatenatingMediaSource;
import com.google.android.exoplayer2.source.MediaSource;
import com.keyboardr.dancedj.model.MediaItem;

import java.util.ArrayList;
import java.util.List;

public class PlaylistPlayer extends Player {

    private List<MediaItem> mediaItems = new ArrayList<>();

    public PlaylistPlayer(@NonNull Context context) {
        super(context);
    }

    public void addToQueue(@NonNull MediaItem mediaItem) {
        mediaItems.add(mediaItem);
        MediaSource[] trackSources = new MediaSource[mediaItems.size()];
        for (int i = 0; i < trackSources.length; i++) {
            trackSources[i] = getMediaSource(mediaItems.get(i));
        }
        MediaSource fullSource = new ConcatenatingMediaSource(trackSources);
        ensurePlayer().prepare(fullSource, false, false);
    }

    @Override
    public MediaItem getCurrentMediaItem() {
        if (mediaItems.size() == 0) {
            return null;
        }
        return  mediaItems.get(ensurePlayer().getCurrentPeriodIndex());
    }
    
    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public void togglePlayPause() {
        ensurePlayer().setPlayWhenReady(true);
    }
}
