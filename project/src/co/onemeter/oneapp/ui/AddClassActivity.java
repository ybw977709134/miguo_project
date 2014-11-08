package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;

/**
 * 添加课堂
 * Created by pzy on 11/8/14.
 */
public class AddClassActivity extends Activity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_class);

        AQuery q = new AQuery(this);
        q.find(R.id.title_back).clicked(this);
        q.find(R.id.btn_add).clicked(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
                onBackPressed();
                break;
            case R.id.btn_add:
                Toast.makeText(this, R.string.not_implemented, Toast.LENGTH_SHORT).show();
                break;
        }
    }
}