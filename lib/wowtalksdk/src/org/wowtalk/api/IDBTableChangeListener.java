package org.wowtalk.api;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-6-28
 * Time: 下午3:09
 * To change this template use File | Settings | File Templates.
 */
public interface IDBTableChangeListener {
    void onDBTableChanged(String tableName);
}
