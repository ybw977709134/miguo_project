package org.wowtalk.api;

import android.os.AsyncTask;
import org.wowtalk.Log;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;

class DownloadFile extends AsyncTask<String, Integer, String> {
	NetworkIFDelegate networkIFDelegate;
	int tag;
	String outputFilepath;

	public void setup(NetworkIFDelegate aDelegate, int aTag,
			String outputPath) {
		networkIFDelegate = aDelegate;
		tag = aTag;
		outputFilepath = outputPath;
	}

	@Override
	protected String doInBackground(String... sData) {
		try {
			Log.e("DownloadFile", "start...............................");

			// data += "&" + URLEncoder.encode("file_id", "UTF-8") + "=" +
			// URLEncoder.encode("get_file", "UTF-8");
			// Send data
			URL url = new URL(
					GlobalSetting.WEB_HOST_HTTP);
			URLConnection conn = url.openConnection();
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
			Log.e("fileLength:", fileLength);
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
				Log.e("Download file length:", res.length);

			} else {
				res = null;
				Log.e("Download file is null");
			}

			input.close();
			if (networkIFDelegate != null) {
				networkIFDelegate.didFinishNetworkIFCommunication(tag, res);
			}

		} catch (Exception e) {
			Log.e("Download failed!" + e.toString());
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


