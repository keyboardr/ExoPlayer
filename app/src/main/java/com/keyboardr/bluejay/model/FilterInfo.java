package com.keyboardr.bluejay.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;
import android.support.annotation.NonNull;
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

  public FilterInfo(@SortMethod int sortMethod, @NonNull Set<Shortlist> requiredShortlists) {
    this.sortMethod = sortMethod;
    this.requiredShortlists = requiredShortlists;
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
      for (Shortlist required : requiredShortlists) {
        if (!shortlists.contains(required)) {
          source.remove(i);
          break;
        }
      }
    }
    Collections.sort(source, getSorting());
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
  }

  protected FilterInfo(Parcel in) {
    //noinspection WrongConstant
    this.sortMethod = in.readInt();
    ArrayList<Shortlist> shortlists = in.readArrayList(null);
    requiredShortlists = new ArraySet<>();
    requiredShortlists.addAll(shortlists);
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
