package com.zhihu.matisse.internal.utils;

import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.loader.AlbumMediaLoader;

/**
 * Created by caij on 2017/12/10.
 */

public class TypeUtil {

    public static int getShowType(SelectionSpec selectionSpec) {
        int type;
        if (selectionSpec.onlyShowImages()) {
            type = AlbumMediaLoader.TYPE_ONLYSHOWIMAGES;
        } else if (selectionSpec.onlyShowVideos()) {
            type = AlbumMediaLoader.TYPE_ONLYSHOWVIDEOS;
        } else {
            type = AlbumMediaLoader.TYPE_ALL;
        }
        return type;
    }
}
