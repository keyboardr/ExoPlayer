package com.keyboardr.bluejay.ui.playlist;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.PlaybackFinishEvent;
import com.keyboardr.bluejay.bus.event.PlaylistErrorEvent;
import com.keyboardr.bluejay.bus.event.QueueChangeEvent;
import com.keyboardr.bluejay.bus.event.TrackIndexEvent;
import com.keyboardr.bluejay.service.PlaylistServiceClient;
import com.keyboardr.bluejay.util.FragmentUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.Date;
import java.util.List;

/**
 * Shows info about currently playing set (e.g. number of tracks, estimated finish time)
 */

public class SetInfoFragment extends Fragment {

  public interface Holder {

    List<MediaSessionCompat.QueueItem> getPlaylist();

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

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onTrackIndexEvent(@NonNull final TrackIndexEvent event) {
    update();
  }

  @Subscribe(threadMode = ThreadMode.MAIN)
  public void onQueueChangeEvent(@NonNull final QueueChangeEvent event) {
    update();
  }

  public void update() {
    if (getView() == null) {
      return;
    }
    List<MediaSessionCompat.QueueItem> queue = getParent().getPlaylist();
    int numTracks = queue.size();
    int currentIndex = Buses.PlaylistUtils.getCurrentTrackIndex();

    trackCount.setText(getContext().getString(R.string.info_track_count,
        Math.min(currentIndex + 1, numTracks), numTracks));

    if (numTracks == 0) {
      runTime.setVisibility(View.INVISIBLE);
      return;
    }
    runTime.setVisibility(View.VISIBLE);

    long timeLeft = 0;
    long endTime;
    for (int i = currentIndex; i < numTracks; i++) {
      timeLeft += PlaylistServiceClient.mediaItemFromQueueItem(queue.get(i))
          .getDuration();
    }
    if (currentIndex < numTracks) {
      timeLeft -= getParent().getCurrentPosition();
    }

    PlaybackFinishEvent playbackFinishEvent = Buses.PLAYLIST.getStickyEvent(
        PlaybackFinishEvent.class);
    if (playbackFinishEvent == null) {
      endTime = timeLeft + System.currentTimeMillis();
    } else {
      endTime = playbackFinishEvent.finishTime;
    }
    runTime.setText(getString(R.string.info_end_time,
        DateFormat.getTimeFormat(getContext()).format(
            new Date(endTime))));
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
    Buses.PLAYLIST.register(this);
    getContext().registerReceiver(updateReceiver, new IntentFilter(Intent.ACTION_TIME_TICK));
  }

  @Override
  public void onStop() {
    super.onStop();
    Buses.PLAYLIST.unregister(this);
    getContext().unregisterReceiver(updateReceiver);
  }

  @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
  public void onPlaylistErrorEvent(@NonNull PlaylistErrorEvent errorEvent) {
    PlaylistErrorEvent.ErrorCode errorCode = errorEvent.getTopError();
    ErrorFragment errorFragment = (ErrorFragment) getChildFragmentManager().findFragmentById(
        R.id.error_holder);
    if (errorCode != null) {
      if (errorFragment == null || errorFragment.getErrorCode() != errorCode) {
        getChildFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.slide_in_from_bottom, R.anim.slide_out_to_bottom)
            .replace(R.id.error_holder, ErrorFragment.newInstance(errorCode))
            .commit();
      }
    } else {
      if (errorFragment != null) {
        getChildFragmentManager().beginTransaction()
            .setCustomAnimations(R.anim.slide_in_from_bottom, R.anim.slide_out_to_bottom)
            .remove(errorFragment)
            .commit();
      }
    }
  }
}
