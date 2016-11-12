package com.keyboardr.dancedj.ui;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.util.FragmentUtils;

public class NoSetFragment extends Fragment {

    public interface Holder {
        void startNewSetlist();
    }

    public static NoSetFragment newInstance() {
        return new NoSetFragment();
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_no_setlist, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        view.findViewById(R.id.new_setlist).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentUtils.getParent(NoSetFragment.this, Holder.class).startNewSetlist();
            }
        });
    }
}
