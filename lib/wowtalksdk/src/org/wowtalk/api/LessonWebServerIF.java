package org.wowtalk.api;

import android.content.Context;
import android.text.TextUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.List;

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
	
	public int getClassInfo(String class_id,List<ClassInfo> classInfos){
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
	
	public int getLessonDetail(int lesson_id,List<LessonDetail> lessonDetails,List<Camera> lessonDetails_camera){
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if (uid == null || password == null)
			return ErrorCode.NOT_LOGGED_IN;

		final String action = "get_lesson_detail";
		String postStr = "action=" + action + "&uid="
				+ Utils.urlencodeUtf8(uid) + "&password="
				+ Utils.urlencodeUtf8(password) + "&lesson_id="
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
