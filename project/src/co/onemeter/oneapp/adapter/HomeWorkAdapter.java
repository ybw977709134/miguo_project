package co.onemeter.oneapp.adapter;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TableLayout;
import android.widget.TextView;

/**
 * 用于加载作业信息
 * Created by hutianfeng on 15-5-28.
 */
public class HomeWorkAdapter extends BaseAdapter{

    @Override
    public int getCount() {
        return 0;
    }

    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}


class ViewHolder{
    //作业
    LinearLayout layout_homework_comment;
    TextView date_homework_comment;//作业日期
    TableLayout imageTable;//图片墙
    TextView textView_homework_title;//作业标题

    //评论
    LinearLayout layout_homework_review;
    TextView date_homework_review;//评论日期
    RatingBar ratingBar_confirm;//完整性
    RatingBar ratingBar_timely;//及时性
    RatingBar ratingBar_exact;//准确性
    TextView textView_homework_review;//老师评语





}
