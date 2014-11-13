package org.wowtalk.api;

import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetManager;
import android.os.Build;
import android.text.TextUtils;
import org.w3c.dom.DOMException;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wowtalk.Log;

import java.io.*;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * <p>Interface for communicating with WowTalk Web Server.</p>
 * <p>See {@link WowEventWebServerIF}, {@link WowMomentWebServerIF} for more functions.</p>
 */
public class WowTalkWebServerIF {

    /** You can download a pseudo photo. */
	public static String PSEUDO_FILEID_PHOTO_LANDSCAPE1 = "3e799dac2f8f6ae7006e73dbed4eb547";
    /** You can download a pseudo photo. */
	public static String PSEUDO_FILEID_PHOTO_LANDSCAPE2 = "3b081feb6b487047f92ffe94bb841a5f";
    /** You can download a pseudo photo. */
	public static String PSEUDO_FILEID_PHOTO_LANDSCAPE3 = "b7555c746ea9dc89110edcb45ae58f6b";
    /** You can download a pseudo photo. */
	public static String PSEUDO_FILEID_PHOTO_LANDSCAPE4 = "de08fc2f781da37d59be9d1a285d1b03";
    /** You can download a pseudo photo. */
	public static String PSEUDO_FILEID_AUDIO = "cac4d5c8947eeaaada97993bf1e697ca";

    private static WowTalkWebServerIF instance;
    private static PrefUtil sPrefUtil;
//    private static SharedPreferences mPref;
    private Context mContext;

	public static final WowTalkWebServerIF getInstance(Context context) {
		if (instance == null) {
			instance = new WowTalkWebServerIF(context);
		}
		return instance;
	}

    private WowTalkWebServerIF(Context context) {
        mContext = context.getApplicationContext();
        sPrefUtil = PrefUtil.getInstance(context);
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

	public void fSetNetworkTimeout(int connectionTimeout,int SocketTimeout){
		Connect2.SetTimeout(connectionTimeout, SocketTimeout);
	}

    /**
     * 检查uid/pwd是否非空，true则uid或pwd为空，不能访问网络接口
     * @param uid
     * @param pwd
     * @return true, uid或pwd为空；false，都不空
     */
    private boolean isAuthEmpty(String uid, String pwd) {
        return (TextUtils.isEmpty(uid) || TextUtils.isEmpty(pwd));
    }

    private void clearLocalData() {
        sPrefUtil.clearLocalData();
        WowTalkVoipIF.getInstance(mContext).fStopWowTalkService();
    }

	/**
	 * Remove all the information related to current account from local
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fAccountDeactive() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fAccountDeactive: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=account_deactive&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
				+ Utils.urlencodeUtf8(strPwd);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;

				clearLocalData();

			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;

	}

	/**
	 * Require the access code 
	 *  access code will be set when isDemoMode set to true, and can be readed by fGetDemoAccessCode().
	 * 
	 * @param strUserName phone number. must be numeric. 
	 * @param strCountryCode
	 * @param strCarrierName
	 * @param isDemoMode
	 * @return 0: good result; -1: no reply; others: error code from server;
	 * @throws RuntimeException
	 *             when strUserName or strCountryCode is null
	 */
	public int fRequireAccessCode(String strUserName, String strCountryCode,
			String strCarrierName, boolean isDemoMode) {
		if (strUserName == null || strCountryCode == null) {
//			throw new RuntimeException(
//					"fRequireAccessCode: strUserName and strCountryCode cannot be null");
            return ErrorCode.INVALID_ARGUMENT;
		}

        sPrefUtil.setCountryCode(strCountryCode);
        sPrefUtil.setUserName(strUserName);

        int applyTimes = sPrefUtil.getApplyTimes();

		String postStr = "action=require_access_code&phone_number="
				+ Utils.urlencodeUtf8(strUserName) + "&demoaccount=" + isDemoMode + "&apply_times="
				+ applyTimes + "&lang=" + Locale.getDefault().getLanguage();

		if (strCountryCode.equals("+81")) {
			postStr += "&carrier=" + Utils.urlencodeUtf8(strCarrierName);
			Log.i(postStr);
		}

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {
			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				// server minor version要素のリストを取得
				NodeList minorVersionList = root.getElementsByTagName("minor");
				// server minor version要素を取得
				Element minorVersionElement = (Element) minorVersionList
						.item(0);
				// server minor version要素の最初の子ノード（テキストノード）の値を取得
				String strServerMinorVersion = minorVersionElement
						.getFirstChild().getNodeValue();

				NodeList nodelist = root.getElementsByTagName("isDemoMode");
				if (nodelist != null && nodelist.item(0) != null) {
					isDemoMode = true;
				}

				errno = 0;
				if (strServerMinorVersion.equals("0") || isDemoMode == true) {
					isDemoMode = true;
					// access code要素のリストを取得
					NodeList codeList = root
							.getElementsByTagName("access_code");
					// access code要素を取得
					Element codeElement = (Element) codeList.item(0);
					// access code要素の最初の子ノード（テキストノード）の値を取得
					String accessCode = codeElement.getFirstChild()
							.getNodeValue();

                    sPrefUtil.setDemoAccessCode(accessCode);

				}

                sPrefUtil.setApplyTimes(++applyTimes);

			} else {
				errno = Integer.parseInt(errorStr);
			}

		} else {
			errno = -1;
		}

		return errno;

	}

	/**
	 * Require the access code after calling IVR
	 * 
	 * @param strUserName
	 * @param strCountryCode
	 * @return 0: good result; -1: no reply; others: error code from server;
	 * @throws RuntimeException
	 *             when strUserName or strCountryCode is null
	 */
	public int fRequireAccessCodeAfterIVRCall(String strUserName, String strCountryCode) {
		if (strUserName == null || strCountryCode == null) {
//			throw new RuntimeException(
//					"fRequireAccessCodeAfterIVRCall: strUserName and strCountryCode cannot be null");
            return ErrorCode.INVALID_ARGUMENT;
		}

        sPrefUtil.setCountryCode(strCountryCode);
        sPrefUtil.setUserName(strUserName);


		String postStr = "action=require_ivr_access_code&phone_number="
				+ Utils.urlencodeUtf8(strUserName);



		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {
			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {


				errno = 0;


				// access code要素のリストを取得
				NodeList codeList = root
						.getElementsByTagName("access_code");
				// access code要素を取得
				Element codeElement = (Element) codeList.item(0);
				// access code要素の最初の子ノード（テキストノード）の値を取得
				String accessCode = codeElement.getFirstChild()
						.getNodeValue();

                sPrefUtil.setDemoAccessCode(accessCode);

			} else {
				errno = Integer.parseInt(errorStr);
			}

		} else {
			errno = -1;
		}

		return errno;

	}


	/**
	 * 
	 * Validate the access code 
	 * 
	 * @param strAccessCode
	 * @return 0: good result -1: no reply others: error code from server
	 * @throws RuntimeException
	 *             when it is called before user name and country code is
	 *             correctly set
	 */
	public int fValidateAccessCode(String strAccessCode, String strAppType) {
		String strUserName = sPrefUtil.getUserName();
		String strCountryCode = sPrefUtil.getMyCountryCode();

		if (strUserName.equals("") || strCountryCode.equals("")) {
//			throw new RuntimeException(
//					"fValidateAccessCode: User name and country code should be set before I am called");
            return ErrorCode.INVALID_ARGUMENT;
		}


		String postStr = "action=validate_access_code&phone_number="
				+ Utils.urlencodeUtf8(strUserName) + "&countrycode="
				+ Utils.urlencodeUtf8(strCountryCode) + "&access_code=" + Utils.urlencodeUtf8(strAccessCode);

		if(strAppType!=null){
			postStr += "&app_type=" + Utils.urlencodeUtf8(strAppType);
		}




		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				NodeList nodeList = root.getElementsByTagName("password");
				Element element = (Element) nodeList.item(0);
				String strPassword = element.getFirstChild().getNodeValue();

				nodeList = root.getElementsByTagName("uid");
				element = (Element) nodeList.item(0);
				String strUID = element.getFirstChild().getNodeValue();

                sPrefUtil.setPassword(strPassword);
                sPrefUtil.setUid(strUID);
                sPrefUtil.setSetupStep(2);
				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}
	
	/**
	 * Bind my account with a mobile phone number.
	 * @param mobile
	 * @param password
	 * @param isForceBound If the phone has been bound to the other account, force to bind will unbind it and bind to the new account.
	 * @return
	 */
	public int fBindMobile(String mobile, String password, boolean isForceBound) {
        String uid = sPrefUtil.getUid();
        if(isAuthEmpty(uid, password))
			return ErrorCode.INVALID_ARGUMENT;

		//TODO
//		if(!mobile.matches("(\\+86)?1[0-9]{10}"))
//			throw new IllegalArgumentException(
//					"Only China mobile phone number is acceptable, e.g. +8613912345678, 138987654321");

		final String action = "bind_phone_number";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid)
				+ "&plain_password=" + Utils.urlencodeUtf8(password) 
				+ "&force=" + Utils.urlencodeUtf8(isForceBound ? "1" : "0")
				+ "&phone_number=" + Utils.urlencodeUtf8(mobile) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
	}

	/**
	 * Bind my account with a email address.
	 * @param email
	 * @param password
	 * @param isForceBound If the phone has been bound to the other account, force to bind will unbind it and bind to the new account.
	 * @return
	 */
	public int fBindEmail(String email, String password, boolean isForceBound) {
        String uid = sPrefUtil.getUid();
        if(isAuthEmpty(uid, password))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "bind_email";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&plain_password=" + Utils.urlencodeUtf8(password) 
				+ "&force=" + Utils.urlencodeUtf8(isForceBound ? "1" : "0")
				+ "&email_address=" + Utils.urlencodeUtf8(email) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
	}

	public int fVerifyEmail(String email, String verificationCode) {
		return _verifyBinding(0, email, verificationCode);
	}

	public int fVerifyMobile(String phoneNumber, String verificationCode) {
		return _verifyBinding(1, phoneNumber, verificationCode);
	}

    public int sendEmergencyMsg(String crop_id,int emergency_status,String msg,double latitude,double longitude) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
            return ErrorCode.INVALID_ARGUMENT;

        String action="send_emergency_msg";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&corp_id=" + crop_id
                + "&emergency_status=" + emergency_status;
        if(!TextUtils.isEmpty(msg)) {
            postStr += "&message="+msg;
        }
        if(latitude != 0 && longitude != 0) {
            postStr += "&latitude="+latitude;
            postStr += "&longitude="+longitude;
        }
        return _doRequestWithoutResponse(postStr);
    }

	/**
	 * 
	 * @param type 0=email, 1=mobile
	 * @param phoneEmailValue
	 * @param verificationCode
	 * @return
	 */
	private int _verifyBinding(int type, String phoneEmailValue, String verificationCode) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
			return ErrorCode.INVALID_ARGUMENT;
		
		final String action = 0 == type ? "verify_email" : "verify_phone_number";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&access_code=" + Utils.urlencodeUtf8(verificationCode) 
				+ "&phone_email_value=" + Utils.urlencodeUtf8(phoneEmailValue)
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
	}

	public int fUnBindMobile(String password) {
        String uid = sPrefUtil.getUid();
        if(isAuthEmpty(uid, password)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        final String action = "unbind_phone_number";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&plain_password=" + Utils.urlencodeUtf8(password)
                + "&lang=" + Locale.getDefault().getLanguage();
        return _doRequestWithoutResponse(postStr);
    }

	public int fUnBindEmail(String password) {
        String uid = sPrefUtil.getUid();
        if(isAuthEmpty(uid, password)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        final String action = "unbind_email";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&plain_password=" + Utils.urlencodeUtf8(password)
                + "&lang=" + Locale.getDefault().getLanguage();
        return _doRequestWithoutResponse(postStr);
    }

	/**
	 * Query binding info.
	 * @param binds for outputting, String[2],
	 * binds[0] will be email(if exists),
	 * binds[1] will be mobile(if exists).
	 * @return
	 */
	public int fQueryBinds(String wowtalkId, String[] binds) {
		int errno = ErrorCode.UNKNOWN;

		final String action = "query_bindings";
		String postStr = "action=" + action
				+ "&wowtalk_id=" + Utils.urlencodeUtf8(wowtalkId) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		binds[0] = binds[1] = null;
		
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();
			errno = Integer.parseInt(errorStr);
			if (errno == 0) {
				Element resultElement = Utils.getFirstElementByTagName(root, action); 
				if(resultElement != null) {
					Element emailEle = Utils.getFirstElementByTagName(resultElement, "email_verified");
					if(emailEle != null && "1".equals(emailEle.getTextContent())) {
						emailEle = Utils.getFirstElementByTagName(resultElement, "email_address");
						binds[0] = emailEle.getTextContent();
					}
					Element mobileEle = Utils.getFirstElementByTagName(resultElement, "phone_verified");
					if(mobileEle != null && "1".equals(mobileEle.getTextContent())) {
						mobileEle = Utils.getFirstElementByTagName(resultElement, "phone_number");
						binds[1] = mobileEle.getTextContent();
					}
				}
			}
		}
		return errno;
	}
	
	/**
	 * Get binded stuff
	 * @param binds for outputting, String[2]
	 * binds[0] will be email
	 * binds[1] will be mobile
	 * @return
	 */
	public int fGetBindedStuff(String[] binds) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if (isAuthEmpty(uid, password))
			return ErrorCode.INVALID_ARGUMENT;
		int errno = ErrorCode.UNKNOWN;
		
		final String action = "get_binded_stuff";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid)
				+ "&password=" + Utils.urlencodeUtf8(password)
				+ "&lang=" + Locale.getDefault().getLanguage();
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		
		binds[0] = binds[1] = null;
		
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();
			errno = Integer.parseInt(errorStr);
			if (errno == 0) {
				Element resultElement = Utils.getFirstElementByTagName(root, action);
				if (resultElement != null) {
					Element emailElement = Utils.getFirstElementByTagName(resultElement, "email_verified");
					if (emailElement != null && "1".equals(emailElement.getTextContent())) {
						emailElement = Utils.getFirstElementByTagName(resultElement, "email_address");
						binds[0] = emailElement.getTextContent();
					}
					Element mobileElement = Utils.getFirstElementByTagName(resultElement, "phone_verified");
					if (mobileElement != null && "1".equals(mobileElement.getTextContent())) {
						mobileElement = Utils.getFirstElementByTagName(resultElement, "phone_number");
						binds[1] = mobileElement.getTextContent();
					}
				}
			}
		}
		return errno;
	}

	/**
	 * Retrieve my lost password.
	 * <p>
	 * The server will send me a verification code via email or SMS, then i
	 * fResetPassword with that code.
	 *  
	 * @param method 0=email, 1=mobile.
	 * @return ErrorCode.
	 */
	public int fRetrievePassword(String wowtalk_id, String destination) {
		final String action = "send_access_code_for_password";
		String postStr = "action=" + action
				+ "&wowtalk_id=" + Utils.urlencodeUtf8(wowtalk_id) 
				+ "&destination=" + Utils.urlencodeUtf8(destination)
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
	}

	/**
	 * Reset my login password.
	 * @param wowtalkId
	 * @param verificationCode
	 * @param newPassword
	 * @return ErrorCode. a possible value is VERIFICATION_CODE_ERROR.
	 */
	public int fResetPassword(String wowtalkId, String verificationCode, String newPassword) {
		final String action = "reset_password";
		String postStr = "action=" + action
				+ "&wowtalk_id=" + Utils.urlencodeUtf8(wowtalkId) 
				+ "&verified_code=" + Utils.urlencodeUtf8(verificationCode) 
				+ "&plain_password=" + Utils.urlencodeUtf8(newPassword) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
	}

	/**
	 * Remove a member from group chat room.
	 * 
	 * @param group_id
	 * @param member_id
	 * @return
	 */
	public int fGroupChat_RemoveMember(String group_id, String member_id) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "remove_group_member";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&group_id=" + Utils.urlencodeUtf8(group_id) 
				+ "&member_id=" + Utils.urlencodeUtf8(member_id) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
	}

    /**
     *
     * @param group_id
     * @param member_id
     * @param level GroupMember.LEVEL_*
     * @return
     */
    public int fGroupChat_SetMemberLevel(String group_id, String member_id, int level) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "set_group_member_level";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&group_id=" + Utils.urlencodeUtf8(group_id)
                + "&member_id=" + Utils.urlencodeUtf8(member_id)
                + "&level=" + level
                + "&lang=" + Locale.getDefault().getLanguage();
        return _doRequestWithoutResponse(postStr);
    }

    public int fGroupChat_SetFavorite(String groupId, boolean isFavorite) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
            return ErrorCode.INVALID_ARGUMENT;

        // TODO
        final String action = "set_group_member_level";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&group_id=" + Utils.urlencodeUtf8(groupId)
                + "&is_favorite=" + Utils.urlencodeUtf8(isFavorite ? "1" : "0")
//                + "&level=" + level
                + "&lang=" + Locale.getDefault().getLanguage();
        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = ErrorCode.OK;
                // 保存本地数据库
                Database dbHelper = new Database(mContext);
                dbHelper.updateGroupChatRoomFavorite(groupId, isFavorite);
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }
        return errno;
    }

	/**
	 * 用户申请加入组
	 * @param group_id
     * @param msg optional message to send to group admin.
	 * @return
	 */
	public int fGroupChat_AskForJoining(String group_id, String msg) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "ask_for_join_group";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&group_id=" + Utils.urlencodeUtf8(group_id);
        if (null != msg)
            postStr += "&msg=" + Utils.urlencodeUtf8(msg);
		return _doRequestWithoutResponse(postStr);
	}

    public int fGroupChat_Reject(String group_id, String buddy_id) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "reject_pending_members";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&group_id=" + Utils.urlencodeUtf8(group_id)
                + "&member_id=" + Utils.urlencodeUtf8(buddy_id);
        return _doRequestWithoutResponse(postStr);
    }


    /**
     * Get pending members, that is, those who have asked to join in.
     * @param group_id
     * @param result Buddy.level will be set to {@link Buddy#LEVEL_PENDING}.
     * @return {@link ErrorCode}
     */
    public int fGroupChat_GetPendingMembers(String group_id, List<GroupMember> result) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password) || TextUtils.isEmpty(group_id))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "list_group_pending_members";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&group_id=" + Utils.urlencodeUtf8(group_id) 
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
					NodeList buddyNodeList = resultElement.getElementsByTagName("buddy");
					if(buddyNodeList != null && buddyNodeList.getLength() > 0) {
						for(int i = 0, n = buddyNodeList.getLength(); i < n; ++i) {
                            Element buddyNode = (Element) buddyNodeList.item(i);
							GroupMember b = new GroupMember(null, group_id);
                            XmlHelper.parseGroupMember(buddyNode, b);
                            b.setLevel(GroupMember.LEVEL_PENDING);
                            b.setAccountType(Buddy.ACCOUNT_TYPE_STUDENT);
							result.add(b);
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
	 * 解散组
	 * @param uid
	 * @param password
	 * @param group_id
	 * @return
	 */
	public int fGroupChat_Disband(String group_id) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password) || TextUtils.isEmpty(group_id))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "disband_group";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&group_id=" + Utils.urlencodeUtf8(group_id) 
				+ "&lang=" + Locale.getDefault().getLanguage();
		return _doRequestWithoutResponse(postStr);
	}


    public int fGroupChat_GetByID(String groupID, GroupChatRoom result) {
        return fGroupChat_GetByAnyID(groupID, result);
    }

	/**
	 * Get group chat room by short group id. 
	 * @param short_group_id
	 * @param result must not be null.
	 * @return if not found, result.groupId == result.shortGroupID == null.
	 */
	public int fGroupChat_GetByShortID(String short_group_id, GroupChatRoom result) {
        return fGroupChat_GetByAnyID(short_group_id, result);
    }

    private int fGroupChat_GetByAnyID(String id, GroupChatRoom result) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password) || TextUtils.isEmpty(id))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "get_group_by_short_group_id"; 
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid)
                + "&id=" + Utils.urlencodeUtf8(id)
				+ "&password=" + Utils.urlencodeUtf8(password);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		
		result.groupID = result.shortGroupID = null;

		int errno = ErrorCode.BAD_RESPONSE;
		if (root != null) {
			Element errorElement = Utils.getFirstElementByTagName(root, "err_no");
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;

				Element resultElement = Utils.getFirstElementByTagName(root, action); 
				if(resultElement != null) {
					Element groupNode = Utils.getFirstElementByTagName(resultElement, "group");
					if(groupNode != null)
						XmlHelper.parseGroup(groupNode, result);
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
	}

    public int fGroupChat_Search(String q, List<GroupChatRoom> result) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
            return ErrorCode.NOT_LOGGED_IN;

        if(q == null)
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "search_group";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + "&q=" + Utils.urlencodeUtf8(q)
                + "&lang=" + Locale.getDefault().getLanguage();
        return _requestForGroups(action, postStr, result);
    }

	/**
	 * 名称模糊搜索组
	 * @param uid
	 * @param password
	 * @param group_name
	 * @param result old items will be preserved.
	 * @return
	 */
	public int fGroupChat_GetByName(String group_name, List<GroupChatRoom> result) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password) || TextUtils.isEmpty(group_name))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "get_group_by_name"; 
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&group_name=" + Utils.urlencodeUtf8(group_name) 
				+ "&lang=" + Locale.getDefault().getLanguage();
        return _requestForGroups(action, postStr, result);
	}

    /**
     * @param useMyLastLocation use my last location or the location specified by latitude and longitude params?
     * @param latitude ignored if not useMyLastLocation.
     * @param longitude ignored if not useMyLastLocation.
     * @param result
     * @return
     */
    public int fGroupChat_GetNearBy(boolean useMyLastLocation,
                                    double latitude, double longitude, List<GroupChatRoom> result) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "get_nearby_groups";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password);
        if(!useMyLastLocation) {
            postStr += "&latitude=" + latitude + "&longitude=" + longitude;
        }

        return _requestForGroups(action, postStr, result);
    }

    private int _requestForGroups(String action, String postStr, List<GroupChatRoom> result) {
        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {
            Element errorElement = Utils.getFirstElementByTagName(root, "err_no");
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = 0;

                Element resultElement = Utils.getFirstElementByTagName(root, action);
                if(resultElement != null) {
                    NodeList buddyNodeList = resultElement.getElementsByTagName("group");
                    if(buddyNodeList != null && buddyNodeList.getLength() > 0) {
                        for(int i = 0, n = buddyNodeList.getLength(); i < n; ++i) {
                            Element groupNode = (Element)buddyNodeList.item(i);
                            GroupChatRoom b = new GroupChatRoom();
                            XmlHelper.parseGroup(groupNode, b);
                            result.add(b);
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
	 * 精确搜索用户
	 * @param wowtalkID
	 * @param result
	 * @return ErrorCode.
     *
     * 如果搜索结果为空，result.userID 将被置为 null。
	 */
	public int fGetBuddyByWowtalkId(String wowtalkID, Buddy result) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password) || TextUtils.isEmpty(wowtalkID))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "get_buddy_by_wowtalk_id"; 
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid) 
				+ "&password=" + Utils.urlencodeUtf8(password) 
				+ "&wowtalk_id=" + Utils.urlencodeUtf8(wowtalkID) 
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
				
				result.wowtalkID = wowtalkID;

				Element resultElement = Utils.getFirstElementByTagName(root, action); 
				if(resultElement != null) {
					Element buddyElement = Utils.getFirstElementByTagName(resultElement, "buddy");
                    if (null != buddyElement) {
                        XmlHelper.parseBuddy(buddyElement, result);
                        Database dbHelper = new Database(mContext);
                        dbHelper.storeNewBuddyDetailWithUpdate(result);
                    } else {
                        result.userID = null;
                    }
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno;
	}

    /**
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param minute
	 * @param second
	 * @param tz e.g., TimeZone.getTimeZone("GMT+08:00"), TimeZone.getTimeZone("GMT")
	 * @return
	 */
	public static Date createDate(int year, int month, int day, int hour, int minute, int second, TimeZone tz) {
		Calendar cal = new GregorianCalendar(tz);
		cal.set(year + 1900, month, day, hour, minute, second);
		return cal.getTime();
	}

	/**
	 * call createDate().
	 * 
	 * @param year
	 * @param month
	 * @param day
	 * @param hour
	 * @param minute
	 * @param second
	 * @return
	 */
	public static Date createGMTDate(int year, int month, int day, int hour, int minute, int second) {
		return createDate(year, month, day, hour, minute, second, TimeZone.getTimeZone("GMT"));
	}

	/**
	 * create a new user with given wowtalk_id and password return the uid,domain,hashed password set.
	 * 
	 * @param wowtalk_id
	 * @param password
     * @param userType refer to {@link Buddy#ACCOUNT_TYPE_STUDENT}.
	 * @return error no.
	 * @see
	 * <ul>
	 * <li>fGetMy*() get my info;</li>
	 * <li>fUpdateMyProfile() set my info;</li>
	 * <li>fPostMyPhoto(), fPostMyThumbnail() set my avatar；</li>
	 * <li>fGetPhotoForUserID() get my avatar；</li>
	 * </ul>
	 */
	public int fRegister(String wowtalk_id, String password, int userType, Buddy result) {
        if(isAuthEmpty(wowtalk_id, password))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "register";
		String postStr = "action=" + action
				+ "&user=" + Utils.urlencodeUtf8(wowtalk_id)
				+ "&plain_password=" + Utils.urlencodeUtf8(password)
                + "&user_type=" + userType
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

				if(result != null) {
                    clearLocalData();
                    result.wowtalkID = wowtalk_id;

                    sPrefUtil.setWowtalkIdChanged(true);
                    sPrefUtil.setPasswordChanged(true);

					Element bodyElement = Utils.getFirstElementByTagName(root, "body");
					Element registerElement = Utils.getFirstElementByTagName(bodyElement, action);
					Element uidElement = Utils.getFirstElementByTagName(registerElement, "uid");
					Element domainElement = Utils.getFirstElementByTagName(registerElement, "domain");
					Element passwordElement = Utils.getFirstElementByTagName(registerElement, "password");
                    Element sipPasswordElement = Utils.getFirstElementByTagName(registerElement, "sip_password");

					if(uidElement != null) {
						result.userID = uidElement.getTextContent();
						sPrefUtil.setUid(result.userID);
					}
					if(domainElement != null) {
						result.domain = domainElement.getTextContent();
					}
					if(passwordElement != null) {
						result.hashedPassword = passwordElement.getTextContent();
						sPrefUtil.setPassword(result.hashedPassword);
					}
                    if(sipPasswordElement != null) {
                        sPrefUtil.setSipPassword(sipPasswordElement.getTextContent());
                    }

                    sPrefUtil.setWowtalkId(wowtalk_id);
                    sPrefUtil.setSetupStep(2);
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}

		return errno;
	}

	/**
	 * @param username wowtalk_id/phonenumber/email_address,password
	 * @param password
	 * @param result optional, output uid,domain,password
	 * @return 0:OK, 1: failed
	 */
	public int fLogin(String username, String password,
			Buddy result) {
        if(isAuthEmpty(username, password))
			return ErrorCode.INVALID_ARGUMENT;

		final String action = "login";
		String postStr = "action=" + action 
				+ "&user=" + Utils.urlencodeUtf8(username) 
				+ "&plain_password=" + Utils.urlencodeUtf8(password) 
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

				if(result != null) {
                    configS3();

                    sPrefUtil.setWowtalkIdChanged(true);
                    sPrefUtil.setPasswordChanged(true);

					Element bodyElement = Utils.getFirstElementByTagName(root, "body");
					Element registerElement = Utils.getFirstElementByTagName(bodyElement, action);
					Element uidElement = Utils.getFirstElementByTagName(registerElement, "uid");
					Element domainElement = Utils.getFirstElementByTagName(registerElement, "domain");
					Element passwordElement = Utils.getFirstElementByTagName(registerElement, "password");
                    Element sipPasswordElement = Utils.getFirstElementByTagName(registerElement, "sip_password");

					if(uidElement != null) {
						result.userID = uidElement.getTextContent();
						String prevUID = sPrefUtil.getPrevUid();
						if(!TextUtils.isEmpty(prevUID) && !prevUID.equals(result.userID)) {
							clearLocalData();
						}
						sPrefUtil.setPrevUid(result.userID);
						sPrefUtil.setUid(result.userID);
					}
					if(domainElement != null) {
						result.domain = domainElement.getTextContent();
					}
					if(passwordElement != null) {
						result.hashedPassword = passwordElement.getTextContent();
						sPrefUtil.setPassword(result.hashedPassword);
					}
                    if(sipPasswordElement != null) {
                        sPrefUtil.setSipPassword(sipPasswordElement.getTextContent());
                    }

                    sPrefUtil.setSetupStep(2);
				}
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}

		return errno;
	}

    /**
     * 第一次从中央服务器登录，获取地方服务器的domain，以后与服务器的连接都使用地方服务器domain
     * @param company 公司id(登录界面输入的值)
     * @param username 用户id(登录界面输入的值)
     * @param password 用户密码(登录界面输入的值)
     * @param result Buddy信息
     * @return
     */
    public int loginForBiz(String company, String username, String password, Buddy result) {
        if(TextUtils.isEmpty(company) || isAuthEmpty(username, password))
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "biz_login";
        String postStr = "action=" + action
                + "&admin_id=" + Utils.urlencodeUtf8(company)
                + "&user=" + Utils.urlencodeUtf8(username)
                + "&plain_password=" + Utils.urlencodeUtf8(password);
        return parseLoginXml(postStr, "login", result);
    }

    public int loginByHashedPwdForBiz(String company, String uid, String hashedPwd, Buddy result) {
        if(TextUtils.isEmpty(company) || isAuthEmpty(uid, hashedPwd))
            return ErrorCode.INVALID_ARGUMENT;

        final String action = "biz_login_by_hashpassword";
        String postStr = "action=" + action
                + "&admin_id=" + Utils.urlencodeUtf8(company)
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(hashedPwd);
        return parseLoginXml(postStr, "login_by_hashpassword", result);
    }

    private int parseLoginXml(String postStr, String actionElementName, Buddy result) {
        Connect2 connect2 = new Connect2(true);
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = 0;

                if(result != null) {
                    sPrefUtil.setWowtalkIdChanged(true);
                    sPrefUtil.setPasswordChanged(true);

                    Element bodyElement = Utils.getFirstElementByTagName(root, "body");
                    Element loginElement = Utils.getFirstElementByTagName(bodyElement, actionElementName);

                    Element uidElement = Utils.getFirstElementByTagName(loginElement, "uid");
                    Element pwdElement = Utils.getFirstElementByTagName(loginElement, "password");
                    Element pwdChangedElement = Utils.getFirstElementByTagName(loginElement, "password_changed");
                    Element wowtalkIdChangedElement = Utils.getFirstElementByTagName(loginElement, "wowtalk_id_changed");
                    Element emailElement = Utils.getFirstElementByTagName(loginElement, "email_address");
                    Element webDomainElement = Utils.getFirstElementByTagName(loginElement, "web_domain");
                    Element sipDomainElement = Utils.getFirstElementByTagName(loginElement, "sip_domain");
                    Element sipPasswordElement = Utils.getFirstElementByTagName(loginElement, "sip_password");
                    Element isUseS3Element = Utils.getFirstElementByTagName(loginElement, "useS3");
                    Element s3UidElement = Utils.getFirstElementByTagName(loginElement, "S3uid");
                    Element s3PwdElement = Utils.getFirstElementByTagName(loginElement, "S3pwd");
                    Element s3BucketElement = Utils.getFirstElementByTagName(loginElement, "S3bucket");

                    if(null != uidElement) {
                        result.userID = uidElement.getTextContent();
                        String prevUID = sPrefUtil.getPrevUid();
                        if(!TextUtils.isEmpty(prevUID) && !prevUID.equals(result.userID)) {
                            clearLocalData();
                        }
                        sPrefUtil.setPrevUid(result.userID);
                        sPrefUtil.setUid(result.userID);
                    }

                    if(null != pwdElement) {
                        result.hashedPassword = pwdElement.getTextContent();
                        sPrefUtil.setPassword(result.hashedPassword);
                    }
                    if(null != pwdChangedElement) {
                        sPrefUtil.setPasswordChanged("1".equals(pwdChangedElement.getTextContent()));
                    }
                    if(null != wowtalkIdChangedElement) {
                        sPrefUtil.setWowtalkIdChanged("1".equals(wowtalkIdChangedElement.getTextContent()));
                    }
                    if(null != emailElement) {
                        result.setEmail(emailElement.getTextContent());
                        sPrefUtil.setMyEmail(result.getEmail());
                    }
                    if(null != webDomainElement) {
                        sPrefUtil.setWebDomain(webDomainElement.getTextContent());
                    }
                    if(null != sipDomainElement) {
                        sPrefUtil.setSipDomain(sipDomainElement.getTextContent());
                    }
                    if (null != sipPasswordElement) {
                        sPrefUtil.setSipPassword(sipPasswordElement.getTextContent());
                    }
                    if (null !=isUseS3Element) {
                        String useS3 = isUseS3Element.getTextContent();
                        boolean isUseS3 = "1".equals(useS3);
                        sPrefUtil.setUseS3(isUseS3);
                        if (isUseS3) {
                            if (null != s3UidElement) {
                                sPrefUtil.setS3Uid(s3UidElement.getTextContent());
                            }
                            if (null != s3PwdElement) {
                                sPrefUtil.setS3Pwd(s3PwdElement.getTextContent());
                            }
                            if (null != s3BucketElement) {
                                sPrefUtil.setS3Bucket(s3BucketElement.getTextContent());
                            }
                        }
                    }

                    sPrefUtil.setSetupStep(2);
                }
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }
        return errno;
    }

    private void configS3() {
        sPrefUtil.setUseS3(GlobalSetting.USE_S3);
        if (GlobalSetting.USE_S3) {
            sPrefUtil.setS3Uid(GlobalSetting.S3_ACCESS_KEY_ID);
            sPrefUtil.setS3Pwd(GlobalSetting.S3_SECRET_KEY);
            sPrefUtil.setS3Bucket(GlobalSetting.S3_BUCKET);
        }
    }

    /**
	 * @param result Buddy, output uid,domain,password,wowtalkId
	 * @return
	 */
	public int fLoginWithAutoCreatedUser(Buddy result) {
		final String action = "login_with_auto_create_user";
		String postStr = "action=" + action;
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = -1;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();
			if (errorStr.equals("0")) {
				clearLocalData();
                configS3();

                sPrefUtil.setWowtalkIdChanged(false);
                sPrefUtil.setPasswordChanged(false);

				Element bodyElement = Utils.getFirstElementByTagName(root, "body");
				Element registerElement = Utils.getFirstElementByTagName(bodyElement, action);
				Element uidElement = Utils.getFirstElementByTagName(registerElement, "uid");
				Element domainElement = Utils.getFirstElementByTagName(registerElement, "domain");
				Element passwordElement = Utils.getFirstElementByTagName(registerElement, "password");
                Element sipPasswordElement = Utils.getFirstElementByTagName(registerElement, "sip_password");
				Element plainPasswordElement = Utils.getFirstElementByTagName(registerElement, "plain_password");
				Element wowtalkidElement = Utils.getFirstElementByTagName(registerElement, "wowtalk_id");

				if(uidElement != null) {
					result.userID = uidElement.getTextContent();
                    sPrefUtil.setPrevUid(result.userID);
                    sPrefUtil.setUid(result.userID);
				}
				if(domainElement != null) {
					result.domain = domainElement.getTextContent();
				}
				if(passwordElement != null) {
					result.hashedPassword = passwordElement.getTextContent();
                    sPrefUtil.setPassword(result.hashedPassword);
				}
                if(sipPasswordElement != null) {
                    sPrefUtil.setSipPassword(sipPasswordElement.getTextContent());
                }
				if(plainPasswordElement != null) {
					result.plainPassword = plainPasswordElement.getTextContent();
				}
				if(wowtalkidElement != null) {
					result.wowtalkID = wowtalkidElement.getTextContent();
				}
				
				Element e = null;
				
				e = Utils.getFirstElementByTagName(registerElement, "wowtalk_id_changed");
				if(e != null) {
                    sPrefUtil.setWowtalkIdChanged("1".equals(e.getTextContent()));
				}
				
				e = Utils.getFirstElementByTagName(registerElement, "password_changed");
				if(e != null) {
                    sPrefUtil.setPasswordChanged("1".equals(e.getTextContent()));
				}

                sPrefUtil.setSetupStep(2);

				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno; 
	}

    public int fLogout() {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();

        return logoutByUid(uid, password);
    }

    public int logoutByUid(String uid, String hashedPwd) {
        Log.i("WowTalkWebServerIF.fLogout()");
        if(isAuthEmpty(uid, hashedPwd)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        final String action = "logout";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(hashedPwd);
        return _doRequestWithoutResponse(postStr);
    }

    /**
     * 获取多帐号未读的信息条数
     * @param accountIds 所有的帐号id，可以包含当前帐号（会在此方法实现中排除）
     * @param resultMap
     * @return
     */
    public int getAccountUnreadCounts(ArrayList<String> accountIds, HashMap<String, Integer> resultMap) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();

        if (isAuthEmpty(uid, password)
                || null == accountIds || accountIds.isEmpty()
                || null == resultMap) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String accountIdString = "";
        for (String accountId : accountIds) {
            if (!uid.equals(accountId)) {
                accountIdString += "&account_id[]=" + Utils.urlencodeUtf8(accountId);
            }
        }

        String action = "get_accounts_unread_counts";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(uid)
                + "&password=" + Utils.urlencodeUtf8(password)
                + accountIdString;

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {

            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if ("0".equals(errorStr)) {
                errno = 0;
                NodeList unreadList = root.getElementsByTagName("unread_set");

                Element tempElement = null;
                Element accountIdElement = null;
                Element unreadCountElement = null;
                String accountId = null;
                int unreadCount = 0;
                for (int i = 0; i < unreadList.getLength(); i++) {
                    tempElement = (Element) unreadList.item(i);
                    accountIdElement = Utils.getFirstElementByTagName(
                            tempElement, "account_id");
                    if(null != accountIdElement) {
                        unreadCountElement = Utils.getFirstElementByTagName(
                                tempElement, "unread_count");
                        if (null != unreadCountElement) {
                            accountId = accountIdElement.getTextContent();
                            unreadCount = Integer.parseInt(unreadCountElement.getTextContent());
                            resultMap.put(accountId, unreadCount);
                        }
                    }
                }
            } else {
                errno = Integer.parseInt(errorStr);
            }
        } else {
            errno = -1;
        }
        return errno;
    }

	public int fChangeWowtalkId(String wowtalkId) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password) || Utils.isNullOrEmpty(wowtalkId))
			return ErrorCode.INVALID_ARGUMENT;
		
		final String action = "change_wowtalk_id";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid)
				+ "&password=" + Utils.urlencodeUtf8(password)
				+ "&wowtalk_id=" + Utils.urlencodeUtf8(wowtalkId.trim());
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = -1;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();
			if (errorStr.equals("0")) {
				errno = 0;

                sPrefUtil.setWowtalkId(wowtalkId);
                sPrefUtil.setWowtalkIdChanged(true);
            } else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno; 
	}

	public int fChangePassword(String newPlainPassword, String oldPlainPassword) {
        String uid = sPrefUtil.getUid();
        String password = sPrefUtil.getPassword();
        if(isAuthEmpty(uid, password))
			return ErrorCode.INVALID_ARGUMENT;
		
		final String action = "change_password";
		String postStr = "action=" + action
				+ "&uid=" + Utils.urlencodeUtf8(uid)
				+ "&password=" + Utils.urlencodeUtf8(password)
				+ "&new_plain_password=" + Utils.urlencodeUtf8(newPlainPassword);
        if (oldPlainPassword != null)
            postStr += "&old_plain_password=" + Utils.urlencodeUtf8(oldPlainPassword);
		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = -1;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();
			if (errorStr.equals("0")) {
				errno = 0;

                sPrefUtil.setPasswordChanged(true);
                sPrefUtil.setPassword(fEncryptPassword(newPlainPassword));
			} else {
				errno = Integer.parseInt(errorStr);
			}
		}
		return errno; 
	}

    /**
     * Report push sercie token to server (TODO: to add google account support)
     *
     * @return 0: good result -1: no reply others: error code from server
     */
    public int fReportInfoWithPushToken() {
        // 每次版本升级之后，需要上报服务器
        if (!sPrefUtil.isAppUpgraded() && sPrefUtil.isPushTokenReportedToServer()) {
            Log.w("fReportInfoWithPushToken called:",
                    "already reported,do nothing");
            return 0;
        }

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        int errno = 0;
        // TODO: need to implement push service here
        String postStr = "action=report_info"
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd)
                + "&device_number=" + Utils.urlencodeUtf8(Build.DEVICE)
                + "&app_ver=" + sPrefUtil.getAppVersion()
                + "&language=" + Locale.getDefault().getDisplayLanguage()
                + "&device_type=android"
                + "&push_token=not_implemented";

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		if (root != null) {
			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				sPrefUtil.setPushTokenReportedToServer(true);
			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}

		return errno;

	}

	/**
	 * Get the latest version of server soft and android client 
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fGetLatestVersionInfo() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGetLatestVersionInfo: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_latest_version_info&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
				+ Utils.urlencodeUtf8(strPwd);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				NodeList nodeList = root.getElementsByTagName("server_version");
				Element element = (Element) nodeList.item(0);
				String strServerVersion = "1000";
				if (element != null && element.getFirstChild() != null) {
					strServerVersion = element.getFirstChild().getNodeValue();
				}

				nodeList = root.getElementsByTagName("android_client_version");
				element = (Element) nodeList.item(0);
				String strAndroidClientVersion = "1000";
				if (element != null && element.getFirstChild() != null) {
					strAndroidClientVersion = element.getFirstChild().getNodeValue();
				}
				sPrefUtil.setServerVersion(Integer.parseInt(strServerVersion));
				sPrefUtil.setClientVersion(Integer.parseInt(strAndroidClientVersion));



				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Fetch and save to local for my nickname, status, birthday, sex, area.
	 * @return 0: good result -1: no reply others: error code from server
	 * @throws RuntimeException
	 *             when it is called before user name is correctly set
	 */
    public int fGetMyProfile() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGetMyProfile: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_my_profile&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
				+ Utils.urlencodeUtf8(strPwd);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
                Buddy me = new Buddy(strUID);
                XmlHelper.parseBuddy(root, me);

                Element element = Utils.getFirstElementByTagName(root, "birthday");
				String strBirthday = "";
				if (element != null && element.getFirstChild() != null) {
					strBirthday = element.getTextContent();
				}

                // save my info in pref
				Editor editor = sPrefUtil.getPreferences().edit();
				editor.putString(PrefUtil.MY_NICKNAME, me.nickName);
				editor.putString(PrefUtil.MY_STATUS, me.status);
				editor.putString(PrefUtil.MY_BIRTHDAY, strBirthday);
				editor.putInt(PrefUtil.MY_SEX, me.getSexFlag());
				editor.putString(PrefUtil.MY_AREA, me.area);
				editor.putLong(PrefUtil.MY_PHOTO_UPLOADED_TIMESTAMP, me.photoUploadedTimeStamp);
				editor.putString(PrefUtil.WOWTALK_ID, me.wowtalkID);
				editor.putString(PrefUtil.MY_PRONUNCIATION, me.pronunciation);
				editor.putString(PrefUtil.MY_PHONE, me.phoneNumber);
				editor.putString(PrefUtil.MY_MOBILE, me.mobile);
				editor.putString(PrefUtil.MY_EMAIL, me.getEmail());
				editor.putString(PrefUtil.MY_JOB_TITLE, me.jobTitle);
				editor.putString(PrefUtil.MY_EMPLOYEE_ID, me.employeeId);
                editor.putInt(PrefUtil.MY_ACCOUNT_TYPE, me.getAccountType());

				editor.commit();

                Database db = new Database(mContext);
                me.setFriendshipWithMe(Buddy.RELATIONSHIP_SELF);
                db.storeNewBuddyWithUpdate(me);
//                db.storeNewBuddyDetailWithUpdate(me);

				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Post nickname,status,birthday,sex,area to server 
	 * 
	 * @param strBirthday yyyy-MM-dd
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fUpdateMyProfile(String strNickName, String strStatus,
			String strBirthday, int sexFlag, String strArea) {
		return fUpdateMyProfile(strNickName, strStatus, strBirthday,
				sexFlag, strArea, null, null);
	}
	
	/**
	 * Post nickname,status,birthday,sex,area to server 
	 * 
	 * @param strNickName
	 * @param strStatus
	 * @param strBirthday yyyy-MM-dd
	 * @param sexFlag
	 * @param strArea
	 * @param photoFilePath call fPostMyPhoto()
	 * @param thumbnailFilePath call fPostMyThumbnail()
	 * @return
	 */
	public int fUpdateMyProfile(String strNickName, String strStatus,
			String strBirthday, int sexFlag, String strArea,
			final String photoFilePath, final String thumbnailFilePath) {

        final String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fUpdateMyProfileWithNickName: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}
		
		if(photoFilePath != null) {
			fPostMyPhoto(photoFilePath, new NetworkIFDelegate() {

				@Override
				public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
				}

				@Override
				public void didFinishNetworkIFCommunication(int arg0,
						byte[] arg1) {
				}

				@Override
				public void setProgress(int arg0, int arg1) {
				}
				
			}, 0);
		}
		
		if(thumbnailFilePath != null) {
			fPostMyThumbnail(thumbnailFilePath, new NetworkIFDelegate() {

				@Override
				public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
				}

				@Override
				public void didFinishNetworkIFCommunication(int arg0,
						byte[] arg1) {
				}

				@Override
				public void setProgress(int arg0, int arg1) {
				}
				
			}, 0);
		}

		String postStr = "action=update_my_profile&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd);
        // TODO 此次的Editor没有commit，是否应该删掉
		Editor editor = sPrefUtil.getPreferences().edit();

		if (strNickName != null) {
			editor.putString(PrefUtil.MY_NICKNAME, strNickName);
			postStr += "&nickname=" + Utils.urlencodeUtf8(strNickName);
		}
		if (strStatus != null) {
			editor.putString(PrefUtil.MY_STATUS, strStatus);
			postStr += "&last_status=" + Utils.urlencodeUtf8(strStatus);
		}
		if (strBirthday != null) {
			editor.putString(PrefUtil.MY_BIRTHDAY, strBirthday);
			postStr += "&birthday=" + Utils.urlencodeUtf8(strBirthday);
		}

		editor.putInt(PrefUtil.MY_SEX, sexFlag);
		postStr += "&sex=" + sexFlag;

		if (strArea != null) {
			editor.putString(PrefUtil.MY_AREA, strArea);
			postStr += "&area=" + Utils.urlencodeUtf8(strArea);
		}
		int errno = _doRequestWithoutResponse(postStr);

        if (errno == ErrorCode.OK) {
            // refresh my info in local db
            new Thread(new Runnable() {
                @Override
                public void run() {
                    fGetMyProfile();
                }
            }).start();
        }

        return errno;
	}
	
	/**
	 * Update my profile, including:
	 * <ul>
	 * <li>nick name</li>
	 * <li>birthday</li>
	 * <li>sex</li>
	 * <li>area</li>
	 * <li>status</li>
	 * <li>photo</li>
	 * <li>last spot(location & time)</li>
	 * </ul>
	 * @param data
	 * @param whichValid which fields are valid in the data?
	 * @return error code.
	 */
	public int fUpdateMyProfile(final Buddy data, int whichValid) {

        final String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
		int errno = ErrorCode.OK;
		Connect2 connect2 = null;

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fUpdateMyProfileWithNickName: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}
		
		if(0 != (Buddy.FIELD_FLAG_PHOTO & whichValid)) {
			fPostMyPhoto(data.pathOfPhoto, new NetworkIFDelegate() {

				@Override
				public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
				}

				@Override
				public void didFinishNetworkIFCommunication(int arg0,
						byte[] arg1) {
				}

				@Override
				public void setProgress(int arg0, int arg1) {
				}
				
			}, 0);
			
			fPostMyThumbnail(data.pathOfThumbNail, new NetworkIFDelegate() {

				@Override
				public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
				}

				@Override
				public void didFinishNetworkIFCommunication(int arg0,
						byte[] arg1) {
				}

				@Override
				public void setProgress(int arg0, int arg1) {
				}
				
			}, 0);
		}
		
		/*
		 * request action=user_become_active
		 */
		if(0 != (Buddy.FIELD_FLAG_SPOT & whichValid)) {
			String postStr = "action=user_become_active&uid=" + Utils.urlencodeUtf8(strUID)
					+ "&password=" + Utils.urlencodeUtf8(strPwd)
					+ "&last_latitude=" + data.lastLocation.latitude
					+ "&last_longitude=" + data.lastLocation.longitude;

			if(connect2 == null)
				connect2 = new Connect2();
			Element root = connect2.Post(postStr);

			if (root != null) {

				// err_no要素のリストを取得
				NodeList errorList = root.getElementsByTagName("err_no");
				// error要素を取得
				Element errorElement = (Element) errorList.item(0);
				// error要素の最初の子ノード（テキストノード）の値を取得
				String errorStr = errorElement.getFirstChild().getNodeValue();

				if (errorStr.equals("0")) {
					errno = 0;

				} else {
					errno = Integer.parseInt(errorStr);
				}
			} else {
				errno = -1;
			}
			if(errno != ErrorCode.OK)
				return errno;
		}
		
		/*
		 * request action=update_my_profile
		 */
		String postStr = "action=update_my_profile&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd);
		Editor editor = sPrefUtil.getPreferences().edit();
		int cnt = 0;

		if(0 != (Buddy.FIELD_FLAG_NICK & whichValid)) {
			editor.putString(PrefUtil.MY_NICKNAME, data.nickName);
			postStr += "&nickname=" + Utils.urlencodeUtf8(data.nickName);
			++cnt;
		}
		if(0 != (Buddy.FIELD_FLAG_STATUS & whichValid)) {
			editor.putString(PrefUtil.MY_STATUS, data.status);
			postStr += "&last_status=" + Utils.urlencodeUtf8(data.status);
			++cnt;
		}
		if(0 != (Buddy.FIELD_FLAG_BIRTHDAY & whichValid)) {
			editor.putString(PrefUtil.MY_BIRTHDAY, new SimpleDateFormat("yyyy-MM-dd").format(data.getBirthday()));
			postStr += "&birthday=" + Utils.urlencodeUtf8(new SimpleDateFormat("yyyy-MM-dd").format(data.getBirthday()));
			++cnt;
		}
		if(0 != (Buddy.FIELD_FLAG_SEX & whichValid)) {
			editor.putInt(PrefUtil.MY_SEX, data.getSexFlag());
			postStr += "&sex=" + data.getSexFlag();
			++cnt;
		}
		if(0 != (Buddy.FIELD_FLAG_AREA & whichValid)) {
			editor.putString(PrefUtil.MY_AREA, data.area);
			postStr += "&area=" + Utils.urlencodeUtf8(data.area);
			++cnt;
		}
        if(0 != (Buddy.FIELD_FLAG_PRONUNCIATION & whichValid)) {
            editor.putString(PrefUtil.MY_PRONUNCIATION, data.pronunciation);
            postStr += "&pronunciation=" + Utils.urlencodeUtf8(data.pronunciation);
            ++cnt;
        }
        if(0 != (Buddy.FIELD_FLAG_PHONE & whichValid)) {
            editor.putString(PrefUtil.MY_PHONE, data.phoneNumber);
            postStr += "&interphone=" + Utils.urlencodeUtf8(data.phoneNumber);
            ++cnt;
        }
        if(0 != (Buddy.FIELD_FLAG_MOBILE & whichValid)) {
            editor.putString(PrefUtil.MY_MOBILE, data.mobile);
            postStr += "&phone_number=" + Utils.urlencodeUtf8(data.mobile);
            ++cnt;
        }
        if(0 != (Buddy.FIELD_FLAG_EMAIL & whichValid)) {
            editor.putString(PrefUtil.MY_EMAIL, data.getEmail());
            postStr += "&email_address=" + Utils.urlencodeUtf8(data.getEmail());
            ++cnt;
        }
        if(0 != (Buddy.FIELD_FLAG_JOB_TITLE & whichValid)) {
            editor.putString(PrefUtil.MY_JOB_TITLE, data.jobTitle);
            postStr += "&title=" + Utils.urlencodeUtf8(data.jobTitle);
            ++cnt;
        }
        if(0 != (Buddy.FIELD_FLAG_EMPLOYEE_ID & whichValid)) {
            editor.putString(PrefUtil.MY_EMPLOYEE_ID, data.employeeId);
            postStr += "&employee_id=" + Utils.urlencodeUtf8(data.employeeId);
            ++cnt;
        }
		if(0 != (Buddy.FIELD_FLAG_PHOTO & whichValid)) {
			long ts = Calendar.getInstance().getTimeInMillis() / 1000;
			editor.putString(PrefUtil.MY_PHOTO_UPLOADED_TIMESTAMP, Long.toString(ts));
			postStr += "&upload_photo_timestamp=" + ts;
			++cnt;
		}

		if(cnt > 0) {
			if(connect2 == null)
				connect2 = new Connect2();
			Element root = connect2.Post(postStr);

			if (root != null) {

				// err_no要素のリストを取得
				NodeList errorList = root.getElementsByTagName("err_no");
				// error要素を取得
				Element errorElement = (Element) errorList.item(0);
				// error要素の最初の子ノード（テキストノード）の値を取得
				String errorStr = errorElement.getFirstChild().getNodeValue();

				if (errorStr.equals("0")) {
					errno = 0;
					editor.commit();

                    // refresh my info in local db
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            fGetMyProfile();
                        }
                    }).start();

				} else {
					errno = Integer.parseInt(errorStr);
				}
			} else {
				errno = -1;
			}
			if(errno != ErrorCode.OK)
				return errno;
		}
		
		return errno;
	}

	// TODO 是否要移到 PrefUtil
	/**
	 * Get the current version of Client 
	 * @return version code
	 */
	public int fGetCurrentClientSDKVersionFromLocal() {
		return GlobalSetting.SDK_CLIENT_VERSION;
	}

	/**
	 * Get the current version of server that client support  
	 * @return version code
	 */
	public int fGetCurrentServerSDKVersionFromLocal() {
		return GlobalSetting.SDK_SERVER_VERSION;
	}

	/**
	 * get the security level for biz
	 * @param companyId
	 * @return security level
	 */
	public String fGetSecurityLevel(String companyId){
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        if (isAuthEmpty(strUID, strPwd)) {
            return "";
        }

        String action = "get_security_level";
        String postStr = "action=" + action +"&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd)
                + "&corp_id="+ Utils.urlencodeUtf8(companyId);

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        String securityLevel = "";
        if (root != null) {

            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                Element resultElement = Utils.getFirstElementByTagName(root, action);
                if(resultElement != null) {
                    Element e = Utils.getFirstElementByTagName(resultElement, "security_level");
                    if (e != null) {
                        securityLevel = e.getTextContent();
                    }
                }
            }
        }
        return securityLevel;
	}

	/**
	 * Upload phone number list to allow the automatic matching
	 * 
	 * 
	 * @param phoneNumberList
	 *            phone number list to be matched
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fUploadContactBook(ArrayList<String> phoneNumberList) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fUploadContactBook: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		ArrayList<String> list = new ArrayList<String>();
		for (int j = 0; j < phoneNumberList.size(); j++) {
			String strTmp = fTranslatePhoneNumberToGlobalPhoneNumber(phoneNumberList.get(j));
			if (strTmp != null && list.indexOf(strTmp) == -1) {
				list.add(strTmp);
			} else
				continue;
		}

		String strPhoneNums = "";
		for (int j = 0; j < list.size(); j++) {
			String strTmp = list.get(j);
			if (strTmp != null)
				strPhoneNums += "&phone_number[]="
						+ fGetEncriptedPhoneNumber(strTmp);
			else
				continue;
		}

		String postStr = "action=upload_contact_book&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + strPhoneNums;
		return _doRequestWithoutResponse(postStr);
	}


	/**
	 * Upload the increament of the phone number list to allow the automatic matching
	 * 
	 * 
	 * @param phoneNumberList
	 *            phone number list to be matched
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fIncreaseContactBook(ArrayList<String> phoneNumberList) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fIncreaseContactBook: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		ArrayList<String> listOfEncriptedNumbers = new ArrayList<String>();
		for (int j = 0; j < phoneNumberList.size(); j++) {
			String strTmp = fTranslatePhoneNumberToGlobalPhoneNumber(phoneNumberList.get(j));
			if (strTmp != null && listOfEncriptedNumbers.indexOf(strTmp) == -1) {
				listOfEncriptedNumbers.add(fGetEncriptedPhoneNumber(strTmp));
			} else
				continue;
		}

		String strPhoneNums = "";
		for (int j = 0; j < listOfEncriptedNumbers.size(); j++) {
			String strTmp = listOfEncriptedNumbers.get(j);
			strPhoneNums += "&phone_number[]=" + Utils.urlencodeUtf8(strTmp);
		}

		String postStr = "action=increase_contact_book&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + strPhoneNums;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				ArrayList<Buddy> buddyList = new ArrayList<Buddy>();

				NodeList userList = root.getElementsByTagName("buddy");

				for (int i = 0; i < userList.getLength(); i++) {

					try {
						// user要素を取得
						Element user = (Element) userList.item(i);
						Buddy buddyTmp = new Buddy();
                        XmlHelper.parseBuddy(user, buddyTmp);

						buddyTmp.isBlocked = false;

						Log.i("get buddy:", buddyTmp.userID + ","
								+ buddyTmp.nickName);
						buddyList.add(buddyTmp);
					} catch (Exception e) {
						continue;
					}
				}

				errno = 0;
				Database dbHelper = new Database(mContext);
				dbHelper.storeBuddies(buddyList);
			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Upload the decrement of the phone number list to allow the automatic matching
	 * 
	 * @param phoneNumberList
	 *            phone number list to be matched
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fDecreaseContactBook(ArrayList<String> phoneNumberList) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fIncreaseContactBook: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		ArrayList<String> listOfEncriptedNumbers = new ArrayList<String>();
		for (int j = 0; j < phoneNumberList.size(); j++) {
			String strTmp = fTranslatePhoneNumberToGlobalPhoneNumber(phoneNumberList.get(j));
			if (strTmp != null && listOfEncriptedNumbers.indexOf(strTmp) == -1) {
				listOfEncriptedNumbers.add(fGetEncriptedPhoneNumber(strTmp));
			} else
				continue;
		}

		String strPhoneNums = "";
		for (int j = 0; j < listOfEncriptedNumbers.size(); j++) {
			String strTmp = listOfEncriptedNumbers.get(j);
			strPhoneNums += "&phone_number[]=" + Utils.urlencodeUtf8(strTmp);
		}

		String postStr = "action=decrease_contact_book&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + strPhoneNums;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;
				Database dbHelper = new Database(mContext);

				for (int j = 0; j < listOfEncriptedNumbers.size(); j++) {
					String strTmp = listOfEncriptedNumbers.get(j);
					dbHelper.deleteBuddyByPhoneNumber(strTmp);
				}

			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Scan the phone number list to get Buddies
	 * 
	 * 
	 * @param phoneNumberList
	 *            phone number list to be matched
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fScanPhoneNumbersForBuddy(ArrayList<String> phoneNumberList) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fScanPhoneNumbersForBuddy: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		ArrayList<String> listOfEncriptedNumbers = new ArrayList<String>();
		for (int j = 0; j < phoneNumberList.size(); j++) {
			String strTmp = fTranslatePhoneNumberToGlobalPhoneNumber(phoneNumberList.get(j));
			if (strTmp != null && listOfEncriptedNumbers.indexOf(strTmp) == -1) {
				listOfEncriptedNumbers.add(fGetEncriptedPhoneNumber(strTmp));
			} else
				continue;
		}

		String strPhoneNums = "";
		for (int j = 0; j < listOfEncriptedNumbers.size(); j++) {
			String strTmp = listOfEncriptedNumbers.get(j);
			strPhoneNums += "&phone_number[]=" + Utils.urlencodeUtf8(strTmp);
		}

		String postStr = "action=scan_phone_numbers&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + strPhoneNums;

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				ArrayList<Buddy> buddyList = new ArrayList<Buddy>();

				NodeList userList = root.getElementsByTagName("buddy");

				for (int i = 0; i < userList.getLength(); i++) {

					try {
						// user要素を取得
						Element user = (Element) userList.item(i);
						Buddy buddyTmp = new Buddy();
                        XmlHelper.parseBuddy(user, buddyTmp);
						buddyTmp.isBlocked = false;

						Log.i("get buddy:", buddyTmp.userID + ","
								+ buddyTmp.nickName);
						buddyList.add(buddyTmp);
					} catch (Exception e) {
						continue;
					}
				}

				errno = 0;
				Database dbHelper = new Database(mContext);
				dbHelper.storeBuddies(buddyList);


			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}






	/**
	 * Fetch the buddy list from server and The result will be saved in database
	 * and can be readed by Database.fetchAllMatchedBuddies
	 * 
	 * 
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fGetMatchedBuddyList() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGetMatchedBuddyList: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_matched_buddy_list&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				ArrayList<Buddy> buddyList = new ArrayList<Buddy>();

				NodeList userList = root.getElementsByTagName("buddy");

				for (int i = 0; i < userList.getLength(); i++) {

					try {
						// user要素を取得
						Element user = (Element) userList.item(i);
						Buddy buddyTmp = new Buddy();
                        XmlHelper.parseBuddy(user, buddyTmp);
						buddyTmp.isBlocked = false;

						Log.i("get buddy:", buddyTmp.userID + ","
								+ buddyTmp.nickName);
						buddyList.add(buddyTmp);
					} catch (Exception e) {
						continue;
					}
				}

				errno = 0;
				Database dbHelper = new Database(mContext);
				dbHelper.deleteAllMatchedBuddies();
				dbHelper.storeBuddies(buddyList);


			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Fetch the possible buddy list from server and The result will be saved in
	 * database and can be readed by Database.fetchAllPossibleBuddies
	 * 
	 * 
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fGetPossibleBuddyList() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGetPossibleBuddyList: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_possible_buddy_list&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + "&phone_number="
				+ fGetEncriptedPhoneNumber(sPrefUtil.getMyPhoneNumber());

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				ArrayList<Buddy> buddyList = new ArrayList<Buddy>();

				NodeList userList = root.getElementsByTagName("buddy");

				for (int i = 0; i < userList.getLength(); i++) {

					try {
						// user要素を取得
						Element user = (Element) userList.item(i);
						Buddy buddyTmp = new Buddy();
                        XmlHelper.parseBuddy(user, buddyTmp);
						buddyTmp.isBlocked = false;

						Log.i("get buddy:", buddyTmp.userID + ","
								+ buddyTmp.nickName);
						buddyList.add(buddyTmp);
					} catch (Exception e) {
						continue;
					}
				}

				errno = 0;
				Database dbHelper = new Database(mContext);
				dbHelper.deleteAllPossibleBuddies();
				dbHelper.storeBuddies(buddyList);


			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}


	/**
	 * Fetch the buddylist and possible buddy list from server and The result will be saved in
	 * database and can be readed by Database.fetchAllPossibleBuddies & Database.fetchAllMatchedBuddies
	 * 
	 * 
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fGetBuddyList() {
		Log.i("WowTalkWebServerIF.fGetBuddyList()");
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGetBuddyList: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_buddy_list&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + "&phone_number="
				+ fGetEncriptedPhoneNumber(sPrefUtil.getMyPhoneNumber());

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		int errno = 0;
		if (root != null) {
			NodeList errorList = root.getElementsByTagName("err_no");
			Element errorElement = (Element) errorList.item(0);
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				ArrayList<Buddy> buddyList = new ArrayList<Buddy>();

				NodeList userList = root.getElementsByTagName("buddy");

				for (int i = 0; i < userList.getLength(); i++) {

					try {
						// user要素を取得
						Element user = (Element) userList.item(i);
						Buddy buddyTmp = new Buddy();
                        XmlHelper.parseBuddy(user, buddyTmp);
                        assert buddyTmp.isBlocked == true;
						buddyTmp.isBlocked = false;

						Log.i("get buddy:", buddyTmp.toString());
						buddyList.add(buddyTmp);
					} catch (Exception e) {
						e.printStackTrace();
						continue;
					}
				}

				errno = 0;
				Database dbHelper = new Database(mContext);
				dbHelper.deleteAllMatchedBuddies();
				dbHelper.deleteAllPossibleBuddies();
				int cnt = dbHelper.storeBuddies(buddyList);
				Log.i("fGetBuddyList() stores " + cnt + " items");
			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Fetch the blocked buddy list from server and The result will be saved in
	 * database and can be readed by Database.fetchAllBlockedBuddies
	 * 
	 * 
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fGetBlockedBuddyList() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGetBlockedBuddyList: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_block_list&uid=" + Utils.urlencodeUtf8(strUID) + "&password=";

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				ArrayList<Buddy> buddyList = new ArrayList<Buddy>();

				NodeList userList = root.getElementsByTagName("buddy");

				for (int i = 0; i < userList.getLength(); i++) {

					try {
						// user要素を取得
						Element user = (Element) userList.item(i);
						Buddy buddyTmp = new Buddy();
                        XmlHelper.parseBuddy(user, buddyTmp);
						buddyTmp.isBlocked = true;

						Log.i("get blocked buddy:", buddyTmp.userID + ","
								+ buddyTmp.nickName);
						buddyList.add(buddyTmp);
					} catch (Exception e) {
						continue;
					}
				}

				errno = 0;
				Database dbHelper = new Database(mContext);
				dbHelper.deleteAllBlockList();
				dbHelper.storeBlockList(buddyList);


			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}

    public int fGetBuddiesNearby(boolean useMyLastLocation, double latitude, double longitude,
                                 ArrayList<Buddy> result) {

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//            throw new RuntimeException(
//                    "fGetBuddiesNearby: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
        }

        final String action = "get_nearby_buddies";
        String postStr = "action=" + action + "&uid=" + Utils.urlencodeUtf8(strUID) + "&password=" + Utils.urlencodeUtf8(strPwd);
        if(!useMyLastLocation) {
            postStr += "&latitude=" + latitude + "&longitude=" + longitude;
        }

        return _requestForBuddies(action, postStr, result);
    }

    private int _requestForBuddies(String action, String postStr, ArrayList<Buddy> result) {
        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {
            Element errorElement = Utils.getFirstElementByTagName(root, "err_no");
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = 0;

                Element resultElement = Utils.getFirstElementByTagName(root, action);
                if(resultElement != null) {
                    NodeList buddyNodeList = resultElement.getElementsByTagName("buddy");
                    if(buddyNodeList != null && buddyNodeList.getLength() > 0) {
                        for(int i = 0, n = buddyNodeList.getLength(); i < n; ++i) {
                            Element node = (Element)buddyNodeList.item(i);
                            Buddy b = new Buddy();
                            XmlHelper.parseBuddy(node, b);
                            result.add(b);
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
	 * Try to add buddy to my list.
	 * 
	 * @param buddy_uid
	 * @return ErrorCode
     *
     * If succeed, the buddy will be saved into local db, both buddydetail and buddies table,
     * you may want to check this:
     * (0 != (Buddy.RELATIONSHIP_FRIEND_HERE & buddy.getFriendShipWithMe()))
	 */
	public int fAddBuddy(String buddy_uid) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.AUTH;
		}

        if (Utils.isNullOrEmpty(buddy_uid)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        if (strUID.equals(buddy_uid)) {
            return ErrorCode.ILLEGAL_OPERATION;
        }

		String postStr = "action=add_buddy&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
				+ Utils.urlencodeUtf8(strPwd) + "&buddy_id=" + Utils.urlencodeUtf8(buddy_uid);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = ErrorCode.UNKNOWN;
		if (root != null) {
            Element errEle = Utils.getFirstElementByTagName(root, "err_no");
            errno = Utils.tryParseInt(errEle.getTextContent(), ErrorCode.BAD_RESPONSE);

            if (ErrorCode.OK == errno) {
                Element user = Utils.getFirstElementByTagName(root, "buddy");
                Database dbHelper = new Database(mContext);

                try {
                    Buddy buddy = new Buddy();
                    XmlHelper.parseBuddy(user, buddy);
                    dbHelper.storeNewBuddyWithUpdate(buddy);
                } catch (Exception e) {
                    e.printStackTrace();
                }
			}
		}
		return errno;
	}

	/**
	 * add local contact to buddies.
	 * @param localPhonePersons local phone numbers which is delegated to the local contact.(带有国际区号的以加号开头的手机号)
	 * @return
	 */
	public List<Buddy> fAddLocalContactsAsBuddies(String[] localPhoneUsers) {
	    List<Buddy> buddies = new ArrayList<Buddy>();
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return buddies;
        }

        String action = "add_mobile_numbers_as_buddy";
        String postStr = "action=" + action + "&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
                + Utils.urlencodeUtf8(strPwd);

        if(localPhoneUsers != null && localPhoneUsers.length != 0) {
            String tempPhoneNumber = null;
            for(String localPhoneUser : localPhoneUsers) {
                if(TextUtils.isEmpty(localPhoneUser) || !localPhoneUser.matches("\\+?[0-9- ]+:.*")) {
                    continue;
                }
                int index = localPhoneUser.indexOf(':');
                if(index > 0) {
                    //TODO
                    // 数组或标量，带有国际区号的以加号开头的手机号
                    tempPhoneNumber = localPhoneUser.substring(0, index);
                    tempPhoneNumber = tempPhoneNumber.replaceAll("[- ]", "");
                    postStr += "&phone_user[]=" + Utils.urlencodeUtf8(tempPhoneNumber + localPhoneUser.substring(index));
                }
            }
        }

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = 0;
        if (root != null) {
            Element errEle = Utils.getFirstElementByTagName(root, "err_no");
            errno = Utils.tryParseInt(errEle.getTextContent(), ErrorCode.BAD_RESPONSE);

            if (ErrorCode.OK == errno) {
                Element resultElement = Utils.getFirstElementByTagName(root, action);
                Database dbHelper = new Database(mContext);
                if (resultElement != null) {
                    // buddy list
                    NodeList buddyNodeList = resultElement.getElementsByTagName("buddy");
                    if (buddyNodeList != null && buddyNodeList.getLength() > 0) {
                        for (int i = 0, n = buddyNodeList.getLength(); i < n; ++i) {
                            Element buddyNode = (Element) buddyNodeList.item(i);
                            try {
                                Buddy buddy = new Buddy();
                                XmlHelper.parseBuddy(buddyNode, buddy);
                                dbHelper.storeNewBuddyWithUpdate(buddy);
                                buddies.add(buddy);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }
            }
        }
        return buddies;
    }

    public int fIgnoreBuddyRequest(String buddyUid) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        String postStr = "action=reject_buddy&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd)
                + "&buddy_id=" + Utils.urlencodeUtf8(buddyUid);
        return _doRequestWithoutResponse(postStr);
    }

    /**
     * Remove buddy.
     *
     * @param buddy_uid
     * @return ErrorCode
     *
     * Also remove buddy from these local db tables:
     * (1) buddies
     * (2) message
     *
     * but not from buddydetail table.
     */
    public int fRemoveBuddy(String buddy_uid) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        String postStr = "action=remove_buddy&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
                + Utils.urlencodeUtf8(strPwd) + "&buddy_id=" + Utils.urlencodeUtf8(buddy_uid)
                + "&two_way=1";

        int errno = _doRequestWithoutResponse(postStr);

        if (errno == ErrorCode.OK) {
            Database dbHelper = new Database(mContext);
            dbHelper.deleteBuddyByUID(buddy_uid);
//            dbHelper.deleteChatMessageWithUser(buddy_uid);
        }

        return errno;
    }

	/**
	 * Block Buddy (server & local)
	 * 
	 * @param buddy_uid
	 *            :
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fBlockBuddy(String buddy_uid) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
		}

		String postStr = "action=block_buddy&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
				+ Utils.urlencodeUtf8(strPwd) + "&buddy_id=" + Utils.urlencodeUtf8(buddy_uid);

		int errno = _doRequestWithoutResponse(postStr);

        if (errno == ErrorCode.OK) {
            errno = 0;
            Database dbHelper = new Database(mContext);
            dbHelper.blockBuddy(buddy_uid);
        } else if(errno == -99){
            Database dbHelper = new Database(mContext);
            dbHelper.deleteBuddyByUID(buddy_uid);
        }

		return errno;
	}

	/**
	 * Unblock Buddy (server & local)
	 * 
	 * @param buddy_uid
	 *            :
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fUnlockBuddy(String buddy_uid) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fUnlockBuddy: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=unblock_buddy&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
				+ Utils.urlencodeUtf8(strPwd) + "&buddy_id=" + Utils.urlencodeUtf8(buddy_uid);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;
				Database dbHelper = new Database(mContext);
				dbHelper.unblockBuddy(buddy_uid);

			} else {
				errno = Integer.parseInt(errorStr);
				if(errno==-99){
					Database dbHelper = new Database(mContext);
					dbHelper.deleteBuddyByUID(buddy_uid);

				}
			}
		} else {
			errno = -1;
		}
		return errno;
	}

    /**
     *
     * @param q
     * @param accountType can be null. Buddy.ACCOUNT_TYPE_* constants.
     * @return
     */
    public int fSearchBuddy(String q, Integer accountType, ArrayList<Buddy> result) {
        if (null == result) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        String postStr = "action=search_buddy&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
                + Utils.urlencodeUtf8(strPwd) + "&q=" + Utils.urlencodeUtf8(q);
        if (accountType != null) {
            // 搜索学生或老师
            if (Buddy.ACCOUNT_TYPE_STUDENT == accountType) {
                postStr += "&acc_type[]=1 & acc_type[]=2";
            } else {
                postStr += "&acc_type=" + accountType;
            }
        }

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = 0;
        if (root != null) {
            Element errEle = Utils.getFirstElementByTagName(root, "err_no");
            errno = Utils.tryParseInt(errEle.getTextContent(), ErrorCode.BAD_RESPONSE);

            if (ErrorCode.OK == errno) {
                NodeList userList = root.getElementsByTagName("buddy");

                Database dbHelper = new Database(mContext);

                for (int i = 0; i < userList.getLength(); i++) {
                    try {
                        Element user = (Element) userList.item(i);
                        Buddy buddy = new Buddy();
                        XmlHelper.parseBuddy(user, buddy);
                        result.add(buddy);
                        dbHelper.storeNewBuddyDetailWithUpdate(buddy);
                    } catch (Exception e) {
                        continue;
                    }
                }
            }
        }

        return errno;
    }


	/**
	 * Get Buddy with UID 
	 * 
	 * @param buddy_uid
	 *            :
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fGetBuddyWithUID(String buddy_uid) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException("fGetBuddyWithUID: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_buddy&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
				+ Utils.urlencodeUtf8(strPwd) + "&buddy_id=" + Utils.urlencodeUtf8(buddy_uid);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				NodeList userList = root.getElementsByTagName("buddy");

                Database dbHelper = new Database(mContext);

				for (int i = 0; i < userList.getLength(); i++) {

					try {
						// user要素を取得
						Element user = (Element) userList.item(i);
						Buddy buddyTmp = new Buddy();
                        buddyTmp.isBlocked = false;
                        XmlHelper.parseBuddy(user, buddyTmp);
						Log.i("get buddy:", buddyTmp.userID + ","
                                + buddyTmp.nickName);
                        dbHelper.storeNewBuddyWithUpdate(buddyTmp);
//                        dbHelper.storeNewBuddyDetailWithUpdate(buddyTmp);
					} catch (Exception e) {
						continue;
					}
				}

				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);
				if(errno==-99){
					Database dbHelper = new Database(mContext);
					dbHelper.deleteBuddyByUID(buddy_uid);
				}
			}
		} else {
			errno = -1;
		}
		return errno;
	}


	/**
	 * Get Buddy with phone_number 
	 * 
	 * @param strPhoneNumber
	 * @param buddies
	 * @return 0: good result -1: no reply -99:no this user
	 */
	public int fGetBuddyWithPhoneNumber(String strPhoneNumber, List<Buddy> buddies) {

		if (strPhoneNumber==null || strPhoneNumber.equals("") ) {
//			throw new RuntimeException("fGetBuddyWithPhoneNumber: parameter not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException("fGetBuddyWithPhoneNumber: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String action = "get_buddy_by_phonenumber";
		String postStr = "action=" + action +"&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
				+ Utils.urlencodeUtf8(strPwd) + "&phone_number=" + Utils.urlencodeUtf8(strPhoneNumber);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;
				Element resultElement = Utils.getFirstElementByTagName(root, action);
				if(resultElement != null) {
                    NodeList buddyNodeList = resultElement.getElementsByTagName("buddy");
                    if(buddyNodeList != null && buddyNodeList.getLength() > 0) {
                        Buddy tempBuddy = null;
                        Database dbHelper = new Database(mContext);
                        for(int i = 0, n = buddyNodeList.getLength(); i < n; ++i) {
                            Element buddyNode = (Element) buddyNodeList.item(i);
                            tempBuddy = new Buddy();
                            XmlHelper.parseBuddy(buddyNode, tempBuddy);
                            buddies.add(tempBuddy);
                            dbHelper.storeNewBuddyDetailWithUpdate(tempBuddy);
                        }
                    }
                }
			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}




	/**
	 * Post my profile photo to server 自分の画像をアップロード
	 * <p>
	 * fUpdateMyProfile() calls this, and update local cache on success.
	 * 
	 * @param filePath image filepath to be post
     * @param delegate
	 * @param tag
	 */
	public void fPostMyPhoto(String filePath, NetworkIFDelegate delegate,
			int tag) {

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
			Log.w("fPostMyPhoto: UserID and password invalid");
			return;
		}

		if (filePath == null || filePath.equals("")) {
			Log.w("fPostMyPhoto: filePath invalid");
			return;
		}

		RemoteFileService.upload(mContext,
				filePath,
				GlobalSetting.S3_PROFILE_PHOTO_DIR,
				strUID,
				delegate,
				tag);
	}

	/**
	 * Post my profile photo thumbnail to server
	 * <p>
	 * fUpdateMyProfile() calls this.
	 * 
	 * @param filePath image file path to be post
	 * @param delegate
	 * @param tag
	 */
	public void fPostMyThumbnail(String filePath, NetworkIFDelegate delegate,
			int tag) {

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
			Log.w("fPostMyThumbnail: UserID and password invalid");
			return;
		}

		if (filePath == null || filePath.equals("")) {
			Log.w("fPostMyThumbnail: filePath invalid");
			return;
		}

		RemoteFileService.upload(mContext,
				filePath,
				GlobalSetting.S3_PROFILE_THUMBNAIL_DIR,
				strUID,
				delegate,
				tag);
	}



	/**
	 * Get photo for uid from server 指定したユーザidの画像をゲット
	 * 
	 * @param userID
	 * 
	 * @param delegate
	 *            Photo data will be sent as data to following delegate void
	 *            didFinishNetworkIFCommunication(int tag,Object data);
	 * 
	 * @param tag
	 */
	public void fGetPhotoForUserID(String userID, NetworkIFDelegate delegate,
			int tag){
		this.fGetPhotoForUserID(userID, delegate, tag, null);
	}

	/**
	 * <p>Get photo for uid from server.</p>
	 * 
	 * @param userID
	 * 
	 * @param NetworkIFDelegate
	 *            Photo data will be sent as data to following delegate void
	 *            didFinishNetworkIFCommunication(int tag,Object data);
	 * 
	 * @param tag
	 * @param outputFilepath
	 */
	public void fGetPhotoForUserID(String userID, NetworkIFDelegate delegate,
			int tag,String outputFilepath) {
		if (userID == null || userID.equals("")) {
			Log.w("fGetPhotoForUserID: UserID invalid");
			return;
		}
		RemoteFileService.download(mContext,
				outputFilepath,
				GlobalSetting.S3_PROFILE_PHOTO_DIR,
				userID,
				delegate,
				tag);
	}

	/**
	 * Get thumbnail for uid from server 指定したユーザidの画像Thumbnailをゲット
	 * 
	 * @param userID
	 * 
	 * @param NetworkIFDelegate
	 *            Thumbnail data will be sent as data to following delegate void
	 *            didFinishNetworkIFCommunication(int tag,Object data);
	 * 
	 * @param tag
	 * 
	 */
	public void fGetThumbnailForUserID(String userID,
			NetworkIFDelegate delegate, int tag){
		this.fGetThumbnailForUserID(userID, delegate, tag, null);
	}

	/**
	 * Get thumbnail for uid from server 
	 * 
	 * @param userID
	 * 
	 * @param NetworkIFDelegate
	 *            Thumbnail data will be sent as data to following delegate void
	 *            didFinishNetworkIFCommunication(int tag,Object data);
	 * 
	 * @param tag
	 * @param outputFilepath
	 */
	public void fGetThumbnailForUserID(String userID,
			NetworkIFDelegate delegate, int tag,String outputFilepath) {
		if (userID == null || userID.equals("")) {
			Log.w("fGetPhotoForUserID: UserID invalid");
			return;
		}

		RemoteFileService.download(mContext,
				outputFilepath,
				GlobalSetting.S3_PROFILE_THUMBNAIL_DIR,
				userID,
				delegate,
				tag);
	}

    public void fPostGroupPhoto(String groupid, String filePath, NetworkIFDelegate delegate,
                                boolean isThumbnail, int tag) {
        fPostGroupPhoto(groupid, filePath, delegate, isThumbnail, tag, false);
    }

	public void fPostGroupPhoto(String groupid, String filePath, NetworkIFDelegate delegate,
			boolean isThumbnail, int tag, boolean autoUpdateLocalCache) {

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
			Log.w("fPostGroupPhoto: UserID and password invalid");
			return;
		}

		if (filePath == null || filePath.equals("")) {
			Log.w("fPostGroupPhoto: filePath invalid");
			return;
		}

		RemoteFileService.upload(mContext,
				filePath,
				isThumbnail ? GlobalSetting.S3_PROFILE_THUMBNAIL_DIR : GlobalSetting.S3_PROFILE_PHOTO_DIR,
				groupid,
				delegate,
				tag);
	}


	/**
	 * Get a file from server
	 * 
	 * @param fileID
	 *            :
	 * @param NetworkIFDelegate
	 *            file data will be sent as data to following delegate void
	 *            didFinishNetworkIFCommunication(int tag,Object data);
	 * 
	 * @param tag
	 */
	public void fGetFileFromServer(String fileID, NetworkIFDelegate delegate,
			int tag){
		this.fGetFileFromServer(fileID,delegate,tag,null,null);
	}

	/**
	 * Call fGetFileFromServer(fileID,delegate,tag,outputFilepath,null);
	 * @param fileID
	 * @param delegate
	 * @param tag
	 * @param outputFilepath
	 */
	public void fGetFileFromServer(String fileID, NetworkIFDelegate delegate,
			int tag, String outputFilepath){
		this.fGetFileFromServer(fileID,delegate,tag,outputFilepath,null);
	}

	/**
	 * Get a file from server
	 * 
	 * @param fileID
	 *            :
	 * @param NetworkIFDelegate
	 *            file data will be sent as data to following delegate void
	 *            didFinishNetworkIFCommunication(int tag,Object data);
	 * 
	 * @param tag
	 * @param outputFilepath
	 */
    public void fGetFileFromServer(String fileID, NetworkIFDelegate delegate,
                                   int tag, String outputFilepath, CancelFlag cancelFlag) {
        fGetFileFromServer(fileID, GlobalSetting.S3_UPLOAD_FILE_DIR, delegate,
                tag, outputFilepath, cancelFlag);
    }

	public void fGetFileFromServer(String fileID, String fileDir, NetworkIFDelegate delegate,
			int tag, String outputFilepath, CancelFlag cancelFlag) {
		if(fGetPseudoFileFromServer(fileID, delegate, tag, outputFilepath))
			return;

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
			Log.w("fGetFileFromServer: UserID and password invalid");
			return;
		}

		if (fileID == null || fileID.equals("")) {
			Log.w("fGetFileFromServer: file_id invalid");
			return;
		}

		RemoteFileService.download(mContext,
				outputFilepath,
				fileDir,
				fileID,
				delegate,
				tag);
	}

	/**
	 * download pseudo(built-in) file.
	 * 
	 * @param fileID PSEUDO_FILEID_*
	 * @param delegate
	 * @param tag
	 * @param outputFilepath
	 * @return processed or not?
	 */
	private boolean fGetPseudoFileFromServer(String fileID,
			NetworkIFDelegate delegate, int tag, String outputFilepath) {
		if (fileID == null || fileID.equals("")) {
			Log.w("fGetFileFromServer: file_id invalid");
			return true;
		}

		if(PSEUDO_FILEID_PHOTO_LANDSCAPE1.equals(fileID)) {
			fGetPseudoFileFromServer_helper("pseudo_photo_landscape1.jpg", delegate, outputFilepath);
			return true;
		}
		if(PSEUDO_FILEID_PHOTO_LANDSCAPE2.equals(fileID)) {
			fGetPseudoFileFromServer_helper("pseudo_photo_landscape2.jpg", delegate, outputFilepath);
			return true;
		}
		if(PSEUDO_FILEID_PHOTO_LANDSCAPE3.equals(fileID)) {
			fGetPseudoFileFromServer_helper("pseudo_photo_landscape3.jpg", delegate, outputFilepath);
			return true;
		}
		if(PSEUDO_FILEID_PHOTO_LANDSCAPE4.equals(fileID)) {
			fGetPseudoFileFromServer_helper("pseudo_photo_landscape4.jpg", delegate, outputFilepath);
			return true;
		}
		if(PSEUDO_FILEID_AUDIO.equals(fileID)) {
			fGetPseudoFileFromServer_helper("pseudo_audio.wav", delegate, outputFilepath);
			return true;
		}
		return false;
	}

	private void fGetPseudoFileFromServer_helper(String assetFileName,
			NetworkIFDelegate delegate,
			String outputFilepath) {
		InputStream is;
		try {
			is = mContext.getAssets().open(assetFileName, AssetManager.ACCESS_STREAMING);
		} catch (IOException e) {
			e.printStackTrace();
			delegate.didFailNetworkIFCommunication(0, "IOException".getBytes());
			return;
		}
		if(outputFilepath == null) {
			delegate.didFinishNetworkIFCommunication(0, readAllFromStream(is));
		} else {
			try {
				FileOutputStream fos = new FileOutputStream(new File(outputFilepath));
				fos.write(readAllFromStream(is));
				delegate.didFinishNetworkIFCommunication(0, "download success".getBytes());
			} catch (FileNotFoundException e) {
				e.printStackTrace();
				delegate.didFailNetworkIFCommunication(0, ("Can't open file to write: " + outputFilepath).getBytes());
			} catch (IOException e) {
				e.printStackTrace();
				delegate.didFailNetworkIFCommunication(0, "Source file not found".getBytes());
			}
		}
		try {
			is.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * Read all bytes from input stream.
	 * @param is
	 * @return
	 */
	private static byte[] readAllFromStream(InputStream is) {
		final int BUFSZ = 1024;
		ArrayList<byte[]> data = new ArrayList<byte[]>();
		ArrayList<Integer> buflen = new ArrayList<Integer>();
		int n = 0;
		while(true) {
			byte[] buf = new byte[BUFSZ];
			int i;
			try {
				i = is.read(buf);
			} catch (IOException e) {
				e.printStackTrace();
				break;
			}
			if(i > 0) {
				data.add(buf);
				buflen.add(i);
				n += i;
			} else {
				break;
			}
		}
		byte[] rtn = new byte[n];
		int idx = -1;
		for(int i = 0; i < data.size(); ++i) {
			for(int j = 0; j < buflen.get(i); ++j) {
				rtn[++idx] = data.get(i)[j];
			}
		}

		return rtn;
	}

    /**
     * Upload a file to server.
     *
     * @param filepath
     * @param delegate
     * @param tag
     */
    public void fPostFileToServer(String filepath, NetworkIFDelegate delegate, int tag) {
        fPostFileToServer(filepath, GlobalSetting.S3_UPLOAD_FILE_DIR, delegate, tag);
    }

    /**
     * Upload a file to server.
     *
     * @param filepath
     * @param targetDir e.g., GlobalSetting.S3_UPLOAD_FILE_DIR
     * @param delegate
     * @param tag
     */
	public void fPostFileToServer(String filepath, String targetDir, NetworkIFDelegate delegate, int tag) {

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
			Log.w("fPostMyThumbnail: UserID and password invalid");
			return;
		}

		if (filepath == null || filepath.equals("")) {
			Log.w("fPostFileToServer: filePath invalid");
			return;
		}

		String fileId = UUID.randomUUID().toString();
		RemoteFileService.upload(mContext,
				filepath,
				targetDir,
				fileId,
				delegate,
				tag);
	}


	/**
	 * Get a file from Shop
	 * 
	 * @param fileID
	 *            :
	 * @param NetworkIFDelegate
	 *            file data will be sent as data to following delegate void
	 *            didFinishNetworkIFCommunication(int tag,Object data);
	 * 
	 * @param tag
	 * @param outputFilepath
	 */
	public void fGetFileFromShop(String fileID, NetworkIFDelegate delegate,
			int tag,String outputFilepath) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
			Log.w("fGetFileFromShop: UserID and password invalid");
			return;
		}

		if (fileID == null || fileID.equals("")) {
			Log.w("fGetFileFromShop: file_id invalid");
			return;
		}

		RemoteFileService.download(mContext,
				outputFilepath,
				GlobalSetting.S3_SHOP_DIR,
				fileID,
				delegate,
				tag);
	}

	/**
	 * Create a group chat room 
	 * 
	 * @param strGroupName : required
	 * @param isTemporaryGroup
	 * @param latitude : required if not temp
	 * @param longitude : required if not temp
	 * @param groupStatus : required if not temp
	 * @return (String[0]=group id, String[1]=short_group_id) or null if failed.
	 */
	public String[] fGroupChat_Create(String strGroupName,
			boolean isTemporaryGroup,
            String place,
            String type,
			float latitude, float longitude,
			String groupStatus
			) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
			return null;
		}

		if (strGroupName == null) {
			return null;
		}
		String postStr = "";
		if (isTemporaryGroup) {
			postStr = "action=create_temp_group_chat_room&uid="
					+ Utils.urlencodeUtf8(strUID)
					+ "&password=" + Utils.urlencodeUtf8(strPwd)
					+ "&group_name=" + Utils.urlencodeUtf8(strGroupName);
		} else {
			postStr = "action=create_group_chat_room&uid="
					+ Utils.urlencodeUtf8(strUID)
					+ "&password=" + Utils.urlencodeUtf8(strPwd)
					+ "&group_name=" + Utils.urlencodeUtf8(strGroupName)
                    + "&place=" + Utils.urlencodeUtf8(place)
                    + "&type=" + Utils.urlencodeUtf8(type)
					+ "&create_latitude=" + latitude
					+ "&create_longitude=" + longitude
					+ "&group_status=" + Utils.urlencodeUtf8(groupStatus)
					;
		}

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
			    String[] groupIds = new String[2];
				NodeList nodeList = root.getElementsByTagName("group_id");
				Element element = (Element) nodeList.item(0);
				String strGroupID = "";
				if (element != null && element.getFirstChild() != null) {
					strGroupID = element.getFirstChild().getNodeValue();
					groupIds[0] = strGroupID;
				}

				nodeList = root.getElementsByTagName("short_group_id");
                element = (Element) nodeList.item(0);
                String strShortGroupID = "";
                if (element != null && element.getFirstChild() != null) {
                    strShortGroupID = element.getFirstChild().getNodeValue();
                    groupIds[1] = strShortGroupID;
                }

                return groupIds;
			}
		}
		return null;
	}

    /**
	 * leave a group chat room and delete the information of it in local db
	 * 
	 * @param groupID
	 *            :
	 */
	public int fGroupChat_LeaveGroup(String groupID) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGroupChat_LeaveGroup: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		if (groupID == null) {
//			throw new RuntimeException("fGroupChat_LeaveGroup not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=leave_group_chat_room&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + "&group_id=" + Utils.urlencodeUtf8(groupID);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {

				Database dbHelper = new Database(mContext);
//				dbHelper.deleteGroupChatRoomWithID(groupID);
//				dbHelper.deleteAllBuddiesInGroupChatRoomByID(groupID);
				dbHelper.deleteBuddyFromGroupChatRoom(groupID, strUID);
				dbHelper.deleteMyselfFlagFromGroupChatRoom(groupID);

				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);

			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Force to add members to a group chat room
	 * 
	 * @param groupID
	 * @param buddies
     * @param onlyForPendingIn Indicate that we're going to accept pending member, not invite.
     *                         A call of this method with onlyForPendingIn=true on buddies who have not requested
     *                         to join in will have no effect.
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fGroupChat_AddMembers(String groupID, List<Buddy> buddies, boolean onlyForPendingIn) {
        return fGroupChat_AddMembers(groupID, buddies, null, onlyForPendingIn);
    }

    /**
     *
     * @param groupID
     * @param buddies
     * @param phone_users phone:nickname
     * @param onlyForPendingIn Indicate that we're going to accept pending member, not invite.
     *                         A call of this method with onlyForPendingIn=true on buddies who have not requested
     *                         to join in will have no effect.
     * @return
     */
    public int fGroupChat_AddMembers(String groupID, List<Buddy> buddies,
                                     List<String> phone_users, boolean onlyForPendingIn) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.INVALID_ARGUMENT;
		}

		if (groupID == null || buddies == null) {
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=add_group_member&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + "&group_id=" + Utils.urlencodeUtf8(groupID);
        if (onlyForPendingIn) {
            postStr += "&only_for_pending_in=1";
        }

		for (int j = 0; j < buddies.size(); j++) {
			Buddy buddy = buddies.get(j);
			if (buddy != null)
				postStr += "&buddy_id[]=" + Utils.urlencodeUtf8(buddy.userID);
			else
				continue;
		}

        if(phone_users != null && !phone_users.isEmpty()) {
            for(String s : phone_users) {
                if(s == null || !s.matches("\\+?[0-9- ]+:.*")) {
//                    throw new IllegalArgumentException("phone_users expects <phone>:<nick>, but get " + s + ".");
                    return ErrorCode.INVALID_ARGUMENT;
                }
                int i = s.indexOf(':');
                if(i < 0)
                    continue; // impossible
                String phone = s.substring(0, i);
                phone = phone.replaceAll("[- ]", "");
                postStr += "&phone_user[]=" + Utils.urlencodeUtf8(phone + s.substring(i));
            }
        }

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {
			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
			    GroupChatRoom room = new GroupChatRoom();
	            XmlHelper.parseGroup(root, room);
			    Database dbHelper = new Database(mContext);
				dbHelper.storeGroupChatRoom(room);

				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Get the list of userID who has joined the group chat room and save to local db
	 * 
	 * @param groupID
	 *            :
	 * @return ("code", 0: good result -1: no reply others: error code from server),
	 *         ("data", ArrayList<GroupMember>)
	 */
	public Map<String, Object> fGroupChat_GetMembers(String groupID) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
		Map<String, Object> resultMap = new HashMap<String, Object>();
		String codeKey = "code";
		String dataKey = "data";

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGroupChat_GetGroupMembers: UserID and Password not set");
		    resultMap.put(codeKey, ErrorCode.INVALID_ARGUMENT);
            return resultMap;
		}

        if (TextUtils.isEmpty(groupID)) {
//			throw new RuntimeException("fGroupChat_GetGroupMembers not set");
		    resultMap.put(codeKey, ErrorCode.INVALID_ARGUMENT);
            return resultMap;
		}

		String postStr = "action=get_group_members&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + "&group_id=" + Utils.urlencodeUtf8(groupID);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				ArrayList<GroupMember> buddyList = new ArrayList<GroupMember>();

				NodeList userList = root.getElementsByTagName("buddy");

				for (int i = 0; i < userList.getLength(); i++) {

					try {
						// user要素を取得
						Element user = (Element) userList.item(i);
						GroupMember buddyTmp = new GroupMember(null, groupID);
                        XmlHelper.parseGroupMember(user, buddyTmp);
						buddyTmp.isBlocked = true;
                        buddyTmp.setAccountType(Buddy.ACCOUNT_TYPE_STUDENT);

						Log.i("get blocked buddy:", buddyTmp.userID + ","
								+ buddyTmp.nickName);
						buddyList.add(buddyTmp);
					} catch (Exception e) {
						continue;
					}
				}
				Database dbHelper = new Database(mContext);
				dbHelper.deleteAllBuddiesInGroupChatRoomByID(groupID);
				dbHelper.addBuddiesToGroupChatRoomByID(groupID, buddyList);
				resultMap.put(codeKey, 0);
				resultMap.put(dataKey, buddyList);
			} else {
				resultMap.put(codeKey, Integer.parseInt(errorStr));
			}
		} else {
			resultMap.put(codeKey, -1);
		}
		return resultMap;
	}

	/**
	 * 获取此群组下的所有成员，包括子部门的成员
	 * @param groupID
	 * @return
	 */
	public int fGroupChat_GetMembersIteration(String groupID) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        Map<String, Object> resultMap = new HashMap<String, Object>();

        if (isAuthEmpty(strUID, strPwd) || TextUtils.isEmpty(groupID)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        int resultCode = ErrorCode.INVALID_ARGUMENT;
        String postStr = "action=get_all_buddys_in_group&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd) + "&group_id=" + Utils.urlencodeUtf8(groupID);

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);
        if (root != null) {

            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                resultCode = 0;
                Element resultElement = Utils.getFirstElementByTagName(root, "get_all_buddys_in_group"); 
                if(resultElement != null) {
                    ArrayList<GroupMember> buddyList = new ArrayList<GroupMember>();
                    NodeList userList = resultElement.getElementsByTagName("buddy");
                    for (int i = 0; i < userList.getLength(); i++) {
                        try {
                            // user要素を取得
                            Element user = (Element) userList.item(i);
                            GroupMember buddyTmp = new GroupMember(null, groupID);
                            XmlHelper.parseGroupMember(user, buddyTmp);
                            buddyTmp.isBlocked = true;
                            buddyTmp.setAccountType(Buddy.ACCOUNT_TYPE_STUDENT);

                            Log.d("get blocked buddy:", buddyTmp.userID + ","
                                    + buddyTmp.nickName);
                            buddyList.add(buddyTmp);
                        } catch (Exception e) {
                            continue;
                        }
                    }
                    Database dbHelper = new Database(mContext);
                    dbHelper.deleteAllBuddiesInGroupChatRoomByID(groupID);
                    dbHelper.addBuddiesToGroupChatRoomByID(groupID, buddyList);
                    dbHelper.storeMembersAsBuddies(buddyList);
                }
            } else {
                resultCode = Integer.parseInt(errorStr);
            }
        } else {
            resultCode =  -1;
        }
        return resultCode;
    }

	/**
	 * Get group chat room info 　
	 * 
	 * @param groupID
	 *            :
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fGroupChat_GetGroupDetail(String groupID) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGroupChat_GetGroupDetail: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		if (groupID == null) {
//			throw new RuntimeException(
//					"fGroupChat_GetGroupDetail:groupID  not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_group_info&uid=" + Utils.urlencodeUtf8(strUID) + "&password="
				+ Utils.urlencodeUtf8(strPwd) + "&group_id=" + Utils.urlencodeUtf8(groupID);

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
            GroupChatRoom room = new GroupChatRoom();
            XmlHelper.parseGroup(root, room);
            room.groupID = groupID;

            Database dbHelper = new Database(mContext);
            dbHelper.storeGroupChatRoom(room);
        }

		return errno;
	}

    /**
     * Operate settings on group, such as block msg notification, add to favorite.
     *
     * @param group
     * @return
     */
    public int fGroupChat_Settings(GroupChatRoom group) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        if (group == null || Utils.isNullOrEmpty(group.groupID)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "opt_group";
        String postStr = String.format("action=%s&uid=%s&password=%s"
                + "&group_id=%s"
                + "&block_msg=%d&block_msg_notification=%d",
                action, strUID, strPwd,
                group.groupID,
                group.willBlockMsg ? 1 : 0,
                group.willBlockNotification ? 1 : 0
        );
        if (group.groupNameLocal != null) {
            postStr += "&alias=" + Utils.urlencodeUtf8(group.groupNameLocal);
        }
        if (group.myNickHere != null) {
            postStr += "&nickname_in_group=" + Utils.urlencodeUtf8(group.myNickHere);
        }

        return _doRequestWithoutResponse(postStr);
    }
    /**
     * Update group info.
     *
     * @param g Only non-null properties will be committed.
     * @return
     */
    public int fGroupChat_UpdateInfo(GroupChatRoom g) {
        if (null == g || Utils.isNullOrEmpty(g.groupID))
            return ErrorCode.INVALID_ARGUMENT;

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.AUTH;
        }

        String action = "update_group_info";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd)
                + "&group_id=" + Utils.urlencodeUtf8(g.groupID);

        if (null != g.groupNameOriginal)
            postStr += "&name=" + Utils.urlencodeUtf8(g.groupNameOriginal);

        if (g.isTemporaryGroup && g.isGroupNameChanged) {
            postStr += "&is_group_name_changed=" + Utils.urlencodeUtf8("1");
        }

        if (null != g.location) {
            postStr += "&lat=" + g.location.y;
            postStr += "&lon=" + g.location.x;
        }

        if (null != g.place)
            postStr += "&place=" + Utils.urlencodeUtf8(g.place);

        if (0 < g.getPhotoUploadedTimestamp())
            postStr += "&upload_photo_timestamp=" + g.getPhotoUploadedTimestamp();

        if (null != g.groupStatus)
            postStr += "&status=" + Utils.urlencodeUtf8(g.groupStatus);

        if (null != g.description)
            postStr += "&intro=" + Utils.urlencodeUtf8(g.description);

        if (null != g.category)
            postStr += "&category=" + Utils.urlencodeUtf8(g.category);

        return _doRequestWithoutResponse(postStr);
    }
	
	/**
	 * Get my group list, contains temp groups and non-temp groups
	 * <p>
	 * The group list is stored in local database, you can retrieve it by
	 * {@link Database}.fetchAllGroupChatRooms().
	 * 
	 * @return
	 */
    public int fGroupChat_GetMyGroups() {
        return fGroupChat_GetMyGroups(0);
    }

    /**
     * get my non-temp groups
     * @return
     */
    public int fGroupChat_GetMyNonTempGroups() {
        return fGroupChat_GetMyGroups(1);
    }

    /**
     * get my temp groups
     * @return
     */
    public int fGroupChat_GetMyTempGroups() {
        return fGroupChat_GetMyGroups(2);
    }

	/**
	 * Get my group list according to the temp flag
	 * @param tempFlag 0, all groups; 1, non temp groups; 2, temp groups
	 * @return
	 */
    private int fGroupChat_GetMyGroups(int tempFlag) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//          throw new RuntimeException(
//                  "fGroupChat_GetGroupDetail: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "get_my_groups";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd);
        // with_temp 默认值为1
        // only_temp 默认值为0，且在包含"with_temp"时无效
        switch (tempFlag) {
        case 0:
            break;
        case 1:
            postStr += "&with_temp=0";
            break;
        case 2:
            postStr += "&only_temp=1";
            break;
        default:
            break;
        }

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno;
        if (root != null) {

            // err_no要素のリストを取得
            NodeList errorList = root.getElementsByTagName("err_no");
            // error要素を取得
            Element errorElement = (Element) errorList.item(0);
            // error要素の最初の子ノード（テキストノード）の値を取得
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                NodeList nodeList = root.getElementsByTagName("group");
                
                Database dbHelper = new Database(mContext);
                dbHelper.clearGroups(false);

                ArrayList<GroupChatRoom> groupsList = new ArrayList<GroupChatRoom>();
                for(int i = 0, n = nodeList.getLength(); i < n; ++i) {
                    Element groupNode = (Element) nodeList.item(i);
                    GroupChatRoom g = new GroupChatRoom();
                    XmlHelper.parseGroup(groupNode, g);
                    g.isMeBelongs = true;
                    groupsList.add(g);
                }
                dbHelper.storeGroupChatRooms(groupsList, true);
                errno = 0;
            } else {
                errno = Integer.parseInt(errorStr);

            }
        } else {
            errno = -1;
        }
        return errno;
    }

	/**
	 * 获得所有群组，包括组织结构关系
	 * @param companyId
	 * @return
	 */
	public int getGroupsByCompanyId(String companyId) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd) || TextUtils.isEmpty(companyId)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "get_all_groups_in_corp";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd)
                + "&corp_id=" + Utils.urlencodeUtf8(companyId);

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno;
        if (root != null) {

            // err_no要素のリストを取得
            NodeList errorList = root.getElementsByTagName("err_no");
            // error要素を取得
            Element errorElement = (Element) errorList.item(0);
            // error要素の最初の子ノード（テキストノード）の値を取得
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                NodeList nodeList = root.getElementsByTagName("group");

                Database dbHelper = new Database(mContext);
                dbHelper.clearGroupsForBiz(false);

                ArrayList<GroupChatRoom> chatRooms = new ArrayList<GroupChatRoom>();
                for(int i = 0, n = nodeList.getLength(); i < n; ++i) {
                    Element groupNode = (Element) nodeList.item(i);
                    GroupChatRoom g = new GroupChatRoom();
                    XmlHelper.parseGroup(groupNode, g);
                    chatRooms.add(g);
                }
                dbHelper.storeGroupChatRooms(chatRooms, false);
                errno = 0;
            } else {
                errno = Integer.parseInt(errorStr);
            }
        } else {
            errno = -1;
        }
        return errno;
    }

    /**
     * 获取公司的部门／员工关系，只是id的对应关系，及员工的部分详情
     * @param companyId
     * @return
     */
    public int getCompanyStructure(String companyId) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd) || TextUtils.isEmpty(companyId)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "get_company_structure";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd)
                + "&corp_id=" + Utils.urlencodeUtf8(companyId);

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno;
        if (root != null) {

            // err_no要素のリストを取得
            NodeList errorList = root.getElementsByTagName("err_no");
            // error要素を取得
            Element errorElement = (Element) errorList.item(0);
            // error要素の最初の子ノード（テキストノード）の値を取得
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = 0;

                Database dbHelper = new Database(mContext);

                // root dept
                Element rootDept = Utils.getFirstElementByTagName(root, "root");
                if (null != rootDept) {
                    ArrayList<String> buddyIds = new ArrayList<String>();
                    ArrayList<Buddy> buddies = new ArrayList<Buddy>();
                    String rootDeptId = XmlHelper.parseRootDeptMembers(mContext, rootDept, buddyIds, buddies);

                    // 保存根部门／成员的对应关系
                    dbHelper.storeGroupMemberIds(rootDeptId, buddyIds);

                    // 保存公司员工的部分详情(uid,nickname,status,upload_photo_timestamp)
                    if (!buddies.isEmpty()) {
                        dbHelper.storeBuddies(buddies, false);
                    }
                }

                // normal dept
                NodeList commonDeptNodes = root.getElementsByTagName("group");
                ArrayList<String> groupIds = new ArrayList<String>();
                ArrayList<ArrayList<String>> buddyIdLists = new ArrayList<ArrayList<String>>();
                for (int i = 0; i < commonDeptNodes.getLength(); i++) {
                    Element commonDept = (Element) commonDeptNodes.item(i);
                    ArrayList<String> buddyIds = new ArrayList<String>();
                    String groupId = XmlHelper.parseNormalDeptMemberIds(commonDept, buddyIds);
                    groupIds.add(groupId);
                    buddyIdLists.add(buddyIds);
                }
                dbHelper.storeGroupMemberIds(groupIds, buddyIdLists);
            } else {
                errno = Integer.parseInt(errorStr);
            }
        } else {
            errno = -1;
        }
        return errno;
    }

	/**
	 * 获得收藏的联系人及群组
	 * @return
	 */
	public int getFavoriteContactsAndGroups() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "get_user_favorite_item_list";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd);

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.UNKNOWN;
        if (root != null) {

            // err_no要素のリストを取得
            NodeList errorList = root.getElementsByTagName("err_no");
            // error要素を取得
            Element errorElement = (Element) errorList.item(0);
            // error要素の最初の子ノード（テキストノード）の値を取得
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = 0;
                Element resultElement = Utils.getFirstElementByTagName(root, action); 
                if(resultElement != null) {
                    NodeList nodeList = resultElement.getElementsByTagName("item");
                    Database dbHelper = new Database(mContext);

                    // 清空收藏的关联表
                    dbHelper.clearFavoriteContactsAndGroups();

                    String[] favorites = new String[2];
                    ArrayList<String> favoriteBuddyIds = new ArrayList<String>();
                    ArrayList<String> favoriteGroupIds = new ArrayList<String>();
                    for(int i = 0, n = nodeList.getLength(); i < n; ++i) {
                        Element favoriteNode = (Element) nodeList.item(i);
                        XmlHelper.parseFavoriteContactsAndGrouops(favoriteNode, favorites);
                        // type=0,buddy; type=1,group
                        if ("0".equals(favorites[1])) {
                            favoriteBuddyIds.add(favorites[0]);
                        } else {
                            favoriteGroupIds.add(favorites[0]);
                        }
                    }
                    dbHelper.updateBuddiesFavorite(favoriteBuddyIds, true);
                    dbHelper.updateGroupChatRoomsFavorite(favoriteGroupIds, true);
                }
            } else {
                errno = Integer.parseInt(errorStr);
            }
        } else {
            errno = -1;
        }
        return errno;
    }

	/**
	 * 设置群组的is_favorite属性（是否收藏）
	 * @param groupId
	 * @param isFavorite
	 * @return
	 */
	public int updateGroupFavorite(String groupId, boolean isFavorite) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd) || TextUtils.isEmpty(groupId)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "set_user_favorite_group";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd)
                + "&item=" + Utils.urlencodeUtf8(groupId)
                + "&is_favorite=" + Utils.urlencodeUtf8(isFavorite? "1" : "0");

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno;
        if (root != null) {

            // err_no要素のリストを取得
            NodeList errorList = root.getElementsByTagName("err_no");
            // error要素を取得
            Element errorElement = (Element) errorList.item(0);
            // error要素の最初の子ノード（テキストノード）の値を取得
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                Database dbHelper = new Database(mContext);
                dbHelper.updateGroupChatRoomFavorite(groupId, isFavorite);
                errno = 0;
            } else {
                errno = Integer.parseInt(errorStr);
            }
        } else {
            errno = -1;
        }
        return errno;
	}

	public int sortFavoriteGroups(String[] groupIds) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd) || null == groupIds || groupIds.length == 0) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "set_user_favorite_group_list";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd);
        for (String groupId : groupIds) {
            postStr += "&item[]=" + Utils.urlencodeUtf8(groupId);
        }

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno;
        if (root != null) {

            // err_no要素のリストを取得
            NodeList errorList = root.getElementsByTagName("err_no");
            // error要素を取得
            Element errorElement = (Element) errorList.item(0);
            // error要素の最初の子ノード（テキストノード）の値を取得
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                Database dbHelper = new Database(mContext);
                dbHelper.sortFavoriteGroupChatRooms(groupIds);
                errno = 0;
            } else {
                errno = Integer.parseInt(errorStr);
            }
        } else {
            errno = -1;
        }
        return errno;
    }

	/**
	 * Send a group chat message.
	 * <p>
	 * <b>Note:</b> 
	 * <ul>
	 * <li>will not save the message in db automatically.</li>
	 * <li>these fields will be leave untouched: ioType, sentDate, isGroupChatMessage, sentStatus.</li>
	 * </ul>
	 * @param groupID
	 * @param msg required fields: msgType, messageContent, sentDate.
	 * @return ErrorCode.
	 */
	public int fGroupChat_SendMessage(String groupID, ChatMessage msg) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGroupChat_SendMessage: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String strOutput = String.format("%s%s|{%s}{%s}%s", msg.msgType,
				msg.sentDate, msg.uniqueKey, strUID, msg.messageContent);

		String postStr = String
				.format("action=transfer_group_message&uid=%s&password=%s&group_id=%s&message=%s",
						strUID, strPwd, groupID, Utils.urlencodeUtf8(strOutput));

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		int errno = ErrorCode.OK;
		Database dbHelper = new Database(mContext);
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;
				dbHelper.setChatMessageSent(msg);
			} else {
				errno = Integer.parseInt(errorStr);
				dbHelper.setChatMessageCannotSent(msg);
			}
		} else {
			errno = -1;
			dbHelper.setChatMessageCannotSent(msg);
		}
		return errno;
	}

    /**
     * get offline chat messages since the time(offlineTimestamp).
     * @param offlineTimestamp
     * @return
     */
    public int getOfflineMessages(long offlineTimestamp) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "get_offline_message";
        String postStr = "action=" + action +"&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd);
        if (-1 != offlineTimestamp) {
            postStr += "&timestamp=" + Utils.urlencodeUtf8(String.valueOf(offlineTimestamp));
        }
        // 调用网络接口之前，先将状态flag置false，调用完成后若成功则更新此flag
        sPrefUtil.setOfflineMsgGotSuccess(false);

        int errno = ErrorCode.BAD_RESPONSE;
        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        if (root != null) {

            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();
            if (errorStr.equals("0")) {
                errno = ErrorCode.OK;

                NodeList chatList = root.getElementsByTagName("chat_record");
                ChatMessage chatMessage = null;
                // 此次获取的离线消息中sentdate的最大值，来更新OFFLINE_MSG_TIMESTAMP
                long maxOfflineTimeStamp = offlineTimestamp;
                ArrayList<ChatMessage> offlineMsgs = new ArrayList<ChatMessage>();
                Element chatRecord = null;
                for (int i = 0; i < chatList.getLength(); i++) {
                    chatRecord = (Element) chatList.item(i);
                    chatMessage = new ChatMessage();
                    XmlHelper.parseOfflineMessage(chatRecord, chatMessage, strUID);
                    offlineMsgs.add(chatMessage);
                    maxOfflineTimeStamp = Math.max(maxOfflineTimeStamp,
                            Database.chatMessage_UTCStringToDateLong(chatMessage.sentDate));
                }

                new Database(mContext).storeOfflineMessages(offlineMsgs, true);

                // 离线消息更新成功后
                sPrefUtil.setOfflineMsgGotSuccess(true);
                // 如果时间戳为-1,说明本地原先没有调用过离线接口，使用本地时间作为下次获取的时间戳入参
                if (maxOfflineTimeStamp == -1) {
                    maxOfflineTimeStamp = System.currentTimeMillis()
                            + sPrefUtil.getUTCOffset() * 1000;
                }
                sPrefUtil.setOfflineMsgTimestamp(maxOfflineTimeStamp);
                Log.i("WowTalkWebServerIF#getOfflineMessages, maxTiemStamp is " + maxOfflineTimeStamp);
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }
        return errno;
    }

    /**
     *
     * @param chatTargetUid
     * @return the count of message history of the target_uid
     */
    public HashMap<String, Integer> getChatHistoryCount(String chatTargetUid) {
        HashMap<String, Integer> historyMap = new HashMap<String, Integer>();

        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        if (isAuthEmpty(strUID, strPwd)) {
            historyMap.put("code", ErrorCode.INVALID_ARGUMENT);
            return historyMap;
        }

        String action = "get_chat_history_count";
        String postStr = "action=" + action +"&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd);
        if (!TextUtils.isEmpty(chatTargetUid)) {
            postStr += "&chat_target=" + Utils.urlencodeUtf8(chatTargetUid);
        }

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                historyMap.put("code", ErrorCode.OK);
                Element resultElement = Utils.getFirstElementByTagName(root, action); 
                if(resultElement != null) {
                    Element countElement = Utils.getFirstElementByTagName(resultElement, "chat_record_summary");
                    if(countElement != null) {
                        try {
                            int count = Integer.parseInt(countElement.getTextContent());
                            historyMap.put("value", count);
                        } catch (NumberFormatException exception) {
                            exception.printStackTrace();
                        } catch (DOMException exception) {
                            exception.printStackTrace();
                        }
                    }
                }
            } else {
                historyMap.put("code", Integer.parseInt(errorStr));
            }
        } else {
            historyMap.put("code", -1);
        }
        return historyMap;
    }

	/**
	 * get chat message history
	 * @param isGroupChat 本地数据库保存，是否是群组聊天
	 * @param limit 可选：每次最多获取条数
	 * @param offset 可选：偏移量
	 * @param asc 可选：acs/desc
	 * @param startIndex 可选：从哪一条消息开始获取
	 * @param endIndex 可选：获取到哪条消息
	 * @param chatTargetUid 可选：获取和指定对象uid的聊天记录。如果为空则获取所有聊天记录
	 * @return
	 */
	public HashMap<String, Object> getChatHistory(Boolean isGroupChat, int limit, int offset, boolean asc, String startIndex, String endIndex, String chatTargetUid) {
	    HashMap<String, Object> resultMap = new HashMap<String, Object>();
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        if (isAuthEmpty(strUID, strPwd)) {
            resultMap.put("code", ErrorCode.INVALID_ARGUMENT);
            return resultMap;
        }

        String action = "get_chat_history";
        String postStr = "action=" + action +"&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd);
        if (limit > 0) {
            postStr += "&limit=" + Utils.urlencodeUtf8(String.valueOf(limit));
        }
        if (offset > 0) {
            postStr += "&offset=" + Utils.urlencodeUtf8(String.valueOf(offset));
        }
        postStr += "&asc=" + Utils.urlencodeUtf8(asc ? "1" : "0");
        if (!TextUtils.isEmpty(startIndex)) {
            postStr += "&start_index=" + Utils.urlencodeUtf8(startIndex);
        }
        if (!TextUtils.isEmpty(endIndex)) {
            postStr += "&end_index=" + Utils.urlencodeUtf8(endIndex);
        }
        if (!TextUtils.isEmpty(chatTargetUid)) {
            postStr += "&chat_target=" + Utils.urlencodeUtf8(chatTargetUid);
        }

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        if (root != null) {

            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                resultMap.put("code", ErrorCode.OK);
                Database dbHelper = new Database(mContext);

                NodeList chatList = root.getElementsByTagName("chat_record");
                ChatMessage chatMessage = null;
                int messageId = -1;
                String log = "";
                ArrayList<Integer> downloadedMsgs = new ArrayList<Integer>();
                for (int i = 0; i < chatList.getLength(); i++) {
                    Element chatRecord = (Element) chatList.item(i);
                    chatMessage = new ChatMessage();
                    XmlHelper.parseHistoryChatMessage(isGroupChat, chatRecord, chatMessage, strUID);
                    chatMessage.localHistoryId = offset + i + 1;
                    messageId = dbHelper.storeChatMessageHistory(chatMessage);
                    downloadedMsgs.add(chatMessage.localHistoryId);

                    log = "primary_key is %d, localHistoryItemId is %d, io_type is %s, content is %s.";
                    Log.d("get chatMessage history:", String.format(log, messageId, chatMessage.localHistoryId, chatMessage.ioType, chatMessage.messageContent));
                }
                resultMap.put("value", downloadedMsgs);

            } else {
                resultMap.put("code", Integer.parseInt(errorStr));
            }
        } else {
            resultMap.put("code", -1);
        }
        return resultMap;
	}

	/**
	 * get chat message history
	 * @param isGroupChat
	 * @param limit the counts
	 * @param chatTarget uid or group_id
	 * @return
	 */
	public HashMap<String, Object> getChatHistory(Boolean isGroupChat, int limit, int offset, boolean asc, String chatTarget) {
	    return getChatHistory(isGroupChat, limit, offset, asc, null, null, chatTarget);
	}

    /**
     * 获取信息列表(SmsActivity)
     * @param offset
     * @param limit
     * @param isObserver 是否需要触发数据库监听，在login时需要，在SmsActitiy加载更多时，不需要
     * @return
     */
    public int getLatestChatTargets(int offset, int limit, boolean isObserver) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "get_latest_chat_target";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd);
        if (limit > 0) {
            String limitSql = "";
            if (offset > 0 ) {
                limitSql = offset + ",";
            }
            limitSql += limit;
            postStr += "&limit=" + Utils.urlencodeUtf8(limitSql);
        }

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errorCode = ErrorCode.INVALID_ARGUMENT;
        if (root != null) {

            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errorCode = ErrorCode.OK;
                NodeList chatList = root.getElementsByTagName("chat_record");

                ArrayList<LatestChatTarget> latestChatTargets = new ArrayList<LatestChatTarget>();
                LatestChatTarget target = null;
                for (int i = 0; i < chatList.getLength(); i++) {
                    Element chatRecord = (Element) chatList.item(i);
                    target = new LatestChatTarget();
                    XmlHelper.parseLatestChatTarget(chatRecord, target, strUID);
                    // 没有记录时，服务器会返回<chat_record/>， 需过滤掉
                    if (!TextUtils.isEmpty(target.targetId)) {
                        latestChatTargets.add(target);
                    }
                }
                // 服务器需要过滤已被删除的群组，所以此处不能用返回的条数来判断，只能近似判断
                sPrefUtil.setHasMoreLatestChatTargetInServer(!latestChatTargets.isEmpty());

                Database dbHelper = new Database(mContext);
                dbHelper.storeLatestChatTargets(latestChatTargets, isObserver);
            } else {
                errorCode = Integer.parseInt(errorStr);
            }
        } else {
            errorCode = -1;
        }
        return errorCode;
    }

	/**
	 * Set privacy 
	 * 
	 * @param addBuddyAutomatically
	 *            : true to allow buddy matching
	 * @param peopleCanAddMe
	 *            : 0, don't allow others to add me;
	 *            1, allow others to add me, need to auth;
	 *            2, allow others to add me, don't need to auth
	 * @param unknownPeopleCanCallMe
	 *            : true to allow unknown people to call me
	 * @param unknownPeopleCanMessageMe
	 *            : true to allow unknown people to message me
	 * @param shouldShowMsgDetailInPush
	 *            : true to show message detail in push service
	 * @param listMeInNearbyResult
	 *            : true to list me in nearby result
	 * @return 0: good result -1: no reply others: error code from server
	 */
	public int fSetPrivacy(boolean addBuddyAutomatically,
			int peopleCanAddMe, boolean unknownPeopleCanCallMe,
			boolean unknownPeopleCanMessageMe, boolean shouldShowMsgDetailInPush, boolean listMeInNearbyResult) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fSetPrivacy: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

        String postStr = String
                .format("action=update_privacy_setting&uid=%s&password=%s&add_buddy_automatically=%d&people_can_add_me=%d&unknown_buddy_can_call_me=%d&unknown_buddy_can_message_me=%d&push_show_detail_flag=%d&list_me_in_nearby_result=%d",
                        strUID, strPwd,
                        addBuddyAutomatically ? 1 : 0,
                        peopleCanAddMe,
                        unknownPeopleCanCallMe ? 1 : 0,
                        unknownPeopleCanMessageMe ? 1 : 0,
                        shouldShowMsgDetailInPush ? 1 : 0,
                        listMeInNearbyResult ? 1 : 0);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);
		int errno = 0;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);
			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Get privacy Privacy and save to local db
	 * 
	 * @param NetworkIFDidFinishDelegate
	 * @param status
	 */
	public int fGetPrivacySetting() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGetPrivacySetting: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_privacy_setting&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {
				NodeList nodeList = root
						.getElementsByTagName("add_buddy_automatically");
				Element element = (Element) nodeList.item(0);
				boolean add_buddy_automatically = true;
				if (element != null && element.getFirstChild() != null) {
					add_buddy_automatically = element.getFirstChild()
							.getNodeValue().equals("1");
				}

				nodeList = root.getElementsByTagName("people_can_add_me");
				element = (Element) nodeList.item(0);
				String people_can_add_me = "";
				if (element != null && element.getFirstChild() != null) {
					people_can_add_me = element.getFirstChild().getNodeValue();
				}

				nodeList = root
						.getElementsByTagName("unknown_buddy_can_call_me");
				element = (Element) nodeList.item(0);
				boolean unknown_buddy_can_call_me = false;
				if (element != null && element.getFirstChild() != null) {
					unknown_buddy_can_call_me = element.getFirstChild()
							.getNodeValue().equals("1");
				}

				nodeList = root
						.getElementsByTagName("unknown_buddy_can_message_me");
				element = (Element) nodeList.item(0);
				boolean unknown_buddy_can_message_me = true;
				if (element != null && element.getFirstChild() != null) {
					unknown_buddy_can_message_me = element.getFirstChild()
							.getNodeValue().equals("1");
				}

				nodeList = root.getElementsByTagName("push_show_detail_flag");
				element = (Element) nodeList.item(0);
				boolean push_show_detail_flag = true;
				if (element != null && element.getFirstChild() != null) {
					push_show_detail_flag = element.getFirstChild()
							.getNodeValue().equals("1");
				}

				nodeList = root.getElementsByTagName("list_me_in_nearby_result");
                element = (Element) nodeList.item(0);
                boolean list_me_in_nearby_result = true;
                if (element != null && element.getFirstChild() != null) {
                    list_me_in_nearby_result = element.getFirstChild()
                            .getNodeValue().equals("1");
                }

                // TODO
                sPrefUtil.setAddBuddyAuto(add_buddy_automatically);
                sPrefUtil.setOthersCanAddMe(people_can_add_me);
                sPrefUtil.setUnknownCanCallMe(unknown_buddy_can_call_me);
                sPrefUtil.setUnknownCanMsgMe(unknown_buddy_can_message_me);
                sPrefUtil.setPushShowDetailFlag(push_show_detail_flag);
                sPrefUtil.setListMeInNearbyResult(list_me_in_nearby_result);

                errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);

			}
		} else {
			errno = -1;
		}
		return errno;
	}

	/**
	 * Get Videocall unsupported device list from server and save to local db  
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 * @throws RuntimeException
	 *             when it is called before user name is correctly set
	 */
	public int fGetUnsupportedDeviceList(){
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGetUnsupportedDeviceList: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_videocall_unsupported_device_list&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {

				NodeList nodelist = root.getElementsByTagName("device_number");

				ArrayList<String> deviceNumberList = new ArrayList<String> ();
				for (int i = 0; i < nodelist.getLength(); i++) {
					String deviceNumber = nodelist.item(i).getFirstChild().getNodeValue();
					Log.i(" - get unsupported device:",deviceNumber);
					deviceNumberList.add(deviceNumber);
				}
				Database dbHelper = new Database(mContext);
				dbHelper.deleteAllVideoCallUnsupportedDevices();
				dbHelper.storeVideoCallUnsupportedDevices(deviceNumberList);


				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);

			}
		} else {
			errno = -1;
		}
		return errno;
	}

	public boolean fIsMyDeviceSupportingVideoCall(){
		boolean result = false;
		Database dbHelper = new Database(mContext);
		result = dbHelper.isDeviceNumberSupportedForVideoCall(Build.DEVICE);

		return result;
	}


	public boolean fIsDeviceSupportingVideoCall(String deviceNumber){
		boolean result = false;
		Database dbHelper = new Database(mContext);
		result = dbHelper.isDeviceNumberSupportedForVideoCall(deviceNumber);

		return result;
	}

	/**
	 * Get the current time in secs from 1970/1/1 0:0:0 UTC from server and save the offset comparing to local
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 * @throws RuntimeException
	 *             when it is called before user name is correctly set
	 */
	public int fAdjustUTCTimeWithServer() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fAdjustUTCTimeWithServer: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_server_utc_timestamp&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {

				NodeList nodeList = root.getElementsByTagName("server_utc_time");
				Element element = (Element) nodeList.item(0);
				String strServerUTCTimeInSec = null;
				if (element != null && element.getFirstChild() != null) {
					strServerUTCTimeInSec = element.getFirstChild().getNodeValue();
				}

				if(strServerUTCTimeInSec!=null){
					int timeInServer = Integer.valueOf(strServerUTCTimeInSec);
					int timeInLocal = (int)(System.currentTimeMillis()/1000);
					int timeOffset=timeInServer-timeInLocal;
					Log.i("timeInServer="+timeInServer+",while timeInLocal="+timeInLocal+"; timeOffset="+timeOffset);

                    sPrefUtil.setUTCOffset(timeOffset);
				}


				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);

			}
		} else {
			errno = -1;
		}
		return errno;
	}

    /**
     * get server infos（从中央服务器获取）, 如果返回结果的第二个元素为1，则需要重启wowtalkservcie
     * @return int[2]: 第一个元素标识errorCode,第二个标识sip_domain是否变化(0,不变;1变化,需要重启wowtalkservcie)
     */
    public int[] getServerInfo() {
        int[] result = new int[2];
        String companyId = sPrefUtil.getCompanyId();
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        if(TextUtils.isEmpty(companyId) || isAuthEmpty(strUID, strPwd)) {
            result[0] = ErrorCode.INVALID_ARGUMENT;
            return result;
        }

        final String action = "biz_get_server_info";
        String postStr = "action=" + action
                + "&admin_id=" + Utils.urlencodeUtf8(companyId)
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd);
        Connect2 connect2 = new Connect2(true);
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = 0;

                Element bodyElement = Utils.getFirstElementByTagName(root, "body");
                Element actionElement = Utils.getFirstElementByTagName(bodyElement, "get_server_info");

                Element element = Utils.getFirstElementByTagName(actionElement, "server_utc_time");
                if (null != element) {
                    int timeInServer = Integer.valueOf(element.getTextContent());
                    int timeInLocal = (int)(System.currentTimeMillis() / 1000);
                    int timeOffset = timeInServer - timeInLocal;
                    sPrefUtil.setUTCOffset(timeOffset);
                }
                element = Utils.getFirstElementByTagName(actionElement, "sip_domain");
                if (null != element) {
                    // sip_domain发生变化，需要重启wowtalkservcie
                    String oldSipDomain = sPrefUtil.getSipDomain();
                    String sipDomain = element.getTextContent();
                    result[1] = oldSipDomain.equals(sipDomain) ? 0 : 1;
                    sPrefUtil.setSipDomain(sipDomain);

                    // sip_domain变化了才重新保存sip_password
                    if (result[1] == 1) {
                        element = Utils.getFirstElementByTagName(actionElement, "sip_password");
                        if (null != element) {
                            sPrefUtil.setSipPassword(element.getTextContent());
                        }
                    }
                }
                element = Utils.getFirstElementByTagName(actionElement, "web_domain");
                if (null != element) {
                    sPrefUtil.setWebDomain(element.getTextContent());
                }
                element = Utils.getFirstElementByTagName(actionElement, "useS3");
                if (null != element) {
                    boolean isUseS3 = "1".equals(element.getTextContent());
                    sPrefUtil.setUseS3(isUseS3);
                    if (isUseS3) {
                        element = Utils.getFirstElementByTagName(actionElement, "S3uid");
                        if (null != element) {
                            sPrefUtil.setS3Uid(element.getTextContent());
                        }
                        element = Utils.getFirstElementByTagName(actionElement, "S3pwd");
                        if (null != element) {
                            sPrefUtil.setS3Pwd(element.getTextContent());
                        }
                        element = Utils.getFirstElementByTagName(actionElement, "S3bucket");
                        if (null != element) {
                            sPrefUtil.setS3Bucket(element.getTextContent());
                        }
                    }
                }

            } else {
                errno = Integer.parseInt(errorStr);
            }
        }

        result[0] = errno;
        return result;
    }

	/**
	 * Set Current Active APP Type to server
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 * @throws RuntimeException
	 *             when it is called before user name is correctly set
	 */
	public int fSetActiveAppType(String strAppType){
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

		if (strAppType==null || strAppType.equals("") ) {
//			throw new RuntimeException(
//					"fSetActiveAppType: strAppType not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fSetActiveAppType: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=set_active_app_type&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd) + "&app_type=" + Utils.urlencodeUtf8(strAppType);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {

				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);

			}
		} else {
			errno = -1;
		}
		return errno;
	}


	/**
	 * Get Current Active APP Type from server and save to local
	 * 
	 * @return 0: good result -1: no reply others: error code from server
	 * @throws RuntimeException
	 *             when it is called before user name is correctly set
	 */
	public int fGetActiveAppType(){
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
//			throw new RuntimeException(
//					"fGetActiveAppType: UserID and Password not set");
            return ErrorCode.INVALID_ARGUMENT;
		}

		String postStr = "action=get_active_app_type&uid=" + Utils.urlencodeUtf8(strUID)
				+ "&password=" + Utils.urlencodeUtf8(strPwd);

		Connect2 connect2 = new Connect2();
		Element root = connect2.Post(postStr);

		int errno;
		if (root != null) {

			// err_no要素のリストを取得
			NodeList errorList = root.getElementsByTagName("err_no");
			// error要素を取得
			Element errorElement = (Element) errorList.item(0);
			// error要素の最初の子ノード（テキストノード）の値を取得
			String errorStr = errorElement.getFirstChild().getNodeValue();

			if (errorStr.equals("0")) {

				NodeList nodeList = root
						.getElementsByTagName("active_app_type");
				Element element = (Element) nodeList.item(0);

				if (element != null && element.getFirstChild() != null) {
					String strActiveAppType = element.getFirstChild().getNodeValue();
                    sPrefUtil.setActiveAppType(strActiveAppType);
				}


				errno = 0;
			} else {
				errno = Integer.parseInt(errorStr);

			}
		} else {
			errno = -1;
		}
		return errno;
	}

	// strip and add country code
	public String fTranslatePhoneNumberToGlobalPhoneNumber(String phoneNumber) {

		if (phoneNumber == null) {
			return null;
		}

		String result = fStripPhoneNumber(phoneNumber);
		if (result == null)
			return null;

		// we are sure the length of result is >1 now since length ==0 is
		// returning as nil in fStripPhoneNumber
		if (!result.substring(0, 1).equals("+")) {
			if (result.substring(0, 1).equals("0")) {
				result = result.substring(1);
			}
            result = sPrefUtil.getMyCountryCode() + result;
		}

		return result;
	}
	
	private static void copyFile(File src, File dst) throws IOException {
	    InputStream in = new FileInputStream(src);
	    OutputStream out = new FileOutputStream(dst);

	    // Transfer bytes from in to out
	    byte[] buf = new byte[1024];
	    int len;
	    while ((len = in.read(buf)) > 0) {
	        out.write(buf, 0, len);
	    }
	    in.close();
	    out.close();
	}
	
	private static String fStripPhoneNumber(String phoneNumber) {
		if (phoneNumber == null) {
			return null;
		}
		phoneNumber = phoneNumber.replace(" ", "");
		phoneNumber = phoneNumber.replace("-", "");
		phoneNumber = phoneNumber.replace("(", "");
		phoneNumber = phoneNumber.replace(")", "");

		if (phoneNumber.equals("")) {
			phoneNumber = null;
		}
		return phoneNumber;
	}

	public static String fGetEncriptedPhoneNumber(String s) {
		try {
			s = s+GlobalSetting.SALT_KEY;
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest
					.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++)
				// hexString.append(String.format("%s", Integer.toHexString(0xFF
				// & messageDigest[i])));
				hexString.append(String.format("%02x", messageDigest[i]));
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return "";
	}

    /**
     * Encrypt password.
     * @param plainPassword
     * @return
     */
    public static String fEncryptPassword(String plainPassword) {
        return fGetEncriptedPhoneNumber(plainPassword);
    }

    /**
     * Tell web server that a message has been sent to a public account.
     * @param msg
     */
    public int fNotifyMessageSentToPublicAccount(final ChatMessage msg) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        String strOutput = String.format("%s%s|{%d}{%s}%s", msg.msgType,
                msg.sentDate, msg.primaryKey, strUID, msg.messageContent);

        final String action = "send_msg_to_official_user";

        String msgType;
        String msgContent;
        if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_PHOTO)) {
            msgType = "image";
            msgContent = "(a image)"; // TODO not implemented
        } else if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VIDEO_NOTE)) {
            msgType = "video";
            msgContent = "(a video)"; // TODO not implemented
        } else if (msg.msgType.equals(ChatMessage.MSGTYPE_MULTIMEDIA_VOICE_NOTE)) {
            msgType = "voice";
            msgContent = "(a voice)"; // TODO not implemented
        } else if (msg.msgType.equals(ChatMessage.MSGTYPE_LOCATION)) {
            msgType = "location";
            msgContent = "(a location)"; // TODO not implemented
        } else {
            msgType = "text";
            msgContent = msg.messageContent;
        }

        String postStr = String.format(
                "action=%s&uid=%s&password=%s&official_uid=%s&message_type=%s&message_content=%s",
                action, strUID, strPwd, msg.chatUserName, msgType,
                Utils.urlencodeUtf8(msgContent));

        return _doRequestWithoutResponse(postStr);
    }

    /**
     * Set album cover, which is a photo file.
     * @param file_id
     * @param ext (jpe?g|png|gif|bmp)
     * @return
     */
    public HashMap<String, Object> fSetAlbumCover(String file_id, String ext) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        HashMap<String, Object> resultMap = new HashMap<String, Object>();

        if (isAuthEmpty(strUID, strPwd)) {
            resultMap.put("result_code", ErrorCode.NOT_LOGGED_IN);
            return resultMap;
        }

        if (Utils.isNullOrEmpty(ext) && !ext.matches("\\.?(jpe?g|png|gif|bmp)")) {
            resultMap.put("result_code", ErrorCode.INVALID_ARGUMENT);
            return resultMap;
        }

        if (ext.charAt(0) == '.') {
            ext = ext.substring(1);
        }

        String postStr = String.format("action=set_album_cover&uid=%s&password=%s&file_id=%s&ext=%s",
                strUID, strPwd, Utils.urlencodeUtf8(file_id), Utils.urlencodeUtf8(ext));
        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        if (root != null) {
            Element errorElement = Utils.getFirstElementByTagName(root, "err_no");
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                resultMap.put("result_code", 0);

                Element resultElement = Utils.getFirstElementByTagName(root, "set_album_cover");
                if(resultElement != null) {
                    Element e = Utils.getFirstElementByTagName(resultElement, "update_timestamp");
                    if (e != null) {
                        long timestamp = Utils.tryParseLong(e.getTextContent(), 0);
                        resultMap.put("update_timestamp", timestamp);
                    }
                }
            } else {
                resultMap.put("result_code", Integer.parseInt(errorStr));
            }
        } else {
            resultMap.put("result_code", ErrorCode.BAD_RESPONSE);
        }
        return resultMap;
    }

    /**
     * remove album cover, set the update_timestamp to "-1" in the server.
     * @return
     */
    public int removeAlbumCover() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        String postStr = String.format("action=remove_album_cover&uid=%s&password=%s",
                strUID, strPwd);
        return _doRequestWithoutResponse(postStr);
    }

    /**
     * Get album cover.
     * @param uid
     * @param result
     * @return
     */
    public int fGetAlbumCover(String uid, AlbumCover result) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        if (Utils.isNullOrEmpty(uid)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "get_album_cover";
        String postStr = String.format("action=%s&uid=%s&password=%s",
                action, strUID, strPwd);
        if (uid != null) {
            postStr += "&owner_uid=" + uid;
        }

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {
            Element errorElement = Utils.getFirstElementByTagName(root, "err_no");
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                errno = 0;

                Element resultElement = Utils.getFirstElementByTagName(root, action);
                if(resultElement != null) {
                    Element e = Utils.getFirstElementByTagName(resultElement, "file_id");
                    if (e != null) {
                        result.fileId = e.getTextContent();
                    }

                    e = Utils.getFirstElementByTagName(resultElement, "ext");
                    if (e != null) {
                        result.ext = e.getTextContent();
                    }

                    e = Utils.getFirstElementByTagName(resultElement, "update_timestamp");
                    if (e != null) {
                        result.timestamp = Utils.tryParseLong(e.getTextContent(), 0);
                    }
                }
            } else {
                errno = Integer.parseInt(errorStr);
            }
        }
        return errno;
    }

    /**
     * Operate settings on buddy, such as block msg notification, add to favorite.
     *
     * @param buddy
     * @return
     */
    public int fOperateBuddy(Buddy buddy) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        if (buddy == null || Utils.isNullOrEmpty(buddy.userID)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "opt_buddy";
        String postStr = String.format("action=%s&uid=%s&password=%s"
                + "&buddy_id=%s"
                + "&block_msg=%d&block_msg_notification=%d&favorite=%d",
                action, strUID, strPwd,
                buddy.userID,
                buddy.willBlockMsg ? 1 : 0,
                buddy.willBlockNotification ? 1 : 0,
                buddy.favorite ? 1 : 0
        );
        if (buddy.alias != null) {
            postStr += "&alias=" + Utils.urlencodeUtf8(buddy.alias);
        }

        return _doRequestWithoutResponse(postStr);
    }

    /**
     * 更新buddy的常用联系人属性
     * @param isFavorite
     * @return
     */
    public int updateBuddyFavorite(String buddyId, boolean isFavorite) {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        if (TextUtils.isEmpty(buddyId)) {
            return ErrorCode.INVALID_ARGUMENT;
        }

        String action = "set_user_favorite_buddy";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd)
                + "&item=" + Utils.urlencodeUtf8(buddyId)
                + "&is_favorite=" + Utils.urlencodeUtf8(isFavorite? "1" : "0");

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno;
        if (root != null) {

            // err_no要素のリストを取得
            NodeList errorList = root.getElementsByTagName("err_no");
            // error要素を取得
            Element errorElement = (Element) errorList.item(0);
            // error要素の最初の子ノード（テキストノード）の値を取得
            String errorStr = errorElement.getFirstChild().getNodeValue();

            if (errorStr.equals("0")) {
                Database dbHelper = new Database(mContext);
                dbHelper.updateBuddyFavorite(buddyId, isFavorite);
                errno = 0;
            } else {
                errno = Integer.parseInt(errorStr);
            }
        } else {
            errno = -1;
        }
        return errno;
    }

    public int fGetPendingRequests() {
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();

        if (isAuthEmpty(strUID, strPwd)) {
            return ErrorCode.NOT_LOGGED_IN;
        }

        String action = "get_my_pending_requests";
        String postStr = String.format("action=%s&uid=%s&password=%s",
                action, strUID, strPwd);

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
            try {
                Database db = new Database(mContext);
                db.clearPendingRequests();

                Element body = Utils.getFirstElementByTagName(root, action);

                // parse group requests
                String[] requestNodeNames = new String[] { "group_in", "group_out", "group_admin" };
                int[] requestTypes = new int[] {
                        PendingRequest.GROUP_IN,
                        PendingRequest.GROUP_OUT,
                        PendingRequest.GROUP_ADMIN };
                for (int typei = 0; typei < 3; ++typei) {
                    Element requestParent = Utils.getFirstElementByTagName(body, requestNodeNames[typei]);
                    if (null != requestParent) {
                        NodeList rs = requestParent.getElementsByTagName("request");
                        int n = rs == null ? 0 : rs.getLength();
                        for (int i = 0; i < n; ++i) {
                            Element r = (Element)rs.item(i);
                            PendingRequest pr = new PendingRequest(requestTypes[typei]);
                            pr.group_id = Utils.getFirstTextByTagName(r, "group_id");
                            pr.group_name = Utils.getFirstTextByTagName(r, "group_name");
                            pr.msg = Utils.getFirstTextByTagName(r, "msg");
                            pr.group_photo_timestamp = Utils.getFirstLongByTagName(r, "group_photo_timestamp", 0);
                            pr.uid = Utils.getFirstTextByTagName(r, "uid");
                            pr.nickname = Utils.getFirstTextByTagName(r, "nickname");
                            pr.buddy_photo_timestamp = Utils.getFirstLongByTagName(r, "buddy_photo_timestamp", 0);
                            db.storePendingRequest(pr);
                        }
                    }
                }

                // parse buddy requests
                requestNodeNames = new String[] { "buddy_in", "buddy_out" };
                requestTypes = new int[] {
                        PendingRequest.BUDDY_IN,
                        PendingRequest.BUDDY_OUT };
                for (int typei = 0; typei < 2; ++typei) {
                    Element requestParent = Utils.getFirstElementByTagName(body, requestNodeNames[typei]);
                    if (null != requestParent) {
                        NodeList rs = requestParent.getElementsByTagName("request");
                        int n = rs == null ? 0 : rs.getLength();
                        for (int i = 0; i < n; ++i) {
                            Element r = (Element)rs.item(i);
                            PendingRequest pr = new PendingRequest(requestTypes[typei]);
                            pr.uid = Utils.getFirstTextByTagName(r, "uid");
                            pr.nickname = Utils.getFirstTextByTagName(r, "nickname");
                            pr.buddy_photo_timestamp = Utils.getFirstLongByTagName(r, "photo_timestamp", 0);
                            pr.msg = Utils.getFirstTextByTagName(r, "msg");
                            db.storePendingRequest(pr);
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return errno;
    }

    public int fCheckForUpdates(UpdatesInfo result) {
        if (null == result)
            return ErrorCode.INVALID_ARGUMENT;

        String action = "check_for_updates";
        String postStr = "action=" + action
                + "&device_type=android"
                + "&lang=" + Locale.getDefault().getLanguage();

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        int errno = ErrorCode.BAD_RESPONSE;
        if (root != null) {
            Element errorElement = Utils.getFirstElementByTagName(root, "err_no");
            errno = Utils.tryParseInt(errorElement.getTextContent(), ErrorCode.BAD_RESPONSE);
            if (errno == ErrorCode.OK) {
                try {
                    Element body = Utils.getFirstElementByTagName(root, "android");
                    XmlHelper.parseUpdatesInfo(body, result);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return errno;
    }

    /**
     * Whether the member belongs to the group.
     * @param memberId member id
     * @param groupID group id
     * @return true, belongs; false, not
     */
    public boolean fIsBelongsToGroup(String memberId, String groupID) {
        boolean isBelongs = false;
        String strUID = sPrefUtil.getUid();
        String strPwd = sPrefUtil.getPassword();
        if (isAuthEmpty(strUID, strPwd)) {
            return isBelongs;
        }

        String action = "is_group_member";
        String postStr = "action=" + action
                + "&uid=" + Utils.urlencodeUtf8(strUID)
                + "&password=" + Utils.urlencodeUtf8(strPwd)
                + "&group_id=" + Utils.urlencodeUtf8(groupID)
                + "&buddy_id=" + Utils.urlencodeUtf8(memberId);

        Connect2 connect2 = new Connect2();
        Element root = connect2.Post(postStr);

        if (root != null) {
            NodeList errorList = root.getElementsByTagName("err_no");
            Element errorElement = (Element) errorList.item(0);
            String errorStr = errorElement.getFirstChild().getNodeValue();
            if (errorStr.equals("0")) {
                String belongsToGroup = Utils.getFirstTextByTagName(root, "value");
                isBelongs = "1".equals(belongsToGroup);
            }
        }
        return isBelongs;
    }
}
