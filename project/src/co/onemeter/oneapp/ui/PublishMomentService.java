package co.onemeter.oneapp.ui;

import android.content.Intent;
import android.os.IBinder;
import org.wowtalk.api.*;
import org.wowtalk.ui.msg.FileUtils;

/**
 * 发布动态。
 *
 * <p>Intent 参数</p>
 * <ul>
 *     <li>{@link #EXTRA_MOMENT}</li>
 * </ul>
 * <p>Intent 可选参数</p>
 * <ul>
 *     <li>{@link #EXTRA_IS_SURVEY}</li>
 *     <li>{@link #EXTRA_ANONYMOUS}</li>
 * </ul>
 *
 * <p>
 * User: pan
 * Date: 13-5-21
 * Time: PM4:34
 * </p>
 */
public class PublishMomentService extends android.app.Service {

    public static final String EXTRA_ACTION = "action";
    /** Extra 参数：动态对象。
     * <p>if ({@link Moment#id}.startsWith({@link Moment#ID_PLACEHOLDER_PREFIX})，
     * 则认为动态已经提交，仅上传附件，否则在上传附件成功后提交动态。</p>
     */
    public static final String EXTRA_MOMENT = "moment";
    /** Extra 参数：这是一条投票类型的动态吗？ */
    public static final String EXTRA_IS_SURVEY = "is_survey";
    /** Extra 参数：是否发布为匿名动态？ */
    public static final String EXTRA_ANONYMOUS = "anonymous";

    private static final String LOG_TAG = "PublishMomentService";

    private WowTalkWebServerIF mWeb;
    private MomentWebServerIF mMomentWeb;
    private Database mDb;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        mWeb = WowTalkWebServerIF.getInstance(this);
        mMomentWeb = MomentWebServerIF.getInstance(this);
        mDb = Database.open(this);

        handleCommand(intent);
        // We want this service to continue running until it is explicitly
        // stopped, so return sticky.
        return START_STICKY;
    }

    private void handleCommand(final Intent intent) {
        if (intent == null) return;

        new Thread(new Runnable() {
            @Override
            public void run() {
                handleCommand_uploadMomentFiles(intent);
            }
        }).start();
    }

    private void handleCommand_uploadMomentFiles(Intent intent) {
        final Moment moment = intent.getParcelableExtra(EXTRA_MOMENT);
        final boolean isSurvey = intent.getBooleanExtra(EXTRA_IS_SURVEY, false);
        final boolean anonymous = intent.getBooleanExtra(EXTRA_ANONYMOUS, false);

        if (null == moment) return;

        boolean hasMediaFiles = null != moment.multimedias && !moment.multimedias.isEmpty();

        // upload multi media files
        if (hasMediaFiles) {
            for (WFile aMomentFile : moment.multimedias) {

                final WFile file = aMomentFile;

                //
                // upload thumbnail, if exists
                //
                if (null != file.localThumbnailPath) {
                    mWeb.fPostFileToServer(
                            file.localThumbnailPath,
                            file.remoteDir,
                            new NetworkIFDelegate() {
                                @Override
                                public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                    file.thumb_fileid = new String(bytes);

                                    String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
                                            file.thumb_fileid, file.getExt());
                                    FileUtils.copyFile(file.localThumbnailPath, destFilePath);
                                }

                                @Override
                                public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                    String err = new String(bytes);
                                    android.util.Log.e(LOG_TAG, "failed to upload moment file: " + err);
                                }

                                @Override
                                public void setProgress(int i, int i2) {
                                }
                            }, 0);
                }

                //
                // upload main file
                //
                mWeb.fPostFileToServer(
                        file.localPath,
                        file.remoteDir,
                        new NetworkIFDelegate() {
                            @Override
                            public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                file.fileid = new String(bytes);

                                String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
                                        file.fileid, file.getExt());
                                FileUtils.copyFile(file.localPath, destFilePath);

                                mDb.storeMultimedia(moment, file);
                            }

                            @Override
                            public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                String err = new String(bytes);
                                android.util.Log.e(LOG_TAG, "failed to upload moment file: " + err);
                            }

                            @Override
                            public void setProgress(int i, int i2) {
                            }
                        }, 0);
            }
        }

        int errno = ErrorCode.OK;

        if (moment.id == null || moment.id.startsWith(Moment.ID_PLACEHOLDER_PREFIX)) {
            if (isSurvey) {
                errno = MomentWebServerIF.getInstance(this).fAddMomentForSurvey(moment);
            } else {
                errno = MomentWebServerIF.getInstance(this).fAddMoment(moment, anonymous);
            }
        }

        if (errno == ErrorCode.OK && hasMediaFiles) {
            for (WFile file : moment.multimedias) {
                errno = mMomentWeb.fUploadMomentMultimedia(moment.id, file);
            }
        }

        if (errno != ErrorCode.OK) {
            // TODO 通知用户
        }
    }
}
