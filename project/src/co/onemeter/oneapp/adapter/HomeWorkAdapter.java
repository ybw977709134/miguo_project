package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TableLayout;
import android.widget.TextView;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.HomeWorkResult;
import org.wowtalk.api.Moment;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WFile;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.ArrayList;
import java.util.List;

import co.onemeter.oneapp.R;

/**
 * 用于加载作业信息
 * Created by hutianfeng on 15-5-28.
 */
public class HomeWorkAdapter extends BaseAdapter{

    private Context context;
    private List<HomeWorkResult> stuResultList;

    private ImageResizer mImageResizer;//处理图片


    public HomeWorkAdapter (Context context,List<HomeWorkResult> stuResultList,ImageResizer mImageResizer) {
        this.context = context;
        this.stuResultList = stuResultList;
        this.mImageResizer = mImageResizer;
    }

    @Override
    public int getCount() {
        return stuResultList.size();
    }

    @Override
    public Object getItem(int position) {
        return stuResultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        //还没有初始化
        Moment moment = new Moment();

        ArrayList<WFile> photoFiles = new ArrayList<WFile>();
        WFile voiceFile = null;
        for (WFile file : moment.multimedias) {
            if (file.isAudioByExt()) {
                voiceFile = file;
            } else {
                photoFiles.add(file);
            }
        }

        ViewHolder holder= null;
        if (convertView == null) {
            //老师和学生的作业列表稍有不同
            if (PrefUtil.getInstance(context).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {//老师
                convertView = LayoutInflater.from(context).inflate(R.layout.item_homework_teacher,null);
            } else {//学生
                convertView = LayoutInflater.from(context).inflate(R.layout.item_homework_student,null);
            }

            holder = new ViewHolder();
            //作业
            holder.layout_homework_comment = (LinearLayout) convertView.findViewById(R.id.layout_homework_comment);
            holder.date_homework_comment = (TextView) convertView.findViewById(R.id.date_homework_comment);
            holder.imageTable = (TableLayout) convertView.findViewById(R.id.imageTable);
            holder.textView_homework_title = (TextView) convertView.findViewById(R.id.textView_homework_title);

            //评论
            holder.layout_homework_review = (LinearLayout) convertView.findViewById(R.id.layout_homework_review);
            holder.date_homework_review = (TextView) convertView.findViewById(R.id.date_homework_review);
            holder.ratingBar_confirm = (RatingBar) convertView.findViewById(R.id.ratingBar_confirm);
            holder.ratingBar_timely = (RatingBar) convertView.findViewById(R.id.ratingBar_timely);
            holder.ratingBar_exact = (RatingBar) convertView.findViewById(R.id.ratingBar_exact);
            holder.textView_homework_review = (TextView) convertView.findViewById(R.id.textView_homework_review);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        holder.date_homework_comment.setText(stuResultList.get(position).stuMoment.insert_timestamp+"");
        //下载图片
        MomentAdapter.setImageLayout(context, moment,mImageResizer, photoFiles, holder.imageTable);


        holder.textView_homework_title.setText(stuResultList.get(position).stuMoment.text_content);

        //判断老师是否评论了作业
        if (stuResultList.get(position).homeWorkReview == null) {
            holder.layout_homework_review.setVisibility(View.GONE);
        } else {
            holder.layout_homework_review.setVisibility(View.VISIBLE);
            //批改时间
            holder.date_homework_review.setText(System.currentTimeMillis()+"");
            holder.ratingBar_confirm.setRating(stuResultList.get(position).homeWorkReview.rank1);
            holder.ratingBar_timely.setRating(stuResultList.get(position).homeWorkReview.rank2);
            holder.ratingBar_exact.setRating(stuResultList.get(position).homeWorkReview.rank3);
            holder.textView_homework_review.setText(stuResultList.get(position).homeWorkReview.text);
        }




        return convertView;
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
