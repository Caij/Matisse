/*
 * Copyright (C) 2014 nohana, Inc.
 * Copyright 2017 Zhihu Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zhihu.matisse.internal.model;

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.loader.content.Loader;

import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.loader.AlbumLoaderV2;
import com.zhihu.matisse.internal.loader.Callback;

import java.lang.ref.WeakReference;
import java.util.List;

public class AlbumCollection implements Callback<List<Album>> {
    private static final int LOADER_ID = 1;
    private static final String STATE_CURRENT_SELECTION = "state_current_selection";
    private static final String ARGS_TYPE = "args_type";
    private WeakReference<Context> mContext;
    private AlbumCallbacks mCallbacks;
    private int mCurrentSelection;
    private AlbumLoaderV2 albumLoaderV2;


    public AlbumLoaderV2 onCreateLoader(int id, Bundle args) {
        Context context = mContext.get();
        if (context == null) {
            return null;
        }

        int type = args.getInt(ARGS_TYPE);

        return AlbumLoaderV2.newInstance(context, type, this);
    }

    public void onLoaderReset(@NonNull Loader<List<Album>> loader) {
        Context context = mContext.get();
        if (context == null) {
            return;
        }

        mCallbacks.onAlbumReset();
    }

    public void onCreate(FragmentActivity activity, AlbumCallbacks callbacks) {
        mContext = new WeakReference<Context>(activity);
        mCallbacks = callbacks;
    }

    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            return;
        }

        mCurrentSelection = savedInstanceState.getInt(STATE_CURRENT_SELECTION);
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putInt(STATE_CURRENT_SELECTION, mCurrentSelection);
    }

    public void onDestroy() {
        if (albumLoaderV2 != null) albumLoaderV2.cancelLoadInBackground();
        mCallbacks = null;
    }

    public void loadAlbums(int type) {
        Bundle bundle = new Bundle();
        bundle.putInt(ARGS_TYPE, type);
        albumLoaderV2 = onCreateLoader(type, bundle);
        albumLoaderV2.startLoad();
    }

    public int getCurrentSelection() {
        return mCurrentSelection;
    }

    public void setStateCurrentSelection(int currentSelection) {
        mCurrentSelection = currentSelection;
    }

    @Override
    public void onResult(List<Album> albums) {
        Context context = mContext.get();
        if (context == null) {
            return;
        }

        mCallbacks.onAlbumLoad(albums);
    }

    public interface AlbumCallbacks {
        void onAlbumLoad(List<Album> data);

        void onAlbumReset();
    }
}
