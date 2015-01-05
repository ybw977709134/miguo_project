package org.wowtalk.api;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 好友动态功能。
 */
public class MomentWebServerIF {

	private Context mContext;

    private PrefUtil mPrefUtil;
	
	private static MomentWebServerIF instance;

	private MomentWebServerIF(Context context) {
		mContext = context.getApplicationContext();
        mPrefUtil = PrefUtil.getInstance(context);
	}
	
	public static MomentWebServerIF getInstance(Context context) {
		if(instance == null) {
			instance = new MomentWebServerIF(context);
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

    public int voteMomentSurvey(Moment moment,ArrayList<String> selectedOptionList) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if(uid == null || password == null)
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "vote_moment_survey";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&moment_id=" + moment.id
                + "&with_detail_back=" + 1;

        if(selectedOptionList.size() > 1) {
            StringBuilder sb=new StringBuilder();
            for(String optionId : selectedOptionList) {
                sb.append("&option_id[]="+optionId);
            }
            postStr += sb.toString();
        } else {
            postStr += "&option_id="+selectedOptionList.get(0);
        }

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0") || errorStr.equals(String.valueOf(ErrorCode.MOMENT_SURVEY_HAS_VOTED))) {
                errno = Integer.parseInt(errorStr);

                //handle returned vote info
                Element resultElement = Utils.getFirstElementByTagName(root, action);
                if(resultElement != null) {
                    Element e = Utils.getFirstElementByTagName(resultElement, "moment");
                    if(e != null) {
                        Database db = new Database(mContext);
                        Moment momentRet = XmlHelper.parseMoment(null, e,mContext,null);

                        momentRet.multimedias=moment.multimedias;
                        db.storeMoment(momentRet,null);
                    }
                }
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }
        return errno;
    }

    public int fAddMomentForSurvey(Moment moment) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if(uid == null || password == null)
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "add_moment_survey";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&text=" + Utils.urlencodeUtf8(moment.text)
                + "&latitude=" + moment.latitude
                + "&longitude=" + moment.longitude
                + "&privacy_level=" + moment.privacyLevel
                + "&allow_multichoice=" + (moment.isSurveyAllowMultiSelect?1:0)
                + "&deadline=" + moment.surveyDeadLine
                + "&with_detail_back=" + 1;
        if (moment.place != null)
            postStr += "&place=" + moment.place;

        StringBuilder sb=new StringBuilder();
        for(Moment.SurveyOption aOption : moment.surveyOptions) {
            sb.append("&option[]="+aOption.optionDesc);
        }
        postStr += sb.toString();

        sb.setLength(0);
        if(null != moment.limitedDepartmentList && moment.limitedDepartmentList.size() > 0) {
            for(String limitedDep : moment.limitedDepartmentList) {
                sb.append("&sharerange[]="+limitedDep);
            }
            postStr += sb.toString();
        }

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
                    Element e = Utils.getFirstElementByTagName(resultElement, "moment");
                    if(e != null) {
                        // build a Moment object, and save it into db.

                        Database db = new Database(mContext);

//                        if (moment == null) {
//                            moment = new Moment();
//                        }

                        String oldMomentId=moment.id;

                        Moment momentRet = XmlHelper.parseMoment(null, e,mContext,oldMomentId);
                        momentRet.multimedias=moment.multimedias;
//                        moment.id = e.getTextContent();
//                        moment.timestamp = Utils.getFirstLongByTagName(resultElement, "timestamp",
//                                new Date().getTime() / 1000);
//                        if (null == moment.owner)
//                            moment.owner = new Buddy();
//                        moment.owner.userID = uid;
                        moment.likedByMe = false;

                        db.storeMoment(momentRet,oldMomentId);
                    }
                }
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }

        return errno;
    }

	/**
	 * Add a Moment to web server and local db.
     *
	 * @param moment
	 * @return {@link ErrorCode}
     *
     * If succeed, moment.id and moment.timestamp will be assigned.
	 */
    public int fAddMoment(Moment moment) {
        return fAddMoment(moment, false);
    }

    /**
     * Add a Moment to web server and local db.
     *
     * @param moment
     * @param anonymous 如果匿名，则此动态不会出现在任何人的好友圈中，但可以通过moment ID访问。
     *                  匿名动态的用途之一是保存学生家长对课堂的反馈。
     * @return
     */
    public int fAddMoment(Moment moment, boolean anonymous) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "add_moment";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid)
				+ "&password=" + Utils.urlencodeUtf8(password)
				+ "&latitude=" + moment.latitude
				+ "&longitude=" + moment.longitude
				+ "&text=" + Utils.urlencodeUtf8(moment.text)
				+ "&privacy_level=" + moment.privacyLevel
                + "&tag=" + moment.tag
                + "&deadline=" + moment.surveyDeadLine
                + "&anonymous=" + (anonymous ? 1 : 0)
				+ "&lang=" + Locale.getDefault().getLanguage();
        if (moment.place != null)
            postStr += "&place=" + moment.place;

        StringBuilder sb=new StringBuilder();
        sb.setLength(0);
        if(null != moment.limitedDepartmentList && moment.limitedDepartmentList.size() > 0) {
            for(String limitedDep : moment.limitedDepartmentList) {
                sb.append("&sharerange[]="+limitedDep);
            }
            postStr += sb.toString();
        }

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
                    Element e = Utils.getFirstElementByTagName(resultElement, "moment_id");
                    if(e != null) {
                        // build a Moment object, and save it into db.

                        Database db = new Database(mContext);

                        if (moment == null) {
                            moment = new Moment();
                        }

                        String oldMomentId=moment.id;
                        moment.id = e.getTextContent();
                        moment.timestamp = Utils.getFirstLongByTagName(resultElement, "timestamp",
                                new Date().getTime() / 1000);
                        if (null == moment.owner) {
                            moment.owner = new Buddy();
                            moment.owner.userID = uid;
                        }
                        moment.likedByMe = false;

                        db.storeMoment(moment,oldMomentId);
                    }
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
    }

    /**
     * Add multi-media to a moment.
     *
     * Local db will NOT be updated.
     *
     * @param moment_id
     * @param WFile f
     * @return
     */
    public int fUploadMomentMultimedia(String moment_id, WFile f) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "upload_moment_multimedia";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&moment_id=" + Utils.urlencodeUtf8(moment_id)
                + "&multimedia_content_type=" + Utils.urlencodeUtf8(f.getExt())
                + "&multimedia_content_path=" + Utils.urlencodeUtf8(f.fileid)
                + "&multimedia_thumbnail_path=" + Utils.urlencodeUtf8(f.thumb_fileid)
                + "&duration=" + f.duration
                + "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
    }

    /**
     * Delete a Moment from web server and local db.
     *
     * @param moment_id
     * @return
     */
    public int fDeleteMoment(String moment_id) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if(uid == null || password == null || moment_id == null)
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "delete_moment";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&moment_id=" + Utils.urlencodeUtf8(moment_id)
                + "&lang=" + Locale.getDefault().getLanguage();
        int ret=_doRequestWithoutResponse(postStr);
        if(ErrorCode.OK == ret) {
            Database db = new Database(mContext);
            db.deleteMoment(moment_id);
        }
        return ret;
    }

    private int notifyServerMomentReaded(Moment m) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if(uid == null || password == null || m == null)
            return ErrorCode.INVALID_ARGUMENT;

        if (m.reviews == null || m.reviews.isEmpty())
            return ErrorCode.OK;

        final String action = "set_review_read";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password);

        for(Review r : m.reviews) {
            postStr += "&review_id_array[]=" + Utils.urlencodeUtf8(r.id);
        }

        return _doRequestWithoutResponse(postStr);
    }

    public int fSetReviewRead(Moment m) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
        if(uid == null || password == null || m == null)
            return ErrorCode.INVALID_ARGUMENT;

        if (m.reviews == null || m.reviews.isEmpty())
            return ErrorCode.OK;


        int retcode = notifyServerMomentReaded(m);
        if(ErrorCode.OK == retcode) {
            Database db = new Database(mContext);
            db.setReviewsRead(m);
        }

        return retcode;
//        final String action = "set_review_read";
//        String postStr = "action=" + action
//                + "&uid=" + Utils.urlencodeUtf8(uid)
//                + "&password=" + Utils.urlencodeUtf8(password);
//
//        for(Review r : m.reviews) {
//            postStr += "&review_id_array[]=" + Utils.urlencodeUtf8(r.id);
//        }


//        return _doRequestWithoutResponse(postStr);
    }

    public int fSetReviewRead(String review_id) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || review_id == null)
			return ErrorCode.INVALID_ARGUMENT;


        int retcode;
		final String action = "set_review_read";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&review_id=" + Utils.urlencodeUtf8(review_id)
				+ "&lang=" + Locale.getDefault().getLanguage();
        retcode= _doRequestWithoutResponse(postStr);
        if(ErrorCode.OK == retcode) {
            Database db = new Database(mContext);
            db.setSpecificReviewReaded(review_id);
        }
        return retcode;
    }

    /**
     * Review a Moment.
     *
     * Local db will be updated.
     *
     * @param momentId
     * @param type Review.TYPE_* constants
     * @param comment
     * @param replyToReviewId non-zero value means I'm replying a existed review.
     * @param result optional
     * @return
     */
    public int fReviewMoment(String momentId, int type, String comment, String replyToReviewId,
                             Review result) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || momentId == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "review_moment";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&moment_id=" + Utils.urlencodeUtf8(momentId)
				+ "&comment_type=" + type 
				+ "&comment=" + (comment == null ? "" : Utils.urlencodeUtf8(comment))
                + "&reply_to_review_id=" + (replyToReviewId == null ? "0" : replyToReviewId)
				+ "&lang=" + Locale.getDefault().getLanguage();

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno;
        if (root != null) {
            Element errorElement = Utils.getFirstElementByTagName(root, "err_no");
            errno = Utils.tryParseInt(errorElement.getTextContent(), ErrorCode.BAD_RESPONSE);
        } else {
            errno = ErrorCode.BAD_RESPONSE;
        }

        if (errno == ErrorCode.OK) {
            Element reviewNode = Utils.getFirstElementByTagName(root, "review");
            if(reviewNode != null) {
//                Database db = Database.open(mContext);
//                Review r = new Review();
                XmlHelper.parseReview(reviewNode, result);
                if (result.id != null) {
                    storeReview2db(momentId,result);
//                    db.storeReview(new Moment(momentId), r);
//                    if(Review.TYPE_LIKE == type) {
//                        db.updateMomentLikedAttr(momentId, true);
//                    }
                }
            }
        }
        return errno;
    }

    public void storeReview2db(String momentId,Review r) {
        Database db = Database.open(mContext);
        db.storeReview(new Moment(momentId), r);
        if(Review.TYPE_LIKE == r.type) {
            db.updateMomentLikedAttr(momentId, true);
        }
    }

    /**
     * Delete a Review of a Moment, from web server and local db.
     *
     * @param moment_id
     * @param review_id
     * @return
     */
    public int fDeleteMomentReview(String moment_id, Review review) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || moment_id == null || review.id == null)
			return ErrorCode.INVALID_ARGUMENT;


		final String action = "delete_moment_review";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&moment_id=" + Utils.urlencodeUtf8(moment_id) 
				+ "&review_id=" + Utils.urlencodeUtf8(review.id)
				+ "&lang=" + Locale.getDefault().getLanguage();
		int retcode= _doRequestWithoutResponse(postStr);
        if(ErrorCode.OK == retcode) {
            Database db = new Database(mContext);
            db.deleteMomentReview(moment_id, review.id);

            if(Review.TYPE_LIKE == review.type) {
                db.updateMomentLikedAttr(moment_id, false);
            }
        }
        return retcode;
    }

    /**
     * Get all Moments of a buddy, the result is saved in db.
     *
     *
     * @param owner_id
     * @param count
     * @param withReview do you also want reviews for each moment?
     * @param maxTimestamp can be 0.   @return
     */
    public int fGetMomentsOfBuddy(String owner_id, long maxTimestamp, int count, boolean withReview) {
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null || owner_id == null)
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "get_moment_for_buddy";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&owner_id=" + Utils.urlencodeUtf8(owner_id) 
				+ "&with_review=" + (withReview ? 1 : 0)
				+ "&lang=" + Locale.getDefault().getLanguage();
        if (maxTimestamp > 0) {
            postStr += "&max_timestamp=" + maxTimestamp;
        }
        if (count > 0) {
            postStr += "&count=" + count;
        }
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;
                Database db = new Database(mContext);

                List<Moment> momentIdFromServerList=new ArrayList<Moment>();

				Element resultElement = Utils.getFirstElementByTagName(root, action); 
				if(resultElement != null) {
					NodeList momentNodes = resultElement.getElementsByTagName("moment");
					if(momentNodes != null && momentNodes.getLength() > 0) {
						for(int i = 0, n = momentNodes.getLength(); i < n; ++i) {
							Element momentNode = (Element)momentNodes.item(i);
                            Moment b = XmlHelper.parseMoment(owner_id, momentNode,mContext,null);
                            if (b != null && b.id != null) {
                                momentIdFromServerList.add(b);
                            }
						}

                        for(Moment aMoment : momentIdFromServerList) {
                            db.storeMoment(aMoment,null);
                        }
					}
				}

                //clear moments not in server
                List<Moment> moments = db.fetchMomentsOfSingleBuddy(owner_id,0, -1,-1);
                deleteLocalMoment(maxTimestamp,count,moments,momentIdFromServerList);
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
    }

    /**
     * Get all reviews on a Moment.
     *
     * Automatically update db.
     *
     * @param moment
     * @param result
     * @return
     */
    public int fGetReviewForMoment(Moment moment, List<Review> result) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
    	if(uid == null || password == null || moment == null)
    		return ErrorCode.INVALID_ARGUMENT;

        String moment_id = moment.id;

        final String action = "get_reviews_on_moment";
    	String postStr = "action=" + action
    			+ "&uid=" + Utils.urlencodeUtf8(uid)
    			+ "&password=" + Utils.urlencodeUtf8(password)
    			+ "&moment_id=" + Utils.urlencodeUtf8(moment_id)
    			+ "&lang=" + Locale.getDefault().getLanguage();
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
					NodeList buddyNodeList = resultElement.getElementsByTagName("review");
					if(buddyNodeList != null && buddyNodeList.getLength() > 0) {

                        Database db = new Database(mContext);

						for(int i = 0, n = buddyNodeList.getLength(); i < n; ++i) {
							Review r = new Review();
                            XmlHelper.parseReview((Element)buddyNodeList.item(i), r);
                            if (r != null) {
                                result.add(r);
                                db.storeReview(moment, r);
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
     * Get unread reviews on me.
     *
     * Automatically update db.
     *
     * @param result
     * @return
     */
    public int fGetReviewsOnMe(List<Review> result) {
        String uid = mPrefUtil.getUid();
        String password = mPrefUtil.getPassword();
    	if(uid == null || password == null)
    		return ErrorCode.INVALID_ARGUMENT;

        final String action = "get_latest_reviews_for_me";
    	String postStr = "action=" + action
    			+ "&uid=" + Utils.urlencodeUtf8(uid) 
    			+ "&password=" + Utils.urlencodeUtf8(password) 
    			+ "&lang=" + Locale.getDefault().getLanguage();
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
					NodeList buddyNodeList = resultElement.getElementsByTagName("review");
					if(buddyNodeList != null && buddyNodeList.getLength() > 0) {

                        Database db = new Database(mContext);

                        Moment m = null;
                        Moment localMoment=null;
						for(int i = 0, n = buddyNodeList.getLength(); i < n; ++i) {
							Review r = new Review();
                            XmlHelper.parseReview((Element)buddyNodeList.item(i), r);
                            if (r != null) {
                                result.add(r);
                                if (m == null || !m.id.equals(r.hostId)) {
                                    m = new Moment(r.hostId);
                                    m.owner = new Buddy(uid);
                                    localMoment=db.fetchMoment(r.hostId);
                                }
                                db.storeReview(m, r);
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

    public int fGetMomentsOfGroup(String groupId,long maxTimestamp, int count, boolean withReview) {
        return doGetMoment("get_moments_for_group",maxTimestamp,count,withReview,groupId, 0);
    }

    public int fGetMomentsOfAll(long maxTimestamp, int count, boolean withReview) {
        return doGetMoment("get_moments_for_all_buddys",maxTimestamp,count,withReview,null, 0);
    }

    public int fGetMomentById(int momentId) {
        return doGetMoment("get_moment_by_id", 0, 0, false, null, momentId);
    }
    
    /**
     * Get the latest Moment of each buddy, the result is saved in db.
     *
     *
     * @param maxTimestamp can be 0.
     * @param count can be 0
     * @param momentId can be 0
     * @return
     */
    private int doGetMoment(String action, long maxTimestamp, int count, boolean withReview, String groupId, int momentId) {
    	int errno;
    	
		String uid = mPrefUtil.getUid();
		String password = mPrefUtil.getPassword();
		if(uid == null || password == null)
			return ErrorCode.INVALID_ARGUMENT;
		
//		final String action = "get_moments_of_all";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password)
                + "&with_review=" + (withReview ? 1 : 0)
				+ "&lang=" + Locale.getDefault().getLanguage();
        if (maxTimestamp > 0) {
            postStr += "&max_timestamp=" + maxTimestamp;
        }
        if (count > 0) {
            postStr += "&count=" + count;
        }
        if(!TextUtils.isEmpty(groupId)) {
            postStr += "&group_id=" + groupId;
        }
        if (momentId > 0) {
            postStr += "&moment_id=" + momentId;
        }
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		
		if(root == null) {
			return ErrorCode.BAD_RESPONSE;
		}
		
		NodeList errorList = root.getElementsByTagName("err_no");
		Element errorElement = (Element) errorList.item(0);
		String errorStr = errorElement.getFirstChild().getNodeValue();

		if (!errorStr.equals("0")) {
			return Integer.parseInt(errorStr);
		}
		
		errno = ErrorCode.OK;

        Database db = new Database(mContext);
        List<Moment> momentIdFromServerList=new ArrayList<Moment>();

		Element resultElement = Utils.getFirstElementByTagName(root, action); 
		if(resultElement != null) {
			NodeList momentNodes = resultElement.getElementsByTagName("moment");
			if(momentNodes != null && momentNodes.getLength() > 0) {
				for(int i = 0, n = momentNodes.getLength(); i < n; ++i) {
					Element momentNode = (Element)momentNodes.item(i);
					Moment b = XmlHelper.parseMoment(null, momentNode,mContext,null);
                    if (b.id != null) {
                        momentIdFromServerList.add(b);
                    }
                }

                //if moment not mine,need not notify user new review,so notify it readed here
                for(Moment aMoment : momentIdFromServerList) {
                    if(null == aMoment.owner.userID || !(aMoment.owner.userID.equals(uid))) {
                        boolean notMineReviewUnread=false;
                        for(Review r : aMoment.reviews) {
                            if(!r.read) {
                                notMineReviewUnread=true;
                                break;
                            }
                        }
                        if(notMineReviewUnread) {
                            Log.i("moment_web_server_if","moment not mine,set readed");
                            notifyServerMomentReaded(aMoment);
                            for(Review r : aMoment.reviews) {
                                r.read=true;
                            }
                        }
                    }
                }

                db.storeMoments(momentIdFromServerList);
            }
        }

        //clear moments not in server
        GroupChatRoom rootRoom=db.fetchRootGroupChatRoom();
        String rootRoomId="";
        if(null != rootRoom) {
            rootRoomId=rootRoom.groupID;
        }
        if(TextUtils.isEmpty(groupId) || groupId.equals(rootRoomId)) {
            List<Moment> moments = db.fetchMomentsOfAllBuddies(0, -1,-1);
            deleteLocalMoment(maxTimestamp,count,moments,momentIdFromServerList);
        }

		return errno;
    }

    private void deleteLocalMoment(long maxTimestamp,int count,List<Moment> localMomentList,List<Moment> momentFromServerList) {
        long maxTimeStampCheck=Long.MIN_VALUE;
        long minTimeStampCheck=Long.MAX_VALUE;
        if(null==localMomentList || null == momentFromServerList) {
            return;
        }

        List <String> momentInServerList=new ArrayList<String>();
        long maxServerMomentTimestamp=-1;
        for(Moment aMoment : momentFromServerList) {
            if(aMoment.timestamp > maxServerMomentTimestamp) {
                maxServerMomentTimestamp=aMoment.timestamp;
            }
        }

        if(maxTimestamp > 0) {
            for(Moment aMoment : momentFromServerList) {
                if(aMoment.timestamp > maxTimeStampCheck) {
                    maxTimeStampCheck=aMoment.timestamp;
                }

                for(Moment aLocalMoment : localMomentList) {
                    if(aMoment.id.equals(aLocalMoment.id)) {
                        if(!momentInServerList.contains(aLocalMoment.id)) {
                            momentInServerList.add(aLocalMoment.id);
                        }
                        break;
                    } else if (isMomentNewThanServerAndMine(maxServerMomentTimestamp, aLocalMoment)) {
                        if(!momentInServerList.contains(aLocalMoment.id)) {
                            momentInServerList.add(aLocalMoment.id);
                        }
                    }
                }
            }

            if(maxTimeStampCheck < maxTimestamp) {
                maxTimeStampCheck=maxTimestamp;
            }

            minTimeStampCheck=getMinTimeStamp(count,momentFromServerList);
        } else {
            maxTimeStampCheck=Long.MAX_VALUE;

            for(Moment aMoment : momentFromServerList) {
                for(Moment aLocalMoment : localMomentList) {
                    if(aMoment.id.equals(aLocalMoment.id)) {
                        if(!momentInServerList.contains(aLocalMoment.id)) {
                            momentInServerList.add(aLocalMoment.id);
                        }
                        break;
                    } else if (isMomentNewThanServerAndMine(maxServerMomentTimestamp, aLocalMoment)) {
                        //new created by me
                        if(!momentInServerList.contains(aLocalMoment.id)) {
                            momentInServerList.add(aLocalMoment.id);
                        }
                    }
                }
            }

            minTimeStampCheck=getMinTimeStamp(count,momentFromServerList);
        }
        Log.i("moment_web_server_if","delete local moment: \nminTimeStampCheck="+minTimeStampCheck+
                "\nmaxTimeStampCheck="+maxTimeStampCheck);

        Database db = new Database(mContext);
        for(Moment aMoment : localMomentList) {
            if(!momentInServerList.contains(aMoment.id) &&
                    aMoment.timestamp>minTimeStampCheck &&
                    aMoment.timestamp<maxTimeStampCheck) {
                Log.w("moment_web_server_if","moment with id "+aMoment.id+" deleted,timestamp "+aMoment.timestamp+",maxTimestam from server "+maxServerMomentTimestamp);
                db.deleteMoment(aMoment.id);
            }
        }
    }

    private boolean isMomentNewThanServerAndMine(long maxServerTimestamp,Moment aMoment) {
        String uid = mPrefUtil.getUid();
        if(aMoment.timestamp > maxServerTimestamp && uid.equals(aMoment.owner.userID)) {
            return true;
        }

        return false;
    }

    private long getMinTimeStamp(int count,List<Moment> momentFromServerList) {
        long ret=Long.MAX_VALUE;

        if(momentFromServerList.size() < count) {
            ret=Long.MIN_VALUE;
        } else {
            for(Moment aMoment : momentFromServerList) {
                if(aMoment.timestamp < ret) {
                    ret=aMoment.timestamp;
                }
            }
        }

        return ret;
    }
}
