package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
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
        
        //登陆后跳转到此页面检测用户是否绑定了邮箱，绑定了，不提示，未绑定，弹框提示用户是否要绑定邮箱
        //如果用户未绑定邮箱，跳转到绑定邮箱界面
        Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("你还未绑定邮箱");
        builder.setMessage("绑定邮箱有助于你找回密码，你需要绑定邮箱吗?");
        builder.setPositiveButton("以后再说", null);
        builder.setNegativeButton("去绑定邮箱", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface arg0, int arg1) {
				Intent intent = new Intent();
				intent.setClass(HomeActivity.this, BindEmailActivity.class);
				startActivity(intent);
				
			}
		});
        builder.create().show();
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
            case R.id.live_play:
            case R.id.btn_live_play:
            	startActivity(new Intent(this, VideoPlayingActivity.class));
            	break;
            default:
                msgbox.toast(R.string.not_implemented);
                break;
        }
    }
}