package org.wowtalk.api;

import android.content.Context;
import android.os.Parcelable;

import java.util.ArrayList;

public interface IHasPhoto extends Parcelable {
	public int getAccountType();
	public String getGUID();
	public void setPhotoUploadedTimestamp(long value);
	public long getPhotoUploadedTimestamp();

    public boolean isHybrid();
    public ArrayList<IHasPhoto> getHybrid(Context context);
}
