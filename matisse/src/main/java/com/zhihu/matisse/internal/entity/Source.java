package com.zhihu.matisse.internal.entity;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by Ca1j on 2017/11/22.
 */

public class Source implements Serializable, Parcelable {

    public boolean isHaveSource;
    public boolean isCheckSource;

    public Source(boolean isHaveSource, boolean isCheckSource) {
        this.isHaveSource = isHaveSource;
        this.isCheckSource = isCheckSource;
    }

    protected Source(Parcel in) {
        isHaveSource = in.readByte() != 0;
        isCheckSource = in.readByte() != 0;
    }

    public static final Creator<Source> CREATOR = new Creator<Source>() {
        @Override
        public Source createFromParcel(Parcel in) {
            return new Source(in);
        }

        @Override
        public Source[] newArray(int size) {
            return new Source[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeByte((byte) (isHaveSource ? 1 : 0));
        dest.writeByte((byte) (isCheckSource ? 1 : 0));
    }
}
