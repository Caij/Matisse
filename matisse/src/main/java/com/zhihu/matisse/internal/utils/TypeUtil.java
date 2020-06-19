package com.zhihu.matisse.internal.utils;

import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.loader.AlbumLoaderV2;

/**
 * Created by caij on 2017/12/10.
 */

public class TypeUtil {

    public static int getShowType(SelectionSpec selectionSpec) {
        int type;
        if (selectionSpec.onlyShowImages()) {
            type = AlbumLoaderV2.TYPE_ONLYSHOWIMAGES;
        } else if (selectionSpec.onlyShowVideos()) {
            type = AlbumLoaderV2.TYPE_ONLYSHOWVIDEOS;
        } else {
            type = AlbumLoaderV2.TYPE_ALL;
        }
        return type;
    }
}
