package com.zhihu.matisse.internal.loader;

import android.annotation.SuppressLint;
import android.content.ContentResolver;
import android.content.ContentUris;
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
import java.util.Map;

import static com.zhihu.matisse.internal.loader.AlbumMediaLoader.TYPE_ONLYSHOWIMAGES;
import static com.zhihu.matisse.internal.loader.AlbumMediaLoader.TYPE_ONLYSHOWVIDEOS;

public class AlbumLoaderV2 {

    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");


    public static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME};

    // === params for showSingleMediaType: false ===
    private static final String SELECTION =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    private static final String[] SELECTION_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };
    // =============================================

    // === params for showSingleMediaType: true ===
    private static final String SELECTION_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    private static final String TAG = "AlbumLoaderV2";

    private static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }
    // =============================================

    private static final int PAGE_SIZE = 200;

    private static final String BUCKET_ORDER_BY_PAGE = MediaStore.MediaColumns.DATE_TAKEN + " DESC limit " + PAGE_SIZE + " offset %d";
//    private static final String BUCKET_ORDER_BY = MediaStore.MediaColumns.DATE_TAKEN + " DESC";

    public static AlbumLoaderV2 newInstance(Context context, int type, Callback<List<Album>> albumCallback) {
        String selection;
        String[] selectionArgs;
        if (type == TYPE_ONLYSHOWIMAGES) {
            selection = SELECTION_FOR_SINGLE_MEDIA_TYPE;
            selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE);
        } else if (type == TYPE_ONLYSHOWVIDEOS) {
            selection = SELECTION_FOR_SINGLE_MEDIA_TYPE;
            selectionArgs = getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO);
        } else {
            selection = SELECTION;
            selectionArgs = SELECTION_ARGS;
        }
        return new AlbumLoaderV2(context, selection, selectionArgs, albumCallback);
    }


    private ContentResolver contentResolver;
    private String selection;
    private  String[] selectionArgs;
    private CancellationSignal cancellationSignal;
    private AsyncTask<Void, Void, List<Album>> asyncTask;
    private Callback<List<Album>> callback;

    public AlbumLoaderV2(@NonNull Context context, String selection, String[] selectionArgs,
                         Callback<List<Album>> callback) {
        contentResolver = context.getContentResolver();
        this.selection = selection;
        this.selectionArgs = selectionArgs;
        this.callback = callback;
    }

    //开始先分页加载
    @SuppressLint("StaticFieldLeak")
    public void startLoad() {
        if (asyncTask != null) asyncTask.cancel(true);
        asyncTask = new AsyncTask<Void, Void, List<Album>>() {

            @Override
            protected List<Album> doInBackground(Void... voids) {
                LongSparseArray<Album> tempMap = new LongSparseArray<>();
                List<Album> allAlbums = new ArrayList<>();
                int size = loadInBackground(String.format(Locale.getDefault(), BUCKET_ORDER_BY_PAGE, 0), tempMap, allAlbums);

                if (!allAlbums.isEmpty()) {
                    allAlbums.get(0).itemSize = size;
                }

                return allAlbums;
            }

            @Override
            protected void onPostExecute(List<Album> albums) {
                super.onPostExecute(albums);
                callback.onResult(albums);

                if (!albums.isEmpty()) {
                    if (albums.get(0).itemSize >= (PAGE_SIZE * 0.9f)) {
                        loadAll();
                    }
                }
            }
        }.execute();
    }

    @SuppressLint("StaticFieldLeak")
    private void loadAll() {
        if (asyncTask != null) asyncTask.cancel(true);
        asyncTask = new AsyncTask<Void, Void, List<Album>>() {

            @Override
            protected List<Album> doInBackground(Void... voids) {
                int offset = 0;
                int size = 0;
                List<Album> allAlbums = new ArrayList<>();
                LongSparseArray<Album> tempMap = new LongSparseArray<>();
                int allItemCount = 0;
                do{
                    size = loadInBackground(String.format(Locale.getDefault(), BUCKET_ORDER_BY_PAGE, offset),
                            tempMap, allAlbums);
                    offset += size;
                    allItemCount += size;
                } while (size > 0);

                if (!allAlbums.isEmpty()) {
                    allAlbums.get(0).itemSize = allItemCount;
                }

                return allAlbums;
            }

            @Override
            protected void onPostExecute(List<Album> albums) {
                super.onPostExecute(albums);
                callback.onResult(albums);

            }
        }.execute();
    }

    private int loadInBackground(String orderBy, LongSparseArray<Album> tempMap, List<Album> albums) {
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
//                    Item item = Item.valueOf(cursor);
                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                    long bucketId = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID));
                    Uri uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id);
                    Album album = tempMap.get(bucketId);
                    if (album == null) {
                        String bucketDisplayName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME));
                        album = new Album(String.valueOf(bucketId), uri, bucketDisplayName);

                        if (albums.isEmpty()) {
                            Album all = new Album(Album.ALBUM_ID_ALL, uri, Album.ALBUM_NAME_ALL);
                            albums.add(all);
                        }

                        albums.add(album);

                        tempMap.put(bucketId, album);
                    }

                    album.itemSize = album.itemSize + 1;
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
