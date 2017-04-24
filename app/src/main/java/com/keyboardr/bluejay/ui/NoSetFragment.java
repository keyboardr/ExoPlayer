package com.keyboardr.bluejay.ui;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.SetMetadata;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.Date;

public class NoSetFragment extends Fragment {

  public interface Holder {
    void startNewSetlist(SetMetadata setMetadata);

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
    final View newSetlist = view.findViewById(R.id.new_setlist);

    view.findViewById(R.id.new_setlist_container).setTransitionName(
        getString(R.string.shared_element_setlist_container));

    final TextInputEditText newSetlistName = (TextInputEditText) view.findViewById(
        R.id.new_setlist_name);
    if (savedInstanceState == null) {
      TextInputLayout newSetlistNameLayout = (TextInputLayout) view.findViewById(
          R.id.new_setlist_name_holder);
      newSetlistNameLayout.setHintAnimationEnabled(false);
      newSetlistName.setText(DateFormat.getDateFormat(getContext()).format(new Date()));
      newSetlistNameLayout.setHintAnimationEnabled(true);
    }
    newSetlistName.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {
      }

      @Override
      public void afterTextChanged(Editable s) {
        newSetlist.setEnabled(s.length() != 0);
      }
    });

    newSetlist.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        // TODO: 4/23/2017 add real metadata
        FragmentUtils.getParentChecked(NoSetFragment.this, Holder.class)
            .startNewSetlist(new SetMetadata(newSetlistName.getText().toString(), false));
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

    View soundCheck = view.findViewById(R.id.sound_check);
    soundCheck.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        FragmentUtils.getParentChecked(NoSetFragment.this, Holder.class)
            .startNewSetlist(new SetMetadata(getString(R.string.sound_check), true));
      }
    });
  }
}
