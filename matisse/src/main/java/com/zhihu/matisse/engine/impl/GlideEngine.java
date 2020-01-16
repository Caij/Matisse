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
package com.zhihu.matisse.engine.impl;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.util.Log;
import android.widget.ImageView;

import androidx.annotation.Nullable;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.zhihu.matisse.engine.ImageEngine;

/**
 * {@link ImageEngine} implementation using Glide.
 */

public class GlideEngine implements ImageEngine {

    @Override
    public void loadThumbnail(Context context, Drawable placeholder, ImageView imageView, Uri path) {
        Glide.with(context).load(path).centerCrop().placeholder(placeholder).into(imageView);
    }

    @Override
    public void loadGifThumbnail(Context context, Drawable placeholder, ImageView imageView, Uri path) {
        Glide.with(context).load(path).centerCrop().placeholder(placeholder).into(imageView);
    }

    @Override
    public void loadImage(Context context, ImageView imageView, String path) {
        Glide.with(context).load(path).fitCenter().into(imageView);
    }

    @Override
    public void loadGifImage(Context context, ImageView imageView, Uri path) {
        Glide.with(context).load(path).fitCenter().into(imageView);
    }

    @Override
    public boolean supportAnimatedGif() {
        return true;
    }

}
