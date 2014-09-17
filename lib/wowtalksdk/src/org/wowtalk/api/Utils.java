package org.wowtalk.api;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Vibrator;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.widget.PopupWindow;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.wowtalk.Log;

import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.Locale;

/**
 * Functions provided here are subject to change. 
 */
public class Utils {

    public static final long DEF_VIBRATE_TIME=500;//ms
    public static final long VIRBRATE_TIME_DELAY = 0;
    public static final long VIRBRATE_TIME_FIRST = 200;
    public static final long VIRBRATE_TIME_INTERVAL = 200;
    public static final long VIRBRATE_TIME_SECOND = 100;
    public static final int VIRBRATE_REPEAT_NO = -1;

    public static void triggerVibrate(Context context,long ms) {
        if(null != context) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if(null != vibrator) {
                vibrator.vibrate(ms);
            }
        }
    }

    /**
     * 间隔震动，入参参见android.os.Vibrator.vibrate(long[] pattern, int repeat)
     * @param context
     * @param pattern an array of longs of times for which to turn the vibrator on or off.
     * @param repeat the index into pattern at which to repeat, or -1 if you don't want to repeat.
     */
    public static void triggerVibrate(Context context, long[] pattern, int repeat) {
        if(null != context) {
            Vibrator vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
            if(null != vibrator && null != pattern) {
                vibrator.vibrate(pattern, repeat);
            }
        }
    }

    public static boolean isNetworkConnected(Context context) {
        if (context != null) {
            ConnectivityManager mConnectivityManager = (ConnectivityManager) context
                    .getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo mNetworkInfo = mConnectivityManager.getActiveNetworkInfo();
            if (mNetworkInfo != null) {
                return mNetworkInfo.isAvailable();
            }
        }
        return false;
    }

    private static void fixPopupWindow(final PopupWindow window) {
        if(null == window) {
            return;
        }
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
            try {
                final Field fAnchor = PopupWindow.class
                        .getDeclaredField("mAnchor");
                fAnchor.setAccessible(true);
                Field listener = PopupWindow.class
                        .getDeclaredField("mOnScrollChangedListener");
                listener.setAccessible(true);
                final ViewTreeObserver.OnScrollChangedListener originalListener = (ViewTreeObserver.OnScrollChangedListener) listener
                        .get(window);
                ViewTreeObserver.OnScrollChangedListener newListener = new ViewTreeObserver.OnScrollChangedListener() {
                    @Override
                    public void onScrollChanged() {
                        try {
                            WeakReference<View> mAnchor = (WeakReference<View>) fAnchor.get(window);
                            if (mAnchor == null || mAnchor.get() == null) {
                                return;
                            } else {
                                originalListener.onScrollChanged();
                            }
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                };
                listener.set(window, newListener);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public static PopupWindow getFixedPopupWindow(View contentView,int width,int height) {
        PopupWindow momentOpPopWindow = new PopupWindow(contentView, width,height);

        fixPopupWindow(momentOpPopWindow);

        return momentOpPopWindow;
    }

    public static void setViewAlpha(View view, float alpha)
    {
        if (Build.VERSION.SDK_INT < 11)
        {
            AlphaAnimation animation = new AlphaAnimation(alpha, alpha);
            animation.setDuration(0);
            animation.setFillAfter(true);
            view.startAnimation(animation);
        }
        else view.setAlpha(alpha);
    }

	public static boolean isNullOrEmpty(String s)
	{
		return s == null || s.equals("");
	}
	
 	public static Element getFirstElementByTagName(Element root, String name) {
 		NodeList lst = root.getElementsByTagName(name);
 		if(lst == null)
 			return null;
 		return (Element)lst.item(0);
 	}

    public static String getFirstTextByTagName(Element root, String name) {
        NodeList lst = root.getElementsByTagName(name);
        if(lst == null)
            return null;
        Element e = (Element)lst.item(0);
        return null == e ? null : e.getTextContent();
    }

    public static long getFirstLongByTagName(Element e, String name, long defValue) {
        return tryParseLong(getFirstTextByTagName(e, name), defValue);
    }

    public static int getFirstIntByTagName(Element e, String name, int defValue) {
        return tryParseInt(getFirstTextByTagName(e, name), defValue);
    }

    public static int tryParseInt(String s, int defaultValue) {
        if (isNullOrEmpty(s))
            return defaultValue;

 		try {
 			return Integer.parseInt(s);
 		} catch (Exception e) {
 			e.printStackTrace();
 			return defaultValue;
 		}
 	}

 	public static long tryParseLong(String s, long defaultValue) {
        if (isNullOrEmpty(s))
            return defaultValue;

        try {
 			return Long.parseLong(s);
 		} catch (Exception e) {
 			e.printStackTrace();
 			return defaultValue;
 		}
 	}

 	public static float tryParseFloat(String s, int defaultValue) {
 		try {
 			return Float.parseFloat(s);
 		} catch (Exception e) {
 			e.printStackTrace();
 			return defaultValue;
 		}
 	}

    public static double tryParseDouble(String s, int defaultValue) {
        try {
            return Double.parseDouble(s);
        } catch (Exception e) {
            e.printStackTrace();;
            return defaultValue;
        }
    }

    // 不抛异常的 url encoder
    public static String urlencodeUtf8(String s)
    {
        String encodeUrl = "";
        if (null != s) {
            try {
                // It will throw NullPointerException if s is null.
                encodeUrl = java.net.URLEncoder.encode(s, "UTF-8");
            }
            catch(java.io.UnsupportedEncodingException e)
            {
                Log.e(e);
            }
        }
        return encodeUrl;
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
    
	public static byte[] concat(byte[] a, byte[] b, int count) {
		// Arrays.copyOf() requires API LEVEL 9
		//byte[] result = Arrays.copyOf(a, a.length + count);
		byte[] result = new byte[a.length + count];
		System.arraycopy(a, 0, result, 0, a.length);
		System.arraycopy(b, 0, result, a.length, count);
		return result;
    }

    public static String makeSortKey(Context context, String nickName) {
        if (null == nickName || nickName.trim().equals(""))
            return "?";
        nickName = nickName.trim();
        if (Character.isDigit(nickName.charAt(0)))
            return "#";
        Locale locale = context.getResources().getConfiguration().locale;
        String language = locale.getLanguage();
        Log.d("system language is " + language);
        String key = "";
        if ("ja".endsWith(language)) {
            key = JapaneseHelper.instance().getKey(context, nickName);
        } else {
            key = PinyinHelper.instance().getPinyin(context, nickName, true);
        }
        Log.d("the key of " + nickName + " is: " + key);
        return key;
    }
}
