package org.wowtalk.ui;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Parcel;
import android.os.Parcelable;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.text.TextUtils;

import org.wowtalk.Log;
import org.wowtalk.api.ChatMessage;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.InputBoardManager.ChangeToOtherAppsListener;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

/**
 * Helper class to input image or video: allow user to pick from gallery or take a new one by camera.
 * <p>
 * MediaInputHelper is designed to use as instance, you should save it in Activity.onSaveInstanceState()
 * to avoid state lost.
 *
 * @version 2013/2/5 11:01
 * @author pan
 */
public class MediaInputHelper implements Parcelable {

	/*
	 *  input media message
	 */
	public static final int MEDIA_TYPE_NONE = 0;
	public static final int MEDIA_TYPE_IMAGE = 1;
	public static final int MEDIA_TYPE_VIDEO = 2;
	public static final int MEDIA_TYPE_VOICE = 3;
	public static final int MEDIA_TYPE_THUMNAIL = 4;

	/* MediaStore.ACTION_IMAGE_CAPTURE will not return the Uri passed as EXTRA_OUTPUT. */
	private Uri mLastImageUri = null;
	private Uri mLastVideoUri;
	private ChangeToOtherAppsListener mChangeAppsListener;

    public MediaInputHelper() {
    }

	public MediaInputHelper(ChangeToOtherAppsListener changeAppsListener) {
	    mChangeAppsListener = changeAppsListener;
	}

    public void setChangeAppsListener(ChangeToOtherAppsListener changeAppsListener) {
        mChangeAppsListener = changeAppsListener;
    }

    /**
     *
     * @param activity context
     * @param requestCode for Activity.startActivityForResult()
     * @param title optional title that will be displayed in the chooser dialog
     */
	public void inputImage(Activity activity, int requestCode, String title) {

        if (mChangeAppsListener != null)
            mChangeAppsListener.changeToOtherApps();
        mLastImageUri = Uri.fromFile(makeOutputMediaFile(MEDIA_TYPE_IMAGE, ".jpg")); // create a file to save the image

        Intent getContentIntent = new Intent();
        getContentIntent.setType("image/*");
        getContentIntent.setAction(Intent.ACTION_GET_CONTENT);

//        Intent pickIntent = new Intent(Intent.ACTION_PICK,
//                android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);

		PackageManager pm = activity.getPackageManager();
		if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
			Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mLastImageUri); // set the image file name
            takePhotoIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.name()); // set the image file name

			Intent chooserIntent = Intent.createChooser(getContentIntent, title);
            chooserIntent.putExtra (
                    Intent.EXTRA_INITIAL_INTENTS,
                    new Intent[] { takePhotoIntent }
            );

			activity.startActivityForResult(chooserIntent, requestCode);
		} else {
			activity.startActivityForResult(getContentIntent, requestCode);
		}
	}

    public void pickPhoto(Activity activity, int requestCode) {
        if (mChangeAppsListener != null)
            mChangeAppsListener.changeToOtherApps();
        Intent pickIntent = new Intent();
        pickIntent.setType("image/*");
        pickIntent.setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(pickIntent, requestCode);
    }

    public void takePhoto(Activity activity, int requestCode) {
        mLastImageUri = Uri.fromFile(makeOutputMediaFile(MEDIA_TYPE_IMAGE, ".jpg"));
        PackageManager pm = activity.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            if (mChangeAppsListener != null)
                mChangeAppsListener.changeToOtherApps();
            Intent takePhotoIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            takePhotoIntent.putExtra(MediaStore.EXTRA_OUTPUT, mLastImageUri);
            takePhotoIntent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.name());
            activity.startActivityForResult(takePhotoIntent, requestCode);
        }
    }

    private void fixVideoPath(final Activity activity) {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    //fix for meizu
                    File dcimFile=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
                    File videoFile=Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES);

//                String meizuDcimPath="/mnt/sdcard/Camera";
//                String meizuVideoPath="/mnt/sdcard/Video";
                    String systemProduct= Build.PRODUCT;

                    if(null != dcimFile && null != videoFile && !TextUtils.isEmpty(systemProduct)) {
                        if(systemProduct.contains("meizu")) {
                            Log.i("to fix meizu video");
                            File videoPathInDcim=new File(dcimFile.getAbsolutePath()+"/Video");
                            if(videoPathInDcim.exists()) {
                                final File[] filesInVideo=videoPathInDcim.listFiles();

                                if(null != filesInVideo && filesInVideo.length > 0) {
                                    Log.i("video file count: " + filesInVideo.length);

                                    File[] filesInDestPath=videoFile.listFiles();
                                    ArrayList<String> fileNameInDestList=new ArrayList<String>();
                                    if(null != filesInDestPath && filesInDestPath.length > 0) {
                                        for(File aFile : filesInDestPath) {
                                            fileNameInDestList.add(aFile.getName());
                                        }
                                    }

                                    for(int i=0; i< filesInVideo.length; ++i) {
                                        String renameToFileName=videoFile.getAbsolutePath()+"/"+getUnDuplicateFileName(fileNameInDestList,filesInVideo[i].getName());
                                        Log.i("rename to:"+renameToFileName);
                                        filesInVideo[i].renameTo(new File(renameToFileName));
                                    }

                                    new fixVideoPathMediaScannerConnection(activity,videoFile.getAbsolutePath()).startScan();
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private String getNewTmpUnDupName() {
        return System.currentTimeMillis()+"_"+(++dup_ids);
    }
    private static long dup_ids=0;
    private String getUnDuplicateFileName(ArrayList<String> existFileNameList,String newString) {
        if(null == existFileNameList || TextUtils.isEmpty(newString)) {
            return getNewTmpUnDupName();
        }
        String ret="";

        try {
            if(existFileNameList.contains(newString)) {
                ret=getNewTmpUnDupName()+"_"+newString;
            } else {
                ret=newString;
            }
        } catch (Exception e) {
            e.printStackTrace();
            ret=getNewTmpUnDupName();
        }

        return ret;
    }

    private class fixVideoPathMediaScannerConnection {
        private Activity activityRef;
        private String mediaPath2scanRef;

        private MediaScannerConnection mConnection;
        private int file2scanCount;
        private ArrayList<String> scannedFilesList;

        public fixVideoPathMediaScannerConnection(Activity activity,String mediaPath2scan) {
            activityRef=activity;
            mediaPath2scanRef=mediaPath2scan;
        }

        public void startScan() {
            if(null == activityRef || TextUtils.isEmpty(mediaPath2scanRef)) {
                return;
            }
            try {
                mConnection = new MediaScannerConnection(activityRef, new MediaScannerConnection.MediaScannerConnectionClient() {
                    @Override
                    public void onMediaScannerConnected() {
                        try {
                            Log.i("media scaaner connected for "+mediaPath2scanRef);
                            final File[] filesInVideo=new File(mediaPath2scanRef).listFiles();
                            if(null != filesInVideo && filesInVideo.length > 0) {
                                file2scanCount=filesInVideo.length;
                                scannedFilesList=new ArrayList<String>(filesInVideo.length);

                                for(int i=0; i< filesInVideo.length; ++i) {
                                    if(!filesInVideo[i].isFile()) {
                                        --file2scanCount;
                                        continue;
                                    }
                                    Log.i("scanning:" + filesInVideo[i].getAbsolutePath());
                                    mConnection.scanFile(filesInVideo[i].getAbsolutePath(), null);
                                }
                                if(file2scanCount <= 0) {
                                    mConnection.disconnect();
                                }
                            } else {
                                Log.w("no file to scan");
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onScanCompleted(String s, Uri uri) {
                        try {
                            Log.i("scanned: " + s + "," + uri.toString());
                            if(!scannedFilesList.contains(s)) {
                                scannedFilesList.add(s);
                            }
                            Log.i("progress "+scannedFilesList.size()+"/"+file2scanCount);
                            if(scannedFilesList.size() >= file2scanCount) {
                                if(mConnection.isConnected()) {
                                    Log.i("disconnect scanner");
                                    mConnection.disconnect();
                                }
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                });
                mConnection.connect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void inputVideo(Activity activity, int requestCode) {
        fixVideoPath(activity);
        if (mChangeAppsListener != null)
            mChangeAppsListener.changeToOtherApps();
        Intent pickIntent = new Intent();
        pickIntent.setType("video/*");
        pickIntent.setAction(Intent.ACTION_GET_CONTENT);

        PackageManager pm = activity.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Intent recoderIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            mLastVideoUri = Uri.fromFile(makeOutputMediaFile(MEDIA_TYPE_VIDEO, null)); // create a file to save the image
            recoderIntent.putExtra(MediaStore.EXTRA_OUTPUT, mLastVideoUri); // set the image file name
            recoderIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // low resolution
            recoderIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 1200);

            String pickTitle = "Select or take a new video"; // Or get from strings.xml
            Intent chooserIntent = Intent.createChooser(pickIntent, pickTitle);
            chooserIntent.putExtra (
                    Intent.EXTRA_INITIAL_INTENTS,
                    new Intent[] { recoderIntent }
            );

            activity.startActivityForResult(chooserIntent, requestCode);
        } else {
            activity.startActivityForResult(pickIntent, requestCode);
        }
    }

	public void pickVideo(Activity activity, int requestCode) {
        fixVideoPath(activity);
        if (mChangeAppsListener != null)
            mChangeAppsListener.changeToOtherApps();
		Intent pickIntent = new Intent();
		pickIntent.setType("video/*");
		pickIntent.setAction(Intent.ACTION_GET_CONTENT);
        activity.startActivityForResult(pickIntent, requestCode);
	}


    public void takeVideo(Activity activity, int requestCode) {
        fixVideoPath(activity);
        if (mChangeAppsListener != null)
            mChangeAppsListener.changeToOtherApps();

        PackageManager pm = activity.getPackageManager();
        if (pm.hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
            Intent recoderIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            mLastVideoUri = Uri.fromFile(makeOutputMediaFile(MEDIA_TYPE_VIDEO, null)); // create a file to save the image
            recoderIntent.putExtra(MediaStore.EXTRA_OUTPUT, mLastVideoUri); // set the image file name
            recoderIntent.putExtra(MediaStore.EXTRA_VIDEO_QUALITY, 0); // low resolution
            recoderIntent.putExtra(MediaStore.EXTRA_DURATION_LIMIT, 1200);
            activity.startActivityForResult(recoderIntent, requestCode);
        }
    }

    /**
	 * Call me in Activity.onActivityResult() if the requestCode matches and
	 * resultCode equals Activity.RESULT_OK.
	 *
	 * @param data
	 * @param thumbnailWidth 0 means thumb nail is not required.
	 * @param thumbnailHeight 0 means thumb nail is not required.
	 * @param path output parameter, original image file path => [0], thumb nail file path => [1].
	 * @return true on success.
	 */
    public boolean handleImageResult(Activity activity,
                                     Intent data,
                                     int thumbnailWidth,
                                     int thumbnailHeight,
                                     String[] path) {

        return handleImageResult(activity, data, 800, 800, thumbnailWidth, thumbnailHeight, path);
    }

    /**
     * Retrieve input image as JPG.
     * @param activity
     * @param data
     * @param limitWidth
     * @param limitHeight
     * @param thumbnailWidth 0 means thumb nail is not required.
     * @param thumbnailHeight 0 means thumb nail is not required.
     * @param path output parameter, original image file path => [0], thumb nail file path => [1].
     * @return
     */
	public boolean handleImageResult(Activity activity,
			Intent data,
            int limitWidth,
            int limitHeight,
			int thumbnailWidth,
			int thumbnailHeight,
			String[] path) {
		Uri uri;

		if(data == null || data.getData() == null) {
			// got a new photo via camera
			uri = mLastImageUri;
		} else {
			// picked a existed picture
			uri = data.getData();
		}

        if (null == uri || TextUtils.isEmpty(uri.toString())) {
            Log.e("MediaInputHelper#handleImageResult, uri is null(" + (null == uri) + ") or empty.");
            return false;
        }
        Log.i("MediaInputHelper#handleImageResult, uri is " + uri.toString());
        Log.i("--mLastImageUri--" + mLastImageUri);
        Log.i("--inputUri--" + uri);

		boolean operationFailed = false;
		File thumbnailFile = null;

        // limit input image size
        File littleFile = makeOutputMediaFile(MEDIA_TYPE_IMAGE, ".jpg");
        String filePath = littleFile.getAbsolutePath();
        try {
            // some devices (OS versions return an URI of com.android instead of com.google.android
            if (uri.toString().startsWith("content://com.android.gallery3d.provider"))  {
                // use the com.google provider, not the com.android provider.
                uri = Uri.parse(uri.toString().replace(
                        "com.android.gallery3d",
                        "com.google.android.gallery3d"));
            }

            if (uri.toString().startsWith("content://com.google.android.gallery3d")) {
                Bitmap littleBmp = BmpUtils.decodeUri(
                        activity,
                        uri,
                        limitWidth, limitHeight);
                if(littleBmp != null) {
                    FileOutputStream o = new FileOutputStream(filePath);
                    littleBmp.compress(Bitmap.CompressFormat.JPEG, 90, o);
                    o.close();

                    BmpUtils.recycleABitmap(littleBmp);
                } else {
                    Log.e("make littleBmp failed");
                    operationFailed = true;
                }
            } else {
                String inputImgPath = resolveMediaPath(activity, uri, MEDIA_TYPE_IMAGE);
        		Log.i("--inputImgPath--" + inputImgPath);
                if(inputImgPath != null && new File(inputImgPath).exists()) {
                    Bitmap littleBmp = BmpUtils.decodeFile(
                            inputImgPath,
                            limitWidth, limitHeight);
                    if(null == littleBmp) {
                        Log.e("make littleBmp failed");
                        operationFailed = true;
                    } else {
                        FileOutputStream o = new FileOutputStream(filePath);
                        littleBmp.compress(Bitmap.CompressFormat.JPEG, 90, o);
                        o.close();

                        BmpUtils.recycleABitmap(littleBmp);
                    }
                } else {
                    operationFailed = true;
                }
            }
        } catch (Exception e) {
            operationFailed = true;
            e.printStackTrace();
        }

		if(!operationFailed && thumbnailWidth > 0 && thumbnailHeight > 0) {
			try {
                Bitmap thumbnail = BmpUtils.decodeFile(
                        filePath,
                        thumbnailWidth, thumbnailHeight);
                if(null == thumbnail) {
                    Log.e("make thumbnail failed");
                    operationFailed = true;
                } else {
                    File f = makeOutputMediaFile(MEDIA_TYPE_THUMNAIL, ".jpg");
                    if(f != null) {
                        FileOutputStream o = new FileOutputStream(f);
                        if(thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, o))
                            thumbnailFile = f;
                        o.close();
                    } else {
                        operationFailed = true;
                    }

                    BmpUtils.recycleABitmap(thumbnail);
                }
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				operationFailed = true;
			} catch (IOException e) {
				e.printStackTrace();
				operationFailed = true;
			} catch (Exception e) {
				e.printStackTrace();
				operationFailed = true;
			}
		}

		if(!operationFailed) {
			path[0] = filePath;
			path[1] = thumbnailFile == null ? null : thumbnailFile.getAbsolutePath();
		}

		return !operationFailed;
	}

    public boolean handleImageResult(Activity activity,
            String srcFilePath,
            int limitWidth,
            int limitHeight,
            int thumbnailWidth,
            int thumbnailHeight,
            String[] path) {

        boolean operationFailed = false;
        File thumbnailFile = null;

        // limit input image size
        File littleFile = makeOutputMediaFile(MEDIA_TYPE_IMAGE, ".jpg");
        String filePath = littleFile.getAbsolutePath();
        try {
            String inputImgPath = srcFilePath;
            if(inputImgPath != null && new File(inputImgPath).exists()) {

                Bitmap littleBmp = BmpUtils.decodeFile(
                        inputImgPath,
                        limitWidth, limitHeight);
                if(null == littleBmp) {
                    Log.e("make littleBmp failed");
                    operationFailed = true;
                } else {
                    FileOutputStream o = new FileOutputStream(filePath);
                    littleBmp.compress(Bitmap.CompressFormat.JPEG, 90, o);
                    o.close();

                    BmpUtils.recycleABitmap(littleBmp);
                }
            } else {
                operationFailed = true;
            }
        } catch (Exception e) {
            operationFailed = true;
            e.printStackTrace();
        }

        if(!operationFailed && thumbnailWidth > 0 && thumbnailHeight > 0) {
            try {
                Bitmap thumbnail = BmpUtils.decodeFile(
                        filePath,
                        thumbnailWidth, thumbnailHeight);
                if(null == thumbnail) {
                    Log.e("make thumbnail failed");
                    operationFailed = true;
                } else {
                    File f = makeOutputMediaFile(MEDIA_TYPE_THUMNAIL, ".jpg");
                    if(f != null) {
                        FileOutputStream o = new FileOutputStream(f);
                        if(thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, o))
                            thumbnailFile = f;
                        o.close();
                    } else {
                        operationFailed = true;
                    }

                    BmpUtils.recycleABitmap(thumbnail);
                }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                operationFailed = true;
            } catch (IOException e) {
                e.printStackTrace();
                operationFailed = true;
            } catch (Exception e) {
                e.printStackTrace();
                operationFailed = true;
            }
        }

        if(!operationFailed) {
            path[0] = filePath;
            path[1] = thumbnailFile == null ? null : thumbnailFile.getAbsolutePath();
        }

        return !operationFailed;
    }

    /**
     *
     * @param activity
     * @param data
     * @param limitWidth not implemented
     * @param limitHeight not implemented
     * @param thumbnailWidth
     * @param thumbnailHeight
     * @param path
     * @return
     */
    public boolean handleVideoResult(Activity activity,
                                     Intent data,
                                     int limitWidth,
                                     int limitHeight,
                                     int thumbnailWidth,
                                     int thumbnailHeight,
                                     String[] path) {

        Uri uri = null;
        if(data == null || data.getData() == null) {
            // got a new photo via camera
            uri = mLastVideoUri;
        } else {
            // picked a existed picture
            uri = data.getData();
        }

        final Uri furi = uri;

        try {
            String videopath = resolveMediaPath(activity, furi, MEDIA_TYPE_VIDEO);
            Bitmap thumbnail = null;
            // MINI_KIND => 960x544,
            // MICRO_KIND is too small

				/* It is suspected that on some devices (Sharp-IS12), the video file is not available immediately,
				 * as ThumbnailUtils.createVideoThumbnail() would return null.
				 */
            Thread.sleep(1500);

            Bitmap thumbnail0 = ThumbnailUtils.createVideoThumbnail(
                    videopath, MediaStore.Images.Thumbnails.MINI_KIND);
            if(thumbnail0 != null) {
                // ... while MINI_KIND is a little too large
                thumbnail = ThumbnailUtils.extractThumbnail(
                        thumbnail0,
                        thumbnailWidth, thumbnailHeight,
                        ThumbnailUtils.OPTIONS_RECYCLE_INPUT);
                thumbnail0.recycle();
            } else {
                thumbnail = ThumbnailUtils.createVideoThumbnail(
                        videopath, MediaStore.Images.Thumbnails.FULL_SCREEN_KIND);
            }

            File fbmp = null;
            if(thumbnail != null) {
                fbmp = makeOutputMediaFile(MEDIA_TYPE_THUMNAIL, ".jpg");
                OutputStream os = new FileOutputStream(fbmp);
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, os);
                os.close();
                thumbnail.recycle();
            }

            path[0] = videopath;
            path[1] = fbmp == null ? null : fbmp.getAbsolutePath();

            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

	/**
	 * Create a File for saving an image or video
	 * @param type MEDIA_TYPE_*
	 * @param ext String file name extension, starts with ".", e.g., ".m4a". can be null.
	 * @return
	 */
	public static File makeOutputMediaFile(int type, String ext){
		// To be safe, you should check that the SDCard is mounted
		// using Environment.getExternalStorageState() before doing this.

		File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "onemeter/.cache");
		// This location works best if you want the created images to be shared
		// between applications and persist after your app has been uninstalled.

		// Create the storage directory if it does not exist
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				return new File("");
			}
		}

		// Create a media file name
		String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss_", Locale.getDefault()).format(new Date());
		String rand = Integer.toString((int)(Math.random() * 1000));
		File mediaFile;
		if (type == MEDIA_TYPE_IMAGE){
			if(ext == null) ext = ".jpg";
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"IMG_"+ timeStamp + rand + ext);
		} else if(type == MEDIA_TYPE_VIDEO) {
			if(ext == null) ext = ".mp4";
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"VID_"+ timeStamp + rand + ext);
		} else if(type == MEDIA_TYPE_VOICE) {
			if(ext == null) ext = "." + ChatMessage.SEND_AUDIO_EXT;
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"AUD_"+ timeStamp + rand + ext);
		} else if(type == MEDIA_TYPE_THUMNAIL) {
			if(ext == null) ext = ".png";
			mediaFile = new File(mediaStorageDir.getPath() + File.separator +
					"THM_"+ timeStamp + rand + ext);
		} else {
			return null;
		}

		return mediaFile;
	}

    /**
	 * convert Uri scheme from "content" to "file".
	 * @param activity
	 * @param uri
	 * @param mediaType MEDIA_TYPE_* constants
	 * @return
	 */
	@TargetApi(Build.VERSION_CODES.KITKAT)
    private static String resolveMediaPath(Activity activity, Uri uri, int mediaType) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;
        // DocumentProvider
        //Log.i("-- DocumentsContract--" +  DocumentsContract.isDocumentUri(activity, uri)); //actually always return false
        //Log.i("-- getScheme--" + uri.getScheme()); //when choose pic  alwasys return "file"

        if (isKitKat && DocumentsContract.isDocumentUri(activity, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {
                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                return getDataColumn(activity, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];
                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                final String selection = "_id=?";
                final String[] selectionArgs = new String[] { split[1] };
                return getDataColumn(activity, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            // Return the remote address
            if (isGooglePhotosUri(uri)) {
                return uri.getLastPathSegment();
            }
            return getDataColumn(activity, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }
        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
            String[] selectionArgs) {
        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = { column };
        try {
            cursor = context.getContentResolver().query(
                    uri, projection, selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                int index = cursor.getColumnIndexOrThrow(column);
                String path = cursor.getString(index);
                return path;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    private static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel arg0, int arg1) {
		arg0.writeParcelable(mLastImageUri, 0);
		arg0.writeParcelable(mLastVideoUri, 0);
	}

	public static final Parcelable.Creator<MediaInputHelper> CREATOR =
			new Parcelable.Creator<MediaInputHelper>() {

				@Override
				public MediaInputHelper createFromParcel(Parcel souParcel) {
					MediaInputHelper m = new MediaInputHelper(null);
					m.mLastImageUri = souParcel.readParcelable(Uri.class.getClassLoader());
					m.mLastVideoUri = souParcel.readParcelable(Uri.class.getClassLoader());
					return m;
				}

				@Override
				public MediaInputHelper[] newArray(int size) {
					return new MediaInputHelper[size];
				}

			};

    /**
     * Generate thumbnail for image.
     * @return thumbnail absolute path.
     */
    public static String generateThumbnailForImage(
            String originalFileName, int thumbWidth, int thumbHeight) {

        String result = null;
        File f = MediaInputHelper.makeOutputMediaFile(MediaInputHelper.MEDIA_TYPE_THUMNAIL, ".jpg");
        if (f != null) {
            result = f.getAbsolutePath();
            Bitmap thumbnail = BmpUtils.decodeFile(originalFileName,
                    thumbWidth, thumbHeight);
            try {
                FileOutputStream fos = new FileOutputStream(result);
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
            }
            catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
