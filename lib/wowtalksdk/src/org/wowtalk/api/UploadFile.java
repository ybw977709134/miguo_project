package org.wowtalk.api;

import android.os.AsyncTask;
import org.apache.http.NameValuePair;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wowtalk.Log;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

class UploadFile extends
AsyncTask<List<NameValuePair>, Integer, String> {
	private NetworkIFDelegate networkIFDelegate;
	private int tag;
	private String inputFilepath;
	private static final String BOUNDARY = "----------V2ymHFg03ehbqgZCaKO6jy";
	private List<NameValuePair> postData;
	String urlServer = GlobalSetting.WEB_HOST_HTTP;

	public void setup(NetworkIFDelegate aDelegate, int aTag, String filePath) {
		networkIFDelegate = aDelegate;
		tag = aTag;
		inputFilepath = filePath;
	}

	private String createBoundaryMessage(String fileName) {
		StringBuffer res = new StringBuffer("--").append(BOUNDARY).append(
				"\r\n");
		for (NameValuePair nv : postData) {
			res.append("Content-Disposition: form-data; name=\"")
			.append(nv.getName()).append("\"\r\n").append("\r\n")
			.append(nv.getValue()).append("\r\n").append("--")
			.append(BOUNDARY).append("\r\n");
		}
		String[] fileChunks = fileName.split("\\.");
		String fileType = "image/" + fileChunks[fileChunks.length - 1];
		res.append("Content-Disposition: form-data; name=\"")
		.append("file").append("\"; filename=\"").append(fileName)
		.append("\"\r\n").append("Content-Type: ").append(fileType)
		.append("\r\n\r\n");
		return res.toString();
	}

	@Override
	protected String doInBackground(List<NameValuePair>... params) {
		postData = params[0];

		Log.e("UploadFile", "start...............................");

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;
		URLConnection conn = null;

		try {
			conn = new URL(urlServer).openConnection();
			conn.setRequestProperty("Content-Type",
					"multipart/form-data; boundary=" + BOUNDARY);
			// User Agentの設定はAndroid1.6の場合のみ必要
			conn.setRequestProperty("User-Agent", "Android");
			// HTTP POSTのための設定
			((HttpURLConnection) conn).setRequestMethod("POST");
			conn.setDoOutput(true);
			// HTTP接続開始
			conn.connect();
			// send post data
			File inputFile = new File(inputFilepath);
			long fileLength = inputFile.length();
			Log.e("upload file length:", fileLength);
			OutputStream os = conn.getOutputStream();
			os.write(createBoundaryMessage(inputFile.getName()).getBytes());

			//

			FileInputStream fileInputStream = new FileInputStream(inputFile);
			// create a buffer of maximum size
			bytesAvailable = fileInputStream.available();
			bufferSize = Math.min(bytesAvailable, maxBufferSize);
			buffer = new byte[bufferSize];

			// read file and write it into form...
			long total = 0;
			bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			total += bytesRead;
			while (bytesRead > 0) {
				os.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, maxBufferSize);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				total += bytesRead;
				publishProgress((int) (total * 100 / fileLength));

			}
			//
			fileInputStream.close();

			String endBoundary = "\r\n--" + BOUNDARY + "--\r\n";
			os.write(endBoundary.getBytes());
			os.flush();
			os.close();

			// download the response from server
			BufferedReader r = new BufferedReader(new InputStreamReader(
					conn.getInputStream(), "UTF-8"));
			StringBuilder resStr = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				resStr.append(line);
			}

			if (networkIFDelegate != null) {
				String xmlStr = resStr.toString();
				Log.e("xmlStr=", xmlStr);

				// ドキュメントビルダーファクトリを生成
				DocumentBuilderFactory dbfactory = DocumentBuilderFactory
						.newInstance();

				// ドキュメントビルダーを生成
				DocumentBuilder builder = dbfactory.newDocumentBuilder();

				// System.out.println(xmlStr);
				xmlStr = xmlStr.replaceAll("&amp;", "＆");
				xmlStr = xmlStr.replaceAll("&quot;", "\"");
				xmlStr = xmlStr.replaceAll("&nbsp;", " ");

				ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(
						xmlStr.getBytes());
				Document doc = builder
						.parse(new InputSource(new InputStreamReader(
								byteArrayInputStream, "UTF-8")));
				if (byteArrayInputStream != null) {
					byteArrayInputStream.close();
				}
				Element root = null;
				root = doc.getDocumentElement();
				byte[] res = { 0 };
				if (root != null) {

					// err_no要素のリストを取得
					NodeList errorList = root
							.getElementsByTagName("err_no");
					// error要素を取得
					Element errorElement = (Element) errorList.item(0);
					// error要素の最初の子ノード（テキストノード）の値を取得
					String errorStr = errorElement.getFirstChild()
							.getNodeValue();

					if (errorStr.equals("0")) {
						try {
							NodeList nodeList = root
									.getElementsByTagName("file_id");
							Element element = (Element) nodeList.item(0);
							String fileid = element.getFirstChild()
									.getNodeValue();
							res = fileid.getBytes();
						} catch (Exception e) {
							res[0] = 0;
						}

					}

				} else {
					res[0] = -1;
				}
				networkIFDelegate.didFinishNetworkIFCommunication(tag, res);

			}

		} catch (Exception e) {
			e.printStackTrace();
			Log.e("HttpMultipartRequest:", e.toString());
			if(networkIFDelegate!=null) networkIFDelegate.didFailNetworkIFCommunication(tag, e.toString().getBytes());
		} finally {
			if (conn != null)
				((HttpURLConnection) conn).disconnect();
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

