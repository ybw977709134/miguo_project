package co.onemeter.oneapp.utils;

import android.content.Context;
import android.os.AsyncTask;
import android.text.TextUtils;
import org.wowtalk.Log;
import org.wowtalk.api.*;

import java.io.File;
import java.util.List;
import java.util.Map;

/**
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

    public static class FetchDisplayInfoTask extends AsyncTask<String, Void, Map<String, String>> {

        private final Context context;
        boolean downloadOk = true;
        private String schoolId;

        public FetchDisplayInfoTask(Context context) {
            this.context = context;
        }

        @Override
        protected Map<String, String> doInBackground(String... strings) {
            try {
                schoolId = strings[0];
                Map<String, String> info = WowTalkWebServerIF.getInstance(context).getSchoolAvatarPath(schoolId);
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
                    String localPath = getAvatarLocalPath(info.get("avatar"));
                    if (new File(localPath).exists()
                            && new File(localPath).lastModified() > System.currentTimeMillis() - 24 * 3600 * 1000) {
                        // hit disk cache
                        info.put("localPath", localPath);
                    } else {
                        RemoteFileService.download(context,
                                localPath,
                                "",
                                info.get("avatar"),
                                nd,
                                0);
                        if (downloadOk) {
                            info.put("localPath", localPath);
                        }
                    }
                    return info;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
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
