package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;
import android.text.TextUtils;
import org.wowtalk.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Moment, status, mini-blog, or whatever.
 */
public class Moment implements Parcelable, IHasMultimedia, IHasReview {
	public String id;
    public String text;
    public double latitude, longitude;
    public String place;
    public int privacyLevel = 1;
    public boolean allowReview = true;
    public ArrayList<WFile> multimedias;
    public ArrayList<Review> reviews;
    public long timestamp;
    public Buddy owner;
	public boolean likedByMe;

    public final static String SERVER_MOMENT_TAG_FOR_NOTICE ="0";
    public final static String SERVER_MOMENT_TAG_FOR_QA ="1";
    public final static String SERVER_MOMENT_TAG_FOR_STUDY ="2";
    public final static String SERVER_MOMENT_TAG_FOR_SURVEY_SINGLE ="3";
    public final static String SERVER_MOMENT_TAG_FOR_SURVEY_MULTI ="4";
    public final static String SERVER_MOMENT_TAG_FOR_LIFE ="5";
    public final static String SERVER_MOMENT_TAG_FOR_VIDEO ="6";

    public String tag="";

    public final static String SERVER_SHARE_RANGE_PUBLIC="0";
    public final static String SERVER_SHARE_RANGE_LIMITED="1";
    public String shareRange;
    public final static String LIMITED_DEPARTMENT_SEP="#";
    public ArrayList<String> limitedDepartmentList=new ArrayList<String>();

//    private final static String VOTED_OPTION_SEP="#";
    public boolean isSurveyAllowMultiSelect=false;
//    public ArrayList<Integer> votedOption= new ArrayList<Integer>();
    public ArrayList<SurveyOption> surveyOptions=new  ArrayList < SurveyOption >();
    public String surveyDeadLine="";

    public boolean isFavorite=false;

    public Moment() {
        init();
    }

    public Moment(String id) {
        this.id = id;
        init();
    }

    private void init() {
        multimedias = new ArrayList<WFile>();
        reviews = new ArrayList<Review>();
        owner = null;
    }

    public void sortSurveyOption() {
        if(null == surveyOptions) {
            return;
        }
        try {
            Comparator<SurveyOption> comparator = new Comparator<SurveyOption>(){
                public int compare(SurveyOption s0, SurveyOption s1) {
                    int ret=0;
                    try {
                        if(null != s0 && null != s1) {
                            int optionId0=Integer.valueOf(s0.optionId);
                            int optionId1=Integer.valueOf(s1.optionId);
                            ret=optionId0-optionId1;
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                    return ret;
                }
            };

            Collections.sort(surveyOptions,comparator);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isMeVoted() {
        for(SurveyOption aOption : surveyOptions) {
            if(aOption.isVoted) {
                return true;
            }
        }

        return false;
    }

//    public void setOptionVoted(String optionId) {
//        for(SurveyOption aOption : surveyOptions) {
//            if(aOption.optionId.equals(optionId)) {
//                ++aOption.votedNum;
//                aOption.isVoted=true;
//                break;
//            }
//        }
//    }

    public void showVoteInfo() {
        Log.i("allow_multi_select "+isSurveyAllowMultiSelect);
//        for(int i=0; i<votedOption.size(); ++i) {
//            Log.i("voted "+votedOption.get(i));
//        }
        for(int i=0; i<surveyOptions.size(); ++i) {
            Log.i("survey option "+i+" : "+surveyOptions.get(i).momentId+
                    "-"+surveyOptions.get(i).optionId+
                    "-"+surveyOptions.get(i).optionDesc+"-"+surveyOptions.get(i).votedNum);
        }
    }

    public void setLimitedDepartment(String limitedDeps) {
        if(TextUtils.isEmpty(limitedDeps)) {
            return;
        }

        try {
            String[] deps=limitedDeps.split(LIMITED_DEPARTMENT_SEP);
            for(String aD : deps) {
                limitedDepartmentList.add(aD);
            }
            Log.i("limited dep "+limitedDepartmentList.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLimitedDepartment() {
        StringBuilder sb=new StringBuilder();

        for(int i=0; i<limitedDepartmentList.size(); ++i) {
            sb.append(limitedDepartmentList.get(i));
            if(i != limitedDepartmentList.size()-1) {
                sb.append(LIMITED_DEPARTMENT_SEP);
            }
        }
        return sb.toString();
    }

//    public void setVotedOption(String votedOptionString) {
//        if(TextUtils.isEmpty(votedOptionString)) {
//            return;
//        }
//        try {
//            String[] votedOptionsA=votedOptionString.split(VOTED_OPTION_SEP);
//            for(String aA : votedOptionsA) {
//                votedOption.add(Integer.valueOf(aA));
//            }
//            Log.i("voted option "+votedOption.toString());
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

//    public String getVotedOption() {
//        StringBuilder sb=new StringBuilder();
//
//        for(int i=0; i<votedOption.size(); ++i) {
//            sb.append(votedOption.get(i));
//            if(i != votedOption.size()-1) {
//                sb.append(VOTED_OPTION_SEP);
//            }
//        }
//        return sb.toString();
//    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(allowReview ? 1 : 0);
        dest.writeString(id);
        dest.writeDouble(latitude);
        dest.writeInt(likedByMe ? 1 : 0);
        dest.writeDouble(longitude);
        dest.writeTypedList(multimedias == null ? new ArrayList<WFile>() : multimedias);
        dest.writeParcelable(owner, flags);
        dest.writeInt(privacyLevel);
        dest.writeTypedList(reviews == null ? new ArrayList<Review>() : reviews);
        dest.writeString(text);
        dest.writeLong(timestamp);
        dest.writeString(place);
        dest.writeString(tag);
        dest.writeString(shareRange);
        dest.writeInt(isSurveyAllowMultiSelect?1:0);
        dest.writeString(surveyDeadLine);
        dest.writeInt(isFavorite?1:0);
    }

    public static Creator<Moment> CREATOR = new Creator<Moment>() {
        @Override
        public Moment createFromParcel(Parcel s) {
            Moment m = new Moment();
            m.allowReview = 1 == s.readInt();
            m.id = s.readString();
            m.latitude = s.readDouble();
            m.likedByMe = 1 == s.readInt();
            m.longitude = s.readDouble();
            m.multimedias = new ArrayList<WFile>();
            s.readTypedList(m.multimedias, WFile.CREATOR);
            m.owner = s.readParcelable(Buddy.class.getClassLoader());
            m.privacyLevel = s.readInt();
            m.reviews = new ArrayList<Review>();
            s.readTypedList(m.reviews, Review.CREATOR);
            m.text = s.readString();
            m.timestamp = s.readLong();
            m.place=s.readString();
            m.tag=s.readString();
            m.shareRange=s.readString();
            m.isSurveyAllowMultiSelect=s.readInt()==1?true:false;
            m.surveyDeadLine=s.readString();
            m.isFavorite=s.readInt()==1?true:false;
            return m;
        }

        @Override
        public Moment[] newArray(int size) {
            return new Moment[size];
        }
    };

    @Override
    public String getMediaDataTableName() {
        return "moment_media";
    }

    @Override
    public String getMediaDataTablePrimaryKeyName() {
        return "moment_id";
    }

    @Override
    public String getMediaDataTablePrimaryKeyValue() {
        return id;
    }

    @Override
    public int getMediaCount() {
        return multimedias == null ? 0 : multimedias.size();
    }

    @Override
    public Iterator<WFile> getMediaIterator() {
        return multimedias == null ? null : multimedias.iterator();
    }

    @Override
    public void addMedia(WFile media) {
        if (multimedias == null)
            multimedias = new ArrayList<WFile>();
        multimedias.add(media);
    }

    @Override
    public void clearMedia() {
        if (multimedias != null && !multimedias.isEmpty())
            multimedias.clear();
    }

    @Override
    public String getReviewDataTableName() {
        return "moment_review";
    }

    @Override
    public String getReviewDataTablePrimaryKeyName() {
        return "moment_id";
    }

    @Override
    public String getReviewDataTablePrimaryKeyValue() {
        return id;
    }

    @Override
    public int getReviewsCount() {
        return reviews == null ? 0 : reviews.size();
    }

    @Override
    public Iterator<Review> getReviewIterator() {
        return reviews == null ? null : reviews.iterator();
    }

    @Override
    public void addReview(Review review) {
        if (reviews == null)
            reviews = new ArrayList<Review>();
        reviews.add(review);
    }

    @Override
    public void clearReviews() {
        if (reviews != null && !reviews.isEmpty())
            reviews.clear();
    }

    public static class SurveyOption {
        public String momentId;
        public String optionDesc;
        public int votedNum;
        public String optionId;
        public boolean isVoted;
    }
}
