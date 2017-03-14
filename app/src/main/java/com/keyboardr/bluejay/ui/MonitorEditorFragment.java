package com.keyboardr.bluejay.ui;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.player.MonitorPlayer;
import com.keyboardr.bluejay.provider.ShortlistManager;
import com.keyboardr.bluejay.ui.monitor.library.MetadataFragment;
import com.keyboardr.bluejay.util.FragmentUtils;

public class MonitorEditorFragment extends Fragment implements MetadataFragment.Holder {

  public interface Holder {

    void closeMetadataEditor();
    @Nullable
    MediaItem getCurrentMonitorTrack();

    ShortlistManager getShortlistManager();

  }
  public static MonitorEditorFragment newInstance() {
    return new MonitorEditorFragment();
  }

  private BroadcastReceiver monitorChangedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      MediaItem track = FragmentUtils.getParentChecked(MonitorEditorFragment.this,
          Holder.class).getCurrentMonitorTrack();
      if (track == null) {
        getChildFragmentManager().beginTransaction().replace(R.id.metadata_editor, EmptyFragment
            .newInstance()).commitNow();
        //noinspection ConstantConditions
        getView().findViewById(R.id.metadata_empty).setVisibility(View.VISIBLE);
      } else {
        getChildFragmentManager().beginTransaction().replace(R.id.metadata_editor,
            MetadataFragment.newInstance(track)).commitNow();
        //noinspection ConstantConditions
        getView().findViewById(R.id.metadata_empty).setVisibility(View.GONE);
      }
    }
  };

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    FragmentUtils.checkParent(this, Holder.class);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_editor, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    view.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        FragmentUtils.getParentChecked(MonitorEditorFragment.this, Holder.class)
            .closeMetadataEditor();
      }
    });
  }

  @Override
  public void onStart() {
    super.onStart();
    monitorChangedReceiver.onReceive(getContext(), null);
    LocalBroadcastManager.getInstance(getContext()).registerReceiver(monitorChangedReceiver,
        new IntentFilter(MonitorPlayer.ACTION_MONITOR_TRACK_CHANGED));
  }

  @Override
  public void onStop() {
    super.onStop();
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(monitorChangedReceiver);
  }

  @Override
  public ShortlistManager getShortlistManager() {
    return FragmentUtils.getParentChecked(this, Holder.class).getShortlistManager();
  }
}
