package co.onemeter.oneapp.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.*;
import co.onemeter.oneapp.R;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.MessageBox;

import java.util.ArrayList;

public class LoginActivity extends Activity implements OnClickListener {

    /**
     * 在UI上显示一条提示信息，比如“你的账号已经在别处登录”。
     */
    public static final String EXTRA_PROMPT = "prompt";
    /**
     * 是否是添加帐户时进入此界面
     */
    public static final String EXTRA_IS_ADD_ACCOUNT = "is_add_account";

	private static final int MSG_LOGIN_SUCCESS = 100;
	private static final int MSG_AUTO_REGISTER_SUCCESS = 101;
	private static final int MSG_USER_NOT_EXISTS = 104;
	private static final int MSG_AUTH = 105;
	private static final int MSG_LOGIN_FAILED = 106;

    private RelativeLayout mTitleBar;
    private ImageButton mTitleBack;
	private TextView txtForgetPwd;
	private Button btnLogin;

    private View mSignUp;

	private EditText edtAccount;
	private EditText edtPassword;
    private ImageButton fieldClear;

	private Buddy buddy = new Buddy();
	
    private MessageBox mMsgBox;
    private static LoginActivity instance;
    private static PrefUtil mPrefUtil;
    private WowTalkWebServerIF mWebIF;

    /**
     * 是否从添加帐户界面进入此界面的
     */
    private boolean mIsAddAccount;

    public static LoginActivity intance() {
        if (instance == null)
            return new LoginActivity();
        return instance;
    }

    private void handleSuccessLogin(final boolean isAutoLogin,final Object object) {
        Account tempAccount = null;
        if (object instanceof Account) {
            tempAccount = (Account) object;
        }
        final Account oldAccount = tempAccount;

        new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int ret= WowTalkWebServerIF.getInstance(LoginActivity.this).fGetMyProfile();

                if(ErrorCode.OK == ret) {
                    WowTalkWebServerIF webIF = WowTalkWebServerIF.getInstance(LoginActivity.this);
                    // login 成功后再 logout
                    if (mIsAddAccount) {
                        webIF.logoutByUid(oldAccount.uid, oldAccount.password);
                    }

                    String company = "wowtest"; // TODO remove company
                    mPrefUtil.setAutoLogin(isAutoLogin);
                    if(!isAutoLogin) {
                        mPrefUtil.setCompanyId(company);
                    }

                    Account newAccount = Account.getAccountFromSP(mPrefUtil);
                    mPrefUtil.saveOldSP(buddy.userID);
                    mPrefUtil.clearOldSP();
                    mPrefUtil.saveAccountsList(oldAccount, newAccount);
                    mPrefUtil.fillNewSP(buddy.userID);
                    // 移除原先显示的部门id，促使在联系人列表界面重新获取部门及成员
                    mPrefUtil.setDisplayGroupId("");
                    if (mIsAddAccount) {
                        ManageAccountsActivity.setupWowtalkService(LoginActivity.this);
                        // 变换Database,Connect2的标志位(用于判断是否切换了用户)
                        // 触发监听用户切换的操作，去变更数据库连接，以及StartActivity#setupApplication
                        Database.switchAccount();
                    }
                }
                return ret;
            }
            @Override
            protected void onPostExecute(Integer result) {
                if(ErrorCode.OK == result) {
                    GlobalValue.IS_BOOT_FROM_LOGIN = true;
                    if (!mIsAddAccount) {
                        Intent newIntent = new Intent(LoginActivity.this, StartActivity.class);
                        newIntent.putExtra(StartActivity.KEY_IS_START_FROM_LOGIN, true);
                        startActivity(newIntent);
                    }

                    finish();
                } else {
                    // 登录失败，则将原来uid对应的SP拷贝到当前SP中
                    if (null != oldAccount) {
                        mPrefUtil.fillNewSP(oldAccount.uid);
                    }
                    Log.e("fGetMyProfile failed");
                }
                mMsgBox.dismissWait();
            }
        }.execute((Void)null);
    }

    @SuppressLint("HandlerLeak")
	private Handler mHandler = new Handler() {
		public void handleMessage(Message msg) {
//            String oldUId = "";
//            String newUId = "";

			switch (msg.what) {
			case MSG_LOGIN_SUCCESS:
                handleSuccessLogin(false, msg.obj);

//                oldUId = String.valueOf(msg.obj);
//                newUId = mWebIF.getPrefUid();
//                clearCachedValues(oldUId, newUId);
//
//                PrefUtil.getInstance(LoginActivity.this).setAutoLogin(false);
//                PrefUtil.getInstance(LoginActivity.this).setCompanyId(edtCompany.getText().toString().trim());
//
//                // 每次登录时，获取所有群组及成员
//                if (GlobalValue.RELEASE_AS_WOWTALKBIZ) {
//                    getAllGroupsAndMembersFromServer();
//                }
//
//				Intent mIntent = new Intent(LoginActivity.this, StartActivity.class);
//				startActivity(mIntent);
//
//                // 获取个人信息成功后，取消等待Dialog
//                mMsgBox.dismissWait();
//				finish();
				break;
			case MSG_AUTO_REGISTER_SUCCESS:
                handleSuccessLogin(true, msg.obj);
//                oldUId = String.valueOf(msg.obj);
//                newUId = mWebIF.getPrefUid();
//                clearCachedValues(oldUId, newUId);
//				PrefUtil.getInstance(LoginActivity.this).setAutoLogin(true);
//				Intent aIntent = new Intent(LoginActivity.this, StartActivity.class);
//				startActivity(aIntent);
//
//                mMsgBox.dismissWait();
//                finish();
				break;
			case MSG_AUTH:
                mMsgBox.dismissWait();
				alert(R.string.login_auth_error);
				break;
			case MSG_USER_NOT_EXISTS:
                mMsgBox.dismissWait();
				alert(R.string.login_user_not_exists);
				break;
			case MSG_LOGIN_FAILED:
                mMsgBox.dismissWait();
				alert(R.string.login_unknown_error);
				break;
			default:
				break;
			}
		}
	};

    private void initView() {
        mTitleBar = (RelativeLayout) findViewById(R.id.title_bar);
        mTitleBar.setVisibility(mIsAddAccount ? View.VISIBLE : View.GONE);
        mTitleBack = (ImageButton) findViewById(R.id.title_back);
        txtForgetPwd = (TextView) findViewById(R.id.forgotPassWord);
        btnLogin = (Button) findViewById(R.id.loginButton);
        mSignUp = findViewById(R.id.registerButton);
		edtAccount = (EditText) findViewById(R.id.accountInput);
		edtPassword = (EditText) findViewById(R.id.passWordInput);
        fieldClear = (ImageButton) findViewById(R.id.field_clear);

        mTitleBack.setOnClickListener(this);
		btnLogin.setOnClickListener(this);
		txtForgetPwd.setOnClickListener(this);
        mSignUp.setOnClickListener(this);
        fieldClear.setOnClickListener(this);

		edtAccount.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					edtAccount.setTextColor(getResources().getColor(R.color.login_hint_font));
				} else {
                    edtAccount.setTextColor(getResources().getColor(R.color.login_input_font));
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
		edtPassword.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
                    fieldClear.setVisibility(View.GONE);
					edtPassword.setTextColor(getResources().getColor(R.color.login_hint_font));
				} else {
                    fieldClear.setVisibility(View.VISIBLE);
                    edtPassword.setTextColor(getResources().getColor(R.color.login_input_font));
				}
			}
			
			@Override
			public void beforeTextChanged(CharSequence s, int start, int count,
					int after) {
				
			}
			
			@Override
			public void afterTextChanged(Editable s) {
				
			}
		});
	}

    public void onClick(View v) {
		switch (v.getId()) {
        case R.id.title_back:
            finish();
            break;
		case R.id.loginButton:
			login();
			break;
		case R.id.registerButton:
			fGotoRegister();
			break;
		case R.id.forgotPassWord:
			fGotoFetchPwd();
			break;
        case R.id.field_clear:
            edtPassword.setText("");
            break;
		default:
            mMsgBox.toast(R.string.not_implemented);
			break;
		}
	}

    private void login() {
        final String username = edtAccount.getText().toString().trim();
        final String pwdStr = edtPassword.getText().toString().trim();
        if (username.equals("")) {
            alert(R.string.login_user_cannot_be_null);
            return;
        }
        if (pwdStr.equals("")) {
            alert(R.string.login_pwd_cannot_be_null);
            return;
        }

        // 添加帐号时，判断此帐号是否已经存在于account_list中
        if (mIsAddAccount) {
            String tempWowtalkId = username;
            ArrayList<Account> accounts = mPrefUtil.getAccountList();
            for (Account account : accounts) {
                if (tempWowtalkId.equalsIgnoreCase(account.wowtalkId)) {
                    alert(R.string.manage_account_add_user_exists);
                    return;
                }
            }
        }

        mMsgBox.showWait();

        new Thread(new Runnable() {

            @Override
            public void run() {
                String oldUId = mPrefUtil.getUid();
                // 登录之前，获取当前的帐户Account信息
                Account account = Account.getAccountFromSP(mPrefUtil);
                if (null != account) {
                    // 获取Account的其他信息
                    Database database = new Database(LoginActivity.this);
                    AlbumCover albumCover = database.getAlbumCover(oldUId);
                    if (null != albumCover) {
                        account.albumCoverFileId = null == albumCover.fileId ? "" : albumCover.fileId;
                        account.albumCoverExt = null == albumCover.ext ? "" : albumCover.ext;
                    }
                }

                WowTalkWebServerIF webIF = WowTalkWebServerIF.getInstance(LoginActivity.this);
                if (mIsAddAccount) {
                    Database database = new Database(LoginActivity.this);
                    database.close();
                    Database.sFlagIndex++;
                    Connect2.sFlagIndex++;
                    // 先保存之前的SP信息
                    mPrefUtil.saveOldSP(oldUId);
                    mPrefUtil.clearOldSP();
                }

                int result = -1;
                result = webIF.fLogin(username, pwdStr, buddy);
                System.out.println(result);
                Message msg = Message.obtain();
                if (result == ErrorCode.OK) {
					msg.what = MSG_LOGIN_SUCCESS;
					msg.obj = (null == account) ? oldUId : account;
					mHandler.sendMessage(msg);
				} else {
				    if (result == ErrorCode.AUTH) {
				        msg.what = MSG_AUTH;
				        mHandler.sendMessage(msg);
				    } else if (result == ErrorCode.USER_NOT_EXISTS) {
				        msg.what = MSG_USER_NOT_EXISTS;
				        mHandler.sendMessage(msg);
				    } else {
				        msg.what = MSG_LOGIN_FAILED;
				        mHandler.sendMessage(msg);
				    }
				    // 登录失败，则将原来uid对应的SP拷贝到当前SP中
				    mPrefUtil.fillNewSP(oldUId);
				}
			}
		}).start();
		
	}

	private void fGotoRegister() {
		Intent registerIntent = new Intent(LoginActivity.this, RegisterActivity.class);
		startActivity(registerIntent);
	}
	
	private void fGotoFetchPwd() {
		Intent fetchIntent = new Intent(LoginActivity.this, Withdraw1Activity.class);
		startActivity(fetchIntent);
	}
	
	private void fAutoRegister() {
        mMsgBox.showWait();
		new Thread(new Runnable() {

			@Override
			public void run() {
                String oldUId = mPrefUtil.getPrevUid();
				int result = mWebIF.fLoginWithAutoCreatedUser(buddy);
				Message msg = Message.obtain();
				if (result == ErrorCode.OK) {
					msg.what = MSG_AUTO_REGISTER_SUCCESS;
					msg.obj = oldUId;
					mHandler.sendMessage(msg);
				} else {
                    LoginActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mMsgBox.dismissWait();
                        }

                    });
                    mMsgBox.toast(R.string.operation_failed);
                }
			}
			
		}).start();
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.login);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(PixelFormat.RGBA_8888);

        instance = this;
        mMsgBox = new MessageBox(this);

        String prompt = getIntent().getStringExtra(EXTRA_PROMPT);
        if (prompt != null) {
            mMsgBox.show(null, prompt);
        }

        mIsAddAccount = getIntent().getBooleanExtra(EXTRA_IS_ADD_ACCOUNT, false);

        initView();
        mWebIF = WowTalkWebServerIF.getInstance(LoginActivity.this);
        mPrefUtil = PrefUtil.getInstance(LoginActivity.this);
        // the value of Connect2.context will be changed in StartActivity
        Connect2.setContext(this);
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


	private void alert(int str) {
        if (isFinishing()) {
            // to avoid android.view.WindowManager$BadTokenException & Android
            // when displaying Dialogs From Background Threads
            return;
        }

		AlertDialog.Builder dialog = new AlertDialog.Builder(LoginActivity.this);
		dialog.setTitle(null).setMessage(str);
		dialog.setPositiveButton(R.string.ok, 
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
		dialog.create().show();
	}

	/**
	 * clear the cached global and sharedPrefrence values.
	 * @param oldUId
	 * @param newUId
	 */
	public static void clearCachedValues(String oldUId, String newUId) {
        Log.i("clear globalValues and SP values after login/register/auto_register, the oldUId is " + oldUId + ", the newUId is " + newUId);
        if (TextUtils.isEmpty(newUId) || !newUId.equals(oldUId)) {
            GlobalValue.clearGolableValues();
            mPrefUtil.setContactsUptodateAfterLogin(false);
            mPrefUtil.setGroupUptodate(false);
            mPrefUtil.setFavoritesUptodate(false);
            mPrefUtil.resetSysNoticeStatus();
        }
    }
}
