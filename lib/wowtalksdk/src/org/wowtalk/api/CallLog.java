package org.wowtalk.api;

import android.os.Bundle;


public class CallLog {
	private static final String PREFIX = "org.wowtalk.calllog.";
	private static final String EXTRAS_PRIMARY_ID = PREFIX
	+ "EXTRAS_PRIMARY_ID";
	private static final String EXTRAS_CONTACT_ID = PREFIX
			+ "EXTRAS_CONTACT_ID";
	private static final String EXTRAS_CONTACT_DISPLAYNAME = PREFIX
			+ "EXTRAS_CONTACT_DISPLAYNAME";
	private static final String EXTRAS_STATUS = PREFIX
			+ "EXTRAS_STATUS";
	private static final String EXTRAS_DIRECTION = PREFIX
	+ "EXTRAS_DIRECTION";
	private static final String EXTRAS_STARTDATE = PREFIX + "EXTRAS_STARTDATE";

	private static final String EXTRAS_DURATION = PREFIX + "EXTRAS_DURATION";

	private static final String EXTRAS_QUALITY = PREFIX + "EXTRAS_QUALITY";

	private static final String EXTRAS_IS_WOWTALK_CALLLOG = PREFIX
			+ "EXTRAS_IS_WOWTALK_CALLLOG";

	

	

	
	final public static int WowTalkCallSuccess = 0;
	final public static int WowTalkCallMissed = 1;
	
	
	public int  primaryKey=-1;
	public String contact="";
	public String displayName="";
	public int status;  //0:success; 1:aborted or missed
	public String direction;   //"out" or "in"
	public String startDate;
	public int  duration;
	public int  quality;

	public String chatUserRecordID;
	public String compositeName;
	public String phoneType;
	
	public boolean hasPrimaryKey() {
		return primaryKey != -1;
	}
	
	/**
	 *  internal use */
	public boolean isSelected;


	
	public CallLog() {

	}
	
	public CallLog(Bundle b) {
		primaryKey = b.getInt(EXTRAS_PRIMARY_ID);
		contact = b.getString(EXTRAS_CONTACT_ID);
		displayName = b.getString(EXTRAS_CONTACT_DISPLAYNAME);
		status = b.getInt(EXTRAS_STATUS);
		direction=b.getString(EXTRAS_DIRECTION);
		startDate = b.getString(EXTRAS_STARTDATE);
		duration = b.getInt(EXTRAS_DURATION);
		quality = b.getInt(EXTRAS_QUALITY);
	}

	/**
	 * Convert all ChatMessage data to an extras bundle to send via an intent
	 */
	public Bundle toBundle() {
		Bundle b = new Bundle();
		b.putBoolean(EXTRAS_IS_WOWTALK_CALLLOG, true);

		b.putInt(EXTRAS_PRIMARY_ID, primaryKey);
		b.putString(EXTRAS_CONTACT_ID, contact);
		b.putString(EXTRAS_CONTACT_DISPLAYNAME, displayName);

		b.putInt(EXTRAS_STATUS, status);
		b.putString(EXTRAS_DIRECTION, direction);
		b.putString(EXTRAS_STARTDATE, startDate);
		b.putInt(EXTRAS_DURATION, duration);
		b.putInt(EXTRAS_QUALITY, quality);

		return b;
	}

}
