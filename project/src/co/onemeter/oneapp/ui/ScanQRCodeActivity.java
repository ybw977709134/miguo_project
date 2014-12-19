package co.onemeter.oneapp.ui;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.Toast;
import co.onemeter.oneapp.utils.ThemeHelper;

import com.zxing.activity.CaptureActivity;
import com.zxing.decoding.CaptureActivityHandler;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.ErrorCode;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.msg.BmpUtils;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.contacts.model.Person;
import co.onemeter.oneapp.utils.StringToJason;
import co.onemeter.oneapp.utils.Utils;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-8-30
 * Time: 下午2:16
 * To change this template use File | Settings | File Templates.
 */
public class ScanQRCodeActivity extends CaptureActivity implements View.OnClickListener{

    private ImageView ivInitLighterAnimMask;
    private ImageView ivBottomFlash;
    private boolean isInitAnimComingLighterShuttered=false;
    private int initAnimCurAlpha=255;
    private final static int INIT_ANIM_ALPHA_LIMIT=0;
    private final static int INIT_ANIM_ALPHA_STEP=10;
    private final static int INIT_ANIM_ALPHA_CHANGE_INTERVAL=30;

    private final static int MSG_ID_INIT_ANIM_MASK_FADE_OUT=110;
    private final static int MSG_ID_INIT_ANIM_DOOR_OPEN=111;
    private Handler myHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case MSG_ID_INIT_ANIM_MASK_FADE_OUT:
//                    Log.i("init anim mask alpha set to " + initAnimCurAlpha);
                    ivInitLighterAnimMask.setAlpha(initAnimCurAlpha);

                    initAnimCurAlpha -= INIT_ANIM_ALPHA_STEP;
                    if (initAnimCurAlpha >= INIT_ANIM_ALPHA_LIMIT) {
                        myHandler.sendEmptyMessageDelayed(MSG_ID_INIT_ANIM_MASK_FADE_OUT, INIT_ANIM_ALPHA_CHANGE_INTERVAL);
                    } else {
                        myHandler.sendEmptyMessage(MSG_ID_INIT_ANIM_DOOR_OPEN);
                    }
                    break;
                case MSG_ID_INIT_ANIM_DOOR_OPEN:
                    hideInitAnimLayout();

                    findViewById(R.id.init_anim_door_open).setVisibility(View.VISIBLE);
                    findViewById(R.id.qr_scan_layout).setVisibility(View.VISIBLE);

                    beginOpenDoorAnim();
                    break;
            }
        }
    };

    private boolean isDoorTopAnimFinish=false;
    private boolean isDoorBottomAnimFinish=false;

    private boolean isFlashOn=false;

    private MessageBox mMsgbBox;
    private WowTalkWebServerIF mWebif = null;
//    private Camera camera;

    private final static int INTENT_ID_PICK_PHOTO=3;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        ThemeHelper.setTheme(this, getIntent().getExtras());

        super.onCreate(savedInstanceState);
//        setContentView(R.layout.scan_qr_code_layout); // passed to parent

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgbBox = new MessageBox(this);
        mWebif = WowTalkWebServerIF.getInstance(this);

        initView();
        
        
    }

    private void initView() {
        ivInitLighterAnimMask=(ImageView) findViewById(R.id.qr_init_lighter_mask_image);
        ivBottomFlash=(ImageView) findViewById(R.id.scan_qr_code_bottom_btn_flash);

        findViewById(R.id.scan_qr_code_bottom_btn_photo).setOnClickListener(this);
        findViewById(R.id.scan_qr_code_bottom_btn_flash).setOnClickListener(this);
        findViewById(R.id.scan_qr_code_bottom_btn_qrcode).setOnClickListener(this);
        findViewById(R.id.title_back).setOnClickListener(this);
;
    }

    private void beginOpenDoorAnim() {
        ImageView doorTop=(ImageView) findViewById(R.id.init_anim_door_top);
        ImageView doorBottom=(ImageView) findViewById(R.id.init_anim_door_bottom);

        //top anim
        Animation doorTopAnim = AnimationUtils.loadAnimation(this, R.anim.scan_qr_code_door_top_anim);
        doorTopAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.i("door top anim finish");
                isDoorTopAnimFinish=true;
                triggerQRScanLayout();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        doorTop.startAnimation(doorTopAnim);

        //bottom anim
        Animation doorBottomAnim = AnimationUtils.loadAnimation(this, R.anim.scan_qr_code_door_bottom_anim);
        doorBottomAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                Log.i("door bottom anim finish");
                isDoorBottomAnimFinish=true;
                triggerQRScanLayout();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
        doorBottom.startAnimation(doorBottomAnim);
    }

    private void setFlashBtnStatus() {
//        releaseCamera();

        if(isFlashOn) {
            ivBottomFlash.setBackgroundResource(R.drawable.qr_scan_flash_on);

            enableFlashLight();
//            if (isFlashSupported()) {
//                try{
//                    camera = Camera.open();
//                    Camera.Parameters parameters = camera.getParameters();
//                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_ON); // 使用闪光灯
//                    parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH); // 开启火炬模式常亮
//                    camera.setParameters(parameters);
//                }catch(Exception e){
//                    e.printStackTrace();
//                }
//            }
        } else {
            disableFlashLight();
            ivBottomFlash.setBackgroundResource(R.drawable.qr_scan_flash_selector);
        }
    }

    private void triggerQRScanLayout() {
        if(isDoorTopAnimFinish && isDoorBottomAnimFinish) {
            Log.i("both anim finish");
            findViewById(R.id.init_anim_door_open).setVisibility(View.GONE);

            findViewById(R.id.qr_scan_layout).setVisibility(View.VISIBLE);

            setFlashBtnStatus();
        }
    }

    private void shutterInitAnim() {
        isInitAnimComingLighterShuttered=true;
        findViewById(R.id.init_anim_coming_lighter_layout).setVisibility(View.VISIBLE);

        myHandler.sendEmptyMessageDelayed(MSG_ID_INIT_ANIM_MASK_FADE_OUT,INIT_ANIM_ALPHA_CHANGE_INTERVAL);
    }

    private void hideInitAnimLayout() {
        findViewById(R.id.init_anim_coming_lighter_layout).setVisibility(View.GONE);
    }

    @Override
    public void onResume() {
        super.onResume();

        if(!isInitAnimComingLighterShuttered) {
            shutterInitAnim();
        } else {
            hideInitAnimLayout();
            triggerQRScanLayout();
        }
    }

//    private void releaseCamera() {
//        if(null != camera) {
//            Camera.Parameters parameters = camera.getParameters();
//            parameters.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
//            camera.setParameters(parameters);
//            camera.release();
//            camera=null;
//        }
//    }

    @Override
    public void onPause() {
        super.onPause();

//        releaseCamera();
        isFlashOn = false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

    }

    private boolean isFlashSupported() {
        boolean hasFlashLight=false;

        FeatureInfo[] feature = getPackageManager().getSystemAvailableFeatures();
        for (FeatureInfo featureInfo : feature) {
            if (PackageManager.FEATURE_CAMERA_FLASH.equals(featureInfo.name)) {
                hasFlashLight = true;
                break;
            }
        }

        return hasFlashLight;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.title_back:
                finish();
                break;
            case R.id.scan_qr_code_bottom_btn_photo:
                Intent intentPick = new Intent(Intent.ACTION_PICK,
                        android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

                startActivityForResult(intentPick, INTENT_ID_PICK_PHOTO);
                break;
            case R.id.scan_qr_code_bottom_btn_flash:
                if(isFlashSupported()) {
                    isFlashOn = !isFlashOn;
                    setFlashBtnStatus();
                } else {
                    mMsgbBox.toast(R.string.flash_not_supported);
                }
                break;
            case R.id.scan_qr_code_bottom_btn_qrcode:
                Intent intentView = new Intent(this,MyQRCodeActivity.class);
                ThemeHelper.putExtraCurrThemeResId(intentView, this);
                startActivity(intentView);
                break;
        }
    }

    @Override
    protected void onDecodeSucceed(String decodedContent) {
        Log.i("decode from camera success: "+decodedContent);
        handleDecodedString(decodedContent);
    }

    private void handleDecodedString(final String str) {
        //TODO
        if(TextUtils.isEmpty(str)) {
            Log.e("empty decoded string for handle");
        } else {
            Log.i("decode succeed: "+str);
//            mMsgbBox.toast("decode succeed: "+str);

            boolean canHandle=true;
            StringToJason jsonWraper=new StringToJason(str);
            String label=jsonWraper.getValue(Utils.QR_CODE_KEY_LABEL);
            if(null==label || !label.equals(Utils.QR_CODE_KEY_DEF_VALUE_LABEL)) {
                canHandle=false;
            } else {
                try {
                    String type=jsonWraper.getValue(Utils.QR_CODE_KEY_TYPE);
                    if(type.equals(Utils.QR_CODE_KEY_DEF_VALUE_TYPE_BUDDY)) {
                        String uid = jsonWraper.getValue(Utils.QR_CODE_KEY_CONTENT, Utils.QR_CODE_KEY_UID);
                        String timestamp = jsonWraper.getValue(Utils.QR_CODE_KEY_CONTENT, Utils.QR_CODE_KEY_TIMESTAMP);
                        if (TextUtils.isEmpty(uid) || TextUtils.isEmpty(timestamp)) {
                            canHandle = false;
                        } else {
                            doHandleUserAccount(uid);
                        }
                    } else if(type.equals(Utils.QR_CODE_KEY_DEF_VALUE_TYPE_INVITECODE)) {
                        String code=jsonWraper.getValue(Utils.QR_CODE_KEY_CONTENT,Utils.QR_CODE_KEY_INVITECODE);
                        String timestamp=jsonWraper.getValue(Utils.QR_CODE_KEY_CONTENT,Utils.QR_CODE_KEY_TIMESTAMP);
                        if(TextUtils.isEmpty(code) || TextUtils.isEmpty(timestamp)) {
                            canHandle=false;
                        } else {
                            doHandleInvitation(code);
                        }
                    } else if (type.equals(Utils.QR_CODE_KEY_DEF_VALUE_TYPE_PUBLIC_ACCOUNT)) {
                        String publicAccountId=jsonWraper.getValue(Utils.QR_CODE_KEY_CONTENT,Utils.QR_CODE_KEY_PUBLIC_ACCOUNT_ID);
                        String timestamp=jsonWraper.getValue(Utils.QR_CODE_KEY_CONTENT,Utils.QR_CODE_KEY_TIMESTAMP);
                        if(TextUtils.isEmpty(publicAccountId) || TextUtils.isEmpty(timestamp)) {
                            canHandle=false;
                        } else {
                            doHandlePublicAccount(publicAccountId);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    canHandle=false;
                }
            }

            if(!canHandle) {
//                Intent intent=new Intent(this, QRCodeDecodedContentActivity.class);
//                intent.putExtra(QRCodeDecodedContentActivity.ACTIVITY_INTENT_EXT_KEY_CONTENT,str);
            	if(str.contains("//")){
            		Builder builder = new AlertDialog.Builder(ScanQRCodeActivity.this);
            		builder.setTitle(getResources().getString(R.string.contacts_QRcode_dialogtitle));
                	builder.setMessage(getResources().getString(R.string.contacts_QRcode_confirm));
                	builder.setPositiveButton(getResources().getString(R.string.contacts_QRcode_ok), new DialogInterface.OnClickListener() {
    					
    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    		            	Intent intent= new Intent(); 
    		            	intent.setAction("android.intent.action.VIEW");
    		            	Uri content_url = Uri.parse(str);
    		            	intent.setData(content_url);
    		                startActivity(intent);
    					}
    				});
                	builder.setNegativeButton(getResources().getString(R.string.contacts_QRcode_cancel), new DialogInterface.OnClickListener() {
    					
    					@Override
    					public void onClick(DialogInterface dialog, int which) {
    						finish();
    					}
    				});
                	
                	builder.create().show();         
            		
            	}
            }
        }
    }

    private void doHandlePublicAccount(final String publicAccountId) {
        Database db = new Database(this);
        Buddy publicAccount = db.buddyWithUserID(publicAccountId);
        if(null != publicAccount && !TextUtils.isEmpty(publicAccount.wowtalkID)
                && !TextUtils.isEmpty(publicAccount.userID)
                && !TextUtils.isEmpty(publicAccount.nickName)) {
            Log.i("public account exist local");
            Person person=Person.fromBuddy(publicAccount);
            Intent intent = new Intent(this, PublicAccountDetailActivity.class);
            intent.putExtra(PublicAccountDetailActivity.PERSON_DETAIL, person);
            startActivity(intent);

            finish();
        } else {
            Log.i("public account not exist local,get from server");
            mMsgbBox.showWait();

            new AsyncTask<String, Void, Buddy>() {
                @Override
                protected Buddy doInBackground(String... param) {
                    Buddy buddy = new Buddy();
                    int errno2ret = mWebif.fGetBuddyByWowtalkId(param[0], buddy);

                    if(ErrorCode.OK != errno2ret || TextUtils.isEmpty(buddy.userID)) {
                        errno2ret = mWebif.fGetBuddyWithUID(param[0]);
                        if(ErrorCode.OK != errno2ret) {
                            return null;
                        } else {
                            Database dbHelper = new Database(ScanQRCodeActivity.this);
                            return dbHelper.buddyWithUserID(param[0]);
                        }
                    } else {
                        return buddy;
                    }
                }

                @Override
                protected void onPostExecute(Buddy buddy) {
                    mMsgbBox.dismissWait();

                    if(null != buddy) {
                        Log.i("added buddy type: "+buddy.getAccountType());
                        Person person=Person.fromBuddy(buddy);
                        Intent intent = new Intent(ScanQRCodeActivity.this, PublicAccountDetailActivity.class);
                        intent.putExtra(PublicAccountDetailActivity.PERSON_DETAIL, person);
                        startActivity(intent);

                        finish();
                    } else {
                        mMsgbBox.toast(String.format(getString(R.string.family_qr_content_not_valid),publicAccountId));
                    }
                }

            }.execute(publicAccountId);
        }
    }

    private void doHandleInvitation(final String code) {
        LoginInvitedActivity.login(this, code, new MessageBox(this));
    }

    private void doHandleUserAccount(final String uid) {
        mMsgbBox.showWait();

        Log.i("handle_qr_code_uid="+uid);

        new AsyncTask<String, Void, Buddy>() {
            @Override
            protected Buddy doInBackground(String... param) {
                Buddy buddy = new Buddy();
                int errno2ret = mWebif.fGetBuddyByWowtalkId(param[0], buddy);

                if(ErrorCode.OK != errno2ret || TextUtils.isEmpty(buddy.userID)) {
                    errno2ret = mWebif.fGetBuddyWithUID(param[0]);
                    if(ErrorCode.OK != errno2ret) {
                        return null;
                    } else {
                        Database dbHelper = new Database(ScanQRCodeActivity.this);
                        return dbHelper.buddyWithUserID(param[0]);
                    }
                } else {
                    return buddy;
                }
            }

            @Override
            protected void onPostExecute(Buddy buddy) {
                mMsgbBox.dismissWait();

                if(null != buddy) {
                    ContactInfoActivity.launch(ScanQRCodeActivity.this, buddy);
                } else {
                    mMsgbBox.toast(String.format(getString(R.string.family_qr_content_not_valid),uid));
                }
            }

        }.execute(uid);
    }

    private Bitmap getScaledBmp(String bmpPath,int maxWidthOrHeight) {
        Bitmap orgBmp=BitmapFactory.decodeFile(bmpPath);
        if(null == orgBmp) {
            Log.e("decode bmp for path fail: "+bmpPath);
            return null;
        }
        if(orgBmp.getWidth() <= maxWidthOrHeight && orgBmp.getHeight() <= maxWidthOrHeight) {
            return orgBmp;
        }
        float scaleFactor=1.0f;
        if(orgBmp.getWidth() > orgBmp.getHeight()) {
            scaleFactor=1.0f*maxWidthOrHeight/orgBmp.getWidth();
        } else {
            scaleFactor=1.0f*maxWidthOrHeight/orgBmp.getHeight();
        }

        Matrix matrix=new Matrix();
        matrix.postScale(scaleFactor,scaleFactor);

        Bitmap bmp2ret=Bitmap.createBitmap(orgBmp,0,0,orgBmp.getWidth(),orgBmp.getHeight(),matrix,false);
        BmpUtils.recycleABitmap(orgBmp);
        return bmp2ret;
    }

    private Bitmap bmp2decode;
    private int curBmp2RotateDegree=90;
    private void decodeFromLocalPhoto(final String bmpPath) {
        bmp2decode=getScaledBmp(bmpPath,MyQRCodeActivity.MY_QR_CODE_PHOTO_DEF_SIZE);
        Log.i("bmp2decode size: "+bmp2decode.getWidth()+"-"+bmp2decode.getHeight());
        curBmp2RotateDegree=90;
        if(null == bmp2decode) {
            Log.e("bmp2decode null");
            return;
        }
        mMsgbBox.showWait();
        decodeBitmap(bmp2decode);
    }

    private void decodeBitmap(final Bitmap bmp) {
        new AsyncTask<Void, Integer, String>() {
            @Override
            protected String doInBackground(Void... params) {
                String ret=null;
                try {
                    ret = CaptureActivityHandler.decodeFromBitmap(bmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return ret;
            }

            @Override
            protected void onPostExecute(String param) {
                if(TextUtils.isEmpty(param)) {
                    if(curBmp2RotateDegree >= 360) {
                        mMsgbBox.dismissWait();
                        mMsgbBox.toast(R.string.decode_qrcode_failed);
                    } else {
                        Log.i("decode with rotate: "+curBmp2RotateDegree);
                        Matrix matrix = new Matrix();
                        matrix.postRotate(curBmp2RotateDegree);
                        curBmp2RotateDegree += 90;
                        Bitmap bmpRotated=Bitmap.createBitmap(bmp2decode,0,0,bmp2decode.getWidth(),bmp2decode.getHeight(),matrix,false);
                        decodeBitmap(bmpRotated);
                    }
                } else {
                    mMsgbBox.dismissWait();
                    Log.i("decode from local photo succeed: "+param);
                    handleDecodedString(param);
                }
            }
        }.execute((Void)null);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == INTENT_ID_PICK_PHOTO && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();

            Log.i("local photo "+picturePath);
            decodeFromLocalPhoto(picturePath);
        }
    }

}
