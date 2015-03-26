package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.provider.MediaStore;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.widget.AutoScrollViewPager;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.UpdatesInfo;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by pzy on 9/18/14.Modified by Jacky on 3/18/2015.
 */
public class HomeActivity extends Activity implements View.OnClickListener {

    public static final int REQ_TAKE_PHO = 1001;
    private static final int REQ_SETTINGS = 1002;

    MessageBox msgbox;
//    private static final int BIND_EMAIL_REQUEST_CODE = 1;

    private AutoScrollViewPager viewPager_home;
    private LinearLayout group;

    private ArrayList<View> pageviews = new ArrayList<View>();
    private ArrayList<ImageView> img_dots = new ArrayList<ImageView>();

    private int[] imgIds;

//    private int curpage = 0;
//
//    private android.os.Handler mHandler = new android.os.Handler(){
//        @Override
//        public void handleMessage(Message msg) {
//            if(msg.what == sLoopMessage){
//                if(curpage >= imgIds.length){
//                    curpage = 0;
//                }
//                viewPager_home.setCurrentItem(curpage);
//            }
//            mHandler.sendEmptyMessageDelayed(sLoopMessage , 5000);
//            curpage ++;
//        }
//    };

    private int mCurrentPagePosition = 1;//用于记录viewpager的当前位置
    private boolean mIsChanged = false;
    private boolean appUpdatesAvailable;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);
        checkAppUpdate();

        initView();

        msgbox = new MessageBox(this);

        imgIds = new int[] {R.drawable.home_banner1,R.drawable.home_banner2};

        final int len = imgIds.length;
        final int pagerlen = len + 2;
        for (int i = 0;i < pagerlen; i ++){
            ImageView imageView = new ImageView(this);
            if (i == 0){
                imageView.setImageResource(imgIds[len - 1]);
            }else if(i == pagerlen - 1){
                imageView.setImageResource(imgIds[0]);
            }else{
                imageView.setImageResource(imgIds[i - 1]);
            }
            pageviews.add(imageView);
        }

        initDots();

        viewPager_home.setAdapter(new HeaderPagerAadapter());

        final int first_pos = 1;
        final int last_pos = len - 1;
        viewPager_home.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int pos) {
                mIsChanged = true;
                if(pos < first_pos){
                    mCurrentPagePosition = len;
                    setCurDot(len - 1);
                }else if(pos > len){
                    mCurrentPagePosition = first_pos;
                    setCurDot(0);
                }else{
                    mCurrentPagePosition = pos;
                    setCurDot(pos - 1);
                }
            }

            @Override
            public void onPageScrollStateChanged(int status) {
                if (ViewPager.SCROLL_STATE_IDLE == status) {
                    if (mIsChanged) {
                        mIsChanged = false;
                        viewPager_home.setCurrentItem(mCurrentPagePosition, false);
                    }
                }
            }
        });
        viewPager_home.setCurrentItem(1,false);
        viewPager_home.setCycle(true);
        viewPager_home.setSlideBorderMode(AutoScrollViewPager.SLIDE_BORDER_MODE_CYCLE);
        viewPager_home.setBorderAnimation(true);
        viewPager_home.setStopScrollWhenTouch(true);
        viewPager_home.setInterval(5000);

        //登陆后跳转到此页面检测用户是否绑定了邮箱，绑定了，不提示，未绑定，弹框提示用户是否要绑定邮箱
        //如果用户未绑定邮箱，跳转到绑定邮箱界面
        checkBindEmail();
    }

    private AsyncTask<Void, Integer, List<Map<String, Object>>> asyncTask_email_status;

    private void checkBindEmail(){
        asyncTask_email_status  = new AsyncTask<Void, Integer, List<Map<String, Object>>> () {

            @Override
            protected List<Map<String, Object>> doInBackground(Void... params) {
                List<Map<String, Object>> reslut = null;
                try {
                    reslut = WowTalkWebServerIF.getInstance(HomeActivity.this).fEmailBindStatus();
                }catch (Exception e){

                }
                return reslut;
            }

            @Override
            protected void onPostExecute(List<Map<String, Object>> result) {
                //msgbox.dismissWait();
                String bindEmail = null;
                if (result != null) {
                    bindEmail = (String) result.get(0).get("email");

                    if (bindEmail == null) {
                        MessageDialog dialog = new MessageDialog(HomeActivity.this);
                        dialog.setTitle("");
                        dialog.setMessage("请绑定邮箱，用于找回密码");
                        dialog.setOnLeftClickListener("以后再说", null);
                        dialog.setOnRightClickListener("去绑定", new MessageDialog.MessageDialogClickListener() {
                            @Override
                            public void onclick(MessageDialog dialog) {
                                dialog.dismiss();
                                Intent intent = new Intent();
                                intent.setClass(HomeActivity.this, BindEmailAddressActivity.class);
                                startActivity(intent);
                            }
                        });
                        dialog.show();
                    } else {
//            			Toast.makeText(HomeActivity.this, bindEmail, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(HomeActivity.this, "请检查网络", Toast.LENGTH_SHORT).show();
                }
            }
        };
        AsyncTaskExecutor.executeShortNetworkTask(asyncTask_email_status);
    }

    private void initDots(){
        int len = imgIds.length;
        for(int i = 0;i < len;i ++){
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params =  new LinearLayout.LayoutParams(DensityUtil.dip2px(this,8),DensityUtil.dip2px(this,8));
            params.setMargins(15,0, 15, 5);
            imageView.setLayoutParams(params);
            if(i == 0){
                imageView.setImageResource(R.drawable.dot2);
            }else{
                imageView.setImageResource(R.drawable.dot1);
            }
            group.addView(imageView);
            img_dots.add(imageView);
        }
    }

    private void setCurDot(int index){
        int len = imgIds.length;
        for(int i = 0;i < len;i ++){
            if(i == index){
                img_dots.get(index).setImageResource(R.drawable.dot2);
            }else{
                img_dots.get(i).setImageResource(R.drawable.dot1);
            }
        }
    }

    private void initView(){
        viewPager_home = (AutoScrollViewPager) findViewById(R.id.viewpager_home_headimages);
        group = (LinearLayout) findViewById(R.id.lay_dots);

        findViewById(R.id.img_home_event).setOnClickListener(this);
        findViewById(R.id.btn_add).setOnClickListener(this);
        findViewById(R.id.img_home_growth_class).setOnClickListener(this);
        findViewById(R.id.img_home_friends).setOnClickListener(this);
        findViewById(R.id.img_home_classnotice).setOnClickListener(this);
        findViewById(R.id.img_home_register).setOnClickListener(this);
        findViewById(R.id.img_home_answerquestion).setOnClickListener(this);
        findViewById(R.id.img_home_homework).setOnClickListener(this);
        findViewById(R.id.img_home_chatroom).setOnClickListener(this);
        findViewById(R.id.img_home_movable).setOnClickListener(this);
        findViewById(R.id.btn_home_setting).setOnClickListener(this);
        findViewById(R.id.btn_goto_myclass).setOnClickListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        //mHandler.sendEmptyMessageDelayed(sLoopMessage , 5000);
        viewPager_home.startAutoScroll();
    }

    @Override
    public void onPause() {
        super.onPause();
        msgbox.dismissWait();
        msgbox.dismissToast();
        //mHandler.removeMessages(sLoopMessage);
        viewPager_home.stopAutoScroll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        asyncTask_email_status.cancel(true);
        asyncTask_check_update.cancel(true);
    }

    @Override
    public void onClick(View view) {
        Intent intent = null;
        switch (view.getId()) {
            case R.id.img_home_event:
                intent = new Intent(this, EventActivity.class);
                startActivity(intent);
                break;
            case R.id.btn_add:
                intent = new Intent(this,AddClassActivity.class);
                startActivity(intent);
                break;
            case R.id.img_home_growth_class:
                intent = new Intent(this,TimelineActivity.class);
                startActivity(intent);
                break;
            case R.id.img_home_friends:
                intent = new Intent(this,TimelineActivity.class);
                startActivity(intent);
                break;

            case R.id.btn_home_setting:
                intent = new Intent(this,SettingActivity.class)
                        .putExtra(SettingActivity.EXTRA_APP_UPDATES_AVAILABLE, appUpdatesAvailable);
                startActivityForResult(intent, REQ_SETTINGS);
                break;

            case R.id.img_home_classnotice:
                break;
            case R.id.img_home_register:
                break;
            case R.id.img_home_answerquestion:
                //startActivity(new Intent(this,PhotoSendToActivity.class));
                break;
            case R.id.img_home_chatroom:
                intent = new Intent(this,ParentChatroomActivity.class);
                startActivity(intent);
                break;
            case R.id.img_home_movable:
                break;
            case R.id.img_home_homework:
                takePhoto(this, REQ_TAKE_PHO);
                break;
            case R.id.btn_goto_myclass:
            	intent = new Intent(HomeActivity.this, ClassDetailActivity.class);
//                intent.putExtra(MyClassesActivity.TAG,true);
                startActivity(intent);
            	break;
            default:
                break;
        }
    }
    private Uri mLastImageUri;
    private void takePhoto(Activity activity, int requestCode) {
        mLastImageUri = Uri.fromFile(MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_IMAGE, ".jpg"));
        PackageManager pm = activity.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mLastImageUri);
            takePhotoIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.name());
            activity.startActivityForResult(takePhotoIntent, requestCode);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(resultCode == RESULT_OK){
            switch (requestCode){
                case REQ_TAKE_PHO:
                    startActivity(new Intent(this,ParentChatroomActivity.class)
                            .setData(mLastImageUri)
                            .putExtra(ParentChatroomActivity.FLAG_HOMEWORK, true));
                    break;
                case REQ_SETTINGS:
                    if (resultCode == RESULT_OK && data != null) {
                        appUpdatesAvailable = data.getBooleanExtra(
                                SettingActivity.EXTRA_APP_UPDATES_AVAILABLE, appUpdatesAvailable);
                        updateUi();
                    }
                    break;
            }
        }
    }

    private void updateUi() {
        if (appUpdatesAvailable) {
            changeNewUpdateFlagView(View.VISIBLE);
        } else {
            changeNewUpdateFlagView(View.GONE);
        }
    }


    private AsyncTask<Void, Void, Integer> asyncTask_check_update;
    /**
     * check whether the application needs to update.
     */
    private void checkAppUpdate() {
        asyncTask_check_update =  new AsyncTask<Void, Void, Integer>() {
            public UpdatesInfo updatesInfo = new UpdatesInfo();

            @Override
            protected Integer doInBackground(Void... voids) {
                try {
                    WowTalkWebServerIF mWeb = WowTalkWebServerIF.getInstance(HomeActivity.this);
                    int errno = mWeb.fCheckForUpdates(updatesInfo);
                    if (ErrorCode.OK != errno)
                        return errno;
                    if (0 == updatesInfo.versionCode)
                        return ErrorCode.BAD_RESPONSE;
                    return errno;
                }
                catch (Exception e) {
                    return ErrorCode.BAD_RESPONSE;
                }
            }

            @Override
            protected void onPostExecute(Integer errno) {
                if (ErrorCode.OK == errno) {
                    try {
                        int currVerCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
                        org.wowtalk.Log.i("checkAppUpdate when start app, the currVercode is " + currVerCode + ", the remoteVersion is " + updatesInfo.versionCode);
                        // There is new version of the app.
                        appUpdatesAvailable = currVerCode < updatesInfo.versionCode;
                        updateUi();
                    }
                    catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        AsyncTaskExecutor.executeShortNetworkTask(asyncTask_check_update);
    }

    private void changeNewUpdateFlagView(int visibility){
        findViewById(R.id.new_update_info).setVisibility(visibility);
    }

    private class HeaderPagerAadapter extends PagerAdapter{


        public HeaderPagerAadapter(){

        }

        @Override
        public int getCount() {
            return pageviews.size();
        }

        @Override
        public boolean isViewFromObject(View view, Object o) {
            return view == o;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {
            container.addView(pageviews.get(position));
            return pageviews.get(position);
        }

        @Override
        public void restoreState(Parcelable state, ClassLoader loader) {
            super.restoreState(state, loader);
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            //super.destroyItem(container, position, object);
            container.removeView((View) object);
        }
    }

    /**
     * 检测用户绑定邮箱的状态
     */
//    private void bindEmailStatus () {
//    	AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, List<Map<String, Object>>> () {
//
//            @Override
//            protected List<Map<String, Object>> doInBackground(Void... params) {
//                return WowTalkWebServerIF.getInstance(HomeActivity.this).fEmailBindStatus();
//            }
//
//            @Override
//            protected void onPostExecute(List<Map<String, Object>> result) {
//            	if (result != null) {
//            	String bindEmail = (String) result.get(0).get("email");
//            	
//            	if (bindEmail != null) {
//					msgbox.show(null, getString(R.string.bind_email_successed));
//					msgbox.dismissDialog();
//				} else {
//					msgbox.show(null, getString(R.string.bind_email_failed));
//					msgbox.dismissDialog();
//				}
//            	
//            } else {
//            	Toast.makeText(HomeActivity.this, "请检查网络", Toast.LENGTH_SHORT).show();
//            }
//            
//            } 
//        });
//    }
    
    /**
     * 
     * @author hutianfeng
     * @date 2015/3/10
     */
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//    	super.onActivityResult(requestCode, resultCode, data);
//    	if (resultCode == RESULT_OK) {
//    	}
//    }
}
