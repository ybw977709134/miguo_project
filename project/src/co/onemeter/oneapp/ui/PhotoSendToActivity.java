package co.onemeter.oneapp.ui;

import android.graphics.Matrix;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.DisplayMetrics;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

import co.onemeter.oneapp.R;

/**
 * Created by jacky on 15-3-26.
 */
public class PhotoSendToActivity extends FragmentActivity implements View.OnClickListener{

    private final int IDX_0 = 0;
    private final int IDX_1 = 1;
    private final int IDX_2 = 2;

    private ImageView img_cursor;
    private TextView txt_one,txt_two,txt_third;
    private ViewPager viewPager;

    private ArrayList<Fragment> mFragList;

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

        mFragList = new ArrayList<Fragment>();
        mFragList.add(new Fragment());
        mFragList.add(new Fragment());
        mFragList.add(new Fragment());
        viewPager.setAdapter(new SendtoPagerAdapter(getSupportFragmentManager(),mFragList));
        viewPager.setCurrentItem(IDX_0);
    }

    private void setImageLeftMargin(int left_margin){
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(mImgWidth,mImgHeight);
        params.setMargins(left_margin, 0, 0, 0);
        img_cursor.setLayoutParams(params);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()){
            case R.id.txt_cancel:
                finish();
                break;
            case R.id.txt_one:
                viewPager.setCurrentItem(IDX_0);
                break;
            case R.id.txt_two:
                viewPager.setCurrentItem(IDX_1);
                break;
            case R.id.txt_third:
                viewPager.setCurrentItem(IDX_2);
                break;

        }
    }

    private void setTextView(int index){
        switch (index){
            case IDX_0:
                txt_one.setTextColor(color_blue);
                txt_two.setTextColor(color_black);
                txt_third.setTextColor(color_black);
                break;
            case IDX_1:
                txt_one.setTextColor(color_black);
                txt_two.setTextColor(color_blue);
                txt_third.setTextColor(color_black);
                break;
            case IDX_2:
                txt_one.setTextColor(color_black);
                txt_two.setTextColor(color_black);
                txt_third.setTextColor(color_blue);
                break;
        }
    }

    private boolean mIsChanging = false;
    private int mcurpagePosition = 0;

    private class MyOnPageChangeListener implements ViewPager.OnPageChangeListener {
        //int one = mOffset + mImgWidth;//两个相邻页面的偏移量

        @Override
        public void onPageScrolled(int position, float percent, int pix) {
            if(mIsChanging){
                setImageLeftMargin(mImgWidth + (int)(percent * mPerLayWidth));
            }
        }

        @Override
        public void onPageScrollStateChanged(int status) {
            if (ViewPager.SCROLL_STATE_DRAGGING == status) {
                mIsChanging = true;
            }
        }

        @Override
        public void onPageSelected(int position) {
            mIsChanging = false;
            mcurpagePosition = position;
            setTextView(position);
            setImageLeftMargin(mPerLayWidth * position + (mPerLayWidth - mImgWidth) / 2);
//            Animation animation = new TranslateAnimation(mCurrIndex * one, postion * one, 0, 0);//平移动画
//            mCurrIndex = postion;
//            animation.setFillAfter(true);//动画终止时停留在最后一帧，不然会回到没有执行前的状态
//            animation.setDuration(400);
//            img_cursor.startAnimation(animation);//是用ImageView来显示动画的
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

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view == object;
        }

    }
}
