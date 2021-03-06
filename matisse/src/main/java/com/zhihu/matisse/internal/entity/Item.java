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
package com.zhihu.matisse.internal.entity;

import android.content.ContentUris;
import android.database.Cursor;
import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.text.TextUtils;

import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.internal.loader.AlbumLoaderV2;
import com.zhihu.matisse.internal.utils.TypeUtil;

public class Item implements Parcelable {

    public static final long ITEM_ID_CAPTURE_IMAGE = -2;
    public static final String ITEM_DISPLAY_NAME_CAPTURE = "Capture";
    public final long id;
    public final String mimeType;
    public final long size;
    public final long duration; // only for video, in ms
    public final Uri uri;
    public String path;

    public Item(long id, String mimeType, long size, long duration) {
        this.id = id;
        this.mimeType = mimeType;
        this.size = size;
        this.duration = duration;

        Uri contentUri;
        if (isImage()) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (isVideo()) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            // ?
            contentUri = MediaStore.Files.getContentUri("external");
        }
        this.uri = ContentUris.withAppendedId(contentUri, id);
    }

    public Item(long id, Uri uri, String mimeType, long size, long duration) {
        this.id = id;
        this.mimeType = mimeType;
        this.size = size;
        this.duration = duration;
        this.uri = uri;
    }


    protected Item(Parcel in) {
        id = in.readLong();
        mimeType = in.readString();
        size = in.readLong();
        duration = in.readLong();
        uri = in.readParcelable(Uri.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(mimeType);
        dest.writeLong(size);
        dest.writeLong(duration);
        dest.writeParcelable(uri, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<Item> CREATOR = new Creator<Item>() {
        @Override
        public Item createFromParcel(Parcel in) {
            return new Item(in);
        }

        @Override
        public Item[] newArray(int size) {
            return new Item[size];
        }
    };

    public static Item valueOf(Cursor cursor) {
        Item item = new Item(cursor.getLong(cursor.getColumnIndex(MediaStore.Files.FileColumns._ID)),
                cursor.getString(cursor.getColumnIndex(MediaStore.MediaColumns.MIME_TYPE)),
                cursor.getLong(cursor.getColumnIndex(MediaStore.MediaColumns.SIZE)),
                cursor.getLong(cursor.getColumnIndex("duration")));
        item.path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        return item;
    }

    public boolean isImage() {
        return TypeUtil.isImage(mimeType);
    }

    public boolean isGif() {
        return TypeUtil.isGif(mimeType);
    }

    public boolean isVideo() {
        return TypeUtil.isVideo(mimeType);
    }

    public Uri getUri() {
        Uri contentUri;
        if (isImage()) {
            contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
        } else if (isVideo()) {
            contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        } else {
            // ?
            contentUri = MediaStore.Files.getContentUri("external");
        }
        return  ContentUris.withAppendedId(contentUri, id);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof Item)) {
            return false;
        }

        Item other = (Item) obj;

        return (mimeType != null && mimeType.equals(other.mimeType)
                    || (mimeType == null && other.mimeType == null))
                && (uri != null && uri.equals(other.uri)
                    || (uri == null && other.uri == null))
                && size == other.size
                && duration == other.duration;
    }

    @Override
    public int hashCode() {
        int result = 1;
        if (!TextUtils.isEmpty(mimeType)) {
            result = 31 * result + mimeType.hashCode();
        }
        result = 31 * result + uri.hashCode();
        result = 31 * result + Long.valueOf(size).hashCode();
        result = 31 * result + Long.valueOf(duration).hashCode();
        return result;
    }

}
