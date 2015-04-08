package co.onemeter.oneapp.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 用来显示好友圈中动态的时间
 * @author hutianfeng
 * @date 2015/4/8
 *
 */
public class MomentDateShow {
	
	private static long time;//获取数据库中发布动态时存取的时间戳
	public MomentDateShow(long time){
		this.time = time;
	}
	
	public static String showMomentDate(){
		String momentDate = null;//显示在动态的字符串
		
		 SimpleDateFormat format = new SimpleDateFormat("yyyy/MM/dd");
	        String strDate = format.format(time);
	        
	        format = new SimpleDateFormat("HH:mm");
	        String strTime = format.format(time);
	        
	        
	        //系统默认时间(当前时间)
			SimpleDateFormat   formatter   =   new   SimpleDateFormat   ("yyyy-MM-dd");     
			 Date   curDate   =   new   Date(System.currentTimeMillis());//获取当前时间     
			String   str   =   formatter.format(curDate); 
			int mYear = Integer.valueOf(str.substring(0, 4));
			int mMonth = Integer.valueOf(str.substring(5, 7))-1;
			int mDay = Integer.valueOf(str.substring(8));
	        
	        
		return momentDate;
	}
	
}
