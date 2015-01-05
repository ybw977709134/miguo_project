package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;
import co.onemeter.oneapp.R;

import org.wowtalk.api.Buddy;

import java.util.ArrayList;
import java.util.List;


/**
 * Created with IntelliJ IDEA.
 * User: zz
 * Date: 14-12-30
 * Time: 下午12:00
 * To change this template use File | Settings | File Templates.
 */
public class ApplicationInfoItemAdapter extends BaseAdapter {
    private Context contextRef;
    private ArrayList<Buddy> buddyList;
    public ApplicationInfoItemAdapter(Context context, ArrayList<Buddy> buddy) {
        contextRef=context;
        buddyList=buddy;
        if(null == buddyList) {
            buddyList=new ArrayList<Buddy>();
        }
    }
    @Override
    public int getCount() {
        return buddyList.size();
    }

    @Override
    public Object getItem(int i) {
        return i;
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if(null == convertView) {
            convertView= LayoutInflater.from(contextRef).inflate(R.layout.listitem_application, null);
        }

        TextView tv_nickname=(TextView) convertView.findViewById(R.id.tv_nickname);

        final Buddy buddy=buddyList.get(position);

        tv_nickname.setText(buddy.nickName);
        return convertView;
    }
    public void addAll(List<Buddy> lst) {
        buddyList.addAll(lst);
        notifyDataSetChanged();
    }
    
    public void clear() {
        buddyList.clear();
        notifyDataSetChanged();
    }
}
