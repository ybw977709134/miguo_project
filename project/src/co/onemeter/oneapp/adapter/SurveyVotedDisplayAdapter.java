package co.onemeter.oneapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
 * Date: 13-9-29
 * Time: 上午10:35
 * To change this template use File | Settings | File Templates.
 */
public class SurveyVotedDisplayAdapter implements LinearLayoutAsListAdapter {
    private Context contextRef;
    private ArrayList<Moment.SurveyOption> surveyOptions;

    private LinearLayoutAsListDataChangeListener dataChangeListener;
    private TimeElapseReportRunnable setWidthRunnable=new TimeElapseReportRunnable();
    private boolean threadStarted=false;
    private View parentRef;

    public SurveyVotedDisplayAdapter(Context context,ArrayList<Moment.SurveyOption> options) {
        contextRef=context;
        surveyOptions=options;

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
            convertView = LayoutInflater.from(contextRef).inflate(R.layout.survey_voted_display_item_layout, null);
        }
        parentRef = parent;

        TextView tvOptionDesc = (TextView) convertView.findViewById(R.id.option_desc);
        TextView tvVotedNum=(TextView) convertView.findViewById(R.id.voted_num);
        TextView tvVotedFactor=(TextView) convertView.findViewById(R.id.voted_factor);
        ImageView ivVotedDesc=(ImageView) convertView.findViewById(R.id.option_voted_iv_desc);

//        tvOptionDesc.setText(surveyOptions.get(position).optionDesc);
        //分别给对应的投票选项添加标志
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
        String ratio = surveyOptions.get(position).votedNum + "/" + totalVotedCount;

//        tvVotedFactor.setText((int)(factor*100) + "%");
        tvVotedFactor.setText(ratio);
        
//        tvVotedNum.setText(surveyOptions.get(position).votedNum+" "+contextRef.getString(R.string.vote));

        ViewGroup.LayoutParams layoutParams = ivVotedDesc.getLayoutParams();

        if(surveyOptions.get(position).votedNum > 0) {
            float maxWidth=parent.getWidth()-contextRef.getResources().getDimension(R.dimen.survey_right_voted_info_min_width);
//            Log.i("whole_width="+parent.getWidth()+
//                    ",right_min_width="+contextRef.getResources().getDimension(R.dimen.survey_right_voted_info_min_width)+
//                    ",factor="+factor);
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
