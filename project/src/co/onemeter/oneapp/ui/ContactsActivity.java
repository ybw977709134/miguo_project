package co.onemeter.oneapp.ui;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import org.wowtalk.api.PrefUtil;

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
    }

    @Override
    public void onStart() {
        super.onStart();
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
        updateMenu();
    }

    private void switchToSchool() {
        q.find(R.id.btn_contacts).background(R.drawable.tab_button_left_white).textColorId(R.color.white);
        q.find(R.id.btn_school).background(R.drawable.tab_button_right_white_a).textColorId(R.color.blue);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, schoolMatesFragment)
                .commit();
        activatedTab = TAB_SCHOOL;
        updateMenu();
    }

    private void updateMenu() {
        int[] icons = null;
        Fragment f = currFragment();

        if (f instanceof BottomButtonBoard.OptionsMenuProvider) {
            final BottomButtonBoard.OptionsMenuProvider provider = ((BottomButtonBoard.OptionsMenuProvider) f);
            icons = provider.getOptionsMenuItemIcons(this);
            if (icons.length > 0) {
                ViewGroup btnBox_left = ((ViewGroup)q.find(R.id.navbar_btn_left).getView());
                btnBox_left.removeAllViews();
                ViewGroup btnBox_right = ((ViewGroup)q.find(R.id.navbar_btn_right).getView());
                btnBox_right.removeAllViews();
                for (int i = 0; i < icons.length; ++i) {
                    ImageView btn = new ImageView(this);
                    btn.setImageResource(icons[i]);
                    btn.setBackgroundResource(0);
                    final int position = i;
                    btn.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            provider.onOptionsItemSelected(position);
                        }
                    });
                    //因为只有两个菜单按钮，所以我做了简单的判断
                    if(i == 0){
                    	btnBox_left.addView(btn);
                    }else{
                    	btnBox_right.addView(btn);
                    }
                }
            }
        }

        if (icons != null && icons.length > 0) {
            q.find(R.id.navbar_btn_right).visible();
        } else {
            q.find(R.id.navbar_btn_right).invisible();
        }
    }

    private Fragment currFragment() {
        if (activatedTab == TAB_CONTACTS) {
            return contactsFragment;
        } else if (activatedTab == TAB_SCHOOL) {
            return schoolMatesFragment;
        }
        return null;
    }

}

