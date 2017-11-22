package com.zhihu.matisse.internal.entity;

import java.io.Serializable;

/**
 * Created by Ca1j on 2017/11/22.
 */

public class Source implements Serializable {

    public boolean isHaveSource;
    public boolean isCheckSource;

    public Source(boolean isHaveSource, boolean isCheckSource) {
        this.isHaveSource = isHaveSource;
        this.isCheckSource = isCheckSource;
    }
}
