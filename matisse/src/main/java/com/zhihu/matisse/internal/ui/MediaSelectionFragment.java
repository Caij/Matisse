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

import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.loader.Callback;
import com.zhihu.matisse.internal.loader.MediaLoaderV2;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter;
import com.zhihu.matisse.internal.ui.widget.MediaGridInset;
import com.zhihu.matisse.internal.utils.TypeUtil;
import com.zhihu.matisse.internal.utils.UIUtils;

import java.util.List;

public class MediaSelectionFragment extends Fragment implements AlbumMediaAdapter.CheckStateListener,
        AlbumMediaAdapter.OnMediaClickListener, Callback<List<Item>> {

    public static final String EXTRA_ALBUM = "extra_album";

    public static final int STATE_NORMAL = 1;
    public static final int STATE_LOADING = 2;
    public static final int STATE_UNABLE = 3;

    private RecyclerView mRecyclerView;
    private AlbumMediaAdapter mAdapter;
    private SelectionProvider mSelectionProvider;
    private AlbumMediaAdapter.CheckStateListener mCheckStateListener;
    private AlbumMediaAdapter.OnMediaClickListener mOnMediaClickListener;
    private MediaLoaderV2 mediaLoaderV2;
    private Album album;
    private int mState = STATE_NORMAL;

    public static MediaSelectionFragment newInstance(Album album) {
        MediaSelectionFragment fragment = new MediaSelectionFragment();
        Bundle args = new Bundle();
        args.putParcelable(EXTRA_ALBUM, album);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof SelectionProvider) {
            mSelectionProvider = (SelectionProvider) context;
        } else {
            throw new IllegalStateException("Context must implement SelectionProvider.");
        }
        if (context instanceof AlbumMediaAdapter.CheckStateListener) {
            mCheckStateListener = (AlbumMediaAdapter.CheckStateListener) context;
        }
        if (context instanceof AlbumMediaAdapter.OnMediaClickListener) {
            mOnMediaClickListener = (AlbumMediaAdapter.OnMediaClickListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_media_selection, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerview);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        album = getArguments().getParcelable(EXTRA_ALBUM);

        SelectionSpec selectionSpec = SelectionSpec.createSelectionSpec(getActivity().getIntent());

        mAdapter = new AlbumMediaAdapter(album, getContext(),
                mSelectionProvider.provideSelectedItemCollection(), mRecyclerView, selectionSpec);
        mAdapter.registerCheckStateListener(this);
        mAdapter.registerOnMediaClickListener(this);
        mRecyclerView.setHasFixedSize(true);

        int spanCount;

        if (selectionSpec.gridExpectedSize > 0) {
            spanCount = UIUtils.spanCount(getContext(), selectionSpec.gridExpectedSize);
        } else {
            spanCount = selectionSpec.spanCount;
        }
        mRecyclerView.setLayoutManager(new GridLayoutManager(getContext(), spanCount));

        int spacing = getResources().getDimensionPixelSize(R.dimen.media_grid_spacing);
        mRecyclerView.addItemDecoration(new MediaGridInset(spanCount, spacing, false));
        mRecyclerView.setAdapter(mAdapter);
        int type = TypeUtil.getShowType(selectionSpec);

        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);

                RecyclerView.LayoutManager layoutManager = mRecyclerView.getLayoutManager();
                final int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItemPosition = 0;

                if (layoutManager instanceof LinearLayoutManager) {
                    LinearLayoutManager linearLayoutManager = (LinearLayoutManager) layoutManager;
                    lastVisibleItemPosition = linearLayoutManager.findLastVisibleItemPosition();
                } else if (layoutManager instanceof StaggeredGridLayoutManager) {
                    StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
                    int[] into = new int[staggeredGridLayoutManager.getSpanCount()];
                    ((StaggeredGridLayoutManager) layoutManager).findLastVisibleItemPositions(into);
                    lastVisibleItemPosition = findMax(into);
                }

                if (lastVisibleItemPosition >= totalItemCount - 1
                        && mState == STATE_NORMAL) {
                    mState = STATE_LOADING;
                    onLoadMore();
                }
            }
        });

        mediaLoaderV2 = MediaLoaderV2.newInstance(getActivity(), type, album.getId(), 0, this);
        mediaLoaderV2.refresh();
    }

    private void onLoadMore() {
        mediaLoaderV2.loadMore();
    }

    private static int findMax(int[] lastPositions) {
        int max = lastPositions[0];
        for (int value : lastPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    @Override
    public void onDestroyView() {

        mediaLoaderV2.cancelLoadInBackground();

        super.onDestroyView();
    }

    public void refreshMediaGrid() {
        mAdapter.notifyDataSetChanged();
    }


    @Override
    public void onUpdate() {
        // notify outer Activity that check state changed
        if (mCheckStateListener != null) {
            mCheckStateListener.onUpdate();
        }
    }

    @Override
    public void onMediaClick(Album album, Item item, int adapterPosition) {
        if (mOnMediaClickListener != null) {
            mOnMediaClickListener.onMediaClick(album,
                    item, adapterPosition);
        }
    }

    @Override
    public void onResult(List<Item> items) {
        mAdapter.setItems(items);
        mAdapter.notifyDataSetChanged();
        mState = STATE_NORMAL;
    }

    public interface SelectionProvider {
        SelectedItemCollection provideSelectedItemCollection();
    }
}
