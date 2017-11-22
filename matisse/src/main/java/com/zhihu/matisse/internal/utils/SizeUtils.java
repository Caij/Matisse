package com.zhihu.matisse.internal.utils;

import com.zhihu.matisse.internal.entity.Item;

import java.io.File;
import java.util.List;

/**
 * Created by Ca1j on 2017/11/22.
 */

public class SizeUtils {

    public static String getSize(List<Item> selectImages) {
        long size = 0;
        for (Item item : selectImages) {
            size += item.size;
        }
        return formatFileSize(size);
    }

    public static String formatFileSize(long size) {
        long kb = (long) (size / 1024f);
        if (kb >= 1000) {
            long mb = (long) (kb / 1000f);
            if (mb >= 1000) {
                long gb = (long) (mb / 1024f);
                return gb + "G";
            }else {
                return mb + "M";
            }
        }else {
            return kb + "KB";
        }
    }
}
