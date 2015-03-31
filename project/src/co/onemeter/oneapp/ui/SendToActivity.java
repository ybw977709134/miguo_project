package co.onemeter.oneapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import co.onemeter.oneapp.R;

/**
 * This activity is used to choose buddy or group or schoolmate's buddy,and send picture
 * Created by jacky on 15-3-26.
 */
public class SendToActivity extends FragmentActivity implements View.OnClickListener{
    public static final String INTENT_PAHT = "pho_path";


    private final int IDX_0 = 0;
    private final int IDX_1 = 1;
    private final int IDX_2 = 2;

    private ImageView img_cursor;
    private TextView txt_one,txt_two,txt_third;
    private ViewPager viewPager;

    private ArrayList<Fragment> mfragments;

    private int mPerLayWidth = 0;
    private int mImgWidth = 0;
    private int mImgHeight = 0;
    //private int mCurrIndex = 0;

    private int color_blue;
    private int color_black;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_photo_send_to);

        color_blue = getResources().getColor(R.color.blue);
        color_black = getResources().getColor(R.color.black_24);

        setupView();

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                System.gc();
            }
        }).start();
    }

    private void setImageView(){
        img_cursor = (ImageView) findViewById(R.id.img_cursor);
        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenW = dm.widthPixels;
        mPerLayWidth = screenW / 3;

        int w = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        int h = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        img_cursor.measure(w, h);

        mImgHeight = img_cursor.getMeasuredHeight();
        mImgWidth = mPerLayWidth / 2;

        setImageLeftMargin((mPerLayWidth - mImgWidth) / 2);

        //Matrix matrix = new Matrix();
        //matrix.postTranslate(mOffset, 0);
        //img_cursor.setImageMatrix(matrix);
    }

    private void setupView(){
        findViewById(R.id.txt_cancel).setOnClickListener(this);
        txt_one = (TextView) findViewById(R.id.txt_one);
        txt_two = (TextView) findViewById(R.id.txt_two);
        txt_third = (TextView) findViewById(R.id.txt_third);

        txt_one.setOnClickListener(this);
        txt_two.setOnClickListener(this);
        txt_third.setOnClickListener(this);

        setImageView();
        setupViewpager();
    }

    private void setupViewpager(){
        viewPager = (ViewPager) findViewById(R.id.viewpager_sendto);
        viewPager.setOnPageChangeListener(new MyOnPageChangeListener());

        mfragments = new ArrayList<Fragment>();
        Bundle bundle = new Bundle();
        bundle.putStringArray(INTENT_PAHT,getIntent().getStringArrayExtra(INTENT_PAHT));
        Fragment fragment = null;
        fragment = new STSmsFragment();
        fragment.setArguments(bundle);
        mfragments.add(fragment);
        fragment = new STContactsFragment();
        fragment.setArguments(bundle);
        mfragments.add(fragment);
        fragment = new STSchoolMateFragment();
        fragment.setArguments(bundle);
        mfragments.add(fragment);
        viewPager.setAdapter(new SendtoPagerAdapter(getSupportFragmentManager(), mfragments));
        viewPager.setCurrentItem(IDX_0);
    }

    private void setImageLeftMargin(int left_margin){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mImgWidth,mImgHeight);
        params.leftMargin = left_margin;
        img_cursor.setLayoutParams(params);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.txt_cancel:
                finish();
                break;
            case R.id.txt_one:
                viewPager.setCurrentItem(IDX_0,true);
                break;
            case R.id.txt_two:
                viewPager.setCurrentItem(IDX_1,true);
                break;
            case R.id.txt_third:
                viewPager.setCurrentItem(IDX_2,true);
                break;

        }
    }

    private void setTextView(int index){
        txt_one.setTextColor(color_black);
        txt_two.setTextColor(color_black);
        txt_third.setTextColor(color_black);
        switch (index){
            case IDX_0:
                txt_one.setTextColor(color_blue);
                break;
            case IDX_1:
                txt_two.setTextColor(color_blue);
                break;
            case IDX_2:
                txt_third.setTextColor(color_blue);
                break;
        }
    }

//    private boolean mScrolling = false;
      private int currentIndex = 0;
//    private int mlastmargin = (mPerLayWidth - mImgWidth) / 2;

    private class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {

        @Override
        public void onPageScrolled(int position, float offset, int pix) {
//            if(mScrolling){
//                setImageLeftMargin(mlastmargin + (int)(mPerLayWidth * offset));
//            }

        }

        @Override
        public void onPageScrollStateChanged(int status) {
//            if (ViewPager.SCROLL_STATE_DRAGGING == status) {
//                mScrolling = true;
//            }else if (ViewPager.SCROLL_STATE_SETTLING == status){
//                mScrolling = false;
//                //setImageLeftMargin(mlastmargin);
//            }else if (ViewPager.SCROLL_STATE_IDLE == status){
//                mScrolling = false;
//            }else {
//                mScrolling = false;
//            }
        }

        @Override
        public void onPageSelected(int position) {
            //mIsChanging = false;
            //currentIndex = position;
            setTextView(position);
//            setImageLeftMargin((mPerLayWidth - mImgWidth) / 2 + mPerLayWidth * position);
//            mlastmargin = (mPerLayWidth - mImgWidth) / 2 + mPerLayWidth * position;
            Animation animation = new TranslateAnimation(currentIndex * mPerLayWidth, position * mPerLayWidth, 0, 0);//平移动画
            animation.setFillAfter(true);//动画终止时停留在最后一帧，不然会回到没有执行前的状态
            animation.setDuration(400);
            img_cursor.startAnimation(animation);//是用ImageView来显示动画的

            currentIndex = position;
        }
    }

    private class SendtoPagerAdapter extends FragmentPagerAdapter{

        ArrayList<Fragment> list;

        public SendtoPagerAdapter(FragmentManager fm,ArrayList<Fragment> list) {
            super(fm);
            this.list = list;
        }

        @Override
        public Fragment getItem(int position) {
            return list.get(position);
        }

        @Override
        public int getCount() {
            if(!list.isEmpty() && list != null){
                return list.size();
            }
            return 0;
        }

    }
}
