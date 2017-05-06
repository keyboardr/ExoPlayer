package com.keyboardr.bluejay.ui.monitor.library;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.os.Bundle;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.StringRes;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.content.PermissionChecker;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.transition.ArcMotion;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroupOverlay;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;
import android.widget.ViewAnimator;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.bus.Buses;
import com.keyboardr.bluejay.bus.event.MediaConnectedEvent;
import com.keyboardr.bluejay.model.FilterInfo;
import com.keyboardr.bluejay.model.MediaItem;
import com.keyboardr.bluejay.provider.ShortlistManager;
import com.keyboardr.bluejay.ui.BottomNavHolder;
import com.keyboardr.bluejay.ui.MonitorContainer;
import com.keyboardr.bluejay.ui.recycler.MediaViewHolder;
import com.keyboardr.bluejay.util.FragmentUtils;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.List;

public class LibraryFragment extends android.support.v4.app.Fragment
    implements FilterFragment.Holder, MetadataFragment.Holder {

  public interface Holder extends MonitorContainer {

    void addToQueue(@NonNull MediaItem mediaItem);

    boolean queueContains(@NonNull MediaItem mediaItem);
  }

  private static final int SWITCHER_LOADING = 0;
  private static final int SWITCHER_EMPTY = 1;
  private static final int SWITCHER_LOADED = 2;
  private static final int SWITCHER_NO_PERMISSION = 3;
  private static final String ARG_FILTER = "filter";

  private ViewAnimator switcher;

  private final LoaderManager.LoaderCallbacks<List<MediaItem>> mediaLoaderCallbacks
      = new LoaderManager.LoaderCallbacks<List<MediaItem>>() {
    @Override
    public Loader<List<MediaItem>> onCreateLoader(int i, Bundle bundle) {
      FilterInfo filterInfo = bundle == null ? null : ((FilterInfo) bundle
          .getParcelable(ARG_FILTER));
      if (filterInfo == null) {
        filterInfo = FilterInfo.EMPTY;
      }
      return new LibraryLoader(getContext(), filterInfo, shortlistManager);
    }

    @Override
    public void onLoadFinished(Loader<List<MediaItem>> loader, List<MediaItem> items) {
      if (items != null) {
        adapter.setMediaItems(items);
        switcher.setDisplayedChild(items.size() != 0
            ? LibraryFragment.SWITCHER_LOADED : LibraryFragment.SWITCHER_EMPTY);
      } else if (PermissionChecker.checkSelfPermission(getContext(), Manifest.permission
          .READ_EXTERNAL_STORAGE) != PermissionChecker.PERMISSION_GRANTED) {
        switcher.setDisplayedChild(SWITCHER_NO_PERMISSION);
        if (!shouldShowRequestPermissionRationale(Manifest.permission.READ_EXTERNAL_STORAGE)) {
          requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
        }
      }
    }

    @Override
    public void onLoaderReset(Loader<List<MediaItem>> loader) {
      switcher.setDisplayedChild(LibraryFragment.SWITCHER_LOADING);
    }
  };

  private final MediaViewHolder.MediaViewDecorator decorator
      = new MediaViewHolder.MediaViewDecorator() {

    @Override
    public void onMediaItemSelected(@NonNull MediaItem mediaItem) {
      getParent().playMediaItemOnMonitor(mediaItem);
    }

    @Override
    @DrawableRes
    public int getIconForItem(@NonNull MediaItem mediaItem) {
      return MediaConnectedEvent.isConnected() ? R.drawable.ic_playlist_add : 0;
    }

    @Override
    @StringRes
    public int getDescriptionForIcon(@DrawableRes int icon) {
      if (icon == R.drawable.ic_playlist_add) {
        return R.string.description_add_track;
      }
      return 0;
    }

    @Override
    public void onDecoratorSelected(@NonNull MediaItem mediaItem, @NonNull View view) {
      if (getParent().queueContains(mediaItem)) {
        ConfirmAddDialogFragment.show(LibraryFragment.this, mediaItem);
      } else {
        doAddToQueue(mediaItem, view);
      }
    }

    @Override
    public boolean showMoreOption() {
      return shortlistManager.isReady();
    }

    @Override
    public void onMoreSelected(@NonNull MediaItem mediaItem) {
      MetadataFragment.show(LibraryFragment.this, mediaItem);
    }

  };

  private final MediaAdapter adapter = new MediaAdapter(decorator);

  private ShortlistManager shortlistManager;

  private BroadcastReceiver shortlistsReadyReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      if (shortlistManager.isReady()) {
        adapter.notifyDataSetChanged();
        LocalBroadcastManager.getInstance(context.getApplicationContext()).unregisterReceiver(this);
      }
    }
  };

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    shortlistManager = ShortlistManager.getInstance(getContext());
    LocalBroadcastManager.getInstance(getContext().getApplicationContext()).registerReceiver
        (shortlistsReadyReceiver, new IntentFilter(ShortlistManager.ACTION_SHORTLISTS_READY));
  }

  @Override
  public void onDestroy() {
    super.onDestroy();
    LocalBroadcastManager.getInstance(getContext().getApplicationContext()).unregisterReceiver
        (shortlistsReadyReceiver);
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

  @Override
  public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                         @NonNull int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    getLoaderManager().restartLoader(0, null, mediaLoaderCallbacks);
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    FragmentUtils.checkParent(this, Holder.class);
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
                           Bundle savedInstanceState) {
    View view = inflater.inflate(R.layout.fragment_library, container, false);
    switcher = (ViewAnimator) view.findViewById(R.id.library_switcher);
    RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.library_recycler);
    LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
        LinearLayoutManager.VERTICAL, false);
    recyclerView.setLayoutManager(layoutManager);
    recyclerView.addItemDecoration(new DividerItemDecoration(getContext(),
        layoutManager.getOrientation()));
    recyclerView.setAdapter(adapter);
    view.findViewById(R.id.grant_permission).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
      }
    });
    return view;
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    getActivity().invalidateOptionsMenu();
    getLoaderManager().initLoader(0, null, mediaLoaderCallbacks);
  }

  @Override
  public void setLibraryFilter(@NonNull FilterInfo filter) {
    Bundle args = new Bundle();
    args.putParcelable(ARG_FILTER, filter);
    getLoaderManager().restartLoader(0, args, mediaLoaderCallbacks);
  }

  @NonNull
  protected Holder getParent() {
    // Checked in #onAttach(Context)
    //noinspection ConstantConditions
    return FragmentUtils.getParent(this, Holder.class);
  }

  @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
  public void onMediaConnectedEvent(MediaConnectedEvent event) {
    adapter.notifyDataSetChanged();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    inflater.inflate(R.menu.frag_library, menu);
  }

  @Override
  public void onPrepareOptionsMenu(Menu menu) {
    View view = getView();
    menu.findItem(R.id.filter_library).setVisible(view != null
        && view.findViewById(R.id.filter_fragment) == null);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.filter_library) {
      new FilterFragment().show(getChildFragmentManager(), null);
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void doAddToQueue(@NonNull MediaItem mediaItem, @Nullable View view) {
    getParent().addToQueue(mediaItem);
    if (view == null) {
      return;
    }
    BottomNavHolder bottomNavHolder = FragmentUtils.getParent(LibraryFragment.this,
        BottomNavHolder.class);
    if (bottomNavHolder == null) {
      return;
    }
    View playlistTabView = bottomNavHolder.getPlaylistTabView();
    if (playlistTabView == null) {
      return;
    }
    final ViewGroupOverlay overlay = ((ViewGroup) playlistTabView.getParent().getParent())
        .getOverlay();
    final ImageView albumArt = (ImageView) view.findViewById(R.id.media_item_album_art);
    if (albumArt == null) {
      return;
    }
    final ViewGroup originalParent = (ViewGroup) albumArt.getParent();
    final ViewGroup.LayoutParams originalLayoutParams = albumArt.getLayoutParams();
    overlay.add(albumArt);

    int[] albumArtLocation = new int[2];
    int[] tabLocation = new int[2];
    albumArt.getLocationOnScreen(albumArtLocation);
    playlistTabView.getLocationOnScreen(tabLocation);

    int duration = getResources().getInteger(android.R.integer.config_mediumAnimTime);

    Path translationPath = new ArcMotion().getPath(albumArtLocation[0], albumArtLocation[1],
        tabLocation[0] + playlistTabView.getWidth() / 2 - albumArt.getWidth(),
        tabLocation[1] - playlistTabView.getHeight() / 2 - albumArt.getHeight());
    ObjectAnimator pathAnimation = ObjectAnimator.ofFloat(albumArt, View.X, View.Y,
        translationPath);
    pathAnimation.setInterpolator(new AccelerateDecelerateInterpolator());
    ObjectAnimator scaleAnimation = ObjectAnimator.ofPropertyValuesHolder(albumArt,
        PropertyValuesHolder.ofFloat(View.SCALE_X, 0),
        PropertyValuesHolder.ofFloat(View.SCALE_Y, 0));

    AnimatorSet animatorSet = new AnimatorSet();
    animatorSet.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        overlay.remove(albumArt);
        albumArt.setTranslationX(0);
        albumArt.setTranslationY(0);
        albumArt.setScaleX(1);
        albumArt.setScaleY(1);
        originalParent.addView(albumArt, originalLayoutParams);
      }
    });
    animatorSet.play(pathAnimation).with(scaleAnimation);
    animatorSet.setDuration(duration);
    animatorSet.start();
  }

  public static class ConfirmAddDialogFragment extends DialogFragment {
    private static final String ARG_MEDIA_ITEM = "mediaItem";

    public static ConfirmAddDialogFragment show(@NonNull LibraryFragment parent,
                                                @NonNull MediaItem mediaItem) {
      ConfirmAddDialogFragment fragment = new ConfirmAddDialogFragment();
      Bundle args = new Bundle();
      args.putParcelable(ARG_MEDIA_ITEM, mediaItem);
      fragment.setArguments(args);
      fragment.show(parent.getChildFragmentManager(), null);
      return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
      AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
      final MediaItem mediaItem = getArguments().getParcelable(ARG_MEDIA_ITEM);
      builder.setTitle(R.string.confirm_add_title);
      //noinspection ConstantConditions
      builder.setMessage(builder.getContext().getString(R.string.confirm_add_message,
          mediaItem.title));
      builder.setPositiveButton(R.string.add_track, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          FragmentUtils.getParentChecked(ConfirmAddDialogFragment.this, LibraryFragment.class)
              .doAddToQueue(mediaItem, null);
          dialogInterface.dismiss();
        }
      });
      builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
          dialogInterface.cancel();
        }
      });
      return builder.create();
    }
  }
}
