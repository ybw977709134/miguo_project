package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.*;
import co.onemeter.oneapp.contacts.util.ContactUtil;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.adapter.ContactListAdapter;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 3/29/13
 * Time: 3:59 PM
 * To change this template use File | Settings | File Templates.
 */
public class PublicAccountActivity extends Activity implements View.OnClickListener {

    private static final int REQ_ADD = 123;
    private ListView lvPublicAccount;
    private EditText edtSearch;

    private ContactListAdapter publicAccountAdapter;

    private TextWatcher textWatcher = new TextWatcher() {

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
            if (s.length() == 0) {

            } else {

            }
            //To change body of implemented methods use File | Settings | File Templates.
        }

        @Override
        public void afterTextChanged(Editable s) {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    };

    private void initView() {
        AQuery q = new AQuery(this);

        lvPublicAccount = (ListView) findViewById(R.id.public_account_list);
        edtSearch = (EditText) findViewById(R.id.edt_search);
        edtSearch.addTextChangedListener(textWatcher);

        q.find(R.id.navbar_btn_left).clicked(this);
        q.find(R.id.navbar_btn_right).clicked(this);

        lvPublicAccount.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                //To change body of implemented methods use File | Settings | File Templates.
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.navbar_btn_left:
                finish();
                break;
            case R.id.navbar_btn_right:
                startActivityForResult(
                        new Intent(this, PublicSearchActivity.class),
                        REQ_ADD
                );
                break;
            default:
                break;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.public_account);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        initView();
        publicAccountAdapter = new ContactListAdapter(this,
                ContactUtil.fFetchPublicAccountsAsPerson(this));
        lvPublicAccount.setAdapter(publicAccountAdapter);
        lvPublicAccount.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                startActivity(new Intent(PublicAccountActivity.this, PublicAccountDetailActivity.class)
                        .putExtra(PublicAccountDetailActivity.PERSON_DETAIL,
                                publicAccountAdapter.getItem(position)));
            }
        });
        ListHeightUtil.setListHeight(lvPublicAccount);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

}
