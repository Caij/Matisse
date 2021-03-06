package com.zhihu.matisse.internal.loader;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;

import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static com.zhihu.matisse.internal.loader.AlbumLoaderV2.TYPE_ONLYSHOWIMAGES;
import static com.zhihu.matisse.internal.loader.AlbumLoaderV2.TYPE_ONLYSHOWVIDEOS;

public class MediaLoaderV2 {

    private static final int KEEP_ALIVE = 1;
    private static final ThreadFactory sThreadFactory = new ThreadFactory() {
        private final AtomicInteger mCount = new AtomicInteger(1);

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(r, "ModernAsyncTask #" + mCount.getAndIncrement());
        }
    };
    public static final Executor THREAD_POOL_EXECUTOR =
            new ThreadPoolExecutor(2, 4, KEEP_ALIVE,
                    TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(10), sThreadFactory);

    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");


    public static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.DATA,
            MediaStore.MediaColumns.DISPLAY_NAME,
            MediaStore.MediaColumns.MIME_TYPE,
            MediaStore.MediaColumns.SIZE,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME,
            "duration"};

    private static final String SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    private static final String SELECTION_ALL =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    private static final String[] SELECTION_ALL_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };

    private static final String SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND "
                    + " bucket_id=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    private static final String SELECTION_ALBUM =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND "
                    + " bucket_id=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    // =============================================

    // === params for showSingleMediaType: true ===
    private static final String SELECTION_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    private static final String TAG = "MediaLoaderV2";

    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }
    // =============================================

    private static final int PAGE_SIZE = 100;

    public static MediaLoaderV2 newInstance(Context context, int type, String bucketId, int position, Callback<List<Item>> albumCallback) {
        String selection;
        String[] selectionArgs;

        if (Album.ALBUM_ID_ALL.equals(bucketId)) {
            if (type == TYPE_ONLYSHOWIMAGES) {
                selection = SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE;
                selectionArgs =
                        getSelectionArgsForSingleMediaType(
                                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
            } else if (type == TYPE_ONLYSHOWVIDEOS) {
                selection = SELECTION_ALL_FOR_SINGLE_MEDIA_TYPE;
                selectionArgs =
                        getSelectionArgsForSingleMediaType(
                                MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
            } else {
                selection = SELECTION_ALL;
                selectionArgs = SELECTION_ALL_ARGS;
            }
        } else {
            if (type == TYPE_ONLYSHOWIMAGES) {
                selection = SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE;
                selectionArgs =
                        getSelectionAlbumArgsForSingleMediaType(
                                MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE,
                                bucketId);
            } else if (type == TYPE_ONLYSHOWVIDEOS) {
                selection = SELECTION_ALBUM_FOR_SINGLE_MEDIA_TYPE;
                selectionArgs = getSelectionAlbumArgsForSingleMediaType(
                        MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO,
                        bucketId);
            } else {
                selection = SELECTION_ALBUM;
                selectionArgs = getSelectionAlbumArgs(bucketId);
            }
        }

        return new MediaLoaderV2(context, selection, selectionArgs, albumCallback, position);
    }

    private static String[] getSelectionAlbumArgsForSingleMediaType(int mediaType, String albumId) {
        return new String[]{String.valueOf(mediaType), albumId};
    }

    private static String[] getSelectionAlbumArgs(String albumId) {
        return new String[]{
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
                String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
                albumId
        };
    }

    private final ContentResolver contentResolver;
    private final String selection;
    private final String[] selectionArgs;
    private android.os.CancellationSignal cancellationSignal;
    private AsyncTask asyncTask;
    private Callback<List<Item>> callback;
    private  List<Item> allItems = new ArrayList<>();

    private boolean isHadMore;
    private int startPosition;

    private int offset;

    public MediaLoaderV2(@NonNull Context context, String selection, String[] selectionArgs,
                         Callback<List<Item>> callback, int position) {
        contentResolver = context.getContentResolver();
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.callback = callback;
        startPosition = position;
        isHadMore = true;
    }

    public void refresh() {
        int left = startPosition - PAGE_SIZE / 2;
        if (left < 0) {
            left = 0;
        }

        final int finalLeft = left;
        asyncTask = new AsyncTask<Void, Void, Object[]>() {

            @Override
            protected Object[] doInBackground(Void... voids) {
                Object[] values = query(finalLeft);
                return values;
            }

            @Override
            protected void onPostExecute(Object[] values) {
                super.onPostExecute(values);
                List<Item> items = (List<Item>) values[0];
                int size = (int)values[1];
                offset = finalLeft + size;

                allItems.clear();
                allItems.addAll(items);
                isHadMore = size > 0;

                callback.onResult(allItems);
            }
        }.executeOnExecutor(THREAD_POOL_EXECUTOR);
    }

    public void loadMore() {
        if (isHadMore) {
            Log.d(TAG, "load more");
            asyncTask = new AsyncTask<Void, Void, Object[]>() {

                @Override
                protected Object[] doInBackground(Void... voids) {
                    Object[] values = query(offset);
                    return values;
                }

                @Override
                protected void onPostExecute(Object[] values) {
                    super.onPostExecute(values);
                    List<Item> items = (List<Item>) values[0];
                    int size = (int)values[1];
                    offset = offset + size;

                    int start = allItems.size();
                    allItems.addAll(items);
                    isHadMore = size > 0;

                    if (items.size() > 0) {
                        callback.onLoadMore(start, items.size());
                    }
                }
            }.executeOnExecutor(THREAD_POOL_EXECUTOR);
        }
    }

    public Object[] query(int offset) {
        synchronized (this) {
            cancellationSignal = new CancellationSignal();
        }
        List<Item> allItems = new ArrayList<>();
        Cursor cursor = null;
        int size = 0;
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                String order = MediaStore.MediaColumns.DATE_ADDED + " DESC";
                Bundle bundle = AlbumLoaderV2.createQueryArgsBundle(selection, selectionArgs, PAGE_SIZE, offset, order);
                cursor = contentResolver.query(QUERY_URI, PROJECTION, bundle, cancellationSignal);
            } else {
                String order;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    order = MediaStore.MediaColumns.DATE_ADDED + " DESC";
                } else {
                    order = "datetaken DESC";
                }
                order = order + " limit " + PAGE_SIZE + " offset " + offset;
                cursor = contentResolver.query(QUERY_URI, PROJECTION, selection, selectionArgs, order, cancellationSignal);
            }
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    Item item = Item.valueOf(cursor);
                    if (!TextUtils.isEmpty(item.path)) {
                        File file = new File(item.path);
                        if (file.exists()) {
                            allItems.add(item);
                        }
                    } else {
                        allItems.add(item);
                    }
                    size ++;
                }
            }
        } finally {
            synchronized (this) {
                cancellationSignal = null;
            }
            if (cursor != null) {
                cursor.close();
            }
        }

        return new Object[] {allItems, size};
    }

    public void cancelLoadInBackground() {
        if (asyncTask != null) asyncTask.cancel(true);
        synchronized (this) {
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
            }
        }
    }

    public interface Callback<T> {

        void onResult(T t);

        void onLoadMore(int start, int size);
    }
}
