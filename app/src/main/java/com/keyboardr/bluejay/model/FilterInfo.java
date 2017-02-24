package com.keyboardr.bluejay.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;
import android.util.ArraySet;

import com.keyboardr.bluejay.provider.ShortlistManager;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class FilterInfo implements Parcelable {

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({SortMethod.ID, SortMethod.TITLE, SortMethod.ARTIST, SortMethod.DURATION})
  public @interface SortMethod {
    int ID = 0;
    int TITLE = 1;
    int ARTIST = 2;
    int DURATION = 3;
  }

  @SortMethod
  public final int sortMethod;

  @NonNull
  public final Set<Shortlist> requiredShortlists;
  @NonNull
  public final Set<Shortlist> disallowedShortlists;
  @Nullable
  public final String filterString;

  public FilterInfo(@SortMethod int sortMethod, @NonNull Set<Shortlist> requiredShortlists,
                    @NonNull Set<Shortlist> disallowedShortlists, @Nullable String filterString) {
    this.sortMethod = sortMethod;
    this.requiredShortlists = requiredShortlists;
    this.disallowedShortlists = disallowedShortlists;
    this.filterString = filterString;
  }

  @NonNull
  private Comparator<MediaItem> getSorting() {
    return new MediaItemComparator(sortMethod);
  }

  public void apply(@NonNull List<MediaItem> source, @NonNull ShortlistManager shortlistManager) {
    for (int i = source.size() - 1; i >= 0; i--) {
      MediaItem item = source.get(i);
      List<Shortlist> shortlists = shortlistManager.getShortlists(item);
      if (shortlists == null) {
        shortlists = new ArrayList<>();
      }

      if (!containsFilterString(item) || containsDisallowed(shortlists)
          || !containsAllRequired(shortlists)) {
        source.remove(i);
      }
    }
    Collections.sort(source, getSorting());
  }

  private boolean containsFilterString(@NonNull MediaItem mediaItem) {
    return TextUtils.isEmpty(filterString)
        || mediaItem.title.toString().toLowerCase().contains(filterString.toLowerCase())
        || mediaItem.artist.toString().toLowerCase().contains(filterString.toLowerCase());

  }

  private boolean containsDisallowed(List<Shortlist> shortlists) {
    for (Shortlist shortlist : shortlists) {
      if (disallowedShortlists.contains(shortlist)) {
        return true;
      }
    }
    return false;
  }

  private boolean containsAllRequired(List<Shortlist> shortlists) {
    for (Shortlist required : requiredShortlists) {
      if (!shortlists.contains(required)) {
        return false;
      }
    }
    return true;
  }

  private static class MediaItemComparator implements Comparator<MediaItem> {
    @SortMethod
    private final int sortMethod;

    public MediaItemComparator(@SortMethod int sortMethod) {
      this.sortMethod = sortMethod;
    }

    @Override
    public int compare(MediaItem left, MediaItem right) {
      switch (sortMethod) {
        case SortMethod.ID:
          return Long.compare(left.getTransientId(), right.getTransientId());
        case SortMethod.TITLE:
          return left.title.toString().compareTo(right.title.toString());
        case SortMethod.ARTIST:
          return left.artist.toString().compareTo(right.artist.toString());
        case SortMethod.DURATION:
          return Long.compare(left.getDuration(), right.getDuration());
      }
      return 0;
    }
  }

  @Override
  public int describeContents() {
    return 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeInt(this.sortMethod);
    dest.writeTypedList(new ArrayList<>(requiredShortlists));
    dest.writeTypedList(new ArrayList<>(disallowedShortlists));
    dest.writeString(filterString);
  }

  protected FilterInfo(Parcel in) {
    //noinspection WrongConstant
    this.sortMethod = in.readInt();
    ArrayList<Shortlist> shortlists = in.readArrayList(null);
    requiredShortlists = new ArraySet<>();
    requiredShortlists.addAll(shortlists);
    shortlists = in.readArrayList(null);
    disallowedShortlists = new ArraySet<>();
    disallowedShortlists.addAll(shortlists);
    filterString = in.readString();
  }

  public static final Parcelable.Creator<FilterInfo> CREATOR = new Parcelable.Creator<FilterInfo>
      () {
    @Override
    public FilterInfo createFromParcel(Parcel source) {
      return new FilterInfo(source);
    }

    @Override
    public FilterInfo[] newArray(int size) {
      return new FilterInfo[size];
    }
  };
}
