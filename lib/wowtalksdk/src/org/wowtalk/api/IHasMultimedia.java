package org.wowtalk.api;

import java.util.Iterator;

/**
 * Holds a WFile list, and the list is stored in local db.
 * User: pan
 * Date: 4/19/13
 * Time: 7:46 PM
 */
public interface IHasMultimedia {
    public String getMediaDataTableName();
    public String getMediaDataTablePrimaryKeyName();
    public String getMediaDataTablePrimaryKeyValue();
    public int getMediaCount();
    public Iterator<WFile> getMediaIterator();
    public void addMedia(WFile media);
    public void clearMedia();
}
