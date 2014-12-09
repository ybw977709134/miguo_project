package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import co.onemeter.oneapp.R;

import com.androidquery.AQuery;

/**
 * 我的课堂页面。
 * Created by pzy on 11/10/14.
 */
public class MyClassesActivity extends Activity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_myclasses);

        AQuery q = new AQuery(this);

        q.find(R.id.title_back).clicked(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
                onBackPressed();
                break;
        }
    }
}