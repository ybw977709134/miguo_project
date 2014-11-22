package org.wowtalk.ui.msg;


import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.HashMap;

/**
 * Display emotion sub types, such as "happy","angry" for kaomoji, or for stamp(image/anime)
 */
public class FaceTypeAdapter extends BaseAdapter{
	public interface OnFaceTypeSelectedListener {
		public void OnFaceTypeSelected(int type, int packIdx);
	}
	
	public static final int KAOMOJI_TABLE_COUNT = 6; // how many emotionX.plist?
	private int mKMJCount = KAOMOJI_TABLE_COUNT;
	private int mImgCount;
	private int mAnimeCount;

	private Context mContext;
	private int mEmotionType;
//	private String currPackId = null;
	
	private OnFaceTypeSelectedListener mOnFaceTypeSelectedListener;
	
	/**
	 * stamp pack index. 0-based.
	 */
	private int mCurrStampPackIndex = 0;

	/**
	 * 
	 * @param list null for kaomoji
	 * @param emotionType Stamp.TYPEID_* constants
	 */
	@SuppressWarnings("unchecked")
	public FaceTypeAdapter(Context context, int emotionType, 
			OnFaceTypeSelectedListener onFaceTypeSelectedListener) {
		mContext = context;
		mOnFaceTypeSelectedListener = onFaceTypeSelectedListener;
		mEmotionType = emotionType;
		HashMap<String, Object> cnf = Stamp.loadConfig(context);
		mAnimeCount = ((HashMap<String, Object>)cnf.get(Stamp.TYPENAME_ANIME)).size();
		mImgCount = ((HashMap<String, Object>)cnf.get(Stamp.TYPENAME_IMAGE)).size();
	}

	@Override
	public int getCount() {
        int count = 0;
        switch (mEmotionType) {
        case Stamp.TYPEID_KAOMOJI:
            count = mKMJCount;
            break;
        case Stamp.TYPEID_ANIME:
            count = mAnimeCount;
            break;
        case Stamp.TYPEID_IMAGE:
            count = mImgCount;
            break;
        default:
            break;
        }
        return count;
    }

	@Override
	public Object getItem(int position) {
		return null;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	public void setSelected(int selected) {
		mCurrStampPackIndex = selected;
//		currPackId = Stamp.getPackId(mContext, mEmotionType, currStampPackIndex);
	}

//	public String getCurrPackId() {
//		return currPackId;
//	}

    public void setEmotionType (int emotionType) {
        mEmotionType = emotionType;
    }

	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null) {
            convertView = LayoutInflater.from(mContext).inflate(R.layout.msg_facetype_item, null);
            holder = new ViewHolder();
            holder.KMJTV = (TextView) convertView.findViewById(R.id.faceitem_kaomoji);
            holder.stampView = (ImageView) convertView.findViewById(R.id.faceitem_stamp);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        if (mEmotionType == Stamp.TYPEID_ANIME || mEmotionType == Stamp.TYPEID_IMAGE) {
            holder.KMJTV.setVisibility(View.GONE);
            holder.stampView.setVisibility(View.VISIBLE);

			try {
			    final String packId = Stamp.getPackId(mContext, mEmotionType, position);
				// display [colored]packimages/<packID>.png
				InputStream is = mContext.getAssets().open(
						String.format("wowtalk/stamp/%s/%s/%s/%s.png", 
								(mEmotionType == Stamp.TYPEID_ANIME ? Stamp.TYPENAME_ANIME : Stamp.TYPENAME_IMAGE),
								packId,
								(position == mCurrStampPackIndex ? Stamp.COLOREDPACKIMAGES : Stamp.PACKIMAGES),
								packId));

				Bitmap bitmap = BitmapFactory.decodeStream(is);
				is.close();

				holder.stampView.setImageDrawable(new PngDrawable(bitmap));
				bitmap.recycle();

				holder.stampView.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
                        mOnFaceTypeSelectedListener.OnFaceTypeSelected(mEmotionType, position);
						setSelected(position);
						FaceTypeAdapter.this.notifyDataSetChanged();
					}
				});
				
//				if(position == currStampPackIdx) {
//					imgFaceType.setBackgroundColor(mContext.getResources().getColor(R.color.wowtalk_blue));
//				} else {
//					imgFaceType.setBackgroundColor(mContext.getResources().getColor(R.color.wowtalk_gray));
//				}
//				holder.stampView.setBackgroundColor(mContext.getResources().getColor(R.color.transparent));
//				if (mCurrStampPackIndex == position) {
//				    holder.stampView.setBackgroundColor(mContext.getResources().getColor(R.color.emotionboard_bg_subtype_active));
//				} else {
//				    holder.stampView.setBackgroundColor(mContext.getResources().getColor(R.color.emotionboard_bg_subtype_normal));
//				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		} else {
            holder.KMJTV.setVisibility(View.VISIBLE);
            holder.stampView.setVisibility(View.GONE);
			if (mCurrStampPackIndex == position) {
			    holder.KMJTV.setBackgroundColor(0);
			    holder.KMJTV.setTextColor(mContext.getResources().getColor(R.color.libmsg_wowtalk_blue));
			} else {
			    holder.KMJTV.setBackgroundColor(0);
			    holder.KMJTV.setTextColor(mContext.getResources().getColor(R.color.libmsg_wowtalk_gray));
			}
			switch (position) {
			case 0:
			    holder.KMJTV.setText(R.string.msg_kaomoji_based);
				break;
			case 1:
			    holder.KMJTV.setText(R.string.msg_kaomoji_happy);
				break;
			case 2:
			    holder.KMJTV.setText(R.string.msg_kaomoji_sad);
				break;
			case 3:
			    holder.KMJTV.setText(R.string.msg_kaomoji_angry);
				break;
			case 4:
			    holder.KMJTV.setText(R.string.msg_kaomoji_animal);
				break;
			case 5:
			    holder.KMJTV.setText(R.string.msg_kaomoji_others);
				break;
			default:
				break;
			}
			holder.KMJTV.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View v) {
					if(mOnFaceTypeSelectedListener != null)
						mOnFaceTypeSelectedListener.OnFaceTypeSelected(
								Stamp.TYPEID_KAOMOJI, position);
					setSelected(position);
					FaceTypeAdapter.this.notifyDataSetChanged();
				}
			});
		}

		return convertView;
	}

    static class ViewHolder {
        TextView KMJTV;
        ImageView stampView;
    }

//	private int fCalculateGridWidth(int colNum, float dpSpaceWidth, float dpColWidth) {
//		float width = dpSpaceWidth * (colNum - 1) + dpColWidth * colNum;
//		return DensityUtil.dip2px(mContext, width);
//	}

	/**
	 * set width, column number of emotion grid view. 
	 * @param colNum
	 * @param colWidth
	 */
//	private void fSetGridWidth(int colNum, float colWidth) {
//		LayoutParams params = gridStamps.getLayoutParams();
//		params.width = fCalculateGridWidth(colNum, 2.0f, colWidth);
//		gridStamps.setLayoutParams(params);
//		gridStamps.setColumnWidth(DensityUtil.dip2px(mContext, colWidth));
//		gridStamps.setNumColumns(colNum);
//	}

}
