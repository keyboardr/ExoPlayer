package com.keyboardr.dancedj.ui.monitor;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;

import com.keyboardr.dancedj.R;
import com.keyboardr.dancedj.model.FilterInfo;
import com.keyboardr.dancedj.util.FragmentUtils;

public class FilterFragment extends Fragment {

    private Spinner sortSpinner;

    public interface Holder {
        void setLibraryFilter(FilterInfo filter);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_filter, container, false);
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
    }

    private void updateFilterInfo() {
        //noinspection WrongConstant,ConstantConditions
        FragmentUtils.getParent(this, Holder.class).setLibraryFilter(new FilterInfo(sortSpinner.getSelectedItemPosition()));
    }
}
