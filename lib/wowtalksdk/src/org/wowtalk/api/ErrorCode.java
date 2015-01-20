package org.wowtalk.api;

import android.content.Context;

public class ErrorCode {
	/** Error Code */
	public static final int OK = 0;
	/** Error Code */
	public static final int INVALID_ARGUMENT = 1;
	/** Error Code */
	public static final int AUTH = 2;
	/** Error Code */
	public static final int DB = 3;
	/** Error Code: ? */
	public static final int MAINTE = 4;
	/** Error Code: Version not compatible. */
	public static final int VER_NOT_COMPATIBLE = 5;
	/** Error Code: User with the same ID already exists. */
	public static final int USER_ALREADY_EXISTS = 6;
	/** Error Code */
	public static final int NOT_CREATOR = 7;
	/** Error Code: Initialized password must be changed. */ 
	public static final int PASSWORD_NOT_CHANGED = 8;
	/** Error Code: Wowtalk ID can be changed only once. */
	public static final int WOWID_NOT_CHANGED = 9;
	/** Error Code */
	public static final int GROUP_NOT_EXISTS = 10;
	/** Error Code */
	public static final int ACTIVITY_NOT_EXISTS = 11;
	/** Error Code */
	public static final int USER_TYPE_NOT_MATCH = 12;
	/** Error Code */
	public static final int MOMENT_NOT_EXISTS = 13;
	/** Error Code */
	public static final int REVIEW_NOT_EXISTS = 14;
    /** Error Code */
	public static final int PHONE_VERIFICATION_CODE_ERROR = 20;
    /** Error Code */
    public static final int EMAIL_VERIFICATION_CODE_ERROR = 21;
    /** Error Code */
    public static final int ACCESS_CODE_ERROR = 22;
    /** Error Code: the email you trying to bind has been bound
     * by others. */
    public static final int EMAIL_USED_BY_OTHERS = 28;
    /** Error Code: the phone number you trying to bind has been bound
     * by others. */
    public static final int PHONE_USED_BY_OTHERS = 29;

    public static final int ERR_OPERATION_DENIED = 37;
    /**
     * timeline 投票已过期
     */
    public static final int MOMENT_SURVEY_OUTOFDATE = 41;
    /**
     * 投票已经在web端或其他客户端投过，重复投票
     */
    public static final int MOMENT_SURVEY_HAS_VOTED = 42;

	/** Error Code: Bad or no response from remote server. */
	public static final int BAD_RESPONSE = -1;
	/** Error Code: User does not exists. */
	public static final int USER_NOT_EXISTS = -99;
	/** Error Code: Unknown error. */
	public static final int UNKNOWN = 99;
    /** Error Code: Group pending in request already processed, most probably by other admins. */
    public static final int PENDING_REQUEST_ALREADY_PROCESSED = 1003;

    private static final int LOCAL_ERROR_BASE = 10000;
    public static final int NOT_LOGGED_IN = LOCAL_ERROR_BASE + 1;
    public static final int ILLEGAL_OPERATION = LOCAL_ERROR_BASE + 2;
    public static final int OPERATION_FAILED = LOCAL_ERROR_BASE + 3;
	/** Local error: upload failed. */
	public static final int LOCAL_UPLOAD_FAILED = LOCAL_ERROR_BASE + 4;

    public static final int FORGET_PWD_EMAIL_NOT_BOUND = 1101;
    public static final int FORGET_PWD_PHONE_NOT_BOUND = 1102;
    public static final int FORGET_PWD_EMAIL_NOT_MATCH = 1103;
    public static final int FORGET_PWD_PHONE_NOT_MATCH = 1104;
    public static final int FORGET_PWD_EMAIL_PHONE_NOT_BOUND = 1105;

	public final static int ERR_INVITATION_CODE_NOT_EXIST = -98;         //INVITATION_CODE not exist error
	public final static int ERR_SCHOOL_USER_HAD_BOUND = -97;         //INVITATION_CODE HAD BOUND error
	public final static int ERR_EXPIRED_INVITATION_CODE = -96;           //INVITATION_CODE EXPIRED error
	public final static int ERR_BOUND_SAME_SCHOOL_USER = -95;         //same school's user HAD BOUND error
	public final static int ERR_SCHOOL_USER_TYPE_NOT_MATCH = -94;        //school user_type not matched
	public final static int ERR_SCHOOL_MEMBERS_GET = 1004;      //not bind INVITATION_CODE or user not student or teacher

	public static String getErrorName(Context context, int errno) {
		switch (errno) {
			case ErrorCode.OK:
				return "OK";
			case ErrorCode.BAD_RESPONSE:
				return "ERR_BAD_RESPONSE";
			case ErrorCode.INVALID_ARGUMENT:
				return "ERR_INVALID_ARGUMENT";
			case ErrorCode.AUTH:
				return "ERR_AUTH";
			case ErrorCode.DB:
				return "ERR_DB";
			case ErrorCode.VER_NOT_COMPATIBLE:
				return "ERR_VER_NOT_COMPATIBLE";
			case ErrorCode.USER_NOT_EXISTS:
				return "ERR_USER_NOT_EXISTS";
			case ErrorCode.USER_ALREADY_EXISTS:
				return "ERR_USER_ALREADY_EXISTS";
			case ErrorCode.PASSWORD_NOT_CHANGED:
				return "ERR_PASSWORD_NOT_CHANGED";
			case ErrorCode.WOWID_NOT_CHANGED:
				return "ERR_WOWID_NOT_CHANGED";
			case ErrorCode.GROUP_NOT_EXISTS:
				return "ERR_GROUP_NOT_EXISTS";
			case ErrorCode.NOT_CREATOR:
				return "ERR_NOT_CREATOR";
			case ErrorCode.UNKNOWN:
				return "ERR_UNKNOWN";
			case ERR_INVITATION_CODE_NOT_EXIST:
				return context.getString(R.string.ERR_INVITATION_CODE_NOT_EXIST);
			case ERR_SCHOOL_USER_HAD_BOUND:
				return context.getString(R.string.ERR_SCHOOL_USER_HAD_BOUND);
			case ERR_EXPIRED_INVITATION_CODE:
				return context.getString(R.string.ERR_EXPIRED_INVITATION_CODE);
			case ERR_BOUND_SAME_SCHOOL_USER:
				return context.getString(R.string.ERR_BOUND_SAME_SCHOOL_USER);
			case ERR_SCHOOL_USER_TYPE_NOT_MATCH:
				return context.getString(R.string.ERR_SCHOOL_USER_TYPE_NOT_MATCH);
			case ERR_SCHOOL_MEMBERS_GET:
				return context.getString(R.string.ERR_SCHOOL_MEMBERS_GET);
			default:
				return context.getString(R.string.ERR_UNKNOWN);
		}
	}

}
