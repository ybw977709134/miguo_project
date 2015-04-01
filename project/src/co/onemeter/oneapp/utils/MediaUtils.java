package co.onemeter.oneapp.utils;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;

import com.pzy.paint.DoodleActivity;

import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.InputBoardManager;

import java.io.FileOutputStream;

/**
 * Created by jacky on 15-4-1.
 */
public class MediaUtils {


    public static void gotoDooleForResult(Activity activity,String originfile,int request_code,String outputfilename){
        activity.startActivityForResult(
                new Intent(activity, DoodleActivity.class)
                        .putExtra(DoodleActivity.EXTRA_MAX_WIDTH, InputBoardManager.PHOTO_SEND_WIDTH)
                        .putExtra(DoodleActivity.EXTRA_MAX_HEIGHT, InputBoardManager.PHOTO_SEND_HEIGHT)
                        .putExtra(DoodleActivity.EXTRA_BACKGROUND_FILENAME, originfile)
                        .putExtra(DoodleActivity.EXTRA_OUTPUT_FILENAME, outputfilename),
                request_code
        );
    }

    public static boolean handleImageResult(Activity activity,MediaInputHelper mediaHelper,Intent data ,String[] photoPath){
        return mediaHelper.handleImageResult(
                activity,
                data,
                InputBoardManager.PHOTO_SEND_WIDTH, InputBoardManager.PHOTO_SEND_HEIGHT,
                InputBoardManager.PHOTO_THUMBNAIL_WIDTH, InputBoardManager.PHOTO_THUMBNAIL_HEIGHT,
                photoPath);
    }

    public static void compressThumb(String originfile,String thumb){
        Bitmap thumbnail = BmpUtils.decodeFile(originfile, InputBoardManager.PHOTO_THUMBNAIL_WIDTH, InputBoardManager.PHOTO_THUMBNAIL_HEIGHT);
        try {
            FileOutputStream fos = new FileOutputStream(thumb);
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    public static void pickPhoFromAlbum(Activity activity, int requestCode){
        Intent pickIntent = new Intent(Intent.ACTION_PICK,android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        activity.startActivityForResult(pickIntent, requestCode);
    }

}
