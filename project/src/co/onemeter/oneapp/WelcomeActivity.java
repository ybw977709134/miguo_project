package co.onemeter.oneapp;

import java.util.ArrayList;
import java.util.List;

import co.onemeter.oneapp.adapter.WelcomeViewPagerAdapter;
import co.onemeter.oneapp.ui.LoginActivity;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;

public class WelcomeActivity extends Activity{
	private ViewPager viewPager_welcome;
	private LinearLayout layout_welcome_container;
	private int[] imgIds = null;
	private ImageView[] dots = null;
	private List<View> list = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_welcome);
		viewPager_welcome = (ViewPager) findViewById(R.id.viewPager_welcome);
		layout_welcome_container = (LinearLayout) findViewById(R.id.layout_welcome_container);

		String[] imgNames = getResources().getStringArray(
				R.array.arrWelcomeImage);
		imgIds = new int[imgNames.length];
		for (int i = 0; i < imgNames.length; i++) {
			imgIds[i] = getResources().getIdentifier(imgNames[i], "drawable",
					getPackageName());
		}

		// 初始化所有点图片
		initDots();
		list = new ArrayList<View>();
		// 通过父布局初始化所有ImageView
		// 生成ViewPager数据源List<View>，因为最后一个Pager的布局不同。则先生成最后一个pager前的所有pager
		for (int i = 0; i < imgIds.length - 1; i++) {
			ImageView imageView = new ImageView(this);
			imageView.setBackgroundResource(imgIds[i]);
			list.add(imageView);
		}
		// 将最后一个pager添加进数据源
		View view = getLayoutInflater().inflate(
				R.layout.item_viewpager_lastpager, null);
		list.add(view);
		// 给ViewPager设置适配器
		viewPager_welcome.setAdapter(new WelcomeViewPagerAdapter(list));
		// viewPager设置监听器
		viewPager_welcome.setOnPageChangeListener(new OnPageChangeListener() {
			@Override
			public void onPageSelected(int position) {
				for (int i = 0; i < dots.length; i++) {
					dots[i].setEnabled(true);
				}
				dots[position].setEnabled(false);
			}

			@Override
			public void onPageScrolled(int arg0, float arg1, int arg2) {
			}

			@Override
			public void onPageScrollStateChanged(int arg0) {
			}
		});
	}

	/**
	 * 立即体验的点击事件
	 * 当点击该按钮时，向配置文件onemeter_visit_log中写入进入应用的标记值
	 */
	public void clickButton(View view) {
		switch (view.getId()) {
		case R.id.button_item_lastpager:
			
			// 将访问记录添加进SharedPreferences文件
			SharedPreferences prefs = getSharedPreferences(
				"onemeter_visit_log", Context.MODE_PRIVATE);
			Editor editor = prefs.edit();
			editor.putBoolean("visit", true);
			editor.commit();
						
			Intent intent = new Intent(this, LoginActivity.class);
			startActivity(intent);
			
			// 关闭当前页面
			WelcomeActivity.this.finish();
			break;
		}
	}

	/**
	 * 初始化圆点对应的点击事件
	 */
	private void initDots() {
		dots = new ImageView[imgIds.length];
		for (int i = 0; i < dots.length; i++) {
			dots[i] = (ImageView) layout_welcome_container.getChildAt(i);

			// dots[i] = (ImageView) findViewById(R.id.imageView1);

			dots[i].setEnabled(true);
			dots[i].setTag(i);
			dots[i].setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					viewPager_welcome.setCurrentItem((Integer) v.getTag());
				}
			});
		}
		dots[0].setEnabled(false);
	}

}
