package co.onemeter.oneapp.liveplayer;

/**
 * 获取rtmp直播地址事件
 *
 */
public class FetchRtmpEvent {
	String liveplayUrl;

	public FetchRtmpEvent() {
		super();
	}

	public FetchRtmpEvent(String liveplayUrl) {
		super();
		this.liveplayUrl = liveplayUrl;
	}

	public String getLiveplayUrl() {
		return liveplayUrl;
	}

}
