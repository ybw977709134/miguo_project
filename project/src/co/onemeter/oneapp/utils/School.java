package co.onemeter.oneapp.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import org.wowtalk.Log;
import org.wowtalk.api.*;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 获取学校的显示信息（名称、头像），自动缓存。
 *
 * Created by pzy on 6/7/15.
 */
public class School {

    /** Does this UUID identify a school? */
    public static boolean isSchoolId(Context context, String uuid) {
        List<GroupChatRoom> schools = new Database(context).fetchSchoolsNoBuddies();
        if (schools != null && !schools.isEmpty()) {
            for (GroupChatRoom school : schools) {
                if (TextUtils.equals(school.groupID, uuid))
                    return true;
            }
        }
        return false;
    }

    /**
     * 获取学校的显示信息（名称、头像），自动缓存。
     *
     * <p>{@link #onPostExecute(Object)} 的参数是
     * <pre>
     * map {
     *  name: 名称,
     *  avatar: 头像在文件服务器上的绝对路径,
     *  localAvatar: 头像在本地文件系统中的绝对路径,
     * }</pre>
     * </p>
     */
    public static class FetchDisplayInfoTask extends AsyncTask<String, Void, Map<String, String>> {

        /** school id => school info */
        private static HashMap<String, Map<String, String>> cache = new HashMap<>();
        private final Context context;
        boolean downloadOk = true;
        private String schoolId;

        public FetchDisplayInfoTask(Context context) {
            this.context = context;
        }

        @Override
        protected Map<String, String> doInBackground(String... strings) {
            try {
                Map<String, String> info;
                schoolId = strings[0];
                info = cache.get(schoolId);
                if (info != null && isFresh(info))
                    return info;

                info = WowTalkWebServerIF.getInstance(context).getSchoolInfo(schoolId);
                if (!info.isEmpty()) {
                    NetworkIFDelegate nd = new NetworkIFDelegate() {

                        @Override
                        public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
                            downloadOk = false;
                            Log.e("School.displayPhoto() failed to download: " + new String(arg1).toString());
                        }

                        @Override
                        public void didFinishNetworkIFCommunication(int arg0, byte[] arg1) {
                            downloadOk = true;
                            Log.e("School.displayPhoto() succeed");
                        }

                        @Override
                        public void setProgress(int arg0, int arg1) {
                        }

                    };
                    String localAvatar = getAvatarLocalPath(info.get("avatar"));
                    if (new File(localAvatar).exists()
                            && new File(localAvatar).lastModified() > System.currentTimeMillis() - 24 * 3600 * 1000) {
                        // hit disk cache
                        info.put("localAvatar", localAvatar);
                    } else {
                        RemoteFileService.download(context,
                                localAvatar,
                                "",
                                info.get("avatar"),
                                nd,
                                0);
                        if (downloadOk) {
                            info.put("localAvatar", localAvatar);
                        }
                    }

                    markTimestamp(info);
                    cache.put(schoolId, info);
                    return info;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        private static boolean isFresh(Map<String, String> schoolInfo) {
            String timestamp = schoolInfo.get("timestamp");
            return timestamp != null && Long.parseLong(timestamp) > System.currentTimeMillis() / 1000 - 5 * 60;
        }

        private static void markTimestamp(Map<String, String> schoolInfo) {
            schoolInfo.put("timestamp", Long.toString(System.currentTimeMillis() / 1000));
        }

        private String getAvatarLocalPath(String remotePath) {
            String ext = "";
            int i = remotePath.lastIndexOf('.');
            if (i != -1)
                ext = remotePath.substring(i);
            return Database.makeLocalFilePath(schoolId, ext);
        }
    }

}
