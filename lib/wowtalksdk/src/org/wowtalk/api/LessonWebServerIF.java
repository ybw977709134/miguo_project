package org.wowtalk.api;

import android.R.integer;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserFactory;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 课堂相关的功能。 Created by pzy on 12/21/14.
 */
public class LessonWebServerIF {

	private Context mContext;

	private PrefUtil mPrefUtil;

	private static LessonWebServerIF instance;

	private LessonWebServerIF(Context context) {
		mContext = context.getApplicationContext();
		mPrefUtil = PrefUtil.getInstance(context);
	}

	public static LessonWebServerIF getInstance(Context context) {
		if (instance == null) {
			instance = new LessonWebServerIF(context);
		}
		return instance;
	}

	/**
	 * do a http post request that do not require response data (except errno).
	 * 
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

	/**
	 * 添加或更新一节课。
	 * <p>
	 * 若输入 lesson_id=0 则添加，否则更新。
	 * </p>
	 * <p>
	 * 添加成功后，会把服务器返回的 lesson_id 保存到输入的 Lesson 对象中。
	 * </p>
	 * <p>
	 * 无论添加还是更新，成功后都把 Lesson 对象保存到本地数据库中。
	 * </p>
	 * 
	 * @param lesson
	 * @return Error Code.
	 */
	public int addOrModifyLesson(Lesson lesson,int live) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		final String action = lesson.lesson_id <= 0 ? "add_lesson"
				: "modify_lesson";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id="
				+ lesson.lesson_id + "&class_id=" + lesson.class_id + "&title="
				+ Utils.urlencodeUtf8(lesson.title) + "&start_date="
				+ lesson.start_date + "&end_date=" + lesson.end_date
				+ "&live=" + live;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					Element e = Utils.getFirstElementByTagName(resultElement,
							"lesson_id");
					if (e != null) {
						try {
							lesson.lesson_id = Integer.parseInt(e
									.getTextContent());
						} catch (NumberFormatException ex) {
							ex.printStackTrace();
							errno = ErrorCode.BAD_RESPONSE;
						}

						if (errno == ErrorCode.OK) {
							Database db = new Database(mContext);
							db.storeLesson(lesson);
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
	 * 添加或更新一道家庭作业。
	 * <p>
	 * 若输入 homework_id=0 则添加，否则更新。
	 * </p>
	 * <p>
	 * 添加成功后，会把服务器返回的 homework_id 保存到输入的 Homework 对象中。
	 * </p>
	 * <p>
	 * 无论添加还是更新，成功后都把 LessonHomework 对象保存到本地数据库中。
	 * </p>
	 * 
	 * @param homework
	 * @return Error Code.
	 */
	public int addOrModifyLessonHomework(LessonHomework homework) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		final String action = homework.homework_id <= 0 ? "add_lesson_homework"
				: "modify_lesson_homework";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id="
				+ homework.lesson_id + "&homework_id=" + homework.homework_id
				+ "&title=" + Utils.urlencodeUtf8(homework.title);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					Element e = Utils.getFirstElementByTagName(resultElement,
							"homework_id");
					if (e != null) {
						try {
							homework.homework_id = Integer.parseInt(e
									.getTextContent());
						} catch (NumberFormatException ex) {
							ex.printStackTrace();
							errno = ErrorCode.BAD_RESPONSE;
						}

						if (errno == ErrorCode.OK) {
							Database db = new Database(mContext);
							db.storeLessonHomework(homework);
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
	 * 添加或更新一个学生在一节课上的课堂表现。
	 * <p>
	 * 无论添加还是更新，成功后都把 LessonPerformance 对象保存到本地数据库中。
	 * </p>
	 * 
	 * @param performances
	 * @return Error Code.
	 */
	public int addOrModifyLessonPerformance(List<LessonPerformance> performances) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		if (performances.isEmpty())
			return ErrorCode.OK;

		final String action = "add_lesson_performance";
		StringBuffer postStr = new StringBuffer("action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id="
				+ performances.get(0).lesson_id + "&student_id="
				+ Utils.urlencodeUtf8(performances.get(0).student_id));
		for (LessonPerformance p : performances) {
			postStr.append("&property_id[]=");
			postStr.append(p.property_id);
			postStr.append("&property_value[]=");
			postStr.append(p.property_value);
		}

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr.toString());

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Database db = new Database(mContext);
				for (LessonPerformance p : performances) {
					db.storeLessonPerformance(p);
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
	}

    public int addOrModifyStudentsRollcall(List<LessonPerformance> performances) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if (uid == null || password == null)
            return ErrorCode.NOT_LOGGED_IN;

        if (performances.isEmpty())
            return ErrorCode.OK;

        final String action = "add_lesson_performance";
        StringBuffer postStr = new StringBuffer("action=" + action + "&uid="
                + Utils.urlencodeUtf8(uid) + "&password="
                + Utils.urlencodeUtf8(password) + "&lesson_id="
                + performances.get(0).lesson_id);
        for (LessonPerformance p : performances) {
            postStr.append("&student_id[]=")
                    .append(Utils.urlencodeUtf8(p.student_id))
                    .append("&property_id[]=")
                    .append(p.property_id)
                    .append("&property_value[]=")
                    .append(p.property_value);
        }
        postStr.append("&notify=1");

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr.toString());

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = ErrorCode.OK;

                Database db = new Database(mContext);
                for (LessonPerformance p : performances) {
                    db.storeLessonPerformance(p);
                }
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }
        return errno;
    }

	/**
	 * 添加或更新对一节课的家长意见。
	 * <p>
	 * 无论添加还是更新，成功后都把 LessonParentFeedback 对象保存到本地数据库中。
	 * </p>
	 * 
	 * @param feedback
	 * @param moment
	 *            家长意见的详情，将发布为匿名动态，然后让 LessonParentFeedback 对象引用之。
	 * @return Error Code.
	 */
	public int addOrModifyLessonParentFeedback(LessonParentFeedback feedback,
			Moment moment) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		if (moment != null && TextUtils.isEmpty(moment.id)) {
			int errno = MomentWebServerIF.getInstance(mContext).fAddMoment(
					moment, true);
			if (errno != ErrorCode.OK) {
				return errno;
			}
		}

		if (moment != null)
			feedback.moment_id = Integer.parseInt(moment.id);

		final String action = "add_lesson_parent_feedback";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id="
				+ feedback.lesson_id + "&student_id=" + feedback.student_id
				+ "&moment_id=" + feedback.moment_id;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;
				Database db = new Database(mContext);
				db.storeLessonParentFeedback(feedback);
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
	}

	public int getClassInfo(String class_id,List<ClassInfo> classInfos,GroupChatRoom result){
		int errno = ErrorCode.BAD_RESPONSE;
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return errno;
		final String action = "get_class_info_by_id";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&class_id="
				+ Utils.urlencodeUtf8(class_id);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
							ClassInfo info = XmlHelper
									.parseInfo(resultElement);
							classInfos.add(info);
				}
				
				Element resultElement2 = Utils.getFirstElementByTagName(root, action); 
				if(resultElement2 != null) {
					Element groupNode = Utils.getFirstElementByTagName(resultElement2, "group_info");
					if(groupNode != null)
						XmlHelper.parseGroup(groupNode, result);
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
	}
	
	public int getLesson(String class_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		final String action = "get_lesson";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&class_id="
				+ Utils.urlencodeUtf8(class_id);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					Database db = new Database(mContext);
					db.deleteLesson(class_id);
					NodeList lessonNodes = resultElement
							.getElementsByTagName("lesson");
					for (int i = 0; i < lessonNodes.getLength(); ++i) {
						Node lessonNode = lessonNodes.item(i);
						if (lessonNode instanceof Element) {
							Lesson lesson = XmlHelper
									.parseLesson((Element) lessonNode);
							if (lesson != null)
								if(db != null){
									db.storeLesson(lesson);
								}
								
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
	 * 通过返回的list<map<>>类型值获得老师的id和姓名
	 * @return list
	 * @author hutianfeng
	 * @date 2015/4/2
	 */
	public List<Map<String, Object>> getClassTeachers (String class_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return null;
		
		String action = "get_class_teachers";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid)
				+ "&password=" + Utils.urlencodeUtf8(password)
				+ "&class_id="+ Utils.urlencodeUtf8(class_id);;
		
		Connect2 connect2 = new Connect2();
		Connect2.SetTimeout(5000, 0);
		
		String xmlStr = connect2.getXmlString(postStr);
		
		//对获得的xml文件进行pull解析
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
			// 实例化一个xml pull解析对象
			XmlPullParser pullParser = factory.newPullParser();
			
			// 将xml文件作为流传入到inputstream
			//System.out.println(xmlStr);
	        xmlStr=xmlStr.replaceAll("&amp;", "＆");
	        xmlStr=xmlStr.replaceAll("&quot;", "\"");
	        xmlStr=xmlStr.replaceAll("&nbsp;", " ");

	        BufferedInputStream bis = new BufferedInputStream(
	        		new ByteArrayInputStream( xmlStr.getBytes()));
	        
	     // xml解析对象接收输入流对象
	        pullParser.setInput(bis, "utf-8");

	        int event = pullParser.getEventType();
	        List<Map<String, Object>> list = null;
	        Map<String, Object> map = null;
	        
	        while (event != XmlPullParser.END_DOCUMENT) {
	        	switch (event) {
	        	
	        	case XmlPullParser.START_DOCUMENT:
	        	list = new ArrayList<>();
	        	break;
	        	
	        	case XmlPullParser.START_TAG:
	        	if ("teacher".equals(pullParser.getName())) {
	        		map = new HashMap<String, Object>();
	        	}
	        	if (pullParser.getName().equals("uid")) {
	        		map.put("teacher_id", pullParser.nextText());
	        	}
	        	
	        	if (pullParser.getName().equals("wowtalk_id")) {
	        		map.put("teacher_username", pullParser.nextText());
	        	}
	        	       	
	        	if (pullParser.getName().equals("alias")) {
	        		map.put("teacher_alias", pullParser.nextText());
	        	}
	       
	        	break;
	        	
	        	case XmlPullParser.END_TAG:
	        	if (pullParser.getName().equals("teacher")) {
	        		list.add(map);
	        	}
	        	break;
	        	
	        	}
	        	event = pullParser.next();
	        
	        }  
	        return list;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	
	
	/**
	 * 通过返回的list<map<>>类型值获得学生的id和姓名
	 * @return list
	 * @author hutianfeng
	 * @date 2015/4/3
	 */
	public List<Map<String, Object>> getClassStudents (String class_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return null;
		
		String action = "get_class_students";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid)
				+ "&password=" + Utils.urlencodeUtf8(password)
				+ "&class_id="+ Utils.urlencodeUtf8(class_id);;
		
		Connect2 connect2 = new Connect2();
		Connect2.SetTimeout(5000, 0);
		
		String xmlStr = connect2.getXmlString(postStr);
		
		//对获得的xml文件进行pull解析
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
			// 实例化一个xml pull解析对象
			XmlPullParser pullParser = factory.newPullParser();
			
			// 将xml文件作为流传入到inputstream
			//System.out.println(xmlStr);
	        xmlStr=xmlStr.replaceAll("&amp;", "＆");
	        xmlStr=xmlStr.replaceAll("&quot;", "\"");
	        xmlStr=xmlStr.replaceAll("&nbsp;", " ");

	        BufferedInputStream bis = new BufferedInputStream(
	        		new ByteArrayInputStream( xmlStr.getBytes()));
	        
	     // xml解析对象接收输入流对象
	        pullParser.setInput(bis, "utf-8");

	        int event = pullParser.getEventType();
	        List<Map<String, Object>> list = null;
	        Map<String, Object> map = null;
	        
	        while (event != XmlPullParser.END_DOCUMENT) {
	        	switch (event) {
	        	
	        	case XmlPullParser.START_DOCUMENT:
	        	list = new ArrayList<>();
	        	break;
	        	
	        	case XmlPullParser.START_TAG:
	        	if ("student".equals(pullParser.getName())) {
	        		map = new HashMap<String, Object>();
	        	}
	        	if (pullParser.getName().equals("uid")) {
	        		map.put("student_id", pullParser.nextText());
	        	}
	        	
	        	if (pullParser.getName().equals("wowtalk_id")) {
	        		map.put("student_username", pullParser.nextText());
	        	}
	        	       	
	        	if (pullParser.getName().equals("alias")) {
	        		map.put("student_alias", pullParser.nextText());
	        	}
	       
	        	break;
	        	
	        	case XmlPullParser.END_TAG:
	        	if (pullParser.getName().equals("student")) {
	        		list.add(map);
	        	}
	        	break;
	        	
	        	}
	        	event = pullParser.next();
	        
	        }  
	        return list;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}
	
	
	
	public int setCameraStatus(String school_id, String[] camera_id,int[] status) {
		int errno = -1;
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return errno;

		final String action = "set_camera_status";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&school_id=" + school_id;
		int len = camera_id.length;
		for(int i = 0;i < len; i++){
			postStr += "&camera_id[]=" + camera_id[i] +"&status[]=" + status[i];
		}
				
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		
		errno = ErrorCode.BAD_RESPONSE;
//		if (root != null) {
//			NodeList errorList = root.getElementsByTagName("err_no");
//			Element errorElement = (Element) errorList.item(0);
//			String errorStr = errorElement.getFirstChild().getNodeValue();
//
//			if (errorStr.equals("0")) {
//				errno = ErrorCode.OK;
//
//				Element resultElement = Utils.getFirstElementByTagName(root,
//						action);
//				if (resultElement != null) {
//					Element e = Utils.getFirstElementByTagName(resultElement,
//							"status");
//					errno = Integer.parseInt(e.getTextContent());
//				}
//			} else {
//				errno = Integer.parseInt(errorStr);
//			}
//		}
		return errno;
		
	}
	public int releaseRoom(int lesson_id){
		int status = -1;
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return status;

		final String action = "release_room";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id=" + lesson_id;
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					Element e = Utils.getFirstElementByTagName(resultElement,
							"status");
					status = Integer.parseInt(e.getTextContent());
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return status;
		
	}
	public int setUseRoom(int room_id, String lesson_id) {
		int status = -1;
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return status;

		final String action = "use_room";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&room_id=" + room_id
				+ "&lesson_id=" + Utils.urlencodeUtf8(lesson_id);
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					Element e = Utils.getFirstElementByTagName(resultElement,
							"status");
					status = Integer.parseInt(e.getTextContent());
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return status;
	}
	
	public int getLessonDetail(int lesson_id,List<LessonDetail> lessonDetails,
			List<Camera> lessonDetails_camera,List<LessonHomework> lessoonDetails_homework,
			List<LessonPerformance> lessoonDetails_performance,List<LessonParentFeedback> lessoonDetails_parent_feedback){
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		final String action = "get_lesson_detail";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id="
				+ lesson_id + "&with_all_students_performances="
				+ 1 +"&with_all_students_parent_feedbacks="
				+ 1;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				
				if (resultElement != null) {
				
					NodeList roomNodes = resultElement
							.getElementsByTagName("room");
					int len = roomNodes.getLength();
					for (int i = 0; i < len; ++i) {
						Node roomNode = roomNodes.item(i);
						if (roomNode instanceof Element) {
							LessonDetail detail = XmlHelper
									.parseLessonDetail_classroom((Element) roomNode);
							lessonDetails.add(detail);
						}
					}
				}
				if (resultElement != null) {
					NodeList roomNodes = resultElement
							.getElementsByTagName("camera");
					int len = roomNodes.getLength();
					for (int i = 0; i < len; ++i) {
						Node roomNode = roomNodes.item(i);
						if (roomNode instanceof Element) {
							Camera detail_camera = XmlHelper
									.parseCamera((Element) roomNode);
							lessonDetails_camera.add(detail_camera);
						}
					}
				}
				if (resultElement != null) {
					NodeList roomNodes = resultElement
							.getElementsByTagName("homework");
					int len = roomNodes.getLength();
					for (int i = 0; i < len; ++i) {
						Node roomNode = roomNodes.item(i);
						if (roomNode instanceof Element) {
							LessonHomework homework = XmlHelper
									.parseHomework((Element) roomNode);
							lessoonDetails_homework.add(homework);
						}
					}
				}
				if (resultElement != null) {
					NodeList roomNodes = resultElement
							.getElementsByTagName("performance");
					int len = roomNodes.getLength();
					for (int i = 0; i < len; ++i) {
						Node roomNode = roomNodes.item(i);
						if (roomNode instanceof Element) {
							LessonPerformance performance = XmlHelper
									.parseLessonPerformance((Element) roomNode);
							lessoonDetails_performance.add(performance);
						}
					}
				}
				if (resultElement != null) {
					NodeList roomNodes = resultElement
							.getElementsByTagName("parent_feedback");
					int len = roomNodes.getLength();
					for (int i = 0; i < len; ++i) {
						Node roomNode = roomNodes.item(i);
						if (roomNode instanceof Element) {
							LessonParentFeedback parentFeedback = XmlHelper
									.parseLessonParentFeedback((Element) roomNode);
							lessoonDetails_parent_feedback.add(parentFeedback);
						}
					}
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
		
	}
	public List<Classroom> getClassroom(String school_id,
			long start_date_timestamp, long end_date_timestamp) {
		List<Classroom> classrooms = new ArrayList<Classroom>();
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return classrooms;

		final String action = "get_room";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&school_id="
				+ Utils.urlencodeUtf8(school_id) + "&start_date="
				+ start_date_timestamp + "&end_date=" + end_date_timestamp;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					NodeList roomNodes = resultElement
							.getElementsByTagName("room");
					int len = roomNodes.getLength();
					for (int i = 0; i < len; ++i) {
						Node roomNode = roomNodes.item(i);
						if (roomNode instanceof Element) {
							Classroom room = XmlHelper
									.parseRoom((Element) roomNode);
							classrooms.add(room);
						}
					}
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return classrooms;
	}
	public List<Camera> getCameraByLesson(String school_id, int lesson_id) {
		List<Camera> cameras = new ArrayList<Camera>();
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return cameras;

		final String action = "get_camera_by_lesson";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&school_id="
				+ Utils.urlencodeUtf8(school_id) + "&lesson_id="
				+ lesson_id;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					NodeList roomNodes = resultElement
							.getElementsByTagName("camera");
					int len = roomNodes.getLength();
					for (int i = 0; i < len; ++i) {
						Node roomNode = roomNodes.item(i);
						if (roomNode instanceof Element) {
							Camera camera = XmlHelper
									.parseCamera((Element) roomNode);
							cameras.add(camera);
						}
					}
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return cameras;
	}
	public List<Camera> getCamera(String school_id, String room_id) {
		List<Camera> cameras = new ArrayList<Camera>();
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return cameras;

		final String action = "get_camera";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&school_id="
				+ Utils.urlencodeUtf8(school_id) + "&room_id="
				+ Utils.urlencodeUtf8(room_id);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					NodeList roomNodes = resultElement
							.getElementsByTagName("camera");
					int len = roomNodes.getLength();
					for (int i = 0; i < len; ++i) {
						Node roomNode = roomNodes.item(i);
						if (roomNode instanceof Element) {
							Camera camera = XmlHelper
									.parseCamera((Element) roomNode);
							cameras.add(camera);
						}
					}
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return cameras;
	}

	public int getLessonHomework(int lesson_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		final String action = "get_lesson_homework";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id=" + lesson_id;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					Database db = new Database(mContext);
					NodeList homeworkNodes = resultElement
							.getElementsByTagName("homework");
					for (int i = 0; i < homeworkNodes.getLength(); ++i) {
						Node homeworkNode = homeworkNodes.item(i);
						if (homeworkNode instanceof Element) {
							LessonHomework homework = XmlHelper
									.parseLessonHomework((Element) homeworkNode);
							if (homework != null)
								db.storeLessonHomework(homework);
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
	 * 获取家长意见。
	 * 
	 * @param lesson_id
	 * @param student_id
	 * @return
	 */
	public int getLessonParentFeedback(int lesson_id, String student_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		final String action = "get_lesson_parent_feedback";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id=" + lesson_id
				+ "&student_id=" + student_id;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					Database db = new Database(mContext);
					NodeList feedbackNodes = resultElement
							.getElementsByTagName("feedback");
					for (int i = 0; i < feedbackNodes.getLength(); ++i) {
						Node feedbackNode = feedbackNodes.item(i);
						if (feedbackNode instanceof Element) {
							LessonParentFeedback feedback = XmlHelper
									.parseLessonParentFeedback((Element) feedbackNode);
							if (feedback != null) {
								db.storeLessonParentFeedback(feedback);
								if (feedback.moment_id > 0) {
									MomentWebServerIF.getInstance(mContext)
											.fGetMomentById(feedback.moment_id);
								}
							}
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
     * 提交请假结果
      * @param lessonId
     * @return List
     */
	public int askForLeave(int lessonId){
		int errno = -1;
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return errno;

		final String action = "ask_for_leave";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password)
				+ "&lesson_id=" + lessonId;

		errno = _doRequestWithoutResponse(postStr);
//		int errno = ErrorCode.BAD_RESPONSE;
//		if (root != null) {
//			NodeList errorList = root.getElementsByTagName("err_no");
//			Element errorElement = (Element) errorList.item(0);
//			String errorStr = errorElement.getFirstChild().getNodeValue();
//
//			if (errorStr.equals("0")) {
//				errno = ErrorCode.OK;
//
//				Element resultElement = Utils.getFirstElementByTagName(root,
//						action);
//				if (resultElement != null) {
//					Element e = Utils.getFirstElementByTagName(resultElement,
//							"performance_id");
//					performance_id = Integer.parseInt(e.getTextContent());
//				}
//			} else {
//				errno = Integer.parseInt(errorStr);
//			}
//		}
		return errno;
	}
    /**
     * 获取课程的上课签到情况
      * @param lessonId
     * @return List
     */
    public List<LessonPerformance> getLessonRollCalls(int lessonId){
    /*
    action=get_lesson_performance&uid=823e3319-1f88-4d8b-9c20-2962e12f05b3&password=db84a80e08be1c8ba1e224289348181e&lesson_id=3&property_id=10
     */
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if (uid == null || password == null)
            return null;

        if(lessonId <= 0)
            return null;

        List<LessonPerformance> performances = new ArrayList<>();
        final String action = "get_lesson_performance";
        String postStr = "action=" + action + "&uid="
                + Utils.urlencodeUtf8(uid) + "&password="
                + Utils.urlencodeUtf8(password) + "&lesson_id=" + lessonId
                + "&student_id=ALL"
                + "&property_id=10";

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);
        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                Element resultElement = Utils.getFirstElementByTagName(root, action);
                if (resultElement != null) {
                    Database db = new Database(mContext);
                    NodeList performanceNodes = resultElement
                            .getElementsByTagName("lesson_performance");
                    for (int i = 0; i < performanceNodes.getLength(); ++i) {
                        Node performanceNode = performanceNodes.item(i);
                        if (performanceNode instanceof Element) {
                            LessonPerformance performance = XmlHelper
                                    .parseLessonPerformance((Element) performanceNode);
                            if (performance != null) {
                                performances.add(performance);
                                db.storeLessonPerformance(performance);
                            }
                        }
                    }
                }
            } else {
                return null;
            }
        }
        return performances;
    }

	public int getLessonPerformance(int lesson_id, String student_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		final String action = "get_lesson_performance";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id=" + lesson_id
				+ "&student_id=" + student_id;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;

				Element resultElement = Utils.getFirstElementByTagName(root,
						action);
				if (resultElement != null) {
					Database db = new Database(mContext);
					NodeList performanceNodes = resultElement
							.getElementsByTagName("lesson_performance");
					// android.util.Log.i("-->>", performanceNodes.toString() +
					// "");
					for (int i = 0; i < performanceNodes.getLength(); ++i) {
						Node performanceNode = performanceNodes.item(i);
						if (performanceNode instanceof Element) {
							LessonPerformance feedback = XmlHelper
									.parseLessonPerformance((Element) performanceNode);
							feedback.lesson_id = lesson_id;
							feedback.student_id = student_id;

							// android.util.Log.i("-->>",
							// feedback.property_value + "");
							if (feedback != null)
								db.storeLessonPerformance(feedback);
						}
					}
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
	}
	public int signupHomeworkResult(int homework_id,Moment moment){
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;


		if (moment != null && TextUtils.isEmpty(moment.id)) {
			int errno = MomentWebServerIF.getInstance(mContext).fAddMoment(
					moment, true);
			if (errno != ErrorCode.OK) {
				return errno;
			}
		}

		final String action = "add_homework_result";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&homework_id="
				+ homework_id
				+ "&moment_id=" + Integer.parseInt(moment.id);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		
		int homeworkResult_id = 0;
	    int moment_id = 0;
	    String student_id = null;
     	int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;
				Element e = Utils.getFirstElementByTagName(root, "id");
				if (null != e)
					homeworkResult_id = Utils.tryParseInt(e.getTextContent(), 0);
				e = Utils.getFirstElementByTagName(root, "student_id");
				if (null != e)
					student_id = e.getTextContent();
				e = Utils.getFirstElementByTagName(root, "moment_id");
				if (null != e)
					moment_id = Utils.tryParseInt(e.getTextContent(), 0);
				Database db = new Database(mContext);
				db.storeLessonAddHomeworkResult(homeworkResult_id,homework_id,moment_id,student_id);
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
	}
	public int addLessonHomework(LessonAddHomework addhomework,Moment moment){
	    int homeworkID = 0;
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;


		if (moment != null && TextUtils.isEmpty(moment.id)) {
			int errno = MomentWebServerIF.getInstance(mContext).fAddMoment(
					moment, true);
			if (errno != ErrorCode.OK) {
				return errno;
			}
		}

		if (moment != null)
			addhomework.moment_id = Integer.parseInt(moment.id);

		final String action = "add_lesson_homework";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id="
				+ addhomework.lesson_id 
				+ "&moment_id=" + addhomework.moment_id;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		
     	int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;
				Element e = Utils.getFirstElementByTagName(root, "id");
				if (null != e)
					homeworkID = Utils.tryParseInt(e.getTextContent(), 0);
				Database db = new Database(mContext);
				db.storeLessonAddHomework(addhomework,homeworkID);
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}

		return errno;
	}

	public int deleteLesson(String lesson_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		final String action = "del_lesson";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id=" + lesson_id;

		int errno = _doRequestWithoutResponse(postStr);
		if (errno == ErrorCode.OK) {
			Database db = new Database(mContext);
			db.deleteLessonById(lesson_id);
		}
		return errno;
	}
	
	public List<Map<String, Object>> get_homework_state (int lesson_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return null;
		
		String action = "get_homework_state";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid)
				+ "&password=" + Utils.urlencodeUtf8(password)
				+ "&lesson_id="+ lesson_id;
		
		Connect2 connect2 = new Connect2();
		Connect2.SetTimeout(5000, 0);
		
		String xmlStr = connect2.getXmlString(postStr);
		
		//对获得的xml文件进行pull解析
		XmlPullParserFactory factory;
		try {
			factory = XmlPullParserFactory.newInstance();
			// 实例化一个xml pull解析对象
			XmlPullParser pullParser = factory.newPullParser();
			
			// 将xml文件作为流传入到inputstream
			//System.out.println(xmlStr);
	        xmlStr=xmlStr.replaceAll("&amp;", "＆");
	        xmlStr=xmlStr.replaceAll("&quot;", "\"");
	        xmlStr=xmlStr.replaceAll("&nbsp;", " ");

	        BufferedInputStream bis = new BufferedInputStream(
	        		new ByteArrayInputStream( xmlStr.getBytes()));
	        
	     // xml解析对象接收输入流对象
	        pullParser.setInput(bis, "utf-8");

	        int event = pullParser.getEventType();
//	        List<List<Map<String, Object>>> listALL = null;
	        List<Map<String, Object>> list = null;
	        List<Map<String, Object>> resultList = null;
	        Map<String, Object> map = null;
//	        String homework_id = null;
	        int flag = 0;
	        
	        while (event != XmlPullParser.END_DOCUMENT) {
	        	int i = 1;

	        	i++;
	        	switch (event) {
	        	
	        	
	        	case XmlPullParser.START_DOCUMENT:
//	        		listALL = new ArrayList<List<Map<String, Object>>>();
	        		
	        		list = new ArrayList<Map<String,Object>>();
	        		
	        	
	        	break;
	        	
	        	case XmlPullParser.START_TAG:
		        	if ("homework".equals(pullParser.getName())) {
//		        		list = new ArrayList<Map<String, Object>>();
		        		flag = 1;
		        	}

		        	if ("homework_id".equals(pullParser.getName())) {
		        		if(flag == 1){
		        			map = new HashMap<String, Object>();	
		        		    map.put("homework_id",  Integer.valueOf(pullParser.nextText()));
		        		}
		        		
		        	}
		        	
		        	if ("student".equals(pullParser.getName())) {
		        		if (flag == 2) {
		        			map = new HashMap<String, Object>();
		        		}

		        	}

		        	if (pullParser.getName().equals("uid")) {
		        		map.put("stu_uid", pullParser.nextText());
		        	}
		        	
		        	if (pullParser.getName().equals("name")) {
		        		map.put("stu_name", pullParser.nextText());
		        	}
		        	       	
		        	if (pullParser.getName().equals("state")) {
		        		map.put("stu_state", pullParser.nextText());
		        	}
		        	if (pullParser.getName().equals("homework_result_id")) {
		        		map.put("result_id", pullParser.nextText());
		        	}
		        	
	        	break;
	        	
	        	case XmlPullParser.END_TAG:
	        	
	        	if (pullParser.getName().equals("student")) {
	        		
	        		list.add(map);
	        		if (flag == 1) {
	        			flag = 2;
	        		}
	        	}
	        	break;
	        	
	        	}
	        	event = pullParser.next();
	        
	        }  
	        return list;
		} catch (Exception e) {
			e.printStackTrace();
		}

		return null;
	}

    /**
     *
     * Created by hutianfeng on 2015/5/21
     * @param lesson_id
     */
    public int getLessonHomeWork(int lesson_id,GetLessonHomework getLessonHomework,String student_id,int tag){

        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();

        if (uid == null || password == null)
            return 0;

        String action = "get_lesson_homework";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&lesson_id="+ lesson_id
                + "&student_id=" + Utils.urlencodeUtf8(student_id);

        Connect2 connect2 = new Connect2();
        Connect2.SetTimeout(5000, 0);

        String xmlStr = connect2.getXmlString(postStr);

        //对获得的xml文件进行pull解析
        XmlPullParserFactory factory;
        try {
            factory = XmlPullParserFactory.newInstance();
            // 实例化一个xml pull解析对象
            XmlPullParser pullParser = factory.newPullParser();

            // 将xml文件作为流传入到inputstream
            //System.out.println(xmlStr);
            xmlStr=xmlStr.replaceAll("&amp;", "＆");
            xmlStr=xmlStr.replaceAll("&quot;", "\"");
            xmlStr=xmlStr.replaceAll("&nbsp;", " ");

            BufferedInputStream bis = new BufferedInputStream(
                    new ByteArrayInputStream( xmlStr.getBytes()));

            // xml解析对象接收输入流对象
            pullParser.setInput(bis, "utf-8");

            int event = pullParser.getEventType();

            List<HomeWorkResult> stuResultList = null;
            List<HomeWorkMultimedia> homeWorkMultimedias = null;
            HomeWorkMoment teacherMoment = null;//老师布置活动
            HomeWorkMoment stuMoment = null;//学生布置活动

            HomeWorkMultimedia homeWorkMultimedia = null;//多媒体对象
            HomeWorkResult homeWorkResult = null;//学生做题结果
            HomeWorkReview homeWorkReview = null;//老师批改结果

            /**
             * 1-老师moment_id，2-老师moment的老师moment_id，3-老师多媒体moment_id
             * 4-学生moment_id，5-学生moment的老师moment_id，6-学生多媒体moment_id
             * */
            int flagMoment = 0;


            while (event != XmlPullParser.END_DOCUMENT) {
                switch (event) {

                    case XmlPullParser.START_TAG:

                        if ("homework".equals(pullParser.getName())) {
                            flagMoment = 1;//最外层
                        }


                        if ("moment".equals(pullParser.getName())) {

                            if (flagMoment == 1) {
                                flagMoment = 2;
                                teacherMoment = new HomeWorkMoment();
                            }

                            if (flagMoment == 4) {
                                flagMoment = 5;
                                stuMoment = new HomeWorkMoment();
                            }

                            homeWorkMultimedias = new ArrayList<HomeWorkMultimedia>();
                        }



                        if (pullParser.getName().equals("id")) {
                            if (flagMoment == 1) {//最外层id
                                getLessonHomework.id = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 4) {//homeWorkResult的id
                                homeWorkResult.id = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 6) {
                                homeWorkReview.id = Integer.valueOf(pullParser.nextText());
                            }
                        }


                        if (pullParser.getName().equals("lesson_id")) {
                            if (flagMoment == 1) {
                                getLessonHomework.lesson_id = Integer.valueOf(pullParser.nextText());
                            }
                        }


                        if (pullParser.getName().equals("moment_id")) {

                            if (flagMoment == 1) {
                                getLessonHomework.moment_id = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 2) {
                                teacherMoment.moment_id = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 3) {
                                homeWorkMultimedia.moment_id = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 4) {
                                homeWorkResult.moment_id = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 5) {
                                stuMoment.moment_id = Integer.valueOf(pullParser.nextText());
                               
                            }

                            if (flagMoment == 6) {
                                homeWorkMultimedia.moment_id = Integer.valueOf(pullParser.nextText());
                            }

                        }

                        /**
                         * 以下是moment段*/
                        if (pullParser.getName().equals("owner_id")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.owner_id = pullParser.nextText();
                            }

                            if (flagMoment == 5) {//学生
                                stuMoment.owner_id = pullParser.nextText();
                            }
                        }


                        if (pullParser.getName().equals("insert_timestamp")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.insert_timestamp = Long.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 5) {//学生
                                stuMoment.insert_timestamp = Long.valueOf(pullParser.nextText());
                            }
                        }


                        if (pullParser.getName().equals("insert_latitude")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.insert_latitude = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 5) {//学生
                                stuMoment.insert_latitude = Integer.valueOf(pullParser.nextText());
                            }
                        }


                        if (pullParser.getName().equals("insert_longitude")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.insert_longitude = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 5) {//学生
                                stuMoment.insert_longitude = Integer.valueOf(pullParser.nextText());
                            }
                        }


                        if (pullParser.getName().equals("text_content")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.text_content = pullParser.nextText();
                            }

                            if (flagMoment == 5) {//学生
                                stuMoment.text_content = pullParser.nextText();
                            }
                        }

                        if (pullParser.getName().equals("privacy_level")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.privacy_level = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 5) {//学生
                                stuMoment.privacy_level = Integer.valueOf(pullParser.nextText());
                            }
                        }


                        if (pullParser.getName().equals("allow_review")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.allow_review = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 5) {//学生
                                stuMoment.allow_review = Integer.valueOf(pullParser.nextText());
                            }
                        }


                        if (pullParser.getName().equals("tag")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.tag = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 5) {//学生
                                stuMoment.tag = Integer.valueOf(pullParser.nextText());
                            }
                        }


                        if (pullParser.getName().equals("deadline")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.deadline = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment ==5) {//学生
                                stuMoment.deadline = Integer.valueOf(pullParser.nextText());
                            }
                        }


                        if (pullParser.getName().equals("delete")) {
                            if (flagMoment == 2) {//老师
                                teacherMoment.delete = Integer.valueOf(pullParser.nextText());
                            }

                            if (flagMoment == 5) {//学生
                                stuMoment.delete = Integer.valueOf(pullParser.nextText());
                            }
                        }



                        if ("multimedias".equals(pullParser.getName())) {

                            if (flagMoment == 2) {
                                flagMoment = 3;
                            }

                            if (flagMoment == 5) {
                                flagMoment = 6;
                            }

                            homeWorkMultimedia = new HomeWorkMultimedia();
                        }


                        if (pullParser.getName().equals("multimedia_content_id")) {
                                homeWorkMultimedia.multimedia_content_id = Integer.valueOf(pullParser.nextText());
                        }

                        if (pullParser.getName().equals("multimedia_content_type")) {
                                homeWorkMultimedia.multimedia_content_type = pullParser.nextText();
                        }


                        if (pullParser.getName().equals("multimedia_content_path")) {
                                homeWorkMultimedia.multimedia_content_path = pullParser.nextText();
                        }


                        if (pullParser.getName().equals("duration")) {
                                homeWorkMultimedia.duration = Integer.valueOf(pullParser.nextText());
                        }


                        if (pullParser.getName().equals("multimedia_thumbnail_path")) {
                                homeWorkMultimedia.multimedia_thumbnail_path = pullParser.nextText();
                        }


                        if ("homework_results".equals(pullParser.getName())) {
                            stuResultList = new ArrayList<HomeWorkResult>();
                        }


                        if ("homework_result".equals(pullParser.getName())) {

                            flagMoment = 4;
                            homeWorkResult = new HomeWorkResult();

                        }


                        if (pullParser.getName().equals("student_id")) {
                            homeWorkResult.student_id = pullParser.nextText();
                        }


                        if (pullParser.getName().equals("homework_id")) {

                            if (flagMoment == 4){
                                homeWorkResult.homework_id = Integer.valueOf(pullParser.nextText());
                            }


                            if (flagMoment == 6) {
                                getLessonHomework.homework_id = Integer.valueOf(pullParser.nextText());
                            }
                        }


                        if ("homework_review".equals(pullParser.getName())) {
                            homeWorkReview = new HomeWorkReview();
                        }


                        if (pullParser.getName().equals("homeworkresult_id")) {
                            homeWorkReview.homeworkresult_id = Integer.valueOf(pullParser.nextText());
                        }

                        if (pullParser.getName().equals("rank1")) {
                            homeWorkReview.rank1 = Integer.valueOf(pullParser.nextText());
                        }

                        if (pullParser.getName().equals("rank2")) {
                            homeWorkReview.rank2 = Integer.valueOf(pullParser.nextText());
                        }

                        if (pullParser.getName().equals("rank3")) {
                            homeWorkReview.rank3 = Integer.valueOf(pullParser.nextText());
                        }

                        if (pullParser.getName().equals("text")) {
                            homeWorkReview.text = pullParser.nextText();
                        }


                        //最外层的作业名
                        if (pullParser.getName().equals("title")) {
                            getLessonHomework.title = pullParser.nextText();
                        }

                        break;

                    case XmlPullParser.END_TAG:


                        if (pullParser.getName().equals("multimedias")) {

                                homeWorkMultimedias.add(homeWorkMultimedia);
                        }

                        if (pullParser.getName().equals("moment")) {

                            if (flagMoment == 3) {//老师的布置的作业结束
                                teacherMoment.homeWorkMultimedias = homeWorkMultimedias;
                                getLessonHomework.teacherMoment = teacherMoment;
                            }

                            if (flagMoment == 6) {//老师的布置的作业结束
                                stuMoment.homeWorkMultimedias = homeWorkMultimedias;

                            }

                        }


                        if (pullParser.getName().equals("homework_review")) {
                            homeWorkResult.homeWorkReview = homeWorkReview;
                        }

                        if (pullParser.getName().equals("homework_result")) {
                        	homeWorkResult.stuMoment = stuMoment;
                        	if(tag == 1){
                        		 
                        	}else if(tag == 2){
                        		MomentWebServerIF.getInstance(mContext).fGetMomentById(stuMoment.moment_id);
                        	}
                        	 
                            stuResultList.add(homeWorkResult);

                        }

                        if (pullParser.getName().equals("homework_results")) {

                            getLessonHomework.stuResultList = stuResultList;

                        }

                        break;

                }
                event = pullParser.next();
            }
            MomentWebServerIF.getInstance(mContext).fGetMomentById(teacherMoment.moment_id);    
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 0;
    }

    public int delHomework(int homework_id){
		int status = -1;
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return status;

		final String action = "del_lesson_homework";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&homework_id=" + homework_id;
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		Database db = new Database(mContext);
		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;
				db.deleteLessonHomework(homework_id);
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return status;
		
	}
    public int delHomeworkResult(int homeworkResult_id){
		int status = -1;
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return status;

		final String action = "del_homework_result";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&id=" + homeworkResult_id;
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		Database db = new Database(mContext);
		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = ErrorCode.OK;
				db.deleteLessonHomeworkResult(homeworkResult_id);
			} else {
				errno = Integer.parseInt(errorStr);
			}
			status = errno;
		}
		return status;
		
	}

    /**
     * 老师添加对作业的评论
     * Created by hutianfeng on 2015/5/27
     * @param rank1
     * @param rank2
     * @param rank3
     * @param text
     * @return
     */
    public int addHomeworkReview(int homeworkresult_id,int rank1, int rank2, int rank3, String text) {
        int errno = -1;
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if (uid == null || password == null)
            return errno;

        final String action = "add_homework_review";
        String postStr = "action=" + action +
                "&uid=" + Utils.urlencodeUtf8(uid) +
                "&password=" + Utils.urlencodeUtf8(password) +
                "&homeworkresult_id=" + homeworkresult_id +
                "&rank1=" + rank1 +
                "&rank2=" + rank2 +
                "&rank3=" + rank3 +
                "&text=" + Utils.urlencodeUtf8(text);

        errno = _doRequestWithoutResponse(postStr);

        return errno;

    }

    /**
     * 删除老师对作业的评论
     * Created by hutianfeng on 2015/5/27
     * @param homeworkresult_id
     * @return
     */
    public int delHomeworkReview(int homeworkresult_id) {
        int errno = -1;
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if (uid == null || password == null)
            return errno;

        final String action = "del_homework_review";
        String postStr = "action=" + action +
                "&uid=" + Utils.urlencodeUtf8(uid) +
                "&password=" + Utils.urlencodeUtf8(password) +
                "&homeworkresult_id=" + homeworkresult_id;

        errno = _doRequestWithoutResponse(postStr);

        return errno;
    }

}
