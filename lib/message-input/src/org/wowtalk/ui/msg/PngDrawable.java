package org.wowtalk.ui.msg;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;

/**
 * Functions like BitmapDrawable, but holding compressed data, so less memory (~10%) is consumed.
 */
public class PngDrawable extends android.graphics.drawable.Drawable {

	private byte[] data;
	private Rect srcRect;
	private float mDensityScale = 1;
	private int mIntrinsicHeight = -1;
	private int mIntrinsicWidth = -1;
	
	public PngDrawable(Bitmap bmp) {
		mDensityScale = 1;
		init(bmp);
	}
	
	/**
	 * Create from a bitmap, with specified density.
	 * @param bmp
	 * @param density
	 */
	public PngDrawable(Bitmap bmp, float density) {
		mDensityScale = density;
		init(bmp);
	}
	
	/**
	 * Create from a bitmap, querying density from display metrics.
	 * @param res
	 * @param bmp
	 */
	public PngDrawable(Resources res, Bitmap bmp) {
		mDensityScale = res.getDisplayMetrics().density;
		init(bmp);
	}
	
	/**
	 * Create from a drawable resource, querying density from display metrics.
	 * @param res
	 * @param bmp
	 */
	public PngDrawable(Resources res, int resid) {
		mDensityScale = res.getDisplayMetrics().density;
		Bitmap bmp = BitmapFactory.decodeResource(res, resid);
		init(bmp);
		bmp.recycle();
	}
	
	public void init(Bitmap bmp) {
		// example compress ratio: 193600B->16184B
		ByteArrayOutputStream os = new ByteArrayOutputStream();
		bmp.compress(Bitmap.CompressFormat.PNG, 9, os);
		data = os.toByteArray();
		try {
			os.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		srcRect = new Rect(0, 0, bmp.getWidth(), bmp.getHeight());
		mIntrinsicHeight = (int)(srcRect.height() * mDensityScale);
		mIntrinsicWidth = (int)(srcRect.width() * mDensityScale);
	}
	
	/**
	 *  this drawable's intrinsic size == bitmap's size * density.
	 */
	@Override
	public int getIntrinsicHeight () {
		return mIntrinsicHeight;
	}
	
	/**
	 *  this drawable's intrinsic size == bitmap's size * density.
	 */
	@Override
	public int getIntrinsicWidth () {
		return mIntrinsicWidth;
	}
	
	@Override
	public void draw(Canvas canvas) {
		BitmapFactory.Options opt = new BitmapFactory.Options();
		opt.inPurgeable = true;
		Bitmap bmp = BitmapFactory.decodeByteArray(data, 0, data.length, opt);
		canvas.drawBitmap(bmp, srcRect, new Rect(0, 0, mIntrinsicWidth, mIntrinsicHeight), null);
		bmp.recycle();
	}

	@Override
	public int getOpacity() {
		return 0;
	}

	@Override
	public void setAlpha(int arg0) {
	}

	@Override
	public void setColorFilter(ColorFilter arg0) {
	}

}
