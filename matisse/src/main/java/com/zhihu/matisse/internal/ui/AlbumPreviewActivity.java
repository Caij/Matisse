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
package com.zhihu.matisse.internal.ui;

import android.database.Cursor;
import android.os.Bundle;
import androidx.annotation.Nullable;

import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.loader.Callback;
import com.zhihu.matisse.internal.loader.MediaLoaderV2;
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter;
import com.zhihu.matisse.internal.utils.TypeUtil;

import java.util.ArrayList;
import java.util.List;


public class AlbumPreviewActivity extends BasePreviewActivity implements Callback<List<Item>> {

    public static final String EXTRA_ALBUM = "extra_album";
    public static final String EXTRA_ITEM = "extra_item";


    private boolean mIsAlreadySetPosition;
    private PreviewPagerAdapter adapter;
    private MediaLoaderV2 mediaLoaderV2;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Album album = getIntent().getParcelableExtra(EXTRA_ALBUM);

        if (album == null) {
            finish();
            return;
        }

        SelectionSpec selectionSpec = SelectionSpec.createSelectionSpec(getIntent());
        int type = TypeUtil.getShowType(selectionSpec);

        Item item = getIntent().getParcelableExtra(EXTRA_ITEM);
        if (mSpec.countable) {
            mCheckView.setCheckedNum(mSelectedCollection.checkedNumOf(item));
        } else {
            mCheckView.setChecked(mSelectedCollection.isSelected(item));
        }

        mediaLoaderV2 = MediaLoaderV2.newInstance(this, type, album.getId(), this);
        mediaLoaderV2.startLoad();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaLoaderV2 != null) {
            mediaLoaderV2.cancelLoadInBackground();
        }
    }

    @Override
    public void onResult(List<Item> items) {
        if (items.isEmpty()) {
            return;
        }

        PreviewPagerAdapter adapter = (PreviewPagerAdapter) mPager.getAdapter();
        adapter.addAll(items);
        adapter.notifyDataSetChanged();
        if (!mIsAlreadySetPosition) {
            //onAlbumMediaLoad is called many times..
            mIsAlreadySetPosition = true;
            Item selected = getIntent().getParcelableExtra(EXTRA_ITEM);
            int selectedIndex = items.indexOf(selected);
            mPager.setCurrentItem(selectedIndex, false);
            mPreviousPos = selectedIndex;
        }
    }
}
