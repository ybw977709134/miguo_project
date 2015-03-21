package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Message;
import android.os.Parcelable;
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
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by pzy on 9/18/14.Modified by Jacky on 3/18/2015.
 */
public class HomeActivity extends Activity implements View.OnClickListener {

    private static final int sLoopMessage = 1001;

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

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initView();

        msgbox = new MessageBox(this);

        imgIds = new int[] {R.drawable.home_banner1,R.drawable.home_banner2};

        int len = imgIds.length;
        for (int i = 0;i < len; i ++){
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(imgIds[i]);
            pageviews.add(imageView);
        }

        initDots();

        viewPager_home.setAdapter(new HeaderPagerAadapter());

        viewPager_home.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int i, float v, int i2) {

            }

            @Override
            public void onPageSelected(int i) {
                setCurDot(i % pageviews.size());
            }

            @Override
            public void onPageScrollStateChanged(int i) {

            }
        });

        viewPager_home.setCycle(true);
        viewPager_home.setSlideBorderMode(AutoScrollViewPager.SLIDE_BORDER_MODE_CYCLE);
        viewPager_home.setBorderAnimation(true);
        viewPager_home.setStopScrollWhenTouch(true);
        viewPager_home.setInterval(5000);

        //登陆后跳转到此页面检测用户是否绑定了邮箱，绑定了，不提示，未绑定，弹框提示用户是否要绑定邮箱
        //如果用户未绑定邮箱，跳转到绑定邮箱界面
        
        //msgbox.showWait();

		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, List<Map<String, Object>>> () {

            @Override
            protected List<Map<String, Object>> doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(HomeActivity.this).fEmailBindStatus();
            }

            @Override
            protected void onPostExecute(List<Map<String, Object>> result) {
            	//msgbox.dismissWait();
            	String bindEmail = null;
            	if (result != null) {
            		bindEmail = (String) result.get(0).get("email");
            	
            		if (bindEmail == null) {
                        MessageDialog dialog = new MessageDialog(HomeActivity.this);

            			//Builder builder = new AlertDialog.Builder(HomeActivity.this);
                        dialog.setTitle("提示");
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
        });
    }

    private void initDots(){
        int len = imgIds.length;
        for(int i = 0;i < len;i ++){
            ImageView imageView = new ImageView(this);
            LinearLayout.LayoutParams params =  new LinearLayout.LayoutParams(20,20);
            params.setMargins(15,0, 15, 0);
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
    public void onClick(View view) {
        Intent intent = null;
        switch (view.getId()) {
            case R.id.img_home_event:
                intent = new Intent(this, EventActivity.class);
                break;
            case R.id.btn_add:
                intent = new Intent(this,AddClassActivity.class);
                break;
            case R.id.img_home_growth_class:
                intent = new Intent(this,TimelineActivity.class);
                break;
            case R.id.img_home_friends:
                intent = new Intent(this,TimelineActivity.class);
                break;

            case R.id.btn_home_setting:
                intent = new Intent(this,SettingActivity.class);
                break;

            case R.id.img_home_classnotice:
                break;
            case R.id.img_home_register:
                break;
            case R.id.img_home_answerquestion:
                break;
            case R.id.img_home_chatroom:
                break;
            case R.id.img_home_movable:
                break;
            case R.id.img_home_homework:
                break;
            case R.id.btn_goto_myclass:
            	intent = new Intent(HomeActivity.this, MyClassesActivity.class);
            	break;
            default:
                break;
        }
        if(intent != null){
            startActivity(intent);
        }
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
     * 绑定邮箱成功后返回到主页界面，显示你已经绑定的邮箱
     * @author hutianfeng
     * @date 2015/3/10
     */
//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//    	super.onActivityResult(requestCode, resultCode, data);
//    	if (resultCode == RESULT_OK) {
//			switch (requestCode) {
//			case BIND_EMAIL_REQUEST_CODE://绑定邮箱成功后的处理结果
//				bindEmailStatus ();
//				break;			
//			default:
//				msgbox.show(null, getString(R.string.bind_email_failed));
//				msgbox.dismissDialog();
//				break;
//			}
//    	}
//    }
}
