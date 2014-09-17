package org.wowtalk.api;

public class CallState {
	public static final int CallEnd = 0;// =CallIdle
	public static final int OutgoCall_CalleeBusy = 1;
	public static final int OutgoCall_CallNoAnswer = 2;
	public static final int OutgoCall_CalleeOffline = 3;
	public static final int OutgoCall_Init = 4;
	public static final int OutgoCall_CalleeRinging = 5;
	public static final int Call_Connected = 6;
	public static final int IncomeCall_WaitForCallerCallAgain = 8;
	public static final int IncomeCall_Init = 9;
	public static final int OUTGOCALL_HANGUP= 10;
	public static final int OutgoCall_Callee_Logout_NotExist = 11;
}
