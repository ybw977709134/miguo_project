package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.contacts.util.ContactUtil;
import co.onemeter.oneapp.ui.SideBar.OnTouchingLetterChangedListener;

import java.util.*;

public class LocalContactsActivity extends Activity implements OnClickListener, OnTouchingLetterChangedListener{
	private class LocalContactAdapter extends BaseAdapter {

		private ArrayList<Person> persons;
		private Context context;
		private LayoutInflater inflater;
		public LocalContactAdapter(Context context) {
			persons = ContactUtil.localPersons;
			this.context = context;
			inflater = LayoutInflater.from(context);
		}
		
		@Override
		public int getCount() {
			return persons.size();
		}

		@Override
		public Object getItem(int position) {
			return persons.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}
		
		public void setPersonSource(ArrayList<Person> persons) {
			this.persons = persons;
		}
		
		public ArrayList<Person> getPersonSource() {
			return this.persons;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			View lView = null;
			char newFirstChar;
			char oldFirstChar;
			if (position == 0) {
				oldFirstChar = 0;
			} else {
				oldFirstChar = persons.get(position - 1).getSortKey().toUpperCase(Locale.getDefault()).charAt(0);
			}
			final Person person = persons.get(position);
			Log.i("sort key : " + person.getSortKey().toUpperCase(Locale.getDefault()));
			if (convertView == null) {
				lView = inflater.inflate(R.layout.listitem_local_contacts, null);
			} else {
				lView = convertView;
			}

			ImageView cBox = (ImageView) lView.findViewById(R.id.cBox);
            TextView btnAddAsFriend=(TextView) lView.findViewById(R.id.tv_add_as_friend);
            TextView btnInvite=(TextView) lView.findViewById(R.id.tv_invite);
            btnAddAsFriend.setVisibility(View.GONE);
            btnInvite.setVisibility(View.GONE);

			if (_isInSelectMode) {
				cBox.setVisibility(View.VISIBLE);
				if (person.isSelected()) {
					cBox.setImageResource(R.drawable.list_selected);
				} else {
					cBox.setImageResource(R.drawable.list_unselected);
				}
			} else {
				cBox.setVisibility(View.GONE);

                if(ContactInfoActivity.BUDDY_TYPE_NOT_USER == person.buddyType) {
                    btnAddAsFriend.setVisibility(View.GONE);
                    btnInvite.setVisibility(View.VISIBLE);
                } else if (ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND == person.buddyType) {
                    btnAddAsFriend.setVisibility(View.VISIBLE);
                    btnInvite.setVisibility(View.GONE);
                }
			}

            btnAddAsFriend.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    addPersonAsFriend(person);
                }
            });
            btnInvite.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    ContactInfoActivity.fSendSmsToInvite(LocalContactsActivity.this,person);
                }
            });

			TextView txtName = (TextView) lView.findViewById(R.id.local_contact_name);
			txtName.setText(person.getName());
			TextView txtFirstChar = (TextView) lView.findViewById(R.id.first_char);
			ImageView dividerView = (ImageView) lView.findViewById(R.id.divider_view);
			newFirstChar = person.getSortKey().toUpperCase(Locale.getDefault()).charAt(0);
			if (newFirstChar < 'A')
				newFirstChar = '#';
			if (newFirstChar > oldFirstChar) {
				txtFirstChar.setVisibility(View.VISIBLE);
				dividerView.setVisibility(View.GONE);
				txtFirstChar.setText(String.valueOf(newFirstChar).toUpperCase(Locale.getDefault()));
			} else {
				txtFirstChar.setVisibility(View.GONE);
				dividerView.setVisibility(View.VISIBLE);
			}
			return lView;
		}
		
	}

    private void addPersonAsFriend(final Person person) {
        mMsgBox.showWait();

        final Buddy buddy=person.toBuddy();

        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int errno = WowTalkWebServerIF.getInstance(LocalContactsActivity.this).fAddBuddy(buddy.userID);
                return errno;
            }
            @Override
            protected void onPostExecute(Integer s) {
                mMsgBox.dismissWait();
                if(s == ErrorCode.OK) {
                    person.buddyType=ContactInfoActivity.BUDDY_TYPE_IS_FRIEND;

                    new Database(LocalContactsActivity.this).storeNewBuddyWithUpdate(buddy);

                    personAdapter.notifyDataSetChanged();
                } else if (s == ErrorCode.ERR_OPERATION_DENIED) {
                    mMsgBox.show(null, getString(R.string.contactinfo_add_friend_denied));
                }else {
                    mMsgBox.show(null, getString(R.string.operation_failed));
                }
            }
        }.execute((Void)null);
    }
	
	TextWatcher textWatcher = new TextWatcher() {
		
		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
			if (s.length() == 0) {
                sideBar.setVisibility(View.VISIBLE);
                fieldClear.setVisibility(View.GONE);
				personAdapter.setPersonSource(ContactUtil.localPersons);
				personAdapter.notifyDataSetChanged();
			} else {
                sideBar.setVisibility(View.GONE);
                fieldClear.setVisibility(View.VISIBLE);
				ContactUtil.fFetchPersonsFromLocalByCondition(s.toString(), true);
				personAdapter.setPersonSource(ContactUtil.curPersons);
				personAdapter.notifyDataSetChanged();
			}
            ListHeightUtil.setListHeight(lvLocalContacts);
		}
		
		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			
		}
		
		@Override
		public void afterTextChanged(Editable s) {
			
		}
	};

    private static final String EXTRA_MULTI_SELECT_MODE = "ms";
    public static final String EXTRA_SELECTION = "se";

	private ImageButton btnTitleBack;
	private ImageButton btnTitleGroup;
	private TextView txtTitleAll;
	private TextView txtTitleDone;
	
	private EditText edtSearchContent;
    private ImageView fieldClear;

    private ScrollView mMainScrollView;
    private LinearLayout mMainLinearLayout;
	private ListView lvLocalContacts;
	private LinearLayout mButtonLayout;
	private Button btnSms;
	private Button btnEmail;
	
	private SideBar sideBar;
	
	private LocalContactAdapter personAdapter;

    private boolean mIsInitForMultiSelect = false;
	private boolean _isInSelectMode = false;
	private boolean _isButtonLayoutShown = false;

    private MessageBox mMsgBox;
	
	private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnTitleGroup = (ImageButton) findViewById(R.id.title_groupsms);
		txtTitleAll = (TextView) findViewById(R.id.select_all);
		txtTitleDone = (TextView) findViewById(R.id.select_done);
		
		edtSearchContent = (EditText) findViewById(R.id.edt_search);
        fieldClear = (ImageView) findViewById(R.id.field_clear);
        mMainScrollView = (ScrollView) findViewById(R.id.main_scroll_view);
        mMainLinearLayout = (LinearLayout) findViewById(R.id.main_linear_layout);
		lvLocalContacts = (ListView) findViewById(R.id.local_contacts_list);
		btnSms = (Button) findViewById(R.id.sms_group);
		btnEmail = (Button) findViewById(R.id.email_group);
		mButtonLayout = (LinearLayout) findViewById(R.id.layout_button);
		sideBar = (SideBar) findViewById(R.id.side_bar);
		
		btnTitleBack.setOnClickListener(this);
		btnTitleGroup.setOnClickListener(this);
        fieldClear.setOnClickListener(this);
		txtTitleAll.setOnClickListener(this);
		txtTitleDone.setOnClickListener(this);
		btnSms.setOnClickListener(this);
		btnEmail.setOnClickListener(this);
		sideBar.setOnTouchingLetterChangedListener(this);
		edtSearchContent.addTextChangedListener(textWatcher);

		lvLocalContacts.setOnItemClickListener(new OnItemClickListener() {

			@Override
			public void onItemClick(AdapterView<?> parent, View v, int position,
					long id) {
                Log.i("local contacts item clicked");
				if (_isInSelectMode) {
					Person person = personAdapter.getPersonSource().get(position);
					person.setSelected(!person.isSelected());
					personAdapter.notifyDataSetChanged();
					fSetBottomIsEnabled();
				} else {
				    // get the buddy type of the phone_number
				    gotoContactInfoActivity((Person) ((LocalContactAdapter)lvLocalContacts.getAdapter()).getItem(position));
				}
			}
		});

        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                ContactUtil.fFetchAllLocalPerson(LocalContactsActivity.this);
                return 0;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                personAdapter = new LocalContactAdapter(LocalContactsActivity.this);
                lvLocalContacts.setAdapter(personAdapter);
                ListHeightUtil.setListHeight(lvLocalContacts);

                if(mIsInitForMultiSelect = getIntent().getBooleanExtra(EXTRA_MULTI_SELECT_MODE, false)) {
                    toggleSelectMode();
                }

                updatePersonBuddyType();
            }
        }.execute((Void)null);

	}

    protected void gotoContactInfoActivity(final Person person) {
        if(ContactInfoActivity.BUDDY_TYPE_UNKNOWN != person.buddyType) {
            ContactInfoActivity.launch(LocalContactsActivity.this, person, person.buddyType);
        }


//        if (null == mMsgBox) {
//            mMsgBox = new MessageBox(LocalContactsActivity.this);
//        }
//        mMsgBox.showWait();
//        new AsyncTask<Void, Void, Integer>() {
//            private List<Buddy> buddies = new ArrayList<Buddy>();
//            private String phoneNumber;
//            @Override
//            protected Integer doInBackground(Void... params) {
//                phoneNumber = person.getGlobalPhoneNumber();
//                int result = -1;
//                if (!TextUtils.isEmpty(phoneNumber)) {
//                    phoneNumber = phoneNumber.replace("-", "").replace(" ", "");
//                    result = WowTalkWebServerIF.getInstance(LocalContactsActivity.this).fGetBuddyWithPhoneNumber(phoneNumber, buddies);
//                }
//                return result;
//            }
//
//            @Override
//            protected void onPostExecute(Integer result) {
//                super.onPostExecute(result);
//                mMsgBox.dismissWait();
//                Person targetPerson = person;
//                if (result == 0) {
//                    int buddyType = ContactInfoActivity.BUDDY_TYPE_NOT_USER;
//                    Log.i("There are " + buddies.size() + " persons associated with the phoneNumber " + phoneNumber);
//                    if (!buddies.isEmpty()) {
//                        Buddy buddy = buddies.get(0);
//                        boolean isFriend = (0 != (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)
//                                || buddy.userID.equals(WowTalkWebServerIF.getInstance(LocalContactsActivity.this).fGetMyUserIDFromLocal()));
//                        buddyType = isFriend ? ContactInfoActivity.BUDDY_TYPE_IS_FRIEND : ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND;
//                        targetPerson = Person.fromBuddy(buddy);
//                    }
//                    ContactInfoActivity.launch(LocalContactsActivity.this, targetPerson, buddyType);
//                }
//            }
//        }.execute((Void)null);
    }

    private final static int MSG_ID_REFRESH_LOCAL_CONTACT_LIST=1;
    private final static long LOCAL_CONTACT_LIST_REFRESH_INTERVAL=200;
    private Handler msgHandler= new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch(msg.what) {
                case MSG_ID_REFRESH_LOCAL_CONTACT_LIST:
                    sortLocalContact();
                    personAdapter.notifyDataSetChanged();
                    ListHeightUtil.setListHeight(lvLocalContacts);
                    break;
            }
        }
    };

    private void sortLocalContact() {
        Collections.sort(ContactUtil.localPersons, new ComparatorForPerson());
    }

    private class ComparatorForPerson implements Comparator<Person> {
        @Override
        public int compare(Person p0, Person p1) {
            return p0.getSortKey().compareTo(p1.getSortKey());
        }
    }

    private int numOfPersonUpdated;
    private void updatePersonBuddyType() {
        if(_isInSelectMode) {
            mMsgBox.dismissWait();
            return;
        }

        numOfPersonUpdated=0;
        for(Person aPerson : ContactUtil.localPersons) {

            new AsyncTask<Person, Integer, Integer>() {
                private Person originalPerson;
                private Buddy  resultBuddy=null;
                @Override
                protected Integer doInBackground(Person... params) {
                    originalPerson=params[0];

                    int result=-1;
                    String phoneNumber = originalPerson.getGlobalPhoneNumber();
                    if (!TextUtils.isEmpty(phoneNumber)) {
                        List<Buddy> buddies = new ArrayList<Buddy>();
                        phoneNumber = phoneNumber.replace("-", "").replace(" ", "");
                        result = WowTalkWebServerIF.getInstance(LocalContactsActivity.this).fGetBuddyWithPhoneNumber(phoneNumber, buddies);
                        if(0 == result) {
                            Log.i("There are " + buddies.size() + " persons associated with the phoneNumber " + phoneNumber);
                            if (!buddies.isEmpty()) {
                                resultBuddy=buddies.get(0);
                            }
                        }
                    }
                    return result;
                }

                @Override
                protected void onPostExecute(Integer result) {
                    if(0 == result) {
                        if(null == resultBuddy) {
                            originalPerson.buddyType=ContactInfoActivity.BUDDY_TYPE_NOT_USER;
                        } else {
                            boolean isFriend = (0 != (resultBuddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE))
                                    || (0 != (resultBuddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_PENDING_OUT))
                                    || resultBuddy.userID.equals(PrefUtil.getInstance(LocalContactsActivity.this).getUid());
                            if(isFriend) {
                                originalPerson.buddyType=ContactInfoActivity.BUDDY_TYPE_IS_FRIEND;
                            } else {
                                originalPerson.buddyType=ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND;
                            }

                            originalPerson.setWithBuddy(resultBuddy);
                        }

                        msgHandler.removeMessages(MSG_ID_REFRESH_LOCAL_CONTACT_LIST);
                        msgHandler.sendEmptyMessageDelayed(MSG_ID_REFRESH_LOCAL_CONTACT_LIST,LOCAL_CONTACT_LIST_REFRESH_INTERVAL);
                    }

                    ++numOfPersonUpdated;
                    if(numOfPersonUpdated >= ContactUtil.localPersons.size()) {
                        mMsgBox.dismissWait();
                    }
                }
            }.execute(aPerson);
        }
    }

    private void commitSelection() {
        List<Person> persons = personAdapter.getPersonSource();
        ArrayList<Person> selection = new ArrayList<Person>();
        if(persons != null) {
            for(Person p : persons) {
                if(p != null && p.isSelected()) {
                    selection.add(p);
                }
            }
        }

        if (mIsInitForMultiSelect) {
            Intent data = new Intent();
            data.putParcelableArrayListExtra(EXTRA_SELECTION, selection);
            setResult(RESULT_OK, data);
            finish();
        } else {
            // Invite local contact to wowcity.
            String phoneNumber = "";
            for (Person person : selection) {
                phoneNumber += person.getGlobalPhoneNumber() + ",";
            }
            // remove the last ","
            if (phoneNumber.contains(",")) {
                phoneNumber = phoneNumber.substring(0, phoneNumber.length() - 1);
            }

            Uri smsUri = Uri.parse("smsto:" + phoneNumber);
            Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
            intent.putExtra("sms_body", "Hi!我正在使用园区通！一起来尝试一下吧！");
            startActivity(intent);
            finish();
        }
    }

	private void toggleSelectMode() {
		if (_isInSelectMode) {
			_isInSelectMode = false;
			btnTitleBack.setVisibility(View.VISIBLE);
			btnTitleGroup.setVisibility(View.VISIBLE);
			txtTitleAll.setVisibility(View.GONE);
			txtTitleDone.setVisibility(View.GONE);
			mButtonLayout.setVisibility(View.GONE);
		} else {
			_isInSelectMode = true;
			btnTitleBack.setVisibility(View.GONE);
			btnTitleGroup.setVisibility(View.GONE);
			txtTitleAll.setVisibility(View.VISIBLE);
			txtTitleDone.setVisibility(View.VISIBLE);
//			mButtonLayout.setVisibility(View.VISIBLE);
            mButtonLayout.setVisibility(View.GONE);
		}
		personAdapter.notifyDataSetChanged();
		fSetBottomIsEnabled();
	}
	
	private void fSetAllPersonIsSelected() {
		boolean isAllSelected = true;
		for (Person person : ContactUtil.localPersons) {
			if (!person.isSelected()) {
				isAllSelected = false;
				break;
			}
		}
		for(Person person : ContactUtil.localPersons) {
			person.setSelected(!isAllSelected);
		}
		personAdapter.notifyDataSetChanged();
		fSetBottomIsEnabled();
	}
	
	private void fSetBottomIsEnabled() {
		ArrayList<Person> tmpPersons = personAdapter.getPersonSource();
		for (Person person : tmpPersons) {
			if (person.isSelected()) {
				btnSms.setEnabled(true);
				btnEmail.setEnabled(true);
				return;
			}
		}
		btnSms.setEnabled(false);
		btnEmail.setEnabled(false);
	}
	
	private void fSendSmsToGroup() {
		StringBuilder sb = new StringBuilder();
		for (Person person : ContactUtil.localPersons) {
			if (person.isSelected()) {
//				sb.append(person.getPhones().get(0)[0]).append(",");
                sb.append(person.getGlobalPhoneNumber()).append(",");
			}
		}
		Uri smsUri = Uri.parse("smsto:" + sb.toString());
		Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
		intent.putExtra("sms_body", "");
		startActivity(intent);
	}
	
	private void fSendEmailToGroup() {
		Intent intent = new Intent(android.content.Intent.ACTION_SEND);
		intent.setType("plain/text");
		String[] emailReciver = new String[]{"jianxd3@gmail.com", "123366451@qq.com"};
		String emailSubject = "你有一条短信";
		String emailBody = "ffffffffff";
		intent.putExtra(android.content.Intent.EXTRA_EMAIL, emailReciver);
		intent.putExtra(android.content.Intent.EXTRA_SUBJECT, emailSubject);
		intent.putExtra(android.content.Intent.EXTRA_TEXT, emailBody);
		startActivity(Intent.createChooser(intent, "请选择邮件发送软件"));
	}
	
	@Override
	public void onClick(View v) {
		switch (v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.title_groupsms:
			toggleSelectMode();
			break;
        case R.id.field_clear:
            edtSearchContent.setText("");
            break;
		case R.id.select_done:
		    commitSelection();
			break;
		case R.id.select_all:
			fSetAllPersonIsSelected();
			break;
		case R.id.sms_group:
			fSendSmsToGroup();
			break;
		case R.id.email_group:
			fSendEmailToGroup();
			break;
		default:	
			break;
		}
	}
	
    @Override
	public void onTouchingLetterChanged(String s) {
        mMsgBox.toast(s);
        int height = mMainLinearLayout.getChildAt(0).getHeight();
        int numCount = 0;
        if (s.equals("Se")) {
            mMainScrollView.smoothScrollTo(0, 0);
        } else if (s.equals("#")) {
            mMainScrollView.smoothScrollTo(0, height);
        } else {
            for (Person person : ContactUtil.localPersons) {
                if (String.valueOf(person.getSortKey().charAt(0)).toUpperCase().equals(s)) {
                    mMainScrollView.smoothScrollTo(0, height);
                }
                View li = lvLocalContacts.getChildAt(numCount);
                if (li != null)
                    height += li.getHeight();
                numCount++;
            }
        }
	}

    @Override
    public void onTouchCanceled() {

    }



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.local_contacts_page);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);



        if(mMsgBox == null)
            mMsgBox = new MessageBox(this);

		initView();
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

    public static void launchForMultiSelecting(Activity context, int requestCode) {
        Intent i = new Intent(context, LocalContactsActivity.class);
        i.putExtra(EXTRA_MULTI_SELECT_MODE, true);
        context.startActivityForResult(i, requestCode);
    }
}
