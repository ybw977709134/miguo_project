package com.zxing.encoding;

import android.graphics.Bitmap;
import android.graphics.Matrix;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;

import java.util.Hashtable;

/**
 * @author Ryan Tang
 */
public final class EncodingHandler {
    private static final int WITH_DATA_COLOR = 0xff000000;
    private static final int WITH_NO_DATA_COLOR = 0xffffffff;

    private static final int INSIDE_IMAGE_HALF_WIDTH = 20;
    private static final int INSIDE_IMAGE_ROUND_RECT_RADIUS_SIZE=5;

    public static Bitmap createQRCode(String str, int widthAndHeight,Bitmap bmpInside){
        Bitmap bitmap=null;

        Bitmap bmpInsideUsing=null;
        if(null != bmpInside) {
            Matrix m = new Matrix();
            float sx = (float) 2*INSIDE_IMAGE_HALF_WIDTH / bmpInside.getWidth();
            float sy = (float) 2*INSIDE_IMAGE_HALF_WIDTH / bmpInside.getHeight();
            m.setScale(sx, sy);

            //set ortho angle to be same color as no data
            bmpInsideUsing = Bitmap.createBitmap(bmpInside, 0, 0, bmpInside.getWidth(), bmpInside.getHeight(), m, false);
            for (int y = 0; y < bmpInsideUsing.getHeight(); y++) {
                for (int x = 0; x < bmpInsideUsing.getWidth(); x++) {
                    if((x-INSIDE_IMAGE_HALF_WIDTH)*(x-INSIDE_IMAGE_HALF_WIDTH)+(y-INSIDE_IMAGE_HALF_WIDTH)*(y-INSIDE_IMAGE_HALF_WIDTH) >
                            INSIDE_IMAGE_HALF_WIDTH*INSIDE_IMAGE_HALF_WIDTH+(INSIDE_IMAGE_HALF_WIDTH-INSIDE_IMAGE_ROUND_RECT_RADIUS_SIZE)*(INSIDE_IMAGE_HALF_WIDTH-INSIDE_IMAGE_ROUND_RECT_RADIUS_SIZE)) {
                        bmpInsideUsing.setPixel(x,y,WITH_NO_DATA_COLOR);
                    }
                }
            }
        }

        try {
            Hashtable<EncodeHintType, String> hints = new Hashtable<EncodeHintType, String>();
            hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
            BitMatrix matrix = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, widthAndHeight, widthAndHeight);
            int width = matrix.getWidth();
            int height = matrix.getHeight();
            int halfW = width / 2;
            int halfH = height / 2;
            int[] pixels = new int[width * height];

//            for (int y = 0; y < height; y++) {
//                for (int x = 0; x < width; x++) {
//                    if (matrix.get(x, y)) {
//                        pixels[y * width + x] = BLACK;
//                    } else {
//                        pixels[y * width + x] = WHITE;
//                    }
//                }
//            }
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    if(null != bmpInsideUsing) {
                        if (x > halfW - INSIDE_IMAGE_HALF_WIDTH && x < halfW + INSIDE_IMAGE_HALF_WIDTH && y > halfH - INSIDE_IMAGE_HALF_WIDTH
                                && y < halfH + INSIDE_IMAGE_HALF_WIDTH) {
                            pixels[y * width + x] = bmpInsideUsing.getPixel(x - halfW + INSIDE_IMAGE_HALF_WIDTH, y
                                    - halfH + INSIDE_IMAGE_HALF_WIDTH);
                        } else {
                            pixels[y * width + x] = matrix.get(x, y)?WITH_DATA_COLOR:WITH_NO_DATA_COLOR;
                        }
                    } else {
                        pixels[y * width + x] = matrix.get(x, y)?WITH_DATA_COLOR:WITH_NO_DATA_COLOR;
                    }
                }
            }

            bitmap = Bitmap.createBitmap(width, height,
                    Bitmap.Config.ARGB_8888);
            bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bitmap;
    }
}
