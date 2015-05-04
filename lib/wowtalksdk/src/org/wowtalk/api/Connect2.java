package org.wowtalk.api;


import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.ParseException;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.apache.http.protocol.HTTP;
import org.apache.http.util.EntityUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.wowtalk.Log;
import org.xml.sax.InputSource;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.*;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

public class Connect2 {

    public static final String NETWORK_EXCEPTION_BROADCAST_ACTION = "org.wowtalk.api.network_exception";
    public static final String AUTH_FAILURE_BROADCAST_ACTION = "org.wowtalk.api.auth_failure";

    /**
     * 中央服务器地址
     */
    public static final String HOST_URL_CENTER_SERVER = GlobalSetting.WEB_HOST_HTTP;
    /**
     * web服务器domain，根据用户的公司填写的服务器地址，第一次登录时从中央服务器获取
     */
    private String mDomain;

    private static int timeoutConnection = GlobalSetting.TIMEOUT_CONNECTION;
    private static int timeoutSocket = GlobalSetting.TIMEOUT_SOCKET;

    /**
     * Used to send broadcast, get the SharedPreferences.
     */
    private static Context sContext = null;

    public static int sFlagIndex;
    private int mFlagIndex;

    /**
     * 连接中央服务器
     */
    public Connect2() {
        this(true);
    }

    /**
     * Construct，连接地方服务器时，使用无参的构造函数
     * @param isCenterServer true，连接中央服务器；false，地方服务器
     */
    public Connect2(boolean isCenterServer) {
        mFlagIndex = sFlagIndex;
        if (isCenterServer) {
            mDomain = HOST_URL_CENTER_SERVER;
        } else {
            // SharedPreference中获取
            mDomain = PrefUtil.getInstance(sContext).getWebDomain();
        }
    }

    public static void setContext(Context context) {
        if (null == sContext) {
            sContext = context;
        }
    }

    public static void SetTimeout(int connectionTimeout,int SocketTimeout){
        timeoutConnection = connectionTimeout;
        timeoutSocket = SocketTimeout;
    }

    /**
     * 切换用户时，取消原先的网络请求
     * @return
     */
    public void shouldCancelConn() {
        if (mFlagIndex != sFlagIndex) {
            throw new ShouldCancelConnException();
        }
    }

    /**
     * HTTP Post.
     * <p>
     * 自动检测以下错误并发出相应的通知(Broadcast)：
     * <ul>
     *     <li>网络连接故障 - {@link #NETWORK_EXCEPTION_BROADCAST_ACTION}；</li>
     *     <li>账号验证失败 - {@link #AUTH_FAILURE_BROADCAST_ACTION}；</li>
     * </ul>
     * </p>
     *
     * @param params
     * @return
     */
    public Element Post(String params){
        // 一般不会为空，如果为空了(被用户手动清除数据了)，再次进入此应用时，会要求用户重新登录
        if (TextUtils.isEmpty(mDomain)) {
            return null;
        }

        return Post(mDomain,params,true);
    } 
    
    /**
     * 输入访问远程服务器的url
     * @param params
     * @return 服务器返回对应的XML字符串
     * @author hutianfeng
     * @date 2015/3/3
     */
    public String getXmlString(String params){
    	// 一般不会为空，如果为空了(被用户手动清除数据了)，再次进入此应用时，会要求用户重新登录
        if (TextUtils.isEmpty(mDomain)) {
            return null;
        }
        
        if(!confirmSend2server()) {
            return null;
        }
        
	   
	        shouldCancelConn();
	        HttpPost httpPost = new HttpPost( mDomain );
	        
	        DefaultHttpClient client = getNewHttpClient();
	        
	        Log.i("@", Thread.currentThread().getId()," postStr= ",params);
	        StringEntity paramEntity = null;
			try {
				paramEntity = new StringEntity(params,HTTP.UTF_8);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
			}

	        paramEntity.setChunked( false );
	        paramEntity.setContentType( "application/x-www-form-urlencoded" );
	        httpPost.setEntity( paramEntity );

            shouldCancelConn();
            HttpResponse response = null;
			try {
				response = client.execute( httpPost );
			} catch (ClientProtocolException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
            shouldCancelConn();
            int status = response.getStatusLine().getStatusCode();
            if ( status != HttpStatus.SC_OK ) {
                Log.e("post http response code "+status);
                try {
					throw new Exception( "" );
				} catch (Exception e) {
					e.printStackTrace();
				}
            }

            try {
				String xmlStr = EntityUtils.toString( response.getEntity(), "UTF-8" );
				return xmlStr;
			} catch (ParseException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}

    	return null;
    }

    private static NetworkStateIndListener networkStateIndListener;
    public static interface NetworkStateIndListener {
        void onNetworkStateInd(boolean connected);
    }

    public static void setOnNetworkStateChangeListener(NetworkStateIndListener listener) {
        networkStateIndListener=listener;
    }

    public static boolean confirmSend2server() {
        if(null != networkStateIndListener && !Utils.isNetworkConnected(sContext)) {
            networkStateIndListener.onNetworkStateInd(false);
            return false;
        }
        return true;
    }
    /**
     * HTTP Post.
     * <p>
     * 自动检测以下错误并发出相应的通知(Broadcast)：
     * <ul>
     *     <li>网络连接故障 - {@link #NETWORK_EXCEPTION_BROADCAST_ACTION}；</li>
     *     <li>账号验证失败 - {@link #AUTH_FAILURE_BROADCAST_ACTION}；</li>
     * </ul>
     * </p>
     *
     * @param url
     * @param params
     * @param parseXML
     * @return
     */
    public Element Post(String url, String params,boolean parseXML )
    {
        Element root = null;

        if(!confirmSend2server()) {
            return null;
        }
	    try
	    {
	        shouldCancelConn();
	        HttpPost httpPost = new HttpPost( url );
	        
	        DefaultHttpClient client = getNewHttpClient();
	        
	        // POST データの設定
            Log.i("@", Thread.currentThread().getId()," postStr= ",params);
	        //StringEntity paramEntity = new StringEntity( params );
	        StringEntity paramEntity = new StringEntity(params,HTTP.UTF_8);

	        paramEntity.setChunked( false );
	        paramEntity.setContentType( "application/x-www-form-urlencoded" );
	        httpPost.setEntity( paramEntity );

            shouldCancelConn();
            HttpResponse response = client.execute( httpPost );
            shouldCancelConn();
            int status = response.getStatusLine().getStatusCode();
            if ( status != HttpStatus.SC_OK ) {
                Log.e("post http response code "+status);
                throw new Exception( "" );
            }

            String xmlStr = EntityUtils.toString( response.getEntity(), "UTF-8" );

            if (null == xmlStr || "".equals(xmlStr.trim())) {
                return null;
            }
            // 服务器返回长串的xml数据
            int startPoint = xmlStr.indexOf("<");
            if (startPoint == -1) {
                return null;
            } else {
                xmlStr = xmlStr.substring(startPoint);
            }

            // 如果错误码为账号验证失败，则广播。
            // 尝试匹配： <err_no>2</err_no>
            int p = xmlStr.indexOf("<err_no>");
            if (p != -1) {
                if (xmlStr.substring(p + 8, p + 10).equals("2<")) {
                    // 切换帐号/登录时，失败，不发送广播
                    // TODO 多帐号之后，其他网络请求认证失败是否需要剔除到登录界面
                    if (!params.startsWith("action=biz_login_by_hashpassword")
                            && !params.startsWith("action=biz_login")) {
                        Intent intent = new Intent(AUTH_FAILURE_BROADCAST_ACTION);
                        sContext.sendBroadcast(intent);
                    }
                }
            }

	        Log.i("xml result returned from server:\n");
            try {
                int stepSize=512;
                for(int start=0; start<xmlStr.length(); start+=stepSize) {
                    int end=(start+stepSize)>xmlStr.length()?xmlStr.length():(start+stepSize);
                    Log.i(xmlStr.substring(start,end));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
	        
	        if(parseXML){
		        
	            // ドキュメントビルダーファクトリを生成
	            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();

	            // ドキュメントビルダーを生成
	            DocumentBuilder builder = dbfactory.newDocumentBuilder();
	            
	            //System.out.println(xmlStr);
	            xmlStr=xmlStr.replaceAll("&amp;", "＆");
	            xmlStr=xmlStr.replaceAll("&quot;", "\"");
	            xmlStr=xmlStr.replaceAll("&nbsp;", " ");

	            
	            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( xmlStr.getBytes());
	            Document doc = builder.parse(new InputSource(new InputStreamReader(byteArrayInputStream, "UTF-8")));
	            if(byteArrayInputStream!=null){
	            	byteArrayInputStream.close();
	            }
	            root = doc.getDocumentElement();
		    }
	        shouldCancelConn();
	        return root;
	    } catch (ShouldCancelConnException e) {
	        // 切换用户或用户logout，此时因取消网络请求
	        Log.w("Exception ", e.toString());
	        return null;
	    } catch ( IOException e ) {
	    	Log.e("Exception ",e.toString());
            if (null != sContext) {
                /**
                 * Intent.putExtra(Exception) 可能遇到以下运行时错误：
                 *
                        java.lang.RuntimeException: An error occured while executing doInBackground()
        at android.os.AsyncTask$3.done(AsyncTask.java:278)
        at java.util.concurrent.FutureTask$Sync.innerSetException(FutureTask.java:273)
        at java.util.concurrent.FutureTask.setException(FutureTask.java:124)
        at java.util.concurrent.FutureTask$Sync.innerRun(FutureTask.java:307)
        at java.util.concurrent.FutureTask.run(FutureTask.java:137)
        at java.util.concurrent.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1076)
        at java.util.concurrent.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:569)
        at java.lang.Thread.run(Thread.java:856)
        Caused by: java.lang.RuntimeException: Parcelable encountered IOException writing serializable object (name = org.apache.http.conn.HttpHostConnectException)
        at android.os.Parcel.writeSerializable(Parcel.java:1181)
        at android.os.Parcel.writeValue(Parcel.java:1135)
        at android.os.Parcel.writeMapInternal(Parcel.java:493)
        at android.os.Bundle.writeToParcel(Bundle.java:1612)
        at android.os.Parcel.writeBundle(Parcel.java:507)
        at android.content.Intent.writeToParcel(Intent.java:6129)
        at android.app.ActivityManagerProxy.broadcastIntent(ActivityManagerNative.java:1838)
        at android.app.ContextImpl.sendBroadcast(ContextImpl.java:909)
        at android.content.ContextWrapper.sendBroadcast(ContextWrapper.java:294)
                 */
                Intent intent = new Intent(NETWORK_EXCEPTION_BROADCAST_ACTION);
                //intent.putExtra("exception", e);
                intent.putExtra("exception-str", e.getClass().getName() + e.getMessage());
                sContext.sendBroadcast(intent);
            }
	    	return null;
	    } catch (Exception e) {
            Log.e("Exception ",e.toString());
            return null;
        }
	}
	
	public class MySSLSocketFactory extends SSLSocketFactory {
	    SSLContext sslContext = SSLContext.getInstance("TLS");

	    public MySSLSocketFactory(KeyStore truststore) throws NoSuchAlgorithmException, KeyManagementException, KeyStoreException, UnrecoverableKeyException {
	        super(truststore);

	        TrustManager tm = new X509TrustManager() {
	            public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	            }

	            public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException {
	            }

	            public X509Certificate[] getAcceptedIssuers() {
	                return null;
	            }
	        };

	        sslContext.init(null, new TrustManager[] { tm }, null);
	    }

	    @Override
	    public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException, UnknownHostException {
	        return sslContext.getSocketFactory().createSocket(socket, host, port, autoClose);
	    }

	    @Override
	    public Socket createSocket() throws IOException {
	        return sslContext.getSocketFactory().createSocket();
	    }
	}

	public DefaultHttpClient getNewHttpClient() {
	    try {
	        KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
	        trustStore.load(null, null);

	        SSLSocketFactory sf = new MySSLSocketFactory(trustStore);
	        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);

	        HttpParams params = new BasicHttpParams();
	        HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
	        HttpProtocolParams.setContentCharset(params, HTTP.UTF_8);

	        // Set the timeout in milliseconds until a connection is established.
	     // The default value is zero, that means the timeout is not used. 


	     HttpConnectionParams.setConnectionTimeout(params, timeoutConnection);
	     // Set the default socket timeout (SO_TIMEOUT) 
	     // in milliseconds which is the timeout for waiting for data.
	     HttpConnectionParams.setSoTimeout(params, timeoutSocket);        
	        
	        
	        SchemeRegistry registry = new SchemeRegistry();
	        registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
	        registry.register(new Scheme("https", sf, 443));

	        ClientConnectionManager ccm = new ThreadSafeClientConnManager(params, registry);

	        return new DefaultHttpClient(ccm, params);
	    } catch (Exception e) {
	        return new DefaultHttpClient();
	    }
	}

    private class ShouldCancelConnException extends RuntimeException {
        public ShouldCancelConnException() {
            super("the account has logout, cancel the connection.");
        }
    }
}
