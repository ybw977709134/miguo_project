package co.onemeter.oneapp.ui;

import android.content.Context;
import co.onemeter.oneapp.R;

import java.util.HashMap;

/**
 * <p>乐趣活动的UI辅助类。 </p>
 * Created by pzy on 11/2/14.
 */
public class WEventUiHelper {
    private static HashMap<String, Integer> catName2Idx;
    private static HashMap<Integer, String> catIdx2Text;

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
}
