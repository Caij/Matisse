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
    public int toolbarColor;
    @ColorInt
    public int toolbarActionColor;
    @ColorInt
    public int bottombarBackground;

    public Theme(int themeId, int toolbarColor, int toolbarActionColor, int bottombarBackground) {
        this.themeId = themeId;
        this.toolbarColor = toolbarColor;
        this.toolbarActionColor = toolbarActionColor;
        this.bottombarBackground = bottombarBackground;
    }
}
