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

  public static final FilterInfo EMPTY = new FilterInfo(SortMethod.ID, true,
      Collections.<Shortlist>emptySet(), Collections.<Shortlist>emptySet(), null);

  @Retention(RetentionPolicy.SOURCE)
  @IntDef({SortMethod.ID, SortMethod.TITLE, SortMethod.ARTIST, SortMethod.DURATION,
      SortMethod.SHUFFLE})
  public @interface SortMethod {
    int ID = 0;
    int TITLE = 1;
    int ARTIST = 2;
    int DURATION = 3;
    int SHUFFLE = 4;
  }

  @SortMethod
  public final int sortMethod;
  public final boolean sortAscending;

  @NonNull
  public final Set<Shortlist> requiredShortlists;
  @NonNull
  public final Set<Shortlist> disallowedShortlists;
  @Nullable
  public final String filterString;
  @Nullable
  public final Long setlistId;

  public FilterInfo(@SortMethod int sortMethod, boolean sortAscending,
                    @NonNull Set<Shortlist> requiredShortlists,
                    @NonNull Set<Shortlist> disallowedShortlists, @Nullable String filterString) {
    this.sortMethod = sortMethod;
    this.sortAscending = sortAscending;
    this.requiredShortlists = requiredShortlists;
    this.disallowedShortlists = disallowedShortlists;
    this.filterString = filterString;
    setlistId = null;
  }

  public FilterInfo(long setlistId) {
    this.setlistId = setlistId;
    sortMethod = SortMethod.ID;
    sortAscending = true;
    requiredShortlists = Collections.emptySet();
    disallowedShortlists = Collections.emptySet();
    filterString = null;
  }

  public String getSortColumn() {
    StringBuilder builder;
    switch (sortMethod) {
      case SortMethod.SHUFFLE:
        return "RANDOM()"; // returns instead of breaks since it doesn't need asc or desc
      case SortMethod.ID:
        builder = new StringBuilder(MediaStore.Audio.Media._ID);
        break;
      case SortMethod.TITLE:
        builder = new StringBuilder(MediaStore.Audio.Media.TITLE);
        break;
      case SortMethod.ARTIST:
        builder = new StringBuilder(MediaStore.Audio.Media.ARTIST);
        break;
      case SortMethod.DURATION:
        builder = new StringBuilder(MediaStore.Audio.Media.DURATION);
        break;
      default:
        throw new IllegalStateException("Unknown SortMethod: " + sortMethod);
    }
    if (sortMethod == SortMethod.TITLE || sortMethod == SortMethod.ARTIST) {
      builder.append(" COLLATE NOCASE");
    }
    builder.append(" ");
    builder.append(sortAscending ? "ASC" : "DESC");
    return builder.toString();
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
    dest.writeInt(this.sortAscending ? 1 : 0);
    dest.writeTypedList(new ArrayList<>(requiredShortlists));
    dest.writeTypedList(new ArrayList<>(disallowedShortlists));
    dest.writeString(filterString);
    if (setlistId != null) {
      dest.writeInt(1);
      dest.writeLong(setlistId);
    } else {
      dest.writeInt(0);
    }
  }

  protected FilterInfo(Parcel in) {
    //noinspection WrongConstant
    this.sortMethod = in.readInt();
    sortAscending = in.readInt() == 1;
    ArrayList<Shortlist> shortlists = in.readArrayList(null);
    requiredShortlists = new ArraySet<>();
    requiredShortlists.addAll(shortlists);
    shortlists = in.readArrayList(null);
    disallowedShortlists = new ArraySet<>();
    disallowedShortlists.addAll(shortlists);
    filterString = in.readString();
    if (in.readInt() == 1) {
      setlistId = in.readLong();
    } else {
      setlistId = null;
    }
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
