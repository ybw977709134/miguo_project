package co.onemeter.oneapp.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.method.LinkMovementMethod;
import android.text.style.URLSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.ui.*;
import co.onemeter.oneapp.utils.LocationHelper;

import org.wowtalk.api.*;
import org.wowtalk.ui.ImageViewActivity;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.PhotoDisplayHelper;
import org.wowtalk.ui.bitmapfun.ui.RecyclingImageView;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;
import org.wowtalk.ui.msg.TimerTextView;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Map.Entry;

public class MomentAdapter extends ArrayAdapter<Moment> {
    public interface MomentActionHandler {
        /**
         *
         * @param position Moment 在列表中的位置
         * @param moment
         * @param replyTo
         * @param like 这是一个“赞”
         */
        public void replyToMoment(int position, Moment moment, Review replyTo, boolean like);
        public void onMomentClicked(int position, Moment moment);
    }

    public interface LoadDelegate {
        /**
         *
         * @return true if a loading for more is started.
         */
        public boolean onLoadMore();
    }

    public interface OnMediaPlayFinishListener {
        public void onMediaPlayFinished();
    }
    
    private static Database dbHelper;

    private static final String TAG_IS_PLAYING = "isplaying";
    private static final String TAG_NOT_PLAYING = "notplaying";

    private static final int VIEW_TYPE_DEFAULT = 0;
    private static final int VIEW_TYPE_LOADMORE = 1;

    private String targetUid;
    private boolean isSingle = false;
	private Context context;
    private static Activity activity;
//    private MediaPlayer mPlayer;

    private ImageResizer mImageResizer;
    private MomentActionHandler mReplyDelegate;
    public LoadDelegate mLoadDelegate;
    private boolean showLoadMoreAsLastItem = false;
    private boolean mIsLoading = false;
    private View mLoadMoreView = null;

    private boolean isNoMomentDescShow=false;
    private View noMomentDescView=null;
    private ProgressBar noMomentLoadingProgress;

    private MessageBox mMsgBox;
    private HashMap<String,ArrayList<String>> surveyMomentChoosedItemList=new HashMap<String,ArrayList<String>>();

    private MediaPlayerWraper mediaPlayerWraper;

    private View myMomentEntryView=null;

    private int newReviewCount=0;
    private View newReviewView=null;

    private boolean isWithFavorite;
    public int countType = -1;//-1全部，0公众账号，1学生账号，2老师账号
    
    //默认的全部adapter
	public MomentAdapter(Context context, Activity activity, ArrayList<Moment> moments, boolean isSingle,boolean favorite,
                         ImageResizer mImageResizer,
                         MomentActionHandler replyDelegate,String uid,MessageBox box) {
        super(context, 0, 0, moments);
		this.context = context;
        this.activity = activity;
        targetUid=uid;
        this.isSingle = isSingle;
        mReplyDelegate = replyDelegate;
        this.mImageResizer = mImageResizer;
        mMsgBox=box;
        isWithFavorite=favorite;

        mediaPlayerWraper=new MediaPlayerWraper(activity, true);
	}
	
	
	//0公众账号，1学生账号，2老师账号,账号对应的adapter
	public MomentAdapter(Context context, Activity activity, ArrayList<Moment> moments, boolean isSingle,boolean favorite,
            ImageResizer mImageResizer,
            MomentActionHandler replyDelegate,String uid,MessageBox box,int countType) {
		super(context, 0, 0, moments);
		this.context = context;
		this.activity = activity;
		targetUid=uid;
		this.isSingle = isSingle;
		mReplyDelegate = replyDelegate;
		this.mImageResizer = mImageResizer;
		mMsgBox=box;
		isWithFavorite=favorite;
		this.countType = countType;
		mediaPlayerWraper=new MediaPlayerWraper(activity, true);
}
	

    public void setNewReviewCount(int count) {
        newReviewCount=count;
    }

    private boolean isNewReviewViewAvaliable() {
        return 0 != newReviewCount && !isWithFavorite;
    }

    private boolean isMyMomentEntryAvaliable() {
        return false && !isSingle && !isWithFavorite;
    }

    @Override
    public int getCount() {
        int count=showLoadMoreAsLastItem ? super.getCount() + 1 : super.getCount();
        if(isNewReviewViewAvaliable()) {
            ++count;
            isNoMomentDescShow=false;
        }
        if(isMyMomentEntryAvaliable()) {
            ++count;
            isNoMomentDescShow=false;
        } else {
            if(0 == count || (1 == count && showLoadMoreAsLastItem)) {
                count=1;
                isNoMomentDescShow=true;
            } else {
                isNoMomentDescShow=false;
            }
        }
        return count;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        if (showLoadMoreAsLastItem && position >= getCount() - 1)
            return VIEW_TYPE_LOADMORE;
        return VIEW_TYPE_DEFAULT;
    }
    
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
        if(isNewReviewViewAvaliable()) {
            if(0 == position) {
                //new reivew layout
                return getNewReviewView();
            }

            if(isMyMomentEntryAvaliable() && 1 == position) {
                return getMyMomentEntryView();
            }
        } else {
            if(isMyMomentEntryAvaliable() && 0 == position) {
                return getMyMomentEntryView();
            }
        }

        if (isNoMomentDescShow) {
            if(0 != position) {
                Log.e("no moment but position "+position);
            }
            return getNoMomentDescView();
        }
        if (showLoadMoreAsLastItem && position >= getCount() - 1) {
            return getLoadMoreView();
        }

		final ViewHolder holder;
        int momentPos=position;
        if(isNewReviewViewAvaliable()) {
            --momentPos;
            if(isMyMomentEntryAvaliable()) {
                --momentPos;
            }
        } else {
            if(isMyMomentEntryAvaliable()) {
                --momentPos;
            }
        }

		final Moment moment = getItem(momentPos);
		ArrayList<Review> reviews = moment.reviews;
		final ArrayList<Review> likeReview = new ArrayList<Review>();
		ArrayList<Review> commentReview = new ArrayList<Review>();
        ArrayList<WFile> photoFiles = new ArrayList<WFile>();
        WFile voiceFile = null;
        for (WFile file : moment.multimedias) {
            if (file.getExt().equals("aac") || file.getExt().equals("m4a") || file.getExt().equals("3gpp")) {
                voiceFile = file;
            } else {
                photoFiles.add(file);
            }
        }
		for (int i = 0; i < reviews.size(); i++) {
			if (reviews.get(i).type == Review.TYPE_LIKE) {
				likeReview.add(reviews.get(i));
			} else if (reviews.get(i).type == Review.TYPE_TEXT){
				commentReview.add(reviews.get(i));
			}
		}
//		convertView.setTag(null);
		if (null == convertView || null == convertView.getTag()) {
            convertView = LayoutInflater.from(context).inflate(R.layout.listitem_trend_friend, null);
			holder = new ViewHolder();
			
//			holder.layout_friend_item = (LinearLayout) convertView.findViewById(R.id.layout_friend_item);
			
			holder.imgThumbnail = (ImageView) convertView.findViewById(R.id.img_thumbnail);
            holder.txtDate = (TextView) convertView.findViewById(R.id.txt_date);
			holder.txtName = (TextView) convertView.findViewById(R.id.txt_name);
			holder.txtTime = (TextView) convertView.findViewById(R.id.txt_time);
			holder.txtContent = (TextView) convertView.findViewById(R.id.txt_content);
            holder.imageTable = (TableLayout) convertView.findViewById(R.id.imageTable);
            holder.reviewLayout = (LinearLayout) convertView.findViewById(R.id.reviewLayout);
			holder.txtLikeNames = (SpannedTextView) convertView.findViewById(R.id.txt_like_names);
			holder.mReview = (LinearLayout) convertView.findViewById(R.id.layout_review);
            holder.reviewDivider = (ImageView) convertView.findViewById(R.id.reviewDivider);
            holder.txtLoc = (TextView) convertView.findViewById(R.id.txt_loc);

            holder.tvLikeCountInd=(TextView) convertView.findViewById(R.id.moment_like_count_ind);
            holder.tvCommentCountInd=(TextView) convertView.findViewById(R.id.moment_comment_count_ind);
            holder.tvAnswerCountInd=(TextView) convertView.findViewById(R.id.moment_answer_count_ind);

            holder.btnLike = (Button) convertView.findViewById(R.id.btn_like);
			holder.btnComment = (Button) convertView.findViewById(R.id.btn_comment);
            holder.btnAnswer = (Button) convertView.findViewById(R.id.btn_answer);
            holder.tvLike = (TextView) convertView.findViewById(R.id.tv_moment_op_like);
            holder.tvComment = (TextView) convertView.findViewById(R.id.tv_moment_op_comment);
            holder.tvAnswer = (TextView) convertView.findViewById(R.id.tv_moment_op_answer);
            holder.layoutLike = (LinearLayout) convertView.findViewById(R.id.layout_like);
            holder.layoutComment = (LinearLayout) convertView.findViewById(R.id.layout_comment);
            holder.layoutAnswer = (LinearLayout) convertView.findViewById(R.id.layout_answer);
            holder.rlMomentFavoriteLayout=(RelativeLayout) convertView.findViewById(R.id.moment_favorite_layout);
            holder.ivMomentFavorite=(ImageView) convertView.findViewById(R.id.moment_favorite);

//            holder.reviewButtons = (LinearLayout) convertView.findViewById(R.id.review_btn);
//            holder.ivMomentOpEllipse=(ImageView) convertView.findViewById(R.id.iv_moment_op_ellipse);
			holder.layoutReview = (LinearLayout) convertView.findViewById(R.id.layout_review);
            holder.micLayout = (LinearLayout) convertView.findViewById(R.id.mic_layout);
            holder.progress = (ProgressBar) convertView.findViewById(R.id.progress);
            holder.micButton = (ImageView) convertView.findViewById(R.id.btn_play);
            holder.micTime = (TimerTextView) convertView.findViewById(R.id.mic_timer);
//            holder.imgLocation = (ImageView) convertView.findViewById(R.id.img_location);
//            holder.layoutLike = (LinearLayout) convertView.findViewById(R.id.layout_like);
//            holder.layoutComment = (LinearLayout) convertView.findViewById(R.id.layout_comment);
            holder.tvMomentTagDesc=(TextView) convertView.findViewById(R.id.moment_item_tag);
            holder.ivMomentTagColor=(ImageView) convertView.findViewById(R.id.iv_moment_item_tag_color_line);
            holder.tvMomentShareRange=(TextView) convertView.findViewById(R.id.tv_share_rang_ind);
            holder.ivMomentShareRange=(ImageView) convertView.findViewById(R.id.iv_share_rang_ind);

            holder.voteSurveyLayout=(LinearLayout) convertView.findViewById(R.id.vote_survey_layout);
            holder.lvSurveyOptions=(LinearLayoutAsListView) convertView.findViewById(R.id.vote_survey_options);
            holder.btnSurvey=(Button) convertView.findViewById(R.id.vote_survey_button);

			convertView.setTag(holder);
		} else {
			holder = (ViewHolder) convertView.getTag();
		}
		
//		holder.btnSurvey.setOnClickListener(new View.OnClickListener() {
//			
//			@Override
//			public void onClick(View arg0) {
//				
//		        surveyMomentChoosedItemList.put(moment.id,new ArrayList<String>());
//			
//		        doVoteSurvey(context, moment, surveyMomentChoosedItemList.get(moment.id), holder.voteSurveyLayout, holder.lvSurveyOptions, surveyMomentChoosedItemList.get(moment.id), holder.btnSurvey);
//		        
//		        setTagdesc(context,moment,holder.ivMomentTagColor,holder.tvMomentTagDesc,
//		                holder.voteSurveyLayout,holder.lvSurveyOptions,surveyMomentChoosedItemList.get(moment.id),holder.btnSurvey);
//				
//			}
//		});
        
		final WFile file = voiceFile;
        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            	if (file != null) {//主要是为了在播放音频的情况下进入详情页，停止播放音频
            		mediaPlayerWraper.stop();
            		holder.micButton.setImageResource(R.drawable.timeline_player_play);
                    holder.micTime.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, file.duration / 60, file.duration % 60));
                    
            	}
                if (mReplyDelegate != null) {
                    mReplyDelegate.onMomentClicked(position, moment);
                }
            }
        });

        setTagDescInfo(holder,moment);
        setupOpButtons(holder,moment,momentPos,likeReview,commentReview);
        

        setTimeForAll(holder.txtTime, Long.valueOf(moment.timestamp * 1000));
        if (moment.owner != null && !Utils.isNullOrEmpty(moment.owner.userID)) {
            PhotoDisplayHelper.displayPhoto(context, holder.imgThumbnail,
                    R.drawable.default_avatar_90, moment.owner, true);
            holder.imgThumbnail.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                	String mMyUid = PrefUtil.getInstance(context).getUid();
                    if(null != moment.owner && !TextUtils.isEmpty(moment.owner.userID) && moment.owner.userID.equals(mMyUid)) {
                    	TimelineActivity.launch(context, moment.owner.userID, moment.owner.nickName,"owner");//跳转到自己成长日志//可进行发布操作
                    } else {
                    	TimelineActivity.launch(context, moment.owner.userID, moment.owner.nickName);//跳转到好友的成长日志//不可进行发布操作
                    }
   //                 TimelineActivity.launch(context, moment.owner.userID, moment.owner.nickName);
                }
            });
        }
        if (moment.owner != null) {
            holder.txtName.setText(TextUtils.isEmpty(moment.owner.alias) ? moment.owner.nickName : moment.owner.alias);
        } else {
            holder.txtName.setText("");
        }

        if (moment.text == null || moment.text.equals("")) {
            holder.txtContent.setVisibility(View.GONE);
        } else {
            holder.txtContent.setVisibility(View.VISIBLE);
            setStringAsURLIfAvaliable(holder.txtContent, moment.text,true);
        }

        //always has no review layout int adapter,show in detail
        if (reviews == null || reviews.size() == 0) {
            holder.reviewLayout.setVisibility(View.GONE);
        } else {
            holder.reviewLayout.setVisibility(View.GONE);
//            holder.reviewLayout.setVisibility(View.VISIBLE);
//            holder.reviewDivider.setVisibility(View.VISIBLE);
        }
//		if (likeReview.size() == 0) {
//			holder.txtLikeNames.setVisibility(View.GONE);
//            holder.reviewDivider.setVisibility(View.GONE);
//		} else {
//			holder.txtLikeNames.setVisibility(View.VISIBLE);
//            setViewForLikeReview(context, holder.txtLikeNames, likeReview);
//		}
//		if (commentReview.size() == 0) {
//            holder.reviewDivider.setVisibility(View.GONE);
//			holder.mReview.setVisibility(View.GONE);
//		} else {
//			holder.mReview.removeAllViews();
//			holder.mReview.setVisibility(View.VISIBLE);
//            setViewForCommentReview(context, holder.mReview, commentReview, position, moment.id, mReplyDelegate);
//		}

        //TODO
//        holder.ivMomentOpEllipse.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                showMomentOpPopupWindow(view,moment,position);
//            }
//        });
        // like and comment buttons
        //like and comment is required even if this moment is mine
//        if (isMyMoment(moment.owner.userID) || !moment.allowReview) {
//        if (!moment.allowReview) {
//            holder.reviewButtons.setVisibility(View.GONE);
//        } else {
//            holder.reviewButtons.setVisibility(View.VISIBLE);
//            holder.btnReview.setEnabled(true);
//
//            if (moment.likedByMe) {
//                holder.btnLike.setEnabled(false);
//                holder.layoutLike.setEnabled(false);
//                holder.btnLike.setBackgroundResource(R.drawable.icon_btn_liked);
//            } else {
//                holder.btnLike.setEnabled(true);
//                holder.layoutLike.setEnabled(true);
//                holder.btnLike.setBackgroundResource(R.drawable.icon_btn_like);
//            }
//
//            holder.btnLike.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    holder.layoutLike.performClick();
//                }
//            });
//            holder.layoutLike.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                   MomentActivity a = MomentActivity.instance();
//                    if (a != null)
//                        a.doLikeMoment_async(moment);
//                }
//            });
//            holder.btnReview.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    holder.layoutComment.performClick();
//                }
//            });
//            holder.layoutComment.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View v) {
//                    if (mReplyDelegate != null) {
//                        mReplyDelegate.replyToMoment(position, moment.id, null);
//                    }
//                }
//            });
//        }


        setImageLayout(context, mImageResizer, photoFiles, holder.imageTable);
        showVoiceFile(voiceFile, holder.micLayout, holder.progress, holder.micButton, holder.micTime);
        if (moment.latitude == 0 && moment.longitude == 0) {
//            holder.imgLocation.setVisibility(View.GONE);
            holder.txtLoc.setVisibility(View.GONE);
        } else {
//            holder.imgLocation.setVisibility(View.GONE);
            holder.txtLoc.setVisibility(View.VISIBLE);
            LocationHelper.getAddressFromLatitudeAndLongitude((int)moment.latitude, (int)moment.longitude, holder.txtLoc);
            holder.txtLoc.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    MessageDetailAdapter.viewLocationInfo(context,moment.latitude,moment.longitude,moment.place);
                }
            });
        }
        
        //通过对convertView的筛选,得到对应的账号结果
//        if (countType == -1) {
//        	return convertView;	
//        } else {
//        	String countUid = PrefUtil.getInstance(context).getUid();
//            if (dbHelper.getBuddyCountType(countUid) == countType) {
//            	return convertView;	
//            } else {
//  //          	convertView.setVisibility(View.GONE);
//  //          	return null;	
// //           	holder.layout_friend_item.setVisibility(View.GONE);
//            	return null;
//            }
//        }

		return convertView;	 
	}
	


	////////////////////////////////////////////////////////////////
	
    public static void setStringAsURLIfAvaliable(TextView tv2set,String str,boolean withClick) {
        CharSequence result=str;
        boolean isValidURLExist=false;

        try {
            if(!TextUtils.isEmpty(str)) {
                String[] strSplit=str.split(" ");
                SpannableString sp = new SpannableString(str);

                for(int i=0; i<strSplit.length; ++i) {
                    String aStr=strSplit[i];
                    if(isStringAValidURL(aStr)) {
                        isValidURLExist=true;

                        int startIndex=str.indexOf(aStr);
                        int endIndex=startIndex+aStr.length();
//                        String urlSpan=aStr.startsWith("http://")?aStr:"http://"+aStr;
                        sp.setSpan(new URLSpan(aStr), startIndex, endIndex,
                                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                    }
                }

                result = sp;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        tv2set.setText(result);
        if(isValidURLExist && withClick) {
            tv2set.setMovementMethod(LinkMovementMethod.getInstance());
        }
    }

    private final static String[] urlMatchCases=new String[] {
            "\\b(https?|ftp|file)://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
    };
    public static boolean isStringAValidURL(String str) {
        boolean ret=false;

        if(!TextUtils.isEmpty(str)) {
            String strLower=str.toLowerCase();
            for(String aCase : urlMatchCases) {
                if(strLower.matches(aCase)) {
                    ret=true;
                    break;
                }
            }
        }

        return ret;
    }

    public static void setTagdesc(final Context context,final Moment moment,ImageView iv,TextView tv,
                                  LinearLayout voteSurveyLayout,final LinearLayoutAsListView lvSurveyOptions,final ArrayList<String> choosed,final Button btnSurvey) {
        if(TextUtils.isEmpty(moment.tag) || moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_LIFE)) {
            iv.setBackgroundColor(context.getResources().getColor(R.color.moment_tag_life));
            tv.setText(R.string.moment_tag_life);
        } else if (moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_QA)) {
            iv.setBackgroundColor(context.getResources().getColor(R.color.moment_tag_qa));
            tv.setText(R.string.moment_tag_qa);
        } else if (moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_NOTICE)) {
            iv.setBackgroundColor(context.getResources().getColor(R.color.moment_tag_notice));
            tv.setText(R.string.moment_tag_notice);
        } else if (moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE) || moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI)) {
            iv.setBackgroundColor(context.getResources().getColor(R.color.moment_tag_survey));
            tv.setText(R.string.moment_tag_survey);
        } else if (moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_STUDY)) {
            iv.setBackgroundColor(context.getResources().getColor(R.color.moment_tag_study));
            tv.setText(R.string.moment_tag_study);
        } else if (moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_VIDEO)) {
            iv.setBackgroundColor(context.getResources().getColor(R.color.moment_tag_video));
            tv.setText(R.string.moment_tag_video);
        }

        setSurveyInfo(context,moment,voteSurveyLayout,lvSurveyOptions,choosed,btnSurvey);
    }

    private static void setSurveyInfo(final Context context,final Moment moment,final LinearLayout voteSurveyLayout,final LinearLayoutAsListView lvSurveyOptions,final ArrayList<String> choosed,final Button btnSurvey) {
        if(Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(moment.tag) || Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI.equals(moment.tag)) {   
        	
        	moment.showVoteInfo();
        	
            voteSurveyLayout.setVisibility(View.VISIBLE);

            TextView tvSurveyDeadLineIndicate=(TextView) voteSurveyLayout.findViewById(R.id.survey_dead_line_indicate);
            boolean surveyOutOfDate=false;

            //survey deadline indicate
            if(TextUtils.isEmpty(moment.surveyDeadLine)) {
                tvSurveyDeadLineIndicate.setVisibility(View.GONE);
            } else {
                tvSurveyDeadLineIndicate.setVisibility(View.VISIBLE);

                long deadlinetime=Utils.tryParseLong(moment.surveyDeadLine, -1);

                Calendar deadlineCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                deadlineCalendar.setTimeInMillis(deadlinetime*1000);
                Date dateDeadLine=deadlineCalendar.getTime();

                if(deadlinetime == Long.parseLong(CreateNormalMomentWithTagActivity.SURVEY_DEADLINE_NO_LIMIT_VALUE)) {
                    surveyOutOfDate=false;
                } else {
                    // 此处需要校准时间
                    Calendar curCalendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
                    long utcOffsetInMillis = PrefUtil.getInstance(context).getUTCOffset() * 1000;
                    if(curCalendar.getTimeInMillis() + utcOffsetInMillis > deadlineCalendar.getTimeInMillis()) {
                        surveyOutOfDate=true;
                    } else {
                        surveyOutOfDate=false;
                    }
                }
                if(surveyOutOfDate) {
                    tvSurveyDeadLineIndicate.setText(context.getString(R.string.vote_out_of_date));
                } else {
                    if(moment.isMeVoted()) {
                        tvSurveyDeadLineIndicate.setText(context.getString(R.string.vote_succeed));
                    } else {
                        String formattedTime="";

                        if(moment.surveyDeadLine.equals(CreateNormalMomentWithTagActivity.SURVEY_DEADLINE_NO_LIMIT_VALUE)) {
                            formattedTime=context.getString(R.string.survey_dead_line_no_limit);
                        } else {
                            SimpleDateFormat dateFormat= new SimpleDateFormat(context.getString(R.string.msg_date_format_with_year));

                            if(null != dateDeadLine) {
                                formattedTime=dateFormat.format(dateDeadLine);
                            }
                        }

                        if(moment.isSurveyAllowMultiSelect) {
                            tvSurveyDeadLineIndicate.setText(context.getString(R.string.survey_can_multi_select)+","+
                                    context.getString(R.string.survey_vote_dead_line)+": "+formattedTime);
                        } else {
                            tvSurveyDeadLineIndicate.setText(context.getString(R.string.vote_single_select)+","+
                                    context.getString(R.string.survey_vote_dead_line)+": "+formattedTime);
                        }
                    }
                }
            }

            //survey options
            if(moment.isMeVoted() || surveyOutOfDate) {
                //voted,show vote factor
                btnSurvey.setVisibility(View.GONE);
                lvSurveyOptions.setListAdapter(new SurveyVotedDisplayAdapter(context,moment.surveyOptions));
                
                //投票刷新成功，取消观察者的监听
        //        Database.removeDBTableChangeListener(momentObserver);             
                
//                ListHeightUtil.setListHeight(lvSurveyOptions);
            } else {
                //not vote yet,show to vote
                btnSurvey.setVisibility(View.VISIBLE);
                SurveyChooseToVoteAdapter adapter=new SurveyChooseToVoteAdapter(context,moment.surveyOptions,choosed,moment.isSurveyAllowMultiSelect);
                adapter.setOnChooseChangeListener(new SurveyChooseToVoteAdapter.OnChooseChangeListener() {
                    @Override
                    public void onChooseChange(int count) {
                        if(0 == count) {
                            btnSurvey.setBackgroundResource(R.drawable.btn_gray_selector);
                        } else {
                            btnSurvey.setBackgroundResource(R.drawable.btn_blue_selector);
                        }
                    }
                });
                lvSurveyOptions.setListAdapter(adapter);
//                ListHeightUtil.setListHeight(lvSurveyOptions);

                btnSurvey.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        if(choosed.size() == 0) {
                            return;
                        }
                        doVoteSurvey(context,moment,choosed,voteSurveyLayout,lvSurveyOptions,choosed,btnSurvey);
                    }
                });
                

             
                
                
                if(0 == choosed.size()) {
                    btnSurvey.setBackgroundResource(R.drawable.btn_gray_selector);
                } else {
                    btnSurvey.setBackgroundResource(R.drawable.btn_blue_selector);
                }
            }
        } else {
            voteSurveyLayout.setVisibility(View.GONE);
        }
        
    }

    
    private static void doVoteSurvey(final Context context,final Moment moment,final ArrayList<String> selectedOptionList,
                                     final LinearLayout voteSurveyLayout,final LinearLayoutAsListView lvSurveyOptions,final ArrayList<String> choosed,final Button btnSurvey) {
        final MessageBox msgBox=new MessageBox(context);
        msgBox.showWait();
        new AsyncTask<Void, Void, Integer>() {
            @Override
            protected Integer doInBackground(Void... params) {
                int resultCode = WowMomentWebServerIF.getInstance(context)
                        .voteMomentSurvey(moment,selectedOptionList);
                // 投票过期，但界面显示未过期，则是本地时间有误差导致，需要重新获取服务器时间offset
                if (ErrorCode.MOMENT_SURVEY_OUTOFDATE == resultCode) {
                    WowTalkWebServerIF.getInstance(context).fAdjustUTCTimeWithServer();
                }
                return resultCode;
            }

            @Override
            protected void onPostExecute(Integer errno) {
                msgBox.dismissWait();
                switch (errno) {
                case ErrorCode.OK:
                	btnSurvey.setVisibility(View.GONE);

                    // 保存我的投票结果到 Moment 对象
                    Moment updatedMoment = new Database(context).fetchMoment(moment.id);
                    if (updatedMoment != null) {
                        moment.surveyOptions = updatedMoment.surveyOptions;
                    }

                	setSurveyInfo(context,moment,voteSurveyLayout,lvSurveyOptions,choosed,btnSurvey);              
                    break;
                case ErrorCode.MOMENT_SURVEY_OUTOFDATE:
                    choosed.clear();
                    setSurveyInfo(context,moment,voteSurveyLayout,lvSurveyOptions,choosed,btnSurvey);
                    break;
                case ErrorCode.MOMENT_SURVEY_HAS_VOTED:
                    msgBox.toast(R.string.moment_survey_has_voted);
                    choosed.clear();
                    setSurveyInfo(context,moment,voteSurveyLayout,lvSurveyOptions,choosed,btnSurvey);
                    break;
                default:
                    msgBox.toast(R.string.operation_failed);
                    break;
                }
            }
        }.execute((Void)null);
    }

    private void setTagDescInfo(final ViewHolder holder,final Moment moment) {
        if((Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(moment.tag) || Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI.equals(moment.tag)) &&
                null == surveyMomentChoosedItemList.get(moment.id)) {
            surveyMomentChoosedItemList.put(moment.id,new ArrayList<String>());
        }
        setTagdesc(context,moment,holder.ivMomentTagColor,holder.tvMomentTagDesc,
                holder.voteSurveyLayout,holder.lvSurveyOptions,surveyMomentChoosedItemList.get(moment.id),holder.btnSurvey);

        if(TextUtils.isEmpty(moment.shareRange) || moment.shareRange.equals(Moment.SERVER_SHARE_RANGE_PUBLIC)) {
//            holder.tvMomentShareRange.setText(R.string.share_range_public_short);
            holder.ivMomentShareRange.setImageResource(R.drawable.timeline_public);
        } else {
            holder.tvMomentShareRange.setText(R.string.share_range_private);
            holder.ivMomentShareRange.setImageResource(R.drawable.timeline_limited);
        }

        View.OnClickListener clickListener=new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(context,ShareRangeSelectActivity.class);
                intent.putStringArrayListExtra(ShareRangeSelectActivity.LITMITED_DEPS,moment.limitedDepartmentList);
                intent.putExtra(ShareRangeSelectActivity.SHOWN_ONLY,true);
                context.startActivity(intent);
            }
        };
        holder.tvMomentShareRange.setOnClickListener(clickListener);
        holder.ivMomentShareRange.setOnClickListener(clickListener);

        String mMyUid = PrefUtil.getInstance(context).getUid();
        if(null != moment.owner && !TextUtils.isEmpty(moment.owner.userID) && !moment.owner.userID.equals(mMyUid)) {
            holder.tvMomentShareRange.setVisibility(View.GONE);
            holder.ivMomentShareRange.setVisibility(View.GONE);
        } else {
            holder.tvMomentShareRange.setVisibility(View.VISIBLE);
            holder.ivMomentShareRange.setVisibility(View.VISIBLE);
        }
    }

    public static void triggerMomentFavorite(Context context,Moment moment,ImageView ivMomentFavorite) {
        Animation animation;
        if(moment.isFavorite) {
            //unfavorite anim
            animation = AnimationUtils.loadAnimation(context, R.anim.moment_unfavorite_anim);
        } else {
            //favorite anim
            animation = AnimationUtils.loadAnimation(context, R.anim.moment_favorite_anim);
        }
        ivMomentFavorite.startAnimation(animation);

        moment.isFavorite = !moment.isFavorite;

        Database dbHelper = Database.open(context);
        dbHelper.storeMoment(moment,null);

        setMomentFavoriteStatus(moment,ivMomentFavorite);
    }

    public static void setMomentFavoriteStatus(Moment moment,ImageView ivMomentFavorite) {
        if(moment.isFavorite) {
            ivMomentFavorite.setImageResource(R.drawable.timeline_favorite_a);
        } else {
            ivMomentFavorite.setImageResource(R.drawable.timeline_favorite);
        }
    }

    private void setupOpButtons(final ViewHolder holder,final Moment moment,final int position,
                                final ArrayList<Review> likeReview,ArrayList<Review> commentReview) {
        if (!TextUtils.isEmpty(moment.tag) &&
                (moment.tag.equals(Moment.SERVER_MOMENT_TAG_FOR_QA))) {
            holder.layoutLike.setVisibility(View.VISIBLE);
            holder.layoutComment.setVisibility(View.GONE);
            holder.layoutAnswer.setVisibility(View.VISIBLE);
        } else if (Moment.SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE.equals(moment.tag) || Moment.SERVER_MOMENT_TAG_FOR_SURVEY_MULTI.equals(moment.tag)) {
            holder.layoutLike.setVisibility(View.VISIBLE);
            holder.layoutComment.setVisibility(View.VISIBLE);
            holder.layoutAnswer.setVisibility(View.GONE);
        } else {
            holder.layoutLike.setVisibility(View.VISIBLE);
            holder.layoutComment.setVisibility(View.VISIBLE);
            holder.layoutAnswer.setVisibility(View.GONE);
        }

        if(View.VISIBLE == holder.layoutLike.getVisibility()) {
//            holder.tvLikeCountInd.setText(""+likeReview.size()+" "+context.getString(R.string.moments_like));
//            holder.tvLikeCountInd.setVisibility(View.VISIBLE);
        } else {
            holder.tvLikeCountInd.setVisibility(View.GONE);
        }

        if(View.VISIBLE == holder.layoutComment.getVisibility()) {
//            holder.tvCommentCountInd.setText(""+commentReview.size()+" "+context.getString(R.string.moments_comment));
//            holder.tvCommentCountInd.setVisibility(View.VISIBLE);
        } else {
            holder.tvCommentCountInd.setVisibility(View.GONE);
        }

        if(View.VISIBLE == holder.layoutAnswer.getVisibility()) {
//            holder.tvAnswerCountInd.setText(""+commentReview.size()+" "+context.getString(R.string.answer_qa));
//            holder.tvAnswerCountInd.setVisibility(View.VISIBLE);
        } else {
            holder.tvAnswerCountInd.setVisibility(View.GONE);
        }

        //显示点赞的数量
        holder.tvLike.setText(""+likeReview.size());

        //显示评论的数量
//        holder.tvComment.setText(R.string.moments_comment);
        holder.tvComment.setText(""+commentReview.size());
        //显示回复的数量
//        holder.tvAnswer.setText(R.string.answer_qa);
        holder.tvAnswer.setText(""+commentReview.size());

        if (moment.likedByMe) {
//            holder.btnLike.setEnabled(false);
//            holder.layoutLike.setEnabled(false);
            holder.btnLike.setBackgroundResource(R.drawable.timeline_like_a);
        } else {
//            holder.btnLike.setEnabled(true);
//            holder.layoutLike.setEnabled(true);
            holder.btnLike.setBackgroundResource(R.drawable.timeline_like);
        }
        
//        if (moment.allowReview) {
//        	holder.btnComment.setBackgroundResource(R.drawable.share_icon_comment);
//        } else {
//        	holder.btnComment.setBackgroundResource(R.drawable.profile_btn_message_p);
//        }
        if(commentReview.size()>0){
        	holder.btnComment.setBackgroundResource(R.drawable.profile_btn_message_p);
        	holder.btnAnswer.setBackgroundResource(R.drawable.profile_btn_message_p);
        	holder.tvComment.setVisibility(View.VISIBLE);
        	holder.tvAnswer.setVisibility(View.VISIBLE);
        }else{
        	holder.btnComment.setBackgroundResource(R.drawable.share_icon_comment);
        	holder.btnAnswer.setBackgroundResource(R.drawable.share_icon_comment);
        	holder.tvComment.setVisibility(View.GONE);
        	holder.tvAnswer.setVisibility(View.GONE);
        }
        if(likeReview.size()>0){
        	holder.tvLike.setVisibility(View.VISIBLE);
        }else{
        	holder.tvLike.setVisibility(View.GONE);
        }
//        if (moment.allowReview) {
//        	holder.btnAnswer.setBackgroundResource(R.drawable.share_icon_comment);
//        } else {
//        	holder.btnAnswer.setBackgroundResource(R.drawable.profile_btn_message_p);
//        }
        
        holder.btnLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean isLike = !moment.likedByMe;
            	if (isLike) {
                  holder.btnLike.setBackgroundResource(R.drawable.timeline_like_a);
            	} else {
                  holder.btnLike.setBackgroundResource(R.drawable.timeline_like);
            	}
                holder.layoutLike.performClick();
            }
        });
        holder.layoutLike.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mReplyDelegate != null) {
                    mReplyDelegate.replyToMoment(position, moment, null, true);
                }
                
            }
        });
        holder.btnComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.layoutComment.performClick();
            }
        });
        holder.layoutComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mReplyDelegate != null) {
                    mReplyDelegate.replyToMoment(position, moment, null, false);
                }
            }
        });
        holder.btnAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                holder.layoutAnswer.performClick();
            }
        });
        holder.layoutAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                holder.layoutComment.performClick();
            }
        });

        setMomentFavoriteStatus(moment,holder.ivMomentFavorite);

        holder.rlMomentFavoriteLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                triggerMomentFavorite(context,moment,holder.ivMomentFavorite);
            }
        });
    }

    private View getNewReviewView() {
//        if(null == newReviewView) {
//            try {
//                newReviewView=LayoutInflater.from(context).inflate(R.layout.new_review_layout, null);
//            } catch (Exception e) {
//                e.printStackTrace();
//            }
//        }

        try {
            newReviewView=LayoutInflater.from(context).inflate(R.layout.new_review_layout, null);
            TextView tvDesc=(TextView) newReviewView.findViewById(R.id.txt_desc);
            tvDesc.setText(String.format(context.getString(R.string.moments_new_review_with_count),newReviewCount));

            View.OnClickListener listener=new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent newReviewsIntent = new Intent(context, NewReviewsActivity.class);
                    newReviewsIntent.putExtra(NewReviewsActivity.EXTRA_KEY_HOSTTYPE,
                            NewReviewsActivity.EXTRA_VALUE_HOSTTYPE_MOMENT);
                    ((Activity)context).startActivity(newReviewsIntent);
//                    ((Activity)context).startActivityForResult(newReviewsIntent,MomentActivity.ACTIVITY_REQ_ID_NEW_REVIEWS);
                }
            };
            newReviewView.setOnClickListener(listener);
            newReviewView.findViewById(R.id.img_thumbnail).setOnClickListener(listener);
            newReviewView.findViewById(R.id.txt_desc).setOnClickListener(listener);
            newReviewView.findViewById(R.id.layout_iv_right).setOnClickListener(listener);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return newReviewView;
    }

    private View getMyMomentEntryView() {
        if(null == myMomentEntryView) {
            try {
                myMomentEntryView=LayoutInflater.from(context).inflate(R.layout.my_moment_entry_layout, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        try {
            String myUid = PrefUtil.getInstance(context).getUid();
            Database dbHelper=new Database(context);
            Buddy buddy=dbHelper.buddyWithUserID(myUid);

            PhotoDisplayHelper.displayPhoto(context, (ImageView)myMomentEntryView.findViewById(R.id.img_thumbnail),
                    R.drawable.default_avatar_90, buddy, true);

            TextView tvName=(TextView) myMomentEntryView.findViewById(R.id.txt_name);
            tvName.setText(TextUtils.isEmpty(buddy.alias) ? buddy.nickName : buddy.alias);

            myMomentEntryView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    String myUid = PrefUtil.getInstance(context).getUid();
                    TimelineActivity.launch(context, myUid, null);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
        return myMomentEntryView;
    }

    private View getNoMomentDescView() {
        if(null == noMomentDescView) {
            if(isSingle) {
                noMomentDescView = LayoutInflater.from(context).inflate(R.layout.no_moment_desc_layout_single, null);
                ImageView ivAdd=(ImageView) noMomentDescView.findViewById(R.id.iv_create_moment);

                String myUid = PrefUtil.getInstance(context).getUid();
                if(!TextUtils.isEmpty(myUid) && !TextUtils.isEmpty(targetUid) && !myUid.equals(targetUid)) {
                    ivAdd.setVisibility(View.GONE);
                } else {
                    ivAdd.setVisibility(View.VISIBLE);
                    ivAdd.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
//                            MomentActivity.instance().showCreateOptionWithTag();
                        }
                    });
                }
            } else {
                noMomentDescView = LayoutInflater.from(context).inflate(R.layout.no_moment_desc_layout_all, null);
            }

            noMomentLoadingProgress=(ProgressBar)noMomentDescView.findViewById(R.id.progress_no_moment_loading_from_server);
        }

        return noMomentDescView;
    }

    public void setLoadingFromServer(boolean loading) {
        if(null == noMomentLoadingProgress) {
            getNoMomentDescView();
        }

        if(null != noMomentLoadingProgress) {
            Log.w("set loading progress "+loading);
            if(loading) {
                noMomentLoadingProgress.setVisibility(View.VISIBLE);
            } else {
                noMomentLoadingProgress.setVisibility(View.GONE);
            }
        }
    }

    private View getLoadMoreView() {
        if (null == mLoadMoreView) {
            mLoadMoreView = LayoutInflater.from(context).inflate(R.layout.listitem_load_more, null);
            ((TextView)mLoadMoreView.findViewById(R.id.text)).setText(
                    mIsLoading ? R.string.loading_more_in_progress : R.string.load_more);
            mLoadMoreView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (null != mLoadDelegate && !mIsLoading) {
                        // notifyLoadingCompleted() may be called before onLoadMore() returns,
                        // so we should not set mIsLoading as true after onLoadMore() return true.
                        mIsLoading = true;
                        ((TextView)mLoadMoreView.findViewById(R.id.text)).setText(R.string.loading_more_in_progress);
                        if (!mLoadDelegate.onLoadMore()) {
                            notifyLoadingCompleted();
                        }
                    }
                }
            });
        }
        return mLoadMoreView;
    }

    public void notifyLoadingCompleted() {
        mIsLoading = false;
        if (null != mLoadMoreView) {
            ((TextView)mLoadMoreView.findViewById(R.id.text)).setText(R.string.load_more);
        }
    }


    static class ViewHolder {
    	
//    	LinearLayout layout_friend_item;//item的整个布局
    	
		ImageView imgThumbnail;
		TextView txtName;
        TextView txtDate;
		TextView txtTime;
		TextView txtContent;
        TableLayout imageTable;
        LinearLayout reviewLayout;
		SpannedTextView txtLikeNames;
        TextView txtLoc;
		LinearLayout mReview;
        ImageView reviewDivider;
//        LinearLayout reviewButtons;
		Button btnLike;
		Button btnComment;
        Button btnAnswer;
        TextView tvLike;
        TextView tvComment;
        TextView tvAnswer;
        LinearLayout layoutLike;
        LinearLayout layoutComment;
        LinearLayout layoutAnswer;
        TextView tvLikeCountInd;
        TextView tvCommentCountInd;
        TextView tvAnswerCountInd;

        RelativeLayout rlMomentFavoriteLayout;
        ImageView ivMomentFavorite;
//        ImageView ivMomentOpEllipse;
		
		LinearLayout layoutReview;

        LinearLayout micLayout;
        ProgressBar progress;
        ImageView micButton;
        TimerTextView micTime;
//        ImageView imgLocation;

        TextView tvMomentTagDesc;
        ImageView ivMomentTagColor;

        TextView tvMomentShareRange;
        ImageView ivMomentShareRange;

        LinearLayout voteSurveyLayout;
        LinearLayoutAsListView lvSurveyOptions;
        Button btnSurvey;
	}

    private boolean isTheSameDay(long newTimestamp, long oldTimestamp) {
        Date newDate = new Date(newTimestamp);
        Date oldDate = new Date(oldTimestamp);
        if (newDate.getDate() == oldDate.getDate() && newDate.getMonth() == oldDate.getMonth()
                && newDate.getYear() == oldDate.getYear()) {
            return true;
        }
        return false;
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
        new AsyncTask<Void, Void, Void>() {
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
        }.execute((Void)null);
    }

    public static void setImageLayout(
            final Context context, final ImageResizer mImageResizer, final ArrayList<WFile> files, TableLayout table) {

        if (files.isEmpty()) {
            table.setVisibility(View.GONE);
            return;
        }
        table.removeAllViews();
        table.setVisibility(View.VISIBLE);

        int photoNum = files.size();
        int columnNum = (int) Math.sqrt(photoNum - 1) + 1;
        int rowNum = photoNum / columnNum + (photoNum % columnNum == 0 ? 0 : 1);

        final String[] thumbnailPathList = new String[photoNum];
        int i = 0;
        for (WFile file : files) {
            thumbnailPathList[i] = file.localThumbnailPath
                    = PhotoDisplayHelper.makeLocalFilePath(file.thumb_fileid, file.getExt());
            file.localPath = PhotoDisplayHelper.makeLocalFilePath(file.fileid, file.getExt());
            if (++i >= photoNum)
                break;
        }

        for (i = 0; i < rowNum; i++) {
            TableRow tableRow = new TableRow(context);
            for (int j  = 0; j < (photoNum <= columnNum ? photoNum : columnNum); j++) {
                final int photoIdx = i * columnNum + j;
                RecyclingImageView imageView = new RecyclingImageView(context);
                imageView.setClickDim(true);
                tableRow.addView(imageView);
                TableRow.LayoutParams params = (TableRow.LayoutParams) imageView.getLayoutParams();
                params.height = DensityUtil.dip2px(context, 80);
                params.width = DensityUtil.dip2px(context, 80);
                params.setMargins(0, 0, DensityUtil.dip2px(context, 3), 0);
                imageView.setLayoutParams(params);
                imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
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
            new AsyncTask<Void, Void, Integer>() {

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
            }.execute((Void)null);
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

    private void setTimeForAll(TextView txtTime, long time) {
        SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd HH:mm");
        String strTime = format.format(time);
        txtTime.setText(strTime);
    }

    private void setTimeForOne(TextView txtDate, TextView txtTime, long time) {
        SimpleDateFormat format = new SimpleDateFormat(context.getResources().getString(R.string.moment_me_time_format));
        String strDate = format.format(time);
        format = new SimpleDateFormat("HH:mm");
        String strTime = format.format(time);
        txtDate.setText(strDate);
        txtTime.setText(strTime);
    }

    private void showVoiceFile(WFile file, final LinearLayout voiceLayout, ProgressBar progress,
                                 ImageView micButton, TimerTextView micTime) {
        voiceLayout.setTag(TAG_NOT_PLAYING);
        if (file == null) {
            voiceLayout.setVisibility(View.GONE);
        } else {
            voiceLayout.setVisibility(View.VISIBLE);
            setViewForVoice(voiceLayout, micButton, progress, micTime, file);
        }
    }

    private Map<String,DlVoiceFileTask> mDlVoiceFileTaskMap = new HashMap<String, DlVoiceFileTask>();//记录当前正在下载的音频
    private void getVoiceFileFromServer(final LinearLayout voiceLayout, final ImageView micButton, final TimerTextView micTime,
            final ProgressBar progress, final WFile file) {
        final String path = PhotoDisplayHelper.makeLocalFilePath(file.fileid, file.getExt());
        micButton.setVisibility(View.GONE);
        progress.setVisibility(View.VISIBLE);
    	mediaPlayerWraper.stop();
        //如果当前任务正在下载，避免new新的任务
		if (!mDlVoiceFileTaskMap.containsKey(path)) {
			DlVoiceFileTask taskDlVoiceFile = new DlVoiceFileTask() {
				boolean ok;
				@Override
				protected Void doInBackground(Void... voids) {
					WowTalkWebServerIF.getInstance(context).fGetFileFromServer(
							file.fileid, GlobalSetting.S3_MOMENT_FILE_DIR,
							new NetworkIFDelegate() {
								@Override
								public void didFinishNetworkIFCommunication(
										int i, byte[] bytes) {
									ok = true;
								}

								@Override
								public void didFailNetworkIFCommunication(
										int i, byte[] bytes) {
									ok = false;
								}

								@Override
								public void setProgress(int i, int i2) {
									publishProgress(i2);
								}
							}, 0, path, null);
					return null;
				}

				@Override
				protected void onPostExecute(Void v) {
					mDlVoiceFileTaskMap.remove(path);
					if(listener != null)
						listener.onTaskPost(ok);
				}

				@Override
				protected void onProgressUpdate(Integer... params) {
					if (progress != null) {
						progress.setProgress(params[0]);
					}
				}
			};
			mDlVoiceFileTaskMap.put(path, taskDlVoiceFile);
			taskDlVoiceFile.setOnDlVoiceFileTaskPostListener(new IOnDlVoiceFileTaskPostListener() {
				@Override
				public void onTaskPost(boolean isSuccess) {
					micButton.setVisibility(View.VISIBLE);
					progress.setVisibility(View.GONE);
					if (isSuccess) {
						if(path.equals(mCurrentPlayingVoicePath))
						{
							mediaPlayerWraper.setTemMediaPath(path);
							setViewForVoice(voiceLayout, micButton, progress,
									micTime, file);
							startPlayingVoice(voiceLayout, micButton, micTime,
									path, file);
						}
					}
				}
			});
			taskDlVoiceFile.execute((Void) null);
		}
    }

    public interface IOnDlVoiceFileTaskPostListener
    {
    	public void onTaskPost(boolean isSuccess);
    }
    
    private class DlVoiceFileTask extends AsyncTask<Void, Integer, Void>
    {
		public IOnDlVoiceFileTaskPostListener listener;
		public void setOnDlVoiceFileTaskPostListener(IOnDlVoiceFileTaskPostListener listener)
		{
			this.listener = listener;
		}
		@Override
		protected Void doInBackground(Void... paramArrayOfParams) {
			return null;
		}
    }
    
    //切换tag时候调用
    public void removeMediaPlayListener()
    {
    	mediaPlayerWraper.setWraperListener(null);
    	
    	//切换tag时，正在下载的item 引用更新
    	Iterator<Entry<String, DlVoiceFileTask>> iter = mDlVoiceFileTaskMap.entrySet().iterator();
    	while (iter.hasNext()) {
    		Map.Entry<String, DlVoiceFileTask> entry = (Map.Entry<String,DlVoiceFileTask>) iter.next();
    		DlVoiceFileTask val = (DlVoiceFileTask) entry.getValue();
    		val.setOnDlVoiceFileTaskPostListener(null);
    	}
    }
    
    private String mCurrentPlayingVoicePath;
    private void setViewForVoice(final LinearLayout voiceLayout, final ImageView micButton,
                                 final ProgressBar progress, final TimerTextView micTime, final WFile file) {
        micButton.setVisibility(View.VISIBLE);
        progress.setVisibility(View.GONE);
        final String localPath = PhotoDisplayHelper.makeLocalFilePath(file.fileid, file.getExt());
        //如果当前显示item的micTime 与 传入到mediaPlayerWraper的micTime是同一个引用，则需要判断当前item的音频是否跟mediaPlayerWraper的音频一致；不一致时，则不更新micTime
        //修改原因：在调用getview时，contentview不为null则不会生成新的view对象，所以多个tag切换时，不同的item的micTime保持了同一个引用，不同的item会展示同一个音频播放进度
        if(mediaPlayerWraper.isSameTvPlayTime(micTime))
        {
        	mediaPlayerWraper.setTemMediaPath(localPath);
        }
        if(localPath.equals(mediaPlayerWraper.getPlayingMediaPath())) {
        	mediaPlayerWraper.setWraperListener(new MediaPlayerWraper.MediaPlayerWraperListener() {
                @Override
                public void onPlayFail(String path) {
                    micButton.setImageResource(R.drawable.timeline_player_play);
                    micTime.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, file.duration / 60, file.duration % 60));
                }

                @Override
                public void onPlayBegin(String path) {
                    micButton.setImageResource(R.drawable.timeline_player_stop);
                }

                @Override
                public void onPlayComplete(String path) {
                    micButton.setImageResource(R.drawable.timeline_player_play);
                    micTime.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, file.duration / 60, file.duration % 60));
                }
            });
        	mediaPlayerWraper.setTemMediaPath(localPath);
            micButton.setImageResource(R.drawable.timeline_player_stop);
            mediaPlayerWraper.setPlayingTimeTV(micTime,true);
        } else {
            micButton.setImageResource(R.drawable.timeline_player_play);
            micTime.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, file.duration / 60, file.duration % 60));
        }
        
        //当前item是否正在下载音频文件
        if(mDlVoiceFileTaskMap.containsKey(localPath))
        {
        	 micButton.setVisibility(View.GONE);
             progress.setVisibility(View.VISIBLE);
             
             //更新控件的引用，原因同 micTime 一样，不同tag的不同item可能持有相同控件的引用
             DlVoiceFileTask task = mDlVoiceFileTaskMap.get(localPath);
             if(task != null)
             {
            	 task.setOnDlVoiceFileTaskPostListener(new IOnDlVoiceFileTaskPostListener() {
     				@Override
     				public void onTaskPost(boolean isSuccess) {
     					micButton.setVisibility(View.VISIBLE);
     					progress.setVisibility(View.GONE);
     					if (isSuccess) {
     						if(localPath.equals(mCurrentPlayingVoicePath))
     						{
     							mediaPlayerWraper.setTemMediaPath(localPath);
     							setViewForVoice(voiceLayout, micButton, progress,
     									micTime, file);
     							startPlayingVoice(voiceLayout, micButton, micTime,
     									localPath, file);
     						}
     					}
     				}
     			});
             }
        }
        voiceLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            	mCurrentPlayingVoicePath = localPath;
                if (new File(localPath).exists()) {
                	mediaPlayerWraper.setTemMediaPath(localPath);
                    startPlayingVoice(voiceLayout, micButton, micTime, localPath,file);
                    if(mediaPlayerWraper.getPlayingMediaPath() == null)
                    {
                    	micButton.setImageResource(R.drawable.timeline_player_play);
                    }
                } else {
                    getVoiceFileFromServer(voiceLayout, micButton, micTime, progress, file);
                }
            }
        });
    }

    private void startPlayingVoice(final LinearLayout voiceLayout, final ImageView micButton, final TimerTextView micTime,
                                   final String localPath, final WFile file) {
        if(!localPath.equals(mediaPlayerWraper.getPlayingMediaPath())) {
            mediaPlayerWraper.stop();
            mediaPlayerWraper.setPlayingTimeTV(micTime,true);
            mediaPlayerWraper.setWraperListener(new MediaPlayerWraper.MediaPlayerWraperListener() {
                @Override
                public void onPlayFail(String path) {
                    micButton.setImageResource(R.drawable.timeline_player_play);
                    micTime.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, file.duration / 60, file.duration % 60));
                }

                @Override
                public void onPlayBegin(String path) {
                    micButton.setImageResource(R.drawable.timeline_player_stop);
                }

                @Override
                public void onPlayComplete(String path) {
                    micButton.setImageResource(R.drawable.timeline_player_play);
                    micTime.setText(String.format(TimerTextView.VOICE_LEN_DEF_FORMAT, file.duration / 60, file.duration % 60));
                }
            });
            mediaPlayerWraper.triggerPlayer(localPath,file.duration);
        } else {
            //second trigger,stop
            mediaPlayerWraper.triggerPlayer(localPath,file.duration);
        }

    }

    /**
     *
     * @param context
     * @param textView
     * @param reviews
     * @return how many likers?
     */
    public static int setViewForLikeReview(
            final Context context, SpannedTextView textView, final ArrayList<Review> reviews) {

        textView.clear();

        if (reviews == null || reviews.isEmpty())
            return 0;

        int cnt = 0;
        for (int i = 0; i < reviews.size(); i++) {
            final Review review = reviews.get(i);
            if (review == null || review.type != Review.TYPE_LIKE)
                continue;

            if (cnt > 0) {
                textView.append(context.getResources().getString(R.string.symbol_comma), null,
                        0, 0, 0, null);
            }
            ++cnt;
            if (!TextUtils.isEmpty(review.nickname)) {
                textView.append(review.nickname, null,
                        context.getResources().getColor(R.color.blue),
                        0,
                        context.getResources().getColor(R.color.text_gray3),
                        new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TimelineActivity.launch(context, review.uid, review.nickname);
                    }
                });
            } else {
                textView.append("(null)", null,
                        context.getResources().getColor(R.color.blue),
                        0,
                        context.getResources().getColor(R.color.text_gray3),
                        null
                );
            }
        }

        if(0 == cnt) {
            textView.clear();
        }
        return cnt;
    }

    /**
     * @param context
     * @param layout
     * @param reviews
     * @param momentPosition
     * @param moment
     * @param mReplyDelegate
     * @return actual count of reviews being displayed.
     */
    public static int setViewForCommentReview(
            final Context context, LinearLayout layout, final ArrayList<Review> reviews,
            final int momentPosition, final Moment moment,
            final MomentActionHandler mReplyDelegate) {

        layout.removeAllViews();

        if (reviews == null || reviews.isEmpty()) {
            return 0;
        }

        int n = 0;

        for (final Review review : reviews) {
            if (review.type == Review.TYPE_LIKE)
                continue;

            View view = LayoutInflater.from(context).inflate(R.layout.listitem_trend_comment, null);
            LinearLayout parentLayout = (LinearLayout) view.findViewById(R.id.parent_layout);
            SpannedTextView textView = (SpannedTextView) view.findViewById(R.id.comment);
            if (!TextUtils.isEmpty(review.nickname)) {
                textView.append(review.nickname, null,
                        context.getResources().getColor(R.color.blue),
                        0,
                        context.getResources().getColor(R.color.text_gray3),
                        new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        TimelineActivity.launch(context, review.uid, review.nickname);
                    }
                });
            } else {
                textView.append("(null)", null,
                        context.getResources().getColor(R.color.blue),
                        0,
                        context.getResources().getColor(R.color.text_gray3),
                        null
                );
            }
            if (review.replyToNickname != null && !Utils.isNullOrEmpty(review.replyToReviewId)){
                textView.append(" " + context.getResources().getString(R.string.moments_reply) + " ", null,
                        0, 0, 0, null);
                textView.append(review.replyToNickname, null,
                        context.getResources().getColor(R.color.blue),
                        0,
                        context.getResources().getColor(R.color.text_gray3),
                        new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                TimelineActivity.launch(context, review.replyToUid, review.replyToNickname);
                            }
                        });
            }
            textView.append(context.getResources().getString(R.string.symbol_colon) + " ", null,
                        0, 0, 0, null);
            textView.append(review.text, null,
                    0, 0, 0, null);
            textView.setBgClicked(context.getResources().getColor(R.color.text_gray3),
                    new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (mReplyDelegate != null) {
                                mReplyDelegate.replyToMoment(momentPosition, moment, review, false);
                            }
                        }
                    });
            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View v) {
                    return true;
                }
            });
            parentLayout.removeView(textView);
            layout.addView(textView);
            ++n;
        }

        return n;
    }

    public void stopMedia() {
        mediaPlayerWraper.stop();
        mCurrentPlayingVoicePath = null;
    }

    public void setReplyDelegate(MomentActionHandler replyDelegate) {
        this.mReplyDelegate = replyDelegate;
    }

    public void setLoadDelegate(LoadDelegate d) {
        mLoadDelegate = d;
    }

    /**
     * require notifyDataSetChanged() manually.
     * @param showLoadMoreAsLastItem
     */
    public void setShowLoadMoreAsLastItem(boolean showLoadMoreAsLastItem) {
        this.showLoadMoreAsLastItem = showLoadMoreAsLastItem;
    }
}
