package co.onemeter.oneapp.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.ui.BottomButtonBoard;

/**
 * <p>联系人页面。</p>
 * Created by pzy on 11/22/14.
 */
public class ContactsActivity extends FragmentActivity implements View.OnClickListener {

    /** optional moment owner's uid */
    public static final String EXTRA_UID = "uid";
    /** If page title is empty, buttons will show instead. */
    public static final String EXTRA_PAGE_TITLE = "title";

    private static final int TAB_CONTACTS = 0;
    private static final int TAB_SCHOOL = 1;

    private static ContactsActivity instance;

    private ContactsFragment contactsFragment;
    private SchoolMatesFragment schoolMatesFragment;
    private AQuery q = new AQuery(this);
    private String uid;
    private String pageTitle;
    private int activatedTab;
    private BottomButtonBoard bottomBoard;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);
        instance = this;

        q.find(R.id.btn_contacts).clicked(this);
        q.find(R.id.btn_school).clicked(this);
        q.find(R.id.title_bar).clicked(this);
        q.find(R.id.navbar_btn_right).clicked(this);

        getData(savedInstanceState == null ? getIntent().getExtras() : savedInstanceState);

        contactsFragment =  new ContactsFragment();
        schoolMatesFragment = new SchoolMatesFragment();
        Bundle args = new Bundle();
        args.putString(MyTimelineFragment.EXTRA_UID,
                uid != null ? uid : PrefUtil.getInstance(this).getUid());
        schoolMatesFragment.setArguments(args);

        if (uid != null) {
            switchToSchool();
        } else {
            switchToContacts();
        }

        setTitle(pageTitle);
        initMenu();
    }

    @Override
    public void setTitle(CharSequence title) {
        super.setTitle(title);
        if (title != null) {
            q.find(R.id.btn_layout).invisible();
            q.find(R.id.title_text).visible().text(title);
        } else {
            q.find(R.id.btn_layout).visible();
            q.find(R.id.title_text).invisible();
        }
    }

    public static ContactsActivity instance() {
        return instance;
    }

    private void getData(Bundle state) {
        if (state != null) {
            uid = state.getString(EXTRA_UID);
            pageTitle = state.getString(EXTRA_PAGE_TITLE);
        }
    }

    public boolean handleBackPress() {
        if (contactsFragment.handleBackPress() || schoolMatesFragment.handleBackPress()) {
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_bar:
                if (activatedTab == TAB_CONTACTS) {
                    contactsFragment.gotoTop();
                } else if (activatedTab == TAB_SCHOOL) {
                    schoolMatesFragment.gotoTop();
                }
                break;
            case R.id.btn_contacts:
                switchToContacts();
                break;
            case R.id.btn_school:
                switchToSchool();
                break;
            case R.id.navbar_btn_right:
                if (activatedTab == TAB_CONTACTS) {
                    contactsFragment.handleBackPress();
                }
                bottomBoard.show();
                break;
        }
    }

    private void switchToContacts() {
        q.find(R.id.btn_contacts).background(R.drawable.tab_button_left_white_a).textColorId(R.color.blue);
        q.find(R.id.btn_school).background(R.drawable.tab_button_right_white).textColorId(R.color.white);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, contactsFragment)
                .commit();
        activatedTab = TAB_CONTACTS;
    }

    private void switchToSchool() {
        q.find(R.id.btn_contacts).background(R.drawable.tab_button_left_white).textColorId(R.color.white);
        q.find(R.id.btn_school).background(R.drawable.tab_button_right_white_a).textColorId(R.color.blue);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, schoolMatesFragment)
                .commit();
        activatedTab = TAB_SCHOOL;
    }

    private void initMenu() {
        bottomBoard = new BottomButtonBoard(this,
                findViewById(R.id.navbar_btn_right));
        bottomBoard.add(getString(R.string.friends_add),
                BottomButtonBoard.BUTTON_BLUE, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent addIntent = new Intent(
                                ContactsActivity.this,
                                ContactAddActivity.class);
                        startActivity(addIntent);
                        bottomBoard.dismiss();
                    }
                });
        bottomBoard.add(getString(R.string.refresh_from_server),
                BottomButtonBoard.BUTTON_BLUE, new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        bottomBoard.dismiss();
                        if (activatedTab == TAB_CONTACTS) {
                            contactsFragment.refresh();
                        } else if (activatedTab == TAB_SCHOOL) {
                           schoolMatesFragment.refresh();
                        }
                    }
                });
        bottomBoard.addCancelBtn(getString(R.string.close));

    }
}

