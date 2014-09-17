package co.onemeter.oneapp.ui;

import java.util.ArrayList;
import java.util.Locale;

import org.wowtalk.Log;
import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.JapaneseHelper;
import org.wowtalk.api.PinyinHelper;
import org.wowtalk.api.PrefUtil;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;

public class LocaleChangedReceiver extends BroadcastReceiver {

    /**
     * 启动应用时，如果检测到语言变化，则需要重新给sort_key排序
     */
    public static final String ACTION_LOCALE_CHANGED_MINE = "co.onemeter.oneapp.LOCALE_CHANGED";

    @Override
    public void onReceive(final Context context, Intent intent) {
        String action = intent.getAction();
        if (Intent.ACTION_LOCALE_CHANGED.equals(action)
                || ACTION_LOCALE_CHANGED_MINE.equals(action)) {

            // 变更语言时，修改联系人的sort_key字段
            // 目前只有和日文之间的切换，才需要重新给sort_key赋值
            Locale locale = context.getResources().getConfiguration().locale;
            final String language = locale.getLanguage();
            final String originalLanguage = PrefUtil.getInstance(context).getLocaleLanguage();
            Log.i("switch locale: " + originalLanguage + " -> " + language);
            JapaneseHelper.setJapaneseDict(context);

            if ("ja".endsWith(language) || "ja".endsWith(originalLanguage)) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                     // 获取所有的联系人信息
                        Database dbHelper = new Database(context);
                        ArrayList<Buddy> buddies = dbHelper.fetchNormalBuddies();
                        // 自己
                        String myUid = PrefUtil.getInstance(context).getUid();
                        if (!TextUtils.isEmpty(myUid)) {
                            Buddy me = new Buddy(myUid);
                            me = dbHelper.fetchBuddyDetail(me);
                            if (null != me) {
                                buddies.add(me);
                            }
                        }

                        // 从其他语言切换为日文
                        if ("ja".endsWith(language)) {
                            for (Buddy buddy : buddies) {
                                if (TextUtils.isEmpty(buddy.pronunciation)) {
                                    if (TextUtils.isEmpty(buddy.alias)) {
                                        buddy.sortKey = JapaneseHelper.instance().getKey(context, buddy.nickName);
                                    } else {
                                        buddy.sortKey = JapaneseHelper.instance().getKey(context, buddy.alias);
                                    }
                                } else {
                                    buddy.sortKey = JapaneseHelper.instance().getKey(context, buddy.pronunciation);
                                }
                            }
                        } else if ("ja".equals(originalLanguage)) {
                            // 从日文切换为其他语言
                            for (Buddy buddy : buddies) {
                                if (TextUtils.isEmpty(buddy.pronunciation)) {
                                    if (TextUtils.isEmpty(buddy.alias)) {
                                        buddy.sortKey = PinyinHelper.instance().getPinyin(context, buddy.nickName, true);
                                    } else {
                                        buddy.sortKey = PinyinHelper.instance().getPinyin(context, buddy.alias, true);
                                    }
                                } else {
                                    buddy.sortKey = PinyinHelper.instance().getPinyin(context, buddy.pronunciation, true);
                                }
                            }
                        }

                        dbHelper.updateBuddiesSortKey(buddies);
                        // 更新完成后再保存语言
                        PrefUtil.getInstance(context).setLocaleLanguage(language);
                    }
                }).start();
            } else {
                PrefUtil.getInstance(context).setLocaleLanguage(language);
            }

        }
    }

}
