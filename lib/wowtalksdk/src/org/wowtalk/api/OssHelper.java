package org.wowtalk.api;

import android.util.Log;
import com.aliyun.openservices.oss.OSSClient;
import com.aliyun.openservices.oss.model.GetObjectRequest;
import com.aliyun.openservices.oss.model.ObjectMetadata;
import com.aliyun.openservices.oss.model.PutObjectResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

/**
 * Upload/download file to/from Aliyun OSS.
 *
 * <p>使用 OSS Java SDK，遇到以下错误：</p>
 * <pre>
 *     Caused by: java.lang.NoSuchMethodError: org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager.<init>
         at com.aliyun.common.comm.HttpFactory.createThreadSafeClientConnManager(HttpFactory.java:105)
         at com.aliyun.common.comm.HttpFactory.createHttpClient(HttpFactory.java:66)
         at com.aliyun.common.comm.DefaultServiceClient.<init>(DefaultServiceClient.java:39)
         at com.aliyun.openservices.oss.OSSClient.<init>(OSSClient.java:149)
         at com.aliyun.openservices.oss.OSSClient.<init>(OSSClient.java:105)
 * </pre>
 *
 * Created by pzy on 11/10/14.
 */
public class OssHelper {

    private static final String TAG = "OssFileUploader";
    private OSSClient ossClient;

    public OssHelper(String accessKeyId, String accessKeySerect) {
        ossClient = new OSSClient(accessKeyId, accessKeySerect);
    }

    public PutObjectResult upload(String bucketName, String key, String filename)
            throws FileNotFoundException {
        return upload(bucketName, key, filename, null, 0);
    }

    public PutObjectResult upload(String bucketName, String key, String filename,
                                  NetworkIFDelegate callback, int callbackTag)
            throws FileNotFoundException {

        File file = new File(filename);
        ObjectMetadata objectMeta = new ObjectMetadata();
        objectMeta.setContentLength(file.length());

        InputStream input = new FileInputStream(file);
        PutObjectResult result = ossClient.putObject(bucketName, key, input, objectMeta);
        Log.i(TAG, "OSS.putObject result: " + result.getETag());

        if (callback != null) {
            callback.didFinishNetworkIFCommunication(callbackTag, key.getBytes());
        }

        return result;
    }

    public boolean download(String bucketName, String key, String filename) {
        return download(bucketName, key, filename, null, 0);
    }

    public boolean download(String bucketName, String key, String filename,
                              NetworkIFDelegate callback, int callbackTag) {
        ObjectMetadata result = ossClient.getObject(new GetObjectRequest(bucketName, key),
                new File(filename));
        Log.i(TAG, "OSS.putObject result: " + result.getETag());

        if (callback != null) {
            callback.didFinishNetworkIFCommunication(callbackTag, filename.getBytes());
        }
        return true;
    }
}
