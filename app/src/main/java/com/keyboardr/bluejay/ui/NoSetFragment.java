package com.keyboardr.bluejay.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.util.FragmentUtils;

public class NoSetFragment extends Fragment {

  public interface Holder {
    void startNewSetlist();

    void editMetadata();

    void editShortlists();
  }

  public static NoSetFragment newInstance() {
    return new NoSetFragment();
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_no_setlist, container, false);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    FragmentUtils.checkParent(this, Holder.class);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    view.findViewById(R.id.new_setlist).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        FragmentUtils.getParentChecked(NoSetFragment.this, Holder.class).startNewSetlist();
      }
    });

    boolean allowEditors = getResources().getBoolean(R.bool.allow_library_editor);

    View editMetadata = view.findViewById(R.id.edit_metadata);
    editMetadata.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        FragmentUtils.getParentChecked(NoSetFragment.this, Holder.class).editMetadata();
      }
    });
    editMetadata.setVisibility(allowEditors ? View.VISIBLE : View.GONE);

    View editShortlists = view.findViewById(R.id.edit_shortlists);
    editShortlists.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        FragmentUtils.getParentChecked(NoSetFragment.this, Holder.class).editShortlists();
      }
    });
    editShortlists.setVisibility(allowEditors ? View.VISIBLE : View.GONE);
  }
}
