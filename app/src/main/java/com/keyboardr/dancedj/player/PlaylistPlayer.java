package com.keyboardr.dancedj.player;

import android.content.Context;
import android.support.annotation.NonNull;

public class PlaylistPlayer extends Player {

    public PlaylistPlayer(@NonNull Context context) {
        super(context);
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
