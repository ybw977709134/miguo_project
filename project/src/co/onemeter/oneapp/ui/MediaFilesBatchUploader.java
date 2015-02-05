package co.onemeter.oneapp.ui;

import android.content.Context;
import android.text.TextUtils;
import org.wowtalk.api.NetworkIFDelegate;
import org.wowtalk.api.WFile;
import org.wowtalk.api.WowTalkWebServerIF;

import java.util.List;

/**
 * 批量上传多媒体文件。
 * Created by pzy on 11/8/14.
 */
public class MediaFilesBatchUploader {
    public interface OnMediaFilesUploadedListener {
        public void onAllMediaFilesUploaded(List<WFile> files);
        public void onMediaFilesUploadFailed(List<WFile> files);
    }

    public static void upload(Context context, List<WFile> files, OnMediaFilesUploadedListener callback) {
        recursivelyUpload(context, files, 0, callback);
    }

    private static void recursivelyUpload(
            final Context context, final List<WFile> files, final int index,
            final OnMediaFilesUploadedListener callback) {

        final WowTalkWebServerIF wowweb = WowTalkWebServerIF.getInstance(context);
        final WFile f = files.get(index);
        if (TextUtils.isEmpty(f.remoteDir)) {
            throw new RuntimeException("WFile.remoteDir not set before uploading.");
        }
        wowweb.fPostFileToServer(f.localPath, f.remoteDir,
                false, new NetworkIFDelegate() {
                    @Override
                    public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                        Log.i(String.format("uploading event media successful for file #%s", new String(bytes)));
                        f.fileid = new String(bytes);

                        if (TextUtils.isEmpty(f.localThumbnailPath)) {
                            next();
                        } else {
                            // upload thumbnail
                            wowweb.fPostFileToServer(f.localThumbnailPath, f.remoteDir,
                                    false, new NetworkIFDelegate() {
                                        @Override
                                        public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                            f.thumb_fileid = new String(bytes);
                                            next();
                                        }

                                        @Override
                                        public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                            if (callback != null)
                                                callback.onMediaFilesUploadFailed(files);
                                        }

                                        @Override
                                        public void setProgress(int i, int i2) {

                                        }
                                    }, 0);
                        }
                    }

                    @Override
                    public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                        Log.e(String.format("uploading event media failed for file #%s", f.localPath));
                        if (callback != null)
                            callback.onMediaFilesUploadFailed(files);
                    }

                    @Override
                    public void setProgress(int i, int i2) {
                        Log.i(String.format("uploading event media: %d%%", i2));
                    }

                    private void next() {
                        if (index + 1 < files.size()) {
                            recursivelyUpload(context, files, index + 1, callback);
                        } else if (callback != null) {
                            callback.onAllMediaFilesUploaded(files);
                        }
                    }
                }, 0);
    }
}
