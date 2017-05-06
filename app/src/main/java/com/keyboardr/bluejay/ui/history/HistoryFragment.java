package com.keyboardr.bluejay.ui.history;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.SetMetadataEvent;
import com.keyboardr.bluejay.model.FilterInfo;
import com.keyboardr.bluejay.model.SetMetadata;
import com.keyboardr.bluejay.provider.SetlistContract;
import com.keyboardr.bluejay.util.FragmentUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

public class HistoryFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

  private ListView setlists;
  private SimpleCursorAdapter setlistAdapter;

  public void onItemSelected(long id) {
    getParent().setHistoryFilter(new FilterInfo(id));
  }

  public interface Holder {
    void setHistoryFilter(FilterInfo filter);
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
      Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_history_filter, container, false);
    setlists = (ListView) view.findViewById(R.id.history_items);
    setlistAdapter = new SimpleCursorAdapter(getContext(),
        R.layout.item_history,
        null, new String[]{SetlistContract.NAME}, new int[]{android.R.id.text1}, 0) {
      @Override
      public View newView(Context context, Cursor cursor, ViewGroup parent) {
        final View newView = super.newView(context, cursor, parent);
        newView.setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            int position = setlists.getPositionForView(newView);
            setlists.setItemChecked(position, true);
            onItemSelected(setlists.getItemIdAtPosition(position));
          }
        });
        newView.findViewById(R.id.setlist_more).setOnClickListener(new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            PopupMenu popupMenu = new PopupMenu(v.getContext(), v);
            popupMenu.inflate(R.menu.item_setlist);
            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
              @Override
              public boolean onMenuItemClick(MenuItem item) {
                String name = ((TextView) newView.findViewById(android.R.id.text1)).getText()
                    .toString();
                long id = setlists.getItemIdAtPosition(setlists.getPositionForView(newView));
                return HistoryItemUtil.onMenuItemClick(HistoryFragment.this, name, id,
                    item.getItemId());
              }
            });
            popupMenu.show();
          }
        });
        return newView;
      }
    };
    setlists.setEmptyView(view.findViewById(android.R.id.empty));
    setlists.setAdapter(setlistAdapter);
    return view;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    onItemSelected(-1);
    getLoaderManager().initLoader(0, null, this);
  }

  @Override
  public void onStart() {
    super.onStart();
    Buses.PLAYLIST.register(this);
  }

  @Override
  public void onStop() {
    super.onStop();
    Buses.PLAYLIST.unregister(this);
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onSetMetadataEvent(SetMetadataEvent event) {
    getLoaderManager().destroyLoader(0);
    getLoaderManager().restartLoader(0, null, this);
  }

  @Override
  public Loader<Cursor> onCreateLoader(int id, Bundle args) {
    SetMetadata setMetadata = SetMetadataEvent.getSetMetadata(Buses.PLAYLIST);
    Long setlistId = setMetadata == null ? null : setMetadata.setlistId;
    return new CursorLoader(getContext(), SetlistContract.CONTENT_URI, null,
        SetlistContract._ID + " != ?",
        new String[]{String.valueOf(setlistId == null ? -1 : setlistId)},
        SetlistContract.DATE + " DESC");
  }

  @Override
  public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
    setlistAdapter.changeCursor(data);
    if (setlistAdapter.getCount() > 0) {
      setlists.setItemChecked(0, true);
    }

    long checkedItemId = -1;
    if (setlists.getCheckedItemCount() > 0) {
      checkedItemId = setlists.getCheckedItemIds()[0];
    }
    onItemSelected(checkedItemId);
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    setlistAdapter.changeCursor(null);
  }

  @NonNull
  private Holder getParent() {
    return FragmentUtils.getParentChecked(this, Holder.class);
  }
}
