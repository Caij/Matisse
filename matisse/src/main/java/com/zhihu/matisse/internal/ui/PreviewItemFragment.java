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

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.graphics.Point;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;

import com.github.chrisbanes.photoview.PhotoView;
import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.ui.widget.BigImageView;
import com.zhihu.matisse.internal.utils.PhotoMetadataUtils;

public class PreviewItemFragment extends Fragment {

    private static final String ARGS_ITEM = "args_item";

    private PhotoView mPhotoView;
    private BigImageView mBigImageView;

    public static PreviewItemFragment newInstance(Item item) {
        PreviewItemFragment fragment = new PreviewItemFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(ARGS_ITEM, item);
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_preview_item, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        final Item item = getArguments().getParcelable(ARGS_ITEM);
        if (item == null) {
            return;
        }

        View videoPlayButton = view.findViewById(R.id.video_play_button);
        if (item.isVideo()) {
            videoPlayButton.setVisibility(View.VISIBLE);
            videoPlayButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Intent intent = new Intent(Intent.ACTION_VIEW);
                    intent.setDataAndType(item.getUri(), "video/*");
                    try {
                        startActivity(intent);
                    } catch (ActivityNotFoundException e) {
                        Toast.makeText(getContext(), R.string.error_no_video_activity, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        } else {
            videoPlayButton.setVisibility(View.GONE);
        }

        mPhotoView = (PhotoView) view.findViewById(R.id.image_view);
        mBigImageView = view.findViewById(R.id.big_view);

        if (item.isGif() || item.isVideo()) {
            Matisse.imageEngine.loadGifImage(getContext(), mPhotoView, item.path);
            mBigImageView.setVisibility(View.GONE);
            mPhotoView.setVisibility(View.VISIBLE);
        } else {
            mBigImageView.setVisibility(View.VISIBLE);
            mPhotoView.setVisibility(View.GONE);
            if (PhotoMetadataUtils.isLongImage(item.path)) {
                mBigImageView.setInitScaleType(BigImageView.INIT_SCALE_TYPE_TOP_CROP);
            }else {
                mBigImageView.setInitScaleType(BigImageView.INIT_SCALE_TYPE_CENTER_INSIDE);
            }
            mBigImageView.setImage(item.path);
        }
    }

    public void resetView() {

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBigImageView.recycle();
    }
}
