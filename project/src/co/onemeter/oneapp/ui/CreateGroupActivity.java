package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.PointF;
import android.graphics.drawable.BitmapDrawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import co.onemeter.oneapp.R;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.*;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.FileUtils;
import org.wowtalk.ui.msg.InputBoardManager;
import org.wowtalk.ui.msg.RoundedImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;

public class CreateGroupActivity extends Activity implements OnClickListener, InputBoardManager.ChangeToOtherAppsListener{
    private static final int REQ_PICK_PHOTO = 1;
    private static final int REQ_PICK_AREA = 2;
    public final static int PHOTO_THUMBNAIL_WIDTH = 200;
    public final static int PHOTO_THUMBNAIL_HEIGHT = 200;
    // resize photo to VGA size before sending
    public final static int PHOTO_SEND_WIDTH = 600;
    public final static int PHOTO_SEND_HEIGHT = 600;

    private static final int CATEGORY_NOT_SELECT = -1;

    private TextView  titleText;
	private ImageButton btnTitleBack;
	private ImageButton btnTitleConfirm;
	private LinearLayout mReqFocusLayout;
	private TextView txtGroupName;
	private TextView txtPlace;
	private TextView txtCategory;
	private EditText edtIntroduce;
    private RoundedImageView imgThumbnail;

    private RelativeLayout mPhoto;
	private LinearLayout mGroupName;
	private LinearLayout mPlace;
	private LinearLayout mCategory;

    private BottomButtonBoard bottomBoard;
    private MediaInputHelper mMediaInputHelper;

    private GroupChatRoom groupRoom;
    private boolean isCreatingNew;
    private boolean isThumbnailChanged = false;
    private int mCategoryWhich = CATEGORY_NOT_SELECT;
    private String[] mCategoryLocals;
    private String[] mCategoryServers;
    private String[] path = new String[2];
	
    private MessageBox mMsgBox;
    private Database mDBHelper;

    private void initCategoryArrays() {
        mCategoryLocals = getResources().getStringArray(R.array.group_category);
        mCategoryServers = getResources().getStringArray(R.array.group_category_msg);
        if (!isCreatingNew) {
            for (int i = 0; i < mCategoryServers.length; i++) {
                if (mCategoryServers[i].equals(groupRoom.category)) {
                    mCategoryWhich = i;
                    break;
                }
            }
        }
    }

	private void initView() {
        titleText = (TextView) findViewById(R.id.title_text);
		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
		txtGroupName = (TextView) findViewById(R.id.txt_group_name);
		txtPlace = (TextView) findViewById(R.id.txt_place);
		txtCategory = (TextView) findViewById(R.id.txt_category);
        imgThumbnail = (RoundedImageView) findViewById(R.id.img_photo);
        mReqFocusLayout = (LinearLayout) findViewById(R.id.request_focus_layout);
		mGroupName = (LinearLayout) findViewById(R.id.layout_group_name);
		mPlace = (LinearLayout) findViewById(R.id.layout_place);
		mCategory = (LinearLayout) findViewById(R.id.layout_category);
        mPhoto = (RelativeLayout) findViewById(R.id.layout_photo);
		edtIntroduce = (EditText) findViewById(R.id.edt_introduce);

        if (!isCreatingNew) {
            titleText.setText(getString(R.string.contacts_group_edit_info));
            txtGroupName.setText(groupRoom.groupNameOriginal);
            txtPlace.setText(groupRoom.place);
            if (mCategoryWhich != CATEGORY_NOT_SELECT) {
                txtCategory.setText(mCategoryLocals[mCategoryWhich]);
            }
            PhotoDisplayHelper.displayPhoto(this, imgThumbnail, R.drawable.default_avatar_90, groupRoom, true);
            edtIntroduce.setText(groupRoom.groupStatus);
        }
		
		btnTitleBack.setOnClickListener(this);
		btnTitleConfirm.setOnClickListener(this);
		mGroupName.setOnClickListener(this);
		mPlace.setOnClickListener(this);
		mCategory.setOnClickListener(this);
        mPhoto.setOnClickListener(this);
        imgThumbnail.setClickDim(true);
	}
	
	private void createGroup() {
		final String strGroupName = txtGroupName.getText().toString();
		final String strIntroduce = edtIntroduce.getText().toString();
		final String strCategory = (mCategoryWhich != CATEGORY_NOT_SELECT) ? mCategoryServers[mCategoryWhich] : "";
		final boolean isTemporaryGroup = false;
		final GroupChatRoom groupRoom = new GroupChatRoom();
        if (strGroupName.equals("")) {
            Toast.makeText(this, R.string.create_group_groupname_cannot_be_null,Toast.LENGTH_SHORT).show();
            return;
        }
		groupRoom.groupNameOriginal = strGroupName;
		groupRoom.groupStatus = strIntroduce;
		groupRoom.place = txtPlace.getText().toString();
		groupRoom.category = strCategory;
		PointF location = new PointF();
		location.x = 31.29f;
		location.y = 120.67f;
		groupRoom.location = location;
		
        mMsgBox.showWait();
		new AsyncTask<Void, Integer, String>(){

			@Override
			protected String doInBackground(Void... arg0) {
				String[] groupids = WebServerIF.getInstance(
                        CreateGroupActivity.this).fGroupChat_Create(
						strGroupName,
						isTemporaryGroup,
						groupRoom.place,
						strCategory,
						31.29f,
						120.67f,
						strIntroduce);
                if (null != groupids && groupids.length > 0 && null != groupids[0]) {
//                    // save the group in local db.
//                    groupRoom.groupID = groupids[0];
//                    groupRoom.shortGroupID = groupids[1];
//                    updateGroupThumbnail(groupRoom);
//                    mDBHelper.storeGroupChatRoom(groupRoom);

                    ArrayList<GroupChatRoom> groupList=new ArrayList<GroupChatRoom>();
                    WebServerIF.getInstance(CreateGroupActivity.this).fGroupChat_Search(groupids[1],groupList);
                    if(groupList.size() > 0) {
                        updateGroupThumbnail(groupList.get(0));
                        groupList.get(0).isMeBelongs=true;
                        mDBHelper.storeGroupChatRoom(groupList.get(0));
                        WebServerIF.getInstance(CreateGroupActivity.this).fGroupChat_GetMembers(groupList.get(0).groupID);
                    }
//                    WowTalkWebServerIF.getInstance(CreateGroupActivity.this).fGroupChat_GetMyGroups();
                    return groupids[0];
                }

                return null;
			}
			
			@Override
			protected void onPostExecute(String gid) {
                mMsgBox.dismissWait();
				if(gid != null) {
					finish();
                    // Go to details Activity of group after creating or updating it.
                    ContactGroupInfoActivity.launchForResult(CreateGroupActivity.this, gid, 0);
				} else {
					mMsgBox.toast(R.string.create_group_failed);
				}
			}
		}.execute((Void)null);
	}

    private void updateGroup() {
        groupRoom.groupNameOriginal = txtGroupName.getText().toString();
        groupRoom.place = txtPlace.getText().toString();
        groupRoom.category = (mCategoryWhich != CATEGORY_NOT_SELECT) ? mCategoryServers[mCategoryWhich] : "";
        groupRoom.groupStatus = edtIntroduce.getText().toString();
        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                if (isThumbnailChanged) {
                    updateGroupThumbnail(groupRoom);
                }
                mDBHelper.updateGroupChatRoom(groupRoom);
                return WebServerIF.getInstance(CreateGroupActivity.this)
                        .fGroupChat_UpdateInfo(groupRoom);
            }

            @Override
            protected void onPostExecute(Integer result) {
                mMsgBox.dismissWait();
                if (result == ErrorCode.OK) {
                    // update the display name of chatmessages.
                    mDBHelper.updateChatMessageDisplayNameWithUser(groupRoom.groupID, groupRoom.groupNameOriginal);
                    setResult(RESULT_OK);
                    ContactsFragment fragment = new ContactsFragment();
                    fragment.refresh();
                    finish();
                }
            }
        }.execute((Void)null);
    }

    private void chooseCategory() {
//        int index = 0;
//        String[] category = getResources().getStringArray(R.array.group_category);
//        String tempChosen = txtCategory.getText().toString();
//        for (int i = 0; i < category.length; i++) {
//            if (tempChosen.equals(category[i])) {
//                index = i;
//                break;
//            }
//        }
        AlertDialog.Builder dialog = new AlertDialog.Builder(this);
        int checkedItem = (mCategoryWhich == CATEGORY_NOT_SELECT) ? 0 : mCategoryWhich;
        dialog.setSingleChoiceItems(R.array.group_category, checkedItem, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mCategoryWhich = which;
                txtCategory.setText(mCategoryLocals[which]);
                dialog.dismiss();
            }
        }).show();
    }
	
	private void showInputNameDialog() {
	    // request focus, avoid the keyboard blocking the group introduction.
	    mReqFocusLayout.requestFocus();
	    mReqFocusLayout.requestFocusFromTouch();

		AlertDialog.Builder dialog = new AlertDialog.Builder(this);
		final EditText editGroupName = new EditText(this);
		editGroupName.setText(txtGroupName.getText());
		dialog.setTitle(getResources().getString(R.string.contact_groupcreate_input_group_name)).setView(editGroupName)
                .setPositiveButton(getResources().getString(R.string.ok),
				new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						txtGroupName.setText(editGroupName.getText().toString());
						// hide the soft keyboard
						hideSoftKeyboard(editGroupName.getWindowToken());
					}
			
		}).setNegativeButton(getResources().getString(R.string.cancel),
		        new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        hideSoftKeyboard(editGroupName.getWindowToken());
                    }
            }).show();
	}

	private void hideSoftKeyboard(IBinder iBinder) {
	    InputMethodManager inputManager = ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE));
        inputManager.hideSoftInputFromWindow(
                iBinder,
                InputMethodManager.HIDE_NOT_ALWAYS);
	}

    private void showPickPhotoDialog() {
        if (bottomBoard == null) {
            bottomBoard = new BottomButtonBoard(this, getWindow().getDecorView());
        }
        bottomBoard.clearView();
        bottomBoard.add(getString(R.string.image_take_photo), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        if (mMediaInputHelper == null)
                            mMediaInputHelper = new MediaInputHelper(CreateGroupActivity.this);
                        mMediaInputHelper.takePhoto(CreateGroupActivity.this, REQ_PICK_PHOTO);
                    }
                });
        bottomBoard.add(getString(R.string.image_pick_from_local), BottomButtonBoard.BUTTON_BLUE,
                new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        if (mMediaInputHelper == null)
                            mMediaInputHelper = new MediaInputHelper(CreateGroupActivity.this);
                        mMediaInputHelper.pickPhoto(CreateGroupActivity.this, REQ_PICK_PHOTO);
                    }
                });
        bottomBoard.addCancelBtn(getString(R.string.cancel));
        bottomBoard.show();
    }

    private void displayPhoto(String path) {
        if (path != null && new File(path).exists()) {
            try {
                imgThumbnail.setVisibility(View.VISIBLE);
                imgThumbnail.setScaleType(ImageView.ScaleType.CENTER_CROP);
                imgThumbnail.setImageDrawable(new BitmapDrawable(this.getResources(), path));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void updateGroupThumbnail(final GroupChatRoom group) {

        if (null == path[0] || null == path[1])
            return;

        final String groupId = group.groupID;

        group.setPhotoUploadedTimestamp(new Date().getTime() / 1000);

        // copy src file to disk cache
        String destFilePath = PhotoDisplayHelper.makeLocalPhotoPath(
                CreateGroupActivity.this, groupId);
        FileUtils.copyFile(path[0], destFilePath);
        destFilePath = PhotoDisplayHelper.makeLocalThumbnailPath(
                CreateGroupActivity.this, groupId);
        FileUtils.copyFile(path[1], destFilePath);

        // upload photo
        WebServerIF.getInstance(CreateGroupActivity.this)
                .fPostGroupPhoto(groupId, path[0], new NetworkIFDelegate() {
                    @Override
                    public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                    }

                    @Override
                    public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                    }

                    @Override
                    public void setProgress(int i, int i2) {
                    }
                }, false, 0, true);

        // upload thumb
        WebServerIF.getInstance(CreateGroupActivity.this)
                .fPostGroupPhoto(groupId, path[1], new NetworkIFDelegate() {
                    @Override
                    public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                        WebServerIF.getInstance(CreateGroupActivity.this)
                                .fGroupChat_UpdateInfo(group);
                    }

                    @Override
                    public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                    }

                    @Override
                    public void setProgress(int i, int i2) {
                    }
                }, true, 0, true);
    }

    @Override
	public void onClick(View v) {
		switch(v.getId()) {
		case R.id.title_back:
			finish();
			break;
		case R.id.title_confirm:
            if (isCreatingNew) {
                createGroup();
            } else {
                updateGroup();
            }
			break;
		case R.id.layout_group_name:
			showInputNameDialog();
			break;
        case R.id.layout_place:
            Intent placeIntent = new Intent(CreateGroupActivity.this, ChooseAreaActivity.class);
            startActivityForResult(placeIntent, REQ_PICK_AREA);
            break;
        case R.id.layout_category:
            chooseCategory();
            break;
        case R.id.layout_photo:
            showPickPhotoDialog();
            break;
		default:
			break;
		}
		
	}

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.create_group);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
		mDBHelper = new Database(CreateGroupActivity.this);
        groupRoom = getIntent().getParcelableExtra("group");
        if (groupRoom == null) {
            isCreatingNew = true;
        } else {
            isCreatingNew = false;
        }

        initCategoryArrays();
		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        AppStatusService.setIsMonitoring(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQ_PICK_PHOTO:
                if (resultCode == RESULT_OK) {
                    isThumbnailChanged = true;
                    if (mMediaInputHelper != null &&
                            mMediaInputHelper.handleImageResult(this, data,
                                    PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
                                    PHOTO_THUMBNAIL_WIDTH, PHOTO_THUMBNAIL_HEIGHT,
                                    path));
                    displayPhoto(path[0]);
                }
                break;
            case REQ_PICK_AREA:
                if (resultCode == RESULT_OK) {
                    txtPlace.setText(data.getStringExtra("text"));
                }
                break;
            default:
                break;
        }
    }

    @Override
    public void changeToOtherApps() {
        AppStatusService.setIsMonitoring(false);
    }
}
