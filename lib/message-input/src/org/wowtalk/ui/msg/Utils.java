/**
 * 字符串处理等基础功能。
 */
package org.wowtalk.ui.msg;

import java.lang.reflect.Field;
import java.util.ArrayList;

import android.app.AlertDialog;
import android.content.Context;

public class Utils {
	// 截断长字符串，追加"..."
	public static String abbr(String src, int maxlen)
	{
		if(maxlen <= 3)
			return src;

		int n = src.length();
		if(n <= maxlen)
			return src;
		else
			return src.substring(0, maxlen - 3) + "...";
	}

	// 不抛异常的 url encoder
	public static String urlencode_utf8(String s)
	{
		try {
			return java.net.URLEncoder.encode(s, "UTF-8");
		}
		catch(java.io.UnsupportedEncodingException e)
		{
			return "";
		}
	}
	/**
	 * 从字符串中提取出超链接。
	 * @param text
	 * @return
	 */
	public static String[] extractHyperLinks(String text)
	{
		ArrayList<String> links = new ArrayList<String>();
		int len = text.length();
		int p = -1;
		String prevLink = null;
		for(int i = 0; i < len; ++i)
		{
			if(p >= 0 && (text.charAt(i) == ' ' 
					|| text.charAt(i) == '\"' 
					|| text.charAt(i) == '\''
					|| text.charAt(i) == '<'))
			{
				String link = text.substring(p, i);
				if(prevLink == null || !prevLink.equals(link)) {
					if(link.startsWith("http://"))
						links.add(link);
					else
						links.add("http://" + link);
				}
				prevLink = link;
				p = -1;
				continue;
			}

			if(p == -1 && (text.startsWith("http://", i) || text.startsWith("www.", i)))
			{
				p = i;
				continue;
			}			
		}
		if(p >= 0) {
			String link = text.substring(p);
			if(prevLink == null || !prevLink.equals(link))
			{
				if(link.startsWith("http://"))
					links.add(link);
				else
					links.add("http://" + link);
			}
		}
		return links.toArray(new String[links.size()]);
	}

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
		return phones.toArray(new String[phones.size()]);
	}

	public static int tryParseInt(String s, int defVal)
	{
		if(s == null || s.equals("")) return defVal;

		try {
			return Integer.parseInt(s);
		} catch (Exception e) {
			return defVal;
		}
	}

	public static int tryParseInt(String s)
	{
		return tryParseInt(s, 0);
	}

	public static long tryParseLong(String s, long defVal)
	{
		if(s == null || s.equals("")) return defVal;

		try {
			return Long.parseLong(s);
		} catch (Exception e) {
			return defVal;
		}
	}

	public static long tryParseLong(String s)
	{
		return tryParseLong(s, 0);
	}
	
	public static double tryParseDouble(String s, double defVal)
	{
		if(s == null || s.equals("")) return defVal;

		try {
			return Double.parseDouble(s);
		} catch (Exception e) {
			return defVal;
		}
	}

	public static double tryParseDouble(String s)
	{
		return tryParseDouble(s, 0);
	}

	public static boolean isNullOrEmpty(String s)
	{
		return s == null || s.equals("");
	}
	
	/**
	 * 规范化手机号码。
	 * @param s
	 * @return 规范化后的手机号码。如果输入的不是有效的手机号码，则返回 null 。
	 */
	public static String normalizeMobilePhone(String s) 
	{
		if(s == null || s.equals(""))
			return null;
		
		if(s.matches("^(\\+86|0)?1[0-9]{10}$"))
			return s.substring(s.length() - 11);
		
		String rtn = "";
		for(int i = 0, n = s.length(); i < n; ++i) {
			if(Character.isDigit(s.charAt(i)) || s.charAt(i) == '+') {
				rtn += s.charAt(i);
			}
		}
		
		if(rtn.matches("^(\\+86|0)?1[0-9]{10}$"))
			return rtn.substring(rtn.length() - 11);		
		
		return null;
	}
	
	/**
	 * show alert dialog.
	 * @param context
	 * @param msg
	 */
	public static void alert(Context context, int msgResId) {
		AlertDialog a = new AlertDialog.Builder(context)
		.setMessage(msgResId)
		.create();
		a.setCanceledOnTouchOutside(true);
		a.show();
	}

	/**
	 * dumps the content of a object to String.
	 * <p>
	 * {@link http://code.google.com/p/dump-to-string/}
	 */
    public static String dump(Object object) {
        Field[] fields = object.getClass().getDeclaredFields();
        StringBuilder sb = new StringBuilder();
        sb.append(object.getClass().getSimpleName()).append('{');

        boolean firstRound = true;

        for (Field field : fields) {
            if (!firstRound) {
                sb.append(", ");
            }
            firstRound = false;
            field.setAccessible(true);
            try {
                final Object fieldObj = field.get(object);
                final String value;
                if (null == fieldObj) {
                    value = "null";
                } else {
                    value = fieldObj.toString();
                }
                sb.append(field.getName()).append('=').append('\'')
                        .append(value).append('\'');
            } catch (IllegalAccessException ignore) {
                //this should never happen
            }

        }

        sb.append('}');
        return sb.toString();
    }
}
