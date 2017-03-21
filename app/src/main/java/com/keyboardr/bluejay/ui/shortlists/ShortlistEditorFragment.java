package com.keyboardr.bluejay.ui.shortlists;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.provider.ShortlistManager;
import com.keyboardr.bluejay.util.FragmentUtils;

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

  private ShortlistManager shortlistManager;

  private ItemTouchHelper itemTouchHelper;
  private RecyclerView recyclerView;

  private BroadcastReceiver shortlistsChangedReceiver = new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      recyclerView.getAdapter().notifyDataSetChanged();
    }
  };

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    shortlistManager = ShortlistManager.getInstance(getContext());
  }

  @Override
  public void onAttach(Context context) {
    super.onAttach(context);
    FragmentUtils.checkParent(this, Holder.class);
  }

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
    recyclerView.setLayoutManager(new LinearLayoutManager(getContext(),
        LinearLayoutManager.VERTICAL, false));
    final ShortlistAdapter<EditorShortlistViewHolder> adapter = new
        ShortlistAdapter<EditorShortlistViewHolder>(
            shortlistManager) {
          @Override
          public EditorShortlistViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
            return new EditorShortlistViewHolder(parent);
          }
        };
    recyclerView.setAdapter(adapter);

    ItemTouchHelper.SimpleCallback touchCallback = new ItemTouchHelper.SimpleCallback
        (ItemTouchHelper.UP | ItemTouchHelper.DOWN, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

      @Override
      public boolean isLongPressDragEnabled() {
        return true;
      }

      @Override
      public boolean canDropOver(RecyclerView recyclerView, RecyclerView.ViewHolder current,
                                 RecyclerView.ViewHolder target) {
        return true;
      }

      @Override
      public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder,
                            RecyclerView.ViewHolder target) {
        int oldIndex = viewHolder.getAdapterPosition();
        int newIndex = target.getAdapterPosition();

        shortlistManager.moveShortlist(oldIndex, newIndex);
        adapter.notifyItemMoved(oldIndex, newIndex);
        return false;
      }

      @Override
      public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
        int removeIndex = viewHolder.getAdapterPosition();
        shortlistManager.deleteShortlist(((ShortlistViewHolder) viewHolder).shortlist);
        adapter.notifyItemRemoved(removeIndex);
      }
    };
    itemTouchHelper = new ItemTouchHelper(touchCallback);
    itemTouchHelper.attachToRecyclerView(recyclerView);

    view.findViewById(R.id.close).setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        FragmentUtils.getParentChecked(ShortlistEditorFragment.this, Holder.class)
            .closeShortlistEditor();
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

  private class EditorShortlistViewHolder extends ShortlistViewHolder {
    public EditorShortlistViewHolder(ViewGroup parent) {
      super(parent, R.layout.item_shortlist_editor);
      itemView.findViewById(R.id.drag_handle).setOnTouchListener(new View.OnTouchListener() {
        @Override
        public boolean onTouch(View view, MotionEvent motionEvent) {
          if (motionEvent.getActionMasked() == MotionEvent.ACTION_DOWN) {
            itemTouchHelper.startDrag(EditorShortlistViewHolder.this);
          }
          return false;
        }
      });
    }

    @Override
    public void onClick(View view) {
      ShortlistRenameDialogFragment fragment = ShortlistRenameDialogFragment.newInstance(shortlist);
      fragment.show(getChildFragmentManager(), null);
    }
  }

}
