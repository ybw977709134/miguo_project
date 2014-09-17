package org.wowtalk.ui;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-7-11
 * Time: 下午2:35
 * To change this template use File | Settings | File Templates.
 */
public class PhotoWithThumbPair {
    private String thumbPath;
    private String thumbFileId;
    private String photoPath;
    private String photoFileId;
    private String ext;

    public PhotoWithThumbPair() {

    }

    public String getThumbPath() {
        return thumbPath;
    }

    public void setThumbPath(String thumbPath) {
        this.thumbPath = thumbPath;
    }

    public String getPhotoPath() {
        return photoPath;
    }

    public void setPhotoPath(String photoPath) {
        this.photoPath = photoPath;
    }

    public String getPhotoFileId() {
        return photoFileId;
    }

    public void setPhotoFileId(String photoFileId) {
        this.photoFileId = photoFileId;
    }

    public String getExt() {
        return ext;
    }

    public void setExt(String ext) {
        this.ext = ext;
    }

    public String getThumbFileId() {
        return thumbFileId;
    }

    public void setThumbFileId(String thumbFileId) {
        this.thumbFileId = thumbFileId;
    }
}
