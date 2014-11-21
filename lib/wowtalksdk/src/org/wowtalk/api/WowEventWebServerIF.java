package org.wowtalk.api;

import android.content.Context;
import android.text.TextUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wowtalk.Log;
import org.xml.sax.InputSource;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Locale;

/**
 * 活动模块相关功能。
 */
public class WowEventWebServerIF {

	private Context mContext;

    private PrefUtil mPrefUtil;

	private static WowEventWebServerIF instance;

    private WowEventWebServerIF(Context context) {
        mContext = context.getApplicationContext();
        mPrefUtil = PrefUtil.getInstance(context);
    }

	public static final WowEventWebServerIF getInstance(Context context) {
		if (instance == null) {
			instance = new WowEventWebServerIF(context);
		}
		return instance;
	}
	
	/**
	 * do a http post request that do not require response data (except errno).
	 * @param postStr
	 * @return
	 */
	private int _doRequestWithoutResponse(String postStr) {
		int errno = ErrorCode.UNKNOWN; 
		
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
	}

    private String fixXmlResultForEventTest(String action) {
        StringBuilder sb=new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        sb.append("<Smartphone xmlns=\"https://wowtalk.org\">");
        sb.append("<header>");
        sb.append("<err_no>0</err_no>");
        sb.append("<s_version>");
        sb.append("<server_version>1000</server_version>");
        sb.append("</s_version>");
        sb.append("<user_id>-1</user_id>");
        sb.append("</header>");
        sb.append("<body>");

        if(action.equals("get_latest_events")) {
            sb.append("<get_latest_events>");

            sb.append("<event>");
                sb.append("<event_id>70abf1f63-8038-4fe5-b97a-3b1f27c0ab14</event_id>");
                sb.append("<event_creator>苏州园区</event_creator>");
                sb.append("<owner_id>xxfv</owner_id>");
                sb.append("<event_title>dancing ball</event_title>");
                sb.append("<event_desc>new_temp_group_6044</event_desc>");
                sb.append("<event_type>0</event_type>");
                sb.append("<joined_member>2</joined_member>");
                sb.append("<max_member>10</max_member>");
                sb.append("<possible_member>1</possible_member>");
                sb.append("<contact_email>xxfa@wowtech-inc.com</contact_email>");
                sb.append("<place>suzhou</place>");
                sb.append("<latitude>-1</latitude>");
                sb.append("<longitude>-1</longitude>");
                sb.append("<tag>tag b</tag>");
                sb.append("<category>stick</category>");
                sb.append("<start_date>2013.2.2</start_date>");
                sb.append("<deadline>2013.3.30</deadline>");
                sb.append("<membership>0</membership>");
                sb.append("<timestamp>0</timestamp>");
                sb.append("<thumbnail>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409028332_7387</thumbnail>");

                sb.append("<multimedia_set>");
                    sb.append("<multimedia>");
                        sb.append("<multimedia_content_id>118</multimedia_content_id>");
                        sb.append("<multimedia_content_type>jpg</multimedia_content_type>");
                        sb.append("<multimedia_content_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409029768_2854</multimedia_content_path>");
                        sb.append("<multimedia_thumbnail_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409028332_7387</multimedia_thumbnail_path>");
                        sb.append("<duration>0</duration>");
                    sb.append("</multimedia>");
                    sb.append("<multimedia>");
                        sb.append("<multimedia_content_id>119</multimedia_content_id>");
                        sb.append("<multimedia_content_type>jpg</multimedia_content_type>");
                        sb.append("<multimedia_content_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409029788_3024</multimedia_content_path>");
                        sb.append("<multimedia_thumbnail_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409028338_2868</multimedia_thumbnail_path>");
                        sb.append("<duration>0</duration>");
                    sb.append("</multimedia>");
                    sb.append("<multimedia>");
                        sb.append("<multimedia_content_id>120</multimedia_content_id>");
                        sb.append("<multimedia_content_type>jpg</multimedia_content_type>");
                        sb.append("<multimedia_content_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409029753_949</multimedia_content_path>");
                        sb.append("<multimedia_thumbnail_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409028332_5478</multimedia_thumbnail_path>");
                        sb.append("<duration>17</duration>");
                    sb.append("</multimedia>");
                sb.append("</multimedia_set>");
            sb.append("</event>");

            sb.append("<event>");
                sb.append("<event_id>70afdc1f63-8038-4fe5-b97a-3b1f27adc0ab14</event_id>");
                sb.append("<event_creator>园区</event_creator>");
                sb.append("<owner_id>xxfk</owner_id>");
                sb.append("<event_title>pipu ball</event_title>");
                sb.append("<event_desc>pipu ball play</event_desc>");
                sb.append("<event_type>1</event_type>");
                sb.append("<joined_member>2</joined_member>");
                sb.append("<max_member>20</max_member>");
                sb.append("<possible_member>1</possible_member>");
                sb.append("<contact_email>xxfb@wowtech-inc.com</contact_email>");
                sb.append("<place>suzhou</place>");
                sb.append("<latitude>-1</latitude>");
                sb.append("<longitude>-1</longitude>");
                sb.append("<tag>tag a</tag>");
                sb.append("<category>joy</category>");
                sb.append("<start_date>2013.4.6</start_date>");
                sb.append("<deadline>2013.5.7</deadline>");
                sb.append("<membership>2</membership>");
                sb.append("<timestamp>1</timestamp>");
                sb.append("<thumbnail></thumbnail>");
            sb.append("</event>");

            sb.append("<event>");
                sb.append("<event_id>70af1f63-8038-4fe5-b97a-3b1f27adc0aadb14</event_id>");
                sb.append("<event_creator>test_event_2</event_creator>");
                sb.append("<owner_id>xxf0</owner_id>");
                sb.append("<event_title>kk ball</event_title>");
                sb.append("<event_desc>kk ball play aaa</event_desc>");
                sb.append("<event_type>0</event_type>");
                sb.append("<joined_member>0</joined_member>");
                sb.append("<max_member>10</max_member>");
                sb.append("<possible_member>2</possible_member>");
                sb.append("<contact_email>xxf0@wowtech-inc.com</contact_email>");
                sb.append("<place>nan jing</place>");
                sb.append("<latitude>-1</latitude>");
                sb.append("<longitude>-1</longitude>");
                sb.append("<tag>love</tag>");
                sb.append("<category>job</category>");
                sb.append("<start_date>2013.6.7</start_date>");
                sb.append("<deadline>2013.6.9</deadline>");
                sb.append("<membership>0</membership>");
                sb.append("<timestamp>2</timestamp>");
                sb.append("<thumbnail>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409028332_4013</thumbnail>");
                sb.append("<multimedia_set>");
                    sb.append("<multimedia>");
                        sb.append("<multimedia_content_id>118</multimedia_content_id>");
                        sb.append("<multimedia_content_type>jpg</multimedia_content_type>");
                        sb.append("<multimedia_content_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409029796_4789</multimedia_content_path>");
                        sb.append("<multimedia_thumbnail_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409028332_4013</multimedia_thumbnail_path>");
                        sb.append("<duration>0</duration>");
                    sb.append("</multimedia>");
                    sb.append("<multimedia>");
                        sb.append("<multimedia_content_id>119</multimedia_content_id>");
                        sb.append("<multimedia_content_type>jpg</multimedia_content_type>");
                        sb.append("<multimedia_content_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409029727_2817</multimedia_content_path>");
                        sb.append("<multimedia_thumbnail_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409028334_3417</multimedia_thumbnail_path>");
                        sb.append("<duration>0</duration>");
                    sb.append("</multimedia>");
                    sb.append("<multimedia>");
                        sb.append("<multimedia_content_id>120</multimedia_content_id>");
                        sb.append("<multimedia_content_type>jpg</multimedia_content_type>");
                        sb.append("<multimedia_content_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409033420_1230</multimedia_content_path>");
                        sb.append("<multimedia_thumbnail_path>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409032000_1503</multimedia_thumbnail_path>");
                        sb.append("<duration>17</duration>");
                    sb.append("</multimedia>");
                sb.append("</multimedia_set>");
            sb.append("</event>");

            sb.append("<event>");
                sb.append("<event_id>70af1f63d-8038-4fe5-b97a-3b1f27adc0aadb14</event_id>");
                sb.append("<event_creator>test_event_3</event_creator>");
                sb.append("<owner_id>xxf1</owner_id>");
                sb.append("<event_title>kak ball</event_title>");
                sb.append("<event_desc>kak ball play aaa</event_desc>");
                sb.append("<event_type>1</event_type>");
                sb.append("<joined_member>3</joined_member>");
                sb.append("<max_member>11</max_member>");
                sb.append("<possible_member>4</possible_member>");
                sb.append("<contact_email>xxf1@wowtech-inc.com</contact_email>");
                sb.append("<place>bei jing</place>");
                sb.append("<latitude>-1</latitude>");
                sb.append("<longitude>-1</longitude>");
                sb.append("<tag>no tag</tag>");
                sb.append("<category>game</category>");
                sb.append("<start_date>2013.7.7</start_date>");
                sb.append("<deadline>2013.7.9</deadline>");
                sb.append("<membership>1</membership>");
                sb.append("<timestamp>3</timestamp>");
                sb.append("<thumbnail>8744f1e8-f88e-4cb5-ab82-6c67bc1a8ef9_1379409032000_1503</thumbnail>");
            sb.append("</event>");

            sb.append("</get_latest_events>");
        } else if (action.equals("get_previous_events")) {
            sb.append("<get_previous_events>");

            sb.append("<event>");
                sb.append("<event_id>70af1f63-8038-4fe5-b97a-3b1f27c0ab16</event_id>");
                sb.append("<event_creator>苏州</event_creator>");
                sb.append("<owner_id>xxfb</owner_id>");
                sb.append("<event_title>ka ball</event_title>");
                sb.append("<event_desc>ka ball play</event_desc>");
                sb.append("<event_type>0</event_type>");
                sb.append("<joined_member>3</joined_member>");
                sb.append("<max_member>15</max_member>");
                sb.append("<possible_member>2</possible_member>");
                sb.append("<contact_email>aaa@wowtech-inc.com</contact_email>");
                sb.append("<place>nan jing</place>");
                sb.append("<latitude>-1</latitude>");
                sb.append("<longitude>-1</longitude>");
                sb.append("<start_date>2013.2.2</start_date>");
                sb.append("<deadline>2013.1.30</deadline>");
                sb.append("<membership>0</membership>");
                sb.append("<timestamp></timestamp>");
                sb.append("<thumbnail>3d6f0b48-6c85-4753-82f1-1235465_-sdfswqrfv_234</thumbnail>");
            sb.append("</event>");

            sb.append("</get_previous_events>");
        }
        sb.append("</body>");
        sb.append("</Smartphone>");


        return sb.toString();
    }

    private Element fixEventForTest(String action) {
        Element root = null;

        try {
            DocumentBuilderFactory dbfactory = DocumentBuilderFactory.newInstance();

            // ドキュメントビルダーを生成
            DocumentBuilder builder = dbfactory.newDocumentBuilder();

            String xmlStr=fixXmlResultForEventTest(action);
            xmlStr=xmlStr.replaceAll("&amp;", "＆");
            xmlStr=xmlStr.replaceAll("&quot;", "\"");
            xmlStr=xmlStr.replaceAll("&nbsp;", " ");


            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream( xmlStr.getBytes());
            Document doc = builder.parse(new InputSource(new InputStreamReader(byteArrayInputStream, "UTF-8")));
            if(byteArrayInputStream!=null){
                byteArrayInputStream.close();
            }
            root = doc.getDocumentElement();
        } catch (Exception e) {
            e.printStackTrace();
        }


        return root;
    }

	/**
	 * 获取活动列表。
	 * @return errno
	 */
	public int fGetLatestEvents() {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null)
			return ErrorCode.INVALID_ARGUMENT;
		
		int errno = ErrorCode.BAD_RESPONSE;
		String[] actions = new String[] {"get_all_events"};

		for(String action :  actions) {
			String postStr = "action=" + action
                    + "&uid=" + Utils.urlencodeUtf8(uid)
                    + "&password=" + Utils.urlencodeUtf8(password)
                    + "&get_finished_event_only=0";
			Connect2 connect2 = new Connect2();
			Element root = connect2.Post(postStr);
//            root=fixEventForTest(action);

			if (root != null) {
				NodeList errorList = root.getElementsByTagName("err_no");
				Element errorElement = (Element) errorList.item(0);
				String errorStr = errorElement.getFirstChild().getNodeValue();

				if (errorStr.equals("0")) {
					errno = ErrorCode.OK;

                    Database dbHelper = new Database(mContext);

                    ArrayList<WEvent> eventsFromServer=new ArrayList<WEvent>();
					Element resultElement = Utils.getFirstElementByTagName(root, action); 
					if(resultElement != null) {
						NodeList eventNodes = resultElement.getElementsByTagName("event");
						if(eventNodes != null && eventNodes.getLength() > 0) {

							for(int i = 0, n = eventNodes.getLength(); i < n; ++i) {
								Element eventNode = (Element)eventNodes.item(i);
                                WEvent a = XmlHelper.parseWEvent(eventNode);
								if(a != null && !Utils.isNullOrEmpty(a.id)) {
									dbHelper.storeEvent(a);
                                    eventsFromServer.add(a);
                                }
							}
						}

                        clearLocalEvents(eventsFromServer,"");
					}
				} else {
					errno = Integer.parseInt(errorStr);
				}
			}
		}
		return errno;
	}

	public int fGetEventInfo(String event_id) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if(uid == null || password == null)
            return ErrorCode.INVALID_ARGUMENT;

        int errno = ErrorCode.BAD_RESPONSE;

        String action = "get_event_info";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&event_id=" + Utils.urlencodeUtf8(event_id);
        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = ErrorCode.OK;

                Database dbHelper = new Database(mContext);

                ArrayList<WEvent> eventsFromServer=new ArrayList<WEvent>();
                Element resultElement = Utils.getFirstElementByTagName(root, action);
                if(resultElement != null) {
                    NodeList eventNodes = resultElement.getElementsByTagName("event");
                    if(eventNodes != null && eventNodes.getLength() > 0) {

                        for(int i = 0, n = eventNodes.getLength(); i < n; ++i) {
                            Element eventNode = (Element)eventNodes.item(i);
                            WEvent a = XmlHelper.parseWEvent(eventNode);
                            if(a != null && !Utils.isNullOrEmpty(a.id)) {
                                dbHelper.storeEvent(a);
                                eventsFromServer.add(a);
                            }
                        }
                    }

                    clearLocalEvents(eventsFromServer,"");
                }
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }
        return errno;
    }

    private final static int DEF_EVENTS_FROM_SERVER_COUNT=20;
    private void clearLocalEvents(ArrayList<WEvent> eventFromServer,String timestamp) {
        long timestampMax;
        long timestampMin;

        if(TextUtils.isEmpty(timestamp)) {
            timestampMax=Long.MAX_VALUE;
            if(eventFromServer.size() < DEF_EVENTS_FROM_SERVER_COUNT) {
                timestampMin=Long.MIN_VALUE;
            } else {
                timestampMin=getMinTimestampFromEventList(eventFromServer);
            }
        } else {
            timestampMax=getMaxTimestampFromEventList(eventFromServer);
            if(eventFromServer.size() < DEF_EVENTS_FROM_SERVER_COUNT) {
                timestampMin=Long.MIN_VALUE;
            } else {
                timestampMin=getMinTimestampFromEventList(eventFromServer);
            }
        }

        Database dbHelper = new Database(mContext);
        ArrayList<WEvent> allLocalEvents=dbHelper.fetchAllEvents();

        long curEventTimeStamp;
        for(WEvent aEvent : allLocalEvents) {
            try {
                curEventTimeStamp= TextUtils.isEmpty(aEvent.timeStamp) ? 0 : Long.valueOf(aEvent.timeStamp);

                if(curEventTimeStamp>timestampMin &&
                        curEventTimeStamp<timestampMax &&
                        !isEventInList(aEvent.id,eventFromServer)) {
                    //delete event
                    Log.w("delete event "+aEvent.id+" as it not exist at server");
                    dbHelper.deleteAEvent(aEvent.id);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private boolean isEventInList(String id,ArrayList<WEvent> events) {
        boolean ret=false;

        for(WEvent aEvent : events) {
            if(aEvent.id.equals(id)) {
                ret=true;
                break;
            }
        }
        return ret;
    }

    private long getMaxTimestampFromEventList(ArrayList<WEvent> events) {
        long maxTimeStamp=Long.MIN_VALUE;

        long tmpTimeStamp;
        for(WEvent aEvent : events) {
            try {
                tmpTimeStamp=Long.valueOf(aEvent.timeStamp);

                if(tmpTimeStamp > maxTimeStamp) {
                    maxTimeStamp=tmpTimeStamp;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return maxTimeStamp;
    }

    private long getMinTimestampFromEventList(ArrayList<WEvent> events) {
        long minTimeStamp=Long.MAX_VALUE;

        long tmpTimeStamp;
        for(WEvent aEvent : events) {
            try {
                tmpTimeStamp=Long.valueOf(aEvent.timeStamp);

                if(tmpTimeStamp < minTimeStamp) {
                    minTimeStamp=tmpTimeStamp;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return minTimeStamp;
    }

    public int fGetPreviousEvents(String timestamp) {
        int errno=ErrorCode.UNKNOWN;

        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if(uid == null || password == null)
            return ErrorCode.INVALID_ARGUMENT;

        String action="get_previous_events";
        String postStr = "action="+action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&timestamp="+timestamp;
        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = ErrorCode.OK;

                Database dbHelper = new Database(mContext);

                ArrayList<WEvent> eventsFromServer=new ArrayList<WEvent>();
                Element resultElement = Utils.getFirstElementByTagName(root, action);
                if(resultElement != null) {
                    NodeList eventNodes = resultElement.getElementsByTagName("event");
                    if(eventNodes != null && eventNodes.getLength() > 0) {

                        for(int i = 0, n = eventNodes.getLength(); i < n; ++i) {
                            Element eventNode = (Element)eventNodes.item(i);
                            WEvent a = XmlHelper.parseWEvent(eventNode);
                            if(a != null && !Utils.isNullOrEmpty(a.id)) {
                                dbHelper.storeEvent(a);
                                eventsFromServer.add(a);
                            }
                        }
                    }

                    clearLocalEvents(eventsFromServer,timestamp);
                }
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }

        return errno;
    }

    /**
	 * 创建活动。
	 * 
	 * @param data The newly created WActivity object's ID will be written into data.actid.
	 * @return errno
	 */
    public int fAdd(WEvent data) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "add_event"; 
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password)
				+ "&text_title=" + Utils.urlencodeUtf8(data.title) 
				+ "&text_content=" + Utils.urlencodeUtf8(data.description) 
				+ "&latitude=" + data.latitude 
				+ "&longitude=" + data.longitude 
				+ "&area=" + Utils.urlencodeUtf8(data.address)
                + "&category=" + Utils.urlencodeUtf8(data.category)
				+ "&startdate=" + (data.startTime != null ? data.startTime.getTime() / 1000 : 0)
                + "&enddate=" + (data.endTime != null ? data.endTime.getTime() / 1000 : 0)
				+ "&max_member=" + data.capacity
				+ "&is_official=" + (data.isOfficial ? 1 : 0)
				+ "&allow_review=" + (data.allowReview ? 1 : 0) 
				+ "&need_work=" + (data.needWork ? 1 : 0);
		if(data.privacy_level != WEvent.PRIVACY_LEVEL_UNDEFINED)
			postStr += "&privacy_level=" + data.privacy_level;

        // HTTP request

        int errno = ErrorCode.UNKNOWN;

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = 0;

                NodeList eventIdNodes = root.getElementsByTagName("event_id");
                data.id = eventIdNodes.item(0).getTextContent();
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }
        return errno;
    }

    /**
     * 修改活动信息。
     * 
     * @param event_id
     * @param text
     * @param privacyLevel
     * @param allowReview
	 * @return errno
     */
    public int fEdit(String event_id,
            String text, int privacyLevel, boolean allowReview) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || event_id == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "edit_event"; 
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&event_id=" + Utils.urlencodeUtf8(event_id) 
				+ "&text_content=" + Utils.urlencodeUtf8(text) 
				+ "&privacy_level=" + privacyLevel 
				+ "&allow_review=" + (allowReview ? 1 : 0) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
    }

    /**
     * 上传多媒体附件。
     * 
     * @param event_id
     * @param multimedia_content_type
     * @param multimedia_content_path
     * @param multimedia_thumbnail_path empty for voice
     * @param duration for voice/video
	 * @return errno
     */
    public int fUploadMultimedia(String event_id,
    		String multimedia_content_type,
            String multimedia_content_path,
            String multimedia_thumbnail_path,
            int duration) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || event_id == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "upload_event_multimedia"; 
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&event_id=" + Utils.urlencodeUtf8(event_id) 
				+ "&multimedia_content_type=" + Utils.urlencodeUtf8(multimedia_content_type) 
				+ "&multimedia_content_path=" + Utils.urlencodeUtf8(multimedia_content_path)
                + "&multimedia_thumbnail_path=" + Utils.urlencodeUtf8(multimedia_thumbnail_path)
                + "&duration=" + duration
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
    }

    /**
     * 删除多媒体附件。
     * 
     * @param event_id
     * @param multimedia_content_id
	 * @return errno
     */
    public int fDeleteMultimedia(String event_id,
    		String multimedia_content_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || event_id == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "delete_event_multimedia";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "event_id=" + Utils.urlencodeUtf8(event_id) 
				+ "&multimedia_content_id=" + Utils.urlencodeUtf8(multimedia_content_id) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
    }

    /**
     * 发表评论。
     * 
     * @param event_id
     * @param comment_type
     * @param comment
	 * @return errno
     */
    public int fReview(String event_id,
    		String comment_type, String comment) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || event_id == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "review_event"; 
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&event_id=" + Utils.urlencodeUtf8(event_id) 
				+ "&comment_type=" + Utils.urlencodeUtf8(comment_type) 
				+ "&comment=" + Utils.urlencodeUtf8(comment)
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
    }

    /**
     * 删除活动。
     * 
     * @param event_id
	 * @return errno
     */
    public int fDelete(String event_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || event_id == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "delete_activity";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "event_id=" + Utils.urlencodeUtf8(event_id) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
    }
    
    /**
     * 删除活动评论。
     * 
     * @param event_id
     * @param review_id
	 * @return errno
     */
    public int fDeleteReview(String event_id, String review_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || event_id == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "delete_event_review";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "event_id=" + Utils.urlencodeUtf8(event_id) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
    }
    
    
    /**
     * 批准加入申请。
     * 
     * @param event_id
     * @param member_id
	 * @return errno
     */
    public int fApproveJoiningRequest(String event_id, String member_id) {
		return _processJoingRequest(event_id, member_id, true);
    }

    /**
     * 拒绝加入申请。
     * 
     * @param event_id
     * @param member_id
	 * @return errno
     */
    public int fRejectJoiningRequest(String event_id, String member_id) {
		return _processJoingRequest(event_id, member_id, false);
    }

	private int _processJoingRequest(String event_id, String member_id,
			boolean approved) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || event_id == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "deal_event_join_request";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&event_id=" + Utils.urlencodeUtf8(event_id) 
				+ "&member_id=" + Utils.urlencodeUtf8(member_id) 
				+ "&code=" + (approved ? 1 : 0)
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
	}
    
	/**
	 * 获取活动的成员列表。
	 * <p>
	 * 包括：
	 * <ul>
	 * <li>我自己</li>
	 * <li>正式成员</li>
	 * <li>已申请加入但尚未通过审核的成员</li>
	 * <li>已被邀请加入但尚未接受的成员</li>
	 * </ul>
	 * 
	 * @param result Buddy.tag value definitions:
	 * <ul>
	 * <li>0: this is a normal member.</li>
	 * <li>1: this is pending member who has requested to joining. </li>
	 * <li>2: this is pending member who has been invited.</li>
	 * <li>3: his joining request has been rejected.</li>
	 * <li>-1: something is wrong</li>
	 * @param event_id pass null to get members across all activities.
	 * @return errno
	 */
	public int fGetMembers(ArrayList<Buddy> result, String event_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null)
			return ErrorCode.INVALID_ARGUMENT;
		
		final String action = event_id == null
				? "list_all_event_join_members" : "list_event_join_members"; 
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password);
		if(event_id != null)
			postStr += "&event_id=" + Utils.urlencodeUtf8(event_id);
		
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;

				Element resultElement = Utils.getFirstElementByTagName(root, action); 
				if(resultElement != null) {
					NodeList nids = resultElement.getElementsByTagName("member_basic_info");
					if(nids != null) {
						for(int i = 0, n = nids.getLength(); i < n; ++i) {
							Buddy a = new Buddy();
							Element e;

							e = Utils.getFirstElementByTagName((Element)nids.item(i), "member_id");
							if(e != null)
								a.userID = e.getTextContent();
							e = Utils.getFirstElementByTagName((Element)nids.item(i), "wowtalk_id");
							if(e != null)
								a.wowtalkID = e.getTextContent();
							e = Utils.getFirstElementByTagName((Element)nids.item(i), "nickname");
							if(e != null)
								a.nickName = e.getTextContent();
							e = Utils.getFirstElementByTagName((Element)nids.item(i), "status");
							if(e != null)
								a.tag = Utils.tryParseInt(e.getTextContent(), 0);

							result.add(a);
						}
					}
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
	}

    /**
	 * Ask for joining a event.
	 * 
	 * @param event_id
	 * @param message say something to the event admin?
	 * @return
	 */
	public int fAskForJoining(String event_id, String message) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || event_id == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "require_to_join_event";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&event_id=" + Utils.urlencodeUtf8(event_id)
				+ "&msg=" + Utils.urlencodeUtf8(message)
				;
		return _doRequestWithoutResponse(postStr);
	}

    public int fJoinEvent(String event_id) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if(uid == null || password == null || event_id == null)
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "join_event";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&event_id=" + Utils.urlencodeUtf8(event_id);
        int errno = _doRequestWithoutResponse(postStr);
        if(ErrorCode.OK == errno) {
            setEventJoined(event_id);
        }
        return errno;
    }

    private void setEventJoined(String eventId) {
        Database dbHelper = new Database(mContext);

        dbHelper.setEventJoined(eventId);
    }

    public int fJoinEventWithDetail(String event_id,String name,String phone_number,String email) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if(uid == null || password == null || event_id == null)
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "join_the_event_with_details";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&event_id=" + Utils.urlencodeUtf8(event_id)
                + "&name=" + Utils.urlencodeUtf8(name)
                + "&phone_number=" + Utils.urlencodeUtf8(phone_number)
                + "&email=" + Utils.urlencodeUtf8(email);
        int errno = _doRequestWithoutResponse(postStr);
        if(ErrorCode.OK == errno) {
            setEventJoined(event_id);
        }
        return errno;
    }
}
