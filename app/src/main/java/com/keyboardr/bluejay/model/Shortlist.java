package com.keyboardr.bluejay.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;

import java.util.Objects;

public class Shortlist implements Parcelable, Comparable<Shortlist> {
    private final long id;
    private final String name;
    private final int position;

    public Shortlist(long id, String name, int position) {
        this.id = id;
        this.name = name;
        this.position = position;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getPosition() {
        return position;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(this.id);
        dest.writeString(this.name);
        dest.writeInt(position);
    }

    protected Shortlist(Parcel in) {
        this.id = in.readLong();
        this.name = in.readString();
        this.position = in.readInt();
    }

    public static final Parcelable.Creator<Shortlist> CREATOR = new Parcelable.Creator<Shortlist>
            () {
        @Override
        public Shortlist createFromParcel(Parcel source) {
            return new Shortlist(source);
        }

        @Override
        public Shortlist[] newArray(int size) {
            return new Shortlist[size];
        }
    };

    @Override
    public int compareTo(@NonNull Shortlist shortlist) {
        return getId() > shortlist.getId() ? 1 : getId() == shortlist.getId() ? 0 : -1;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Shortlist shortlist = (Shortlist) o;
        return id == shortlist.id &&
            Objects.equals(name, shortlist.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name);
    }
}
