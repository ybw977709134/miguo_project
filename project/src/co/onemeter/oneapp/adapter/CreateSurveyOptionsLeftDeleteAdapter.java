package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import co.onemeter.oneapp.R;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-28
 * Time: 下午1:31
 * To change this template use File | Settings | File Templates.
 */
public class CreateSurveyOptionsLeftDeleteAdapter extends BaseAdapter {
    private int optionsCount;
    private Context contextRef;

    private OnOptionDeleteListener onDeleteListener;

    public CreateSurveyOptionsLeftDeleteAdapter(Context context,OnOptionDeleteListener listener) {
        optionsCount=0;
        contextRef=context;

        onDeleteListener=listener;
    }

    public void setCount(int count) {
        optionsCount=count;
    }
    @Override
    public int getCount() {
        return optionsCount;
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
        if (convertView == null) {
            convertView = LayoutInflater.from(contextRef).inflate(R.layout.create_survey_options_left_del_item, null);
        }

        ImageView ivRemove=(ImageView)convertView.findViewById(R.id.iv_remove_icon);
        ivRemove.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(null != onDeleteListener) {
                    onDeleteListener.onDelete(position);
                }
            }
        });

        return convertView;
    }

    public static interface OnOptionDeleteListener {
        void onDelete(int position);
    }
}
