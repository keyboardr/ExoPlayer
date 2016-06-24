package com.keyboardr.dancedj;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class MonitorControlsFragment extends Fragment {

    public static MonitorControlsFragment newInstance() {
        return new MonitorControlsFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_monitor_controls, container, false);
    }

}
