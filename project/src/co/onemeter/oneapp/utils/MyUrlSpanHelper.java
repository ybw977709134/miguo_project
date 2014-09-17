package co.onemeter.oneapp.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Browser;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ClickableSpan;
import android.text.style.URLSpan;
import android.view.View;
import android.widget.TextView;

public class MyUrlSpanHelper {

    private boolean mIsLongClick;

    public MyUrlSpanHelper(TextView textView) {
        if (null == textView) {
            return;
        }
        CharSequence text = textView.getText();
        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            URLSpan[] urlSpans = spannable.getSpans(0, text.length(), URLSpan.class);
            SpannableStringBuilder style = new SpannableStringBuilder(text);
            style.clearSpans();
            MyUrlSpan mySpan = null;
            for (URLSpan urlSpan : urlSpans) {
                mySpan = new MyUrlSpan(urlSpan.getURL());
                style.setSpan(mySpan, spannable.getSpanStart(urlSpan),
                        spannable.getSpanEnd(urlSpan), Spannable.SPAN_EXCLUSIVE_INCLUSIVE);
            }
            textView.setText(style);
        }
    }

    public void onLongClicked() {
        mIsLongClick = true;
    }

    private class MyUrlSpan extends ClickableSpan {

        private String mUrl;

        public MyUrlSpan(String text) {
            mUrl = text;
        }

        @Override
        public void onClick(View widget) {
            if (!mIsLongClick) {
                Uri uri = Uri.parse(mUrl);
                Context context = widget.getContext();
                Intent intent = new Intent(Intent.ACTION_VIEW, uri);
                intent.putExtra(Browser.EXTRA_APPLICATION_ID, context.getPackageName());
                context.startActivity(intent);
            }
            mIsLongClick = false;
        }
    }
}
