package org.wowtalk.ui.msg;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 3/21/13
 * Time: 11:20 AM
 * To change this template use File | Settings | File Templates.
 */
public interface IHeightAwareView {
    public interface OnHeightChangedListener {
        public void onHeightChanged(int requestHeight, int maxHeight);
    }

    public void setOnHeightChangedListener(IHeightAwareView.OnHeightChangedListener l);
}
