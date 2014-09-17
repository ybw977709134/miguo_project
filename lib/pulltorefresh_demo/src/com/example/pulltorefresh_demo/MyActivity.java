package com.example.pulltorefresh_demo;

import android.app.Activity;
import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.*;
import android.widget.*;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;

import static com.handmark.pulltorefresh.library.PullToRefreshBase.*;

public class MyActivity extends Activity implements View.OnClickListener {
    static final int ROTATION_ANIMATION_DURATION = 1200;
    private static final int HEADER_VIEW_ID = 234345;
    PullToRefreshListView mPtrSingle;
    ImageView mHeaderRotateImage;
    ImageView mHeaderRotateImageDummy;
    View mHeaderRotateImageParent;
    ImageView mHeaderBgImage;
    private float mRotationPivotX, mRotationPivotY;
    private Matrix mHeaderImageMatrix;
    private RotateAnimation mRotateAnimation;

    static final Interpolator ANIMATION_INTERPOLATOR = new LinearInterpolator();

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        mPtrSingle = (PullToRefreshListView)findViewById(R.id.ptr_list);

        View v = getLayoutInflater().inflate(R.layout.h, null);
        v.setId(HEADER_VIEW_ID);
        v.setOnClickListener(this);
        mPtrSingle.getRefreshableView().addHeaderView(v);

        mPtrSingle.getRefreshableView().setAdapter(
                new ArrayAdapter<String>(
                        this,
                        android.R.layout.simple_list_item_1,
                        new String[]{"abc", "123", "456", "abc", "123", "456", "abc", "123", "456", "abc", "123", "456", "--end--"}
                )
        );

        setupPullToRefresh();

        View btn = findViewById(R.id.button);
        if (btn != null) {
            btn.setOnClickListener(this);
        }
    }

    private void setupPullToRefresh() {
        mHeaderRotateImage = (ImageView) findViewById(R.id.imgRotate);
        mHeaderRotateImageDummy = (ImageView) findViewById(R.id.img_rotate_dummy);
        mHeaderRotateImageParent =  findViewById(R.id.box_image);

        mHeaderBgImage = (ImageView)findViewById(R.id.bg);

        // let mHeaderBgImage.height = mHeaderBgImage.width
        mHeaderBgImage.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (mHeaderBgImage.getWidth() > 0 && mHeaderBgImage.getHeight() != mHeaderBgImage.getWidth()) {
                            ViewGroup.LayoutParams lp = mHeaderBgImage.getLayoutParams();
                            lp.height = mHeaderBgImage.getWidth();
                            mHeaderBgImage.setLayoutParams(lp);

                            int hideHeight = (int)(lp.height * 0.4f);
                            mPtrSingle.setRefreshableViewMarginTop(-hideHeight);

                            RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams)mHeaderRotateImage.getLayoutParams();
                            if (rlp != null) {
                                rlp.setMargins(rlp.leftMargin,
                                        hideHeight - (int)(mHeaderRotateImage.getDrawable().getIntrinsicHeight() * 1.2f),
                                        rlp.rightMargin, rlp.bottomMargin);
                                mHeaderRotateImage.setLayoutParams(rlp);
                            }

                            mHeaderBgImage.requestLayout();

                            mPtrSingle.setHeaderPullThreshold(hideHeight / 2);

                        }
                    }
                }
        );

        mRotationPivotX = mHeaderRotateImage.getDrawable().getIntrinsicWidth() / 2;
        mRotationPivotY = mHeaderRotateImage.getDrawable().getIntrinsicHeight() / 2;

        mHeaderRotateImage.setScaleType(ImageView.ScaleType.MATRIX);
        mHeaderImageMatrix = new Matrix();
        mHeaderRotateImage.setImageMatrix(mHeaderImageMatrix);

        mRotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                0.5f);
        mRotateAnimation.setInterpolator(ANIMATION_INTERPOLATOR);
        mRotateAnimation.setDuration(ROTATION_ANIMATION_DURATION);
        mRotateAnimation.setRepeatCount(Animation.INFINITE);
        mRotateAnimation.setRepeatMode(Animation.RESTART);

        mPtrSingle.setOnPullEventListener(new OnPullEventListener<ListView>() {
            @Override
            public void onPullEvent(PullToRefreshBase<ListView> refreshView, State state, Mode direction) {
                if (state == State.REFRESHING) {
                    // header view 会向上 smooth scroll。我们希望的效果是，在此过程中 rotate image 相对于屏幕的位置保持不变，
                    // 因此我们让 rotate image 相对于 header view 做向下的平移动画，通过调节动画的幅度和时长，使其刚好抵消
                    // header view 的向上运动。
                    //
                    // 让 rotate image 一边平移一边绕自己的中心旋转有点困难(试过 AnimationSet，image 的轨迹呈螺线型），所以
                    // 让 image parent 和 image 分布进行平移和旋转。
                    //
                    // 这又产生了新的问题：平移如期发生了，旋转就没有，解决办法是让一个隐藏的 dummy image 也进行旋转。原理未知。
                    TranslateAnimation ta = new TranslateAnimation(
                            Animation.ABSOLUTE, 0,
                            Animation.ABSOLUTE, 0,
                            Animation.ABSOLUTE, 0,
                            Animation.ABSOLUTE, mPtrSingle.getHeaderPullThreshold());

                    ta.setInterpolator(ANIMATION_INTERPOLATOR);
                    ta.setDuration(PullToRefreshBase.SMOOTH_SCROLL_DURATION_MS);
                    ta.setFillEnabled(true);
                    ta.setFillAfter(true);

                    mHeaderRotateImageParent.startAnimation(ta);
                    mHeaderRotateImage.startAnimation(mRotateAnimation);
                    mHeaderRotateImageDummy.startAnimation(mRotateAnimation);
                } else {
                    mHeaderRotateImage.clearAnimation();
                    mHeaderImageMatrix.reset();
                    mHeaderRotateImage.setImageMatrix(mHeaderImageMatrix);
                }
            }

            @Override
            public void onPullScaleChanged(float scaleOfLayout) {
                rotatePtrImage(scaleOfLayout);
            }
        });

        mPtrSingle.setOnRefreshListener(new OnRefreshListener<ListView>() {
            @Override
            public void onRefresh(PullToRefreshBase<ListView> refreshView) {
                new AsyncTask<Void, Void, Void>() {
                    @Override
                    protected Void doInBackground(Void... voids) {
                        try {
                            Thread.sleep(2000);
                        } catch (Exception e) {

                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(Void v) {
                        // 撤销向下的平移动画，使 rotate image 相对于 header view 的位置还原。
                        mHeaderRotateImageParent.clearAnimation();

                        // 补一个向上的平移动画，使上面的撤销过程平滑。
                        // onRefreshComplete() 要在动画结束后才调用，以确保动画完整进行。
                        TranslateAnimation ta = new TranslateAnimation(
                                Animation.ABSOLUTE, 0,
                                Animation.ABSOLUTE, 0,
                                Animation.ABSOLUTE, mPtrSingle.getHeaderPullThreshold(),
                                Animation.ABSOLUTE, 0);
                        ta.setInterpolator(ANIMATION_INTERPOLATOR);
                        ta.setDuration(PullToRefreshBase.SMOOTH_SCROLL_DURATION_MS);
                        ta.setFillEnabled(true);
                        ta.setFillAfter(true);
                        ta.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {
                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                mPtrSingle.onRefreshComplete();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {
                            }
                        });
                        mHeaderRotateImageParent.startAnimation(ta);
                    }
                }.execute((Void) null);
            }
        });

        mPtrSingle.setShowViewWhileRefreshing(false);
    }

    private void rotatePtrImage(float scaleOfLayout) {
        float angle = scaleOfLayout * 90f;
        mHeaderImageMatrix.setRotate(angle, mRotationPivotX, mRotationPivotY);
        mHeaderRotateImage.setImageMatrix(mHeaderImageMatrix);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.button:
                Toast.makeText(MyActivity.this, "button clicked", Toast.LENGTH_SHORT).show();
                break;
            case HEADER_VIEW_ID:
                Toast.makeText(MyActivity.this, "nest view clicked", Toast.LENGTH_SHORT).show();
                break;
        }
    }
}
