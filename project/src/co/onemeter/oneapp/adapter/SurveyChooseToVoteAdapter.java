package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import org.wowtalk.api.Moment;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.LinearLayoutAsListAdapter;
import co.onemeter.oneapp.ui.LinearLayoutAsListDataChangeListener;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-9-28
 * Time: 下午5:37
 * To change this template use File | Settings | File Templates.
 */
public class SurveyChooseToVoteAdapter implements LinearLayoutAsListAdapter {
    private Context contextRef;
    private ArrayList<Moment.SurveyOption> surveyOptions;
    private ArrayList<String> choosedOptionsIdList;
    private boolean withMultiChoose;

    private LinearLayoutAsListDataChangeListener dataChangeListener;

    public static interface OnChooseChangeListener {
        void onChooseChange(int count);
    }
    private OnChooseChangeListener onChooseChangeListener;

    public SurveyChooseToVoteAdapter(Context context,ArrayList<Moment.SurveyOption> options,ArrayList<String> choosed,boolean multi) {
        contextRef=context;
        surveyOptions=options;

        choosedOptionsIdList=choosed;
        withMultiChoose=multi;

        if(null == choosedOptionsIdList) {
            choosedOptionsIdList=new ArrayList<String>();
        }
    }

    public void setOnChooseChangeListener(OnChooseChangeListener listener) {
        onChooseChangeListener=listener;
    }

    @Override
    public int getCount() {
        return surveyOptions.size();
    }

//    @Override
//    public Object getItem(int i) {
//        return surveyOptions.get(i);
//    }
//
//    @Override
//    public long getItemId(int i) {
//        return i;
//    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(contextRef).inflate(R.layout.survey_choose_to_vote_layout, null);
        }

        LinearLayout chooseLayout=(LinearLayout) convertView.findViewById(R.id.choose_layout);
        ImageView ivCheck=(ImageView) convertView.findViewById(R.id.choose_layout_inner_check);

        final String optionId=surveyOptions.get(position).optionId;

        chooseLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(choosedOptionsIdList.contains(optionId)) {
                    choosedOptionsIdList.remove(optionId);
                } else {
                    if(choosedOptionsIdList.size() > 0) {
                        if(!withMultiChoose) {
                            choosedOptionsIdList.clear();
                        }
                        choosedOptionsIdList.add(optionId);
                    } else {
                        choosedOptionsIdList.add(optionId);
                    }
                }
                if(null != onChooseChangeListener) {
                    onChooseChangeListener.onChooseChange(choosedOptionsIdList.size());
                }
                notifyDataSetChanged();
            }
        });

        if(withMultiChoose) {//多选时表现的投票选中项
        	if(choosedOptionsIdList.contains(optionId)) {
        		ivCheck.setImageResource(R.drawable.share_vote_chb_1);
        	} else {
        		ivCheck.setImageResource(R.drawable.share_vote_chb);
        	}
        } else {//单选时表现的投票选中项
        	if(choosedOptionsIdList.contains(optionId)) {
        		ivCheck.setImageResource(R.drawable.share_vote_checked);
        	} else {
        		ivCheck.setImageResource(R.drawable.share_vote_unchecked);
        	}
        }
        
        
//        if(choosedOptionsIdList.contains(optionId)) {
//            ivCheck.setImageResource(R.drawable.timeline_checked);
//        } else {//单选时表现的投票选中项
//            ivCheck.setImageResource(R.drawable.timeline_unchecked);
//        }

        
        TextView tvDesc=(TextView) convertView.findViewById(R.id.option_desc);
        tvDesc.setText(surveyOptions.get(position).optionDesc);

        return convertView;
    }

    @Override
    public void notifyDataSetChanged() {
        if(null != dataChangeListener) {
            dataChangeListener.onDataChanged();
        }
    }

    @Override
    public void notifyDataSetInvalidated() {
        if(null != dataChangeListener) {
            dataChangeListener.onDataChanged();
        }
    }

    @Override
    public void setDataChangeListener(LinearLayoutAsListDataChangeListener listener) {
        dataChangeListener=listener;
    }
}
