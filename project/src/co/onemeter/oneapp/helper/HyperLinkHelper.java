package co.onemeter.oneapp.helper;

import android.text.TextUtils;
import co.onemeter.oneapp.adapter.MomentAdapter;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-6-19
 * Time: PM1:49
 * To change this template use File | Settings | File Templates.
 */
public class HyperLinkHelper {
    /**
     * 从字符串中提取出超链接。
     * @param text
     * @return
     */
    public static String[] extractHyperLinks(String text)
    {
        ArrayList<String> links = new ArrayList<String>();
        if(!TextUtils.isEmpty(text)) {
            String[] strSplit=text.split(" ");

            for(int i=0; i<strSplit.length; ++i) {
                String aStr=strSplit[i];
                if(MomentAdapter.isStringAValidURL(aStr)) {
                    links.add(aStr);
                }
            }
        }
//        int len = text.length();
//        int p = -1;
//        String prevLink = null;
//        for(int i = 0; i < len; ++i)
//        {
//            if(p >= 0 && (text.charAt(i) == ' '
//                    || text.charAt(i) == '\"'
//                    || text.charAt(i) == '\''
//                    || text.charAt(i) == '<'))
//            {
//                String link = text.substring(p, i);
//                if(prevLink == null || !prevLink.equals(link)) {
//                    if(link.startsWith("http://"))
//                        links.add(link);
//                    else
//                        links.add("http://" + link);
//                }
//                prevLink = link;
//                p = -1;
//                continue;
//            }
//
//            if(p == -1 && (text.startsWith("http://", i) || text.startsWith("www.", i)))
//            {
//                p = i;
//                continue;
//            }
//        }
//        if(p >= 0) {
//            String link = text.substring(p);
//            if(prevLink == null || !prevLink.equals(link))
//            {
//                if(link.startsWith("http://"))
//                    links.add(link);
//                else
//                    links.add("http://" + link);
//            }
//        }
        return links.toArray(new String[links.size()]);
    }

}
