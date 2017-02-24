package com.keyboardr.bluejay.ui.monitor.library;

import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.TypedValue;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.flexbox.FlexboxLayoutManager;
import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.provider.ShortlistManager;
import com.keyboardr.bluejay.util.FragmentUtils;

/**
 * Metadata editor for a track
 */

public class MetadataFragment extends DialogFragment {

  private ShortlistManager shortlistManager;
  private RecyclerView shortlistsView;
  private TextInputEditText newShortlistText;

  public interface Holder {
    ShortlistManager getShortlistManager();
  }

  private static final String ARG_MEDIA_ITEM = "mediaItem";
  private MediaItem mediaItem;

  private BroadcastReceiver shortlistsChangedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      shortlistsView.getAdapter().notifyDataSetChanged();
    }
  };

  @NonNull
  public static <P extends Fragment & Holder>
  MetadataFragment show(@NonNull P parent, @NonNull MediaItem mediaItem) {
    FragmentManager fragmentManager = parent.getChildFragmentManager();
    return show(fragmentManager, mediaItem);
  }

  @NonNull
  public static <P extends FragmentActivity & Holder>
  MetadataFragment show(@NonNull P parent, @NonNull MediaItem mediaItem) {
    FragmentManager fragmentManager = parent.getSupportFragmentManager();
    return show(fragmentManager, mediaItem);
  }

  @NonNull
  private static MetadataFragment show(@NonNull FragmentManager fragmentManager,
                                       @NonNull MediaItem mediaItem) {
    MetadataFragment metadataFragment = newInstance(mediaItem);
    metadataFragment.show(fragmentManager, "");
    return metadataFragment;
  }

  public static MetadataFragment newInstance(@NonNull MediaItem mediaItem) {
    Bundle args = new Bundle();
    args.putParcelable(ARG_MEDIA_ITEM, mediaItem);
    MetadataFragment fragment = new MetadataFragment();
    fragment.setArguments(args);
    return fragment;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    this.mediaItem = getArguments().getParcelable(ARG_MEDIA_ITEM);
    shortlistManager = FragmentUtils.getParentChecked(this, Holder.class).getShortlistManager();
    if (shortlistManager == null) {
      dismiss();
    }
  }

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_metadata, container, false);
  }

  @Override
  public void onViewCreated(final View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ((TextView) view.findViewById(R.id.title)).setText(mediaItem.title);
    ((TextView) view.findViewById(R.id.artist)).setText(mediaItem.artist);

    shortlistsView = (RecyclerView) view.findViewById(R.id.shortlists);
    shortlistsView.setLayoutManager(new FlexboxLayoutManager());
    shortlistsView.setAdapter(new ShortlistAdapter<MetadataShortlistViewHolder>(shortlistManager) {

      @Override
      public MetadataShortlistViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MetadataShortlistViewHolder(parent);
      }
    });

    final View addShortlist = view.findViewById(R.id.add_shortlist);
    newShortlistText = (TextInputEditText) view.findViewById(R.id
        .new_shortlist);

    addShortlist.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        createShortlist();
      }
    });
    addShortlist.setEnabled(newShortlistText.length() != 0);

    newShortlistText.addTextChangedListener(new TextWatcher() {

      @Override
      public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {
      }

      @Override
      public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        addShortlist.setEnabled(charSequence.length() != 0);
      }

      @Override
      public void afterTextChanged(Editable editable) {
      }
    });
    newShortlistText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
      @Override
      public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
        return createShortlist();
      }
    });

    LocalBroadcastManager.getInstance(getContext()).registerReceiver(shortlistsChangedReceiver,
        new IntentFilter(ShortlistManager.ACTION_SHORTLISTS_CHANGED));
    shortlistsView.requestFocus();
  }

  @Override
  public void onStart() {
    super.onStart();
    Dialog dialog = getDialog();
    if (dialog != null) {
      boolean landscape = getResources().getConfiguration().orientation ==
          Configuration.ORIENTATION_LANDSCAPE;
      int widthId = landscape
          ? android.R.dimen.dialog_min_width_major : android.R.dimen.dialog_min_width_minor;
      TypedValue widthValue = new TypedValue();
      getResources().getValue(widthId, widthValue, true);
      float width = widthValue.getFraction(dialog.getOwnerActivity().getWindow().getDecorView()
          .getWidth(), 1);
      dialog.getWindow().setLayout((int) Math.ceil(width), ViewGroup.LayoutParams.MATCH_PARENT);
    }
  }

  private boolean createShortlist() {
    if (TextUtils.isEmpty(newShortlistText.getText())) {
      return false;
    }
    shortlistManager.createShortlist(newShortlistText.getText().toString());
    newShortlistText.setText("");
    return true;
  }

  @Override
  public void onDestroyView() {
    super.onDestroyView();
    LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(shortlistsChangedReceiver);
  }

  private class MetadataShortlistViewHolder extends ShortlistViewHolder {

    public MetadataShortlistViewHolder(ViewGroup parent) {
      super(parent);
    }

    @Override
    protected Boolean getCheckedState() {
      return isInShortlist() ? Boolean.TRUE : null;
    }

    @Override
    protected void toggleState() {
      if (!isInShortlist()) {
        shortlistManager.add(mediaItem, shortlist);
      } else {
        shortlistManager.remove(mediaItem, shortlist);
      }
    }

    private boolean isInShortlist() {
      return shortlistManager.isInShortlist(mediaItem, shortlist);
    }

  }
}
