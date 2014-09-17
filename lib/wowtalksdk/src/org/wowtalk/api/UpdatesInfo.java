package org.wowtalk.api;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-5-14
 * Time: PM9:23
 * To change this template use File | Settings | File Templates.
 */
public class UpdatesInfo {
    public int versionCode;
    public String versionName;
    /**
     * the link of the latest apk
     */
    public String link;
    public String[] changeLog;
    /**
     * MD5 sum of apk file for downloading.
     */
    public String md5sum;
}
