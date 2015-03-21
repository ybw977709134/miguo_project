package co.onemeter.oneapp.ui;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnFocusChangeListener;
import android.view.View.OnTouchListener;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;

import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;

import org.wowtalk.api.*;
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
    /** 可选，自动采用该用户名登录。*/
    public static final String EXTRA_USERNAME = "username";
    /** 可选，自动采用该密码登录。*/
    public static final String EXTRA_PASSWORD = "password";

	private static final int MSG_LOGIN_SUCCESS = 100;
	private static final int MSG_AUTO_REGISTER_SUCCESS = 101;
	private static final int MSG_USER_NOT_EXISTS = 104;
	private static final int MSG_AUTH = 105;
	private static final int MSG_LOGIN_FAILED = 106;

    private static final int REQ_SCAN = 123;
    
    
    private RelativeLayout layout_login;
    private EditText edtAccount;
	private EditText edtPassword;
    private ImageButton fieldClear_account;
    private ImageButton fieldClear_passWord;

	private Buddy buddy = new Buddy();
	
    private MessageBox mMsgBox;
    private static LoginActivity instance;
    private static PrefUtil mPrefUtil;
    private WowTalkWebServerIF mWebIF;
    
    private InputMethodManager mInputMethodManager;

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

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Integer>() {

            @Override
            protected Integer doInBackground(Void... params) {
                int ret = WowTalkWebServerIF.getInstance(LoginActivity.this).fGetMyProfile();

                if (ErrorCode.OK == ret) {
                    WowTalkWebServerIF webIF = WowTalkWebServerIF.getInstance(LoginActivity.this);
                    // login 成功后再 logout
                    if (mIsAddAccount) {
                        webIF.logoutByUid(oldAccount.uid, oldAccount.password);
                    }

                    // Company ID 是 Wowtalk biz 的概念，
                    // 为了在“一米家校”中以最小改动复用 Wowtalk biz 的代码，
                    // 这里硬编码一个 dummy company ID.
                    String company = "onemeter";
                    mPrefUtil.setAutoLogin(isAutoLogin);
                    if (!isAutoLogin) {
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
                if (ErrorCode.OK == result) {
                    GlobalValue.IS_BOOT_FROM_LOGIN = true;
                    if (!mIsAddAccount) {
                        Intent newIntent = new Intent(LoginActivity.this, StartActivity.class);
                        newIntent.putExtra(StartActivity.KEY_IS_START_FROM_LOGIN, true);
                        newIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(newIntent);
                    }
                } else {
                    // 登录失败，则将原来uid对应的SP拷贝到当前SP中
                    if (null != oldAccount) {
                        mPrefUtil.fillNewSP(oldAccount.uid);
                    }
                    Log.e("fGetMyProfile failed");
                }
                mMsgBox.dismissWait();
            }
        });
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
				break;
			case MSG_AUTH:
                mMsgBox.dismissWait();
				alert(LoginActivity.this,getResources().getString(R.string.login_unknown_error),getResources().getString(R.string.login_auth_error));
				break;
			case MSG_USER_NOT_EXISTS:
                mMsgBox.dismissWait();
//				alert(R.string.login_user_not_exists);
				alert(LoginActivity.this,getResources().getString(R.string.login_unknown_error),getResources().getString(R.string.login_user_not_exists));
				break;
			case MSG_LOGIN_FAILED:
                mMsgBox.dismissWait();
//				alert(R.string.login_unknown_error);
				alert(LoginActivity.this,getResources().getString(R.string.login_unknown_error),getResources().getString(R.string.login_net_error));
				break;
			default:
				break;
			}
		}
	};

    private void initView() {
        AQuery q = new AQuery(this);
        
        layout_login = (RelativeLayout) findViewById(R.id.layout_login);
        
        layout_login.setOnTouchListener(new OnTouchListener() {
			
			@Override
			public boolean onTouch(View v, MotionEvent event) {
				layout_login.setFocusable(true);
				layout_login.setFocusableInTouchMode(true);
				layout_login.requestFocus();
				fieldClear_account.setVisibility(View.GONE);
				fieldClear_passWord.setVisibility(View.GONE);
				return false;
			}
		});
       
		edtAccount = (EditText) findViewById(R.id.accountInput);
		
		edtAccount.setFocusable(true);
		edtAccount.setFocusableInTouchMode(true); 
		edtAccount.requestFocus();
		
		edtPassword = (EditText) findViewById(R.id.passWordInput);
		
		fieldClear_account = (ImageButton) findViewById(R.id.field_clear_account);
		fieldClear_passWord = (ImageButton) findViewById(R.id.field_clear_password);
        

        q.find(R.id.forgotPassWord).clicked(this);
        q.find(R.id.login_username).clicked(this);
//        q.find(R.id.login_qrcode).clicked(this);
        q.find(R.id.btn_signup).clicked(this);
        fieldClear_account.setOnClickListener(this);
        fieldClear_passWord.setOnClickListener(this);
        
        
        edtAccount.addTextChangedListener(new TextWatcher() {
			
			@Override
			public void onTextChanged(CharSequence s, int start, int before, int count) {
				if (s.length() == 0) {
					fieldClear_account.setVisibility(View.GONE);
					
				} else {
					fieldClear_account.setVisibility(View.VISIBLE);
					
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
					fieldClear_passWord.setVisibility(View.GONE);
				} else {
					fieldClear_passWord.setVisibility(View.VISIBLE);
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
		
        edtPassword.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    login();
                    return true;
                }
                return false;
            }
        });
        
        
        edtAccount.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					edtAccount.setText("");
				} else {
					fieldClear_account.setVisibility(View.GONE);
				}
				
			}
		});
        
        //每次密码框重新获得焦点时，清空密码框
        edtPassword.setOnFocusChangeListener(new OnFocusChangeListener() {
			
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (hasFocus) {
					edtPassword.setText("");
				} else {
					fieldClear_passWord.setVisibility(View.GONE);
				}
				
			}
		});
	}

    public void onClick(View v) {
		switch (v.getId()) {
        case R.id.title_back:
            finish();
            break;
		case R.id.login_username:
			login();
			break;
//		case R.id.login_qrcode:
//        {
//            Intent i = new Intent(this, ScanQRCodeActivity.class)
//                            .putExtra(ScanQRCodeActivity.ACTIVITY_ARG_WITH_LAYOUT, true)
//                            .putExtra(ScanQRCodeActivity.ACTIVITY_ARG_LAYOUT_ID, R.layout.scan_qr_code_layout)
//                            .putExtra(ScanQRCodeActivity.ACTIVITY_ARG_FINISH_AFTER_DECODE, false);
//            ThemeHelper.putExtraCurrThemeResId(i, this);
//            startActivityForResult(i, REQ_SCAN);
//            break;
//        }
        case R.id.btn_signup:
            startActivity(new Intent(this, RegisterActivity.class));
            break;
		case R.id.forgotPassWord:
//			fGotoFetchPwd();
			//找回密码
			showPopupMenu();
			break;
        case R.id.field_clear_account:
        	edtAccount.setText("");
            break;
        case R.id.field_clear_password:
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
//          alert(R.string.login_user_cannot_be_null);
            alert(LoginActivity.this,getResources().getString(R.string.login_unknown_error),getResources().getString(R.string.login_user_cannot_be_null));
            return;
        }
        if (pwdStr.equals("")) {
//            alert(R.string.login_pwd_cannot_be_null);
            alert(LoginActivity.this,getResources().getString(R.string.login_unknown_error),getResources().getString(R.string.login_pwd_cannot_be_null));
            return;
        }
        login(username, pwdStr);


    }

    private void login(final String username, final String pwdStr) {
        // 添加帐号时，判断此帐号是否已经存在于account_list中
        if (mIsAddAccount) {
            String tempUsername = username;
            ArrayList<Account> accounts = mPrefUtil.getAccountList();
            for (Account account : accounts) {
                if (tempUsername.equalsIgnoreCase(account.username)) {
//                    alert(R.string.manage_account_add_user_exists);
                	findViewById(R.id.bodyLayout).setVisibility(View.INVISIBLE);
                	
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

    /**
     * 跳转到找回密码的界面
     */
    private void fGotoFetchPwd() {
		Intent fetchIntent = new Intent(LoginActivity.this, FindPasswordEmialActivity.class);
		startActivity(fetchIntent);
//    	Toast.makeText(LoginActivity.this, "功能正在实现中..", Toast.LENGTH_SHORT).show();
	}

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(PixelFormat.RGBA_8888);
        
        //点击屏幕的其他的地方可以收起键盘
        mInputMethodManager = (InputMethodManager) this.getSystemService(this.INPUT_METHOD_SERVICE);

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

        // 处理自动登录
        Bundle extras = getIntent().getExtras();
        if (extras != null) {
        	if (prompt != null) {//如果帐号已存在，显示登陆界面
        		findViewById(R.id.bodyLayout).setVisibility(View.VISIBLE);
            		
        	} else {//从注册成功后自动跳转到登录页，隐藏登录界面，显示正在登录中...
            	findViewById(R.id.bodyLayout).setVisibility(View.INVISIBLE);
            	findViewById(R.id.layout_login).setBackgroundColor(Color.TRANSPARENT);
        	}

            String username = extras.getString(EXTRA_USERNAME);
            if (username != null) {
                edtAccount.setText(username);
                String password = extras.getString(EXTRA_PASSWORD);
                if (password != null) {
                    edtPassword.setText(password);
                    login(username, password);
                }
            }
        }   
    }
    
    @Override
    public boolean onTouchEvent(MotionEvent event) {
    	// TODO Auto-generated method stub
    	  if(event.getAction() == MotionEvent.ACTION_DOWN){  
    		  if(getCurrentFocus()!=null && getCurrentFocus().getWindowToken()!=null){  
    			  mInputMethodManager.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), InputMethodManager.HIDE_NOT_ALWAYS);  
    			  }  
    		  }
    	return super.onTouchEvent(event);
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


	private void alert(Context context, String title, String message) {
        if (isFinishing()) {
            // to avoid android.view.WindowManager$BadTokenException & Android
            // when displaying Dialogs From Background Threads
            return;
        }

		AlertDialog.Builder dialog = new AlertDialog.Builder(LoginActivity.this);
		dialog.setTitle(title).setMessage(message);
		dialog.setPositiveButton(R.string.ok, 
				new DialogInterface.OnClickListener() {
					
					@Override
					public void onClick(DialogInterface dialog, int which) {
						
					}
				});
		dialog.create().show();
        
//        View view;
//		view = LayoutInflater.from(context).inflate(R.layout.custom_dialog, null);
//		TextView button_ok = (TextView) view.findViewById(R.id.textView_ok);
//		TextView button_cancel = (TextView) view.findViewById(R.id.textView_cancel);
//		ImageView imageView_dialogLine2 = (ImageView) view.findViewById(R.id.imageView_dialogLine2);
//		button_cancel.setVisibility(View.GONE);
//		imageView_dialogLine2.setVisibility(View.GONE);
//		
//		
//		((TextView) view.findViewById(R.id.textView_title)).setText(title);//标题
//		((TextView) view.findViewById(R.id.textView_message)).setText(message);//消息内容
//		
//		final Dialog dialog = new Dialog(context);
//		dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
//		
//		WindowManager.LayoutParams params = dialog.getWindow().getAttributes(); // 得到AlertDialog属性
//        params.gravity = Gravity.CENTER; // 显示其显示在中间
//        params.width = (int) (((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getWidth() * 0.5); // 设置对话框的宽度为手机屏幕的0.8，MyTool.getWidth()自己写的一个方法，获取手机屏幕的宽度
//        params.height = (int) (((WindowManager) context.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay().getHeight() * 0.2);// 设置对话框的高度为手机屏幕的0.25
//        dialog.getWindow().setAttributes(params); //设置属性
//		dialog.getWindow().setContentView(view);// 把自定义view加上去
//		
//		button_ok.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View arg0) {
//				dialog.dismiss();
//			}
//		});
//		
//		button_cancel.setOnClickListener(new OnClickListener() {
//			
//			@Override
//			public void onClick(View arg0) {
//				dialog.dismiss();
//			}
//		});
//		dialog.show();
	}
	
    private void showPopupMenu() {
        final BottomButtonBoard bottomBoard = new BottomButtonBoard(this, findViewById(R.id.layout_login));

        //邮箱找回
        bottomBoard.add(getString(R.string.login_findPassWord_email), BottomButtonBoard.BUTTON_BLUE, new OnClickListener() {
            @Override
            public void onClick(View v) {
                bottomBoard.dismiss();
                fGotoFetchPwd();
            }
        });

        //close popupMenu
        bottomBoard.addCancelBtn(getString(R.string.login_findPassWord_cancel));
        bottomBoard.show();
    }

}
