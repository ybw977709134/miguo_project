package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.wowtalk.api.Buddy;
import org.wowtalk.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-5
 * Time: 下午2:33
 * To change this template use File | Settings | File Templates.
 */
public class FamilyContactAdapter extends BaseAdapter {
    private Context context;
    private ArrayList<Buddy> familyBuddyList;

    public FamilyContactAdapter(Context ctx,ArrayList<Buddy> list) {
        context=ctx;
        familyBuddyList=list;
    }

    @Override
    public int getCount() {
        return familyBuddyList.size();
    }

    @Override
    public Object getItem(int i) {
        return familyBuddyList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;

        if (convertView == null) {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(context).inflate(R.layout.family_contact_adapter_item, null);
            holder.dividerView=(ImageView) convertView.findViewById(R.id.divider_view);
            holder.imgPhoto = (ImageView) convertView.findViewById(R.id.contact_photo);
            holder.txtContactName = (TextView) convertView.findViewById(R.id.contact_name);
            holder.txtContactStatus = (TextView) convertView.findViewById(R.id.contact_status);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if(0 == position) {
            holder.dividerView.setVisibility(View.GONE);
        } else {
            holder.dividerView.setVisibility(View.VISIBLE);
        }

        PhotoDisplayHelper.displayPhoto(context, holder.imgPhoto,
                R.drawable.default_avatar_90, familyBuddyList.get(position), true);

        String userName;
        if(TextUtils.isEmpty(familyBuddyList.get(position).alias)) {
            userName=familyBuddyList.get(position).nickName;
        } else {
            userName=familyBuddyList.get(position).alias;
        }
        holder.txtContactName.setText(userName);
        holder.txtContactStatus.setText(familyBuddyList.get(position).status);

        return convertView;
    }

    private class ViewHolder {
        ImageView dividerView;
        ImageView imgPhoto;
        TextView txtContactName;
        TextView txtContactStatus;
    }
}
