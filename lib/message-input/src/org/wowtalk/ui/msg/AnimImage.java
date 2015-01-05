package org.wowtalk.ui.msg;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.json.JSONException;
import org.json.JSONObject;
import org.wowtalk.Log;
import org.wowtalk.api.NetworkIFDelegate;
import org.wowtalk.api.WebServerIF;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;

public class AnimImage extends ImageView implements OnClickListener {

	Context mContext;
	private static final int ANIM_DURATION = 100;

	private int imgWidth = 220;

	private int imgHeight = 220;

	private String mSourceCode;
	private String packid;

	private String stampid;

	private String type = "stamp_image";

	private AnimationDrawable anim = null;

	private Drawable firstFrame;

	private boolean isRunning = false;

	private int imgCnt = 1;

	private float mDensity = 1;
	
	/**
	 * size of stamp bmp is 220x220, which is a bit larger for current mobile phone screen,
	 * so DENSITY_SCALE is < 1.
	 */
	private static final float PNGDRAWABLE_DENSITY_SCALE = 1 / 2.5f;

	public AnimImage(Context context, AttributeSet attrs, int style) {
		super(context, attrs, style);
		mContext = context;
		mDensity = context.getResources().getDisplayMetrics().density * PNGDRAWABLE_DENSITY_SCALE;
	}

	public AnimImage(Context context, AttributeSet attrs) {
		super(context, attrs);
		mContext = context;
		mDensity = context.getResources().getDisplayMetrics().density * PNGDRAWABLE_DENSITY_SCALE;
	}

	public AnimImage(Context context) {
		super(context);
		mContext = context;
		mDensity = context.getResources().getDisplayMetrics().density * PNGDRAWABLE_DENSITY_SCALE;
	}

	private int getImageCount() {
		return imgCnt;
	}

	public void setSourceCode(String sourceCode) {
		if(mSourceCode != null && mSourceCode.equals(sourceCode))
			return;
		
		mSourceCode = sourceCode;
		anim = null;
		
		// sample: {"stamp_image_w":220,"stamptype":"stamp_image","packid":"3","stampid":"1","stamp_image_h":220,"filepath":"stamp/images/3/1.png"}

		//num = position;
		String remoteFilePath = null;
		try {
			JSONObject json = new JSONObject(sourceCode);
			type = json.getString("stamptype");
			if (type.equals("stamp_anime")) {
				imgWidth = json.getInt("stamp_anime_w");
				imgHeight = json.getInt("stamp_anime_h");
			} else {
				imgWidth = json.getInt("stamp_image_w");
				imgHeight = json.getInt("stamp_image_h");
			}
			packid = json.getString("packid");
			stampid = json.getString("stampid");
			remoteFilePath = json.getString("filepath");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		
		/* relative to
		 *  <assets>/wowtalk
		 *  or
		 *  <sdcard>/<Stamp.HOME>/
		 */
		String localFilePath = String.format(
				"stamp/%s/%s/%s/%s.png", 
				(type.equals(Stamp.JSON_STAMPTYPE_ANIME) ? "anime" : "image"),
				packid,
				(type.equals(Stamp.JSON_STAMPTYPE_ANIME) ? "firstframe" : "contents"),
				stampid);
		
		boolean notfound = false;
		try {
			InputStream is = mContext.getAssets().open("wowtalk/" + localFilePath);
			if(is != null) {
				setBackground(is);
				is.close();
			} else {
				notfound = true;
			}
		} catch (IOException e) {
			notfound = true;
			e.printStackTrace();
		}
		
		File file = null;
		if(notfound) {
			// try external storage
			File sdDir = new File(Environment.getExternalStorageDirectory(), Stamp.HOME);
			file = new File(sdDir, localFilePath);
			
			if(file.exists()) {
				setBackground(file);
				notfound = false;
			}
		}
		
		if(notfound) {
			// download & save to external storage
			
			setBackground(R.drawable.sms_default_pic);
			
			final File ffile = file;
			final String fremoteFilePath = remoteFilePath;

			new AsyncTask<Void, Integer, Void>(){
                private int mFininshedPercent;

				@Override
				protected Void doInBackground(Void... params) {
				    WebServerIF.getInstance(mContext).fGetFileFromShop(fremoteFilePath,
				            new NetworkIFDelegate() {

                        @Override
                        public void didFailNetworkIFCommunication(int arg0,
                                byte[] arg1) {
                            Log.e("failed to download " + ffile.getAbsolutePath());
                        }

                        @Override
                        public void didFinishNetworkIFCommunication(int arg0,
                                byte[] arg1) {
                            AnimImage.this.post(new Runnable(){
                                @Override
                                public void run() {
                                    File firstFrameFile = ffile;
                                    // get the first frame, if the type is anime.
                                    if (Stamp.JSON_STAMPTYPE_ANIME.equals(type)) {
                                        try {
                                            // the temp file to write the first frame
                                            String tempFilePath = String.format(
                                                    "stamp/anime/%s/firstframe/temp.png", 
                                                    packid);
                                            File stampDir = new File(Environment.getExternalStorageDirectory(), Stamp.HOME);
                                            File tempFile = new File(stampDir, tempFilePath);
                                            if (!tempFile.exists()) {
                                                tempFile.createNewFile();
                                            }
                                            FileOutputStream outputStream = new FileOutputStream(tempFile);
                                            FileInputStream inputStream = new FileInputStream(firstFrameFile);
                                            BitmapFactory.Options opt = new BitmapFactory.Options();
                                            opt.inPurgeable = true;
                                            final Bitmap largeBmp = BitmapFactory.decodeStream(inputStream, null, opt);
                                            if (null != largeBmp) {
                                                // get the first frame
                                                int count = largeBmp.getWidth() / largeBmp.getHeight();
                                                if (count > 0) {
                                                    Bitmap bmp = Bitmap.createBitmap(largeBmp, 0, 0, imgWidth, imgHeight);
                                                    bmp.compress(Bitmap.CompressFormat.PNG, 100, outputStream);
                                                    bmp.recycle();
                                                }
                                                largeBmp.recycle();
                                            }
                                            inputStream.close();
                                            outputStream.close();
                                            // rename the file
                                            firstFrameFile.delete();
                                            tempFile.renameTo(firstFrameFile);
                                        } catch (IOException exception) {
                                            exception.printStackTrace();
                                        }
                                    }
                                    setBackground(firstFrameFile);
                                }
                            });
                        }

                        @Override
                        public void setProgress(int arg0, int arg1) {
                            mFininshedPercent = arg1;
                        }

                    }, 0, ffile.getAbsolutePath());
                    return null;
                }

                protected void onPostExecute(Void result) {
                    Log.i("Finish download stamp " + mFininshedPercent + "%");
                    if ((100 != mFininshedPercent) && ffile.exists()) {
                        Log.e("download stamp failure, delete the file " + ffile.getAbsolutePath());
                        ffile.delete();
                    }
                };
            }.execute((Void)null);
		}
	}

	private void setBackground(File file) {
		try {
			Bitmap bm = BitmapFactory.decodeFile(file.getAbsolutePath());
			firstFrame = new PngDrawable(bm, mDensity);
			bm.recycle();
			this.setBackgroundDrawable(firstFrame);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setBackground(InputStream file) {
		try {
			Bitmap bm = BitmapFactory.decodeStream(file);
			firstFrame = new PngDrawable(bm, mDensity);
			bm.recycle();
			this.setBackgroundDrawable(firstFrame);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void setBackground(int resId) {
		try {
			firstFrame = mContext.getResources().getDrawable(resId);
			this.setBackgroundDrawable(firstFrame);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * start stamp animation.
	 */
	public void playStamp() {

        if (Stamp.JSON_STAMPTYPE_IMAGE.equals(type)) {
            return;
        }

		if(anim != null) {
			if(anim.isRunning()) return;
			setBackgroundDrawable(anim);
			isRunning = true;
			anim.start();
			return;
		}
		
		/* relative to
		 *  <assets>/wowtalk
		 *  or
		 *  <sdcard>/<Stamp.HOME>/
		 */
		String localFilePath = String.format("stamp/%s/%s/contents/%s.png",
				type.substring(6, type.length()),
				packid,
				stampid); // stamp/image/2/contents/1.png
		
		boolean notfound = false;
		try {
			InputStream is = mContext.getAssets().open("wowtalk/" + localFilePath);
			if(is != null) {
				playStampHelper(is);
				is.close();
			} else {
				notfound = true;
			}
		} catch (IOException e) {
			notfound = true;
			e.printStackTrace();
		}
		
		File file = null;
		if(notfound) {
			// try external storage
			File sdDir = new File(Environment.getExternalStorageDirectory(), Stamp.HOME);
			file = new File(sdDir, localFilePath);
			
			if(file.exists()) {
				playStampHelper(file);
				notfound = false;
			}
		}
		
		if(notfound) { 
			// download & save to external storage
			final File destFile = file;
			
			final String remoteFilePath = String.format("stamp/%s/%s/%s.png",
					type.substring(6, type.length()),
					packid,
					stampid); // stamp/image/2/1.png

			new AsyncTask<Void, Integer, Void>(){
                private int mFininshedPercent;

				@Override
				protected Void doInBackground(Void... arg0) {
					WebServerIF.getInstance(mContext).fGetFileFromShop(remoteFilePath,
							new NetworkIFDelegate() {

						@Override
						public void didFailNetworkIFCommunication(int arg0,
								byte[] arg1) {
                            Log.e("failed to download " + destFile.getAbsolutePath());
						}

						@Override
						public void didFinishNetworkIFCommunication(int arg0,
								byte[] arg1) {
							AnimImage.this.post(new Runnable() {
                                @Override
                                public void run() {
                                    playStampHelper(destFile);
                                }
                            });
						}

						@Override
						public void setProgress(int arg0, int arg1) {
                                mFininshedPercent = arg1;
						}

					}, 0, destFile.getAbsolutePath());
					return null;
				}

                protected void onPostExecute(Void result) {
                    Log.i("Finish download play stamp " + mFininshedPercent + "%");
                    if ((mFininshedPercent != 100) && destFile.exists()) {
                        Log.e("download play stamp failure, delete the file " + destFile.getAbsolutePath());
                        destFile.delete();
                    }
                };
			}.execute((Void)null);
		}
	}

	/**
	 * Note: 
	 * 1, may cost several seconds to complete;
	 * 2, it's should be but currently NOT safe to run on async task.
	 * @param bmpFile
	 */
	private void playStampHelper(File bmpFile) {
		try {
			FileInputStream fis = new FileInputStream(bmpFile);
			playStampHelper(fis);
			fis.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void playStampHelper(InputStream bmpFile) {
		try {
			BitmapFactory.Options opt = new BitmapFactory.Options();
			opt.inPurgeable = true;
			final Bitmap largeBmp = BitmapFactory.decodeStream(bmpFile, null, opt);
			/* WARNING: possible exception --
			 * java.lang.OutOfMemoryError: bitmap size exceeds VM budget
			 */
	
			anim = new AnimationDrawable();
			anim.setOneShot(false);
			setBackgroundDrawable(anim);
			if (null != largeBmp) {
				imgCnt = largeBmp.getWidth() / largeBmp.getHeight();
				for (int i = 0; i < getImageCount(); i++) {
					Bitmap bmp = Bitmap.createBitmap(largeBmp, imgWidth * i, 0, imgWidth, imgHeight);
					Drawable drawable = new PngDrawable(bmp, mDensity); 
					bmp.recycle();
					anim.addFrame(drawable, ANIM_DURATION);
				}
				largeBmp.recycle();
				isRunning = true;
				anim.start();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void pauseStamp() {
		isRunning = false;
		if (anim == null || !anim.isRunning()) {
			return;
		} else {
			anim.stop();
			//this.setBackgroundDrawable(firstFrame); // cause double firstFrame?
		}
	}

	public static int px2dip(Context context, float pxValue){ 
		final float scale = context.getResources().getDisplayMetrics().density; 
		return (int)(pxValue / scale + 0.5f); 
	}

	public static int dip2px(Context context, float dipValue){ 
		final float scale = context.getResources().getDisplayMetrics().density; 
		return (int)(dipValue * scale + 0.5f); 
	}

	public void setPerSize(int width, int height) {
		imgWidth = width;
		imgHeight = height;
	}

	public void clearBitmap() {
		if(anim != null)
			anim.stop();
	}

	public boolean onTouchEvent(MotionEvent event) {
		switch (event.getAction()) {
		case MotionEvent.ACTION_DOWN:
			if (!isRunning) {
				playStamp();
			} else {
				pauseStamp();
			}
			break;
		default:
			break;
		}
		return super.onTouchEvent(event);
	}

	@Override
	public void onClick(View v) {
	}

}
