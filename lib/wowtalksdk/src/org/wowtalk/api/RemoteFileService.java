package org.wowtalk.api;

import android.content.Context;
import android.text.TextUtils;
import co.onemeter.utils.AsyncTaskExecutor;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.wowtalk.Log;

import java.io.File;
import java.net.URLEncoder;
import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * <p>Unity remote file storage service, delegating upload/download task to
 * S3 or OSS or Web client implementation.</p>
 * Created by pzy on 11/11/14.
 */
public class RemoteFileService {

    private static final String TAG = "RemoteFileService";

    public static void upload(
            Context context,
            String inFilename, String remoteDir, String remoteFileId,
            NetworkIFDelegate callback, int callbackTag) {

        if (TextUtils.isEmpty(inFilename)) {
            String errmsg = "inFilename is required";
            if (callback != null)
                callback.didFailNetworkIFCommunication(callbackTag, errmsg.getBytes());
            new InvalidParameterException(errmsg).printStackTrace(); // for debug
            return;
        }

        if (!new File(inFilename).exists()) {
            String errmsg = "local file " + inFilename + " not found.";
            if (callback != null)
                callback.didFailNetworkIFCommunication(callbackTag, errmsg.getBytes());
            new InvalidParameterException(errmsg).printStackTrace(); // for debug
            return;
        }

        PrefUtil sPrefUtil = PrefUtil.getInstance(context);
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        try {
            if (sPrefUtil.isUseOss()) {
                if (TextUtils.isEmpty(remoteFileId))
                    remoteFileId = UUID.randomUUID().toString();
                new OssClient(
                        sPrefUtil.getS3Uid(),
                        sPrefUtil.getS3Pwd(),
                        sPrefUtil.getS3Bucket())
                        .setCallback(callback, callbackTag)
                        .setRemoteDir(remoteDir)
                        .upload(remoteFileId, inFilename);
            }
            else if(sPrefUtil.isUseS3()){
                S3FileUploader uploadFile = new S3FileUploader(sPrefUtil);
                uploadFile.setup(callback, callbackTag, inFilename);
                uploadFile.uploadFile(remoteDir);
            }
            else{
                List<NameValuePair> postData = new ArrayList<NameValuePair>(2);
                postData.add(new BasicNameValuePair(URLEncoder.encode("uid",
                        "UTF-8"), URLEncoder.encode(strUID, "UTF-8")));
                postData.add(new BasicNameValuePair(URLEncoder.encode("password",
                        "UTF-8"), URLEncoder.encode(strPwd, "UTF-8")));
                postData.add(new BasicNameValuePair(URLEncoder.encode("action",
                        "UTF-8"), URLEncoder.encode("upload_file", "UTF-8")));

                UploadFile uploadFile = new UploadFile();
                uploadFile.setup(callback, callbackTag, inFilename);
                AsyncTaskExecutor.executeLongNetworkTask(uploadFile, postData);
            }
        } catch (Exception e) {
            Log.e(TAG, " upload failed.");
            e.printStackTrace();
            if (callback != null) {
                callback.didFailNetworkIFCommunication(callbackTag, e.getMessage().getBytes());
            }
        }
    }

    public static void download(
            Context context,
            String outFilename, String remoteDir, String remoteFileId,
            NetworkIFDelegate callback, int callbackTag) {

        if (TextUtils.isEmpty(outFilename)) {
            String errmsg = "outFilename is required";
            if (callback != null)
                callback.didFailNetworkIFCommunication(callbackTag, errmsg.getBytes());
            new InvalidParameterException(errmsg).printStackTrace(); // for debug
            return;
        }

        if (TextUtils.isEmpty(remoteFileId)) {
            String errmsg = "remoteFileId is required";
            if (callback != null)
                callback.didFailNetworkIFCommunication(callbackTag, errmsg.getBytes());
            new InvalidParameterException(errmsg).printStackTrace(); // for debug
            return;
        }

        PrefUtil sPrefUtil = PrefUtil.getInstance(context);
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        try {
            if (sPrefUtil.isUseOss()) {
                new OssClient(
                        sPrefUtil.getS3Uid(),
                        sPrefUtil.getS3Pwd(),
                        sPrefUtil.getS3Bucket())
                        .setCallback(callback, callbackTag)
                        .setRemoteDir(remoteDir)
                        .download(remoteFileId, outFilename);
            }
            else if(sPrefUtil.isUseS3()){
                S3FileDownloader downloadFile = new S3FileDownloader(context);
                downloadFile.setup(callback, callbackTag, outFilename);
                downloadFile.downloadFile(remoteFileId, remoteDir);
            }
            else{
                String action = "get_file";
                if (TextUtils.equals(remoteDir, GlobalSetting.S3_PROFILE_PHOTO_DIR)) {
                    action = "get_photo";
                } else if (TextUtils.equals(remoteDir, GlobalSetting.S3_PROFILE_THUMBNAIL_DIR)) {
                    action = "get_thumbnail";
                } else if (TextUtils.equals(remoteDir, GlobalSetting.S3_SHOP_DIR)) {
                    action = "get_file_from_shop";
                }
                String postData = URLEncoder.encode("uid", "UTF-8") + "="
                        + URLEncoder.encode(strUID, "UTF-8");
                postData += "&" + URLEncoder.encode("password", "UTF-8") + "="
                        + URLEncoder.encode(strPwd, "UTF-8");
                postData += "&" + URLEncoder.encode("file_id", "UTF-8") + "="
                        + URLEncoder.encode(remoteFileId, "UTF-8");
                postData += "&" + URLEncoder.encode("action", "UTF-8") + "="
                        + URLEncoder.encode(action, "UTF-8");

                DownloadFile downloadFile = new DownloadFile();
                downloadFile.setup(callback, callbackTag, null);
                AsyncTaskExecutor.executeLongNetworkTask(downloadFile, postData);
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e(TAG, " download failed");
            if (callback != null)
                callback.didFailNetworkIFCommunication(callbackTag, e.getMessage().getBytes());
        }
    }
}
