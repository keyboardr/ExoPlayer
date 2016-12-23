package com.keyboardr.bluejay.ui.monitor.library;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.FilterInfo;
import com.keyboardr.bluejay.model.Shortlist;
import com.keyboardr.bluejay.provider.ShortlistManager;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.ArrayList;
import java.util.Set;

public class FilterFragment extends Fragment {

  private static final String STATE_SHORTLISTS = "shortlists";

  private Spinner sortSpinner;
  private RecyclerView shortlistsView;

  private Set<Shortlist> selectedShortlists = new ArraySet<>();
  private BroadcastReceiver shortlistsChangedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      shortlistsView.getAdapter().notifyDataSetChanged();
    }
  };

  public interface Holder {
    void setLibraryFilter(FilterInfo filter);

    ShortlistManager getShortlistManager();
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      ArrayList<Shortlist> shortlists = savedInstanceState.getParcelableArrayList(STATE_SHORTLISTS);
      if (shortlists != null) {
        selectedShortlists.addAll(shortlists);
      }
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_filter, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    sortSpinner = ((Spinner) view.findViewById(R.id.sort));
    sortSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        updateFilterInfo();
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {

      }
    });

    shortlistsView = (RecyclerView) view.findViewById(R.id.shortlists);
    shortlistsView.setLayoutManager(new LinearLayoutManager(getContext()));
    shortlistsView.setAdapter(new ShortlistAdapter<FilterShortlistViewHolder>(getParent()
        .getShortlistManager()) {

      @Override
      public FilterShortlistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FilterShortlistViewHolder(parent);
      }
    });

    LocalBroadcastManager.getInstance(getContext()).registerReceiver(shortlistsChangedReceiver,
        new IntentFilter(ShortlistManager.ACTION_SHORTLISTS_CHANGED));
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(shortlistsChangedReceiver);
  }

  @Override
  public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putParcelableArrayList(STATE_SHORTLISTS, new ArrayList<>(selectedShortlists));
  }

  private class FilterShortlistViewHolder extends ShortlistViewHolder {

    public FilterShortlistViewHolder(ViewGroup parent) {
      super(parent);
    }

    @Override
    protected void setChecked(boolean checked) {
      if (checked) {
        selectedShortlists.add(shortlist);
      } else {
        selectedShortlists.remove(shortlist);
      }
      updateFilterInfo();
    }

    @Override
    protected boolean isChecked() {
      return selectedShortlists.contains(shortlist);
    }
  }

  private void updateFilterInfo() {
    //noinspection WrongConstant
    getParent().setLibraryFilter(new FilterInfo(sortSpinner.getSelectedItemPosition(),
        selectedShortlists));
  }

  @NonNull
  private Holder getParent() {
    return FragmentUtils.getParentChecked(this, Holder.class);
  }
}
