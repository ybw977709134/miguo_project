package co.onemeter.oneapp.ui;

import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.MomentAdapter;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.ArrayList;

/**
 * <p>浏览我发布的动态。</p>
 * Created by pzy on 10/13/14.
 */
public class MyTimelineFragment extends ListFragment implements MomentAdapter.ReplyDelegate {
    private Bundle args;
    private String uid;
    private Database dbHelper;
    private MomentAdapter adapter;
    private ArrayList<Moment> moments;
    private int originalHeaderViewsCount = 0;
    private boolean isPullToRefreshHeaderPullThresholdSet;

    //
    // views
    //
    private PullToRefreshListView listView;
    private View dialogBackground;
    // 相册封面（含头像）
    private View headerView_albumCover;
    // tag 过滤器
    private View headerView_tagbar;
    // 相册封面相关
    private AlbumCover mAlbumCover;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_all_timeline, container, false);
        listView = (PullToRefreshListView) view.findViewById(R.id.list);
        dialogBackground = view.findViewById(R.id.dialog_container);
        dialogBackground.setVisibility(View.GONE);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        isPullToRefreshHeaderPullThresholdSet = false; // force resize album cover

        args = getArguments();
        uid = args.getString("uid");
        if (uid == null) {
            throw new RuntimeException("uid is null");
        }

        //
        // load moments
        //
        dbHelper = new Database(getActivity());
        moments = dbHelper.fetchMomentsOfSingleBuddy(uid, 0, 20);
        ImageResizer imageResizer = new ImageResizer(getActivity(), DensityUtil.dip2px(getActivity(), 100));
        adapter = new MomentAdapter(getActivity(),
                getActivity(),
                moments,
                false,
                false,
                imageResizer,
                this,
                null,
                new MessageBox(getActivity()));
        setListAdapter(adapter);
    }

    @Override
    public void onResume() {
        super.onResume();
        setupListHeaderView();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void replyToMoment(int position, String momentId, Review replyTo) {

    }

    private void setupListHeaderView() {
        if (headerView_tagbar == null || getListView().getHeaderViewsCount() == originalHeaderViewsCount) {
            originalHeaderViewsCount = getListView().getHeaderViewsCount();
            setupListHeaderView_albumCover();
            setupListHeaderView_tagbar();
        } else {
            ImageView mHeaderBgImageView = (ImageView) headerView_albumCover.findViewById(R.id.imgAlbumBg);
            View iconView = headerView_albumCover.findViewById(R.id.imgRefreshRotate);
            resizeAlbumCoverOnLayoutDone(mHeaderBgImageView, iconView);
        }
    }

    private void setupListHeaderView_albumCover() {
        headerView_albumCover = LayoutInflater.from(getActivity())
                .inflate(R.layout.timeline_albumcover, null);
        getListView().addHeaderView(headerView_albumCover);

        // retrieve views
        ImageView mHeaderBgImageView = (ImageView) headerView_albumCover.findViewById(R.id.imgAlbumBg);
        ImageView imgThumbnail = (ImageView) headerView_albumCover.findViewById(R.id.img_thumbnail);
        TextView txtName = (TextView) headerView_albumCover.findViewById(R.id.txt_name);
        TextView txtSignature = (TextView) headerView_albumCover.findViewById(R.id.txt_signature);
        View headerBottomBg= headerView_albumCover.findViewById(R.id.box_info_bg);

        // retrieve data
        Buddy buddy = dbHelper.buddyWithUserID(uid);
        mAlbumCover = dbHelper.getAlbumCover(uid);

        // display data
        txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
        if (buddy.status == null || buddy.status.equals("")) {
            txtSignature.setText("");
            headerBottomBg.setVisibility(View.GONE);
        } else {
            txtSignature.setText(buddy.status);
            headerBottomBg.setVisibility(View.VISIBLE);
        }
        PhotoDisplayHelper.displayPhoto(getActivity(),
                imgThumbnail, R.drawable.default_avatar_90, buddy, true);

        resizeAlbumCover(mHeaderBgImageView, headerView_albumCover.findViewById(R.id.imgRefreshRotate));
//        if (mAlbumCover != null)
//            displayAlbumCover();

        listView.setOnPullEventListener(new OnPullEventListener(
                (ImageView) headerView_albumCover.findViewById(R.id.imgRefreshRotate)));
    }

    private void setupListHeaderView_tagbar() {
        headerView_tagbar = LayoutInflater.from(getActivity())
                .inflate(R.layout.timeline_tag_tabbar, null);
        getListView().addHeaderView(headerView_tagbar);
//            AQuery q = new AQuery(headerView_tagbar);
//            TimelineFilterOnClickListener clickListener = new TimelineFilterOnClickListener(
//                    dialogBackground,
//                    headerView_tagbar,
//                    headerView_tagbar.findViewById(R.id.btn_sender),
//                    headerView_tagbar.findViewById(R.id.btn_cat)
//            );
//            clickListener.setOnFilterChangedListener(onMomentSenderChangedListener);
//            q.find(R.id.btn_sender).clicked(clickListener);
//            q.find(R.id.btn_cat).clicked(clickListener);
    }

    private void resizeAlbumCover(final View bg, final View PtrIcon) {
        bg.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (bg.getWidth() > 0/* && !isPullToRefreshHeaderPullThresholdSet TODO*/) {
                            isPullToRefreshHeaderPullThresholdSet=true;
                            resizeAlbumCoverOnLayoutDone(bg, PtrIcon);
                        }
                    }
                }
        );
    }

    private void resizeAlbumCoverOnLayoutDone(View bg, View PtrIcon) {
        ViewGroup.LayoutParams lp = bg.getLayoutParams();
        lp.height = (int)(bg.getWidth()*2.0/3);
        bg.setLayoutParams(lp);

        int headerHideHeight = (int)(lp.height * 0.28f);
        listView.setRefreshableViewMarginTop(-headerHideHeight);

        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) PtrIcon.getLayoutParams();
        if (rlp != null) {
            hidePtrIcon(PtrIcon);
        }
        bg.requestLayout();

        listView.setHeaderPullThreshold(0);
    }

    private void hidePtrIcon(View view) {
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) view.getLayoutParams();
        if (rlp != null) {
            int avaliableHeight= measurePTRIconHeight();
            rlp.setMargins(avaliableHeight/4,
                    avaliableHeight/2,
                    avaliableHeight/4, rlp.bottomMargin);
            view.setLayoutParams(rlp);
        }
    }

    private void showPtrIcon(View view) {
        RelativeLayout.LayoutParams rlp = (RelativeLayout.LayoutParams) view.getLayoutParams();
        if (rlp != null) {
            int avaliableHeight= measurePTRIconHeight();
            rlp.setMargins(avaliableHeight/4,
                    (int)(avaliableHeight*2+avaliableHeight/4),
                    avaliableHeight/4, rlp.bottomMargin);
            view.setLayoutParams(rlp);
        }
    }

    private int measurePTRIconHeight() {
        if(0 == PtrIconHeightCache) {
            ImageView aImageView=new ImageView(getActivity());
            aImageView.setImageResource(R.drawable.refresh);
            aImageView.measure(0,0);
            PtrIconHeightCache =aImageView.getMeasuredHeight();
        }
        return PtrIconHeightCache;
    }
    private int PtrIconHeightCache = 0;

    private void displayAlbumCover(ImageView view) {
        if (mAlbumCover != null && mAlbumCover.timestamp != -1 && mAlbumCover.fileId != null) {
            PhotoDisplayHelper.displayPhoto(getActivity(), view,
                    R.drawable.moment_default_album,
                    mAlbumCover.fileId, mAlbumCover.ext, GlobalSetting.S3_MOMENT_FILE_DIR, null);
        } else {
            view.setImageResource(R.drawable.moment_default_album);
        }
    }

    private class OnPullEventListener implements PullToRefreshBase.OnPullEventListener<ListView> {
        private static final int ROTATION_ANIMATION_DURATION = 1200;
        private Matrix mHeaderImageMatrix;
        private ImageView iconView;
        private RotateAnimation mRotateAnimation;
        private float mRotationPivotX, mRotationPivotY;
        private boolean isPulling;

        public OnPullEventListener(ImageView iconView) {
            this.iconView = iconView;

            mRotateAnimation = new RotateAnimation(0, 720, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF,
                    0.5f);
            mRotateAnimation.setInterpolator(new LinearInterpolator());
            mRotateAnimation.setDuration(ROTATION_ANIMATION_DURATION);
            mRotateAnimation.setRepeatCount(Animation.INFINITE);
            mRotateAnimation.setRepeatMode(Animation.RESTART);

            mHeaderImageMatrix = new Matrix();

            mRotationPivotX = iconView.getDrawable().getIntrinsicWidth() / 2;
            mRotationPivotY = iconView.getDrawable().getIntrinsicHeight() / 2;
        }

        @Override
        public void onPullEvent(PullToRefreshBase<ListView> refreshView,
                                PullToRefreshBase.State state,
                                PullToRefreshBase.Mode direction) {
            if (state == PullToRefreshBase.State.REFRESHING) {
                // header view 会向上 smooth scroll。我们希望的效果是，在此过程中 rotate image 相对于屏幕的位置保持不变，
                // 因此我们让 rotate image 相对于 header view 做向下的平移动画，通过调节动画的幅度和时长，使其刚好抵消
                // header view 的向上运动。
                //
                // 让 rotate image 一边平移一边绕自己的中心旋转有点困难(试过 AnimationSet，image 的轨迹呈螺线型），所以
                // 让 image parent 和 image 分布进行平移和旋转。
                //
                // 这又产生了新的问题：平移如期发生了，旋转就没有，解决办法是让一个隐藏的 dummy image 也进行旋转。原理未知。
                showPtrIcon(iconView);
                iconView.startAnimation(mRotateAnimation);
            } else {
                if (state == PullToRefreshBase.State.RESET) {
                    hidePtrIcon(iconView);
                }
                iconView.clearAnimation();
                mHeaderImageMatrix.reset();
                iconView.setImageMatrix(mHeaderImageMatrix);
            }

            if(state == PullToRefreshBase.State.PULL_TO_REFRESH ||
                    state == PullToRefreshBase.State.RELEASE_TO_REFRESH) {
                isPulling=true;
            } else {
                isPulling=false;
            }
            Log.e("state: "+state);
            //updateHandler.sendEmptyMessage(MSG_ID_UPDATE_HEADER_BUTTONS);
        }

        @Override
        public void onPullScaleChanged(float scaleOfLayout) {
            rotatePtrImage(scaleOfLayout);
        }

        private void rotatePtrImage(float scaleOfLayout) {
            float angle = scaleOfLayout * 90f;
            mHeaderImageMatrix.setRotate(angle, mRotationPivotX, mRotationPivotY);
            iconView.setImageMatrix(mHeaderImageMatrix);
        }
    }
}