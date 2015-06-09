package org.wowtalk.api;

import android.content.Context;
import android.os.Parcelable;

import java.util.ArrayList;

public interface IHasPhoto extends Parcelable {
	int getAccountType();
	String getGUID();
	String getRemotePhotoPath();
	String getRemoteThumbnailPath();
	void setPhotoUploadedTimestamp(long value);
	long getPhotoUploadedTimestamp();

    boolean isHybrid();
    ArrayList<IHasPhoto> getHybrid(Context context);
}
