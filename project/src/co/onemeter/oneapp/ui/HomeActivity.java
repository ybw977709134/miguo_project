package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.liveplayer.VideoPlayingActivity;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;

import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.MessageDialog;

/**
 * Created by pzy on 9/18/14.
 */
public class HomeActivity extends Activity implements View.OnClickListener {
    MessageBox msgbox;
//    private static final int BIND_EMAIL_REQUEST_CODE = 1;

    private ViewPager viewPager_home;

    private ArrayList<View> pageviews = new ArrayList<View>();

    private int[] imgIds;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        initView();

        msgbox = new MessageBox(this);

        imgIds = new int[] {R.drawable.home_banner,R.drawable.home_banner};

        int len = imgIds.length;
        for (int i = 0;i < len; i ++){
            ImageView imageView = new ImageView(this);
            imageView.setImageResource(imgIds[i]);
            pageviews.add(imageView);
        }

        viewPager_home.setAdapter(new HeaderPagerAadapter());
        
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
                        dialog.setOnRightClickListener("以后再说", null);
                        dialog.setOnLeftClickListener("去绑定", new MessageDialog.MessageDialogClickListener() {
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

    private void initView(){
        viewPager_home = (ViewPager) findViewById(R.id.viewpager_home_headimages);

        findViewById(R.id.img_home_event).setOnClickListener(this);
        findViewById(R.id.btn_add).setOnClickListener(this);
        findViewById(R.id.img_home_growth_class).setOnClickListener(this);
        findViewById(R.id.img_home_friends).setOnClickListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        msgbox.dismissWait();
        msgbox.dismissToast();
    }

    @Override
    public void onClick(View view) {
        Intent intent = new Intent();
        switch (view.getId()) {
            case R.id.img_home_event:
                intent.setClass(this,EventActivity.class);
                break;
            case R.id.btn_add:
                intent.setClass(this,AddClassActivity.class);
            case R.id.img_home_growth_class:

                break;
            case R.id.img_home_friends:
                break;
            default:
                break;
        }
        startActivity(intent);
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