package co.onemeter.oneapp.ui;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import co.onemeter.oneapp.R;
import co.onemeter.utils.AsyncTaskExecutor;
import org.wowtalk.Log;
import org.wowtalk.api.*;
import org.wowtalk.ui.bitmapfun.util.ImageWorker;
import org.wowtalk.ui.msg.BmpUtils;
import org.wowtalk.ui.msg.FileUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Help to download, cache, update and display photo.
 */
public class PhotoDisplayHelper extends ImageWorker {

    public static class Params {
        public List<IHasPhoto> photoOwners;
        public boolean thumbnail = true;
        public int height = 400;
        public int width = 400;

        /**
         * To be useful as cache key used by {@link ImageWorker}, the string value
         * of this object should be defined properly.
         */
        @Override
        public String toString() {
            String s = "PhotoDisplayHelper.Params";
            if (null != photoOwners && !photoOwners.isEmpty()) {
                int i = 0;
                for(IHasPhoto p : photoOwners) {
                    s = s + p.getGUID();
                    if (++i >= 4)
                        break;
                }
            }
            return s;
        }
    }

    private static final int MAX_DISPLAY_WIDTH = 1024;
    private static final int MAX_DISPLAY_HEIGHT = 1024;

    private Context mContext;

    /**
	 * set of fileid.
	 */
	private static HashMap<String, ImageView> downloadingLock = new HashMap<String, ImageView>();

    public PhotoDisplayHelper(Context context) {
        super(context);
        mContext = context;

        // 由于 RoundedImageView 对 TransitionDrawable 支持不好，
        // 而 fade in 效果又要求 ImageWorker 采用 TransitionDrawable，
        // 所以关闭 fade in。
        setImageFadeIn(false);
    }

    /**
	 * Display buddy or group's photo. The photo is cached, and updated automatically.
	 * The downloading is async.
	 * 
	 * @param context
	 * @param view
	 * @param defaultResid
	 * @param target can be null.
	 * @param isThumbnail
	 */
	public static void displayPhoto(final Context context, final ImageView view,
			int defaultResid,
			final IHasPhoto target, final boolean isThumbnail) {
        displayPhoto(context, view, context.getResources().getDrawable(defaultResid),
                target, isThumbnail);
    }

    public static void displayPhoto(final Context context, final ImageView view,
                                    Drawable defaultDrawable,
                                    final IHasPhoto target, final boolean isThumbnail) {

//		view.setScaleType(isThumbnail ? ScaleType.CENTER_CROP : ScaleType.FIT_CENTER);
        view.setScaleType(ScaleType.CENTER_CROP);

        if(target == null) {
            view.setImageDrawable(defaultDrawable);
            return;
        }
		
		if(target.getPhotoUploadedTimestamp() <= 0) {
//			Log.e("PhotoDisplayHelper.displayPhoto() this buddy has never upload a photo, uid="
//					+ target.getGUID()
//					+ ", photoUploadedTimeStamp="
//					+ target.getPhotoUploadedTimestamp());
            view.setImageDrawable(defaultDrawable);
			return;
		}
		
		final String path = isThumbnail ? makeLocalThumbnailPath(context, target.getGUID())
				: makeLocalPhotoPath(context, target.getGUID());
        if (path == null) {
            view.setImageDrawable(defaultDrawable);
            return;
        }

        Bitmap bmp;
        if (new File(path).exists()
                /*&& (bmp = BmpUtils.decodeFile(path, MAX_DISPLAY_WIDTH, MAX_DISPLAY_HEIGHT)) != null*/) {
//            view.setImageDrawable(new BitmapDrawable(context.getResources(), bmp));
            BitmapDrawable drawable = new BitmapDrawable(context.getResources(), path);
            if (drawable.getBitmap() != null) {
                view.setImageDrawable(drawable);
            } else {
                view.setImageDrawable(defaultDrawable);
            }
            if(target.getPhotoUploadedTimestamp() > 0 &&
                    new File(path).lastModified() / 1000 < target.getPhotoUploadedTimestamp()) {
                fetchAndDisplayPhoto(context, view, target, isThumbnail, path);
            }
        } else {
            // file does not exists, or is corrupted
            view.setImageDrawable(defaultDrawable);
            fetchAndDisplayPhoto(context, view, target, isThumbnail, path);
        }
    }

    /**
     *
     * @param data {@link Params}
     * @return
     */
    @Override
    protected Bitmap processBitmap(Object data) {

        if (data instanceof IHasPhoto) {
            Params p = new Params();
            p.photoOwners = new ArrayList<IHasPhoto>();
            IHasPhoto i = (IHasPhoto)data;
            if (i.isHybrid()) {
                ArrayList<IHasPhoto> h = i.getHybrid(mContext);
                if (h != null && !h.isEmpty())
                    p.photoOwners.addAll(h);
            } else {
                p.photoOwners.add(i);
            }
            return processBitmap(p);
        }

        else if (data instanceof Params) {
            final Params p = (Params)data;
            if (null != p.photoOwners && !p.photoOwners.isEmpty()) {

                Bitmap[] bmpArr = new Bitmap[4];
                int bmpCnt = 0;

                for(IHasPhoto target : p.photoOwners) {

                    if (bmpCnt > 3)
                        break;

                    if(target == null) {
                        continue;
                    }

                    // target.getPhotoUploadedTimestamp() <= 0 说明未上传头像，使用默认头像
                    boolean isDefaultAvatar = false;
                    if(target.getPhotoUploadedTimestamp() <= 0) {
                        isDefaultAvatar = true;
//                        continue;
                    }

                    final String path = makeLocalThumbnailPath(mContext, target.getGUID());
                    if (path == null) {
                        continue;
                    }

                    Bitmap bmp;
                    boolean needDownload = false;
                    // 默认头像则不需要判断是否需要下载
                    if (isDefaultAvatar) {
                        int defDrawableResId=R.drawable.default_avatar_90;
                        if (target instanceof GroupChatRoom) {
                            GroupChatRoom room = (GroupChatRoom) target;
                            if (!room.isTemporaryGroup) {
                                defDrawableResId=R.drawable.default_group_avatar_90;
                            }
                        }
                        bmp = BitmapFactory.decodeResource(mContext.getResources(), defDrawableResId);
                        if (null != bmp) {
                            bmpArr[bmpCnt++] = bmp;
                        }
                    } else {
                        if (new File(path).exists()
                                && (bmp = BmpUtils.decodeFile(path, p.width,
                                        p.height)) != null) {
                            if (target.getPhotoUploadedTimestamp() > 0
                                    && new File(path).lastModified() / 1000 < target
                                    .getPhotoUploadedTimestamp()) {
                                needDownload = true;
                            } else {
                                bmpArr[bmpCnt++] = bmp;
                            }
                        } else {
                            // file does not exists, or is corrupted
                            needDownload = true;
                        }
                    }
                    if (needDownload) {
                        RemoteFileService.download(mContext, path, "", target.getRemoteThumbnailPath(),
                                new NetworkIFDelegate() {
                                    @Override
                                    public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                    }

                                    @Override
                                    public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                    }

                                    @Override
                                    public void setProgress(int i, int i2) {
                                    }
                                }, 0);
                        bmp = BmpUtils.decodeFile(path, p.width, p.height);
                        if (null != bmp) {
                            bmpArr[bmpCnt++] = bmp;
                        }
                    }
                }

                if (0 == bmpCnt)
                    return null;

                Bitmap bmp = Bitmap.createBitmap(p.width, p.height, Bitmap.Config.ARGB_4444);
                Canvas c = new Canvas(bmp);
                Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
                c.drawColor(0xffffffff);

                if (1 == bmpCnt) {
                    c.drawBitmap(bmpArr[0],
                            getCenterCorpRect(bmpArr[0].getWidth(), bmpArr[0].getHeight(), p.width, p.height),
                            new Rect(0, 0, p.width, p.height),
                            paint);
                    // recycle
                    if (null != bmpArr[0] && !bmpArr[0].isRecycled()) {
                        bmpArr[0].recycle();
                    }
                    return bmp;
                }

                // avatar layout:
                //         [2]         [2][3]
                // [0][1] [0][1]  or   [0][1]
                //
                int w = p.width / 2 - 1; // nest avatar's width
                int h = p.height / 2 - 1; // nest avatar's height
                int top0 = bmpCnt > 2 ? p.height / 2 : p.height / 4;
                int top1 = 0;
                int left1 = bmpCnt > 3 ? 0 : p.width / 4;

                c.drawBitmap(bmpArr[0],
                        getCenterCorpRect(bmpArr[0].getWidth(), bmpArr[0].getHeight(), w, h),
                        new Rect(0, top0, w, top0 + h),
                        paint);

                if (1 < bmpCnt) {
                    c.drawBitmap(bmpArr[1],
                            getCenterCorpRect(bmpArr[1].getWidth(), bmpArr[1].getHeight(), w, h),
                            new Rect(p.width / 2, top0, p.width / 2 + w, top0 + h),
                            paint);
                    // recycle
                    if (null != bmpArr[1] && !bmpArr[1].isRecycled()) {
                        bmpArr[1].recycle();
                    }
                }

                if (2 < bmpCnt) {
                    c.drawBitmap(bmpArr[2],
                            getCenterCorpRect(bmpArr[2].getWidth(), bmpArr[2].getHeight(), w, h),
                            new Rect(left1, top1, left1 + w, top1 + h),
                            paint);
                    // recycle
                    if (null != bmpArr[2] && !bmpArr[2].isRecycled()) {
                        bmpArr[2].recycle();
                    }
                }

                if (3 < bmpCnt) {
                    c.drawBitmap(bmpArr[3],
                            getCenterCorpRect(bmpArr[3].getWidth(), bmpArr[3].getHeight(), w, h),
                            new Rect(p.width / 2, top1, p.width / 2 + w, top1 + h),
                            paint);
                    // recycle
                    if (null != bmpArr[3] && !bmpArr[3].isRecycled()) {
                        bmpArr[3].recycle();
                    }
                }

                return bmp;
            }
        }

        return null;
    }

    /**
     * @param sw source width
     * @param sh source height
     * @param tw target width
     * @param th target height
     * @return
     */
    private Rect getCenterCorpRect(int sw, int sh, int tw, int th) {
        if (sh == 0 || th == 0)
            return new Rect(0, 0, 0, 0);

        if (sw * 1f / sh <= tw * 1f / th) {
            int corpHeight = th * sw / tw;
            return new Rect(0, (sh - corpHeight) / 2, sw, corpHeight);
        } else {
            int corpWidth = tw * sh / th;
            return new Rect((sw - corpWidth) / 2, 0, corpWidth, sh);
        }
    }

    public interface OnFileDownloadedListener {
		public void onFileDownloaded(String localPath);
	}
	
	/**
	 * Download & display a image file.
	 * The downloading is async.
     *
	 * @param context
	 * @param view
	 * @param defaultResid
	 * @param fileid
	 * @param ext
     * @param fileDir optional, defaults as GlobalSettings.S3_MOMENT_FILE_DIR
	 * @param onFileDownloadedListener optional, used to tell the caller the local path
	 * of the newly downloaded file, so the caller is able to update relative object in
	 * db.
	 */
	public static void displayPhoto(final Context context, ImageView view,
			final int defaultResid, final String fileid, String ext,
            final String fileDir,
			final OnFileDownloadedListener onFileDownloadedListener) {
		
		final String path = makeLocalFilePath(fileid, ext);
        if (TextUtils.isEmpty(path)) {
            view.setImageResource(defaultResid);
            return;
        }

        if (new File(path).exists()) {
            view.setScaleType(ScaleType.CENTER_CROP);
            view.setImageDrawable(new BitmapDrawable(context.getResources(), path));
            return;
        }

        if(downloadingLock.containsKey(fileid)) {
            downloadingLock.put(fileid, view);
            return;
        }

        downloadingLock.put(fileid, view);

        view.setScaleType(ScaleType.CENTER_CROP);
        view.setImageResource(defaultResid);
        view.setTag(fileid);

		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<ImageView, Integer, Void>() {

            ImageView view = null;
            boolean ok = true;

            @Override
            protected Void doInBackground(ImageView... arg0) {

                view = arg0[0];

                WowTalkWebServerIF.getInstance(context).fGetFileFromServer(fileid,
                        fileDir,
                        new NetworkIFDelegate() {

                            @Override
                            public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
                                ok = false;
                                Log.e("AvatarUtils.displayPhoto() failed to download: "
                                        + new String(arg1).toString());
                            }

                            @Override
                            public void didFinishNetworkIFCommunication(int arg0, byte[] arg1) {
                                ok = true;
                                Log.e("AvatarUtils.displayPhoto() succeed");
                            }

                            @Override
                            public void setProgress(int arg0, int arg1) {
                            }

                        }, 0, path, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if (ok) {
                    Log.e("i'm here");
                    if (fileid.equals((String) view.getTag())) {
                        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), path);
                        if (drawable.getBitmap() != null) {
                            view.setImageDrawable(drawable);
                        } else {
                            view.setImageResource(defaultResid);
                        }
                    } else {
                        Log.e("skip set image drawable for fileid " + fileid);
                    }

                    if (onFileDownloadedListener != null) {
                        onFileDownloadedListener.onFileDownloaded(path);
                    }
                }
                downloadingLock.remove(fileid);
            }

        }, view);
	}

    /**
     *
     * @param fileid
     * @param ext not required to start with ".".
     * @return
     */
	public static String makeLocalFilePath(String fileid, String ext) {
        //as Database need this function too,move it there
        //Database is in wowtalksdk,it cannot reference this class
        return Database.makeLocalFilePath(fileid,ext);
//		File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "/onemeter/.cache/file/");
//		if (! mediaStorageDir.exists()){
//			if (! mediaStorageDir.mkdirs()){
//				return null;
//			}
//		}
//        String s = mediaStorageDir.getAbsolutePath() + "/" + fileid;
//        if (ext != null) {
//            if (ext.startsWith(".")) {
//                s += ext;
//            } else {
//                s += "." + ext;
//            }
//        }
//        return s;
	}

	private static void fetchAndDisplayPhoto(final Context context,
			final ImageView view, final IHasPhoto buddy, final boolean thumbnail,
			final String path) {
		
		view.setTag(buddy.getGUID());
		
		if(downloadingLock.containsKey(buddy.getGUID())) {
			downloadingLock.put(buddy.getGUID(), view);
//			return;
		}
		downloadingLock.put(buddy.getGUID(), view);
		
		AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Integer, Void>() {

            boolean ok = true;

            @Override
            protected Void doInBackground(Void... arg0) {
                NetworkIFDelegate nd = new NetworkIFDelegate() {

                    @Override
                    public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
                        ok = false;
                        Log.e("AvatarUtils.displayPhoto() failed to download: "
                                + new String(arg1).toString());
                    }

                    @Override
                    public void didFinishNetworkIFCommunication(int arg0, byte[] arg1) {
                        ok = true;
                        Log.e("AvatarUtils.displayPhoto() succeed");
                    }

                    @Override
                    public void setProgress(int arg0, int arg1) {
                    }

                };
                if (thumbnail) {
                    RemoteFileService.download(context,
                            path,
                            "",
                            buddy.getRemoteThumbnailPath(),
                            nd,
                            0);
                } else {
                    RemoteFileService.download(context,
                            path,
                            "",
                            buddy.getRemotePhotoPath(),
                            nd,
                            0);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if (ok) {
                    if (buddy.getGUID().equals((String) view.getTag())) {
                        BitmapDrawable drawable = new BitmapDrawable(context.getResources(), path);
                        if (drawable.getBitmap() != null) {
                            view.setImageDrawable(drawable);
                        }
                    }
//					if(thumbnail) 
//						buddy.pathOfThumbNail = path;
//					else
//						buddy.pathOfPhoto = path;
                }
                downloadingLock.remove(buddy.getGUID());
            }

        });
	}

	public static String makeLocalPhotoPath(Context context, String uid) {
		return makeLocalPath(uid, "photo");
	}

	public static String makeLocalThumbnailPath(Context context, String uid) {
		return makeLocalPath(uid, "thumb");
	}

	private static String makeLocalPath(String uid, String subtype) {
		File mediaStorageDir = new File(Environment.getExternalStorageDirectory(), "/onemeter/.cache/profile/" + subtype + "/");
		if (! mediaStorageDir.exists()){
			if (! mediaStorageDir.mkdirs()){
				return "";
			}
		}
		return mediaStorageDir.getAbsolutePath() + "/" + uid + ".jpg";
	}

    /**
     * Copy photo in to local cache.
     * @param context
     * @param entity_id buddy uid or group id.
     * @param srcPath
     * @param isThumbnail
     * @return
     */
    public static boolean locallyCopy(Context context, String entity_id, String srcPath, boolean isThumbnail) {
        File dest = new File(isThumbnail
                ? PhotoDisplayHelper.makeLocalThumbnailPath(context, entity_id)
                : PhotoDisplayHelper.makeLocalPhotoPath(context, entity_id));
        return  FileUtils.copyFile(new File(srcPath), dest);
    }
}
