package co.onemeter.oneapp.utils;

import android.content.Context;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.PrefUtil;

import java.util.Calendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Utils {

    public final static String QR_CODE_KEY_LABEL="label";
    public final static String QR_CODE_KEY_DEF_VALUE_LABEL="WTH_QR_CODE_CHECK";

    public final static String QR_CODE_KEY_TYPE="type";
    public final static String QR_CODE_KEY_DEF_VALUE_TYPE_BUDDY ="0";
    public final static String QR_CODE_KEY_DEF_VALUE_TYPE_INVITECODE ="1";
    public final static String QR_CODE_KEY_DEF_VALUE_TYPE_GROUP="2";
    public final static String QR_CODE_KEY_DEF_VALUE_TYPE_EVENT="3";
    public final static String QR_CODE_KEY_DEF_VALUE_TYPE_PUBLIC_ACCOUNT="4";

    public final static String QR_CODE_KEY_CONTENT="content";
    public final static String QR_CODE_KEY_UID="uid";
    public final static String QR_CODE_KEY_INVITECODE="invitecode";
    public final static String QR_CODE_KEY_TIMESTAMP="timestamp";
    public final static String QR_CODE_KEY_GROUP_ID="group_id";
    public final static String QR_CODE_KEY_CREATOR_ID="creator_id";
    public final static String QR_CODE_KEY_EVENT_ID="event_id";
    public final static String QR_CODE_KEY_PUBLIC_ACCOUNT_ID="public_account_id";
    /**
     * 可以使用2-20个字母，数字，下划线和减号
     * @param username
     * @return
     */
    public static boolean verifyUsername(String username) {
        String pattern = "[a-zA-Z0-9_-]{2,20}";
        return verify(pattern, username);
    }

    /**
     * 可以使用6-20个字母，数字，下划线和减号
     * @param password
     * @return
     */
    public static boolean verifyWowTalkPwd(String password) {
        String pattern = "[a-zA-Z0-9_-]{6,20}";
        return verify(pattern, password);
    }

    /**
     * verify the format of the email.
     * @param strEmail
     * @return
     */
    public static boolean verifyEmail(String strEmail) {
        String pattern = "^([a-zA-Z0-9_\\.\\-])+\\@(([a-zA-Z0-9\\-])+\\.)+([a-zA-Z0-9]{2,4})+$";
        return verify(pattern, strEmail);
    }

    /**
     * verify mobile phone number, 使用"+"(可能没有) 和 6-15个数字
     * @param phoneNumber
     * @return
     */
    public static boolean verifyPhoneNumber(String phoneNumber) {
        String pattern = "(\\+)?[0-9]{6,15}";
        return verify(pattern, phoneNumber);
    }

    /**
     * verify inner phone number, 使用"+"(可能没有) 和 6-15个数字
     * @param phoneNumber
     * @return
     */
    public static boolean verifyInnerPhoneNumber(String phoneNumber) {
        String pattern = "(\\+)?[0-9]{1,15}";
        return verify(pattern, phoneNumber);
    }


    private static boolean verify(String patternString, String value) {
        Pattern pattern = Pattern.compile(patternString);
        Matcher matcher = pattern.matcher(value);
        return matcher.matches();
    }

    /**
     * Match the name with filterString
     * @param isIgnorecase whether ignorecase or not
     * @param filterString filter characters
     * @param targetString the target strings
     * @return true, match filter; false, not
     */
    public static boolean isMatchFilters(boolean isIgnorecase, String filterString, String targetString) {
        char[] filters = null;
        char[] names = null;
        if (isIgnorecase) {
            filters = filterString.toLowerCase().toCharArray();
            names = targetString.toLowerCase().toCharArray();
        } else {
            filters = filterString.toCharArray();
            names = targetString.toCharArray();
        }

        int first = 0;
        boolean isMatch = false;
        for (int i = 0; i < filters.length; i++) {
            for (int j = first; i < names.length; j++) {
                if ((filters.length - i) > (names.length - j)) {
                    return isMatch;
                } else {
                    if (names[j] == filters[i]) {
                        first = j + 1;
                        break;
                    }
                }
                first = j + 1;
            }
            if (first == names.length && i < (filters.length - 1)) {
                return isMatch;
            }
        }

        if (first == 0 || (filters[filters.length - 1] != names[first - 1])) {
            isMatch = false;
        } else {
            isMatch = true;
        }
        return isMatch;
    }
    
    public static boolean isAccoTeacher(Context context){
    	if(PrefUtil.getInstance(context).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER){
    		return true;
    	}
    	return false;
    }
    
    /*
     * 获取当天即0点的时间戳
     */
    public static long getDayStampMoveMillis(){
    	Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.MILLISECOND, 0);
		return cal.getTimeInMillis() / 1000;
    }
}
