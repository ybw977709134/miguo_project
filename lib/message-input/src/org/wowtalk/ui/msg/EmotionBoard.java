package org.wowtalk.ui.msg;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.*;

import org.wowtalk.ui.HorizontalListView;
import org.wowtalk.ui.msg.StampAdapter.OnStampSelectedListener;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EmotionBoard implements FaceTypeAdapter.OnFaceTypeSelectedListener {

	/*
	 * emotion coordinates
	 */
	/**
	 * emotion type, Stamp.TYPEID_* constants
	 */
	private int currEmotionType = Stamp.TYPEID_IMAGE;
	/**
	 * stamp position index in ListView. 0-based.
	 */
	private int mCurrStampIndex = 0;

	private FaceTypeAdapter mEmotionTypeAdapter;
	private OnStampSelectedListener mOnStampSelectedListener;

	private Context mContext;
	private GridView gridFaceType;
	private ImageButton btnFaceType;
	private boolean prepared = false;
	private HorizontalListView mKaomojiListView;
	private HorizontalListView mStampListView;
	private View mVgEmojiDivHorizontal = null;
    private KaomojiAdapter mKMJAdapter;
    private StampAdapter mStampAdapter;
    private List<String> mKaomojiLists = new ArrayList<String>();
    private Map<Integer, List<String>> mKaomojiMap = new HashMap<Integer, List<String>>();
    private HashMap<Integer, ArrayList<PngDrawable>> mPngAnimeMap = new HashMap<Integer, ArrayList<PngDrawable>>();
    private HashMap<Integer, ArrayList<PngDrawable>> mPngImgMap = new HashMap<Integer, ArrayList<PngDrawable>>();
    private static String[][] mKaomojiData = new String[Stamp.KAOMOJI_TAB_COUNT][];

	public EmotionBoard(Context context, View rootView, 
			OnStampSelectedListener onStampSelectedListener) {
		mContext = context;
		mOnStampSelectedListener = onStampSelectedListener;

		gridFaceType = (GridView)rootView.findViewById(R.id.grid_facetype);
		btnFaceType = (ImageButton)rootView.findViewById(R.id.face_type);
		mKaomojiListView = (HorizontalListView)rootView.findViewById(R.id.kaomoji_wrapper);
		mStampListView = (HorizontalListView)rootView.findViewById(R.id.stamp_wrapper);
		mVgEmojiDivHorizontal = (ViewGroup)rootView.findViewById(R.id.vg_emoji_div_horizontal);

        btnFaceType.setScaleType(ImageView.ScaleType.FIT_XY);

        mStampAdapter = new StampAdapter(context, Stamp.TYPEID_IMAGE,
                Stamp.getPackId(mContext, currEmotionType, mCurrStampIndex),
                null, mOnStampSelectedListener);
        mStampListView.setAdapter(mStampAdapter);
        mKaomojiListView.setVisibility(View.GONE);
        mVgEmojiDivHorizontal.setVisibility(View.GONE);
        mStampListView.setVisibility(View.VISIBLE);

	}
	
	public boolean isPrepared() {
		return prepared;
	}

    /**
     * Setup event handlers, populate grid views.
     */
	synchronized public void prepare() {
        if(prepared)
            return;

        mEmotionTypeAdapter = new FaceTypeAdapter(
                mContext, 
                currEmotionType,
                this);
        gridFaceType.setAdapter(mEmotionTypeAdapter);

        // set the image of btnFaceType
        switch (currEmotionType) {
        case Stamp.TYPEID_KAOMOJI:
            btnFaceType.setBackgroundResource(R.drawable.kaomoji);
            break;
        case Stamp.TYPEID_IMAGE:
            btnFaceType.setBackgroundResource(R.drawable.stamp1);
            break;
        case Stamp.TYPEID_ANIME:
            btnFaceType.setBackgroundResource(R.drawable.stamp2);
            break;
        default:
            break;
        }

        btnFaceType.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                switch (currEmotionType) {
                case Stamp.TYPEID_KAOMOJI:
                    currEmotionType = Stamp.TYPEID_IMAGE;
                    mCurrStampIndex = 0;
                    btnFaceType.setBackgroundResource(R.drawable.stamp1);
                    mEmotionTypeAdapter.setSelected(0);
                    mEmotionTypeAdapter.setEmotionType(currEmotionType);
                    mEmotionTypeAdapter.notifyDataSetChanged();
                    loadStamp();
                    mKaomojiListView.setVisibility(View.GONE);
                    mVgEmojiDivHorizontal.setVisibility(View.GONE);
                    mStampListView.setVisibility(View.VISIBLE);
                    break;
                case Stamp.TYPEID_IMAGE:
                    // TODO 暂时没有anime类型，所以直接切到kaomoji
//                    currEmotionType = Stamp.TYPEID_ANIME;
//                    mCurrStampIndex = 0;
//                    btnFaceType.setBackgroundResource(R.drawable.stamp2);
//                    mEmotionTypeAdapter.setSelected(0);
//                    mEmotionTypeAdapter.setEmotionType(currEmotionType);
//                    mEmotionTypeAdapter.notifyDataSetChanged();
//                    loadStamp();
//                    mKaomojiListView.setVisibility(View.GONE);
//                    mVgEmojiDivHorizontal.setVisibility(View.GONE);
//                    mStampListView.setVisibility(View.VISIBLE);
//                    break;
                case Stamp.TYPEID_ANIME:
                    currEmotionType = Stamp.TYPEID_KAOMOJI;
                    mCurrStampIndex = 0;
                    btnFaceType.setBackgroundResource(R.drawable.kaomoji);
                    mEmotionTypeAdapter.setSelected(0);
                    mEmotionTypeAdapter.setEmotionType(currEmotionType);
                    mEmotionTypeAdapter.notifyDataSetChanged();
                    loadKaomoji();
                    mKaomojiListView.setVisibility(View.VISIBLE);
                    mVgEmojiDivHorizontal.setVisibility(View.GONE);
                    mStampListView.setVisibility(View.GONE);
                    break;
                default:
                    break;
                }
            }
        });

//		currEmotionType = Stamp.TYPEID_KAOMOJI;
//		loadKaomoji();

        currEmotionType = Stamp.TYPEID_IMAGE;
        loadStamp();

        prepared = true;
    }

	private void fSetTypeGridWidth(int colNum, float colWidth) {
		LayoutParams params = gridFaceType.getLayoutParams();
		params.width = fCalculateGridWidth(colNum, 3.0f, colWidth);
		gridFaceType.setLayoutParams(params);
		gridFaceType.setColumnWidth(DensityUtil.dip2px(mContext, colWidth));
		gridFaceType.setNumColumns(colNum);
	}
	
	private int fCalculateGridWidth(int colNum, float dpSpaceWidth, float dpColWidth) {
		float width = dpSpaceWidth * (colNum + 1) + dpColWidth * colNum;
		return DensityUtil.dip2px(mContext, width);
	}
	
    /**
     * Load kaomoji data from asset plist file into memory array.
     * @param tabIdx
     * @return
     */
    synchronized String[] loadKaomijiPlist(int tabIdx) {
        if (mKaomojiData != null && mKaomojiData[tabIdx] != null) { // already loaded ?
            return mKaomojiData[tabIdx];
        }
        
        InputStream is;
        PlistHandler handler = new PlistHandler();
        try {
            is = mContext.getAssets().open(
                    String.format("wowtalk/kaomoji/emotion%d.plist", tabIdx),
                    AssetManager.ACCESS_BUFFER);
            SAXParserFactory f = SAXParserFactory.newInstance();
            SAXParser parser = f.newSAXParser();
            parser.parse(is, handler);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParserConfigurationException e) {
            e.printStackTrace();
        } catch (SAXException e) {
            e.printStackTrace();
        }

        HashMap<String, Object> obj = handler.getMaResult();
        if(obj != null) {
            String[] arr = new String[obj.size()];
            int i = 0;
            for (Object object : obj.values()) {
                arr[i++] = String.valueOf(object);
            }
            if(tabIdx < Stamp.KAOMOJI_TAB_COUNT) {
                if(mKaomojiData == null) {
                    mKaomojiData = new String[Stamp.KAOMOJI_TAB_COUNT][];
                }
                mKaomojiData[tabIdx] = arr;
            }
            return arr;
        }
        return null;
    }

    private void loadKaomoji() {
        if (mKaomojiMap.containsKey(mCurrStampIndex)){
            mKaomojiLists.clear();
            mKaomojiLists.addAll(mKaomojiMap.get(mCurrStampIndex));
        }
        else {
            List<String> tempLists = Arrays.asList(loadKaomijiPlist(mCurrStampIndex));
            mKaomojiMap.put(mCurrStampIndex, tempLists);
            mKaomojiLists.clear();
            mKaomojiLists.addAll(tempLists);
        }

        if (null == mKMJAdapter) {
            mKMJAdapter = new KaomojiAdapter(mContext, mKaomojiLists, mOnStampSelectedListener);
            mKaomojiListView.setAdapter(mKMJAdapter);
        } else {
            mKMJAdapter.notifyDataSetChanged();
        }
        mKaomojiListView.scrollToStartPosition();

		// get the cache.
//		mKaomojiListView.setSelection(0);

//		mHScrollView.scrollTo(0, 0);
        fSetTypeGridWidth(mEmotionTypeAdapter.getCount(), 51.0f);
//		gridFaceType.setAdapter(mEmotionTypeAdapter);
	}

	private void loadStamp() {
        mStampListView.scrollToStartPosition();

        ArrayList<PngDrawable> pngDrawables = null;
        boolean isAnime = currEmotionType == Stamp.TYPEID_ANIME;
        if (isAnime) {
            pngDrawables = mPngAnimeMap.get(mCurrStampIndex);
        } else {
            pngDrawables = mPngImgMap.get(mCurrStampIndex);
        }

        if(null == pngDrawables) {
            pngDrawables = new ArrayList<PngDrawable>();
            loadStamp(isAnime, pngDrawables);
        }
        if (null == mStampAdapter) {
            mStampAdapter = new StampAdapter(mContext, currEmotionType,
                    Stamp.getPackId(mContext, currEmotionType, mCurrStampIndex),
                    pngDrawables, mOnStampSelectedListener);
            mStampListView.setAdapter(mStampAdapter);
        } else {
            mStampAdapter.setDataSource(currEmotionType, Stamp.getPackId(mContext, currEmotionType, mCurrStampIndex), pngDrawables);
            mStampAdapter.notifyDataSetChanged();
        }
    }

    private void loadStamp(boolean isAnime, ArrayList<PngDrawable> pngDrawables) {
        Bitmap bmp, bmp2;

        PngDrawable pngDrawable = null;
        AssetManager assetManager = mContext.getAssets();
        InputStream inputStream = null;
        String path = null;
        String dir = String.format("wowtalk/stamp/%s/%s/thumbs",
                isAnime ? "anime" : "image",
                Stamp.getPackId(mContext, currEmotionType, mCurrStampIndex));
        int counts = 0;
        try {
            String[] imgs = assetManager.list(dir);
            if (null != imgs) {
                counts = imgs.length;
            }
        } catch (IOException exception1) {
            exception1.printStackTrace();
        }

        for (int i = 1; i <= counts; i++) {
            try {
                // wowtalk/stamp/image/1/thumbs/1.png
                path = String.format(dir + "/%d.png", i);
                inputStream = assetManager.open(path);
                bmp = BitmapFactory.decodeStream(inputStream);
                bmp2 = BmpUtils.roundCorner(bmp,
                        (int) mContext.getResources().getDimension(R.dimen.multimedia_thumbnail_round_radius));

                float density = mContext.getResources().getDisplayMetrics().density * 1 / 1.5f;
                pngDrawable = new PngDrawable(bmp2, density);
                pngDrawables.add(pngDrawable);

                BmpUtils.recycleABitmap(bmp);;
                BmpUtils.recycleABitmap(bmp2);
            } catch (IOException exception) {
                exception.printStackTrace();
            }
        }

        if (!pngDrawables.isEmpty()) {
            if (isAnime) {
                mPngAnimeMap.put(mCurrStampIndex, pngDrawables);
            } else {
                mPngImgMap.put(mCurrStampIndex, pngDrawables);
            }
        }
    }

    @Override
	public void OnFaceTypeSelected(int type, int packIdx) {
        currEmotionType = type;
	    mCurrStampIndex = packIdx;
	    switch (type) {
        case Stamp.TYPEID_KAOMOJI:
            loadKaomoji();
            break;
        case Stamp.TYPEID_ANIME:
        case Stamp.TYPEID_IMAGE:
            loadStamp();
            break;
        default:
            break;
        }
	}
}
