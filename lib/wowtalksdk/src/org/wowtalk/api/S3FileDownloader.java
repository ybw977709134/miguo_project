package org.wowtalk.api;

import android.content.Context;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.transfer.Download;
import com.amazonaws.services.s3.transfer.TransferManager;
import org.wowtalk.Log;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

class S3FileDownloader {

	private Context mContext;
	private NetworkIFDelegate networkIFDelegate;
	private int tag;
	String outputFilepath;
	private TransferManager s3tx;
	Download myDownload;
	public CancelFlag cancelFlag = null;

    private String mS3Uid;
    private String mS3Pwd;
    private String mS3Bucket;

    public S3FileDownloader(Context context) {
        mContext = context;
        PrefUtil prefUtil = PrefUtil.getInstance(context);
        mS3Uid = prefUtil.getS3Uid();
        mS3Pwd = prefUtil.getS3Pwd();
        mS3Bucket = prefUtil.getS3Bucket();
    }

	public void setup(NetworkIFDelegate aDelegate, int aTag,
			String outputPath) {
		networkIFDelegate = aDelegate;
		tag = aTag;
		outputFilepath = outputPath;
	}

	public void downloadThumbnail(String userID){
		Log.e("S3FileDownloader downloadThumbnail:", "userID=" + userID);
		String filePath = GlobalSetting.S3_PROFILE_THUMBNAIL_DIR + userID;
		this.download(filePath);
	}

	
	/**
	 * 
	 * @param targetID buddy uid or group id.
	 */
	public void downloadPhoto(String targetID){

		Log.e("S3FileDownloader downloadPhoto:", "userID=" + targetID);
		String filePath = GlobalSetting.S3_PROFILE_PHOTO_DIR + targetID;
		this.download(filePath);
	}

	public void downloadActivityLogo(String activityID){
		Log.e("S3FileDownloader downloadActivityLogo:", "actID=" + activityID);
		String filePath = GlobalSetting.S3_PROFILE_THUMBNAIL_DIR + "ACT-" + activityID;
		this.download(filePath);
	}

	public void downloadFile(String fileID){
		Log.e("S3FileDownloader downloadFile:", "fileID=" + fileID);
		String filePath = GlobalSetting.S3_UPLOAD_FILE_DIR + fileID;
		this.download(filePath);
	}

    /**
     *
     * @param fileID
     * @param fileDir optional, defaults as GlobalSetting.S3_UPLOAD_FILE_DIR
     */
    public void downloadFile(String fileID, String fileDir){
        Log.e("S3FileDownloader downloadFile:", "fileID=" + fileID);
        if (fileDir == null)
            fileDir = GlobalSetting.S3_UPLOAD_FILE_DIR;
        String filePath = fileDir + fileID;
        this.download(filePath);
    }

	public void downloadFileFromShop(String fileID){
		Log.e("S3FileDownloader downloadFileFromShop:", "fileID=" + fileID);
		String filePath = GlobalSetting.S3_SHOP_DIR + fileID;
		this.download(filePath);
	}


	// This method is automatically called by the image picker when an image
	// is selected.
	private void download(String filePath) {
        File myOutfile=null;
		try {

			s3tx = new TransferManager(new BasicAWSCredentials(mS3Uid, mS3Pwd));

			myOutfile = outputFilepath!=null?
					(new java.io.File(outputFilepath)) :File.createTempFile("prefix", "extension", mContext.getCacheDir());

					myDownload=s3tx.download(mS3Bucket, filePath, myOutfile);
					ObjectMetadata omd = myDownload.getObjectMetadata();
					long fileLength = omd.getContentLength();
					Log.e("File length: " +fileLength);

					while (myDownload.isDone() == false) {
						if(cancelFlag != null && cancelFlag.cancelled) {
							Log.e("  - cancelled by user");
							break;
						}
						Log.e("Transfer: " + myDownload.getDescription());
						Log.e("  - State: " + myDownload.getState());

						double bytesTransfered=myDownload.getProgress().getBytesTransfered();
						Log.e("  - Progress: " +bytesTransfered );
						if(networkIFDelegate != null)
							networkIFDelegate.setProgress(tag, (int) (bytesTransfered * 100 / fileLength));
						// Do work while we wait for our upload to complete...
						Thread.sleep(300);
					}

					double bytesTransfered=myDownload.getProgress().getBytesTransfered();
					Log.e("  - Progress: " +bytesTransfered );
					if(networkIFDelegate != null)
						networkIFDelegate.setProgress(tag, (int) (bytesTransfered * 100 / fileLength));

					Log.e("  - Total: " +bytesTransfered );

					if(networkIFDelegate!=null) {
						if(cancelFlag != null && cancelFlag.cancelled) {
                            myOutfile.delete();
							networkIFDelegate.didFailNetworkIFCommunication(tag, "Cancelled by user".getBytes());
                        } else {
							if(outputFilepath!=null){
								networkIFDelegate.didFinishNetworkIFCommunication(tag, null);
							}
							else{
								Log.e("  - No output filepath is set, return as byte array ^^");

								// download the file
								InputStream input = new BufferedInputStream(
										new FileInputStream(myOutfile));
								Log.e("  - fileLength:", myOutfile.length());
								byte data[] = new byte[1024];
								long total = 0;
								int count;

								byte res[] = new byte[1024];
								if ((count = input.read(res)) != -1) {
									total += count;
									while ((count = input.read(data)) != -1) {
										total += count;
										res = Utils.concat(res, data, count);
									}
									Log.e("  - file to byte array length:", res.length);

								} else {
									res = null;
									Log.e("  - file is null");
								}
								input.close();
								networkIFDelegate.didFinishNetworkIFCommunication(tag, res);
							}
						}
					}


		} catch (Exception e) {
			Log.e("S3FileDownloader Failure", e.getMessage());
			e.printStackTrace();
            myOutfile.delete();
			if(networkIFDelegate!=null) {networkIFDelegate.didFailNetworkIFCommunication(tag, e.getMessage().getBytes());}
		}
	}


}
