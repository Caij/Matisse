package com.zhihu.matisse.internal.loader;

import android.Manifest;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
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
import androidx.core.content.ContextCompat;

import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.AppAlbum;
import com.zhihu.matisse.internal.utils.TypeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AppAlbumLoader {

    private static final String TAG = "AppAlbumLoader";

    public static final String[] PROJECTION = {
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME};

    private static class Holder {
        private static AppAlbumLoader appAlbumLoader = new AppAlbumLoader();
    }

    private String selection;
    private  String[] selectionArgs;
    private String BUCKET_ORDER_BY_PAGE = AlbumLoaderV2.getOrder(500);

    public List<Listener> listeners = new ArrayList<>();

    private volatile boolean isLoading = false;

    public static AppAlbumLoader getInstance() {
        return Holder.appAlbumLoader;
    }

    private AppAlbumLoader() {
        selection = AlbumLoaderV2.SELECTION;
        selectionArgs = AlbumLoaderV2.SELECTION_ARGS;
    }

    public void preLoad(final Context context) {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE)
                == PackageManager.PERMISSION_GRANTED) {
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
                            int imageCount = getAlbumMediaCount(album.id, context, AlbumLoaderV2.SELECTION_FOR_SINGLE_MEDIA_TYPE,
                                    AlbumLoaderV2.getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE));
                            int videoCount = getAlbumMediaCount(album.id, context, AlbumLoaderV2.SELECTION_FOR_SINGLE_MEDIA_TYPE,
                                    AlbumLoaderV2.getSelectionArgsForSingleMediaType(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO));
                            album.imageCount = imageCount;
                            album.videoCount = videoCount;
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
    }

    private int loadInBackground(Context context, ContentResolver contentResolver, String orderBy,
                                 LongSparseArray<AppAlbum> tempMap, List<AppAlbum> albums) {
        int size = 0;
        Cursor cursor = null;
        Log.d(TAG, "order by " + orderBy);
        try {
            cursor = ContentResolverCompat.query(contentResolver, AlbumLoaderV2.QUERY_URI, PROJECTION, selection, selectionArgs,
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
        values.put("id", album.id);
        values.put("displayName", album.displayName);
        values.put("imageCount", album.imageCount);
        values.put("videoCount", album.videoCount);
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
                int imageCount = cursor.getInt(cursor.getColumnIndex("imageCount"));
                int videoCount = cursor.getInt(cursor.getColumnIndex("videoCount"));
                AppAlbum appAlbum = new AppAlbum(id, displayName);
                appAlbum.imageCount = imageCount;
                appAlbum.videoCount = videoCount;
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

    public static int getAlbumMediaCount(String bucket_id, Context context, String selection,  String[] selectionArgs) {
        String s = null;
        String[] sargs = null;
        if (!TextUtils.isEmpty(bucket_id) && !bucket_id.equals(Album.ALBUM_ID_ALL)) {
            s = selection +  " AND bucket_id = ?";
            sargs = new String[selectionArgs.length + 1];
            System.arraycopy(selectionArgs, 0, sargs, 0, selectionArgs.length);
            sargs[sargs.length - 1] = bucket_id;
        } else {
            s = selection;
            sargs = selectionArgs;
        }

        return getAlbumMediaCount(context, s, sargs);
    }

    public static int getAlbumMediaCount(Context context, String selection, String[] sargs) {
        final String[] imageCountProjection = new String[]{
                "count(" + MediaStore.Files.FileColumns._ID + ")",
        };

        Cursor countCursor = null;
        try {
            countCursor = context.getContentResolver().query(AlbumLoaderV2.QUERY_URI,
                    imageCountProjection,
                    selection,
                    sargs, null);
            countCursor.moveToFirst();
            return countCursor.getInt(0);
        } catch (Exception e) {
            return 0;
        } finally {
            if (countCursor != null) {
                countCursor.close();
            }
        }
    }

    public static interface Listener {
        void onFinish();
    }
}
