package org.wowtalk.ui.msg;

import java.util.List;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;

public class StampAdapter extends BaseAdapter {

    public interface OnStampSelectedListener {
        public void OnStampSelected(Stamp s);
        public void OnKomojiSelected(String kaomoji);
    }

    private static final int STAMP_ROWS = 2;
    private Context mContext;
    private int mTypeId;
    private String mPackId;
    private List<PngDrawable> mStamps;
    private OnStampSelectedListener mOnStampSelectedListener;

    public StampAdapter(Context context, int typeidImage, String currStampPackIdx,
            List<PngDrawable> stamps, OnStampSelectedListener onStampSelectedListener) {
        mContext = context;
        mTypeId = typeidImage;
        mPackId = currStampPackIdx;
        mStamps = stamps;
        mOnStampSelectedListener = onStampSelectedListener;
    }

    @Override
    public int getCount() {
        boolean isAliquot = (mStamps.size() % STAMP_ROWS == 0);
        int count = isAliquot ? (mStamps.size() / STAMP_ROWS) : (mStamps.size() / STAMP_ROWS + 1);
        return count;
    }

    @Override
    public Object getItem(int position) {
        return mStamps.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (null != convertView) {
            holder = (ViewHolder) convertView.getTag();
        } else {
            holder = new ViewHolder();
            convertView = LayoutInflater.from(mContext).inflate(R.layout.msg_stamp_item, null);
            holder.stampFirst = (ImageView) convertView.findViewById(R.id.stamp_1);
            holder.stampSecond = (ImageView) convertView.findViewById(R.id.stamp_2);
            convertView.setTag(holder);
        }

        if (position < getCount() - 1
                || (position + 1) * STAMP_ROWS == mStamps.size()){
            holder.stampFirst.setImageDrawable(mStamps.get(position * STAMP_ROWS));
            holder.stampSecond.setImageDrawable(mStamps.get(position * STAMP_ROWS + 1));;
            holder.stampFirst.setOnClickListener(new StampListener(position * STAMP_ROWS));
            holder.stampSecond.setOnClickListener(new StampListener(position * STAMP_ROWS + 1));
        } else {
            holder.stampFirst.setImageDrawable(mStamps.get(position * STAMP_ROWS));
            holder.stampSecond.setImageDrawable(null);;
            holder.stampFirst.setOnClickListener(new StampListener(position * STAMP_ROWS));
            holder.stampSecond.setOnClickListener(null);
        }

        return convertView;
    }

    static class ViewHolder{
        ImageView stampFirst;
        ImageView stampSecond;
    }

    private class StampListener implements OnClickListener{

        private long mPosition;

        public StampListener(long position) {
            mPosition = position;
        }

        @Override
        public void onClick(View v) {
            Stamp stamp = new Stamp(
                    mContext,
                    mTypeId,
                    mPackId,
                    Long.toString(mPosition + 1));
            mOnStampSelectedListener.OnStampSelected(stamp);
        }
    }

    public void setDataSource(int emotionType, String packid, List<PngDrawable> stampDrawables) {
        mTypeId = emotionType;
        mPackId = packid;
        mStamps = stampDrawables;
    }
}
