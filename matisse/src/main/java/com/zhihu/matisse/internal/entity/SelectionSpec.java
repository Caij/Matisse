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

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Parcelable;
import android.support.annotation.StyleRes;

import com.zhihu.matisse.Matisse;
import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.R;
import com.zhihu.matisse.engine.ImageEngine;
import com.zhihu.matisse.engine.impl.GlideEngine;
import com.zhihu.matisse.filter.Filter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public final class SelectionSpec {

    private static final String MIMETYPESET = "mimetypeset";
    private static final String MEDIATYPEEXCLUSIVE = "mediatypeexclusive";
    private static final String SHOWSINGLEMEDIATYPE = "showsinglemediatype";
    private static final String THEME = "theme";
    private static final String ORIENTATION = "orientation";
    private static final String COUNTABLE = "countable";
    private static final String MAXSELECTABLE = "maxselectable";
    private static final String MAXIMAGESELECTABLE = "maximageselectable";
    private static final String MAXVIDEOSELECTABLE = "maxvideoselectable";
    private static final String CAPTURE = "capture";
    private static final String CAPTURESTRATEGY = "capturestrategy";
    private static final String SPANCOUNT = "spancount";
    private static final String GRIDEXPECTEDSIZE = "gridexpectedsize";
    private static final String SOURCE = "source";
    private static final String OLDITEMS = "olditems";

    public Set<MimeType> mimeTypeSet;
    public boolean mediaTypeExclusive;
    public boolean showSingleMediaType;
    public Theme theme;
    public int orientation;
    public boolean countable;
    public int maxSelectable;
    public int maxImageSelectable;
    public int maxVideoSelectable;

    public boolean capture;
    public CaptureStrategy captureStrategy;
    public int spanCount = 3;
    public int gridExpectedSize;

    public Source source; //是否可选原图
    public ArrayList<Item> oldItems; //是否可选原图

    public List<Filter> filters;

    public SelectionSpec() {
    }

    public static SelectionSpec createSelectionSpec(Intent intent) {
        SelectionSpec selectionSpec = new SelectionSpec();
        selectionSpec.mimeTypeSet = (Set<MimeType>) intent.getSerializableExtra(MIMETYPESET);
        selectionSpec.mediaTypeExclusive = intent.getBooleanExtra(MEDIATYPEEXCLUSIVE, true);
        selectionSpec.showSingleMediaType = intent.getBooleanExtra(SHOWSINGLEMEDIATYPE, false);
        selectionSpec.theme = intent.getParcelableExtra(THEME);
        selectionSpec.orientation = intent.getIntExtra(ORIENTATION, 0);
        selectionSpec.countable = intent.getBooleanExtra(COUNTABLE, false);
        selectionSpec.maxSelectable = intent.getIntExtra(MAXSELECTABLE, 1);
        selectionSpec.maxImageSelectable = intent.getIntExtra(MAXIMAGESELECTABLE, 0);
        selectionSpec.maxVideoSelectable = intent.getIntExtra(MAXVIDEOSELECTABLE, 0);

        selectionSpec.capture = intent.getBooleanExtra(CAPTURE, false);
        selectionSpec.captureStrategy = intent.getParcelableExtra(CAPTURESTRATEGY);
        selectionSpec.spanCount = intent.getIntExtra(SPANCOUNT, 3);
        selectionSpec.gridExpectedSize = intent.getIntExtra(GRIDEXPECTEDSIZE, 0);

        selectionSpec.source = intent.getParcelableExtra(SOURCE);
        selectionSpec.oldItems = intent.getParcelableArrayListExtra(OLDITEMS);
        return selectionSpec;
    }

    public Intent createIntent(Intent intent) {
        intent.putExtra(MIMETYPESET, (Serializable) mimeTypeSet);
        intent.putExtra(MEDIATYPEEXCLUSIVE, mediaTypeExclusive);
        intent.putExtra(SHOWSINGLEMEDIATYPE, showSingleMediaType);
        intent.putExtra(THEME, (Parcelable) theme);
        intent.putExtra(ORIENTATION, orientation);
        intent.putExtra(COUNTABLE, countable);
        intent.putExtra(MAXSELECTABLE, maxSelectable);
        intent.putExtra(MAXIMAGESELECTABLE, maxImageSelectable);
        intent.putExtra(MAXVIDEOSELECTABLE, maxVideoSelectable);

        intent.putExtra(CAPTURE, capture);
        intent.putExtra(CAPTURESTRATEGY, (Parcelable) captureStrategy);
        intent.putExtra(SPANCOUNT, spanCount);
        intent.putExtra(GRIDEXPECTEDSIZE, gridExpectedSize);

        intent.putExtra(SOURCE, (Parcelable) source);
        intent.putParcelableArrayListExtra(OLDITEMS, oldItems);

        return intent;
    }


    public boolean singleSelectionModeEnabled() {
        return !countable && (maxSelectable == 1 || (maxImageSelectable == 1 && maxVideoSelectable == 1));
    }

    public boolean needOrientationRestriction() {
        return orientation != ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED;
    }

    public boolean onlyShowImages() {
        return showSingleMediaType && MimeType.ofImage().containsAll(mimeTypeSet);
    }

    public boolean onlyShowVideos() {
        return showSingleMediaType && MimeType.ofVideo().containsAll(mimeTypeSet);
    }

}
