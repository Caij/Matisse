package com.zhihu.matisse.internal.ui.widget;

import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.PointF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.FrameLayout;

import com.davemorrissey.labs.subscaleview.ImageSource;
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView;
import com.zhihu.matisse.R;

/**
 * Created by Ca1j on 2017/1/18.
 */

public class BigImageView extends FrameLayout implements SubsamplingScaleImageView.OnImageEventListener {

    public static final int INIT_SCALE_TYPE_CENTER_INSIDE = 1;
    public static final int INIT_SCALE_TYPE_CENTER_CROP = 2;
    public static final int INIT_SCALE_TYPE_TOP_CROP = 4;

    private int mScaleType;
    private SubsamplingScaleImageView mSubsamplingScaleImageView;
    private SubsamplingScaleImageView.OnImageEventListener onImageEventListener;

    private boolean isRecycle;

    public BigImageView(Context context, AttributeSet attr) {
        super(context, attr);
        init(context);
    }

    public BigImageView(Context context) {
        this(context, null);
    }

    private void init(Context context) {
        inflate(context, R.layout.view_big_image, this);
        mSubsamplingScaleImageView = (SubsamplingScaleImageView) findViewById(R.id.ssiv);
        mSubsamplingScaleImageView.setOrientation(SubsamplingScaleImageView.ORIENTATION_USE_EXIF);
        mSubsamplingScaleImageView.setMinimumScaleType(SubsamplingScaleImageView.SCALE_TYPE_CUSTOM);
        mSubsamplingScaleImageView.setOnImageEventListener(this);

        setInitScaleType(INIT_SCALE_TYPE_CENTER_INSIDE);
    }

    public void setInitScaleType(int initScaleType) {
        mScaleType = initScaleType;
        invalidate();
    }

    @Override
    public void onReady() {
        if (mScaleType == INIT_SCALE_TYPE_TOP_CROP) {
            float scale = getWidth() * 1f / mSubsamplingScaleImageView.getSWidth();
            mSubsamplingScaleImageView.setScaleAndCenter(scale, new PointF(mSubsamplingScaleImageView.getSWidth() / 2, 0));
        }else if (mScaleType == INIT_SCALE_TYPE_CENTER_CROP) {
            float scale = Math.max(getWidth() * 1f / mSubsamplingScaleImageView.getSWidth(),  getHeight() * 1f / mSubsamplingScaleImageView.getSHeight());
            mSubsamplingScaleImageView.setScaleAndCenter(scale, new PointF(mSubsamplingScaleImageView.getSWidth() / 2, mSubsamplingScaleImageView.getSHeight() / 2));
        }else if (mScaleType == INIT_SCALE_TYPE_CENTER_INSIDE) {
            float scale = Math.min(getWidth() * 1f / mSubsamplingScaleImageView.getSWidth(),  getHeight() * 1f / mSubsamplingScaleImageView.getSHeight());
            mSubsamplingScaleImageView.setScaleAndCenter(scale, new PointF(mSubsamplingScaleImageView.getSWidth() / 2, mSubsamplingScaleImageView.getSHeight() / 2));
        }

        float scale = getWidth() * 1f / mSubsamplingScaleImageView.getSWidth();
        mSubsamplingScaleImageView.setMaxScale(Math.max(1, scale) * 2F);
    }

    @Override
    public void onImageLoaded() {
        if (onImageEventListener != null) onImageEventListener.onImageLoaded();
        if (isRecycle) {
            mSubsamplingScaleImageView.recycle();
        }
    }

    @Override
    public void onPreviewLoadError(Exception e) {
        if (onImageEventListener != null) onImageEventListener.onPreviewLoadError(e);
    }

    @Override
    public void onImageLoadError(Exception e) {
        if (onImageEventListener != null) onImageEventListener.onImageLoadError(e);
    }

    @Override
    public void onTileLoadError(Exception e) {
        if (onImageEventListener != null) onImageEventListener.onTileLoadError(e);
    }

    @Override
    public void onPreviewReleased() {
        if (onImageEventListener != null) onImageEventListener.onPreviewReleased();
    }

    public void setOnImageEventListener(SubsamplingScaleImageView.OnImageEventListener onImageEventListener) {
        this.onImageEventListener = onImageEventListener;
    }

    public void setDebug(boolean debug) {
        mSubsamplingScaleImageView.setDebug(debug);
    }

    public void setImage(String uri) {
        mSubsamplingScaleImageView.setImage(ImageSource.uri(uri));
    }

    public void setImage(Bitmap bitmap, int orientation) {
        mSubsamplingScaleImageView.setOrientation(orientation);
        mSubsamplingScaleImageView.setImage(ImageSource.bitmap(bitmap));
    }

    public void recycle() {
        isRecycle = true;
        mSubsamplingScaleImageView.recycle();
        mSubsamplingScaleImageView.setOnImageEventListener(null);
    }

    @Override
    public void setOnClickListener(final OnClickListener l) {
        mSubsamplingScaleImageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                l.onClick(BigImageView.this);
            }
        });
    }

    @Override
    public void setOnLongClickListener(final OnLongClickListener l) {
        mSubsamplingScaleImageView.setOnLongClickListener(new OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                l.onLongClick(BigImageView.this);
                return true;
            }
        });
    }

}
