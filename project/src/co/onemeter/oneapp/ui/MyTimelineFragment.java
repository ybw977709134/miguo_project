package co.onemeter.oneapp.ui;

import android.graphics.Matrix;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.*;
import co.onemeter.oneapp.R;
import com.handmark.pulltorefresh.library.PullToRefreshBase;
import com.handmark.pulltorefresh.library.PullToRefreshListView;
import org.wowtalk.api.*;
import org.wowtalk.ui.BottomButtonBoard;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
import org.wowtalk.ui.msg.InputBoardManager;
import org.wowtalk.ui.msg.RoundedImageView;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * <p>浏览我发布的动态。</p>
 * Created by pzy on 10/13/14.
 */
public class MyTimelineFragment extends TimelineFragment implements InputBoardManager.ChangeToOtherAppsListener {
    private int originalHeaderViewsCount = 0;
    private InputBoardManager mInputMgr;
    private MediaInputHelper mMediaInput;
    private String mInputedPhotoPathForAlbumcover;
    private MomentActivity.BeginUploadAlbumCover mBeginUploadAlbumCover;
    private NetworkIFDelegate albumCoverNetworkDelegate;
    private AlbumCover mAlbumCover;
    private int establishedAlbumCoverHeight = 0;
    private int establishedAvatarRadius = 0;

    //
    // UI
    //
    private PullToRefreshListView listView;
    private View dialogBackground;
    // 相册封面（含头像）
    private View headerView_albumCover;
    // tag 过滤器
    private View headerView_tagbar;
    // 相册封面相关
    private ImageView albumCoverImageView;
    private MessageBox mMsgBox;
    private ProgressBar mProgressUploadingAlbumcover;
    private boolean isAlbumCoverUploadingInProgress;

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

        mMsgBox = new MessageBox(getActivity());
        // TODO mInputMgr
    }

    private String uid() {
        Bundle args = getArguments();
        return args != null ? args.getString("uid") : null;
    }

    @Override
    protected ArrayList<Moment> loadLocalMoments(int tag) {
        return dbHelper.fetchMomentsOfSingleBuddy(uid(), 0, 20, tag);
    }

    @Override
    protected int loadRemoteMoments() {
        WowMomentWebServerIF web = WowMomentWebServerIF.getInstance(getActivity());
        return web.fGetMomentsOfBuddy(uid(), 0, 20, true);
    }

    @Override
    public void onResume() {
        super.onResume();
        checkoutAlbumCover();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    @Override
    public void replyToMoment(int position, String momentId, Review replyTo) {

    }

    @Override
    protected void setupListHeaderView() {
        if (headerView_tagbar == null || getListView().getHeaderViewsCount() == originalHeaderViewsCount) {
            originalHeaderViewsCount = getListView().getHeaderViewsCount();
            setupListHeaderView_albumCover();
            setupListHeaderView_tagbar();
        }
    }

    private void setupListHeaderView_albumCover() {
        headerView_albumCover = LayoutInflater.from(getActivity())
                .inflate(R.layout.timeline_albumcover, null);
        getListView().addHeaderView(headerView_albumCover);

        // retrieve views
        albumCoverImageView = (ImageView) headerView_albumCover.findViewById(R.id.imgAlbumBg);
        mProgressUploadingAlbumcover = (ProgressBar)headerView_albumCover.findViewById(R.id.progress_uploading_albumcover);
        ImageView imgPtrRefreshIcon = (ImageView) headerView_albumCover.findViewById(R.id.imgRefreshRotate);
        ImageView imgThumbnail = (ImageView) headerView_albumCover.findViewById(R.id.img_thumbnail);
        TextView txtName = (TextView) headerView_albumCover.findViewById(R.id.txt_name);
        TextView txtSignature = (TextView) headerView_albumCover.findViewById(R.id.txt_signature);
        View headerBottomBg= headerView_albumCover.findViewById(R.id.box_info_bg);

        // retrieve data
        Buddy buddy = dbHelper.buddyWithUserID(uid());
        mAlbumCover = dbHelper.getAlbumCover(uid());

        // display data
        txtName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);
        if (buddy.status == null || buddy.status.equals("")) {
            txtSignature.setText("");
            headerBottomBg.setVisibility(View.GONE);
        } else {
            txtSignature.setText(buddy.status);
            headerBottomBg.setVisibility(View.VISIBLE);
        }
        if (imgThumbnail instanceof RoundedImageView) {
            setAvatarRadius((RoundedImageView)imgThumbnail);
        }
        PhotoDisplayHelper.displayPhoto(getActivity(),
                imgThumbnail, R.drawable.default_avatar_90, buddy, true);
        if (mAlbumCover != null)
            displayAlbumCover(albumCoverImageView);

        // bind event handlers
        listView.setOnPullEventListener(new OnPullEventListener(imgPtrRefreshIcon));
        albumCoverImageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMe()) {
                    if (isAlbumCoverUploadingInProgress) {
                        mMsgBox.toast(getString(R.string.moments_uploading_in_progress));
                    } else {
                        showAlbumCoverMenu();
                    }
                }
            }
        });

        // misc
        resizeAlbumCover(albumCoverImageView, imgPtrRefreshIcon);
        mBeginUploadAlbumCover = new UploadCoverListener(mProgressUploadingAlbumcover);
        albumCoverNetworkDelegate = new AlbumCoverNetworkDelegate(mProgressUploadingAlbumcover);
    }

    private void setAvatarRadius(final RoundedImageView view) {
        view.setBorderWidth(2);

        if (establishedAvatarRadius > 0) {
            view.setCornerRadius(establishedAvatarRadius);
            return;
        }

        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (view.getHeight() > 0 && establishedAvatarRadius <= 0) {
                    establishedAvatarRadius = view.getHeight() / 2;
                    view.setCornerRadius(establishedAvatarRadius);
                }
            }
        });
    }

    private boolean handleItemClick() {
        if (mInputMgr != null && mInputMgr.isShowing()) {
            mInputMgr.hide();
            return true;
        }

        return false;
    }

    private class UploadCoverListener implements MomentActivity.BeginUploadAlbumCover {
        private ProgressBar progressBar;

        public UploadCoverListener(ProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        @Override
        public void onBeginUploadCover(String filePath) {
            if (progressBar != null) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(0);
            }

            mInputedPhotoPathForAlbumcover = filePath;
        }
    };

    class AlbumCoverNetworkDelegate implements NetworkIFDelegate {
        private ProgressBar progressBar;

        public AlbumCoverNetworkDelegate(ProgressBar progressBar) {
            this.progressBar = progressBar;
        }

        @Override
        public void didFinishNetworkIFCommunication(int tag, byte[] bytes) {
            if (progressBar != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }

        @Override
        public void didFailNetworkIFCommunication(int tag, byte[] bytes) {
            if (progressBar != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        progressBar.setVisibility(View.GONE);
                    }
                });
            }
        }

        @Override
        public void setProgress(int tag, int progress) {

        }
    };

    private void showAlbumCoverMenu() {
        handleItemClick();
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(getActivity(),
                getActivity().getWindow().getDecorView());
        if (mMediaInput == null) {
            mMediaInput = new MediaInputHelper(this);
        }
        bottomBoard.add(getString(R.string.image_change_cover), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        AlbumCoverChangeActivity.launchActivity(
                                getActivity(), mMediaInput, albumCoverNetworkDelegate, mBeginUploadAlbumCover);
                    }
                });
        if (mAlbumCover.timestamp != -1 && !TextUtils.isEmpty(mAlbumCover.fileId)) {
            bottomBoard.add(getString(R.string.image_remove_cover), BottomButtonBoard.BUTTON_BLUE,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            bottomBoard.dismiss();
                            mInputedPhotoPathForAlbumcover = null;
                            new AsyncTask<Void, Void, Void>() {
                                @Override
                                protected Void doInBackground(Void... params) {
                                    removeCoverImage();
                                    return null;
                                }
                            }.execute((Void)null);
                        }
                    });
        }
        bottomBoard.addCancelBtn(getString(R.string.cancel));
        bottomBoard.show();
    }

    /**
     * 保存albumCover到SP中
     * @param albumCover
     */
    private void saveAlbumCover2SP(AlbumCover albumCover) {
        PrefUtil mPrefUtil = PrefUtil.getInstance(getActivity());
        ArrayList<Account> prefAccounts = mPrefUtil.getAccountList();
        Account account = null;
        for (Iterator<Account> iterator = prefAccounts.iterator(); iterator.hasNext();) {
            account = iterator.next();
            if (uid().equals(account.uid)) {
                if (albumCover.timestamp == -1) {
                    account.albumCoverTimeStamp = -1;
                    account.albumCoverFileId = "";
                    account.albumCoverExt = "";
                } else {
                    account.albumCoverTimeStamp = albumCover.timestamp;
                    account.albumCoverFileId = null == albumCover.fileId ? "" : albumCover.fileId;
                    account.albumCoverExt = null == albumCover.ext ? "" : albumCover.ext;
                }
                break;
            }
        }
        mPrefUtil.setAccountList(prefAccounts);
    }

    private void removeCoverImage() {
        WowTalkWebServerIF mWeb = WowTalkWebServerIF.getInstance(getActivity());
        if (ErrorCode.OK == mWeb.removeAlbumCover()) {
            mAlbumCover = new AlbumCover();
            mAlbumCover.timestamp = -1;
            dbHelper.storeAlbumCover(uid(), mAlbumCover);
            saveAlbumCover2SP(mAlbumCover);
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    displayAlbumCover(albumCoverImageView);
                }
            });
        } else {
            mMsgBox.show(null, getString(R.string.operation_failed));
        }
    }

    private boolean isMe() {
        return TextUtils.equals(uid(), PrefUtil.getInstance(getActivity()).getUid());
    }

    private void setupListHeaderView_tagbar() {
        TimelineTagbar timelineTagbar = new TimelineTagbar(getActivity());
        headerView_tagbar = timelineTagbar.getView();
        getListView().addHeaderView(headerView_tagbar);
        timelineTagbar.setListener(this);
        timelineTagbar.updateIndicatesVisibility(getMomentTag());
    }

    /**
     * 设置相册封面和下拉刷新列表的尺寸和位置。
     * 测量得到的高度保存在 {@link #establishedAlbumCoverHeight} 成员变量中。
     */
    private void resizeAlbumCover(final View bg, final View PtrIcon) {
        if (establishedAlbumCoverHeight > 0) {
            resizeAlbumCoverDirectly(bg, PtrIcon, establishedAlbumCoverHeight);
            return;
        }

        bg.getViewTreeObserver().addOnGlobalLayoutListener(
                new ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        if (bg.getWidth() > 0 && establishedAlbumCoverHeight == 0) {
                            establishedAlbumCoverHeight = (int) (bg.getWidth() * 2.0 / 3);
                            resizeAlbumCoverDirectly(bg, PtrIcon, establishedAlbumCoverHeight);
                        }
                    }
                }
        );
    }

    /**
     * 在已知合适高度的情况下，设置相册封面和下拉刷新列表的尺寸和位置。
     * @param bg
     * @param PtrIcon
     * @param height
     */
    private void resizeAlbumCoverDirectly(View bg, View PtrIcon, int height) {
        ViewGroup.LayoutParams lp = bg.getLayoutParams();
        lp.height = height;
        bg.setLayoutParams(lp);

        int headerHideHeight = (int)(height * 0.28f);
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
                    R.drawable.default_album_cover,
                    mAlbumCover.fileId, mAlbumCover.ext, GlobalSetting.S3_MOMENT_FILE_DIR, null);
        } else {
            view.setImageResource(R.drawable.default_album_cover);
        }
    }

    @Override
    public void changeToOtherApps() {

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

    /**
     * checkout album cover for updates.
     * <p>每次检查album cover的时间戳，判断是否需要下载cover
     */
    private void checkoutAlbumCover() {
        new AsyncTask<Void, Integer, Integer>() {

            AlbumCover ac = new AlbumCover();

            @Override
            protected Integer doInBackground(Void... params) {
                try {
                    WowTalkWebServerIF mWeb = WowTalkWebServerIF.getInstance(getActivity());
                    return mWeb.fGetAlbumCover(uid(), ac);
                } catch (Exception e) {
                    e.printStackTrace();
                    return ErrorCode.BAD_RESPONSE;
                }
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (ErrorCode.OK == errno) {
                    dbHelper.storeAlbumCover(uid(), ac);

                    if (mAlbumCover == null
                            || mAlbumCover.fileId == null
                            || !mAlbumCover.fileId.equals(ac.fileId)
                            || mAlbumCover.timestamp != ac.timestamp) {
                        mAlbumCover = ac;
                        // 改变SP中保存的背景图片
                        saveAlbumCover2SP(mAlbumCover);
                        displayAlbumCover(albumCoverImageView);
                    }
                }
            }
        }.execute((Void)null);
    }
}