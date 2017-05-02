package com.keyboardr.bluejay.ui.monitor.library;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.keyboardr.bluejay.R;
import com.keyboardr.bluejay.model.FilterInfo;
import com.keyboardr.bluejay.util.FragmentUtils;

public class FilterHolder extends Fragment implements FilterFragment.Holder,
    HistoryFragment.Holder {

  private static final int INDEX_LIBRARY_FILTER = 0;
  private static final int INDEX_HISTORY_FILTER = 1;
  private FilterInfo libraryFilter;
  private FilterInfo historyFilter;
  private TabLayout tabLayout;

  @Nullable
  @Override
  public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                           @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_filter_holder, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    ViewPager pager = (ViewPager) view.findViewById(R.id.filter_pager);
    pager.setAdapter(new FragmentPagerAdapter(getChildFragmentManager()) {
      FilterFragment libraryFilterFragment = new FilterFragment();
      HistoryFragment historyFilterFragment = new HistoryFragment();

      @Override
      public Fragment getItem(int position) {
        return position == INDEX_LIBRARY_FILTER ? libraryFilterFragment : historyFilterFragment;
      }

      @Override
      public int getCount() {
        return 2;
      }
    });
    tabLayout = (TabLayout) view.findViewById(R.id.filter_tabs);
    tabLayout.setupWithViewPager(pager, false);
    tabLayout.getTabAt(INDEX_LIBRARY_FILTER).setIcon(R.drawable.ic_filter_list);
    tabLayout.getTabAt(INDEX_HISTORY_FILTER).setIcon(R.drawable.ic_history);
    tabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
    tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
      @Override
      public void onTabSelected(TabLayout.Tab tab) {
        getParent().setLibraryFilter(tab.getPosition() == INDEX_LIBRARY_FILTER
            ? libraryFilter
            : historyFilter);
      }

      @Override
      public void onTabUnselected(TabLayout.Tab tab) {
      }

      @Override
      public void onTabReselected(TabLayout.Tab tab) {
      }
    });
  }

  @Override
  public void setLibraryFilter(FilterInfo filter) {
    libraryFilter = filter;
    if (tabLayout.getSelectedTabPosition() == INDEX_LIBRARY_FILTER) {
      getParent().setLibraryFilter(libraryFilter);
    }
  }

  public void setHistoryFilter(FilterInfo filter) {
    historyFilter = filter;
    if (tabLayout.getSelectedTabPosition() == INDEX_HISTORY_FILTER) {
      getParent().setLibraryFilter(historyFilter);
    }
  }

  private FilterFragment.Holder getParent() {
    return FragmentUtils.getParentChecked(this, FilterFragment.Holder.class);
  }
}
