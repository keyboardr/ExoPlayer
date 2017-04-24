package com.keyboardr.bluejay.model;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Info about a set that does not change from the time it is started until it ends
 */

public class SetMetadata implements Parcelable {
  public final String name;
  public final boolean isSoundCheck;

  public SetMetadata(String name, boolean isSoundCheck) {
    this.name = name;
    this.isSoundCheck = isSoundCheck;
  }

  protected SetMetadata(Parcel in) {
    this.name = in.readString();
    this.isSoundCheck = in.readByte() != 0;
  }

  @Override
  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.name);
    dest.writeByte(this.isSoundCheck ? (byte) 1 : (byte) 0);
  }

  @Override
  public int describeContents() {
    return 0;
  }

  public static final Parcelable.Creator<SetMetadata> CREATOR = new Parcelable
      .Creator<SetMetadata>() {
    @Override
    public SetMetadata createFromParcel(Parcel source) {
      return new SetMetadata(source);
    }

    @Override
    public SetMetadata[] newArray(int size) {
      return new SetMetadata[size];
    }
  };
}
