package com.keyboardr.bluejay.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
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

  public String getSortColumn() {
    switch (sortMethod) {
      case SortMethod.ID:
        return MediaStore.Audio.Media._ID;
      case SortMethod.TITLE:
        return MediaStore.Audio.Media.TITLE;
      case SortMethod.ARTIST:
        return MediaStore.Audio.Media.ARTIST;
      case SortMethod.DURATION:
        return MediaStore.Audio.Media.DURATION;
      default:
        throw new IllegalStateException("Unknown SortMethod: " + sortMethod);
    }
  }

  public boolean isAllowed(@NonNull MediaItem mediaItem,
                           @NonNull ShortlistManager shortlistManager) {
    List<Shortlist> shortlists = shortlistManager.getShortlists(mediaItem);
    if (shortlists == null) {
      shortlists = Collections.emptyList();
    }
    return containsFilterString(mediaItem) && !containsDisallowed(shortlists)
        && containsAllRequired(shortlists);
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
