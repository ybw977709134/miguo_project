package org.wowtalk.ui.msg;

import android.content.Context;
import android.os.AsyncTask;
import org.wowtalk.Log;
import org.wowtalk.api.NetworkIFDelegate;
import org.wowtalk.api.WowTalkWebServerIF;

/**
 * 批量上传文件，得到的ID保存在 fileIds 中。
 * Created by pzy on 12/11/14.
 */
class PostFileTask extends AsyncTask<String, Integer, Boolean> {

	Context context;
	public String[] fileIds;

	public PostFileTask(Context context) {
		this.context = context;
	}

	@Override
	protected Boolean doInBackground(final String... pathes) {
		fileIds = new String[pathes.length];

		for (int i = 0; i < pathes.length; ++i) {
			final int fileIdx = i;
			WowTalkWebServerIF.getInstance(context).fPostFileToServer(
					pathes[i],
					new NetworkIFDelegate() {

						@Override
						public void didFailNetworkIFCommunication(int arg0,
																  byte[] arg1) {
							Log.e("upload thumb file failure.");
						}

						@Override
						public void didFinishNetworkIFCommunication(int arg0,
																	byte[] arg1) {
							fileIds[fileIdx]= new String(arg1);
						}

						@Override
						public void setProgress(int tag, final int progress) {
							// assume all files are of the same size.
							publishProgress(Math.round((float)progress / pathes.length));
						}

					}, 0);

			if (fileIds[fileIdx] == null) { // failed
				return false;
			}
		}

		// all files posted
		return true;
	}
}
