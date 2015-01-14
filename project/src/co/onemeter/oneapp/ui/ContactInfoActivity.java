package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.utils.AsyncTaskExecutor;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;

public class ContactInfoActivity extends Activity implements OnClickListener{
    private static final int MOMENT_NUMBER = 3;

	private static final String EXTRA_BUDDY_TYPE = "buddy_type";
	private static final String EXTRA_BUDDY_DETAIL = "buddy_detail";

	public static final int BUDDY_TYPE_IS_FRIEND = 1001;
	public static final int BUDDY_TYPE_NOT_FRIEND = 1002;
	public static final int BUDDY_TYPE_NOT_USER = 1003;
    public static final int BUDDY_TYPE_UNKNOWN = 0;
    public static final int BUDDY_TYPE_MYSELF = 1;

//	private static final int MSG_FETCH_MOMENT_SUCCESS = 100;

    private static final String[] IMAGE_TYPES = new String[] {
            "bmp",
            "jpg",
            "tiff",
            "gif",
            "pcx",
            "tga",
            "exif",
            "fpx",
            "svg",
            "psd",
            "cdr",
            "pcd",
            "dxf",
            "ufo",
            "eps",
            "ai",
            "raw"
    };

    private ImageView mGenderImg;
    private TextView mFriendNameBiz;
    private TextView mPronunciation;
    private LinearLayout mStatusLayout;
    private TextView mStatusTextView;

    private LinearLayout mDeptJobLayout;
    private LinearLayout mDeptLayout;
    private TextView mDeptTextView;
    private ImageView mDividerDeptJob;
    private LinearLayout mJobTitleLayout;
    private TextView mJobTitleTextView;

    private LinearLayout mContactWayLayout;
    private LinearLayout mPhoneLayout;
    private TextView mPhoneTextView;
    private ImageView mDividerPhobeMobile;
    private LinearLayout mMobileLayout;
    private TextView mMobileTextView;
    private ImageView mDividerMobileEmail;
    private LinearLayout mEmailLayout;
    private TextView mEmailTextView;

    private LinearLayout mBranchStoreEmployeeIdLayout;
    private LinearLayout mBranchStoreLayout;
    private TextView mBranchStoreTextView;
    private ImageView mDividerBranchStoreEmployeeId;
    private LinearLayout mEmployeeIdLayout;
    private TextView mEmployeeIdTextView;

    private Database dbHelper;

    private Person person; // access Person only if Buddy is unavailable.
    /**
     * 用于记录初始的buddy的部分信息，比较其是否发生变化，变化则通知联系人了列表界面刷新
     */
    private Buddy mInitBuddy;
    private Buddy buddy;

    private int buddyType;
    
    private boolean isFromSchool = false;

    private MessageBox mMsgBox = null;

    private ImageView imgPhoto;

    private void resetViewsForBiz() {
        mGenderImg.setBackgroundResource(buddy.getSexFlag() == Buddy.SEX_FEMALE ? R.drawable.avatar_female : R.drawable.avatar_male);
        initView_visibilities();
        setFriendInfoForBiz();
        setStatus();
        setDeptJobTitle();
        setContactWay();
        PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, buddy, true);
    }

    /**
     * 头像右侧的个人信息
     */
    private void initView_friendInfoForBiz() {
        AQuery q = new AQuery(this);

        mFriendNameBiz = (TextView) findViewById(R.id.friend_name_biz);
        mPronunciation = (TextView) findViewById(R.id.friend_name_pronunciation);

        q.find(R.id.btn_msg).clicked(this);
        q.find(R.id.btn_call).clicked(this);
        q.find(R.id.btn_video).clicked(this);

        setFriendInfoForBiz();
    }

    private void setFriendInfoForBiz() {
        // 默认男性
        mFriendNameBiz.setText(buddy.nickName);
        mPronunciation.setText(buddy.pronunciation);
    }

    private void initView_status() {
        mStatusLayout = (LinearLayout) findViewById(R.id.status_layout);
        mStatusTextView = (TextView) findViewById(R.id.txt_status);
        setStatus();
    }

    private void setStatus() {
        if (TextUtils.isEmpty(buddy.status)) {
            mStatusLayout.setVisibility(View.GONE);
        } else {
            mStatusLayout.setVisibility(View.VISIBLE);
            mStatusTextView.setText(buddy.status);
        }
    }

    /**
     * 部门，职位
     */
    private void initView_deptJobTitle() {
        mDeptJobLayout = (LinearLayout)findViewById(R.id.layout_dept_job);
        mDeptLayout = (LinearLayout)findViewById(R.id.dept_layout);
        mDeptTextView = (TextView)findViewById(R.id.txt_dept);
        mDividerDeptJob = (ImageView) findViewById(R.id.divider_dept_job);
        mJobTitleLayout = (LinearLayout)findViewById(R.id.layout_job_title);
        mJobTitleTextView = (TextView)findViewById(R.id.txt_job_title);

        setDeptJobTitle();
    }

    private void setDeptJobTitle() {
        int emptyLayoutCount = 0;
        if (TextUtils.isEmpty(buddy.deptName)) {
            mDeptLayout.setVisibility(View.GONE);
            emptyLayoutCount++;
        } else {
            mDeptLayout.setVisibility(View.VISIBLE);
            mDeptTextView.setText(buddy.deptName);
        }

        if (TextUtils.isEmpty(buddy.jobTitle)) {
            mJobTitleLayout.setVisibility(View.GONE);
            emptyLayoutCount++;
        } else {
            mJobTitleLayout.setVisibility(View.VISIBLE);
            mJobTitleTextView.setText(buddy.jobTitle);
        }

        LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mDeptJobLayout.getLayoutParams();
        switch (emptyLayoutCount) {
        case 0:
            mDeptJobLayout.setVisibility(View.VISIBLE);
            mDividerDeptJob.setVisibility(View.VISIBLE);
            params.height = 2 * getResources().getDimensionPixelSize(R.dimen.section_row_height);
            mDeptJobLayout.setLayoutParams(params);
            break;
        case 1:
            mDeptJobLayout.setVisibility(View.VISIBLE);
            mDividerDeptJob.setVisibility(View.GONE);
            params.height = getResources().getDimensionPixelSize(R.dimen.section_row_height);
            mDeptJobLayout.setLayoutParams(params);
            break;
        case 2:
            mDeptJobLayout.setVisibility(View.GONE);
            break;
        default:
            break;
        }
    }

    /**
     * 联系方式
     */
    private void initView_contactWay() {
        mContactWayLayout = (LinearLayout)findViewById(R.id.layout_contact_way);
        mPhoneLayout = (LinearLayout)findViewById(R.id.layout_phone);
        mPhoneTextView = (TextView)findViewById(R.id.txt_phone);
        mDividerPhobeMobile = (ImageView) findViewById(R.id.divider_phone_mobile);
        mMobileLayout = (LinearLayout)findViewById(R.id.layout_mobile);
        mMobileTextView = (TextView)findViewById(R.id.txt_mobile);
        mDividerMobileEmail = (ImageView) findViewById(R.id.divider_mobile_email);
        mEmailLayout = (LinearLayout)findViewById(R.id.layout_email);
        mEmailTextView = (TextView)findViewById(R.id.txt_email);

        mPhoneTextView.setOnClickListener(this);
        mMobileTextView.setOnClickListener(this);
        mEmailTextView.setOnClickListener(this);

        setContactWay();
    }

    private void setContactWay() {
        boolean isHasPhone = false;
        if (TextUtils.isEmpty(buddy.phoneNumber)) {
            mPhoneLayout.setVisibility(View.GONE);
        } else {
            isHasPhone = true;
            mPhoneLayout.setVisibility(View.VISIBLE);
            mPhoneTextView.setText(buddy.phoneNumber);
        }

        boolean isHasMobile = false;
        if (TextUtils.isEmpty(buddy.mobile)) {
            mMobileLayout.setVisibility(View.GONE);
        } else {
            isHasMobile = true;
            mMobileLayout.setVisibility(View.VISIBLE);
            mMobileTextView.setText(buddy.mobile);
        }

        boolean isHasEmail = false;
        if (TextUtils.isEmpty(buddy.getEmail())) {
            mEmailLayout.setVisibility(View.GONE);
        } else {
            isHasEmail = true;
            mEmailLayout.setVisibility(View.VISIBLE);
            mEmailTextView.setText(buddy.getEmail());
        }

        // 设置divider
        if (isHasPhone && (isHasMobile || isHasEmail)) {
            mDividerPhobeMobile.setVisibility(View.VISIBLE);
        } else {
            mDividerPhobeMobile.setVisibility(View.GONE);
        }
        if (isHasMobile && isHasEmail) {
            mDividerMobileEmail.setVisibility(View.VISIBLE);
        } else {
            mDividerMobileEmail.setVisibility(View.GONE);
        }

        if (!isHasPhone && !isHasMobile && !isHasEmail) {
            mContactWayLayout.setVisibility(View.GONE);
        } else {
            int heightCount = 0;
            if (isHasPhone) {
                heightCount++;
            }
            if (isHasMobile) {
                heightCount++;
            }
            if (isHasEmail) {
                heightCount++;
            }
            mContactWayLayout.setVisibility(View.VISIBLE);
            LinearLayout.LayoutParams params = (LinearLayout.LayoutParams)mContactWayLayout.getLayoutParams();
            params.height = getResources().getDimensionPixelSize(R.dimen.section_row_height) * heightCount;
            mContactWayLayout.setLayoutParams(params);
        }
    }

    /** 根据我与当前用户的关系设置界面元素的可见性 */
    private void initView_visibilities() {

    	  AQuery q = new AQuery(this);
    	  if(buddyType == BUDDY_TYPE_IS_FRIEND){
              q.find(R.id.module_chat).visible();
              q.find(R.id.btn_goto_moments).visible();
              q.find(R.id.btn_delete).visible();
              q.find(R.id.btn_add).gone();
    	  }if(buddyType == BUDDY_TYPE_NOT_FRIEND){
    		  if(isFromSchool){
    			  q.find(R.id.btn_msg).visible();
    			  q.find(R.id.btn_call).invisible();
    			  q.find(R.id.btn_video).invisible();
    		  }else{
    			  q.find(R.id.module_chat).invisible();
    		  }
              q.find(R.id.btn_goto_moments).gone();
              q.find(R.id.btn_delete).gone();
              q.find(R.id.btn_add).visible();
    	  }if(buddyType == BUDDY_TYPE_MYSELF){
              q.find(R.id.module_chat).visible();
              q.find(R.id.btn_goto_moments).visible();
              q.find(R.id.btn_delete).gone();
              q.find(R.id.btn_add).gone();
    	  }
    }


    private void initView() {
        AQuery q = new AQuery(this);

        imgPhoto = (ImageView) findViewById(R.id.img_thumbnail);
        mGenderImg = (ImageView) findViewById(R.id.avatar_gender);
        mGenderImg.setBackgroundResource(buddy.getSexFlag() == Buddy.SEX_FEMALE
                ? R.drawable.avatar_female : R.drawable.avatar_male);
        PhotoDisplayHelper.displayPhoto(this, imgPhoto, R.drawable.default_avatar_90, buddy, true);

        initView_visibilities();
        initView_friendInfoForBiz();
        initView_status();
        initView_deptJobTitle();
        initView_contactWay();

        q.find(R.id.img_thumbnail).clicked(this);
        q.find(R.id.navbar_btn_left).clicked(this);
        q.find(R.id.btn_goto_moments).clicked(this);
        q.find(R.id.btn_add).clicked(this);
        q.find(R.id.btn_delete).clicked(this);
    }

    /**
     * 获取联系人的详情信息，刚开始没有下载，每次加载时下载
     */
    private void getBuddyInfoFromServer() {
        if (null == buddy || TextUtils.isEmpty(buddy.userID)) {
            return;
        }

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int resultCode = WowTalkWebServerIF.getInstance(ContactInfoActivity.this)
                        .fGetBuddyWithUID(buddy.userID);
                if (resultCode == ErrorCode.OK) {
                    buddy = dbHelper.buddyWithUserID(buddy.userID);

                    // 获取buddy的部门,此部门为组合的部门信息，不是纯粹的单个部门
                    GroupChatRoom dept = dbHelper.getDeptInfoByMemberId(buddy.userID);
                    // 如果不属于任何子部门，则隐藏
                    if (null != dept && dept.isEditable) {
                        buddy.deptName = dept.groupNameOriginal;
                    } else {
                        buddy.deptName = null;
                    }
                }
                return resultCode;
            }

            protected void onPostExecute(Integer resultCode) {
                if (resultCode == ErrorCode.OK) {
                    if (0 != (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                        buddyType = BUDDY_TYPE_IS_FRIEND;
                    } else {
                        buddyType = BUDDY_TYPE_NOT_FRIEND;
                    }
                    resetViewsForBiz();
                }
            }

            ;
        });
    }

    public static void fSendSmsToInvite(Context context,Person person) {
        String phoneNumber = person.getGlobalPhoneNumber();
        Uri smsUri = Uri.parse("smsto:" + phoneNumber);
        Intent intent = new Intent(Intent.ACTION_SENDTO, smsUri);
        intent.putExtra("sms_body", context.getString(R.string.setting_tell_friend_sms_content));
        context.startActivity(intent);
    }

	@Override
	public void onClick(View v) {
		switch (v.getId()) {
            case R.id.navbar_btn_left:
                handleBackEvent();
                break;
            case R.id.btn_msg:
                chatWith(buddy.userID, buddy.nickName);
                break;
            case R.id.btn_call:
                CallMainActivity.startNewOutGoingCall(ContactInfoActivity.this, buddy.userID, buddy.nickName, false);
                break;
            case R.id.btn_video:
                CallMainActivity.startNewOutGoingCall(ContactInfoActivity.this, buddy.userID, buddy.nickName, true);
                break;
            case R.id.img_thumbnail:
                ImageViewActivity.launch(this, buddy);
                break;
            case R.id.txt_phone:
                String phoneNumber = mPhoneTextView.getText().toString().trim();
                if (!TextUtils.isEmpty(phoneNumber)) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + phoneNumber));
                    startActivity(callIntent);
                }
                break;
            case R.id.txt_mobile:
                String mobileNumber = mMobileTextView.getText().toString().trim();
                if (!TextUtils.isEmpty(mobileNumber)) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + mobileNumber));
                    startActivity(callIntent);
                }
                break;
            case R.id.txt_email:
                String emailAddr = mEmailTextView.getText().toString().trim();
                if (!TextUtils.isEmpty(emailAddr)) {
                    Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + emailAddr));
                    startActivity(Intent.createChooser(emailIntent, null));
                }
                break;
            case R.id.btn_goto_moments:
                TimelineActivity.launch(this, buddy.userID, buddy.nickName);
                break;
            case R.id.btn_add:
                android.util.Log.d("---------------------", buddy+"");
            	Intent intent = new Intent(this, FriendValidateActivity.class);
            	ArrayList<Buddy> buddyList = new ArrayList<Buddy>();
            	buddyList.add(buddy);
            	intent.putExtra("buddyList2", buddyList);
            	startActivity(intent);
//                addBuddy();
                break;
            case R.id.btn_delete:
            	Builder builder = new AlertDialog.Builder(this);
            	builder.setTitle("提示");
            	builder.setMessage("你确定要删除好友吗?");
            	builder.setPositiveButton("确定", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						// TODO Auto-generated method stub
						removeBuddy();
						Toast.makeText(ContactInfoActivity.this, "删除好友成功", Toast.LENGTH_SHORT).show();
					}
				});
            	
            	builder.setNegativeButton("取消", new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface arg0, int arg1) {
						
					}
				});
            	
            	builder.create().show();
                
                break;
            default:
                break;
        }
    }

    private void addBuddy() {
        final Context context = this;
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(context)
                        .fAddBuddy(buddy.userID);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                buddy = new Database(context).buddyWithUserID(buddy.userID); // refresh buddy data
                if (result == ErrorCode.OK) {
                    if (0 != (Buddy.RELATIONSHIP_FRIEND_HERE & buddy.getFriendShipWithMe())) {
                        mMsgBox.toast(R.string.contacts_add_buddy_succeed_without_pending);
                    } else if (0 != (Buddy.RELATIONSHIP_PENDING_OUT & buddy.getFriendShipWithMe())) {
//                        mMsgBox.show(null, getString(R.string.contacts_add_buddy_pending_out));
                        Toast.makeText(ContactInfoActivity.this,
                                getString(R.string.contacts_add_buddy_pending_out),
                                Toast.LENGTH_SHORT).show();
                    }
                } else if (result == ErrorCode.ERR_OPERATION_DENIED) {
                    mMsgBox.show(null, getString(R.string.contactinfo_add_friend_denied));
                } else {
                    mMsgBox.show(null, getString(R.string.operation_failed));
                }
            }
        });
    }

    private void removeBuddy() {
        final Context context = this;
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                return WowTalkWebServerIF.getInstance(context)
                        .fRemoveBuddy(buddy.userID);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    finish();
                }
            }
        });
    }

    private void handleBackEvent() {
        boolean isChanged = true;
        // pronunciation,status,photoUploadTimeStamp全相同，则认为buddy详情未变化
        if (((TextUtils.isEmpty(mInitBuddy.pronunciation) && TextUtils.isEmpty(buddy.pronunciation))
                    || (!TextUtils.isEmpty(mInitBuddy.pronunciation) && mInitBuddy.pronunciation.equals(buddy.pronunciation)))
                && ((TextUtils.isEmpty(mInitBuddy.status) && TextUtils.isEmpty(buddy.status))
                    || (!TextUtils.isEmpty(mInitBuddy.status) && mInitBuddy.status.equals(buddy.status)))
                && mInitBuddy.photoUploadedTimeStamp == buddy.photoUploadedTimeStamp) {
            isChanged = false;
        }
        Intent data = new Intent();
        data.putExtra("uid", buddy.userID);
        data.putExtra("is_changed", isChanged);
        setResult(RESULT_OK, data);
        finish();
    }

    @Override
    public void onBackPressed() {
        handleBackEvent();
    }

    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_contact_info);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

		buddyType = getIntent().getIntExtra(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_USER);
		person = getIntent().getParcelableExtra(EXTRA_BUDDY_DETAIL);
		isFromSchool = getIntent().getBooleanExtra("fromSchool", false);
		Log.i("person signature : " + person.getPersonState()
				+ " rigion : " + person.getRigion()
				+ " photoUploadedTimestamp: " + person.photoUploadedTimestamp
				+ " username : " + person.getUsername());
        dbHelper = new Database(this);
		buddy = dbHelper.buddyWithUserID(person.getID());
		if(buddy == null) {
            buddy=person.toBuddy();
        } else {
            // 获取buddy的部门,此部门为组合的部门信息，不是纯粹的单个部门
            GroupChatRoom dept = dbHelper.getDeptInfoByMemberId(buddy.userID);
            // 如果不属于任何子部门，则隐藏
            if (null != dept && dept.isEditable) {
                buddy.deptName = dept.groupNameOriginal;
            } else {
                buddy.deptName = null;
            }
        }
        // 填充用于判断联系人的以下四个属性是否变化的Buddy
        mInitBuddy = new Buddy();
        mInitBuddy.userID = buddy.userID;
        mInitBuddy.pronunciation = buddy.pronunciation;
        mInitBuddy.photoUploadedTimeStamp = buddy.photoUploadedTimeStamp;
        mInitBuddy.status = buddy.status;

        mMsgBox = new MessageBox(this);

        initView();

        getBuddyInfoFromServer();
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

    private void chatWith(String uid, String phoneNumber) {
		Intent intent = new Intent(this,
				MessageComposerActivity.class);
		intent.putExtra(MessageComposerActivity.KEY_TARGET_UID, 
				uid);
		intent.putExtra(
				MessageComposerActivity.KEY_TARGET_DISPLAYNAME, 
				phoneNumber);
		startActivity(intent);
	}

    public static void launch(Context context, Buddy buddy) {
        int mFriendType = ContactInfoActivity.BUDDY_TYPE_UNKNOWN;

        if (!TextUtils.isEmpty(buddy.userID)) {
            if (buddy.userID.equals(PrefUtil.getInstance(context).getUid())) {
                mFriendType = ContactInfoActivity.BUDDY_TYPE_MYSELF;
            } else {
                mFriendType = (0 != (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE))
                        ? ContactInfoActivity.BUDDY_TYPE_IS_FRIEND
                        : ContactInfoActivity.BUDDY_TYPE_NOT_FRIEND;
            }
        }
        launch(context, buddy.userID, mFriendType);
    }

	/**
	 * 
	 * @param context
	 * @param buddyUid
	 * @param buddyType BUDDY_TYPE_*
	 */
	public static void launch(final Context context, final String buddyUid, final int buddyType) {
		Database db = new Database(context);
		Buddy b = db.buddyWithUserID(buddyUid);
        if (b != null) {
            if (b.getAccountType() == Buddy.ACCOUNT_TYPE_NOTICE_MAN) {
                return;
            }
            launch(context, Person.fromBuddy(b), buddyType);
        } else {
            final MessageBox m = new MessageBox(context);
            m.showWait();
            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {
                @Override
                protected Integer doInBackground(Void... params) {
                    WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(context);
                    return web.fGetBuddyWithUID(buddyUid);
                }

                @Override
                protected void onPostExecute(Integer errno) {
                    m.dismissWait();
                    if (ErrorCode.OK == errno) {
                        Database db = new Database(context);
                        Buddy b = db.buddyWithUserID(buddyUid);
                        if (b != null) {
                            launch(context, Person.fromBuddy(b), buddyType);
                        }
                    }
                }
            });
        }
	}

	/**
	 * 
	 * @param context
	 * @param person
	 * @param buddyType BUDDY_TYPE_*
	 */
	public static void launch(Context context, Person person,
			int buddyType) {
		
		Intent i = new Intent(context, ContactInfoActivity.class);
		Database db = new Database(context);
		Bundle bu = new Bundle();
        if (buddyType == BUDDY_TYPE_UNKNOWN) {
            Buddy b = db.buddyWithUserID(person.getID());
            if (b != null) {
                String myselfId = PrefUtil.getInstance(context).getUid();
                if (!TextUtils.isEmpty(myselfId) && myselfId.equals(b.userID)) {
                    bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_MYSELF);
                } else if (0 != (b.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                    bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_IS_FRIEND);
                } else {
                    bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
                }
            } else {
                bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
            }
        } else {
            bu.putInt(EXTRA_BUDDY_TYPE, buddyType);
        }
		bu.putParcelable(EXTRA_BUDDY_DETAIL, person);
		i.putExtras(bu);
		context.startActivity(i);
	}

	public static void launch(Context context, Person person,
			int buddyType,boolean isFromSchool) {
		
		Intent i = new Intent(context, ContactInfoActivity.class);
		Database db = new Database(context);
		Bundle bu = new Bundle();
        if (buddyType == BUDDY_TYPE_UNKNOWN) {
            Buddy b = db.buddyWithUserID(person.getID());
            if (b != null) {
            	i.putExtra("fromSchool", isFromSchool);
                String myselfId = PrefUtil.getInstance(context).getUid();
                if (!TextUtils.isEmpty(myselfId) && myselfId.equals(b.userID)) {
                    bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_MYSELF);
                } else if (0 != (b.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                    bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_IS_FRIEND);
                } else {
                    bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
                }
            } else {
                bu.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
            }
        } else {
            bu.putInt(EXTRA_BUDDY_TYPE, buddyType);
        }
		bu.putParcelable(EXTRA_BUDDY_DETAIL, person);
		i.putExtras(bu);
		context.startActivity(i);
	}
	
    public static void launchForResult(Activity activity, int requestCode,
            Person person, int buddyType) {

        Intent intent = new Intent(activity, ContactInfoActivity.class);
        Database db = new Database(activity);
        Bundle bundle = new Bundle();
        if (buddyType == BUDDY_TYPE_UNKNOWN) {
            Buddy buddy = db.buddyWithUserID(person.getID());
            if (buddy != null) {
                String myselfId = PrefUtil.getInstance(activity).getUid();
                if (!TextUtils.isEmpty(myselfId) && myselfId.equals(buddy.userID)) {
                    bundle.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_MYSELF);
                } else if (0 != (buddy.getFriendShipWithMe() & Buddy.RELATIONSHIP_FRIEND_HERE)) {
                    bundle.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_IS_FRIEND);
                } else {
                    bundle.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
                }
            } else {
                bundle.putInt(EXTRA_BUDDY_TYPE, BUDDY_TYPE_NOT_FRIEND);
            }
        } else {
            bundle.putInt(EXTRA_BUDDY_TYPE, buddyType);
        }
        bundle.putParcelable(EXTRA_BUDDY_DETAIL, person);
        intent.putExtras(bundle);
        activity.startActivityForResult(intent, requestCode);
    }
}
