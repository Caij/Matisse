package com.zhihu.matisse.internal.utils;

import com.zhihu.matisse.MimeType;
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

    public static boolean isImage(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.equals(MimeType.JPEG.toString())
                || mimeType.equals(MimeType.PNG.toString())
                || mimeType.equals(MimeType.GIF.toString())
                || mimeType.equals(MimeType.BMP.toString())
                || mimeType.equals(MimeType.WEBP.toString())
                || mimeType.equals(MimeType.HEIC.toString());
    }

    public static boolean isGif(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.equals(MimeType.GIF.toString());
    }

    public static boolean isVideo(String mimeType) {
        if (mimeType == null) return false;
        return mimeType.equals(MimeType.MPEG.toString())
                || mimeType.equals(MimeType.MP4.toString())
                || mimeType.equals(MimeType.QUICKTIME.toString())
                || mimeType.equals(MimeType.THREEGPP.toString())
                || mimeType.equals(MimeType.THREEGPP2.toString())
                || mimeType.equals(MimeType.MKV.toString())
                || mimeType.equals(MimeType.WEBM.toString())
                || mimeType.equals(MimeType.TS.toString())
                || mimeType.equals(MimeType.AVI.toString());
    }

}
