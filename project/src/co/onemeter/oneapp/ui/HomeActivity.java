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
import com.pzy.paint.DoodleActivity;
import org.wowtalk.api.*;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.InputBoardManager;

import java.io.File;
import java.io.FileOutputStream;
import java.util.*;

/**
 * Created by pzy on 9/18/14.Modified by Jacky on 3/18/2015.
 */
public class HomeActivity extends Activity implements View.OnClickListener {

    private static final int REQ_TAKE_PHO = 1001;
    private static final int REQ_SETTINGS = 1002;
    private static final int REQ_TAKE_PHO_DOODLE = 1003;
    private static final int REQ_SEND_DOODLE = 1004;
    private static final int REQ_PICK_PHOTO_DOOLE = 1005;
    private static final int REQ_HOME = 1006;
    private static final int REQ_ADD_CLASS = 1007;

    MessageBox msgbox;
//    private static final int BIND_EMAIL_REQUEST_CODE = 1;

    private AutoScrollViewPager viewPager_home;
    private LinearLayout group;

    private ArrayList<View> pageviews = new ArrayList<View>();
    private ArrayList<ImageView> img_dots = new ArrayList<ImageView>();

    private int[] imgIds;

    private int mCurrentPagePosition = 1;//用于记录viewpager的当前位置
    private boolean mIsChanged = false;
    private boolean appUpdatesAvailable;
    private List<GroupChatRoom> schools;
    private boolean isRequestingMyClasses = false;
    private boolean isWaitingForMyClasses = false;

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


        if (PrefUtil.getInstance(HomeActivity.this).getMyPhoneNumber().length() == 0) {

            MessageDialog dialog = new MessageDialog(HomeActivity.this,false,MessageDialog.SIZE_NORMAL);
            dialog.setTitle("");
            dialog.setMessage("请绑定手机号，用于找回密码和登录");
            dialog.setCancelable(false);
            dialog.setRightBold(true);
            dialog.setOnLeftClickListener("去绑定", new MessageDialog.MessageDialogClickListener() {
                @Override
                public void onclick(MessageDialog dialog) {
                    dialog.dismiss();
                    Intent intent = new Intent(HomeActivity.this, BindCellPhoneActivity.class);
                    startActivity(intent);
                }
            });
            dialog.show();


        }


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
        schools = new Database(HomeActivity.this).fetchSchools();
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
        findViewById(R.id.layout_home_setting).setOnClickListener(this);

        findViewById(R.id.btn_goto_myclass).setOnClickListener(this);
        
        findViewById(R.id.txt_home_classnotice).setOnClickListener(this);
        findViewById(R.id.txt_home_register).setOnClickListener(this);
        findViewById(R.id.txt_goto_myclass).setOnClickListener(this);
        findViewById(R.id.txt_home_answerquestion).setOnClickListener(this);
        findViewById(R.id.txt_home_homework).setOnClickListener(this);
        findViewById(R.id.txt_home_chatroom).setOnClickListener(this);
        
    }
    @Override
    protected void onResume() {
        super.onResume();
        viewPager_home.startAutoScroll();
    }

    @Override
    public void onPause() {
        super.onPause();
        msgbox.dismissWait();
        msgbox.dismissToast();
        viewPager_home.stopAutoScroll();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
                startActivityForResult(intent, REQ_HOME);
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
            case R.id.layout_home_setting:
                intent = new Intent(this,SettingActivity.class)
                        .putExtra(SettingActivity.EXTRA_APP_UPDATES_AVAILABLE, appUpdatesAvailable);
                startActivityForResult(intent, REQ_SETTINGS);
                break;

            case R.id.img_home_classnotice://班级通知
            case R.id.txt_home_classnotice:
            	intent = new Intent(this, ClassNotificationActivity.class);
                startActivity(intent);
                break;
            case R.id.img_home_register://签到请假
            case R.id.txt_home_register:
            	//学生请假
            	if (PrefUtil.getInstance(this).getMyAccountType() == Buddy.ACCOUNT_TYPE_STUDENT) {
            		intent = new Intent(this, StudentAbsenceActivity.class);
            	} 
            	//老师签到
            	if (PrefUtil.getInstance(this).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {
            		intent = new Intent(this, TeacherSignActivity.class);
            	}
                startActivity(intent);
                break;
            case R.id.img_home_answerquestion:
            case R.id.txt_home_answerquestion:
                //startActivity(new Intent(this,PhotoSendToActivity.class));
                //takePhoto(this,REQ_TAKE_PHO_DOODLE);
                showTakeOrPickphoto(view);
                break;
            case R.id.img_home_chatroom:
            case R.id.txt_home_chatroom:
                intent = new Intent(this,ParentChatroomActivity.class);
                startActivity(intent);
                break;
            case R.id.img_home_movable:
                break;
            case R.id.img_home_homework://在线作业
            case R.id.txt_home_homework:
//                MessageDialog dialog = new MessageDialog(this);
//                dialog.setIsDouleBtn(false);
//                dialog.setTitle("");
//                dialog.setMessage("请拍下作业,发送给老师");
//                dialog.setOnLeftClickListener("确定",new MessageDialog.MessageDialogClickListener() {
//                    @Override
//                    public void onclick(MessageDialog dialog) {
//                        dialog.dismiss();
//                        takePhoto(HomeActivity.this, REQ_TAKE_PHO);
//                    }
//                });
//                dialog.show();
            	intent = new Intent(this, TeacherSignActivity.class);
            	intent.putExtra("homework", "homework");
            	startActivity(intent);

                break;
            case R.id.btn_goto_myclass:
            case R.id.txt_goto_myclass:
                if (isRequestingMyClasses) {
                    msgbox.showWait();
                    isWaitingForMyClasses = true;
                } else {
                    gotoMyClasses();
                }
            	break;
            default:
                break;
        }
    }

    private void gotoMyClasses() {
//        Intent intent;
//        if (isEmpty()) {
//            intent = new Intent(HomeActivity.this, AddClassActivity.class);
//        } else {
//            intent = new Intent(HomeActivity.this, MyClassesActivity.class);
//            intent.putExtra(MyClassesActivity.TAG, true);
//
//        }
//        startActivityForResult(intent, REQ_ADD_CLASS);
    	Intent intent = new Intent(HomeActivity.this, MyClassesActivity.class);
//    	intent.putExtra(MyClassesActivity.TAG, true);
    	startActivityForResult(intent, REQ_ADD_CLASS);
    }

    private void showTakeOrPickphoto(View v){
        final BottomButtonBoard board = new BottomButtonBoard(this,v);
        board.add(getString(R.string.image_take_photo),BottomButtonBoard.BUTTON_BLUE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                board.dismiss();
                takePhoto(HomeActivity.this,REQ_TAKE_PHO_DOODLE);
            }
        });
        board.add(getString(R.string.image_provider_cover),BottomButtonBoard.BUTTON_BLUE, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                board.dismiss();
                pickPhoto(HomeActivity.this,REQ_PICK_PHOTO_DOOLE);
            }
        });
        board.addCancelBtn(getString(R.string.cancel));
        board.show();
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

    private void pickPhoto(Activity activity, int requestCode){
        Intent pickIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
//        pickIntent.setType("image/*");
//        pickIntent.setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(pickIntent, requestCode);
    }

    private String outputfilename;

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
                case REQ_TAKE_PHO_DOODLE:
                    String[] photoPath = new String[2];
                    boolean isSuccess = new MediaInputHelper().handleImageResult(
                            this,
                            new Intent().setData(mLastImageUri),
                            InputBoardManager.PHOTO_SEND_WIDTH, InputBoardManager.PHOTO_SEND_HEIGHT,
                            InputBoardManager.PHOTO_THUMBNAIL_WIDTH, InputBoardManager.PHOTO_THUMBNAIL_HEIGHT,
                            photoPath);
                    if(isSuccess){
                        outputfilename = Database.makeLocalFilePath(UUID.randomUUID().toString(), "jpg");
                        startActivityForResult(
                                new Intent(this, DoodleActivity.class)
                                        .putExtra(DoodleActivity.EXTRA_MAX_WIDTH, InputBoardManager.PHOTO_SEND_WIDTH)
                                        .putExtra(DoodleActivity.EXTRA_MAX_HEIGHT, InputBoardManager.PHOTO_SEND_HEIGHT)
                                        .putExtra(DoodleActivity.EXTRA_BACKGROUND_FILENAME, photoPath[0])
                                        .putExtra(DoodleActivity.EXTRA_OUTPUT_FILENAME, outputfilename),
                                REQ_SEND_DOODLE
                        );
                    }

                    break;
                case REQ_PICK_PHOTO_DOOLE:
                    photoPath = new String[2];
                    isSuccess = new MediaInputHelper().handleImageResult(
                            this,
                            data,
                            InputBoardManager.PHOTO_SEND_WIDTH, InputBoardManager.PHOTO_SEND_HEIGHT,
                            InputBoardManager.PHOTO_THUMBNAIL_WIDTH, InputBoardManager.PHOTO_THUMBNAIL_HEIGHT,
                            photoPath);
                    if(isSuccess){
                        outputfilename = Database.makeLocalFilePath(UUID.randomUUID().toString(), "jpg");
                        startActivityForResult(
                                new Intent(this, DoodleActivity.class)
                                        .putExtra(DoodleActivity.EXTRA_MAX_WIDTH, InputBoardManager.PHOTO_SEND_WIDTH)
                                        .putExtra(DoodleActivity.EXTRA_MAX_HEIGHT, InputBoardManager.PHOTO_SEND_HEIGHT)
                                        .putExtra(DoodleActivity.EXTRA_BACKGROUND_FILENAME, photoPath[0])
                                        .putExtra(DoodleActivity.EXTRA_OUTPUT_FILENAME, outputfilename),
                                REQ_SEND_DOODLE
                        );
                    }
                    break;
                case REQ_SEND_DOODLE:
                    photoPath = new String[2];
                    photoPath[0] = outputfilename;
                    // generate thumbnail
                    File f = MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_THUMNAIL, ".jpg");
                    if (f != null) {
                        photoPath[1] = f.getAbsolutePath();
                        Bitmap thumbnail = BmpUtils.decodeFile(photoPath[0],InputBoardManager.PHOTO_THUMBNAIL_WIDTH, InputBoardManager.PHOTO_THUMBNAIL_HEIGHT);
                        try {
                            FileOutputStream fos = new FileOutputStream(photoPath[1]);
                            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                            fos.close();
                        } catch (java.io.IOException e) {
                            e.printStackTrace();
                        }
                    }
                    startActivity(new Intent(this,SendToActivity.class).putExtra(SendToActivity.INTENT_PAHT,photoPath));
                    break;
                case REQ_HOME:
                	refresh();
                	break;
                case REQ_ADD_CLASS:
                	refresh();
                	break;
            }
        }
    }

    //刷新组织架构
    private void refresh() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... voids) {
                isRequestingMyClasses = true;
                List<GroupChatRoom> result = new LinkedList<>();
                int errno = WowTalkWebServerIF.getInstance(HomeActivity.this).getMySchoolsErrno(true, result);
                if (errno == ErrorCode.OK) {
                    schools.clear();
                    schools.addAll(result);
                }
                isRequestingMyClasses = false;
            	return errno;
            }
            @Override
            public void onPostExecute(Integer errno) {
                if (errno == ErrorCode.OK) {
                    if (msgbox.isWaitShowing())
                        msgbox.dismissWait();
                    new Database(HomeActivity.this).storeSchools(schools);
                    if (isWaitingForMyClasses) {
                        gotoMyClasses();
                    }
                } else {
                    if (msgbox.isWaitShowing()) {
                        msgbox.dismissWait();
                        msgbox.toast(R.string.operation_failed);
                    }
                }
                isWaitingForMyClasses = false;
            }
        });
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
}
