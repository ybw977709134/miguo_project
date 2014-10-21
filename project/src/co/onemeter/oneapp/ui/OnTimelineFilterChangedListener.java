package co.onemeter.oneapp.ui;

/**
 * <p>用于侦听时间线过滤器的变化。</p>
 * Created by pzy on 10/21/14.
 */
public interface OnTimelineFilterChangedListener {
    public void onSenderChanged(int index);

    /**
     * @param index 从0开始，0表示不限。
     */
    public void onTagChanged(int index);
}
