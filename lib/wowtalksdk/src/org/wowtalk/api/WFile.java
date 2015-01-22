package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Describe a multimedia file. 
 */
public class WFile implements Parcelable {

    private static final String EXT_PATTERN = "^\\.?(jpe?g|png|mp4|m4a|aac|3gpp|avi|wav)$";

    /**
     * File name extension.
     */
    private String ext;
	/**
	 * Remote file id.
	 */
	public String fileid;
    /**
     * Remote thumbnail file id.
     */
    public String thumb_fileid;
    /**
     * Remote Database ID.
     */
    public String remoteDbId;
	/**
	 * Local file path.
	 */
    public String localPath;
    /**
     * Local thumbnail file path.
     */
    public String localThumbnailPath;
    /**
     * Local Database ID.
     */
    public int localDbId;
    /**
     * Duration in seconds, only useful if this media is a voice.
     */
    public int duration;

    /**
     * Remote relative dir name, defaults as GlobalSetting.S3_UPLOAD_FILE_DIR.
     */
    public String remoteDir = GlobalSetting.S3_UPLOAD_FILE_DIR;

	public WFile() {
	}

    /**
     * Ctor for photo or video.
     * @param ext
     * @param fileid
     * @param thumb_fileid
     * @param localPath
     */
    public WFile(String ext, String fileid, String thumb_fileid, String localPath) {
        this.fileid = fileid;
        this.thumb_fileid = thumb_fileid;
        this.setExt(ext);
        this.duration = 0;
        this.localPath = localPath;
        this.remoteDbId = null;
        this.localDbId = 0;
    }

    /**
     * Ctor for voice.
     * @param ext
     * @param fileid
     * @param duration
     * @param localPath
     */
    public WFile(String ext, String fileid, int duration, String localPath) {
		this.fileid = fileid;
        this.thumb_fileid = null;
        this.setExt(ext);
        this.duration = duration;
		this.localPath = localPath;
        this.remoteDbId = null;
        this.localDbId = 0;
	}

    /** 移除文件扩展名的点号。 */
    static String trimExt(String ext) {
        if (ext != null && ext.length() > 1 && ext.charAt(0) == '.') {
            ext = ext.substring(1);
        }
        return ext;
    }

    @Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(duration);
        dest.writeString(getExt());
        dest.writeString(fileid);
        dest.writeInt(localDbId);
        dest.writeString(localPath);
        dest.writeString(localThumbnailPath);
        dest.writeString(remoteDbId);
        dest.writeString(remoteDir);
        dest.writeString(thumb_fileid);
	}

    protected void loadFromParcel(Parcel source) {
        duration = source.readInt();
        setExt(source.readString());
        fileid = source.readString();
        localDbId = source.readInt();
        localPath = source.readString();
        localThumbnailPath = source.readString();
        remoteDbId = source.readString();
        remoteDir = source.readString();
        thumb_fileid = source.readString();
    }

	public static Parcelable.Creator<WFile> CREATOR =
			new Parcelable.Creator<WFile>() {

		@Override
		public WFile createFromParcel(Parcel source) {
			WFile f = new WFile();
            f.loadFromParcel(source);
			return f;
		}

		@Override
		public WFile[] newArray(int size) {
			return new WFile[size];
		}
	};

    /**
     * Get file name extension.
     */
    public String getExt() {
        return ext;
    }

    /**
     * Set file name extension.
     * @param ext regex pattern: "^\\.?(jpe?g|png|mp4|m4a|aac|3gpp|avi|wav)$"
     */
    public void setExt(String ext) {
        if (ext != null && !isValidExt(ext)) {
//            Log.w("expect ext to be like " + EXT_PATTERN + ", got " + ext);
        }
        if (ext == null) {
            this.ext = ext;
            return;
        }
        if (ext.startsWith("."))
            this.ext = ext.substring(1).toLowerCase();
        else
            this.ext = ext.toLowerCase();
    }

    public static boolean isValidExt(String ext) {
        return !Utils.isNullOrEmpty(ext)
                && ext.toLowerCase().matches(EXT_PATTERN);
    }

    /** 根据扩展名判断该文件是音频。 */
    public boolean isAudioByExt() {
        return ext != null && ext.matches("aac|m4a|3gpp");
    }

    /** 根据扩展名判断该文件是图像。 */
    public boolean isImageByExt() {
        return ext != null && ext.matches("jpg|jpeg|bmp|png");
    }

    /** 根据扩展名判断该文件是视频。 */
    public boolean isVideoByExt() {
        return ext != null && ext.matches("avi|wmv|mp4|asf|mpg|mp2|mpeg|mpe|mpv|m2v|m4v|3gp");
    }
}
