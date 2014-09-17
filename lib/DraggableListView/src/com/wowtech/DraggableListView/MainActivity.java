package com.wowtech.DraggableListView;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AutoDragActivity {
    private final static int SLEEP_INTERVAL=2000;
    private ArrayAdapter<String> mAdapter;

    private List<String> mStrings = new ArrayList<String>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mAdapter = new ArrayAdapter<String>(this, R.layout.pulldown_item, mStrings);
        setContentListViewAdapter(mAdapter);

        TextView tvEmptyContent=new TextView(this);
        tvEmptyContent.setText("no data added");
        setEmptyContentView(tvEmptyContent);
        loadData();
    }

    private void loadData(){
        new AsyncTask<Void, Void, List<String>>() {
            @Override
            protected List<String> doInBackground(Void... params) {
                try {
                    Thread.sleep(SLEEP_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                List<String> strings = new ArrayList<String>();
                for (String body : mStringArray) {
                    strings.add(body);
                }
                return strings;
            }

            @Override
            protected void onPostExecute(List<String> strings) {
                mStrings.addAll(strings);
                mAdapter.notifyDataSetChanged();
                notifyLoadDataFinish();
                triggerAutoLoadMore();
            }
        }.execute((Void) null);
    }

    @Override
    public void onRefreshHandle() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    Thread.sleep(SLEEP_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String astring="After refresh " + System.currentTimeMillis();
                return astring;
            }

            @Override
            protected void onPostExecute(String aString) {
                if(!TextUtils.isEmpty(aString)) {
//                    mStrings.add(0, aString);
                    mStrings.remove(0);
                    mAdapter.notifyDataSetChanged();
                }
                // 告诉它更新完毕
                notifyRefreshFinish();
            }
        }.execute((Void) null);
    }

    @Override
    public void onMoreHandle() {
        new AsyncTask<Void, Void, String>() {
            @Override
            protected String doInBackground(Void... params) {
                try {
                    Thread.sleep(SLEEP_INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String astring="After more " + System.currentTimeMillis();
                return astring;
            }

            @Override
            protected void onPostExecute(String aString) {
                if(!TextUtils.isEmpty(aString)) {
                    mStrings.add(aString);
                    mAdapter.notifyDataSetChanged();
                }
                // 告诉它更新完毕
                notifyLoadMoreFinish();
            }
        }.execute((Void) null);
    }

    @Override
    public void onItemClickedHandle(AdapterView <?> parent, View view, int position, long id) {
        Toast.makeText(this, "click " + position, Toast.LENGTH_SHORT).show();

        mStrings.add("click " + position);
        mAdapter.notifyDataSetChanged();
    }

    // 模拟数据
    private String[] mStringArray = {
//            "Abbaye de Belloc",
//            "Abbaye du Mont des Cats",
//            "Abertam",
//            "Abondance",
//            "Ackawi",
//            "Acorn",
            "Adelost",
            "Affidelice au Chablis",
            "Afuega'l Pitu",
            "Airag",
            "Airedale",
            "Aisy Cendre",
            "Allgauer Emmentaler",
            "Alverca",
            "Ambert",
            "American Cheese"
    };

}
