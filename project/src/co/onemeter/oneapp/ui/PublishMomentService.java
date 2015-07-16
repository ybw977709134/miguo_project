package co.onemeter.oneapp.ui;

import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import co.onemeter.oneapp.R;
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

        Log.d("---hasMediaFiles:",hasMediaFiles+"");

        final int notiId = (int) (System.currentTimeMillis() & 0xFFFFFFFF); // unique
        final NotificationManager mNotifyManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        final NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(this);
        mBuilder.setContentTitle(getString(R.string.moment_publishing_noti_title))
                .setTicker(getString(R.string.moment_publishing_noti_title))
                .setContentText(moment.text)
                .setSmallIcon(R.drawable.icon)
                .setOngoing(true);

        final int[] errno = {ErrorCode.OK};

        // upload multi media files
        if (hasMediaFiles) {
            final int[] baseProgress = {0};
            for (WFile aMomentFile : moment.multimedias) {

                final WFile file = aMomentFile;

                //
                // upload thumbnail, if exists
                //
                if (null != file.localThumbnailPath) {
                    mWeb.fPostFileToServer(
                            file.localThumbnailPath,
                            file.remoteDir,
                            true, new NetworkIFDelegate() {
                                @Override
                                public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                    file.thumb_fileid = new String(bytes);

                                    String destFilePath = PhotoDisplayHelper.makeLocalFilePath(
                                            file.thumb_fileid, file.getExt());
                                    FileUtils.copyFile(file.localThumbnailPath, destFilePath);
                                }

                                @Override
                                public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                    errno[0] = ErrorCode.LOCAL_UPLOAD_FAILED;
                                    String err = new String(bytes);
                                    android.util.Log.e(LOG_TAG, "failed to upload moment file: " + err);
                                }

                                @Override
                                public void setProgress(int tag, int progress) {
                                    // 假设缩略图占上传工作量的5%
                                    progress = (int) (baseProgress[0] + progress * (0.05f / (moment.multimedias.size())));
                                    mBuilder.setProgress(100, progress, false);
                                    mNotifyManager.notify(notiId, mBuilder.build());
                                }
                            }, 0);
                }

                if (errno[0] != ErrorCode.OK) {
                    break;
                }

                //
                // upload main file
                //
                mWeb.fPostFileToServer(
                        file.localPath,
                        file.remoteDir,
                        true, new NetworkIFDelegate() {
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
                                errno[0] = ErrorCode.LOCAL_UPLOAD_FAILED;
                                String err = new String(bytes);
                                android.util.Log.e(LOG_TAG, "failed to upload moment file: " + err);
                            }

                            @Override
                            public void setProgress(int tag, int progress) {
                                // 假设占上传工作量的95%
                                progress = (int) (baseProgress[0] + progress * (0.95f / (moment.multimedias.size())));
                                mBuilder.setProgress(100, progress, false);
                                mNotifyManager.notify(notiId, mBuilder.build());
                            }
                        }, 0);

                baseProgress[0] += 100f / moment.multimedias.size();

                if (errno[0] != ErrorCode.OK) {
                    break;
                }
            }
        }

        if (errno[0] == ErrorCode.OK &&
                (moment.id == null || moment.id.startsWith(Moment.ID_PLACEHOLDER_PREFIX))) {
            if (isSurvey) {
                errno[0] = MomentWebServerIF.getInstance(this).fAddMomentForSurvey(moment);
            } else {
                errno[0] = MomentWebServerIF.getInstance(this).fAddMoment(moment, anonymous);
            }
        }

        if (errno[0] == ErrorCode.OK && hasMediaFiles) {
            Log.d("---fUploadMomentMultimedia:","fUploadMomentMultimedia");
            for (WFile file : moment.multimedias) {
                errno[0] = mMomentWeb.fUploadMomentMultimedia(moment.id, file);
            }
        }

        if (errno[0] == ErrorCode.OK) {
            mNotifyManager.cancel(notiId);
        } else {
            // 从本地数据库中移除该动态
            new Database(this).deleteMoment(moment.id, false);

            // 点击进入动态编辑页面，从而可以重新发布
            // hack: dummy random action to avoid extra data being overrode
            Intent activityIntent = new Intent(this, CreateNormalMomentWithTagActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(CreateNormalMomentWithTagActivity.EXTRA_MOMENT, moment)
                    .setAction(Long.toString(System.currentTimeMillis()));

            PendingIntent pendingIntent = PendingIntent.getActivity(
                    this, 1, activityIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            mBuilder.setContentTitle(getString(R.string.moment_publishing_failed))
                    .setTicker(getString(R.string.moment_publishing_failed))
                    .setContentText(getString(R.string.moment_publishing_click_to_retry) + " " + moment.text)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true);

            mNotifyManager.notify(notiId, mBuilder.build());
        }
    }
}
