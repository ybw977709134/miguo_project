package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import co.onemeter.oneapp.R;

/**
 * Created with IntelliJ IDEA.
 * User: jianxingdong
 * Date: 13-5-5
 * Time: PM5:50
 * To change this template use File | Settings | File Templates.
 */
public class ChooseEventRepeatActivity extends Activity implements View.OnClickListener{
    private ImageButton btnTitleBack;
    private ImageButton btnTitleConfirm;

    private void initView() {
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);

        btnTitleBack.setOnClickListener(this);
        btnTitleConfirm.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.event_repeat_page);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        initView();
    }
}
