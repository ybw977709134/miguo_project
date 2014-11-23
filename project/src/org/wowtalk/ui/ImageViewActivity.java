package org.wowtalk.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.wowtalk.api.*;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.FileUtils;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.Log;
import co.onemeter.oneapp.widget.scale_viewpager.PhotoView;
import co.onemeter.oneapp.widget.scale_viewpager.PhotoViewPager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * User: pan
 * Date: 3/25/13
 * Time: 11:13 AM
 */
public class ImageViewActivity extends Activity implements View.OnClickListener {
	private static final String EXTRA_IMGPOSITION="imgPosition";
    private static final String EXTRA_ISDUMMY = "imgdmmy";
//	private static final String EXTRA_IMGPATH = "imgpath";
    private static final String EXTRA_PHOTO_UID = "photouid";
    private static final String EXTRA_WFILES = "wfiles";
    private static final String EXTRA_UPDATE_TYPE = "update_type";
    private static final String EXTRA_IS_MESSAGE_HISTORY = "is_message_history";

    public static final int UPDATE_WITH_NONE=-1;
    public static final int UPDATE_WITH_CHAT_MESSAGE=1;
    public static final int UPDATE_WITH_MOMENT_MEDIA=2;
    private int updateType=UPDATE_WITH_NONE;

    private boolean mIsMessageHistory;
    private String mSavePath;
    private MessageBox mMsgBox;
    private BottomButtonBoard mMenu;

    //private int curPhotoIdx;
    private ArrayList<WFile> files;
    IHasPhoto mPhotoEntity;
    private int mCurPhotoIdx = 0;
    List<ImageView> mImageViews;

    //private ImageView ivMainPhoto;
    private TextView  tvPhotoIndexIndicator;
    
    private ViewPager mainViewPager;
    private MyViewPagerAdapter vpAdapter;

    //the name is assumed to be not same with others
    private final static String DEFAULT_IMAGE_ID_ON_NO_IMAGE="default____image__id_on_____no___image_01234_____56789_____q_i_n";

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(PixelFormat.RGBA_8888);
        
        setContentView(R.layout.activity_imageview);

        mMenu = new BottomButtonBoard(this, findViewById(android.R.id.content));

        tvPhotoIndexIndicator=(TextView)findViewById(R.id.txtPhotoIdxIndicate);
        tvPhotoIndexIndicator.setVisibility(TextView.INVISIBLE);
        findViewById(R.id.btn_back).setOnClickListener(this);
        findViewById(R.id.btn_save).setOnClickListener(this);

        Intent intent = getIntent();
        updateType=intent.getIntExtra(EXTRA_UPDATE_TYPE, UPDATE_WITH_NONE);
        mIsMessageHistory = intent.getBooleanExtra(EXTRA_IS_MESSAGE_HISTORY, false);

        int curPhotoIdx = intent.getIntExtra(EXTRA_IMGPOSITION, 0);
        if (curPhotoIdx < 0) curPhotoIdx = 0;

        boolean dataSrcFound = false;

//        if (!dataSrcFound) {
//            String singleFilename = getIntent().getStringExtra(EXTRA_IMGPATH);
//            if(null != singleFilename) {
//                files = new ArrayList<WFile>();
//                WFile f = new WFile();
//                f.localPath = singleFilename;
//                files.add(f);
//                curPhotoIdx = 0;
//                dataSrcFound = true;
//            }
//        }

        if (!dataSrcFound) {
            files = getIntent().getParcelableArrayListExtra(EXTRA_WFILES);
            if(null != files && !files.isEmpty()) {
                if (curPhotoIdx >= files.size())
                    curPhotoIdx = files.size() - 1;
                //test if showing position indicator tv
                if(files.size() >= 2) {
                    tvPhotoIndexIndicator.setVisibility(TextView.VISIBLE);
                    tvPhotoIndexIndicator.setText((curPhotoIdx + 1) + "/" + files.size());
                }
                dataSrcFound = true;
            }
        }

        if (!dataSrcFound) {
            mPhotoEntity = getIntent().getParcelableExtra(EXTRA_PHOTO_UID);
            if (null != mPhotoEntity && mPhotoEntity.getPhotoUploadedTimestamp() > 0) {
                WFile f = new WFile();
                f.localPath = PhotoDisplayHelper.makeLocalThumbnailPath(this, mPhotoEntity.getGUID());
                files = new ArrayList<WFile>();
                files.add(f);
                curPhotoIdx = 0;
                dataSrcFound = true;
            }
        }

        if(mMsgBox == null)
            mMsgBox = new MessageBox(this);

        if(!dataSrcFound) {
            //if no image inputted,show the single default image
            String defImagePath=PhotoDisplayHelper.makeLocalThumbnailPath(this, DEFAULT_IMAGE_ID_ON_NO_IMAGE + mPhotoEntity.getAccountType());
            if(!new File(defImagePath).exists()) {
                //if such def image not exist,create it
                Bitmap bmp=BitmapFactory.decodeResource(getResources(), getFallbackDrawableResId(mPhotoEntity.getAccountType()));
                try {
                    OutputStream os = new FileOutputStream(defImagePath);
                    boolean ret = bmp.compress(Bitmap.CompressFormat.JPEG, 80, os); // XXX format should be same with main file?
                    os.close();

                    BmpUtils.recycleABitmap(bmp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            mPhotoEntity = null;

            WFile f = new WFile();
            f.localPath = defImagePath;
            files = new ArrayList<WFile>();
            files.add(f);
            curPhotoIdx = 0;

            dataSrcFound = true;
        }

        //setup view pager to show all photos
        if (dataSrcFound) {
            setupViewPager(curPhotoIdx);
//            downloadPhoto_async();
        } else {
            //here will never reach
//            mainViewPager = (ViewPager)findViewById(R.id.mainViewPager);
//            mImageViews = new ArrayList<ImageView>();
//            ImageView iv = new ImageView(this);
//            iv.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
//                    LinearLayout.LayoutParams.WRAP_CONTENT));
//            iv.setImageResource(R.drawable.default_avatar_90);
//            mImageViews.add(iv);
//            vpAdapter = new MyViewPagerAdapter();
//            mainViewPager.setAdapter(vpAdapter);
//            return;
        }
    }

    private static int getFallbackDrawableResId(int accountType) {
        switch (accountType) {
            case Buddy.ACCOUNT_TYPE_PUBLIC:
                return R.drawable.default_official_avatar_90;
            case Buddy.ACCOUNT_TYPE_GROUP:
                return R.drawable.default_group_avatar_90;
            default:
                return R.drawable.default_avatar_90;
        }
    }

    /**
     * Reload local photo into image view.
     * @param position
     */
    private void refreshImageView_onUiThrd(final int position) {
        if (position < 0) return;

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (null == mImageViews || mImageViews.size() <= position
                        || null == files || files.size() <= position)
                    return;

                View v = mImageViews.get(position);
                if (!(v instanceof ImageView)) return;

                ImageView iv = (ImageView) v;
                BitmapDrawable oldBmpDrawable=(BitmapDrawable)iv.getDrawable();
//                BitmapDrawable d = new BitmapDrawable(getResources(),
//                        files.get(position).localPath);
                iv.setImageDrawable(getDrawableFromWFile(files.get(position)));
                iv.invalidate();

                freeABitmapDrawable(oldBmpDrawable);

                if(UPDATE_WITH_CHAT_MESSAGE == updateType) {
                    Database mDbHelper=new Database(ImageViewActivity.this);
                    ChatMessage msg = null;
                    if (mIsMessageHistory) {
                        msg = mDbHelper.getMessageHistoryByPrimaryKey(files.get(position).getExt());
                        msg.pathOfMultimedia = files.get(position).localPath;
                        mDbHelper.updateChatMessageHistory(msg, true);
                    } else {
                        msg = mDbHelper.fetchChatMessageByPrimaryKey(Integer.valueOf(files.get(position).getExt()));
                        msg.pathOfMultimedia = files.get(position).localPath;
                        mDbHelper.updateChatMessage(msg, true);
                    }
                    Log.i("msg.pathOfMultimedia set to "+msg.pathOfMultimedia);
                } else if (UPDATE_WITH_MOMENT_MEDIA == updateType) {
                    //moment media file path is not stored in db,no need update,file exist is will be ok
                }
            }
        });
    }

    private void freeABitmapDrawable(final BitmapDrawable bmpDrawable) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                if(null != bmpDrawable && null != bmpDrawable.getBitmap()) {
                    BmpUtils.recycleABitmap(bmpDrawable.getBitmap());
                }

                return null;
            }
        }.execute((Void)null);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        for(View aView : mImageViews) {
            ImageView iv = (ImageView) aView;
            freeABitmapDrawable((BitmapDrawable)iv.getDrawable());
        }
    }

    /**
     * Download photos.
     */
    private ArrayList<Integer> downloadingFiles=new ArrayList<Integer>();
    private void downloadPhoto_async() {

        if (null != mPhotoEntity) return;
        // let PhotoDisplayHelper do the downloading work

        if(downloadingFiles.contains(mCurPhotoIdx)) {
            Log.w("duplicate download for "+mCurPhotoIdx);
            return;
        }

        downloadingFiles.add(mCurPhotoIdx);

        mMsgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {

            private WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(ImageViewActivity.this);
            private int downloadFileIndex=mCurPhotoIdx;
            @Override
            protected Integer doInBackground(Void... voids) {

                // check current photo first
                if(UPDATE_WITH_MOMENT_MEDIA == updateType) {
                    downloadFileFromMomentMedia(downloadFileIndex);
                } else {
                    downloadFile(downloadFileIndex);
                }

                // check other photos
//                for (int i = 0, n = files.size(); i < n; ++i) {
//                    if (i == mCurPhotoIdx)
//                        continue;
//                    downloadFile(i);
//                }
                return null;
            }

            private void downloadFileFromMomentMedia(final int position) {
                WFile file = files.get(position);
                if (!new File(file.localPath).exists()) {
                    web.fGetFileFromServer(file.fileid,GlobalSetting.S3_MOMENT_FILE_DIR,
                            new NetworkIFDelegate() {
                                @Override
                                public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                    // refresh image view
                                    refreshImageView_onUiThrd(position);
                                }

                                @Override
                                public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                }

                                @Override
                                public void setProgress(int i, int i2) {
                                }
                            }, 0, file.localPath, null);
                }
            }

            private void downloadFile(final int position) {
                WFile file = files.get(position);
                if (!new File(file.localPath).exists()) {
                    web.fGetFileFromServer(file.fileid,
                            new NetworkIFDelegate() {
                                @Override
                                public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                    // refresh image view
                                    refreshImageView_onUiThrd(position);
                                }

                                @Override
                                public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                }

                                @Override
                                public void setProgress(int i, int i2) {
                                }
                            }, 0, file.localPath, null);
                }
            }

            @Override
            protected void onPostExecute(Integer v) {
                downloadingFiles.remove(Integer.valueOf(downloadFileIndex));
                mMsgBox.dismissWait();
            }
        }.execute((Void) null);
    }

    private BitmapDrawable getDrawableFromWFile(WFile aFile) {
        BitmapDrawable drawable=null;

        if (!TextUtils.isEmpty(aFile.localPath) && new File(aFile.localPath).exists()) {
            drawable=new BitmapDrawable(getResources(), aFile.localPath);
        } else if (!TextUtils.isEmpty(aFile.localThumbnailPath) && new File(aFile.localThumbnailPath).exists()) {
            drawable=new BitmapDrawable(getResources(), aFile.localThumbnailPath);
        } else {
            Log.e("can not create bitmap drawable from WFile");
            drawable=new BitmapDrawable(getResources(), BitmapFactory.decodeResource(getResources(),R.drawable.feed_default_pic));
        }
        return drawable;
    }

    private void setupViewPager(int curPhotoIdx) {
        mainViewPager = (PhotoViewPager)findViewById(R.id.mainViewPager);

        //hold all image view in viewPager
        mImageViews = new ArrayList<ImageView>();

        LinearLayout.LayoutParams mParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        for (WFile file : files) {
            ImageView iv = new PhotoView(this);
            iv.setLayoutParams(mParams);

//            if (new File(file.localPath).exists())
//                iv.setImageDrawable(new BitmapDrawable(getResources(), file.localPath));
//            else
//                iv.setImageDrawable(new BitmapDrawable(getResources(), file.localThumbnailPath));
//            iv.setImageDrawable(getDrawableFromWFile(file));
            iv.setImageDrawable(null);
            mImageViews.add(iv);

            if(mPhotoEntity != null) {
                String thumbPath = PhotoDisplayHelper.makeLocalThumbnailPath(this, mPhotoEntity.getGUID());
                BitmapDrawable thumbDrawable = new BitmapDrawable(this.getResources(), thumbPath);
                PhotoDisplayHelper.displayPhoto(this, iv, thumbDrawable, mPhotoEntity, false);
            }
        }

        //set adapter
        vpAdapter = new MyViewPagerAdapter();
        mainViewPager.setAdapter(vpAdapter);
        
        //page change listener
        OnPageChangeListener pageChangeListener = new OnPageChangeListener() {
            public void  onPageScrollStateChanged (int state) {                
            }
            
            public void  onPageScrolled (int position, float positionOffset, 
                                            int positionOffsetPixels) {
            }
            
            public void  onPageSelected (int position) {
                mCurPhotoIdx = position;
                tvPhotoIndexIndicator.setText((position + 1) + "/" + files.size());

                triggerPhotoDownload(position);
            }
        };
        mainViewPager.setOnPageChangeListener(pageChangeListener);
        
        mainViewPager.setCurrentItem(curPhotoIdx);
        //as if only one image exist,then no onPageSelected will be called,so trigger it fix here
        triggerPhotoDownload(curPhotoIdx);
    }

    private void triggerPhotoDownload(int position) {
        if(!TextUtils.isEmpty(files.get(position).localPath) && !new File(files.get(position).localPath).exists()) {
            Log.i("position "+position+" local path not exist");
            downloadPhoto_async();
        } else {
            Log.i("position "+position+" local path already exist");
        }
    }

    public static void launch(Context context, IHasPhoto entity) {
        Intent i = new Intent(context, ImageViewActivity.class);
        i.putExtra(EXTRA_PHOTO_UID, entity);
        i.putExtra(EXTRA_UPDATE_TYPE, UPDATE_WITH_NONE);
        context.startActivity(i);
    }

    /**
     * @param context
     * @param filename image filename
     */
//    public static void launch(Context context, String filename) {
//        Intent i = new Intent(context, ImageViewActivity.class);
//        if (null != filename)
//            i.putExtra(EXTRA_IMGPATH, filename);
//        i.putExtra(EXTRA_UPDATE_TYPE, UPDATE_WITH_NONE);
//        context.startActivity(i);
//    }
//
//    public static void launch(Context context, int position, String[] filenames) {
//        ArrayList<WFile> files = new ArrayList<WFile>(filenames.length);
//        for (String path : filenames) {
//            if (null == path) continue;
//            WFile f = new WFile();
//            f.localPath = path;
//            files.add(f);
//        }
//        launch(context, position, files,UPDATE_WITH_NONE);
//    }

    public static void launch(Context context, int position, List<PhotoWithThumbPair> pairList,int updateType) {
        launch(context, position, pairList, updateType, false);
    }

    public static void launch(Context context, int position, List<PhotoWithThumbPair> pairList,int updateType, boolean isMessageHistory) {
        ArrayList<WFile> files = new ArrayList<WFile>(pairList.size());
        for (PhotoWithThumbPair aPair : pairList) {
            WFile f = new WFile();
            f.localThumbnailPath=aPair.getThumbPath();
            f.thumb_fileid=aPair.getThumbFileId();
            f.localPath = aPair.getPhotoPath();
            f.fileid=aPair.getPhotoFileId();
            f.setExt(aPair.getExt());
            files.add(f);
        }
        launch(context, position, files,updateType, isMessageHistory);
    }

    /**
     *
     * @param context
     * @param position
     * @param files require WFile.localPath
     */
    public static void launch(Context context, int position, ArrayList<WFile> files,int updateType) {
        launch(context, position, files, updateType, false);
    }

    public static void launch(Context context, int position, ArrayList<WFile> files,int updateType, boolean isMessageHistory) {
        Intent i = new Intent(context, ImageViewActivity.class);
        i.putExtra(EXTRA_IMGPOSITION, position);
        i.putExtra(EXTRA_WFILES, files);
        i.putExtra(EXTRA_UPDATE_TYPE, updateType);
        i.putExtra(EXTRA_IS_MESSAGE_HISTORY, isMessageHistory);
        context.startActivity(i);
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.btn_back:
                onBackPressed();
                break;
            case R.id.btn_save:
                onSaveImagePressed();
                break;
        }
    }

    private void onSaveImagePressed() {


        if (null == files || files.isEmpty()) {
            mMsgBox.toast(R.string.operation_failed);
            return;
        }

        if (mCurPhotoIdx < 0 || files.size() <= mCurPhotoIdx) {
            mMsgBox.toast(R.string.operation_failed);
            return;
        }

        final String mSrcPath = files.get(mCurPhotoIdx).localPath;

        File sf;
        if(null == mSrcPath || !(sf = new File(mSrcPath)).exists()) {
            mMsgBox.toast(R.string.operation_failed);
            return;
        }
        mSavePath = makeSavePath(mSrcPath);
        final File df = new File(mSavePath);
        if(df.exists()) {
            mMenu.clearView();
            mMenu.add(getString(R.string.image_override), BottomButtonBoard.BUTTON_RED,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mMenu.dismiss();
                            saveFile(new File(mSrcPath), df);
                        }
                    });
            mMenu.add(getString(R.string.image_save_as), BottomButtonBoard.BUTTON_BLUE,
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            mMenu.dismiss();
                            String newSavePath = getAlternativeSavePath();
                            if (null != newSavePath)
                                saveFile(new File(mSrcPath), new File(newSavePath));
                        }
                    });
            mMenu.addCancelBtn(getString(R.string.cancel));
            mMenu.show();
        } else {
            saveFile(sf, df);
        }
    }

    private String getAlternativeSavePath() {
        String pathWithoutExt = mSavePath;
        String ext = "";
        int p = mSavePath.lastIndexOf('/');
        int q = mSavePath.lastIndexOf('.');
        if (p < q) {
            ext = mSavePath.substring(q);
            pathWithoutExt = mSavePath.substring(0, q);
        }
        int idx = 0;
        while (idx < 100 &&
            new File(pathWithoutExt + "_" + Integer.toString(idx) + ext).exists()) {
            ++idx;
        }
        if (idx < 100)
            return pathWithoutExt + "_" + Integer.toString(idx) + ext;
        return null;
    }

    private void saveFile(File sf, File df) {
        if (FileUtils.copyFile(sf, df)) {
            MediaScannerConnection.scanFile(this,
                    new String[]{df.getAbsolutePath()},
                    new String[]{"image/jpeg"},
                    new MediaScannerConnection.OnScanCompletedListener() {
                        @Override
                        public void onScanCompleted(String path, Uri uri) {
                        }
                    }
            );
            mMsgBox.toast(R.string.image_saved);
        } else {
            mMsgBox.toast(R.string.operation_failed);
        }
    }

    private static String makeSavePath(String orgPath) {
        File mediaStorageDir = new File(Environment.getExternalStorageDirectory(),
                "/Download/");
        if (! mediaStorageDir.exists()){
            if (! mediaStorageDir.mkdirs()){
                return null;
            }
        }
        //String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_", Locale.getDefault()).format(new Date());
        //String rand = Integer.toString((int)(Math.random() * 1000));
        //return mediaStorageDir.getAbsolutePath() + "/" + timeStamp + rand + ".jpg";
        String photoName = orgPath.substring(orgPath.lastIndexOf("/")+1, orgPath.lastIndexOf("."));
        return mediaStorageDir.getAbsolutePath() + "/" + photoName + ".jpg";
    }

    class MyViewPagerAdapter extends PagerAdapter {
        public MyViewPagerAdapter (){
        }

        @Override
        public void destroyItem(ViewGroup container, int position, Object object) {
            freeABitmapDrawable((BitmapDrawable) mImageViews.get(position).getDrawable());
            mImageViews.get(position).setImageDrawable(null);
            container.removeView(mImageViews.get(position));
        }

        @Override
        public int getCount() {
            if (mImageViews != null) {
                return mImageViews.size();
            }

            return 0;
        }

        @Override
        public Object instantiateItem(ViewGroup container, int position) {

            BitmapDrawable bmpDrawable=(BitmapDrawable) mImageViews.get(position).getDrawable();
            if(null == bmpDrawable) {
                mImageViews.get(position).setImageDrawable(getDrawableFromWFile(files.get(position)));
            }
            container.addView(mImageViews.get(position), 0);

            return mImageViews.get(position);
        }

        @Override
        public boolean isViewFromObject(View arg0, Object arg1) {
            return (arg0 == arg1);
        }
    }
}

