package org.wowtalk.api;

import android.content.Context;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;

import org.wowtalk.Log;

public class PinyinHelper {
    private static PinyinHelper theObj = null;
    private int mDataResid = 0;
    private HashMap<String, String> pinyin = null;

    private PinyinHelper() {
    }

    /**
     * Single instance.
     * @return
     */
    public static PinyinHelper instance() {
        if (theObj == null) {
            theObj = new PinyinHelper();
        }
        return theObj;
    }

    /**
     * init data source.
     * @param context Context
     * @param dataResid Resource ID of raw pinyin data file, e.g., R.raw.pinyin.
     */
    public void init(Context context, int dataResid) {
        mDataResid = dataResid;
        // 保存此mDataResid，有时类中的这个变量会被清除，需要重新获取
        PrefUtil.getInstance(context).setPinYinResId(dataResid);
    }

    /**
     * PinyinHelper.init() should be called before this.
     *
     * @param context
     * @param text
     * @param withAbbr
     * @return
     */
    public String getPinyin(Context context, String text, boolean withAbbr)
    {
        if(TextUtils.isEmpty(text)) {
            return "";
        }
        if(pinyin == null)
        {
            loadPinyin(context);
        }
        if (pinyin == null)
            return text;

        String rtn = "", rtn2 = "";
        for(int i = 0; i < text.length(); ++i)
        {
            char c = text.charAt(i);
            if (('A'<= c && c<='Z') || ('a'<=c && c<='z')) {
                rtn += Character.toString(c).toLowerCase();
            } else if (Character.isDigit(c)) {
                rtn += c;
            } else {
                String s = pinyin.get(encode(c));
                if(s != null && s.length() > 0)
                {
                    rtn += s;
                    if(withAbbr)
                        rtn2 += s.charAt(0);
                }
            }
        }
        if(rtn.equals("")) // 纯英文？
            rtn = text.toLowerCase();
        return rtn + " " + rtn2;
    }

    /**
     * 从资源文件加载拼音数据。
     *
     * @param context
     * @throws IOException
     */
    private synchronized void loadPinyin (Context context) {
        InputStreamReader isr = null;

        try {
            if (pinyin != null) {
                return;
            }
            if (mDataResid == 0) {
                Log.d("the mDataResid is 0, get it from sp again!");
                mDataResid = PrefUtil.getInstance(context).getPinyinResId();
                if (mDataResid == 0) {
                    Log.e("the mDataResid saved in sp is 0 !!!");
                    return;
                }
            }

            pinyin = new HashMap<String, String>();

            isr = new InputStreamReader (
                    context.getResources().openRawResource(mDataResid));

            if(isr == null)
                return;;

            BufferedReader br = new BufferedReader(isr);

            String s, key, val;
            while ((s = br.readLine()) != null) {
                if(s.length() > 1)
                {
                    key = encode(s.substring(0, 1).charAt(0));
                    val = s.substring(1);
                    if(pinyin.containsKey(key))
                        pinyin.put(key, pinyin.get(key) + val);
                    else
                        pinyin.put(key, val);
                }
            }
            isr.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // 把汉字编码为数字形式。然后才能作为hashmap的key。
    private static String encode(char c) {
        return String.format("%04x", (int) c);
    }
}
