package org.wowtalk.helper;

import co.onemeter.oneapp.utils.Utils;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-6-19
 * Time: PM1:47
 * To change this template use File | Settings | File Templates.
 */
public class PhoneNumberHelper {

    /**
     * 从字符串中提取出电话号码。
     * @param text
     * @return
     */
    public static String[] extractPhoneNumbers(String text)
    {
        ArrayList<String> phones = new ArrayList<String>();
        int len = text.length();
        int p = -1;
        String prev = null;
        for(int i = 0; i < len; ++i)
        {
            if(p >= 0 && !Character.isDigit(text.charAt(i)))
            {
                if(i - p >= 5) {
                    String curr = text.substring(p, i);
                    if(prev == null || !prev.equals(curr)) {
                        phones.add(curr);
                        prev = curr;
                    }
                }
                p = -1;
                continue;
            }

            if(p == -1 && Character.isDigit(text.charAt(i)))
            {
                p = i;
                continue;
            }
        }
        if(p >= 0 && len - p >= 5) {
            String curr = text.substring(p);
            if(prev == null || !prev.equals(curr)) {
                phones.add(text.substring(p));
            }
        }

        ArrayList<String> phones2ret = new ArrayList<String>();
        for(String aPhone : phones) {
            if(Utils.verifyPhoneNumber(aPhone)) {
                phones2ret.add(aPhone);
            }
        }
        return phones2ret.toArray(new String[phones2ret.size()]);
    }

}
