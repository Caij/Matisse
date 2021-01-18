package com.zhihu.matisse.internal.loader;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.collection.LongSparseArray;
import androidx.core.content.ContentResolverCompat;

import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.AppAlbum;
import com.zhihu.matisse.internal.utils.TypeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppAlbumLoader {

    private static class Holder {
        private static AppAlbumLoader appAlbumLoader = new AppAlbumLoader();
    }

    private static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");


    public static final String[] PROJECTION = {
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

    private static final int PAGE_SIZE = 500;

    private static final String BUCKET_ORDER_BY_PAGE;

    public List<Listener> listeners = new ArrayList<>();

    static {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            BUCKET_ORDER_BY_PAGE = MediaStore.MediaColumns.DATE_ADDED + " DESC limit " + PAGE_SIZE + " offset %d";
        } else {
            BUCKET_ORDER_BY_PAGE = MediaStore.MediaColumns.DATE_TAKEN + " DESC limit " + PAGE_SIZE + " offset %d";
        }
    }

    private String selection;
    private  String[] selectionArgs;
    private volatile boolean isLoading = false;

    public static AppAlbumLoader getInstance() {
        return Holder.appAlbumLoader;
    }

    private AppAlbumLoader() {
        selection = SELECTION;
        selectionArgs = SELECTION_ARGS;
    }

    public void preLoad(final Context context) {
        if (!isLoading) {
            new AsyncTask<Void, Void, List<Album>>() {

                @Override
                protected List<Album> doInBackground(Void... voids) {
                    isLoading = true;

                    int offset = 0;
                    int size = 0;
                    ContentResolver contentResolver = context.getContentResolver();
                    List<AppAlbum> allAlbums = new ArrayList<>();
                    LongSparseArray<AppAlbum> tempMap = new LongSparseArray<>();
                    do {
                        size = loadInBackground(context, contentResolver, String.format(Locale.getDefault(), BUCKET_ORDER_BY_PAGE, offset),
                                tempMap, allAlbums);
                        offset += size;
                    } while (size > 0);

                    SQLiteDatabase database = DBHelper.getSQLiteDatabase(context);
                    database.beginTransaction();

                    database.execSQL("delete from AppAlbum");
                    for (AppAlbum album : allAlbums) {
                        insertAlbum(database, album);
                    }
                    database.setTransactionSuccessful();
                    database.endTransaction();

                    isLoading = false;
                    return null;
                }

                @Override
                protected void onPostExecute(List<Album> albums) {
                    super.onPostExecute(albums);
                }

            }.executeOnExecutor(MediaLoaderV2.THREAD_POOL_EXECUTOR);
        } else {
            Log.d(TAG, "loading wait");
        }
    }

    private int loadInBackground(Context context, ContentResolver contentResolver, String orderBy,
                                 LongSparseArray<AppAlbum> tempMap, List<AppAlbum> albums) {
        int size = 0;
        Cursor cursor = null;
        Log.d(TAG, "order by " + orderBy);
        try {
            cursor = ContentResolverCompat.query(contentResolver, QUERY_URI, PROJECTION, selection, selectionArgs,
                    orderBy, null);
            if (cursor != null) {
                size = cursor.getCount();
                while (cursor.moveToNext()) {
                    long bucketId = cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_ID));
                    AppAlbum album = tempMap.get(bucketId);
                    if (album == null) {
                        String bucketDisplayName = cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.BUCKET_DISPLAY_NAME));
                        album = new AppAlbum(String.valueOf(bucketId), bucketDisplayName);

                        AppAlbum all = null;
                        if (albums.isEmpty()) {
                            all = new AppAlbum(Album.ALBUM_ID_ALL, Album.ALBUM_NAME_ALL);
                            albums.add(all);
                        }

                        albums.add(album);
                        tempMap.put(bucketId, album);
                    }
                }
            }

            if (albums.isEmpty()) {
                AppAlbum all = new AppAlbum(Album.ALBUM_ID_ALL, context.getString(R.string.album_name_all));
                albums.add(all);
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return size;
    }

    public static void insertAlbum(SQLiteDatabase database,  AppAlbum album) {
        ContentValues values = new ContentValues();
        values.put("id", album.getId());
        values.put("displayName", album.getDisplayName());
        database.insertWithOnConflict("AppAlbum", null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static List<AppAlbum> getAllAppAlbum(Context context) {
        SQLiteDatabase database = DBHelper.getSQLiteDatabase(context);
        Cursor cursor = database.rawQuery("SELECT * FROM AppAlbum", null);
        List<AppAlbum> appAlbums = new ArrayList<>();
        if (cursor != null) {
            while (cursor.moveToNext()) {
                String id = cursor.getString(cursor.getColumnIndex("id"));
                String displayName = cursor.getString(cursor.getColumnIndex("displayName"));
                AppAlbum appAlbum = new AppAlbum(id, displayName);
                appAlbums.add(appAlbum);
            }
        }
        return appAlbums;
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }

    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }

    public static interface Listener {
        void onFinish();
    }
}
