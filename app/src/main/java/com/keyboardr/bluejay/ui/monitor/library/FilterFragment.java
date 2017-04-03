package com.keyboardr.bluejay.ui.monitor.library;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.ArraySet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.Spinner;
import android.widget.ToggleButton;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.FilterInfo;
import com.keyboardr.bluejay.model.Shortlist;
import com.keyboardr.bluejay.provider.ShortlistManager;
import com.keyboardr.bluejay.ui.shortlists.CheckableShortlistViewHolder;
import com.keyboardr.bluejay.ui.shortlists.ShortlistAdapter;
import com.keyboardr.bluejay.util.FragmentUtils;

import java.util.ArrayList;
import java.util.Set;

public class FilterFragment extends DialogFragment {

  private static final String STATE_SHORTLISTS = "shortlists";
  private static final String STATE_DESELECTED_SHORTLISTS = "deselected_shortlists";

  private Spinner sortSpinner;
  private ToggleButton sortToggle;

  private RecyclerView shortlistsView;

  private Set<Shortlist> selectedShortlists = new ArraySet<>();
  private Set<Shortlist> deselectedShortlists = new ArraySet<>();
  private String filterText;
  private BroadcastReceiver shortlistsChangedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      switch (intent.getIntExtra(ShortlistManager.EXTRA_CHANGE_TYPE, ShortlistManager.Change
          .UNKNOWN)) {
        case ShortlistManager.Change.REMOVE:
          Shortlist shortlist = intent.getParcelableExtra(ShortlistManager.EXTRA_SHORTLIST);
          boolean modified;
          modified = selectedShortlists.remove(shortlist);
          modified |= deselectedShortlists.remove(shortlist);
          if (modified) {
            updateFilterInfo();
          }
          break;
      }
      if (shortlistsView != null && shortlistsView.getAdapter() != null) {
        shortlistsView.getAdapter().notifyDataSetChanged();
      }
    }
  };

  public interface Holder {
    void setLibraryFilter(FilterInfo filter);
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    if (savedInstanceState != null) {
      ArrayList<Shortlist> selected = savedInstanceState.getParcelableArrayList(STATE_SHORTLISTS);
      if (selected != null) {
        selectedShortlists.addAll(selected);
      }
      ArrayList<Shortlist> deselected = savedInstanceState.getParcelableArrayList
          (STATE_DESELECTED_SHORTLISTS);
      if (deselected != null) {
        deselectedShortlists.addAll(deselected);
      }
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_filter, container, false);
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
    builder.setPositiveButton(R.string.done, new DialogInterface.OnClickListener() {
      @Override
      public void onClick(DialogInterface dialogInterface, int i) {
        dialogInterface.dismiss();
      }
    });
    FrameLayout frameLayout = new FrameLayout(builder.getContext());
    frameLayout.setMinimumHeight(
        getResources().getDimensionPixelSize(R.dimen.min_filter_dialog_height));
    View contentView = onCreateView(LayoutInflater.from(builder.getContext()), frameLayout,
        savedInstanceState);
    builder.setView(frameLayout);
    frameLayout.addView(contentView);
    //noinspection ConstantConditions
    onViewCreated(contentView, savedInstanceState);
    return builder.create();
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

    IntentFilter filter = new IntentFilter(ShortlistManager.ACTION_SHORTLISTS_CHANGED);
    filter.addAction(ShortlistManager.ACTION_SHORTLISTS_READY);
    LocalBroadcastManager.getInstance(getContext()).registerReceiver(shortlistsChangedReceiver,
        filter);

    shortlistsView = (RecyclerView) view.findViewById(R.id.shortlists);
    shortlistsView.setLayoutManager(new LinearLayoutManager(getContext()));
    shortlistsView.setAdapter(new ShortlistAdapter<FilterShortlistViewHolder>(
        ShortlistManager.getInstance(getContext())) {

      @Override
      public FilterShortlistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new FilterShortlistViewHolder(parent);
      }
    });

    final EditText filterTextInput = (EditText) view.findViewById(R.id.filter_text);
    filterTextInput.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
        filterText = charSequence.toString();
        updateFilterInfo();
      }

      @Override
      public void afterTextChanged(Editable editable) {
      }
    });

    view.findViewById(R.id.filter_clear).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        filterTextInput.setText("");
      }
    });

    sortToggle = (ToggleButton) view.findViewById(R.id.sort_direction);
    sortToggle.setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override
          public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
            updateFilterInfo();
          }
        });
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
    outState.putParcelableArrayList(STATE_DESELECTED_SHORTLISTS,
        new ArrayList<>(deselectedShortlists));
  }

  private class FilterShortlistViewHolder extends CheckableShortlistViewHolder {

    public FilterShortlistViewHolder(ViewGroup parent) {
      super(parent);
    }

    @Override
    protected void toggleState() {
      Boolean oldState = getCheckedState();
      if (oldState == null) {
        selectedShortlists.add(shortlist);
      } else {
        if (oldState) {
          selectedShortlists.remove(shortlist);
          deselectedShortlists.add(shortlist);
        } else {
          deselectedShortlists.remove(shortlist);
        }
      }
      updateFilterInfo();
    }

    @Override
    @Nullable
    protected Boolean getCheckedState() {
      return selectedShortlists.contains(shortlist)
          ? Boolean.TRUE
          : (deselectedShortlists.contains(shortlist)
                 ? Boolean.FALSE
                 : null);
    }
  }

  private void updateFilterInfo() {
    //noinspection WrongConstant
    getParent().setLibraryFilter(new FilterInfo(sortSpinner.getSelectedItemPosition(),
        sortToggle.isChecked(),
        selectedShortlists, deselectedShortlists, filterText));
  }

  @NonNull
  private Holder getParent() {
    return FragmentUtils.getParentChecked(this, Holder.class);
  }
}
