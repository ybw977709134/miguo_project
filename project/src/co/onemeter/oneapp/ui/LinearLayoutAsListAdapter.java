package co.onemeter.oneapp.ui;

import android.view.View;
import android.view.ViewGroup;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-12-2
 * Time: 上午9:26
 * To change this template use File | Settings | File Templates.
 */
public interface LinearLayoutAsListAdapter {
    public int getCount();
    public View getView(final int position, View convertView, ViewGroup parent);
    public void notifyDataSetChanged();
    public void notifyDataSetInvalidated();
    public void setDataChangeListener(LinearLayoutAsListDataChangeListener listener);
}
