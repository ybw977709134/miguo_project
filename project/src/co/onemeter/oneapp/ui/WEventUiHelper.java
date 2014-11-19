package co.onemeter.oneapp.ui;

import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import co.onemeter.oneapp.R;

import java.util.HashMap;

/**
 * <p>乐趣活动的UI辅助类。 </p>
 * Created by pzy on 11/2/14.
 */
public class WEventUiHelper {
    private static HashMap<String, Integer> catName2Idx;
    private static HashMap<Integer, String> catIdx2Text;

    private Context context;

    public WEventUiHelper(Context context) {
        this.context = context;
    }

    public static String getCategoryNameByIndex(Context context, int index) {
        String[] names = context.getResources().getStringArray(R.array.event_category_name);
        return index >= 0 && index < names.length ? names[index] : null;
    }

    /**
     * 获取乐趣活动的类型的文本。
     * @param context
     * @param categoryName
     * @return
     */
    public static String getEventCatetoryText(Context context, String categoryName) {
        if (catName2Idx == null) {
            catName2Idx = new HashMap<String, Integer>();
            String[] names = context.getResources().getStringArray(R.array.event_category_name);
            for (int i = 0; i < names.length; ++i) {
                catName2Idx.put(names[i], i);
            }
        }
        if (catIdx2Text == null) {
            catIdx2Text = new HashMap<Integer, String>();
            String[] texts = context.getResources().getStringArray(R.array.event_category_text);
            for (int i = 0; i < texts.length; ++i) {
                catIdx2Text.put(i, texts[i]);
            }
        }
        return catIdx2Text.get(catName2Idx.get(categoryName));
    }

    public Spannable formatField(String label, String value)
    {
        return formatField(label, value,
                context.getResources().getColor(R.color.text_gray3),
                context.getResources().getColor(R.color.text_gray2));
    }

    public Spannable formatField(String label, String value, int color1, int color2)
    {
        int start = 0;
        SpannableStringBuilder str = new SpannableStringBuilder(label == null ? "" : label);
        str.append(": ");
        int end = str.length();
        str.setSpan(new ForegroundColorSpan(color1),
                start, end, 0);
        start = end;
        str.append(value == null ? "" : value);
        end = str.length();
        str.setSpan(new ForegroundColorSpan(color2),
                start, end, 0);
        return str;
    }
}
