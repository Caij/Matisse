package com.zhihu.matisse.internal.entity;

import android.support.annotation.ColorInt;
import android.support.annotation.StyleRes;

import com.zhihu.matisse.R;

/**
 * Created by Ca1j on 2017/11/24.
 */

public class Theme {

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
}
