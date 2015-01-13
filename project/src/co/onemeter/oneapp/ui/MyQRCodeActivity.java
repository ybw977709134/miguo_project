package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ThemeHelper;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;
import com.zxing.encoding.EncodingHandler;
import org.json.JSONObject;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-4
 * Time: 上午10:47
 * To change this template use File | Settings | File Templates.
 */
public class MyQRCodeActivity extends Activity implements View.OnClickListener{
    private BottomButtonBoard bottomBoard;
    private WowTalkWebServerIF wif = null;
    private PrefUtil mPrefUtil;
    private MessageBox mMsgbBox;

    private Bitmap QRCodeBitmap;

    public final static int    MY_QR_CODE_PHOTO_DEF_SIZE=300;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ThemeHelper.setTheme(this, getIntent().getExtras());

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        setContentView(R.layout.my_qr_code_layout);

        bottomBoard = new BottomButtonBoard(this, getWindow().getDecorView());
        wif = WowTalkWebServerIF.getInstance(this);
        mPrefUtil = PrefUtil.getInstance(this);
        mMsgbBox = new MessageBox(this);

        initView();
    }

    private void initView() {
        findViewById(R.id.title_close).setOnClickListener(this);
        findViewById(R.id.title_right_op).setOnClickListener(this);
        findViewById(R.id.my_photo).setOnClickListener(this);
        findViewById(R.id.my_qr_code_photo).setOnClickListener(this);

        TextView tvName=(TextView)findViewById(R.id.my_name);
        tvName.setText(mPrefUtil.getMyNickName());

        Buddy me = new Buddy();
        me.userID = mPrefUtil.getUid();
        me.photoUploadedTimeStamp = mPrefUtil.getMyPhotoUploadedTimestamp();
        ImageView ivMyPhoto=(ImageView)findViewById(R.id.my_photo);
        PhotoDisplayHelper.displayPhoto(this, ivMyPhoto, R.drawable.default_avatar_90, me, true);

        bottomBoard.clearView();
        bottomBoard.add(getString(R.string.my_qr_code_bottom_op_share), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        shareMyQRCodePhoto();
                    }
                });
        bottomBoard.add(getString(R.string.my_qr_code_bottom_op_save), BottomButtonBoard.BUTTON_BLUE,
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        bottomBoard.dismiss();
                        saveQRCodePhoto();
                    }
                });
        bottomBoard.addCancelBtn(getString(R.string.cancel));

        encodeMyQRCodePhoto();
    }

    private void shareMyQRCodePhoto() {

    }

    public static File getNewSystemPhotoStoreLocation() {
        String photoDir;

        if(Environment.getExternalStorageState().equals(android.os.Environment.MEDIA_MOUNTED)) {
            //sdcard exist
            // /mnt/sdcard and /sdcard are alias,but here /mnt/sdcard is needed as to used int MediaScanner
            photoDir = "/mnt/sdcard/DCIM/camera/";
        } else {
            //sdcard not exist
            photoDir = "/stystm/DCIM/camera/";
        }

        File dirFile=new File(photoDir);
        if (! dirFile.exists()){
            if (! dirFile.mkdirs()){
                return null;
            }
        }

        File aFile=new File(photoDir+System.currentTimeMillis()+"_qr_code_image.png");

        return aFile;
    }

    private void saveQRCodePhoto() {
        mMsgbBox.showWait();

        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, File>() {
            @Override
            protected File doInBackground(Void... params) {
                File file = getNewSystemPhotoStoreLocation();
                try {
                    OutputStream os = new FileOutputStream(file);
                    QRCodeBitmap.compress(Bitmap.CompressFormat.PNG, 100, os);
                    os.close();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                return file;
            }

            @Override
            protected void onPostExecute(File file) {
                mMsgbBox.dismissWait();

                if (TextUtils.isEmpty(file.getAbsolutePath())) {
                    mMsgbBox.toast(R.string.msg_operation_failed);
                } else {
                    mMsgbBox.toast(String.format(getString(R.string.save_successed), file.getAbsolutePath()));

                    //trigger media service scan new file,or you can not see this photo in gallery
                    Intent scanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file));
                    MyQRCodeActivity.this.sendBroadcast(scanIntent);
                }
            }
        });
    }

    private void encodeMyQRCodePhoto() {
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Bitmap>() {
            @Override
            protected Bitmap doInBackground(Void... params) {
                JSONObject strObj = new JSONObject();
                try {
                    strObj.put(Utils.QR_CODE_KEY_LABEL, Utils.QR_CODE_KEY_DEF_VALUE_LABEL);
                    strObj.put(Utils.QR_CODE_KEY_TYPE, Utils.QR_CODE_KEY_DEF_VALUE_TYPE_BUDDY);

                    JSONObject contentObj = new JSONObject();

                    String myid = mPrefUtil.getUid();
                    if (TextUtils.isEmpty(myid)) {
                        myid = mPrefUtil.getUid();
                        if (TextUtils.isEmpty(myid)) {
                            return null;
                        }
                    }
                    contentObj.put(Utils.QR_CODE_KEY_UID, myid);
                    contentObj.put(Utils.QR_CODE_KEY_TIMESTAMP, System.currentTimeMillis());

                    strObj.put(Utils.QR_CODE_KEY_CONTENT, contentObj);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                String str2encode = strObj.toString();

                Bitmap bmp2ret = null;

                Buddy me = new Buddy();
                me.userID = mPrefUtil.getUid();
                me.photoUploadedTimeStamp = mPrefUtil.getMyPhotoUploadedTimestamp();
                String path = PhotoDisplayHelper.makeLocalThumbnailPath(MyQRCodeActivity.this, me.getGUID());
                Bitmap bmpInside = null;
                if (new File(path).exists()) {
                    bmpInside = BitmapFactory.decodeFile(path);
                } else {
                    bmpInside = BitmapFactory.decodeResource(MyQRCodeActivity.this.getResources(), R.drawable.icon);
                }

                try {
                    bmp2ret = EncodingHandler.createQRCode(str2encode, MY_QR_CODE_PHOTO_DEF_SIZE, bmpInside);
                }
                catch (Exception e) {
                    e.printStackTrace();
                }

                return bmp2ret;
            }

            @Override
            protected void onPostExecute(Bitmap bmp) {
                if (null == bmp) {
                    mMsgbBox.toast(R.string.encode_qrcode_failed);
                } else {
                    QRCodeBitmap = bmp;
                    findViewById(R.id.title_right_op).setVisibility(View.VISIBLE);
                    ImageView ivMyQRCodePhoto = (ImageView) findViewById(R.id.my_qr_code_photo);
                    ivMyQRCodePhoto.setImageBitmap(bmp);
                }
            }
        });
    }

    @Override
    public void onClick(View view) {
        switch(view.getId()) {
            case R.id.title_close:
                finish();
                break;
            case R.id.my_photo:
                Buddy b = new Buddy();
                b.userID = mPrefUtil.getUid();
                b.photoUploadedTimeStamp = mPrefUtil.getMyPhotoUploadedTimestamp();
                ImageViewActivity.launch(this, b);
                break;
            case R.id.title_right_op:
                bottomBoard.show();
                break;
            default:
                Log.e("my qr code: unhandled view id "+view.getId());
                break;
        }
    }
}
