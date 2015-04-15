package org.wowtalk.api;

import android.content.Context;
import android.text.TextUtils;

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
                + "property_id=10";

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
}
