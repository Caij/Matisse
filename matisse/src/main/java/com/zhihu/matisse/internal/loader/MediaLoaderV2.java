package com.zhihu.matisse.internal.loader;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.MediaStore;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContentResolverCompat;
import androidx.core.os.CancellationSignal;

import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.zhihu.matisse.internal.loader.AlbumMediaLoader.TYPE_ONLYSHOWIMAGES;
import static com.zhihu.matisse.internal.loader.AlbumMediaLoader.TYPE_ONLYSHOWVIDEOS;

public class MediaLoaderV2 {

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
    private static final String TAG = "AlbumLoaderV2";
    private final boolean isPreLoad;

    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }
    // =============================================

    private static final int PAGE_SIZE = 200;

    private static final String BUCKET_ORDER_BY_PAGE = MediaStore.MediaColumns.DATE_TAKEN + " DESC limit " + PAGE_SIZE + " offset %d";
//    private static final String BUCKET_ORDER_BY = MediaStore.MediaColumns.DATE_TAKEN + " DESC";

    public static MediaLoaderV2 newInstance(Context context, int type, String bucketId, Callback<List<Item>> albumCallback) {
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

        return new MediaLoaderV2(context, selection, selectionArgs, albumCallback, Album.ALBUM_ID_ALL.equals(bucketId));
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

    private ContentResolver contentResolver;
    private String selection;
    private  String[] selectionArgs;
    private CancellationSignal cancellationSignal;
    private AsyncTask<Void, Void, List<Item>> asyncTask;
    private Callback<List<Item>> callback;

    public MediaLoaderV2(@NonNull Context context, String selection, String[] selectionArgs,
                         Callback<List<Item>> callback, boolean isPreLoad) {
        contentResolver = context.getContentResolver();
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.callback = callback;
        this.isPreLoad = isPreLoad;
    }

    //开始先分页加载
    @SuppressLint("StaticFieldLeak")
    public void startLoad() {
        if (asyncTask != null) asyncTask.cancel(true);
        if (isPreLoad) {
            asyncTask = new AsyncTask<Void, Void, List<Item>>() {

                @Override
                protected List<Item> doInBackground(Void... voids) {
                    List<Item> allItems = new ArrayList<>();
                    loadInBackground(String.format(Locale.getDefault(), BUCKET_ORDER_BY_PAGE, 0), allItems);
                    return allItems;
                }

                @Override
                protected void onPostExecute(List<Item> albums) {
                    super.onPostExecute(albums);
                    callback.onResult(albums);
                    loadAll();
                }
            }.execute();
        } else {
            loadAll();
        }
    }

    @SuppressLint("StaticFieldLeak")
    private void loadAll() {
        if (asyncTask != null) asyncTask.cancel(true);
        asyncTask = new AsyncTask<Void, Void, List<Item>>() {

            @Override
            protected List<Item> doInBackground(Void... voids) {
                int offset = 0;
                int size = 0;
                List<Item> allItems = new ArrayList<>();
                do{
                    size = loadInBackground(String.format(Locale.getDefault(), BUCKET_ORDER_BY_PAGE, offset), allItems);
                    offset += size;
                } while (size > 0);

                return allItems;
            }

            @Override
            protected void onPostExecute(List<Item> albums) {
                super.onPostExecute(albums);
                callback.onResult(albums);

            }
        }.execute();
    }

    private int loadInBackground(String orderBy, List<Item> allItems) {
        int size = 0;
        synchronized (this) {
            cancellationSignal = new CancellationSignal();
        }
        Cursor cursor = null;
        Log.d(TAG, "order by " + orderBy);
        try {
            cursor = ContentResolverCompat.query(contentResolver, QUERY_URI, PROJECTION, selection, selectionArgs,
                            orderBy, cancellationSignal);
            if (cursor != null) {
                size = cursor.getCount();
                while (cursor.moveToNext()) {
                    Item item = Item.valueOf(cursor);
                    allItems.add(item);
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
        return size;
    }

    public void cancelLoadInBackground() {
        if (asyncTask != null) asyncTask.cancel(true);
        synchronized (this) {
            if (cancellationSignal != null) {
                cancellationSignal.cancel();
            }
        }
    }
}
