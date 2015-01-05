package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import android.widget.ImageView.ScaleType;

import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.Account;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.GroupChatRoom;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WebServerIF;
import org.wowtalk.ui.*;
import org.wowtalk.ui.msg.Utils;
import org.wowtalk.ui.msg.InputBoardManager;

import co.onemeter.oneapp.R;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;

public class MyInfoActivity extends Activity implements OnClickListener, InputBoardManager.ChangeToOtherAppsListener {
	private ImageButton btnTitleBack;
	private TextView mPersonType, txtNick,txtSex,txtAge,txtArea,txtStatus;
	private TextView mPronunciationText;
	private TextView mDeptText;
	private TextView mJobTitleText;
	private TextView mPhoneText;
	private TextView mMobileText;
	private TextView mEmailText;
	private TextView mBranchStoreText;
	private TextView mEmployeeIdText;

	private RelativeLayout change_avatar;
	private ImageView imgPhoto;
	/**
	 * 在onResume()和updateMyAvatar()中的AsyncTask回调函数中都要调用，
	 * 但它可能不是同一个对象(在onCreate中创建，进入AsyncTask，但后来返回，重新进入创建)，因此需要static
	 */
	private static ProgressBar progressBar;

    private BottomButtonBoard bottomBoard;
	
	public static final int REQ_INPUT_NICK=1;
	public static final int REQ_INPUT_AGE=2;
	public static final int REQ_INPUT_AREA=3;
	public static final int REQ_INPUT_SEX=4;
	public static final int REQ_INPUT_STATUS=5;
	public static final int REQ_INPUT_PHOTO = 6;
	public static final int REQ_INPUT_PRONUNCIATION = 7;
	public static final int REQ_INPUT_PHONE = 8;
	public static final int REQ_INPUT_MOBILE = 9;
	public static final int REQ_INPUT_EMAIL = 10;
	public static final int REQ_INPUT_BRANCH_STORE = 11;

    public final static int PHOTO_THUMBNAIL_WIDTH = 200;
    public final static int PHOTO_THUMBNAIL_HEIGHT = 200;
    // resize photo to VGA size before sending
    public final static int PHOTO_SEND_WIDTH = 600;
    public final static int PHOTO_SEND_HEIGHT = 600;
	
	private MediaInputHelper mMediaInputHelper = null;
	WebServerIF wif = null;
    private MessageBox mMsgBox;
    private PrefUtil mPrefUtil;

    private boolean mProfileUpdated = false;
    private String mDeptName;

	final Calendar calendar = Calendar.getInstance();
	private int mYear = calendar.get(Calendar.YEAR);
	private int mMonth = calendar.get(Calendar.MONTH);
	private int mDay = calendar.get(Calendar.DAY_OF_MONTH);
	private Date birthdayDate;
	private DatePickerDialog.OnDateSetListener mDateSetListener = new DatePickerDialog.OnDateSetListener() {
		@Override
		public void onDateSet(DatePicker view, int year, int monthOfYear,
				int dayOfMonth) {
			mYear = year;
			mMonth = monthOfYear;
			mDay = dayOfMonth;
			birthdayDate = new Date();
			birthdayDate.setYear(mYear - 1900);
			birthdayDate.setMonth(mMonth);
			birthdayDate.setDate(mDay);
			updateMyBirthday(birthdayDate);
		}
	};

    private void initView() {
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		progressBar = (ProgressBar) findViewById(R.id.progressBar1);
        mPersonType = (TextView)findViewById(R.id.person_account_type);
		txtNick=(TextView)findViewById(R.id.person_name);
		mPronunciationText = (TextView)findViewById(R.id.pronunciation);
		txtSex=(TextView)findViewById(R.id.person_sex);
		txtAge=(TextView)findViewById(R.id.person_age);
		txtArea=(TextView)findViewById(R.id.person_rigion);
		txtStatus=(TextView)findViewById(R.id.person_signature);
		change_avatar=(RelativeLayout)findViewById(R.id.change_avatar);
		imgPhoto=(ImageView)findViewById(R.id.thumbnail);

		mDeptText = (TextView)findViewById(R.id.txt_dept);
		mJobTitleText = (TextView)findViewById(R.id.txt_job_title);
		mPhoneText = (TextView)findViewById(R.id.txt_phone);
		mMobileText = (TextView)findViewById(R.id.txt_mobile);
		mEmailText = (TextView)findViewById(R.id.txt_email);
		mBranchStoreText = (TextView)findViewById(R.id.txt_branch_store);
		mEmployeeIdText = (TextView)findViewById(R.id.txt_employee_id);

		progressBar.setVisibility(View.GONE);

		btnTitleBack.setOnClickListener(this);
		change_avatar.setOnClickListener(this);
        if (GlobalValue.RELEASE_AS_WOWCITY) {
            findViewById(R.id.boxNick).setOnClickListener(this);
        }
		findViewById(R.id.boxSex).setOnClickListener(this);
		findViewById(R.id.boxAge).setOnClickListener(this);
		findViewById(R.id.boxArea).setOnClickListener(this);
		findViewById(R.id.boxStatus).setOnClickListener(this);
		findViewById(R.id.thumbnail).setOnClickListener(this);

		findViewById(R.id.box_pronunciation).setOnClickListener(this);
		findViewById(R.id.layout_phone).setOnClickListener(this);
		findViewById(R.id.layout_mobile).setOnClickListener(this);
		findViewById(R.id.layout_email).setOnClickListener(this);
		findViewById(R.id.layout_branch_store).setOnClickListener(this);
		txtAge.setOnClickListener(this);
	}

	/**
	 * fetch and display.
	 */
	private void fetchPersonalInfo() {
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				return wif.fGetMyProfile();
			}

			@Override
			protected void onPostExecute(Integer result) {
				if (result == ErrorCode.OK){
					displayPeronalInfo();
				}
			}

		}.execute((Void)null);
	}
	
	protected String getSexLiteral(int fGetMySexFromLocal) {
		switch(fGetMySexFromLocal) {
		case Buddy.SEX_FEMALE:
			return this.getString(R.string.sex_female);
		case Buddy.SEX_MALE:
			return this.getString(R.string.sex_male);
		default:
			return this.getString(R.string.sex_unknown);
		}
	}

	@SuppressWarnings("deprecation")
	public void onClick(View v) {
		switch (v.getId()) {
            case R.id.title_back:
                setResult(mProfileUpdated ? Activity.RESULT_OK : Activity.RESULT_CANCELED);
                finish();
                break;
            case R.id.boxNick:
            {
                Intent i = new Intent(MyInfoActivity.this, InputPlainTextActivity.class)
                        .putExtra(InputPlainTextActivity.EXTRA_VALUE, mPrefUtil.getMyNickName())
                        .putExtra(InputPlainTextActivity.EXTRA_TITLE, getString(R.string.change_nickname))
                        .putExtra(InputPlainTextActivity.EXTRA_DESCRIPTION, getString(R.string.change_nickname_info));
                startActivityForResult(i, REQ_INPUT_NICK);
            }
            break;
            case R.id.boxSex:
                bottomBoard.clearView();
                bottomBoard.add(getSexLiteral(Buddy.SEX_FEMALE), BottomButtonBoard.BUTTON_BLUE,
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                bottomBoard.dismiss();
                                updateMySex(Buddy.SEX_FEMALE);
                            }
                        });
                bottomBoard.add(getSexLiteral(Buddy.SEX_MALE), BottomButtonBoard.BUTTON_BLUE,
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                bottomBoard.dismiss();
                                updateMySex(Buddy.SEX_MALE);
                            }
                        });
                bottomBoard.addCancelBtn(getString(R.string.cancel));
                bottomBoard.show();
                break;
            case R.id.person_age:
            {
                showDialog(1);
            }
            break;
            case R.id.boxArea:
            {
                Intent i = new Intent(MyInfoActivity.this, ChooseAreaActivity.class);
                startActivityForResult(i, REQ_INPUT_AREA);
                break;
            }
            case R.id.boxStatus:
            {
                Intent i = new Intent(MyInfoActivity.this, InputTextActivity.class);
                i.putExtra("status", mPrefUtil.getMyStatus());
                startActivityForResult(i, REQ_INPUT_STATUS);
                break;
            }
            case R.id.thumbnail:
                Buddy b = new Buddy();
                b.userID = mPrefUtil.getUid();
                b.photoUploadedTimeStamp = mPrefUtil.getMyPhotoUploadedTimestamp();
                ImageViewActivity.launch(this, b);
                break;
            case R.id.change_avatar:
//                if(mMediaInputHelper == null)
//                    mMediaInputHelper = new MediaInputHelper();
//                mMediaInputHelper.inputImage(this, REQ_INPUT_PHOTO);
                bottomBoard.clearView();
                bottomBoard.add(getString(R.string.image_take_photo), BottomButtonBoard.BUTTON_BLUE,
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                bottomBoard.dismiss();
                                if (mMediaInputHelper == null)
                                    mMediaInputHelper = new MediaInputHelper(MyInfoActivity.this);
                                mMediaInputHelper.takePhoto(MyInfoActivity.this, REQ_INPUT_PHOTO);
                            }
                        });
                bottomBoard.add(getString(R.string.image_pick_from_local), BottomButtonBoard.BUTTON_BLUE,
                        new OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                bottomBoard.dismiss();
                                if (mMediaInputHelper == null)
                                    mMediaInputHelper = new MediaInputHelper(MyInfoActivity.this);
                                mMediaInputHelper.pickPhoto(MyInfoActivity.this, REQ_INPUT_PHOTO);
                            }
                        });
                bottomBoard.addCancelBtn(getString(R.string.cancel));
                bottomBoard.show();
                break;
            case R.id.box_pronunciation:
                Intent pronunciationIntent = new Intent(MyInfoActivity.this, InputSimpleTextActivity.class);
                pronunciationIntent.putExtra(InputSimpleTextActivity.CATEGORY_KEY, InputSimpleTextActivity.CATEGORY_PRONUNCIATION);
                pronunciationIntent.putExtra(InputSimpleTextActivity.ORIGINAL_VALUE_KEY, mPrefUtil.getMyPronunciation());
                startActivityForResult(pronunciationIntent, REQ_INPUT_PRONUNCIATION);
                break;
            case R.id.layout_phone:
                Intent phoneIntent = new Intent(MyInfoActivity.this, InputSimpleTextActivity.class);
                phoneIntent.putExtra(InputSimpleTextActivity.CATEGORY_KEY, InputSimpleTextActivity.CATEGORY_PHONE);
                phoneIntent.putExtra(InputSimpleTextActivity.ORIGINAL_VALUE_KEY, mPrefUtil.getMyPhoneNumber());
                startActivityForResult(phoneIntent, REQ_INPUT_PHONE);
                break;
            case R.id.layout_mobile:
                Intent mobileIntent = new Intent(MyInfoActivity.this, InputSimpleTextActivity.class);
                mobileIntent.putExtra(InputSimpleTextActivity.CATEGORY_KEY, InputSimpleTextActivity.CATEGORY_MOBILE);
                mobileIntent.putExtra(InputSimpleTextActivity.ORIGINAL_VALUE_KEY, mPrefUtil.getMyMobile());
                startActivityForResult(mobileIntent, REQ_INPUT_MOBILE);
                break;
            case R.id.layout_email:
                Intent emailIntent = new Intent(MyInfoActivity.this, InputSimpleTextActivity.class);
                emailIntent.putExtra(InputSimpleTextActivity.CATEGORY_KEY, InputSimpleTextActivity.CATEGORY_EMAIL);
                emailIntent.putExtra(InputSimpleTextActivity.ORIGINAL_VALUE_KEY, mPrefUtil.getMyEmail());
                startActivityForResult(emailIntent, REQ_INPUT_EMAIL);
                break;
            case R.id.layout_branch_store:
                Intent branchStoreIntent = new Intent(MyInfoActivity.this, InputSimpleTextActivity.class);
                branchStoreIntent.putExtra(InputSimpleTextActivity.CATEGORY_KEY, InputSimpleTextActivity.CATEGORY_BRANCH_STORE);
                branchStoreIntent.putExtra(InputSimpleTextActivity.ORIGINAL_VALUE_KEY, mPrefUtil.getMyArea());
                startActivityForResult(branchStoreIntent, REQ_INPUT_BRANCH_STORE);
                break;
            default:
                break;
        }
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		switch(id) {
		case 1:
			//得到当前的生日
			String bs = mPrefUtil.getMyBirthday();
			if(!Utils.isNullOrEmpty(bs)) {//"0000-00-00"
				mYear = Integer.valueOf(bs.substring(0, 4));
				mMonth = Integer.valueOf(bs.substring(5, 7))-1;
				mDay = Integer.valueOf(bs.substring(8));
			}                   
			return new DatePickerDialog(this, mDateSetListener, mYear, mMonth, mDay);//弹框时显示的是原先设置的生日
		default:
			break;
		}
		return null;
	}
	
	@Override
	protected void onPrepareDialog(int id, Dialog dialog) {
		switch(id) {
		case 1:
			((DatePickerDialog) dialog).updateDate(mYear, mMonth, mDay);
			break;
		}
	}
	
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.person_info_page);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
		wif = WebServerIF.getInstance(MyInfoActivity.this);
        bottomBoard = new BottomButtonBoard(this, getWindow().getDecorView());
        mPrefUtil = PrefUtil.getInstance(MyInfoActivity.this);
		
		initView();

		Database dbHelper = new Database(MyInfoActivity.this);
        GroupChatRoom dept = dbHelper.getDeptInfoByMemberId(mPrefUtil.getUid());
        if (null != dept && dept.isEditable) {
            mDeptName = dept.groupNameOriginal;
        }
		displayPeronalInfo();
		fetchPersonalInfo();
		
		if(savedInstanceState != null) {
			mMediaInputHelper = savedInstanceState.getParcelable("mMediaInputHelper");
		}
	}

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        AppStatusService.setIsMonitoring(true);
        showProgressBar(true);
    }

    /**
     * 是否需要显示progressBar
     * @param isMaybeVisible maybe the progress is visible
     */
    private void showProgressBar(boolean isMaybeVisible) {
        if (isMaybeVisible) {
            if (mPrefUtil.isUploadingMyAvatar()) {
                progressBar.setVisibility(View.VISIBLE);
            }
        } else {
            mPrefUtil.setIsUploadingMyAvatar(false);
            progressBar.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
	
	@Override
	protected void onSaveInstanceState(Bundle b) {
        super.onSaveInstanceState(b);
		if(mMediaInputHelper != null) {
			b.putParcelable("mMediaInputHelper", mMediaInputHelper);
		}
	}

    @Override
    protected void onStop() {
        super.onStop();
        if(mProfileUpdated) {
//            notifyProfileUpdating();
            mProfileUpdated = false;
        }
    }

//    private void notifyProfileUpdating() {
//        new AsyncTask<Void, Integer, Void> () {
//            @Override
//            protected Void doInBackground(Void... params) {
//                Database db = new Database(MyInfoActivity.this);
//                db.open();
//                ArrayList<Buddy> buddies = db.fetchAllBuddies();
//                if(buddies != null && !buddies.isEmpty()) {
//                    String myUid = wif.fGetMyUserIDFromLocal();
//                    String myNick = wif.fGetMyNickNameFromLocal();
//                    for(Buddy b : buddies) {
//                        ChatMessage msg = new ChatMessage();
//                        msg.chatUserName = b.userID;
//                        msg.msgType = ChatMessage.MSGTYPE_THIRDPARTY_MSG;
//                        msg.isGroupChatMessage = false;
//                        msg.displayName = myNick;
//                        msg.formatContentAsBuddyProfileUpdated(myUid);
//
//                        WowTalkVoipIF.getInstance(MyInfoActivity.this).fSendChatMessage(msg);
//                        // this message will not displayed
//                        db.deleteChatMessage(msg);
//                    }
//                }
//                return null;
//            }
//        }.execute((Void)null);
//    }

    @Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == RESULT_OK) {
            switch (requestCode) {
            case REQ_INPUT_NICK:
                updateMyNick(data.getExtras().getString(InputPlainTextActivity.EXTRA_VALUE));
                break;
            case REQ_INPUT_PHOTO:
                String[] path = new String[2];
                if (mMediaInputHelper != null
                        && mMediaInputHelper.handleImageResult(this, data,
                                PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
                                PHOTO_THUMBNAIL_WIDTH,
                                PHOTO_THUMBNAIL_HEIGHT, path)) {
                    //					displayPhoto(path[0]);
                    updateMyAvatar(path[0], path[1]);
                }
                break;
            case REQ_INPUT_AREA:
                updateMyArea(data.getExtras().getString("text"));
                break;
            case REQ_INPUT_STATUS:
                updateMyStatus(data.getExtras().getString("text"));
                break;
            case REQ_INPUT_PRONUNCIATION:
                mPronunciationText.setText(data.getExtras().getString(InputSimpleTextActivity.RESULT_VALUE_KEY));
                mProfileUpdated = true;
                break;
            case REQ_INPUT_PHONE:
                mPhoneText.setText(data.getExtras().getString(InputSimpleTextActivity.RESULT_VALUE_KEY));
                mProfileUpdated = true;
                break;
            case REQ_INPUT_MOBILE:
                mMobileText.setText(data.getExtras().getString(InputSimpleTextActivity.RESULT_VALUE_KEY));
                mProfileUpdated = true;
                break;
            case REQ_INPUT_EMAIL:
                mEmailText.setText(data.getExtras().getString(InputSimpleTextActivity.RESULT_VALUE_KEY));
                mProfileUpdated = true;
                break;
            case REQ_INPUT_BRANCH_STORE:
                mBranchStoreText.setText(data.getExtras().getString(InputSimpleTextActivity.RESULT_VALUE_KEY));
                mProfileUpdated = true;
                break;
            default:
                break;
            }
        }
	}

	private void updateMyNick(final String nick) {
		if(nick == null)
			return;
		
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				Buddy data = new Buddy();
				data.nickName = nick;
				return wif.fUpdateMyProfile(data, Buddy.FIELD_FLAG_NICK);
			}

			@Override
			protected void onPostExecute(Integer result) {
				displayPeronalInfo();
                if(result == ErrorCode.OK) {
                    Database dbHelper = new Database(MyInfoActivity.this);
                    String myUid = mPrefUtil.getUid();
                    Buddy me = new Buddy(myUid);
                    me.nickName = nick;
                    dbHelper.storeNewBuddyWithUpdate(me);
                    mProfileUpdated = true;
                }
			}

		}.execute((Void)null);
	}

	private void updateMySex(final int sex) {
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				Buddy data = new Buddy();
				data.setSexFlag(sex);
				return wif.fUpdateMyProfile(data, Buddy.FIELD_FLAG_SEX);
			}

			@Override
			protected void onPostExecute(Integer result) {
				displayPeronalInfo();
                if(result == ErrorCode.OK) {
                    mProfileUpdated = true;
                }
			}

		}.execute((Void)null);
	}

	private void updateMyBirthday(final Date day) {
        if (day.after(new Date())) {
            mMsgBox.toast(R.string.input_birthday_error);
            return;
        }
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				Buddy data = new Buddy();
				data.setBirthday(day);
				return wif.fUpdateMyProfile(data, Buddy.FIELD_FLAG_BIRTHDAY);
			}

			@Override
			protected void onPostExecute(Integer result) {
				displayPeronalInfo();
                if(result == ErrorCode.OK) {
                    mProfileUpdated = true;
                }
			}

		}.execute((Void)null);
	}

	private void updateMyArea(final String area) {
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				Buddy data = new Buddy();
				data.area = area;
				return wif.fUpdateMyProfile(data, Buddy.FIELD_FLAG_AREA);
			}

			@Override
			protected void onPostExecute(Integer result) {
				displayPeronalInfo();
                if(result == ErrorCode.OK) {
                    mProfileUpdated = true;
                }
			}

		}.execute((Void)null);
	}

    private void updateMyStatus(final String status) {
        new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                Buddy data = new Buddy(mPrefUtil.getUid());
                data.status = status;
                int resultCode = wif.fUpdateMyProfile(data, Buddy.FIELD_FLAG_STATUS);
                if (resultCode == ErrorCode.OK) {
                    Database dbHelper = new Database(MyInfoActivity.this);
                    dbHelper.updateMyselfInfo(data, Buddy.FIELD_FLAG_STATUS);
                }
                return resultCode;
            }

            @Override
            protected void onPostExecute(Integer result) {
                displayPeronalInfo();
                if(result == ErrorCode.OK) {
                    mProfileUpdated = true;
                }
            }

        }.execute((Void)null);
    }

	private void updateMyAvatar(final String filePath, final String thumbnailPath) {
		progressBar.setVisibility(View.VISIBLE);
		new AsyncTask<Void, Integer, Integer>() {

			@Override
			protected Integer doInBackground(Void... params) {
				Buddy data = new Buddy();
				data.pathOfPhoto = filePath;
				data.pathOfThumbNail = thumbnailPath;
				mPrefUtil.setIsUploadingMyAvatar(true);
				return wif.fUpdateMyProfile(data, Buddy.FIELD_FLAG_PHOTO);
			}

			@Override
			protected void onPostExecute(Integer result) {
                showProgressBar(false);
                if(result == ErrorCode.OK) {
                    mProfileUpdated = true;
                    displayPhoto(filePath);

                    final String uid = mPrefUtil.getUid();
                    PhotoDisplayHelper.locallyCopy(MyInfoActivity.this, uid, filePath, false);
                    PhotoDisplayHelper.locallyCopy(MyInfoActivity.this, uid, thumbnailPath, true);
                    // refresh my info in local db
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            wif.fGetMyProfile();
                            Database database = new Database(MyInfoActivity.this);
                            // 触发observer
                            database.updateMyselfInfo(null, Buddy.FIELD_FLAG_PHOTO);
                            long photoTimeStamp = mPrefUtil.getMyPhotoUploadedTimestamp();
                            ArrayList<Account> accounts = mPrefUtil.getAccountList();
                            Account tempAccount = null;
                            for (Iterator<Account> iterator = accounts.iterator(); iterator .hasNext();) {
                                tempAccount = iterator.next();
                                if (mPrefUtil.getUid().equals(tempAccount.uid)) {
                                    tempAccount.photoUploadTimeStamp = photoTimeStamp;
                                    break;
                                }
                            }
                            mPrefUtil.setAccountList(accounts);
                        }
                    }).start();
                }
			}

		}.execute((Void)null);
	}

	private void displayPhoto(String path) {
		if(path != null && new File(path).exists()) {
			try {
				imgPhoto.setScaleType(ScaleType.CENTER_CROP);
				imgPhoto.setImageDrawable(new BitmapDrawable(this.getResources(), path));
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

    private void displayPeronalInfo() {
        if (Buddy.ACCOUNT_TYPE_STUDENT == mPrefUtil.getMyAccountType()) {
            mPersonType.setText(R.string.register_user_type_student);
        } else if (Buddy.ACCOUNT_TYPE_TEACHER == mPrefUtil.getMyAccountType()) {
            mPersonType.setText(R.string.register_user_type_teacher);
        }
		txtNick.setText(mPrefUtil.getMyNickName());
		mPronunciationText.setText(mPrefUtil.getMyPronunciation());
		try {
			String bs = mPrefUtil.getMyBirthday();
			if(!Utils.isNullOrEmpty(bs)) {
                if (bs.equals("0000-00-00")) {
                    txtAge.setText(getString(R.string.settings_not_inputed));
                } else {
                    txtAge.setText(bs);
                }
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		txtSex.setText(getSexLiteral(mPrefUtil.getMySex()));
		txtArea.setText(mPrefUtil.getMyArea());
        String strStatus = mPrefUtil.getMyStatus();
        if (strStatus != null && !strStatus.equals("")) {
            txtStatus.setText(mPrefUtil.getMyStatus());
        } else {
            txtStatus.setText(getResources().getString(R.string.settings_not_inputed));
        }

		Buddy me = new Buddy();
		me.userID = mPrefUtil.getUid();
		me.photoUploadedTimeStamp = mPrefUtil.getMyPhotoUploadedTimestamp();
		PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, me, true);

		mDeptText.setText(mDeptName);
		mJobTitleText.setText(mPrefUtil.getMyJobTitle());
		mPhoneText.setText(mPrefUtil.getMyPhoneNumber());
		mMobileText.setText(mPrefUtil.getMyMobile());
		mEmailText.setText(mPrefUtil.getMyEmail());
		mBranchStoreText.setText(mPrefUtil.getMyArea());
		mEmployeeIdText.setText(mPrefUtil.getMyEmployeeId());
	}

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }

}
