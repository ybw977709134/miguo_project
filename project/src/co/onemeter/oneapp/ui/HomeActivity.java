package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;

/**
 * Created by pzy on 9/18/14.
 */
public class HomeActivity extends Activity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        AQuery q = new AQuery(this);
        q.find(R.id.btn_event).clicked(this);
        q.find(R.id.vg_event).clicked(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_event:
            case R.id.vg_event:
                startActivity(new Intent(this, EventActivity.class));
                break;
        }
    }
}