package co.onemeter.oneapp.liveplayer;

import android.text.TextUtils;
import android.util.Base64;

public class DES {

	/**
	 * 加密/解密方法
	 * @param content 需要加/解密的字符串
	 * @param operation "DECODE"加密 ， "ENCODE"解密
	 * @return
	 */
	public static String authcode(String content, String operation) {
		String result = null;
		if (operation != null && operation.equals("DECODE")) {
			if (!TextUtils.isEmpty(content)) {
				result = mEncrypt(content);
			}
		} else if (operation != null && operation.equals("ENCODE")) {
			if (!TextUtils.isEmpty(content)) {
				result = mDecrypt(content);
			}
		}
		return result;
	}

	private static String mEncrypt(String content) {
		String encodeToString = Base64.encodeToString(content.getBytes(), Base64.DEFAULT);
		StringBuilder sb = new StringBuilder();
		sb.append("http://www.onemeter.co/").append(encodeToString.trim()).append("ShMetowEr");
		return sb.toString();
	}

	private static String mDecrypt(String content) {
		String trim = content.replace("http://www.onemeter.co/", "").replace("ShMetowEr", "").trim();
		StringBuilder sb = new StringBuilder();
		byte[] decode = Base64.decode(trim, Base64.DEFAULT);
		String s = new String(decode);
		sb.append("https://pcs.baidu.com/rest/2.0/pcs/device?method=liveplay&shareid=").append(s);
		return sb.toString();
	}
}