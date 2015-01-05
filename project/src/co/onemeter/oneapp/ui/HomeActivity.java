package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.liveplayer.VideoPlayingActivity;

import com.androidquery.AQuery;

import org.wowtalk.ui.MessageBox;

/**
 * Created by pzy on 9/18/14.
 */
public class HomeActivity extends Activity implements View.OnClickListener {
    MessageBox msgbox;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        msgbox = new MessageBox(this);

        AQuery q = new AQuery(this);
        q.find(R.id.btn_event).clicked(this);
        q.find(R.id.vg_event).clicked(this);
        q.find(R.id.btn_study).clicked(this);
        q.find(R.id.vg_study).clicked(this);
        q.find(R.id.btn_bonus).clicked(this);
        q.find(R.id.vg_bonus).clicked(this);
        q.find(R.id.btn_grow).clicked(this);
        q.find(R.id.vg_grow).clicked(this);
        q.find(R.id.btn_add).clicked(this);
        q.find(R.id.btn_live_play).clicked(this);
        q.find(R.id.btn_funnyevent).clicked(this);
        q.find(R.id.btn_myclasses).clicked(this);
        q.find(R.id.live_play).clicked(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        msgbox.dismissWait();
        msgbox.dismissToast();
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_event:
            case R.id.vg_event:
                startActivity(new Intent(this, EventActivity.class));
                break;
            case R.id.btn_add:
                startActivity(new Intent(this, AddClassActivity.class));
                break;
            case R.id.btn_funnyevent:
                startActivity(new Intent(this, EventActivity.class));
                break;
            case R.id.btn_myclasses:
                startActivity(new Intent(this, MyClassesActivity.class));
                break;
            case R.id.btn_live_play:
            	startActivity(new Intent(this, VideoPlayingActivity.class));
            	break;
            default:
                msgbox.toast(R.string.not_implemented);
                break;
        }
    }
}