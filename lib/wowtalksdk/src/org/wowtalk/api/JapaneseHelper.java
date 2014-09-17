package org.wowtalk.api;

import java.io.IOException;

import android.content.Context;
import android.text.TextUtils;

import com.kawao.kakasi.Kakasi;

/**
 * Convert Japanese kanji into romaji
 *
 */
public class JapaneseHelper {
    private static JapaneseHelper theObj = null;
    /**
     * 日文字典
     */
    public static String JA_DICTIONARY_PATH_PRE = "/data/data/";
    public static String JA_DICTIONARY_KANWA_PATH_SUFFIX = "/files/kanwadict";
    public static String JA_DICTIONARY_ITAIJI_PATH_SUFFIX = "/files/itaiji";

    private Kakasi mKakasi;

    private JapaneseHelper() {
    }

    /**
     * Single instance.
     * @return
     */
    public static JapaneseHelper instance() {
        if (theObj == null) {
            theObj = new JapaneseHelper();
        }
        return theObj;
    }

    /**
     * Convert Japanese into romaji
     *
     * @param text the specific Japanese to convert
     * @return romaji
     */
    public String getKey(Context context, String text)
    {
        if(TextUtils.isEmpty(text)) {
            return "";
        }

        String romaji = text;
        String suffix = "";
        if (null == mKakasi) {
            initKakasi(context);
        }
        try {
            romaji = mKakasi.doString(text);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
        for (int i = 0; i < text.length(); i++) {
            suffix += getAscii(text);
        }

        return romaji.toLowerCase() + "" + suffix;
    }

    public static void setJapaneseDict(Context context) {
        Kakasi.setKanwaDictionaryPath(JA_DICTIONARY_PATH_PRE + context.getPackageName()
                + JA_DICTIONARY_KANWA_PATH_SUFFIX);
        Kakasi.setItaijiDictionaryPath(JA_DICTIONARY_PATH_PRE + context.getPackageName()
                + JA_DICTIONARY_ITAIJI_PATH_SUFFIX);
    }

    private void initKakasi(Context context) {
        mKakasi = new Kakasi();
        mKakasi.setupKanjiConverter(Kakasi.ASCII);
        mKakasi.setupHiraganaConverter(Kakasi.ASCII);
        mKakasi.setupKatakanaConverter(Kakasi.ASCII);
        setJapaneseDict(context);
    }

    private String getAscii(String text) {
        String ascii = "";
        if (TextUtils.isEmpty(text)) {
            return ascii;
        }
        for (int i = 0; i < text.length(); i++) {
            ascii += String.format("%04x", (int)text.charAt(i));
        }
        return ascii;
    }

}
