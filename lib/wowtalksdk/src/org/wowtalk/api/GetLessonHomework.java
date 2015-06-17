package org.wowtalk.api;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by hutianfeng on 15-5-20.
 * @date 2015/5/20
 */
public class GetLessonHomework implements Parcelable{

    public int id;
    public int lesson_id;
    public int moment_id;
    public String teacher_id;
    public HomeWorkMoment teacherMoment;
    public List<HomeWorkResult> stuResultList;
    public int homework_id;
    public String title;



    public GetLessonHomework(){}


//    public int getId(){
//        return this.id;
//    }
//
//
//    public int getLessonId(){
//        return this.lesson_id;
//    }
//
//    public int getMomentId(){
//        return this.moment_id;
//    }
//
//    public HomeWorkMoment getTeacherMoment(){
//        return this.teacherMoment;
//    }
//
//    public List<HomeWorkResult> getStuResultList(){
//        return this.stuResultList;
//    }
//
//
//    public int getHomeWorkId(){
//        return this.homework_id;
//    }
//
//    public String getTitle(){
//        return this.title;
//    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(id);
        dest.writeInt(lesson_id);
        dest.writeInt(moment_id);
        dest.writeParcelable(teacherMoment, flags);
        dest.writeTypedList(stuResultList);
        dest.writeInt(homework_id);
        dest.writeString(title);

    }

    public final static Creator<GetLessonHomework> CREATOR = new Creator<GetLessonHomework>() {
        @Override
        public GetLessonHomework createFromParcel(Parcel source) {
            GetLessonHomework l = new GetLessonHomework();
            l.id = source.readInt();
            l.lesson_id = source.readInt();
            l.moment_id = source.readInt();
            l.teacherMoment = source.readParcelable(HomeWorkMoment.class.getClassLoader());
            l.stuResultList = new ArrayList<HomeWorkResult>();
            source.readTypedList(l.stuResultList,HomeWorkResult.CREATOR);
            l.homework_id = source.readInt();
            l.title = source.readString();
            return l;
        }

        @Override
        public GetLessonHomework[] newArray(int size) {
            return new GetLessonHomework[size];
        }
    };
}
