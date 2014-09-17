package com.example.messagebox_demo;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import org.wowtalk.ui.MessageBox;

public class MyActivity extends Activity implements View.OnClickListener {
    private MessageBox mMsgBox;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        mMsgBox = new MessageBox(this);
        findViewById(android.R.id.content).setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
//        testWait();

//        mMsgBox.show("title", "this is message body");
        testToast();
    }

    private void testWait() {
        mMsgBox.showWait();
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(2500);
                } catch (Exception e) {

                }
                mMsgBox.dismissWait();
                return null;
            }
        }.execute((Void) null);
    }

    private void testToast() {
        mMsgBox.toast("this is a long long --------------- message");
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    Thread.sleep(500);
                } catch (Exception e) {

                }
                mMsgBox.toast("new toast");
                return null;
            }
        }.execute((Void) null);
    }
}
