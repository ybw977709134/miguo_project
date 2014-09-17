package org.wowtalk.ui.msg;

import android.content.Context;
import android.graphics.*;
import android.graphics.Bitmap.Config;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.InputStream;

public class BmpUtils {
	private static int calculateInSampleSize(
			BitmapFactory.Options options, int reqSize) {
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqSize || width > reqSize) {
			if (width > height) {
				inSampleSize = Math.round((float)height / (float)reqSize);
			} else {
				inSampleSize = Math.round((float)width / (float)reqSize);
			}
			
			// round to powers of 2(larger is preferred), e.g.,
			// 2=>2
			// 3=>4
			for(int i = 1, pow = 2; i < 10; ++i, pow *= 2) {
				if(inSampleSize > pow && inSampleSize <= pow * 2) {
					inSampleSize = pow * 2;
					break;
				}
			}
		}
		
		return inSampleSize;
	}
	
	/**
	 * make round corner.
	 * @param bitmap
	 * @param roundPx
	 * @return
	 */
	public static Bitmap roundCorner(Bitmap bitmap, int roundPx) {
		Bitmap output = Bitmap.createBitmap(bitmap.getWidth(),
				bitmap.getHeight(), Config.ARGB_4444);
		Canvas canvas = new Canvas(output);

		final int color = 0xff424242;
		final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
		final Rect rect = new Rect(0, 0, bitmap.getWidth(), bitmap.getHeight());
		final RectF rectF = new RectF(rect);

		canvas.drawARGB(0, 0, 0, 0);
		paint.setColor(color);
		canvas.drawRoundRect(rectF, roundPx, roundPx, paint);

		paint.setXfermode(new PorterDuffXfermode(android.graphics.PorterDuff.Mode.SRC_IN));
		canvas.drawBitmap(bitmap, rect, rect, paint);

		return output;
	}

    /**
     * Decode bitmap from file, automatic rotate according to EXIF info.
     * @param path
     * @return
     */
    public static Bitmap decodeFile(String path) {
        final int REQUIRED_SIZE = 70;
        return decodeFile(path, REQUIRED_SIZE, REQUIRED_SIZE);
    }

    public static Bitmap decodeFile(String path, int requiredWidth, int requiredHeight) {
        return decodeFile(path, requiredWidth, requiredHeight, false);
    }

    public static Bitmap decodeFile(String path, int requiredWidth, int requiredHeight, boolean requireBoth) {
        //you can provide file path here
        int orientation;
        try {
            if (path == null) {
                return null;
            }
            // decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(path, o);
            // Find the correct scale value. It should be the power of 2.
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if ((requireBoth && width_tmp  < requiredWidth && height_tmp < requiredHeight)
                    || (!requireBoth && (width_tmp <requiredWidth || height_tmp < requiredHeight)))
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            // decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            Bitmap bm = BitmapFactory.decodeFile(path, o2);
            Bitmap bitmap = bm;

            ExifInterface exif = new ExifInterface(path);

            orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, 1);

            Log.e("ExifInteface .........", "rotation =" + orientation);

            //          exif.setAttribute(ExifInterface.ORIENTATION_ROTATE_90, 90);

            Log.e("orientation", "" + orientation);
            Matrix m = new Matrix();

            if ((orientation == ExifInterface.ORIENTATION_ROTATE_180)) {
                m.postRotate(180);
                //              m.postScale((float) bm.getWidth(), (float) bm.getHeight());
                // if(m.preRotate(90)){
                Log.e("in orientation", "" + orientation);
                bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                        bm.getHeight(), m, true);
                recycleABitmap(bm);
                return bitmap;
            } else if (orientation == ExifInterface.ORIENTATION_ROTATE_90) {
                m.postRotate(90);
                Log.e("in orientation", "" + orientation);
                bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                        bm.getHeight(), m, true);
                recycleABitmap(bm);
                return bitmap;
            }
            else if (orientation == ExifInterface.ORIENTATION_ROTATE_270) {
                m.postRotate(270);
                Log.e("in orientation", "" + orientation);
                bitmap = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(),
                        bm.getHeight(), m, true);
                recycleABitmap(bm);
                return bitmap;
            }
            return bitmap;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void trigerGC() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(500);
                    System.gc();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    public static void recycleABitmap(Bitmap bmp) {
        if(null != bmp && !bmp.isRecycled()) {
            bmp.recycle();
//            System.gc();
        }
    }

    public static Bitmap decodeUri(Context context, Uri uri, int requiredWidth, int requiredHeight) {
        return decodeUri(context, uri, requiredWidth, requiredHeight, false);
    }

    public static Bitmap decodeUri(Context context, Uri uri, int requiredWidth, int requiredHeight, boolean requireBoth) {
        try {
            if (uri == null) {
                return null;
            }

            // decode image size
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream is = context.getContentResolver().openInputStream(uri);
            BitmapFactory.decodeStream(is, null, o);
            is.close();

            // Find the correct scale value. It should be the power of 2.
            int width_tmp = o.outWidth, height_tmp = o.outHeight;
            int scale = 1;
            while (true) {
                if ((requireBoth && width_tmp  < requiredWidth && height_tmp < requiredHeight)
                        || (!requireBoth && (width_tmp <requiredWidth || height_tmp < requiredHeight)))
                    break;
                width_tmp /= 2;
                height_tmp /= 2;
                scale *= 2;
            }

            // decode with inSampleSize
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = scale;
            is = context.getContentResolver().openInputStream(uri);
            Bitmap bmp = BitmapFactory.decodeStream(is, null, o2);
            is.close();
            return bmp;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
