package co.onemeter.oneapp.adapter;

import android.app.Activity;
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
import co.onemeter.oneapp.utils.TimeElapseReportRunnable;

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
    
    private TimeElapseReportRunnable setWidthRunnable=new TimeElapseReportRunnable();
    private boolean threadStarted=false;
    private View parentRef;

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
        
        setWidthRunnable.setElapseReportListener(new TimeElapseReportRunnable.TimeElapseReportListener() {
            @Override
            public void reportElapse(final long elapsed) {
                if(contextRef instanceof Activity) {
                    Activity activity=(Activity) contextRef;
                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if(null != parentRef && parentRef.getWidth() > 0 && threadStarted) {
                                setWidthRunnable.stop();
                                threadStarted=false;
                                notifyDataSetInvalidated();
                            }
                        }
                    });
                }
            }
        });
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
        parentRef = parent;
        
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

        
        TextView tvOptionDesc = (TextView) convertView.findViewById(R.id.option_desc);
        TextView tvVotedFactor=(TextView) convertView.findViewById(R.id.voted_factor);
        ImageView ivVotedDesc=(ImageView) convertView.findViewById(R.id.option_voted_iv_desc);
        
        
        String[] optionFlags = new String[]{"A","B","C","D","E","F","G","H","I","J","K","L","M","N",
        	"O","P","Q","R","S","T","U","V","W","X","Y","Z"};
        tvOptionDesc.setText(optionFlags[position] + ":"+surveyOptions.get(position).optionDesc);
        int totalVotedCount=0;
        for(Moment.SurveyOption aOption : surveyOptions) {
            totalVotedCount += aOption.votedNum;
        }
        
      //对不同的投票选项设置不同的颜色背景
        if (position % 3 == 0) {
        	ivVotedDesc.setBackgroundResource(R.drawable.share_vote_1);
        } else if (position % 3 == 1) {
        	ivVotedDesc.setBackgroundResource(R.drawable.share_vote_2);
        } else if (position % 3 == 2) {
        	ivVotedDesc.setBackgroundResource(R.drawable.share_vote_3);
        }
        
        float factor = 1.0f * surveyOptions.get(position).votedNum / totalVotedCount;
        String ratio = surveyOptions.get(position).votedNum + "/" + totalVotedCount;//投票的显示结果
        tvVotedFactor.setText(ratio);
        
        ViewGroup.LayoutParams layoutParams = ivVotedDesc.getLayoutParams();
        if(surveyOptions.get(position).votedNum > 0) {
            float maxWidth=parent.getWidth()-contextRef.getResources().getDimension(R.dimen.survey_right_voted_info_min_width);
            maxWidth *= factor;
            layoutParams.width=(int)maxWidth;
            ivVotedDesc.setLayoutParams(layoutParams);

            if(maxWidth < 0 && !threadStarted) {
                new Thread(setWidthRunnable).start();
                threadStarted=true;
            }
        } else {
            layoutParams.width=(int)contextRef.getResources().getDimension(R.dimen.survey_voted_note_iv_def_width);
            ivVotedDesc.setLayoutParams(layoutParams);
        }

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
