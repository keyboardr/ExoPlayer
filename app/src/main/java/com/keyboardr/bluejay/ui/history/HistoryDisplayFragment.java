package com.keyboardr.bluejay.ui.history;

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
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import android.widget.TextView;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.SetMetadataEvent;
import com.keyboardr.bluejay.model.FilterInfo;
import com.keyboardr.bluejay.model.SetMetadata;
import com.keyboardr.bluejay.provider.SetlistContract;
import com.keyboardr.bluejay.ui.monitor.library.LibraryFragment;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

/**
 * Shows a history selector and a list of tracks for the selected setlist
 */

public class HistoryDisplayFragment extends Fragment implements LoaderManager
    .LoaderCallbacks<Cursor>, AdapterView.OnItemSelectedListener {

  private SimpleCursorAdapter setlistAdapter;
  private Spinner spinner;
  private View renameButton;
  private View deleteButton;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.history, container, false);
    setlistAdapter = new SimpleCursorAdapter(getContext(), android.R.layout.simple_spinner_item,
        null, new String[]{SetlistContract.NAME}, new int[]{android.R.id.text1}, 0);
    setlistAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    spinner = (Spinner) view.findViewById(R.id.setlist_selector);
    spinner.setAdapter(setlistAdapter);
    spinner.setOnItemSelectedListener(this);
    spinner.setEmptyView(view.findViewById(R.id.setlist_empty));

    renameButton = view.findViewById(R.id.rename);
    renameButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        SetlistRenameDialogFragment.newInstance(
            getSelectedSetlistName(), spinner.getSelectedItemId())
            .show(getChildFragmentManager(), null);
      }
    });
    deleteButton = view.findViewById(R.id.delete);
    deleteButton.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        DeleteSetlistDialogFragment.newInstance(
            getSelectedSetlistName(), spinner.getSelectedItemId())
            .show(getChildFragmentManager(), null);
      }
    });
    return view;
  }

  @NonNull
  private String getSelectedSetlistName() {
    return ((TextView) spinner.getSelectedView().findViewById(android.R.id.text1))
        .getText().toString();
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    onNothingSelected(spinner);
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
  }

  @Override
  public void onLoaderReset(Loader<Cursor> loader) {
    setlistAdapter.changeCursor(null);
  }

  @NonNull
  private LibraryFragment getLibraryFragment() {
    return (LibraryFragment) getChildFragmentManager().findFragmentById(R.id.history_library);
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
    getLibraryFragment().setLibraryFilter(new FilterInfo(id));
    renameButton.setEnabled(true);
    deleteButton.setEnabled(true);
    getChildFragmentManager().beginTransaction().show(getLibraryFragment()).commit();
  }

  @Override
  public void onNothingSelected(AdapterView<?> parent) {
    getLibraryFragment().setLibraryFilter(new FilterInfo(-1));
    renameButton.setEnabled(false);
    deleteButton.setEnabled(false);
    getChildFragmentManager().beginTransaction().hide(getLibraryFragment()).commit();
  }

}
