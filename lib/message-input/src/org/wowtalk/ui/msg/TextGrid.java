package org.wowtalk.ui.msg;

import org.wowtalk.ui.msg.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * Display text grid with variable column width. 
 */
public class TextGrid extends RelativeLayout implements OnClickListener {

	public interface OnCellClickListener {
		/**
		 * @param text
		 * @param colIdx left most is 0
		 * @param rowIdx top most is 0
		 */
		public void onCellClicked(String text, int colIdx, int rowIdx);
	}
	
	static class CellData {
		public String text;
		public int colIdx;
		public int rowIdx;
		
		public CellData(String text, int colIdx, int rowIdx) {
			this.text = text;
			this.colIdx = colIdx;
			this.rowIdx = rowIdx;
		}
	}
	
	private Context mContext;
	private LinearLayout mColumns;
	private int mRowCount = 3;
	private int mColCount = 0;
	private TextView[][] mGrid;
	private int mPaddingLeft = 0;
	private int mPaddingTop = 0;
	private int mPaddingRight = 0;
	private int mPaddingBottom = 0;
	private float mTextHeight = 15;
	private float mCellHeight = 20;
	private int mTextColor = 0xff000000;
	private Drawable mCellBg = null;
	private OnCellClickListener mListener;
	private int mReloadCounter = 0;
	private Drawable mVerticalDivDrawable = null;
	
	public TextGrid(Context context, AttributeSet attrs) {
		super(context, attrs);
		
		mContext = context;
		
		// load attrs value from style xml
		
		TypedArray appearance = context.getTheme().obtainStyledAttributes(
				attrs, R.styleable.TextGrid, 0, 0);
		mTextColor = appearance.getColor(R.styleable.TextGrid_textColor, Color.BLACK);

		mTextHeight = appearance.getDimension(R.styleable.TextGrid_textSize, mTextHeight);
		mCellHeight = appearance.getDimension(R.styleable.TextGrid_cellHeight, mCellHeight);
		mPaddingLeft = mPaddingTop = mPaddingRight = mPaddingBottom
				= (int)appearance.getDimension(R.styleable.TextGrid_padding, 0);
		mPaddingLeft = (int)appearance.getDimension(R.styleable.TextGrid_paddingLeft, mPaddingLeft);
		mPaddingTop = (int)appearance.getDimension(R.styleable.TextGrid_paddingTop, mPaddingTop);
		mPaddingRight = (int)appearance.getDimension(R.styleable.TextGrid_paddingRight, mPaddingRight);
		mPaddingBottom = (int)appearance.getDimension(R.styleable.TextGrid_paddingBottom, mPaddingBottom);
		mCellBg = appearance.getDrawable(R.styleable.TextGrid_cellBackground);
		mVerticalDivDrawable = appearance.getDrawable(R.styleable.TextGrid_verticalDivider);
        appearance.recycle();
	}
	
	public void setOnCellClickListener(OnCellClickListener l) {
		mListener = l;
	}
	
	public void loadData(final String[] textArray, int rowCount) {
		++mReloadCounter;
		this.removeAllViews();
		
		mRowCount = rowCount;
		mColCount = (int)Math.ceil(textArray.length / 3f);
		mGrid = new TextView[mRowCount][mColCount];
		mColumns = new LinearLayout(mContext);
		mColumns.setOrientation(LinearLayout.HORIZONTAL);
		this.addView(mColumns);
		
		// plan A, load all at once
//		loadDataRange(textArray, 0, textArray.length);
		
		// plan B, load a few immediately, the rest async.
		final int COLS_LOAD_ONCE = 9; // enough for first scroll
		loadDataRange(textArray, 0, mRowCount * COLS_LOAD_ONCE);
		final int reloadCounterBak = mReloadCounter;
		for(int c = COLS_LOAD_ONCE; c < mColCount; c += COLS_LOAD_ONCE) {
			if(reloadCounterBak != mReloadCounter)
				break;
			
			final int start = c * mRowCount;
			final int end = start + mRowCount * COLS_LOAD_ONCE;
			this.postDelayed(new Runnable(){
				@Override
				public void run() {
					if(reloadCounterBak != mReloadCounter)
						return;
					loadDataRange(textArray, start, end);
				}
			}, (c == COLS_LOAD_ONCE ? 250 : 0));
		}
	}

	private void loadDataRange(final String[] textArray, int begin, int end) {
		if(end > textArray.length)
			end = textArray.length;
		
		Paint p = new Paint();
		p.setTextSize(mTextHeight);
		
		for(int idx = begin; idx < end; idx += mRowCount) {
			
			int maxTextWidth = 0;
			int maxRowIdx = 0;
			for(int r = 0; r < mRowCount; ++r) {
				if(idx + r >= textArray.length)
					break;
				
				int w = (int)p.measureText(textArray[idx + r]);
				if(w > maxTextWidth) {
					maxTextWidth = w;
					maxRowIdx = r;
				}
			}
			
			final int colIdx = idx / mRowCount;
			
			final LinearLayout colLayout = new LinearLayout(mContext);
			colLayout.setOrientation(LinearLayout.VERTICAL);
			
			for(int j = 0; j < mRowCount; ++j) {
				if(idx + j >= textArray.length)
					break;

				// create layout params
				LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
						LinearLayout.LayoutParams.WRAP_CONTENT,
						(int)mCellHeight);
				lp.setMargins(2, 2, 0, 0);
				if(j == maxRowIdx) {
					lp.width = LayoutParams.WRAP_CONTENT;
				} else {
					lp.width = LayoutParams.MATCH_PARENT;
				}
				
				TextView tv = mGrid[j][colIdx] = new TextView(mContext);
				tv.setText(textArray[idx + j]);
				tv.setTag(new CellData(textArray[idx + j], colIdx, j));
				
				tv.setLayoutParams(lp);
				tv.setTextSize(mTextHeight);
				tv.setTextColor(mTextColor);
				if(mCellBg != null)
					tv.setBackgroundDrawable(mCellBg);
				tv.setOnClickListener(this);
				tv.setGravity(Gravity.CENTER);
				tv.setPadding(mPaddingLeft, mPaddingTop, mPaddingRight, mPaddingBottom);
				
				colLayout.addView(mGrid[j][colIdx]);
			}
			mColumns.addView(colLayout);
			
			if(mVerticalDivDrawable != null) {
				ImageView sep = new ImageView(mContext);
				sep.setBackgroundDrawable(mVerticalDivDrawable);
				mColumns.addView(sep);
			}
		}
	}
	
	@Override
	public void onClick(View arg0) {
		if(arg0 instanceof TextView && mListener != null) {
			TextView t = (TextView)arg0;
			if(t.getTag() != null && t.getTag() instanceof CellData) {
				CellData d = (CellData)t.getTag();
				mListener.onCellClicked(d.text, d.colIdx, d.rowIdx);
			}
		}
	}
}
