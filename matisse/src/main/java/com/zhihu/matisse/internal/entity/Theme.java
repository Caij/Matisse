package com.zhihu.matisse.internal.entity;

import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.ColorInt;
import androidx.annotation.StyleRes;

import com.zhihu.matisse.R;

import java.io.Serializable;

/**
 * Created by Ca1j on 2017/11/24.
 */

public class Theme implements Serializable, Parcelable {

    @StyleRes
    public int themeId = R.style.Matisse_Zhihu;
    @ColorInt
    public int statusColor;
    @ColorInt
    public int toolbarColor;
    @ColorInt
    public int toolbarActionColor;
    @ColorInt
    public int bottombarBackground;

    public boolean isWindowLightStatusBar;

    public Theme(int themeId, int statusColor, int toolbarColor, int toolbarActionColor, int bottombarBackground, boolean isWindowLightStatusBar) {
        this.themeId = themeId;
        this.statusColor = statusColor;
        this.toolbarColor = toolbarColor;
        this.toolbarActionColor = toolbarActionColor;
        this.bottombarBackground = bottombarBackground;
        this.isWindowLightStatusBar = isWindowLightStatusBar;
    }

    protected Theme(Parcel in) {
        themeId = in.readInt();
        statusColor = in.readInt();
        toolbarColor = in.readInt();
        toolbarActionColor = in.readInt();
        bottombarBackground = in.readInt();
        isWindowLightStatusBar = in.readByte() != 0;
    }

    public static final Creator<Theme> CREATOR = new Creator<Theme>() {
        @Override
        public Theme createFromParcel(Parcel in) {
            return new Theme(in);
        }

        @Override
        public Theme[] newArray(int size) {
            return new Theme[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(themeId);
        dest.writeInt(statusColor);
        dest.writeInt(toolbarColor);
        dest.writeInt(toolbarActionColor);
        dest.writeInt(bottombarBackground);
        dest.writeByte((byte) (isWindowLightStatusBar ? 1 : 0));
    }
}
