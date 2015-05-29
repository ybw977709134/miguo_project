package co.onemeter.oneapp.ui.widget;

import android.app.Activity;
import android.os.Bundle;

import org.wowtalk.api.HomeWorkResult;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.List;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.adapter.HomeWorkAdapter;
import co.onemeter.oneapp.ui.DensityUtil;

/**
 * Created by hutianfeng on 2015/5/29
 */
public class SubmitHomeWorkActivity extends Activity {

    private HomeWorkAdapter adapter;
    private List<HomeWorkResult> stuResultList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_submit_home_work);

        ImageResizer imageResizer = new ImageResizer(this, DensityUtil.dip2px(this, 100));

        adapter = new HomeWorkAdapter(this,stuResultList,imageResizer);

    }

}
