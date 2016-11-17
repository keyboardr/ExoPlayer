package com.keyboardr.bluejay.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Comparator;

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

    public FilterInfo(@SortMethod int sortMethod) {
        this.sortMethod = sortMethod;
    }

    public Comparator<MediaItem> getSorting() {
        return new MediaItemComparator(sortMethod);
    }

    private static class MediaItemComparator implements Comparator<MediaItem>{
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
    }

    protected FilterInfo(Parcel in) {
        //noinspection WrongConstant
        this.sortMethod = in.readInt();
    }

    public static final Parcelable.Creator<FilterInfo> CREATOR = new Parcelable.Creator<FilterInfo>() {
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
