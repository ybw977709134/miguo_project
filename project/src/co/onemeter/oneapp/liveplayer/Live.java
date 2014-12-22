package co.onemeter.oneapp.liveplayer;

import java.io.IOException;

import android.text.TextUtils;

import com.google.gson.Gson;

/**
 * 直播
 * @author liyou
 *
 */
public class Live {

	private String url;
	private String request_id;
	private String status;
	private String description;

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getRequest_id() {
		return request_id;
	}

	public void setRequest_id(String request_id) {
		this.request_id = request_id;
	}

	/**
	 * 获取rtmp直播地址
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 */
	public static Live getRtmp(String url) throws IOException {
		String liveplay = MyHttpUtil.run(url);
		if (!TextUtils.isEmpty(liveplay)) {
			Gson gson = new Gson();
			return gson.fromJson(liveplay, Live.class);
		}
		return null;
	}

	protected void finalize() throws Throwable {
		super.finalize();
		//System.out.println("JavaBean [Live: " + TimeUtil.convertTime(System.currentTimeMillis()) + "] is gc");
	}
}
