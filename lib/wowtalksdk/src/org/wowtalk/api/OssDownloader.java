package org.wowtalk.api;

import android.os.AsyncTask;
import org.wowtalk.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * 通过 HTTP API 访问 Aliyun OSS。
 *
 * Created by pzy on 11/10/14.
 */
class OssDownloader extends AsyncTask<String, Integer, String> {
	private static final String TAG = "OssDownloader";

	private static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";

	private final String accessKeyId;
	private final String accessKeySerect;
	private final String bucketName;
	private String urlServer;
	private NetworkIFDelegate networkIFDelegate;
	private int tag;

	public OssDownloader(String accessKeyId, String accessKeySerect, String bucketName) {
		this.accessKeyId = accessKeyId;
		this.accessKeySerect = accessKeySerect;
		this.bucketName = bucketName;
		urlServer = String.format("http://%s.oss-cn-hangzhou.aliyuncs.com", bucketName);
	}

	public void setup(NetworkIFDelegate aDelegate, int aTag) {
		networkIFDelegate = aDelegate;
		tag = aTag;
	}

	@Override
	protected String doInBackground(String... sData) {
		try {
			Log.e(TAG, "start...............................");

			// data += "&" + URLEncoder.encode("file_id", "UTF-8") + "=" +
			// URLEncoder.encode("get_file", "UTF-8");
			// Send data
			URL url = new URL(urlServer);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			// If you invoke the method setDoOutput(true) on the
			// URLConnection, it will always use the POST method.
			conn.setDoOutput(true);
			OutputStreamWriter wr = new OutputStreamWriter(
					conn.getOutputStream());
			wr.write(sData[0]);
			wr.flush();

			// download the file
			InputStream input = new BufferedInputStream(
					conn.getInputStream());

			int fileLength = conn.getContentLength();
			Log.e(TAG, "fileLength:", fileLength);
			byte data[] = new byte[1024];
			long total = 0;
			int count;

			byte res[] = new byte[1024];
			if ((count = input.read(res)) != -1) {
				total += count;
				while ((count = input.read(data)) != -1) {
					total += count;
					// publishing the progress....
					publishProgress((int) (total * 100 / fileLength));

					res = Utils.concat(res, data, count);
				}
				Log.e(TAG, "Download file length:", res.length);

			} else {
				res = null;
				Log.e(TAG, "Download file is null");
			}

			input.close();
			if (networkIFDelegate != null) {
				networkIFDelegate.didFinishNetworkIFCommunication(tag, res);
			}

		} catch (Exception e) {
			Log.e(TAG, "Download failed!" + e.toString());
		}
		return null;
	}

	@Override
	protected void onPreExecute() {
		super.onPreExecute();
		if (networkIFDelegate != null) {
			networkIFDelegate.setProgress(tag, 0);
		}
	}

	@Override
	protected void onProgressUpdate(Integer... progress) {
		super.onProgressUpdate(progress);
		// Log.e("onProgressUpdate",progress[0]);
		if (networkIFDelegate != null) {
			networkIFDelegate.setProgress(tag, progress[0]);
		}
	}
}


