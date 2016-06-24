package com.keyboardr.dancedj.ui;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ViewAnimator;

import com.keyboardr.dancedj.R;

/**
 * Standard fragment for displaying a RecyclerView
 */

public abstract class RecyclerFragment<VH extends RecyclerView.ViewHolder, L> extends Fragment {

    private static final int SWITCHER_LOADING = 0;
    private static final int SWITCHER_EMPTY = 1;
    private static final int SWITCHER_LOADED = 2;

    private ViewAnimator switcher;

    private LoaderManager.LoaderCallbacks<L> loaderCallbacks = new LoaderManager.LoaderCallbacks<L>() {
        @Override
        public Loader<L> onCreateLoader(int id, Bundle args) {
            return getLoaderCallbacks().onCreateLoader(id, args);
        }

        @Override
        public void onLoadFinished(Loader<L> loader, L data) {
            getLoaderCallbacks().onLoadFinished(loader, data);
            switcher.setDisplayedChild(getAdapter().getItemCount() != 0
                    ? SWITCHER_LOADED : SWITCHER_EMPTY);
        }

        @Override
        public void onLoaderReset(Loader<L> loader) {
            switcher.setDisplayedChild(SWITCHER_LOADING);
            getLoaderCallbacks().onLoaderReset(loader);
        }
    };

    @NonNull
    protected abstract LoaderManager.LoaderCallbacks<L> getLoaderCallbacks();

    @NonNull
    protected abstract RecyclerView.Adapter<VH> getAdapter();

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_recycler, container, false);
        switcher = (ViewAnimator) view.findViewById(R.id.library_switcher);
        RecyclerView recyclerView = (RecyclerView) view.findViewById(R.id.library_recycler);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext(),
                LinearLayoutManager.VERTICAL, false);
        recyclerView.setLayoutManager(layoutManager);
        new ItemTouchHelper(getItemTouchHelperCallback()).attachToRecyclerView(recyclerView);
        recyclerView.setAdapter(getAdapter());
        return view;
    }

    @SuppressWarnings("WeakerAccess")
    @NonNull
    protected ItemTouchHelper.Callback getItemTouchHelperCallback() {
        return new ItemTouchHelper.SimpleCallback(0,0) {
            @Override
            public boolean onMove(RecyclerView recyclerView, RecyclerView.ViewHolder viewHolder, RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {

            }
        };
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        getLoaderManager().initLoader(0, null, loaderCallbacks);
    }
}
