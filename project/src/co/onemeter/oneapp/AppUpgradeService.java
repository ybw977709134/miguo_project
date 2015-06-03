package co.onemeter.oneapp;

import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import co.onemeter.oneapp.utils.ChecksumUtil;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 *
 * <p>
 * User: pan
 * Date: 13-5-21
 * Time: PM4:34
 * </p>
 */
public class AppUpgradeService extends android.app.Service {

    public static final String EXTRA_URL = "url";
    public static final String EXTRA_DEST_FILENAME = "dest_filename";
    public static final String EXTRA_MD5SUM = "md5sum";

    /**
     * The single instance is running?
     */
    private static boolean running = false;

    int notiId; // unique
    NotificationManager mNotifyManager;
    NotificationCompat.Builder mBuilder;

    public static boolean isRunning() {
        return running;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!running) {
            running = true;
            handleCommand(intent);
        }
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleCommand(final Intent intent) {
        if (intent == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                handleCommand_uploadMomentFiles(intent);
            }
        }).start();
    }

    private void handleCommand_uploadMomentFiles(Intent intent) {
        final String apkUrl = intent.getStringExtra(EXTRA_URL);
        final String filename = intent.getStringExtra(EXTRA_DEST_FILENAME);
        final String expectedMd5sum = intent.getStringExtra(EXTRA_MD5SUM);

        createNotice();

        try {
            if (BuildConfig.DEBUG) {
                Log.d("upgrade", "start downloading " + apkUrl + " to " + filename);
            }

            URL url = new URL(apkUrl);
            URLConnection connection = url.openConnection();
            connection.connect();
            // this will be useful so that you can show a typical 0-100% progress bar
            int fileLength = connection.getContentLength();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(filename);

            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while (!isCancelled() && (count = input.read(data)) != -1) {
                total += count;

                // publishing the progress....
                int progress = ((int) (total * 100 / fileLength));
                mBuilder.setProgress(100, progress, false);
                mNotifyManager.notify(notiId, mBuilder.build());

                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            if (isCancelled()) {
                return;
            }

            if (expectedMd5sum != null) {
                String actualMd5 = ChecksumUtil.calculateMD5(new File(filename));
                if (expectedMd5sum.equals(actualMd5)) {
                    dismissNotice();
                    // install!
                    installApk(filename);
                } else {
                    mBuilder.setOngoing(false);
                    updateNoticeMsg(getString(R.string.upgrade_failed_md5sum));
                    return;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
            mBuilder.setOngoing(false);
            updateNoticeMsg(getString(R.string.upgrade_failed_download));
        } catch (Exception e) {
            e.printStackTrace();
            mBuilder.setOngoing(false);
            updateNoticeMsg(getString(R.string.upgrade_failed_unknown));
        } finally {
            running = false;
        }
    }

    private void createNotice() {
        notiId = (int) (System.currentTimeMillis() & 0xFFFFFFFF); // unique
        mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.upgrade_downloading))
                .setTicker(getString(R.string.upgrade_downloading))
                .setContentText(getString(R.string.app_name))
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true);
        mNotifyManager.notify(notiId, mBuilder.build());
    }

    private void updateNoticeMsg(String msg) {
        mBuilder.setContentTitle(msg)
                .setTicker(msg)
                .setContentText(null)
                .setAutoCancel(true);
        mNotifyManager.notify(notiId, mBuilder.build());
    }

    private void dismissNotice() {
        mNotifyManager.cancel(notiId);
    }

    private boolean isCancelled() {
        return false; // TODO
    }

    private void installApk(String apkPath) {
        // try to invoke the package installer directly by explicitly setting Intent component,
        // to avoid the "verify and install" alternative to perform this action.
        try {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setClassName("com.android.packageinstaller",
                            "com.android.packageinstaller.PackageInstallerActivity")
                    .setDataAndType(Uri.fromFile(new File(apkPath)),
                            "application/vnd.android.package-archive"));
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Intent.ACTION_VIEW)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .setDataAndType(Uri.fromFile(new File(apkPath)),
                            "application/vnd.android.package-archive"));
        }
    }
}
