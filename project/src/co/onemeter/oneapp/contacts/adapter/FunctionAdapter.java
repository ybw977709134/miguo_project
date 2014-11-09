package co.onemeter.oneapp.contacts.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import org.wowtalk.ui.GlobalValue;

import java.util.HashMap;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: jianxingdong
 * Date: 13-5-8
 * Time: PM6:06
 * To change this template use File | Settings | File Templates.
 */
public class FunctionAdapter extends BaseAdapter {
    private Context mContext;
    private List<HashMap<String, Integer>> list;

    public FunctionAdapter(Context context,
        List<HashMap<String, Integer>> list) {
        mContext = context;
        this.list = list;
    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.listitem_contact_function, parent, false);
        }
        ImageView imgView = (ImageView) convertView.findViewById(R.id.contact_photo);
        TextView txtName = (TextView) convertView.findViewById(R.id.txt_name);
        TextView txtInfo = (TextView) convertView.findViewById(R.id.new_info);
        imgView.setImageResource(list.get(position).get("image"));
        txtName.setText(mContext.getResources().getString(list.get(position).get("text")));
        int badge = list.get(position).get("badge");
        if (badge == 0) {
            txtInfo.setVisibility(View.GONE);
        } else if (badge == GlobalValue.BADGE_VALUE_NONZERO_NOTSURE) {
//            txtInfo.setText("+");
            txtInfo.setVisibility(View.VISIBLE);
        } else {
//            txtInfo.setText(Integer.toString(badge));
            txtInfo.setVisibility(View.VISIBLE);
        }
        return convertView;
    }
}
