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

import android.app.Activity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.fragment.app.Fragment;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;

import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.CaptureStrategy;

import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MediaStoreCompat {

    private static final String TAG = "MediaStoreCompat";

    private final WeakReference<Activity> mContext;
    private final WeakReference<Fragment> mFragment;
    private       CaptureStrategy         mCaptureStrategy;
    private       Uri                     mCurrentPhotoUri;

    public MediaStoreCompat(Activity activity) {
        mContext = new WeakReference<>(activity);
        mFragment = null;
    }

    public MediaStoreCompat(Activity activity, Fragment fragment) {
        mContext = new WeakReference<>(activity);
        mFragment = new WeakReference<>(fragment);
    }

    /**
     * Checks whether the device has a camera feature or not.
     *
     * @param context a context to check for camera feature.
     * @return true if the device has a camera feature. false otherwise.
     */
    public static boolean hasCameraFeature(Context context) {
        try {
            PackageManager pm = context.getApplicationContext().getPackageManager();
            return pm.hasSystemFeature(PackageManager.FEATURE_CAMERA);
        }catch (Exception e) {
            return false;
        }
    }

    public void setCaptureStrategy(CaptureStrategy strategy) {
        mCaptureStrategy = strategy;
    }

    public void dispatchCaptureIntent(Context context, int requestCode) {
        Intent captureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (captureIntent.resolveActivity(context.getPackageManager()) != null) {

            if (Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                mCurrentPhotoUri = insertMedia(context.getContentResolver(), createImageFileName());
            } else {
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (photoFile != null) {
                    mCurrentPhotoUri = FileProvider.getUriForFile(mContext.get(),
                            mCaptureStrategy.authority, photoFile);
                }
            }

            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, mCurrentPhotoUri);
            captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                List<ResolveInfo> resInfoList = context.getPackageManager()
                        .queryIntentActivities(captureIntent, PackageManager.MATCH_DEFAULT_ONLY);
                for (ResolveInfo resolveInfo : resInfoList) {
                    String packageName = resolveInfo.activityInfo.packageName;
                    context.grantUriPermission(packageName, mCurrentPhotoUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                }
            }

            try {
                if (mFragment != null) {
                    mFragment.get().startActivityForResult(captureIntent, requestCode);
                } else {
                    mContext.get().startActivityForResult(captureIntent, requestCode);
                }
            } catch (Exception e) {
                Toast.makeText(context.getApplicationContext(), R.string.error_no_permission, Toast.LENGTH_SHORT).show();
                Log.e(TAG, e.getMessage());
            }

        }
    }

    private static Uri insertMedia(ContentResolver resolver, String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);

        values.put(MediaStore.MediaColumns.IS_PENDING, 0);


        Uri collection = null;
        String mimeType;
        String suffix = null;
        try {
            int index = fileName.lastIndexOf(".");
            if (index >=0) {
                suffix = fileName.substring(index+1);
            }
        } catch (Exception e) {

        }

        collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        mimeType = "image/" + (TextUtils.isEmpty(suffix) ? "*" : suffix);
        String relativePath = Environment.DIRECTORY_DCIM;

        values.put(MediaStore.MediaColumns.MIME_TYPE, mimeType);

        String time = String.valueOf(System.currentTimeMillis());
        values.put(MediaStore.Images.Media.DATE_TAKEN, time);

        values.put(MediaStore.MediaColumns.RELATIVE_PATH, relativePath);

        return resolver.insert(collection, values);
    }

    private File createImageFile() throws IOException {
        // Create an image file name

        File storageDir;
        if (mCaptureStrategy.isPublic) {
            storageDir = Environment.getExternalStoragePublicDirectory(
                    Environment.DIRECTORY_DCIM);
        } else {
            storageDir = mContext.get().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        }

        // Avoid joining path components manually
        File tempFile = new File(storageDir, createImageFileName());

        // Handle the situation that user's external storage is not ready
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }

        if (!tempFile.getParentFile().exists()) {
            boolean isSuccess = tempFile.getParentFile().mkdirs();
            if (!isSuccess) {
                throw new IOException("create dir error");
            }
        }

        return tempFile;
    }

    private String createImageFileName() {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = String.format("JPEG_%s.jpg", timeStamp);
        return imageFileName;
    }

    public Uri getCurrentPhotoUri() {
        return mCurrentPhotoUri;
    }

}
