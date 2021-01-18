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
package com.zhihu.matisse.ui;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.fragment.app.Fragment;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.Toolbar;

import android.os.Parcelable;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.zhihu.matisse.MimeType;
import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.Album;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.loader.MediaLoaderV2;
import com.zhihu.matisse.internal.model.AlbumCollection;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.AlbumPreviewActivity;
import com.zhihu.matisse.internal.ui.BasePreviewActivity;
import com.zhihu.matisse.internal.ui.MediaSelectionFragment;
import com.zhihu.matisse.internal.ui.SelectedPreviewActivity;
import com.zhihu.matisse.internal.ui.adapter.AlbumMediaAdapter;
import com.zhihu.matisse.internal.ui.adapter.AlbumsAdapter;
import com.zhihu.matisse.internal.ui.widget.AlbumsSpinner;
import com.zhihu.matisse.internal.utils.MediaStoreCompat;
import com.zhihu.matisse.internal.utils.SizeUtils;
import com.zhihu.matisse.internal.utils.TypeUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * Main Activity to display albums and media content (images/videos) in each album
 * and also support media selecting operations.
 */
public class MatisseActivity extends AppCompatActivity implements
        AlbumCollection.AlbumCallbacks, AdapterView.OnItemSelectedListener,
        MediaSelectionFragment.SelectionProvider, View.OnClickListener,
        AlbumMediaAdapter.CheckStateListener, AlbumMediaAdapter.OnMediaClickListener,
        AlbumMediaAdapter.OnPhotoCapture {

    private static final int REQUEST_CAMERA_PERMISSION_CODE = 1000;
    private static final int REQUEST_MEDIA_PERMISSION_CODE = 1001;

    public static final String EXTRA_RESULT_SELECTION = "extra_result_selection";
    public static final String EXTRA_RESULT_SOURCE = "extra_result_source";
    private static final int REQUEST_CODE_PREVIEW = 23;
    private static final int REQUEST_CODE_CAPTURE = 24;
    private final AlbumCollection mAlbumCollection = new AlbumCollection();
    private MediaStoreCompat mMediaStoreCompat;
    private SelectedItemCollection mSelectedCollection;
    private SelectionSpec mSpec;

    private AlbumsSpinner mAlbumsSpinner;
    private AlbumsAdapter mAlbumsAdapter;
    private TextView mButtonPreview;
    private TextView mButtonApply;
    private View mContainer;
    private View mEmptyView;
    private AppCompatCheckBox mCbSource;
    private TextView tvCapture;
    private boolean isInitAlbum;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        // programmatically set theme before super.onCreate()
        mSpec = SelectionSpec.createSelectionSpec(getIntent());
        mSelectedCollection = new SelectedItemCollection(this, mSpec);
        if (mSpec.theme != null) {
            setTheme(mSpec.theme.themeId);
        }else {
            setTheme(R.style.Matisse_Zhihu);
        }
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_matisse);

        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        tvCapture = findViewById(R.id.tv_capture);
        tvCapture.setOnClickListener(this);

        if (mSpec.capture) {
            mMediaStoreCompat = new MediaStoreCompat(this);
            if (mSpec.captureStrategy == null)
                throw new RuntimeException("Don't forget to set CaptureStrategy.");
            mMediaStoreCompat.setCaptureStrategy(mSpec.captureStrategy);
            tvCapture.setVisibility(View.VISIBLE);
        } else {
            tvCapture.setVisibility(View.GONE);
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(false);
        actionBar.setDisplayHomeAsUpEnabled(true);
        Drawable navigationIcon = toolbar.getNavigationIcon();
        TypedArray ta = getTheme().obtainStyledAttributes(new int[]{R.attr.album_element_color});
        int color = ta.getColor(0, 0);
        ta.recycle();
        navigationIcon.setColorFilter(color, PorterDuff.Mode.SRC_IN);



        mButtonPreview = (TextView) findViewById(R.id.button_preview);
        mButtonApply = (TextView) findViewById(R.id.button_apply);
        mButtonPreview.setOnClickListener(this);
        mButtonApply.setOnClickListener(this);
        mContainer = findViewById(R.id.container);
        mEmptyView = findViewById(R.id.empty_view);

        mSelectedCollection.onCreate(savedInstanceState);


        mAlbumsAdapter = new AlbumsAdapter(this);
        mAlbumsSpinner = new AlbumsSpinner(this);
        mAlbumsSpinner.setOnItemSelectedListener(this);
        TextView tvSelectedAlbum = (TextView) findViewById(R.id.selected_album);
        mAlbumsSpinner.setSelectedTextView(tvSelectedAlbum);
        mAlbumsSpinner.setPopupAnchorView(findViewById(R.id.toolbar));
        mAlbumsSpinner.setAdapter(mAlbumsAdapter);
        mAlbumCollection.onCreate(this, this);
        mAlbumCollection.onRestoreInstanceState(savedInstanceState);

        mCbSource = findViewById(R.id.cb_source);
        if (mSpec.source != null && mSpec.source.isHaveSource) {
            mCbSource.setVisibility(View.VISIBLE);
            mCbSource.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    showCheckFileSize();
                }
            });

            if (mSpec.source.isCheckSource) {
                mCbSource.setChecked(true);
            }
        }else {
            mCbSource.setVisibility(View.GONE);
        }

        if (mSpec.theme != null) {
            toolbar.setBackgroundColor(mSpec.theme.toolbarColor);
            tvSelectedAlbum.setTextColor(mSpec.theme.toolbarActionColor);
            tvCapture.setTextColor(mSpec.theme.toolbarActionColor);

            navigationIcon.setColorFilter(mSpec.theme.toolbarActionColor, PorterDuff.Mode.SRC_IN);
            Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_arrow_drop_down_white_24dp);
            drawable.setBounds(0, 0, drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight());
            drawable.setColorFilter(mSpec.theme.toolbarActionColor, PorterDuff.Mode.SRC_IN);
            tvSelectedAlbum.setCompoundDrawables(null, null, drawable, null);

            findViewById(R.id.bottom_toolbar).setBackgroundColor(mSpec.theme.bottombarBackground);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                getWindow().setStatusBarColor(mSpec.theme.statusColor);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                setStatusModeDefault(this, mSpec.theme.isWindowLightStatusBar);
            }
        }

        updateBottomToolbar();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            doNext();
        }else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, REQUEST_MEDIA_PERMISSION_CODE);
            }
        }
    }

    private void doNext() {
        int type = TypeUtil.getShowType(mSpec);

        mAlbumCollection.loadAlbums(type);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private static void setStatusModeDefault(Activity activity, boolean windowLightStatusBar) {
        View decor = activity.getWindow().getDecorView();
        int ui = decor.getSystemUiVisibility();
        if (windowLightStatusBar) {
            ui |= View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        } else {
            ui &= ~View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR;
        }
        decor.setSystemUiVisibility(ui);
    }

    private void showCheckFileSize() {
        List<Item> selectedItems = mSelectedCollection.asList();
        if (selectedItems != null && !selectedItems.isEmpty() && mCbSource.isChecked()) {
            mCbSource.setText(getString(R.string.source) + SizeUtils.getSize(selectedItems));
        }else {
            mCbSource.setText(getString(R.string.source));
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mSelectedCollection.onSaveInstanceState(outState);
        mAlbumCollection.onSaveInstanceState(outState);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mAlbumCollection.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        setResult(Activity.RESULT_CANCELED);
        super.onBackPressed();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != RESULT_OK)
            return;

        if (requestCode == REQUEST_CODE_PREVIEW) {
            Bundle resultBundle = data.getBundleExtra(BasePreviewActivity.EXTRA_RESULT_BUNDLE);
            ArrayList<Item> selected = resultBundle.getParcelableArrayList(SelectedItemCollection.STATE_SELECTION);
            boolean isCheckSource = data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_SOURCE, false);
            int collectionType = resultBundle.getInt(SelectedItemCollection.STATE_COLLECTION_TYPE,
                    SelectedItemCollection.COLLECTION_UNDEFINED);
            if (data.getBooleanExtra(BasePreviewActivity.EXTRA_RESULT_APPLY, false)) {
                Intent result = new Intent();
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selected);
                result.putExtra(EXTRA_RESULT_SOURCE, isCheckSource);
                setResult(RESULT_OK, result);
                finish();
            } else {
                mSelectedCollection.overwrite(selected, collectionType);
                Fragment mediaSelectionFragment = getSupportFragmentManager().findFragmentByTag(
                        MediaSelectionFragment.class.getSimpleName());
                if (mediaSelectionFragment instanceof MediaSelectionFragment) {
                    ((MediaSelectionFragment) mediaSelectionFragment).refreshMediaGrid();
                }

                mCbSource.setChecked(isCheckSource);

                updateBottomToolbar();
            }
        } else if (requestCode == REQUEST_CODE_CAPTURE) {
            // Just pass the data back to previous calling Activity.
            Uri contentUri = mMediaStoreCompat.getCurrentPhotoUri();

            if (contentUri != null) {
                sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, contentUri));

                Item item = new Item(Item.ITEM_ID_CAPTURE_IMAGE, contentUri, MimeType.JPEG.toString(), 0, 0);
                ArrayList<Item> selectedItems = new ArrayList<>();
                selectedItems.add(item);
                Intent result = new Intent();
                result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedItems);
                result.putExtra(EXTRA_RESULT_SOURCE, mCbSource.isChecked());
                setResult(RESULT_OK, result);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP)
                    MatisseActivity.this.revokeUriPermission(contentUri,
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                finish();
            }
        }
    }

    private void updateBottomToolbar() {
        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mButtonPreview.setEnabled(false);
            mButtonApply.setEnabled(false);
            mButtonApply.setText(getString(R.string.button_apply_default));
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            mButtonPreview.setEnabled(true);
            mButtonApply.setText(R.string.button_apply_default);
            mButtonApply.setEnabled(true);
        } else {
            mButtonPreview.setEnabled(true);
            mButtonApply.setEnabled(true);
            mButtonApply.setText(getString(R.string.button_apply, selectedCount));
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_preview) {
            Intent intent = new Intent(this, SelectedPreviewActivity.class);
            intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
            intent.putExtra(BasePreviewActivity.EXTRA_RESULT_SOURCE, mCbSource.isChecked());
            intent = mSpec.createIntent(intent);
            startActivityForResult(intent, REQUEST_CODE_PREVIEW);
        } else if (v.getId() == R.id.button_apply) {
            Intent result = new Intent();
            ArrayList<Item> selectedUris = (ArrayList<Item>) mSelectedCollection.asList();
            result.putParcelableArrayListExtra(EXTRA_RESULT_SELECTION, selectedUris);
            result.putExtra(EXTRA_RESULT_SOURCE, mCbSource.isChecked());
            setResult(RESULT_OK, result);
            finish();
        } else if (v.getId() == R.id.tv_capture) {
            capture();
        }
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        Album album = mAlbumsAdapter.getItem(position);
        mAlbumCollection.setStateCurrentSelection(album);
        onAlbumSelected(album);
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    @Override
    public void onAlbumLoad(final List<Album> data) {
        mAlbumsAdapter.setAlbums(data);
        mAlbumsAdapter.notifyDataSetChanged();

        // select default album.
        Handler handler = new Handler(Looper.getMainLooper());
        handler.post(new Runnable() {

            @Override
            public void run() {
                Album album = mAlbumCollection.getCurrentSelection();
                if (album == null) {
                    album = mAlbumsAdapter.getItem(0);
                }
                album = mAlbumsSpinner.setSelection(MatisseActivity.this, album);

                if (!isInitAlbum) {
                    onAlbumSelected(album);
                }
                isInitAlbum = true;
            }
        });
    }

    @Override
    public void onAlbumReset() {
        mAlbumsAdapter.setAlbums(null);
        mAlbumsAdapter.notifyDataSetChanged();
    }

    private void onAlbumSelected(Album album) {
        if (album.isAll() && album.isEmpty()) {
            mContainer.setVisibility(View.GONE);
            mEmptyView.setVisibility(View.VISIBLE);
        } else {
            mContainer.setVisibility(View.VISIBLE);
            mEmptyView.setVisibility(View.GONE);
            Fragment fragment = MediaSelectionFragment.newInstance(album);
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.container, fragment, MediaSelectionFragment.class.getSimpleName())
                    .commitAllowingStateLoss();
        }
    }

    @Override
    public void onUpdate() {
        // notify bottom toolbar that check state changed.
        updateBottomToolbar();

        if (mCbSource.isChecked()) {
            showCheckFileSize();
        }
    }

    @Override
    public void onMediaClick(Album album, Item item, int adapterPosition) {
        Intent intent = new Intent(this, AlbumPreviewActivity.class);
        intent.putExtra(AlbumPreviewActivity.EXTRA_ALBUM, album);
        intent.putExtra(AlbumPreviewActivity.EXTRA_ITEM, item);
        intent.putExtra(AlbumPreviewActivity.EXTRA_POSITION, adapterPosition);
        intent.putExtra(BasePreviewActivity.EXTRA_DEFAULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(BasePreviewActivity.EXTRA_RESULT_SOURCE, mCbSource.isChecked());
        mSpec.createIntent(intent);
        startActivityForResult(intent, REQUEST_CODE_PREVIEW);
    }

    public static ArrayList<Item> toPreview(int position, List<Item> items) {
        int start = Math.max(0, position - 1);
        int end = Math.min(position + 1, items.size());
        return new ArrayList<>(items.subList(start, end));
    }

    @Override
    public SelectedItemCollection provideSelectedItemCollection() {
        return mSelectedCollection;
    }

    @Override
    public void capture() {
        if (mMediaStoreCompat != null) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                mMediaStoreCompat.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE);
            }else {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA}, REQUEST_CAMERA_PERMISSION_CODE);
                }
            }

        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CAMERA_PERMISSION_CODE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                mMediaStoreCompat.dispatchCaptureIntent(this, REQUEST_CODE_CAPTURE);
            }
        } else if (requestCode == REQUEST_MEDIA_PERMISSION_CODE) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
               doNext();
            }
        }
    }
}
