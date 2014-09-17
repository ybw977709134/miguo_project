package org.wowtalk.api;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.transfer.Transfer.TransferState;
import com.amazonaws.services.s3.transfer.TransferManager;
import com.amazonaws.services.s3.transfer.Upload;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wowtalk.Log;

import java.io.File;
import java.util.Date;
import java.util.Random;

class S3FileUploader {

	private PrefUtil mPrefUtil;
	private NetworkIFDelegate networkIFDelegate;
	private int tag;
	private String inputFilepath;
	private TransferManager s3tx;
    private String mS3Uid;
    private String mS3Pwd;
    private String mS3Bucket;

    public S3FileUploader(PrefUtil pref) {
        mPrefUtil = pref;
        mS3Uid = pref.getS3Uid();
        mS3Pwd = pref.getS3Pwd();
        mS3Bucket = pref.getS3Bucket();
    }

	public void setup(NetworkIFDelegate aDelegate, int aTag, String filePath) {
		networkIFDelegate = aDelegate;
		tag = aTag;
		inputFilepath = filePath;
	}


    /**
     * @param fileDir optional, defaults as GlobalSettings.S3_UPLOAD_FILE_DIR. ends with "/".
     */
	public void uploadFile(String fileDir) {
		String strUID = mPrefUtil.getUid();
		Date today = new Date();
		String fileID = strUID + "_"
				+ today.getTime() + "_"
				+ (new Random()).nextInt(10000);
        if (fileDir == null)
            fileDir = GlobalSetting.S3_UPLOAD_FILE_DIR;
		String filePath = fileDir + fileID;
		Log.i("S3FileUploader uploadFile:", "fileID=" + fileID);
		try {

			File inputFile = new File(inputFilepath);
			long fileLength = inputFile.length();
			Log.i("  - upload file length:", fileLength);
            BasicAWSCredentials credentials=new BasicAWSCredentials(mS3Uid, mS3Pwd);
			s3tx = new TransferManager(credentials);
			Upload myUpload = s3tx.upload(mS3Bucket, filePath,
					new java.io.File(inputFilepath));

			while (myUpload.isDone() == false) {
				Log.i("Transfer: " + myUpload.getDescription());
				Log.i("  - State: " + myUpload.getState());

				double bytesTransfered=myUpload.getProgress().getBytesTransfered();
				Log.i("  - Progress: " +bytesTransfered );
				if(networkIFDelegate!=null) {
					int percentage= (int) (bytesTransfered * 100 / fileLength);
					networkIFDelegate.setProgress(tag,percentage);
				}  
				// Do work while we wait for our upload to complete...
				Thread.sleep(300);
			} 
			double bytesTransfered=myUpload.getProgress().getBytesTransfered();
			Log.i("  - State: " + myUpload.getState());
			Log.i("  - Total: " +bytesTransfered );
			if(networkIFDelegate!=null) {
				if(myUpload.getState()==TransferState.Completed){
					networkIFDelegate.didFinishNetworkIFCommunication(tag, fileID.getBytes());
				}
				else{
					networkIFDelegate.didFailNetworkIFCommunication(tag, "uploadFile failed".getBytes());
				}
			}

            if(myUpload.getState()==TransferState.Completed) {
                AmazonS3 s3Client=new AmazonS3Client(credentials);
                s3Client.setObjectAcl(mS3Bucket, filePath, CannedAccessControlList.PublicRead);
            }

		} catch (Exception e) {
			Log.e("S3FileUploader Failure, ", e.getMessage());
			if(networkIFDelegate!=null) networkIFDelegate.didFailNetworkIFCommunication(tag, e.getMessage().getBytes());
		}
	}


	public void uploadProfilePhoto(){

		String strUID = mPrefUtil.getUid();
		String filePath = GlobalSetting.S3_PROFILE_PHOTO_DIR + strUID;
		Log.i("S3FileUploader uploadProfilePhoto:", "strUID=" + strUID);
		this.uploadPhoto(filePath, null);
	}
	public void uploadProfileThumbnail(){

		String strUID = mPrefUtil.getUid();
		String filePath = GlobalSetting.S3_PROFILE_THUMBNAIL_DIR + strUID;
		Log.i("S3FileUploader uploadProfileThumbnail:", "strUID=" + strUID);
		this.uploadPhoto(filePath, null);
	}
	
	public void uploadGroupPhoto(String group_id){
		if(group_id == null || group_id.equals(""))
			return;
		
		String filePath = GlobalSetting.S3_PROFILE_PHOTO_DIR + group_id;
		Log.i("S3FileUploader uploadGroupPhoto:", "group_id=" + group_id);
		this.uploadPhoto(filePath, group_id);
	}

	public void uploadGroupThumbnail(String group_id){
		if(group_id == null || group_id.equals(""))
			return;
		
		String filePath = GlobalSetting.S3_PROFILE_THUMBNAIL_DIR + group_id;
		Log.i("S3FileUploader uploadGroupThumbnail:", "group_id=" + group_id);
		this.uploadPhoto(filePath, group_id);
	}

	public void uploadActivityLogo(String activity_id){

		/* a activity id can never equal to a user id, so it's ok for activity and user
		 * to share the same file path format.
		 */
		String filePath = GlobalSetting.S3_PROFILE_THUMBNAIL_DIR + "ACT-" + activity_id;
		Log.i("S3FileUploader uploadActivityThumbnail:", "activity_id=" + activity_id);
		this.uploadPhoto(filePath, null);
	}

	/**
	 * 
	 * @param filePath
	 * @param forGroupID null if this photo is for a buddy, group id if this photo is for a group.
	 */
	private void uploadPhoto(String filePath, String forGroupID) {

		try {

			File inputFile = new File(inputFilepath);
			long fileLength = inputFile.length();
			Log.i("  - upload file length:", fileLength);

            BasicAWSCredentials credentials=new BasicAWSCredentials(mS3Uid, mS3Pwd);
			s3tx = new TransferManager(credentials);
			Upload myUpload = s3tx.upload(mS3Bucket, filePath,
					new java.io.File(inputFilepath));

			while (myUpload.isDone() == false) {
				Log.i("Transfer: " + myUpload.getDescription());
				Log.i("  - State: " + myUpload.getState());

				double bytesTransfered=myUpload.getProgress().getBytesTransfered();
				Log.i("  - Progress: " +bytesTransfered );
				if(networkIFDelegate!=null) {
					int percentage= (int) (bytesTransfered * 100 / fileLength);
					networkIFDelegate.setProgress(tag,percentage);
				}
				// Do work while we wait for our upload to complete...
				Thread.sleep(300);
			}
			double bytesTransfered=myUpload.getProgress().getBytesTransfered();
			Log.i("  - Progress: " +bytesTransfered );
			if(networkIFDelegate != null)
				networkIFDelegate.setProgress(tag, (int) (bytesTransfered * 100 / fileLength));
			Log.i("  - State: " + myUpload.getState());
			Log.i("  - Total: " +bytesTransfered );
            if (myUpload.getState() == TransferState.Failed) {
                if (networkIFDelegate != null) {
                    networkIFDelegate.didFailNetworkIFCommunication(tag, "uploadPhoto failed".getBytes());
                }
                return;
            }
            AmazonS3 s3Client=new AmazonS3Client(credentials);
            s3Client.setObjectAcl(mS3Bucket, filePath, CannedAccessControlList.PublicRead);


			//report photo upload timestamp
			String strUID = mPrefUtil.getUid();
			String strPwd = mPrefUtil.getPassword();

			if (strUID.equals("") || strPwd.equals("")) {
				throw new RuntimeException(
						"UserID and Password not set");
			}

			String postStr = null;
			if(forGroupID == null) {
				postStr = "action=update_my_upload_photo_timestamp&uid=" + strUID + "&password="
						+ strPwd;
			} else {
				postStr = "action=update_group_thumbnail_timestamp&uid="
						+ strUID
						+ "&password=" + strPwd
						+ "&group_id=" + Utils.urlencodeUtf8(forGroupID);
			}

			Connect2 connect2 = new Connect2();
			Element root = connect2.Post(postStr);

			int errno = 0;
			if (root != null) {

				// err_no要素のリストを取得
				NodeList errorList = root.getElementsByTagName("err_no");
				// error要素を取得
				Element errorElement = (Element) errorList.item(0);
				// error要素の最初の子ノード（テキストノード）の値を取得
				String errorStr = errorElement.getFirstChild().getNodeValue();

				if (errorStr.equals("0")) {
					errno = 0;
				} else {
					errno = Integer.parseInt(errorStr);
				}
			} else {
				errno = -1;
			}				     


			if(networkIFDelegate!=null) {
				if(myUpload.getState()==TransferState.Completed && errno==0){
					networkIFDelegate.didFinishNetworkIFCommunication(tag, null);
				}
				else{
					networkIFDelegate.didFailNetworkIFCommunication(tag, "uploadPhoto failed".getBytes());
				}
			}

		} catch (Exception e) {
			Log.e("S3FileUploader uploadPhotoOrThumbnail Failure", e.getMessage());
			if(networkIFDelegate!=null) networkIFDelegate.didFailNetworkIFCommunication(tag, e.getMessage().getBytes());
		}
	}


}