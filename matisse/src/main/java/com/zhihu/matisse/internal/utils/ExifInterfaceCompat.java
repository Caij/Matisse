/*
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.utils;

import android.content.Context;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.IOException;

/**
 * Bug fixture for ExifInterface constructor.
 */
final class ExifInterfaceCompat {
    private static final String TAG = ExifInterfaceCompat.class.getSimpleName();
    private static final int EXIF_DEGREE_FALLBACK_VALUE = -1;

    /**
     * Do not instantiate this class.
     */
    private ExifInterfaceCompat() {
    }

    /**
     * Creates new instance of {@link ExifInterface}.
     * Original constructor won't check filename value, so if null value has been passed,
     * the process will be killed because of SIGSEGV.
     * Google Play crash report system cannot perceive this crash, so this method will throw
     * {@link NullPointerException} when the filename is null.
     *
     * @param uri a JPEG filename.
     * @param context
     * @return {@link ExifInterface} instance.
     * @throws IOException something wrong with I/O.
     */
    public static ExifInterface newInstance(Context context, Uri uri) throws IOException {
        if (uri == null) throw new NullPointerException("filename should not be null");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            ParcelFileDescriptor parcelFileDescriptor =
                    context.getContentResolver().openFileDescriptor(uri, "r");
            FileDescriptor fileDescriptor = parcelFileDescriptor.getFileDescriptor();
            return new ExifInterface(fileDescriptor);
        } else {
            return new ExifInterface(PathUtils.getPath(context, uri));
        }
    }


    /**
     * Read exif info and get orientation value of the photo.
     *
     * @param filepath to get exif.
     * @return exif orientation value
     */
    public static int getExifOrientation(Context context, Uri filepath) {
        ExifInterface exif;
        try {
            // ExifInterface does not check whether file path is null or not,
            // so passing null file path argument to its constructor causing SIGSEGV.
            // We should avoid such a situation by checking file path string.
            exif = newInstance(context, filepath);
        } catch (IOException ex) {
            Log.e(TAG, "cannot read exif", ex);
            return EXIF_DEGREE_FALLBACK_VALUE;
        }

        int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, EXIF_DEGREE_FALLBACK_VALUE);
        if (orientation == EXIF_DEGREE_FALLBACK_VALUE) {
            return 0;
        }
        // We only recognize a subset of orientation tag values.
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return 90;
            case ExifInterface.ORIENTATION_ROTATE_180:
                return 180;
            case ExifInterface.ORIENTATION_ROTATE_270:
                return 270;
            default:
                return 0;
        }
    }
}