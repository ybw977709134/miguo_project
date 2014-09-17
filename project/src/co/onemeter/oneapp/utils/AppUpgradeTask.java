package co.onemeter.oneapp.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;
import co.onemeter.oneapp.BuildConfig;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;

/**
 * Execute argument: apk url, output filename.
 *
 * The output filename is passed to onPostExecute(), if it's null, check lastError().
 *
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-5-14
 * Time: PM7:39
 */
public class AppUpgradeTask extends AsyncTask<String, Integer, String> {
    public static final int ERR_OK = 0;
    public static final int ERR_UNKNOWN = 1;
    /** the downloaded file is corrupted. */
    public static final int ERR_CHECKSUM = 2;
    public static final int ERR_CANCELLED = 3;

    Context mContext;
    private boolean mIsExecuting = false;
    public String expectedMd5sum = null;
    public int mLastErrno = 0;

    /**
     *
     * @param context
     * @param md5sum optional.
     */
    public AppUpgradeTask(Context context, String md5sum) {
        mContext = context;
        expectedMd5sum = md5sum;
    }

    /**
     * return APK file path;
     */
    @Override
    protected String doInBackground(String... params) {
        mIsExecuting = true;

        try {
            if (BuildConfig.DEBUG) {
                Log.d("upgrade", "start downloading " + params[0] + " to " + params[1]);
            }

            URL url = new URL(params[0]);
            URLConnection connection = url.openConnection();
            connection.connect();
            // this will be useful so that you can show a typical 0-100% progress bar
            int fileLength = connection.getContentLength();

            // download the file
            InputStream input = new BufferedInputStream(url.openStream());
            OutputStream output = new FileOutputStream(params[1]);

            byte data[] = new byte[1024];
            long total = 0;
            int count;
            while (!isCancelled() && (count = input.read(data)) != -1) {
                total += count;
                // publishing the progress....
                publishProgress((int) (total * 100 / fileLength));
                output.write(data, 0, count);
            }

            output.flush();
            output.close();
            input.close();

            if (isCancelled()) {
                mIsExecuting = false;
                mLastErrno = ERR_CANCELLED;
                return null;
            }

            if (expectedMd5sum != null) {
                String actualMd5 = ChecksumUtil.calculateMD5(new File(params[1]));
                if (!expectedMd5sum.equals(actualMd5)) {
                    mLastErrno = ERR_CHECKSUM;
                    return null;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            mIsExecuting = false;
            mLastErrno = ERR_UNKNOWN;
            return null;
        }
        mIsExecuting = false;
        return params[1];
    }

    @Override
    public void onPostExecute(String path) {
        mIsExecuting = false;

        if(path != null) {
            File file = new File(path);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
            mContext.startActivity(intent);
        }
    }

    @Override
    protected void onCancelled() {
        mIsExecuting = false;
        super.onCancelled();
    }

    public boolean isExecuting() {
        return mIsExecuting;
    }

    public int lastError() {
        return mLastErrno;
    }
}
