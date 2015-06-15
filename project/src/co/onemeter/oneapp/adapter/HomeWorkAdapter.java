package co.onemeter.oneapp.adapter;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.TextUtils;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import org.wowtalk.api.Buddy;
import org.wowtalk.api.Database;
import org.wowtalk.api.GetLessonHomework;
import org.wowtalk.api.GlobalSetting;
import org.wowtalk.api.HomeWorkResult;
import org.wowtalk.api.Moment;
import org.wowtalk.api.NetworkIFDelegate;
import org.wowtalk.api.PrefUtil;
import org.wowtalk.api.WFile;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.ui.RecyclingImageView;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.DensityUtil;
import co.onemeter.oneapp.ui.ImageViewActivity;
import co.onemeter.oneapp.ui.Log;
import co.onemeter.oneapp.ui.PhotoDisplayHelper;
import co.onemeter.oneapp.utils.Utils;
import co.onemeter.utils.AsyncTaskExecutor;

/**
 * 用于加载作业信息
 * Created by hutianfeng on 15-5-28.
 */
public class HomeWorkAdapter extends BaseAdapter{

    private Context context;
    private List<HomeWorkResult> stuResultList;

    private ImageResizer mImageResizer;//处理图片
    private Database mDb;
    private GetLessonHomework getLessonHomework; 


    public HomeWorkAdapter (Context context,List<HomeWorkResult> stuResultList,ImageResizer mImageResizer,GetLessonHomework getLessonHomework) {
        this.context = context;
        this.stuResultList = stuResultList;
        this.mImageResizer = mImageResizer;
        this.mDb = new Database(context);
        this.getLessonHomework = getLessonHomework;
    }

    @Override
    public int getCount() {
        return stuResultList.size();
    }

    @Override
    public Object getItem(int position) {
        return stuResultList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {

        //还没有初始化
//        Moment moment = mDb.fetchMoment(String.valueOf(stuResultList.get(position).moment_id));
    	
    	
    	Moment moment = mDb.fetchMoment(String.valueOf(stuResultList.get(position).moment_id));
			

        ArrayList<WFile> photoFiles = new ArrayList<WFile>();

        if (moment != null && moment.multimedias != null && !moment.multimedias.isEmpty()) {
        	for (WFile file : moment.multimedias) {
                if (!file.isAudioByExt()) {
                    photoFiles.add(file);
                }
            }
        }
        

        ViewHolder holder= null;
        if (convertView == null) {
            //老师和学生的作业列表稍有不同
            if (PrefUtil.getInstance(context).getMyAccountType() == Buddy.ACCOUNT_TYPE_TEACHER) {//老师
                convertView = LayoutInflater.from(context).inflate(R.layout.item_homework_teacher,null);
            } else {//学生
                convertView = LayoutInflater.from(context).inflate(R.layout.item_homework_student,null);
            }

            holder = new ViewHolder();
            //作业
            holder.layout_homework_comment = (LinearLayout) convertView.findViewById(R.id.layout_homework_comment);
            holder.date_homework_comment = (TextView) convertView.findViewById(R.id.date_homework_comment);
            holder.imageTable = (TableLayout) convertView.findViewById(R.id.imageTable);
            holder.textView_homework_title = (TextView) convertView.findViewById(R.id.textView_homework_title);

            //评论
            holder.layout_homework_review = (LinearLayout) convertView.findViewById(R.id.layout_homework_review);
            holder.date_homework_review = (TextView) convertView.findViewById(R.id.date_homework_review);
            holder.ratingBar_confirm = (RatingBar) convertView.findViewById(R.id.ratingBar_confirm);
            holder.ratingBar_timely = (RatingBar) convertView.findViewById(R.id.ratingBar_timely);
            holder.ratingBar_exact = (RatingBar) convertView.findViewById(R.id.ratingBar_exact);
            holder.textView_homework_review = (TextView) convertView.findViewById(R.id.textView_homework_review);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        long date_comment = stuResultList.get(position).stuMoment.insert_timestamp;
        String date_comment_str = Utils.stampsToDateTime(date_comment);
        holder.date_homework_comment.setText(date_comment_str);

        //下载图片
        if (photoFiles.size() == 0) {
            holder.imageTable.setVisibility(View.GONE);
        } else {
            holder.imageTable.setVisibility(View.VISIBLE);
            setImageLayout(context, moment,mImageResizer, photoFiles, holder.imageTable);
        }

        //老师布置作业的标题
        if (holder.textView_homework_title.getText().length() == 0) {
            holder.textView_homework_title.setVisibility(View.GONE);
        } else {
            holder.textView_homework_title.setVisibility(View.VISIBLE);
            holder.textView_homework_title.setText(stuResultList.get(position).stuMoment.text_content);
        }

        //判断老师是否评论了作业
        if (stuResultList.get(position).homeWorkReview == null) {
            holder.layout_homework_review.setVisibility(View.GONE);
        } else {
            holder.layout_homework_review.setVisibility(View.VISIBLE);
            //批改时间
//            holder.date_homework_review.setText(System.currentTimeMillis()+"");
            holder.ratingBar_confirm.setRating(stuResultList.get(position).homeWorkReview.rank1);
            holder.ratingBar_timely.setRating(stuResultList.get(position).homeWorkReview.rank2);
            holder.ratingBar_exact.setRating(stuResultList.get(position).homeWorkReview.rank3);

            holder.ratingBar_confirm.setIsIndicator(true);
            holder.ratingBar_timely.setIsIndicator(true);
            holder.ratingBar_exact.setIsIndicator(true);


            if (holder.textView_homework_review.getText().length() == 0) {
                holder.textView_homework_review.setVisibility(View.GONE);
            } else {
                holder.textView_homework_review.setVisibility(View.VISIBLE);
                holder.textView_homework_review.setText(stuResultList.get(position).homeWorkReview.text);
            }
        }

        return convertView;
    }
    
    
    public static void setImageLayout(
            final Context context, Moment moment, final ImageResizer mImageResizer, final ArrayList<WFile> files, TableLayout table) {

        if (files.isEmpty()) {
            table.setVisibility(View.GONE);
            return;
        }
        table.removeAllViews();
        table.setVisibility(View.VISIBLE);

        //用于判断一张图片的时候图片显示的大一点
        final int photosize = files.size();
        int photoNum = files.size();//图片数量，此变量循环中会变化
        int columnNum = (int) Math.sqrt(photoNum - 1) + 1;

        //三张图片将它们显示一行
        if(photosize == 3){
            columnNum = photosize;
        }
        int rowNum = photoNum / columnNum + (photoNum % columnNum == 0 ? 0 : 1);

        final String[] thumbnailPathList = new String[photoNum];//根据图片的数量来初始化缩略路径数组
        int i = 0;
        for (WFile file : files) {
            // 如果是本地创建的动态，那么 localThumbnailPath 和 localPath 应该已经指向本地文件了，
            // 这种情况下应该保留。
            if (TextUtils.isEmpty(file.localThumbnailPath) || !new File(file.localThumbnailPath).exists()) {
                thumbnailPathList[i] = file.localThumbnailPath
                        = PhotoDisplayHelper.makeLocalFilePath(file.thumb_fileid, file.getExt());
            } else {
                thumbnailPathList[i] = file.localThumbnailPath;
            }
            if (TextUtils.isEmpty(file.localPath) || !new File(file.localPath).exists()) {
                file.localPath = PhotoDisplayHelper.makeLocalFilePath(file.fileid, file.getExt());
            }
            if (++i >= photoNum)
                break;
        }
        final boolean isVedio = moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_VIDEO);
        for (i = 0; i < rowNum; i++) {
            TableRow tableRow = new TableRow(context);
            for (int j  = 0; j < (photoNum <= columnNum ? photoNum : columnNum); j++) {
                final int photoIdx = i * columnNum + j;
                RecyclingImageView imageView = new RecyclingImageView(context);
                imageView.setClickDim(true);

                //对视频的item添加添加一个播放icon,并且一张图片以及是视频将长宽定义200*200
                if(isVedio){
                    FrameLayout frameLayout = new FrameLayout(context);
                    ImageView imgPlay = new ImageView(context);
                    imgPlay.setImageResource(R.drawable.icon_share_list_video_play);

                    frameLayout.addView(imageView);
                    frameLayout.addView(imgPlay);
                    tableRow.addView(frameLayout);

                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) imageView.getLayoutParams();
                    params.height = DensityUtil.dip2px(context, 200);
                    params.width = DensityUtil.dip2px(context, 200);
                    params.setMargins(0, 0, DensityUtil.dip2px(context, 3), 0);
                    imageView.setLayoutParams(params);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);

                    FrameLayout.LayoutParams palyparams = (FrameLayout.LayoutParams) imgPlay.getLayoutParams();
                    palyparams.gravity = Gravity.CENTER;
                    palyparams.height = DensityUtil.dip2px(context, 50);
                    palyparams.width = DensityUtil.dip2px(context, 50);
                    palyparams.setMargins(0, 0, DensityUtil.dip2px(context, 3), 0);
                    imgPlay.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    imgPlay.setLayoutParams(palyparams);
                }else {
                    tableRow.addView(imageView);

                    TableRow.LayoutParams params = (TableRow.LayoutParams) imageView.getLayoutParams();
//                    if(photosize == 1){
//                        params.height = DensityUtil.dip2px(context, 80);
//                        params.width = DensityUtil.dip2px(context, 80);
//                    }else {
//                        params.height = DensityUtil.dip2px(context, 80);
//                        params.width = DensityUtil.dip2px(context, 80);
//                    }
                    params.height = DensityUtil.dip2px(context, 100);
                    params.width = DensityUtil.dip2px(context, 100);
                    params.setMargins(0, 0, DensityUtil.dip2px(context, 3), 0);
                    imageView.setLayoutParams(params);
                    imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
                }

                final String imageThumbnailPath = thumbnailPathList[photoIdx];
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        WFile f = files.get(photoIdx);
                        String ext = f.getExt();
                        if (ext != null)
                            ext = ext.toLowerCase();
                        if(ext != null && ext.matches("jpg|jpeg|bmp|png")) {
                            // image
                            ImageViewActivity.launch(context, photoIdx, files, ImageViewActivity.UPDATE_WITH_MOMENT_MEDIA);
                        } else if(ext != null && ext.matches("avi|wmv|mp4|asf|mpg|mp2|mpeg|mpe|mpv|m2v|m4v|3gp")) {
                            // video
                            viewVideo(f, context);
                        }
                    }
                });
                
                if (new File(imageThumbnailPath).exists()) {
                    mImageResizer.loadImage(imageThumbnailPath, imageView);
//                    imageView.setImageDrawable(new BitmapDrawable(BitmapFactory.decodeFile(imageThumbnailPath)));
                } else {
                    imageView.setImageResource(R.drawable.feed_default_pic);
                    addGetFileToContextList(context, files.get(i * columnNum + j).thumb_fileid,
                            files.get(i * columnNum + j).getExt(), imageView, mImageResizer);
                }
            }
            photoNum -= columnNum;
            table.addView(tableRow, new TableLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            TableLayout.LayoutParams params = (TableLayout.LayoutParams) tableRow.getLayoutParams();
            params.setMargins(0, DensityUtil.dip2px(context, 3), 0, 0);
            tableRow.setLayoutParams(params);
        }
    }
    
    
    
    private static void viewVideo(final WFile file, final Context context) {
        if (!new File(file.localPath).exists()) {
            // need download
            AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Integer>() {

                private WowTalkWebServerIF web = WowTalkWebServerIF.getInstance(context);
                private boolean status;
                MessageBox msgbox = new MessageBox(context);

                @Override
                protected void onPreExecute() {
                    msgbox.showWait();
                }

                @Override
                protected Integer doInBackground(Void... voids) {
                    web.fGetFileFromServer(file.fileid, GlobalSetting.S3_MOMENT_FILE_DIR,
                            new NetworkIFDelegate() {
                                @Override
                                public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                                    status = true;
                                }

                                @Override
                                public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                                    status = false;
                                }

                                @Override
                                public void setProgress(int i, int i2) {
                                }
                            }, 0, file.localPath, null);
                    return null;
                }

                @Override
                protected void onPostExecute(Integer arg) {
                    msgbox.dismissWait();
                    if (status)
                        viewVideoDirectly(file, context);
                    else
                        msgbox.toast(R.string.download_failed);
                }
            });
        } else {
            viewVideoDirectly(file, context);
        }
    }
    
    private static void viewVideoDirectly(WFile f, Context context) {
        Uri uri = Uri.fromFile(new File(f.localPath));
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(uri, "video/*");
        context.startActivity(intent);
    }
    
    private final static int MAX_FILE_GETTING_FROM_SERVER_COUNT=5;
    private static int curLoadingFileFromServerCount;
    private static class GetFileFromServerContext {
        Context context;
        String fileId;
        String fileType;
        ImageView imageView;
        ImageResizer mImageResizer;

        public GetFileFromServerContext(Context context, String fileId,
                                        String fileType, ImageView imageView,
                                        ImageResizer mImageResizer) {
            this.context=context;
            this.fileId=fileId;
            this.fileType=fileType;
            this.imageView=imageView;
            this.mImageResizer=mImageResizer;
        }
    }
    private static ArrayList<GetFileFromServerContext> getFileFromServerContextArrayList=new ArrayList<GetFileFromServerContext>();
    private static ArrayList<GetFileFromServerContext> loadingFromServerContextArrayList=new ArrayList<GetFileFromServerContext>();
    private static void addGetFileToContextList(Context context, String fileId,
                                                String fileType, ImageView imageView,
                                                ImageResizer mImageResizer) {
        for(GetFileFromServerContext aContext : getFileFromServerContextArrayList) {
            if(aContext.fileId.equals(fileId) && aContext.fileType.equals(fileType)) {
                Log.w("duplicate file require download,omit it");
                return;
            }
        }
        for(GetFileFromServerContext aContext : loadingFromServerContextArrayList) {
            if(aContext.fileId.equals(fileId) && aContext.fileType.equals(fileType)) {
                Log.w("duplicate file require download loading,omit it");
                return;
            }
        }

        getFileFromServerContextArrayList.add(new GetFileFromServerContext(context,fileId,fileType,imageView,mImageResizer));
        triggerLoadingFileFromServer();
    }
    
    
    private static void triggerLoadingFileFromServer() {
        if(curLoadingFileFromServerCount <= MAX_FILE_GETTING_FROM_SERVER_COUNT && getFileFromServerContextArrayList.size()>0) {
            ++curLoadingFileFromServerCount;

            GetFileFromServerContext aContext=getFileFromServerContextArrayList.remove(0);
            getMomentMultimedia(aContext);
        }
    }
    private static void getMomentMultimedia(final GetFileFromServerContext aContext) {
        loadingFromServerContextArrayList.add(aContext);
        AsyncTaskExecutor.executeShortNetworkTask(new AsyncTask<Void, Void, Void>() {
            boolean ok;
            final String path = PhotoDisplayHelper.makeLocalFilePath(aContext.fileId, aContext.fileType);
            @Override
            protected Void doInBackground(Void... params) {
                WowTalkWebServerIF.getInstance(aContext.context).fGetFileFromServer(aContext.fileId, GlobalSetting.S3_MOMENT_FILE_DIR,
                        new NetworkIFDelegate() {
                    @Override
                    public void didFinishNetworkIFCommunication(int i, byte[] bytes) {
                        ok = true;
                    }

                    @Override
                    public void didFailNetworkIFCommunication(int i, byte[] bytes) {
                        ok = false;
                    }

                    @Override
                    public void setProgress(int i, int i2) {
                    }
                }, 0, path, null);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if (ok) {
                    aContext.imageView.setTag(path);
//                    imageView.setImageDrawable(new BitmapDrawable(context.getResources(), remoteDbId));
                    aContext.mImageResizer.loadImage(path, aContext.imageView);
//                    imageView.setImageDrawable(new BitmapDrawable(BitmapFactory.decodeFile(path)));
                }

                loadingFromServerContextArrayList.remove(aContext);
                --curLoadingFileFromServerCount;
                triggerLoadingFileFromServer();
            }
        });
    }
    
    
    
    
}

class ViewHolder{
    //作业
    LinearLayout layout_homework_comment;
    TextView date_homework_comment;//作业日期
    TableLayout imageTable;//图片墙
    TextView textView_homework_title;//作业标题

    //评论
    LinearLayout layout_homework_review;
    TextView date_homework_review;//评论日期
    RatingBar ratingBar_confirm;//完整性
    RatingBar ratingBar_timely;//及时性
    RatingBar ratingBar_exact;//准确性
    TextView textView_homework_review;//老师评语

}

