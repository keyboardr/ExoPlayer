package com.keyboardr.bluejay.ui.shortlists;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;

/**
 * Fragment for editing, rearranging, and deleting shortlists
 */

public class ShortlistEditorFragment extends Fragment {

  public static ShortlistEditorFragment newInstance() {
    return new ShortlistEditorFragment();
  }

  public interface Holder {
    void closeShortlistEditor();
  }

  private RecyclerView recyclerView;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_shortlist_editor, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    recyclerView = (RecyclerView) view.findViewById(R.id.shortlist_recycler);

  }


  private class EditorShortlistViewHolder extends ShortlistViewHolder {
    public EditorShortlistViewHolder(ViewGroup parent) {
      super(parent, R.layout.item_shortlist_editor);
    }

    @Override
    public void onClick(View view) {
      // TODO: 3/20/2017
    }
  }
}
