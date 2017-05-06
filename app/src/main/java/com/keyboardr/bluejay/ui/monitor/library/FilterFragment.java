package com.keyboardr.bluejay.ui.monitor.library;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AlertDialog;
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
  private static final String ARG_FILTER_INFO = "filterInfo";

  private boolean updateReady;

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
  private AlertDialog dialog;

  public interface Holder {
    void setLibraryFilter(FilterInfo filter);
  }

  public static FilterFragment newInstance(@Nullable FilterInfo filterInfo) {
    FilterFragment filterFragment = new FilterFragment();
    Bundle args = new Bundle();
    args.putParcelable(ARG_FILTER_INFO, filterInfo == null ? FilterInfo.EMPTY : filterInfo);
    filterFragment.setArguments(args);
    return filterFragment;
  }

  @RestrictTo(RestrictTo.Scope.SUBCLASSES)
  public FilterFragment() {
    super();
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
    } else if (getArguments().containsKey(ARG_FILTER_INFO)) {
      FilterInfo existing = getArguments().getParcelable(ARG_FILTER_INFO);
      if (existing != null) {
        selectedShortlists.addAll(existing.requiredShortlists);
        deselectedShortlists.addAll(existing.disallowedShortlists);
        filterText = existing.filterString;
      }
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
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
    dialog = builder.create();
    return dialog;
  }

  @Override
  public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    if (dialog != null) {
      dialog.setView(view);
    }
    sortSpinner = ((Spinner) view.findViewById(R.id.sort));
    sortSpinner.post(new Runnable() {

      @Override
      public void run() {
        sortSpinner.setOnItemSelectedListener(new AdapterView
            .OnItemSelectedListener
            () {
          @Override
          public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
            applyShuffleIconTreatment(position);
            updateFilterInfo();
          }

          @Override
          public void onNothingSelected(AdapterView<?> adapterView) {
          }
        });
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

    if (savedInstanceState == null && getArguments().containsKey(ARG_FILTER_INFO)) {
      FilterInfo filterInfo = getArguments().getParcelable(ARG_FILTER_INFO);
      if (filterInfo != null) {
        filterTextInput.setText(filterText);
        sortSpinner.setSelection(filterInfo.sortMethod);
        sortToggle.setChecked(filterInfo.sortAscending);
      }
    }
    applyShuffleIconTreatment(sortSpinner.getSelectedItemPosition());
    updateReady = true;
  }

  private void applyShuffleIconTreatment(int sortMethod) {
    if (sortMethod == FilterInfo.SortMethod.SHUFFLE) {
      int currentTextColor = sortToggle.getCurrentTextColor();
      if (Color.alpha(currentTextColor) != 0) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofInt(sortToggle, "textColor",
            ColorUtils.setAlphaComponent(currentTextColor, 0));
        objectAnimator.setEvaluator(new ArgbEvaluator());
        objectAnimator.start();
      }
      sortToggle.setActivated(true);
    } else {
      int currentTextColor = sortToggle.getCurrentTextColor();
      if (Color.alpha(currentTextColor) != 0xFF) {
        ObjectAnimator objectAnimator = ObjectAnimator.ofInt(sortToggle, "textColor",
            ColorUtils.setAlphaComponent(currentTextColor, 0xFF));
        objectAnimator.setEvaluator(new ArgbEvaluator());
        objectAnimator.start();
      }
      sortToggle.setActivated(false);
    }
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
    if (!updateReady) {
      return;
    }
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
