package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
import org.wowtalk.ui.bitmapfun.util.AsyncTask;
import org.wowtalk.ui.msg.RoundedImageView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jianxingdong
 * Date: 13-5-3
 * Time: PM2:17
 * To change this template use File | Settings | File Templates.
 */
public class PublicSearchActivity extends Activity {
    private class PublicAdapter extends BaseAdapter {
        private Context mContext;

        public PublicAdapter(Context context) {
            mContext = context;
        }

        @Override
        public int getCount() {
            return publicBuddies.size();
        }

        @Override
        public Object getItem(int position) {
            return publicBuddies.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final Buddy buddy = publicBuddies.get(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_public_search, parent, false);
            }
            RoundedImageView imgPhoto = (RoundedImageView) convertView.findViewById(R.id.img_photo);
            TextView txtName = (TextView) convertView.findViewById(R.id.txt_name);
            TextView txtStatus = (TextView) convertView.findViewById(R.id.txt_status);
            PhotoDisplayHelper.displayPhoto(mContext, imgPhoto, R.drawable.default_official_avatar_90, buddy, true);
            txtName.setText(buddy.nickName);
            txtStatus.setText(buddy.status);
            return convertView;
        }
    }

    private ImageButton btnTitleBack;
    private Button searchButton;
    private EditText edtSearch;
    private ListView lvPublic;
//    private Buddy buddy;
    private ImageButton fieldClear;
    private ArrayList<Buddy> publicBuddies;
    private PublicAdapter publicAdapter;

    private MessageBox mMsgBox;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_public_search);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);

        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        searchButton = (Button) findViewById(R.id.btn_search);
        edtSearch = (EditText) findViewById(R.id.edt_search);
        fieldClear = (ImageButton) findViewById(R.id.field_clear);
        lvPublic = (ListView) findViewById(R.id.list_public);
        btnTitleBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        searchButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchPublicAccount(edtSearch.getText().toString());
            }
        });
        fieldClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                edtSearch.setText("");
            }
        });
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.length() == 0) {
                    fieldClear.setVisibility(View.GONE);
                } else {
                    fieldClear.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        lvPublic.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Person person = Person.fromBuddy(publicBuddies.get(position));
                Intent intent = new Intent(PublicSearchActivity.this, PublicAccountDetailActivity.class);
                intent.putExtra(PublicAccountDetailActivity.PERSON_DETAIL, person);
                startActivity(intent);
            }
        });
    }

    private void searchPublicAccount(final String searchContent) {
        if(TextUtils.isEmpty(searchContent)) {
            mMsgBox.toast(getString(R.string.alert_search_target_empty));
            return;
        }
        mMsgBox.showWait();
//        buddy = new Buddy();
        publicBuddies = new ArrayList<Buddy>();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(PublicSearchActivity.this)
                        .fSearchBuddy(searchContent, Buddy.ACCOUNT_TYPE_PUBLIC, publicBuddies);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();

                if (result == ErrorCode.OK) {
                    if(0 == publicBuddies.size()) {
                        mMsgBox.toast(getString(R.string.alert_no_such_public_account));
                        return;
                    }

                    publicAdapter = new PublicAdapter(PublicSearchActivity.this);
                    lvPublic.setAdapter(publicAdapter);
                }
            }
        }.execute((Void)null);
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
