package org.wowtalk.ui.msg;

import java.util.List;

import org.wowtalk.ui.msg.StampAdapter.OnStampSelectedListener;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

public class KaomojiAdapter extends BaseAdapter {

	private static final int KAOMOJI_text_ROWS = 3;
	private Context mContext;
	private List<String> mKaomojiLists;
	private OnStampSelectedListener mOnStampSelectedListener;
	
	public KaomojiAdapter(Context context, List<String> kaomojiLists, OnStampSelectedListener onStampSelectedListener) {
		mContext = context;
		mKaomojiLists = kaomojiLists;
		mOnStampSelectedListener = onStampSelectedListener;
	}
	
//	public void setAdapterList(List<String> kaomojiLists){
//		mKaomojiLists = kaomojiLists;
//	}

	@Override
	public int getCount() {
		boolean isAliquot = (mKaomojiLists.size() % KAOMOJI_text_ROWS == 0);
		int count = isAliquot ? (mKaomojiLists.size() / KAOMOJI_text_ROWS) : (mKaomojiLists.size() / KAOMOJI_text_ROWS + 1);
		return count;
	}

	@Override
	public Object getItem(int position) {
		return mKaomojiLists.get(position);
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
			convertView = LayoutInflater.from(mContext).inflate(R.layout.msg_kaomojiitem_text, null);
			//TODO 行数不确定，用数组
			holder.kaomojiFirst = (TextView) convertView.findViewById(R.id.kaomoji_1);
			holder.kaomojiSecond = (TextView) convertView.findViewById(R.id.kaomoji_2);
			holder.kaomojiThird = (TextView) convertView.findViewById(R.id.kaomoji_3);
			KaomojiListener listener = new KaomojiListener();
			holder.kaomojiFirst.setOnClickListener(listener);
			holder.kaomojiSecond.setOnClickListener(listener);
			holder.kaomojiThird.setOnClickListener(listener);
			// TODO 设置触发事件，不用每次加载都设置？
			convertView.setTag(holder);
		}
		
		if (position < getCount() - 1){
			holder.kaomojiFirst.setText(mKaomojiLists.get(position * KAOMOJI_text_ROWS));
			holder.kaomojiSecond.setText(mKaomojiLists.get(position * KAOMOJI_text_ROWS + 1));
			holder.kaomojiThird.setText(mKaomojiLists.get(position * KAOMOJI_text_ROWS + 2));
		}
		else {
			//TODO
			// the index of first text is from position * rows.
			if (mKaomojiLists.size() == position * KAOMOJI_text_ROWS) {
				holder.kaomojiFirst.setText(mKaomojiLists.get(position * KAOMOJI_text_ROWS));
				holder.kaomojiSecond.setText("");
				holder.kaomojiThird.setText("");
			}
			else if (mKaomojiLists.size() == position * KAOMOJI_text_ROWS + 1){
				holder.kaomojiFirst.setText(mKaomojiLists.get(position * KAOMOJI_text_ROWS));
				holder.kaomojiSecond.setText(mKaomojiLists.get(position * KAOMOJI_text_ROWS + 1));
				holder.kaomojiThird.setText("");
			}
			else if (mKaomojiLists.size() == position * KAOMOJI_text_ROWS + 2){
				holder.kaomojiFirst.setText(mKaomojiLists.get(position * KAOMOJI_text_ROWS));
				holder.kaomojiSecond.setText(mKaomojiLists.get(position * KAOMOJI_text_ROWS + 1));
				holder.kaomojiThird.setText(mKaomojiLists.get(position * KAOMOJI_text_ROWS + 2));
			}
		}
		
		//TODO 触发
//		holder.imageView.setOnClickListener(new View.OnClickListener() {
//			@Override
//			public void onClick(View v) {
//				if(mOnStampSelectedListener != null)
//					mOnStampSelectedListener.OnStampSelected(new Stamp(
//						mContext,
//						stampTypeId,
//						stampPackId, 
//						Integer.toString(position + 1)));
//			}
//		});
		return convertView;
	}
	
	static class ViewHolder{
		TextView kaomojiFirst;
		TextView kaomojiSecond;
		TextView kaomojiThird;
	}
	
	private class KaomojiListener implements OnClickListener{

		@Override
		public void onClick(View v) {
			if (v instanceof TextView){
				String kaomoji = ((TextView)v).getText().toString();
				mOnStampSelectedListener.OnKomojiSelected(kaomoji);
			}
		}
		
	}

}
