package com.keyboardr.bluejay.ui.playlist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.service.PlaylistServiceClient;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.Date;
import java.util.List;

/**
 * Shows info about currently playing set (e.g. number of tracks, estimated finish time)
 */

public class SetInfoFragment extends Fragment {

  public interface Holder {

    List<MediaSessionCompat.QueueItem> getPlaylist();

    int getCurrentTrackIndex();

    long getCurrentPosition();

  }

  private TextView trackCount;
  private TextView runTime;

  private BroadcastReceiver updateReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      update();
    }
  };

  public void update() {
    if (getView() == null) {
      return;
    }
    List<MediaSessionCompat.QueueItem> queue = getParent().getPlaylist();
    int numTracks = queue.size();
    int currentIndex = getParent().getCurrentTrackIndex();

    trackCount.setText(getContext().getString(R.string.info_track_count,
        Math.min(currentIndex + 1, numTracks), numTracks));

    long timeLeft = 0;
    for (int i = currentIndex; i < numTracks; i++) {
      timeLeft += PlaylistServiceClient.mediaItemFromQueueItem(queue.get(i))
          .getDuration();
    }
    if (currentIndex < numTracks) {
      timeLeft -= getParent().getCurrentPosition();
    }
    runTime.setText(getString(R.string.info_end_time,
        DateFormat.getTimeFormat(getContext()).format(
            new Date(timeLeft + System.currentTimeMillis()))));
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    FragmentUtils.checkParent(this, Holder.class);
  }

  private Holder getParent() {
    return FragmentUtils.getParentChecked(this, Holder.class);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_set_info, container, false);
    trackCount = (TextView) view.findViewById(R.id.info_track_count);
    runTime = (TextView) view.findViewById(R.id.info_run_time);
    return view;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    update();
  }

  @Override
  public void onStart() {
    super.onStart();
    update();
    getContext().registerReceiver(updateReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
  }

  @Override
  public void onStop() {
    super.onStop();
    getContext().unregisterReceiver(updateReceiver);
  }
}
