package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import org.wowtalk.ui.MessageBox;

/**
 * <p>Login with invitation code.</p>
 * Created by pzy on 11/21/14.
 */
public class LoginInvitedActivity extends Activity implements View.OnClickListener {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_invite);

        AQuery q = new AQuery(this);

        q.find(R.id.btn_login).clicked(this);
        q.find(R.id.btn_login_others).clicked(this);
        q.find(R.id.btn_signup).clicked(this);
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.btn_login:
                new MessageBox(this).toast(R.string.not_implemented);
                break;
            case R.id.btn_login_others:
                startActivity(new Intent(this, LoginActivity.class));
                break;
            case R.id.btn_signup:
                startActivity(new Intent(this, RegisterActivity.class));
                break;
            default:
                new MessageBox(this).toast(R.string.not_implemented);
                break;
        }
    }
}