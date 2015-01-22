package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import org.wowtalk.api.*;
import co.onemeter.oneapp.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.R;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-6-8
 * Time: AM11:16
 * To change this template use File | Settings | File Templates.
 */
public class NewReviewAdapter extends ArrayAdapter<Review> {

    private int mLayoutResourceId;
    private List<Review> mReviews;
    private PhotoDisplayHelper mPhotoHelper;
    private Database mDb;


    public NewReviewAdapter(Context context, List<Review> objects) {
        this(context, R.layout.listitem_new_review, R.id.txt_content, objects);
    }

    public NewReviewAdapter(Context context, int resource, int textViewResourceId, List<Review> objects) {
        super(context, resource, textViewResourceId, objects);
        mLayoutResourceId = resource;
        mReviews = objects;
        mPhotoHelper = new PhotoDisplayHelper(context);
        mDb = Database.open(context);
    }

    @Override
    public View getView (int position, View convertView, ViewGroup parent) {
        ViewHolder vh;
        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(mLayoutResourceId, parent, false);
            vh = new ViewHolder();
            vh.imgPhoto = (ImageView)convertView.findViewById(R.id.img_photo);
            vh.txtNick = (TextView)convertView.findViewById(R.id.txt_nick);
            vh.txtTime = (TextView)convertView.findViewById(R.id.txt_time);
            vh.txtContent = (TextView)convertView.findViewById(R.id.txt_content);
            vh.likeIV = (ImageView)convertView.findViewById(R.id.iv_like);
            vh.tvMomentContent = (TextView)convertView.findViewById(R.id.moment_txt_content);
            vh.ivMomentPhoto = (ImageView)convertView.findViewById(R.id.moment_photo);
            convertView.setTag(vh);
        } else {
            vh = (ViewHolder)convertView.getTag();
        }

        if (vh == null)
            return convertView;

        Review r = mReviews.get(position);

        vh.txtNick.setText(TextUtils.isEmpty(r.nickname) ? "(null)" : r.nickname);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        vh.txtTime.setText(sdf.format(new Date(r.timestamp * 1000)));
        if (r.type == Review.TYPE_TEXT) {
            vh.txtContent.setText(r.text);
            vh.likeIV.setVisibility(View.GONE);
        } else {
            vh.txtContent.setVisibility(View.GONE);
            vh.likeIV.setVisibility(View.VISIBLE);
//            vh.txtContent.setText(R.string.moments_think_is_good);
        }
        Buddy b = mDb.buddyWithUserID(r.uid);
        if (b != null) {
            PhotoDisplayHelper.displayPhoto(getContext(), vh.imgPhoto, R.drawable.default_avatar_90, b, true);
        } else {
            vh.imgPhoto.setImageResource(R.drawable.default_avatar_90);
        }

        Moment moment=mDb.fetchMoment(r.hostId);
        if(null != moment) {
            boolean photoExist=false;
            WFile firstPhotoFile=null;

            if(null == moment.multimedias || moment.multimedias.size() <= 0) {
                photoExist=false;
            } else {
                for (WFile file : moment.multimedias) {
                    if (!(file.isAudioByExt())) {
                        firstPhotoFile=file;
                        photoExist=true;
                        break;
                    }
                }

            }

            if(photoExist) {
                vh.tvMomentContent.setVisibility(View.GONE);
                vh.ivMomentPhoto.setVisibility(View.VISIBLE);

                vh.ivMomentPhoto.setImageDrawable(Drawable.createFromPath(PhotoDisplayHelper.makeLocalFilePath(firstPhotoFile.thumb_fileid, firstPhotoFile.getExt())));
            } else {
                vh.tvMomentContent.setVisibility(View.VISIBLE);
                vh.ivMomentPhoto.setVisibility(View.GONE);

                vh.tvMomentContent.setText(moment.text);
            }
        } else {
            vh.tvMomentContent.setVisibility(View.GONE);
            vh.ivMomentPhoto.setVisibility(View.GONE);
        }

        return convertView;
    }

    static class ViewHolder {
        public ImageView imgPhoto;
        public TextView txtNick, txtTime, txtContent;
        public ImageView likeIV;
        public TextView tvMomentContent;
        public ImageView ivMomentPhoto;
    }
}

