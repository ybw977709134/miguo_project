package co.onemeter.oneapp.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 用来显示好友圈中动态的时间
 * 
 * 显示规则：不同年份和同年不同月直接显示年月日
 * 同年同月不同天，显示天数
 * 同年同月同天，显示小时
 * 同年同月同天同小时，显示分钟数
 * @author hutianfeng
 * @date 2015/4/8
 *
 */
public class MomentDateShow {
	
	private static long time;//获取数据库中发布动态时存取的时间戳
	public MomentDateShow(long time){
		this.time = time;
	}
	
	public static String showMomentDate(long time){
//		String momentDate = null;//显示在动态的字符串
		Date   curDate   =   new   Date(System.currentTimeMillis());//获取当前时间
		
		 SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");//年月日
		 	//动态发布时的时间戳转化为日期格式
	        String strDate = format.format(time);
	        //动态对应的年月日
	        int mYear = Integer.valueOf(strDate.substring(0, 4));
			int mMonth = Integer.valueOf(strDate.substring(5, 7))-1;
			int mDay = Integer.valueOf(strDate.substring(8));
	        
	        //当前系统时间
	        String   cur_date   =   format.format(curDate); 
	        //当前时间对应的年月日
			int curYear = Integer.valueOf(cur_date.substring(0, 4));
			int curMonth = Integer.valueOf(cur_date.substring(5, 7))-1;
			int curDay = Integer.valueOf(cur_date.substring(8));
	        
	        
	        format = new SimpleDateFormat("HH:mm");//时分
	        String strTime = format.format(time);
	        
	        int mHour = Integer.valueOf(strTime.substring(0, 2));
			int mMinute = Integer.valueOf(strTime.substring(3));
			
			
			 String   cur_time   =   format.format(curDate);
	        
			 int curHour = Integer.valueOf(cur_time.substring(0, 2));
			 int curMinute = Integer.valueOf(cur_time.substring(3));
			 
			 
			 if (curYear - mYear > 0) {//动态发布在去年
				 return strDate;
			 } else {//同年比较月份
				 if (curMonth - mMonth > 0) {//不同月
					 return strDate;
				 } else {//同月
					 if (curDay - mDay > 0 ) {//不同天
						 if (curDay - mDay == 1) {
							 return "昨天";
						 } else {
							 return (curDay - mDay - 1) + "天前";
						 }
						 
					 } else {//同一天
						if (curHour - mHour > 0) {//不同小时
							return (curHour - mHour) + "小时前";
						}  else {//同个小时
							if (curMinute - mMinute > 0) {//不同分钟
								return (curMinute - mMinute) + "分钟前";
							} else {//同分钟
								return "1分钟前";
							}
						}
					 }
				 }
			 }
 
	}
	
}
