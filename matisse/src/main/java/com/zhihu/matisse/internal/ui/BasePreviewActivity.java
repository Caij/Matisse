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

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.TextView;

import com.zhihu.matisse.R;
import com.zhihu.matisse.internal.entity.IncapableCause;
import com.zhihu.matisse.internal.entity.Item;
import com.zhihu.matisse.internal.entity.SelectionSpec;
import com.zhihu.matisse.internal.model.SelectedItemCollection;
import com.zhihu.matisse.internal.ui.adapter.PreviewPagerAdapter;
import com.zhihu.matisse.internal.ui.widget.CheckView;
import com.zhihu.matisse.internal.utils.Platform;
import com.zhihu.matisse.internal.utils.SizeUtils;

import java.util.List;

public abstract class BasePreviewActivity extends AppCompatActivity implements View.OnClickListener,
        ViewPager.OnPageChangeListener {

    public static final String EXTRA_DEFAULT_BUNDLE = "extra_default_bundle";
    public static final String EXTRA_RESULT_BUNDLE = "extra_result_bundle";
    public static final String EXTRA_RESULT_APPLY = "extra_result_apply";
    public static final String EXTRA_RESULT_SOURCE = "extra_result_source";

    protected SelectedItemCollection mSelectedCollection;
    protected SelectionSpec mSpec;
    protected ViewPager mPager;

    protected PreviewPagerAdapter mAdapter;

    protected CheckView mCheckView;
    protected TextView mButtonBack;
    protected TextView mButtonApply;
    private AppCompatCheckBox mCbSource;

    protected int mPreviousPos = -1;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        mSpec = SelectionSpec.createSelectionSpec(getIntent());
        mSelectedCollection = new SelectedItemCollection(this, mSpec);
        if (mSpec.theme != null) {
            setTheme(mSpec.theme.themeId);
        }else {
            setTheme(R.style.Matisse_Zhihu);
        }
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_media_preview);
        if (Platform.hasKitKat()) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        }

        if (mSpec.needOrientationRestriction()) {
            setRequestedOrientation(mSpec.orientation);
        }

        mCbSource = findViewById(R.id.cb_source);
        if (mSpec.source != null && mSpec.source.isHaveSource) {
            mCbSource.setVisibility(View.VISIBLE);
            mCbSource.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    showCheckFileSize();
                }
            });
        }else {
            mCbSource.setVisibility(View.GONE);
        }

        if (savedInstanceState == null) {
            mSelectedCollection.onCreate(getIntent().getBundleExtra(EXTRA_DEFAULT_BUNDLE));
        } else {
            mSelectedCollection.onCreate(savedInstanceState);
        }

        mButtonBack = (TextView) findViewById(R.id.button_back);
        mButtonApply = (TextView) findViewById(R.id.button_apply);
        mButtonBack.setOnClickListener(this);
        mButtonApply.setOnClickListener(this);

        mPager = (ViewPager) findViewById(R.id.pager);
        mPager.addOnPageChangeListener(this);
        mAdapter = new PreviewPagerAdapter(getSupportFragmentManager(), null);
        mPager.setAdapter(mAdapter);
        mCheckView = (CheckView) findViewById(R.id.check_view);
        mCheckView.setCountable(mSpec.countable);

        mCheckView.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Item item = mAdapter.getMediaItem(mPager.getCurrentItem());
                if (mSelectedCollection.isSelected(item)) {
                    mSelectedCollection.remove(item);
                    if (mSpec.countable) {
                        mCheckView.setCheckedNum(CheckView.UNCHECKED);
                    } else {
                        mCheckView.setChecked(false);
                    }
                } else {
                    if (assertAddSelection(item)) {
                        mSelectedCollection.add(item);
                        if (mSpec.countable) {
                            mCheckView.setCheckedNum(mSelectedCollection.checkedNumOf(item));
                        } else {
                            mCheckView.setChecked(true);
                        }
                    }
                }
                updateApplyButton();

                if (mCbSource.isChecked()) {
                    showCheckFileSize();
                }
            }
        });
        updateApplyButton();

        boolean isCheck = getIntent().getBooleanExtra(EXTRA_RESULT_SOURCE, false);
        mCbSource.setChecked(isCheck);
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
        mSelectedCollection.onSaveInstanceState(outState);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        sendBackResult(false);
        super.onBackPressed();
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_back) {
            onBackPressed();
        } else if (v.getId() == R.id.button_apply) {
            sendBackResult(true);
            finish();
        }
    }

    @Override
    public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {

    }

    @Override
    public void onPageSelected(int position) {
        PreviewPagerAdapter adapter = (PreviewPagerAdapter) mPager.getAdapter();
        if (mPreviousPos != -1 && mPreviousPos != position) {
            ((PreviewItemFragment) adapter.instantiateItem(mPager, mPreviousPos)).resetView();

            Item item = adapter.getMediaItem(position);
            if (mSpec.countable) {
                int checkedNum = mSelectedCollection.checkedNumOf(item);
                mCheckView.setCheckedNum(checkedNum);
                if (checkedNum > 0) {
                    mCheckView.setEnabled(true);
                } else {
                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            } else {
                boolean checked = mSelectedCollection.isSelected(item);
                mCheckView.setChecked(checked);
                if (checked) {
                    mCheckView.setEnabled(true);
                } else {
                    mCheckView.setEnabled(!mSelectedCollection.maxSelectableReached());
                }
            }
        }
        mPreviousPos = position;
    }

    @Override
    public void onPageScrollStateChanged(int state) {

    }

    private void updateApplyButton() {
        int selectedCount = mSelectedCollection.count();
        if (selectedCount == 0) {
            mButtonApply.setText(R.string.button_apply_default);
            mButtonApply.setEnabled(false);
        } else if (selectedCount == 1 && mSpec.singleSelectionModeEnabled()) {
            mButtonApply.setText(R.string.button_apply_default);
            mButtonApply.setEnabled(true);
        } else {
            mButtonApply.setEnabled(true);
            mButtonApply.setText(getString(R.string.button_apply, selectedCount));
        }
    }

    protected void sendBackResult(boolean apply) {
        Intent intent = new Intent();
        intent.putExtra(EXTRA_RESULT_BUNDLE, mSelectedCollection.getDataWithBundle());
        intent.putExtra(EXTRA_RESULT_APPLY, apply);
        intent.putExtra(EXTRA_RESULT_SOURCE, mCbSource.isChecked());
        setResult(Activity.RESULT_OK, intent);
    }

    private boolean assertAddSelection(Item item) {
        IncapableCause cause = mSelectedCollection.isAcceptable(item);
        IncapableCause.handleCause(this, cause);
        return cause == null;
    }
}
