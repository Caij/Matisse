package com.zhihu.matisse.internal.loader;

import android.annotation.SuppressLint;
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

import androidx.annotation.NonNull;
import androidx.collection.LongSparseArray;
import androidx.core.content.ContentResolverCompat;
import androidx.core.os.CancellationSignal;

import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.AppAlbum;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AlbumLoaderV2 implements AppAlbumLoader.Listener {

    public static final int TYPE_ONLYSHOWIMAGES = 1;
    public static final int TYPE_ONLYSHOWVIDEOS = 2;
    public static final int TYPE_ALL = 3;

    public static final Uri QUERY_URI = MediaStore.Files.getContentUri("external");


    public static final String[] PROJECTION = {
            MediaStore.Files.FileColumns._ID,
            MediaStore.MediaColumns.BUCKET_ID,
            MediaStore.MediaColumns.BUCKET_DISPLAY_NAME};

    // === params for showSingleMediaType: false ===
    public static final String SELECTION =
            "(" + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " OR "
                    + MediaStore.Files.FileColumns.MEDIA_TYPE + "=?)"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";

    public static final String[] SELECTION_ARGS = {
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE),
            String.valueOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO),
    };
    // =============================================

    // === params for showSingleMediaType: true ===
    public static final String SELECTION_FOR_SINGLE_MEDIA_TYPE =
            MediaStore.Files.FileColumns.MEDIA_TYPE + "=?"
                    + " AND " + MediaStore.MediaColumns.SIZE + ">0";
    private static final String TAG = "AlbumLoaderV2";

    public static String[] getSelectionArgsForSingleMediaType(int mediaType) {
        return new String[]{String.valueOf(mediaType)};
    }
    // =============================================

    private static final int PAGE_SIZE = 200;

    private static final String BUCKET_ORDER_BY_PAGE;
//    private static final String BUCKET_ORDER_BY = MediaStore.MediaColumns.DATE_TAKEN + " DESC";

    static {
        BUCKET_ORDER_BY_PAGE = getOrder(PAGE_SIZE);
    }

    public static String getOrder(int pageSize) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return MediaStore.MediaColumns.DATE_ADDED + " DESC limit " + pageSize + " offset %d";
        } else {
            return MediaStore.MediaColumns.DATE_TAKEN + " DESC limit " + pageSize + " offset %d";
        }
    }

    public static AlbumLoaderV2 newInstance(Context context, int type, Callback<List<Album>> albumCallback) {
        return new AlbumLoaderV2(context, type, albumCallback);
    }


    private AsyncTask asyncTask;
    private Callback<List<Album>> callback;
    private int type;
    private Context context;
    String selection;
    String[] selectionArgs;

    public AlbumLoaderV2(@NonNull Context context, int type,
                         Callback<List<Album>> callback) {
        this.callback = callback;
        this.type = type;
        this.context = context;

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

        AppAlbumLoader.getInstance().addListener(this);
    }

    //开始先分页加载
    @SuppressLint("StaticFieldLeak")
    public void load() {
        if (asyncTask != null) asyncTask.cancel(true);
        asyncTask = new AsyncTask<Void, Void, List<Album>>() {

            @Override
            protected List<Album> doInBackground(Void... voids) {
                List<AppAlbum> allAlbums = AppAlbumLoader.getAllAppAlbum(context);
                List<Album> albums = new ArrayList<>();
                if (!allAlbums.isEmpty()) {
                    for (AppAlbum appAlbum : allAlbums) {
                        Album album = null;
                        if (type == TYPE_ONLYSHOWIMAGES) {
                            if (appAlbum.imageCount > 0) {
                                album = new Album(appAlbum.id, getAlbumCover(appAlbum.id),
                                        appAlbum.displayName);
                                album.itemSize = appAlbum.imageCount;
                            }
                        } else if (type == TYPE_ONLYSHOWVIDEOS) {
                            if (appAlbum.videoCount > 0) {
                                album = new Album(appAlbum.id, getAlbumCover(appAlbum.id),
                                        appAlbum.displayName);
                                album.itemSize = appAlbum.videoCount;
                            }
                        } else {
                            album = new Album(appAlbum.id, getAlbumCover(appAlbum.id),
                                    appAlbum.displayName);
                            album.itemSize = appAlbum.videoCount + appAlbum.imageCount;
                        }

                        if (album != null) {
                            albums.add(album);
                        }
                    }

                } else {
                    Album album = new Album(Album.ALBUM_ID_ALL, getAlbumCover(null), context.getString(R.string.album_name_all));
                    albums.add(album);
                }

                asyncGetCount(albums);

                return albums;
            }

            @Override
            protected void onPostExecute(List<Album> albums) {
                super.onPostExecute(albums);
                if (!isCancelled()) {
                    callback.onResult(albums);
                }
            }
        }.executeOnExecutor(MediaLoaderV2.THREAD_POOL_EXECUTOR);;
    }

    private void asyncGetCount(final List<Album> albums) {
        asyncTask = new AsyncTask<Void, Void, Integer>() {

            @Override
            protected Integer doInBackground(Void... voids) {
                for (Album album : albums) {
                    album.itemSize = getAlbumMediaCount(album.getId());
                }
                return null;
            }

            @Override
            protected void onPostExecute(Integer integer) {
                super.onPostExecute(integer);
                if (!isCancelled()) {
                    callback.onResult(albums);
                }
            }
        }.executeOnExecutor(MediaLoaderV2.THREAD_POOL_EXECUTOR);
    }

    private Uri getAlbumCover(String bucket_id) {
        String selection = null;
        String[] sargs = null;
        if (!TextUtils.isEmpty(bucket_id) && !bucket_id.equals(Album.ALBUM_ID_ALL)) {
            selection = this.selection +  " AND bucket_id = ?";
            sargs = new String[selectionArgs.length + 1];
            System.arraycopy(selectionArgs, 0, sargs, 0, selectionArgs.length);
            sargs[sargs.length - 1] = bucket_id;
        } else {
            selection = this.selection;
            sargs = selectionArgs;
        }

        Uri uri = null;
        Cursor cursor = null;
        try {
            cursor = context.getContentResolver().query(QUERY_URI, PROJECTION, selection, sargs, String.format(getOrder(1), 0));

            if (cursor != null) {
                if (cursor.moveToNext()) {
                    long id = cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                    uri = ContentUris.withAppendedId(MediaStore.Files.getContentUri("external"), id);
                }
            }
        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }
        return uri;
    }

    private int getAlbumMediaCount(String bucket_id) {
        return AppAlbumLoader.getAlbumMediaCount(bucket_id, context, selection, selectionArgs);
    }

    public void cancelLoadInBackground() {
        if (asyncTask != null) asyncTask.cancel(true);

        AppAlbumLoader.getInstance().removeListener(this);
    }

    @Override
    public void onFinish() {
        load();
    }
}
