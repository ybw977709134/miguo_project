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

import javax.net.SocketFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>Upload/download file to/from Aliyun OSS.</p>
 *
 * * 通过 RESTful 接口访问OSS服务器；
 * * 实现这个类时，阿里云OSS的官方SDK for Android尚未发布；
 * * 关于上传：
 * 		- 最初通过 {@link HttpURLConnection} 执行 HTTP 请求，但是在 Android 4.4 上
 * 		  上传大文件时会发生 OOM；
 * 		- 官方 SDK (oss-android-sdk-1.3.0.jar) 在 Android 4.4 上也存在类似的 OOM 现象；
 * 		- 所以现在没有使用任何 HTTP 库，而是直接用 Socket；
 *
 * Created by pzy on 11/10/14.
 */
public class OssClient {
	private static final String TAG = "OssClient";

	private static final String BOUNDARY = "9431149156168";
	private static final String OSS_HOST = "oss-cn-hangzhou.aliyuncs.com";

	private final String accessKeyId;
	private final String accessKeySerect;
	private final String bucketName;
	private String host;
	/** guaranty to be empty or ends with '/', and not starts with '/' */
	private String remoteDir = "";
	private NetworkIFDelegate callback;
	private int callbackTag;

	public OssClient(String accessKeyId, String accessKeySerect, String bucketName) {
		this.accessKeyId = accessKeyId;
		this.accessKeySerect = accessKeySerect;
		this.bucketName = bucketName;
		host = bucketName + "." + OSS_HOST;
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

		List<NameValuePair> postData = new ArrayList<>(10);
		postData.add(new BasicNameValuePair("OSSAccessKeyId", accessKeyId));
		String policy = getPolicy();
		postData.add(new BasicNameValuePair("policy", policy));
		postData.add(new BasicNameValuePair("Signature", getSignature(policy)));
		postData.add(new BasicNameValuePair("key", remoteDir + fileId));

		Log.i(TAG, " start upload ", inFilename, " => http://", host, "/", remoteDir, fileId);

		try {

			// post data 有三部分：
			// part 1: createBoundaryMessage()
			// part 2: file
			// part 3: end boundary

			File inputFile = new File(inFilename);
			long fileLength = inputFile.length();
			Log.i(TAG, " upload file length:", fileLength);

			String postDataPart1 = createBoundaryMessage(postData, inputFile.getName());
			String endBoundary = "\r\n--" + BOUNDARY + "--\r\n";

			// init http client

			Socket socket = SocketFactory.getDefault().createSocket(host, 80);
			OutputStream os = socket.getOutputStream();

			// send HTTP headers

			write(os, "POST / HTTP/1.1\r\n");
			write(os, "Host: " + host + "\r\n");
			write(os, "Content-Length: " +
					(postDataPart1.getBytes().length +
							fileLength +
							endBoundary.getBytes().length) + "\r\n");
			write(os, "Content-Type: multipart/form-data; boundary=" + BOUNDARY + "\r\n");
			write(os, "Accept:application/xml\r\n");
			write(os, "User-Agent: Android\r\n");
			write(os, "\r\n");

			// send HTTP body - part1, some post fields

			write(os, postDataPart1);
			Log.i(TAG, " >>> [binary]");

			// send HTTP body - part2, the file

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

				// try to solve OOM on Android 4.4.4
				os.flush();

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

			// send HTTP body - part3

			write(os, endBoundary);
			os.flush();
			socket.shutdownOutput();

			// read response from server
			// response code may be 204
			//
			// NOTE: getResponseCode() 会阻塞，所以分配一定的进度给它
			boolean requestOk = false;

			Log.i(TAG, " progress 100%");
			if (callback != null)
				callback.setProgress(callbackTag, 100);

			BufferedReader r = new BufferedReader(new InputStreamReader(
					socket.getInputStream(), "UTF-8"));
			StringBuilder resStr = new StringBuilder();
			String line;
			int lineIdx = 0;
			boolean isReadingBody = false;
			while ((line = r.readLine()) != null) {
				Log.i(TAG, " <<<: ", line);

				if (lineIdx == 0) {
					// first line, "HTTP/1.1 204 No Content"
					String[] a = line.split(" ");
					requestOk = a[1].startsWith("2");
				}

				++lineIdx;

				if (!isReadingBody) {
					continue;
				}

				if (line.equals("\r\n")) {
					isReadingBody = true;
					continue;
				}

				resStr.append(line);
			}

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

			socket.close();
		} catch (Exception e) {
			e.printStackTrace();
			if(callback !=null)
				callback.didFailNetworkIFCommunication(callbackTag, e.toString().getBytes());
		} finally {
		}
	}

	private void write(OutputStream os, String line) throws IOException {
		os.write(line.getBytes());
		Log.i(TAG, " >>> ", line);
	}

	public void download(String fileId, String outFilename) {

		String url = "http://" + host + "/" + remoteDir + fileId;
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

