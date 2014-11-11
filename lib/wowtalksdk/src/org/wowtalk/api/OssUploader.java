package org.wowtalk.api;

import android.os.AsyncTask;
import android.util.Base64;
import com.aliyun.common.auth.HmacSHA1Signature;
import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wowtalk.Log;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 通过 HTTP API 访问 Aliyun OSS。
 *
 * Created by pzy on 11/10/14.
 */
public class OssUploader extends
		AsyncTask<String, Integer, String> {
	private static final String TAG = "OssUploader";

	private static final String BOUNDARY = "9431149156168";

	private final String accessKeyId;
	private final String accessKeySerect;
	private final String bucketName;
	private String urlServer;
	private NetworkIFDelegate networkIFDelegate;
	private int tag;

	public OssUploader(String accessKeyId, String accessKeySerect, String bucketName) {
		this.accessKeyId = accessKeyId;
		this.accessKeySerect = accessKeySerect;
		this.bucketName = bucketName;
		urlServer = String.format("http://%s.oss-cn-hangzhou.aliyuncs.com", bucketName);
	}

	public void setup(NetworkIFDelegate aDelegate, int aTag) {
		networkIFDelegate = aDelegate;
		tag = aTag;
	}

	private String createBoundaryMessage(List<NameValuePair> postData, String fileName) {
		StringBuffer res = new StringBuffer("--").append(BOUNDARY).append(
				"\r\n");
		for (NameValuePair nv : postData) {
			res.append(String.format(
					"Content-Disposition: form-data; name=\"%s\"\r\n\r\n%s\r\n--%s\r\n",
					nv.getName(), nv.getValue(), BOUNDARY));
		}
		res.append(String.format(
				"Content-Disposition: form-data; name=\"file\"; filename=\"%s\"\r\n\r\n",
				fileName));
		Log.i(TAG, " post data: ", res.toString());
		return res.toString();
	}

	@Override
	protected String doInBackground(String... params) {
		String key = params[0];
		String filename = params[1];

		List<NameValuePair> postData = new ArrayList<NameValuePair>(10);
		postData.add(new BasicNameValuePair("OSSAccessKeyId", accessKeyId));
		String policy = getPolicy();
		postData.add(new BasicNameValuePair("policy", policy));
		postData.add(new BasicNameValuePair("Signature", getSignature(policy)));
		postData.add(new BasicNameValuePair("key", key));

		// TODO file last

		Log.e(TAG, " start...............................");

		int bytesRead, bytesAvailable, bufferSize;
		byte[] buffer;
		int maxBufferSize = 1 * 1024 * 1024;
		HttpURLConnection conn = null;

		try {
			conn = (HttpURLConnection) new URL(urlServer).openConnection();
			conn.setRequestProperty("Content-Type",
					"multipart/form-data; boundary=" + BOUNDARY);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", "browser_data");
			conn.setDoOutput(true);
			// HTTP接続開始
			conn.connect();
			// send post data

			File inputFile = new File(filename);
			long fileLength = inputFile.length();
			Log.e(TAG, " upload file length:", fileLength);
			OutputStream os = conn.getOutputStream();
			os.write(createBoundaryMessage(postData, inputFile.getName()).getBytes());

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

			// read response from server
			// response code may be 204
			boolean requestOk = conn.getResponseCode() / 200 == 1;

			BufferedReader r = new BufferedReader(new InputStreamReader(
					requestOk ? conn.getInputStream() : conn.getErrorStream(),
					"UTF-8"));
			StringBuilder resStr = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				resStr.append(line);
			}
			Log.e(TAG, " response: ", resStr.toString());

			if (requestOk) {
				Log.e(TAG, " upload succeed. ");
				if (networkIFDelegate != null)
					networkIFDelegate.didFinishNetworkIFCommunication(tag, key.getBytes());
			} else {
				String xmlStr = resStr.toString();
				Element root = parseXml(xmlStr);
				String err = root.getElementsByTagName("Message").item(0).getTextContent();
				Log.e(TAG, " upload failed: ", err);
				if (networkIFDelegate != null)
					networkIFDelegate.didFailNetworkIFCommunication(tag, err.getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
			Log.e(TAG, " HttpMultipartRequest:", e.toString());
			if(networkIFDelegate!=null) networkIFDelegate.didFailNetworkIFCommunication(tag, e.toString().getBytes());
		} finally {
			if (conn != null)
				conn.disconnect();
		}

		return null;
	}

	private Element parseXml(String xmlStr) throws ParserConfigurationException, SAXException, IOException {
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
		return doc.getDocumentElement();
	}

	private String getSignature(String policy) {
		String result = HmacSHA1Signature.create().computeSignature(accessKeySerect, policy);
		Log.i(TAG, " get signature:", result);
		return result;
	}

	private String getPolicy() {
		JSONObject root = new JSONObject();
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
			sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
			root.put("expiration", sdf.format(
					new Date(Calendar.getInstance(Locale.getDefault()).getTimeInMillis() + 3600 * 1000 * 24)));

			JSONArray conditions = new JSONArray();
			JSONObject bucket = new JSONObject();
			bucket.put("bucket", bucketName);
			conditions.put(bucket);

			root.put("conditions", conditions);
			String json = root.toString();
			String result = Base64.encodeToString(json.getBytes("UTF-8"), Base64.URL_SAFE);
			result = result.replace("\n", "");
			Log.i(TAG, " get policy json:", json);
			Log.i(TAG, " get policy base64:", result);
			return result;
		} catch (JSONException e) {
			e.printStackTrace();
		}
		catch (UnsupportedEncodingException e)
		{
			e.printStackTrace();
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
		if (networkIFDelegate != null) {
			networkIFDelegate.setProgress(tag, progress[0]);
		}
	}

}

