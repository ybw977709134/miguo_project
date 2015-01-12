package org.wowtalk.api;

import android.text.TextUtils;
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
 * <p>Upload/download file to/from Aliyun OSS.</p>
 *
 * Created by pzy on 11/10/14.
 */
public class OssClient {
	private static final String TAG = "OssClient";

	private static final String BOUNDARY = "9431149156168";

	private final String accessKeyId;
	private final String accessKeySerect;
	private final String bucketName;
	private String rootUrl;
	/** guaranty to be empty or ends with '/', and not starts with '/' */
	private String remoteDir = "";
	private NetworkIFDelegate callback;
	private int callbackTag;

	public OssClient(String accessKeyId, String accessKeySerect, String bucketName) {
		this.accessKeyId = accessKeyId;
		this.accessKeySerect = accessKeySerect;
		this.bucketName = bucketName;
		rootUrl = String.format("http://%s.oss-cn-hangzhou.aliyuncs.com", bucketName);
	}

	public OssClient setRemoteDir(String dir) {
		if (TextUtils.isEmpty(dir)) {
			remoteDir = "";
		} else {
			remoteDir = dir.endsWith("/") ? dir : dir + "/";
			if (remoteDir.charAt(0) == '/')
				remoteDir = remoteDir.substring(1);
		}
		return this;
	}

	public OssClient setCallback(NetworkIFDelegate aDelegate, int aTag) {
		callback = aDelegate;
		callbackTag = aTag;
		return this;
	}

	public void upload(String fileId, String inFilename) {

		List<NameValuePair> postData = new ArrayList<NameValuePair>(10);
		postData.add(new BasicNameValuePair("OSSAccessKeyId", accessKeyId));
		String policy = getPolicy();
		postData.add(new BasicNameValuePair("policy", policy));
		postData.add(new BasicNameValuePair("Signature", getSignature(policy)));
		postData.add(new BasicNameValuePair("key", remoteDir + fileId));

		Log.i(TAG, " start upload ", inFilename, " => ", remoteDir, fileId);

		HttpURLConnection conn = null;

		try {

			// post data 有三部分：
			// part 1: createBoundaryMessage()
			// part 2: file
			// part 3: end boundary

			File inputFile = new File(inFilename);
			long fileLength = inputFile.length();
			Log.i(TAG, " upload file length:", fileLength);

			byte[] postDataPart1 = createBoundaryMessage(postData, inputFile.getName()).getBytes();
			String endBoundary = "\r\n--" + BOUNDARY + "--\r\n";
			byte[] postDataPart3 = endBoundary.getBytes();

			// init http client

			conn = (HttpURLConnection) new URL(rootUrl).openConnection();
			conn.setRequestProperty("Content-Type",
					"multipart/form-data; boundary=" + BOUNDARY);
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", "Android");
			conn.setDoOutput(true);
			conn.setFixedLengthStreamingMode((int)(
					postDataPart1.length  + fileLength + postDataPart3.length));
			conn.connect();

			// send post data

			OutputStream os = conn.getOutputStream();
			os.write(postDataPart1);

			//

			int bytesToRead = (int) (fileLength / 100);
			if (bytesToRead < 1024)
				bytesToRead = 1024;
			if (bytesToRead > 1024 * 256)
				bytesToRead = 1024 * 256;

			FileInputStream fileInputStream = new FileInputStream(inputFile);
			// create a buffer of maximum size
			int bytesAvailable = fileInputStream.available();
			int bufferSize = Math.min(bytesAvailable, bytesToRead);
			byte[] buffer = new byte[bufferSize];

			// 写文件占整个上传任务的进度的百分比，
			// 剩余进度分配给 getResponseCode()
			final int PROGRESS_WEIGHT_WRITE_FILE = 90;

			// read file and write it into form...
			long total = 0;
			int bytesRead = fileInputStream.read(buffer, 0, bufferSize);
			total += bytesRead;
			int oldProgress = (int) (total * PROGRESS_WEIGHT_WRITE_FILE / fileLength);
			while (bytesRead > 0) {
				os.write(buffer, 0, bufferSize);
				bytesAvailable = fileInputStream.available();
				bufferSize = Math.min(bytesAvailable, bytesToRead);
				bytesRead = fileInputStream.read(buffer, 0, bufferSize);
				total += bytesRead;
				int newProgress = (int) (total * PROGRESS_WEIGHT_WRITE_FILE / fileLength);
				if (newProgress != oldProgress) {
					Log.i(TAG, " progress ", newProgress, "%");
					if (callback != null)
						callback.setProgress(callbackTag, newProgress);
					oldProgress = newProgress;
				}
			}
			//
			fileInputStream.close();

			os.write(postDataPart3);
			os.flush();
			os.close();

			// read response from server
			// response code may be 204
			//
			// NOTE: getResponseCode() 会阻塞，所以分配一定的进度给它
			boolean requestOk = conn.getResponseCode() / 200 == 1;

			Log.i(TAG, " progress 100%");
			if (callback != null)
				callback.setProgress(callbackTag, 100);

			BufferedReader r = new BufferedReader(new InputStreamReader(
					requestOk ? conn.getInputStream() : conn.getErrorStream(),
					"UTF-8"));
			StringBuilder resStr = new StringBuilder();
			String line;
			while ((line = r.readLine()) != null) {
				resStr.append(line);
			}
			Log.i(TAG, " response: ", resStr.toString());

			if (requestOk) {
				Log.i(TAG, " upload succeed. ");
				if (callback != null)
					callback.didFinishNetworkIFCommunication(callbackTag, fileId.getBytes());
			} else {
				String xmlStr = resStr.toString();
				Element root = parseXml(xmlStr);
				String err = root.getElementsByTagName("Code").item(0).getTextContent()
						+ ": " + root.getElementsByTagName("Message").item(0).getTextContent();
				Log.e(TAG, " upload failed: ", err);
				if (callback != null)
					callback.didFailNetworkIFCommunication(callbackTag, err.getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(callback !=null)
				callback.didFailNetworkIFCommunication(callbackTag, e.toString().getBytes());
		} finally {
			if (conn != null)
				conn.disconnect();
		}
	}

	public void download(String fileId, String outFilename) {

		String url = rootUrl + "/" + remoteDir + fileId;
		Log.i(TAG, " start download ", outFilename, " <= ", url);

		HttpURLConnection conn = null;

		try {
			String date = getDateForHttp();
			String verb = "GET";

			conn = (HttpURLConnection) new URL(url).openConnection();
			conn.setRequestMethod(verb);
			conn.setRequestProperty("Date", date);
			conn.setRequestProperty("Authorization", getHeaderAuth(verb, date, fileId));
			conn.setRequestProperty("User-Agent", "Android");
			conn.setDoOutput(false);
			conn.connect();

			// response code may be 204
			boolean requestOk = conn.getResponseCode() / 200 == 1;

			if (requestOk) {
				// download the file
				OutputStream dest = new FileOutputStream(outFilename);
				InputStream input = new BufferedInputStream(
						conn.getInputStream());

				int contentLength = conn.getContentLength();
				Log.i(TAG, " download content-length: ", contentLength);

				byte data[] = new byte[1024];
				long total = 0;
				int count;
				while ((count = input.read(data)) != -1) {
					total += count;
					dest.write(data, 0, count);
					if (callback != null)
						callback.setProgress(callbackTag, (int) (total * 100 / contentLength));
				}

				input.close();
				Log.i(TAG, " download succeed. ");
				if (callback != null)
					callback.didFinishNetworkIFCommunication(callbackTag, fileId.getBytes());
			} else {
				BufferedReader r = new BufferedReader(new InputStreamReader(
						conn.getErrorStream(), "UTF-8"));

				StringBuilder resStr = new StringBuilder();
				String line;
				while ((line = r.readLine()) != null) {
					resStr.append(line);
				}
				Log.i(TAG, " response: ", resStr.toString());
				String xmlStr = resStr.toString();
				Element root = parseXml(xmlStr);
				String err = root.getElementsByTagName("Code").item(0).getTextContent()
						+ ": " + root.getElementsByTagName("Message").item(0).getTextContent();
				Log.e(TAG, " download failed: ", err);
				if (callback != null)
					callback.didFailNetworkIFCommunication(callbackTag, err.getBytes());
			}
		} catch (Exception e) {
			e.printStackTrace();
			if(callback !=null)
				callback.didFailNetworkIFCommunication(callbackTag, e.toString().getBytes());
		} finally {
			if (conn != null)
				conn.disconnect();
		}
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
		return res.toString();
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

	/**
	 * <p>Get Internet standard date string</p>
	 *
	 * <p>
	 * Sun, 06 Nov 1994 08:49:37 GMT  ; RFC 822, updated by RFC 1123
	 * </p>
	 *
	 * http://www.w3.org/Protocols/rfc2616/rfc2616-sec3.html
	 * @return
	 */
	private String getDateForHttp() {
		SimpleDateFormat sdf = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss", Locale.ENGLISH);
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
		return sdf.format(Calendar.getInstance().getTime()) + " GMT";
	}

	private String getDateForPolicy() {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.000Z'");
		sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
		return sdf.format(
				new Date(Calendar.getInstance(Locale.getDefault()).getTimeInMillis() + 3600 * 1000 * 24));
	}

	private String getHeaderAuth(String verb, String date, String objName) {
		String canonicalizedResource = String.format("/%s/%s%s", bucketName, remoteDir, objName);

		String result = "OSS " + accessKeyId + ":" + HmacSHA1Signature.create().computeSignature(
				accessKeySerect,
				verb + "\n"
						+ "\n" // content-MD5
						+ "\n" // content-type
						+ date + "\n"
						+ canonicalizedResource);

		Log.i(TAG, " get header auth: ", result);
		return result;
	}

	private String getSignature(String policy) {
		String result = HmacSHA1Signature.create().computeSignature(accessKeySerect, policy);
		Log.i(TAG, " get signature: ", result);
		return result;
	}

	private String getPolicy() {
		JSONObject root = new JSONObject();
		try {
			root.put("expiration", getDateForPolicy());

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
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return null;
	}
}

