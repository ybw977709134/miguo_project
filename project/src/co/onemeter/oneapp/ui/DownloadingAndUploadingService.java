package co.onemeter.oneapp.ui;

import android.content.Intent;
import android.os.IBinder;
import org.wowtalk.api.*;
import org.wowtalk.ui.PhotoDisplayHelper;
import org.wowtalk.ui.msg.FileUtils;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-5-21
 * Time: PM4:34
 * To change this template use File | Settings | File Templates.
 */
public class DownloadingAndUploadingService extends android.app.Service {

    public static final String EXTRA_ACTION = "action";
    public static final String EXTRA_MOMENT_ID = "moment_id";
    /** List&lt;WFile&gt; */
    public static final String EXTRA_WFILES = "wfiles";
    public static final String EXTRA_WFILE_CLASS = "wfile_class";

    public static final int ACTION_UPLOAD_MOMENT_FILE = 0;
    private static final String LOG_TAG = "DUService";

    private WowTalkWebServerIF mWeb;
    private WowMomentWebServerIF mMomentWeb;
    private Database mDb;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mWeb = WowTalkWebServerIF.getInstance(this);
        mMomentWeb = WowMomentWebServerIF.getInstance(this);
        mDb = Database.open(this);

        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleCommand(final Intent intent) {
        if (intent == null) return;

        int action = intent.getIntExtra(EXTRA_ACTION, -1);
        if (action == -1) return;

        if (action == ACTION_UPLOAD_MOMENT_FILE) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    handleCommand_uploadMomentFiles(intent);
                }
            }).start();
        }
    }

    private void handleCommand_uploadMomentFiles(Intent intent) {
        final String moment_id = intent.getStringExtra(EXTRA_MOMENT_ID);
        if (null == moment_id) return;

        ArrayList<WFile> files = intent.getParcelableArrayListExtra(EXTRA_WFILES);
        if (null == files || files.isEmpty()) return;

        for (WFile aMomentFile : files) {

            final Moment moment = new Moment();
            moment.id = moment_id;

            final WFile file=aMomentFile;
//            new AsyncTask<WFile, Integer, Integer>() {
//                WFile file;
//
//                @Override
//                protected Integer doInBackground(WFile... wPhotos) {
//                    file = wPhotos[0];

                    //
                    // upload thumbnail, if exists
                    //
                    if (null != file.localThumbnailPath) {
                        mWeb.fPostFileToServer(
                                file.localThumbnailPath,
                                file.remoteDir,
                                new NetworkIFDelegate() {
                                    @Override
                                    public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                        file.thumb_fileid = new String(bytes);

                                        String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
                                                file.thumb_fileid, file.getExt());
                                        FileUtils.copyFile(file.localThumbnailPath, destFilePath);
                                    }

                                    @Override
                                    public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                        String err = new String(bytes);
                                        android.util.Log.e(LOG_TAG, "failed to upload moment file: " + err);
                                    }

                                    @Override
                                    public void setProgress(int i, int i2) {
                                    }
                                }, 0);
                    }

                    //
                    // upload main file
                    //
                    mWeb.fPostFileToServer(
                            file.localPath,
                            file.remoteDir,
                            new NetworkIFDelegate() {
                                @Override
                                public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                    file.fileid = new String(bytes);

                                    String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
                                            file.fileid, file.getExt());
                                    FileUtils.copyFile(file.localPath, destFilePath);



                                    mDb.storeMultimedia(moment, file);

                                    mMomentWeb.fUploadMomentMultimedia(moment_id, file);
                                }

                                @Override
                                public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                    String err = new String(bytes);
                                    android.util.Log.e(LOG_TAG, "failed to upload moment file: " + err);
                                }

                                @Override
                                public void setProgress(int i, int i2) {
                                }
                            }, 0);

//                    return 0;
//                }
//
//                @Override
//                protected void onPostExecute(Integer errno) {
//
//                }
//            }.execute(file);

        }
    }
}
