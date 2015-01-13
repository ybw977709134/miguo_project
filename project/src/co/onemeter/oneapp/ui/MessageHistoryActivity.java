package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.MessageDetailAdapter.MessageDetailListener;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.api.*;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.HeightAwareRelativeLayout;
import org.wowtalk.ui.msg.HeightAwareRelativeLayout.IKeyboardStateChangedListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class MessageHistoryActivity extends Activity implements OnClickListener, MessageDetailListener, IKeyboardStateChangedListener{

    /**
     * 每次下载的页数，免得每次下一页都去请求服务器
     */
    private static final int PAGES_PER_DOWNLOAD = 5;
    private static final int ITEMS_PER_PAGE = 6;
    private boolean mIsGroupChat;
    private String mTargetId;
    private String mTargetName;
    private ArrayList<ChatMessage> mMsgHistorys;
    private MessageDetailAdapter mAdapter;

    private HeightAwareRelativeLayout mRootLayout;
    private TextView mTitleView;
//    private ImageButton mMoreImgBtn;
    private LinearLayout mPageLayout;
    private ImageView mFirstPageImage;
    private ImageView mPreviousImage;
    private ImageView mNextImage;
    private ImageView mLastPageImage;
    private EditText mCurrentPageView;
    private TextView mTotalPageView;
    private ListView mMsgHistoryListView;

    private Database mDBHelper;
    private WowTalkWebServerIF mWebIF;
    private MessageBox mMsgBox;
    private Handler mHandler = new Handler();
    private int mCurrentPage;
    private int mTotalPages;
    private HashSet<String> mDownloadedPages = new HashSet<String>();
    private boolean mIsFirstOnResume = true;

//    private TextWatcher mTextWatcher = new TextWatcher() {
//        @Override
//        public void onTextChanged(CharSequence s, int start, int before, int count) {
//        }
//
//        @Override
//        public void beforeTextChanged(CharSequence s, int start, int count,
//                int after) {
//        }
//
//        @Override
//        public void afterTextChanged(Editable s) {
//            int number = 0;
//            try {
//                number = Integer.parseInt(s.toString().trim());
//            } catch (Exception exception) {
//            }
//            if (number != 0) {
//                mCurrentPage = number > mTotalPages ? mTotalPages : number;
//                showMessageHistory();
//            }
//        }
//    };

    private IDBTableChangeListener mMessageHistoryObserver = new IDBTableChangeListener() {
        public void onDBTableChanged(String tableName) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    // 有资源下载完成，重新刷新当前页面
                    showMessageHistory();
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_message_history);

        boolean isNormalGroup = getIntent().getBooleanExtra(MessageComposerActivity.KEY_TARGET_IS_NORMAL_GROUP, false);
        boolean isTempGroup = getIntent().getBooleanExtra(MessageComposerActivity.KEY_TARGET_IS_TMP_GROUP, false);
        mIsGroupChat = isNormalGroup || isTempGroup;
        mTargetId = getIntent().getStringExtra(MessageComposerActivity.KEY_TARGET_UID);
        mTargetName = getIntent().getStringExtra(MessageComposerActivity.KEY_TARGET_DISPLAYNAME);
        mDBHelper = new Database(this);
        mMsgBox = new MessageBox(this);
        mWebIF = WowTalkWebServerIF.getInstance(this);
        initView();

        getMessageHistoryCounts();
    }

    @Override
    protected void onResume() {
        super.onResume();

        AppStatusService.setIsMonitoring(true);
        if (!mIsFirstOnResume) {
            showMessageHistory();
        }
        mIsFirstOnResume = false;

        Database.addDBTableChangeListener(Database.TBL_MESSAGES_HISTORY,mMessageHistoryObserver);
    }

    @Override
    protected void onPause() {
        super.onPause();

        Database.removeDBTableChangeListener(mMessageHistoryObserver);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        mDBHelper.deleteAllMessageHistoryWithRes();
        if (null != mAdapter) {
            mAdapter.stopPlayingVoice();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != mAdapter) {
            mAdapter.releaseRes();
        }
        mDBHelper.deleteAllMessageHistoryWithRes();
        Log.d("#onDestory, delete all message history");
    }

    private void initView() {
        mRootLayout = (HeightAwareRelativeLayout)findViewById(R.id.root);
        mTitleView = (TextView)findViewById(R.id.target_name);
        if (!TextUtils.isEmpty(mTargetName)) {
            mTitleView.setText(mTargetName);
        }
        mMsgHistoryListView = (ListView) findViewById(R.id.message_history);
        mMsgHistoryListView.setDividerHeight(0);
        mPageLayout = (LinearLayout)findViewById(R.id.layout_page);
        mFirstPageImage = (ImageView)findViewById(R.id.first_page);
        mPreviousImage = (ImageView)findViewById(R.id.previous_page);
        mNextImage= (ImageView)findViewById(R.id.next_page);
        mLastPageImage = (ImageView)findViewById(R.id.last_page);
        mCurrentPageView = (EditText)findViewById(R.id.current_page);
        mTotalPageView = (TextView)findViewById(R.id.total_page);

        mRootLayout.setKeyboardChangedListener(this);
        mPageLayout.setOnClickListener(this);
        mFirstPageImage.setOnClickListener(this);
        mPreviousImage.setOnClickListener(this);
        mNextImage.setOnClickListener(this);
        mLastPageImage.setOnClickListener(this);
        findViewById(R.id.img_back).setOnClickListener(this);
    }

    private void showMessageHistory() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                // the mCurrentPage is the new one
                mMsgHistorys = mDBHelper.getMessageHistoryByTargetId(mTargetId,
                        (mCurrentPage - 1) * ITEMS_PER_PAGE,
                        ITEMS_PER_PAGE);
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {

                // TODO mMsgHistorys==null
                if (null == mAdapter) {
                    mAdapter = new MessageDetailAdapter(MessageHistoryActivity.this, mMsgHistorys, true, mHandler, MessageHistoryActivity.this);
                    mMsgHistoryListView.setAdapter(mAdapter);
                } else {
                    mAdapter.setDataSource(mMsgHistorys);
                    mAdapter.notifyDataSetChanged();
                }

                dealPreOrNextPageBtn();

                // set the current/total page counts
                mCurrentPageView.setText(String.valueOf(mCurrentPage));
                mTotalPageView.setText(String.format("/%d", mTotalPages));
            }
        });
    }

    /**
     * 下载当前页附近的5页数据
     * <p>如当前页为10,下载[5,10]；当前页为11,下载[11,15]
     */
    private void downloadHistory() {
        if (!mMsgBox.isWaitShowing()) {
            mMsgBox.showWait();
        }
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, HashMap<String, Object>>() {
            @Override
            protected HashMap<String, Object> doInBackground(Void... params) {
                int offset = 0;
                if (mCurrentPage != 0) {
                    if (mCurrentPage % PAGES_PER_DOWNLOAD == 0) {
                        offset = mCurrentPage - PAGES_PER_DOWNLOAD;
                    } else {
                        offset = PAGES_PER_DOWNLOAD * (mCurrentPage / PAGES_PER_DOWNLOAD);
                    }
                }
                return mWebIF.getChatHistory(mIsGroupChat,
                        PAGES_PER_DOWNLOAD * ITEMS_PER_PAGE,
                        offset * ITEMS_PER_PAGE, true, mTargetId);
            }

            @Override
            protected void onPostExecute(HashMap<String, Object> resultMap) {
                mMsgBox.dismissWait();
                int resultCode = (Integer) resultMap.get("code");
                Log.d("download message history, the result code is " + resultCode);
                if (ErrorCode.OK == resultCode) {
                    // 下载的页数存入mDownloadedPages缓存
                    ArrayList<Integer> localItems = (ArrayList<Integer>) resultMap.get("value");
                    int page = 0;
                    for (int localItem : localItems) {
                        page = localItem / ITEMS_PER_PAGE;
                        page += (localItem % ITEMS_PER_PAGE == 0) ? 0 : 1;
                        mDownloadedPages.add(String.valueOf(page));
                    }
                    showMessageHistory();
                }
            }
        });
    }

    /**
     * 从服务器获取记录条数，并据此下载最后n条数据并显示(先删除本地原有的数据)
     */
    private void getMessageHistoryCounts() {
        mMsgBox.showWait();
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, HashMap<String, Integer>>() {
            @Override
            protected HashMap<String, Integer> doInBackground(Void... params) {
                // 每次进入时，先删除本地的数据
                mDBHelper.deleteAllMessageHistoryWithRes();
                return mWebIF.getChatHistoryCount(mTargetId);
            }

            @Override
            protected void onPostExecute(HashMap<String, Integer> result) {

                int errno = result.get("code");
                if (ErrorCode.OK == errno) {
                    int count = result.get("value");
                    Log.d("download message history, the message history count of uid(" + mTargetId + ") is " + count);
                    if (count != 0) {
                        mTotalPages = (count % ITEMS_PER_PAGE == 0) ? count / ITEMS_PER_PAGE : count / ITEMS_PER_PAGE + 1;
                        mCurrentPage = mTotalPages;
                        downloadHistory();
                    } else {
                        mMsgBox.dismissWait();
                        mTotalPages = 1;
                        mCurrentPage = 1;
                        mMsgBox.toast(R.string.messagehistory_no_history_server);
                        showMessageHistory();
                    }
                } else {
                    mMsgBox.dismissWait();
                }
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
        case R.id.layout_page:
            mCurrentPageView.requestFocus();
            mCurrentPageView.setSelection(mCurrentPageView.length());
            InputMethodManager manager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            manager.showSoftInput(mCurrentPageView, 0);
            break;
        case R.id.first_page:
            mCurrentPage = 1;
            skipToNewPage();
            break;
        case R.id.previous_page:
            --mCurrentPage;
            skipToNewPage();
            break;
        case R.id.next_page:
            ++mCurrentPage;
            skipToNewPage();
            break;
        case R.id.last_page:
            mCurrentPage = mTotalPages;
            skipToNewPage();
            break;
        case R.id.img_back:
            finish();
            break;
        default:
            break;
        }
    }

    private void skipToNewPage() {
        // whether the target page has been downloaded
        if (mDownloadedPages.contains(String.valueOf(mCurrentPage))) {
            showMessageHistory();
        } else {
            downloadHistory();
        }

    }

    private void dealPreOrNextPageBtn() {
        if (mTotalPages == 1) {
            // mCurrentPage must be 1.
            mFirstPageImage.setEnabled(false);
            mPreviousImage.setEnabled(false);
            mNextImage.setEnabled(false);
            mLastPageImage.setEnabled(false);
        } else if(mCurrentPage <= 1) {
            mFirstPageImage.setEnabled(false);
            mPreviousImage.setEnabled(false);
            mNextImage.setEnabled(true);
            mLastPageImage.setEnabled(true);
        } else if (mCurrentPage >= mTotalPages) {
            mFirstPageImage.setEnabled(true);
            mPreviousImage.setEnabled(true);
            mNextImage.setEnabled(false);
            mLastPageImage.setEnabled(false);
        } else {
            mFirstPageImage.setEnabled(true);
            mPreviousImage.setEnabled(true);
            mNextImage.setEnabled(true);
            mLastPageImage.setEnabled(true);
        }
    }

    @Override
    public void onKeyboardStateChanged(int state) {
        switch (state) {
        case HeightAwareRelativeLayout.KEYBOARD_STATE_HIDE:
            int number = 0;
            String targetPage = "";
            try {
                targetPage = mCurrentPageView.getText().toString();
                number = Integer.parseInt(targetPage);
            } catch (Exception exception) {
                Log.e("hide the keyboard, the target page is " + targetPage);
            }
            Log.i("hide the keyboard, the target page is " + number);
            if (mCurrentPage != number && number != 0) {
                mCurrentPage = number > mTotalPages ? mTotalPages : number;
                skipToNewPage();
            }

            mCurrentPageView.setTextColor(getResources().getColor(R.color.black_30));
            mCurrentPageView.clearFocus();
            mFirstPageImage.setVisibility(View.VISIBLE);
            mPreviousImage.setVisibility(View.VISIBLE);
            mNextImage.setVisibility(View.VISIBLE);
            mLastPageImage.setVisibility(View.VISIBLE);
            break;
        case HeightAwareRelativeLayout.KEYBOARD_STATE_SHOW:
            mCurrentPageView.setTextColor(getResources().getColor(R.color.blue));
            mCurrentPageView.setSelection(mCurrentPageView.getText().length());
            mFirstPageImage.setVisibility(View.INVISIBLE);
            mPreviousImage.setVisibility(View.INVISIBLE);
            mNextImage.setVisibility(View.INVISIBLE);
            mLastPageImage.setVisibility(View.INVISIBLE);
            break;
        default:
            break;
        }
    }

    @Override
    public void onViewItemClicked() {
    }

    @Override
    public void onMessageTextClicked(ChatMessage message, String[] phones, String[] links) {
    }

    @Override
    public void onConfirmOutgoingCall() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.call);
        builder.setMessage(mTargetName + " ?");

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                CallMainActivity.startNewOutGoingCall(MessageHistoryActivity.this, mTargetId, mTargetName, false);
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
            }
        });
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public void onResendMessage(ChatMessage msg) {
        //To change body of implemented methods use File | Settings | File Templates.
    }
}
