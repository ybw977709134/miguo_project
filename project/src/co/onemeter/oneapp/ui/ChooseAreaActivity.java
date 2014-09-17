package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.umeng.analytics.MobclickAgent;
import co.onemeter.oneapp.R;

/**
 * Created with IntelliJ IDEA.
 * User: Wow
 * Date: 13-4-27
 * Time: 下午7:50
 * To change this template use File | Settings | File Templates.
 */
public class ChooseAreaActivity extends Activity {
    private class AreaAdapter extends BaseAdapter {
        private Context mContext;
        private String[] areaArr;

        public AreaAdapter(Context context, String[] areaArr) {
            mContext = context;
            this.areaArr = areaArr;
        }

        @Override
        public int getCount() {
            return areaArr.length;
        }

        @Override
        public Object getItem(int position) {
            return areaArr[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_choose_area, null);
            }

            TextView textView = (TextView) convertView.findViewById(R.id.text);
            textView.setText(areaArr[position]);
            return convertView;
        }
    }
    private ImageButton btnTitleBack;
    private ListView areaList;
    private AreaAdapter areaAdapter;

    private String[] areaArr;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_area_choose);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        areaArr = getResources().getStringArray(R.array.area_array);
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        areaList = (ListView) findViewById(R.id.area_list);
        btnTitleBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        areaList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent intent = new Intent();
                intent.putExtra("text", areaArr[position]);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
        areaAdapter = new AreaAdapter(this, areaArr);
        areaList.setAdapter(areaAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
