package co.onemeter.oneapp.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * Created with IntelliJ IDEA.
 * User: pan
 * Date: 13-5-22
 * Time: PM3:22
 * To change this template use File | Settings | File Templates.
 */
public class ScrollViewWithoutFadingEdge extends ScrollView {
    public ScrollViewWithoutFadingEdge(Context context) {
        super(context, null, 0);
    }

    public ScrollViewWithoutFadingEdge(Context context, AttributeSet attrs) {
        super(context, attrs, 0);
    }

    public ScrollViewWithoutFadingEdge(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    protected float getBottomFadingEdgeStrength () {
        return 0;
    }

    @Override
    protected float getTopFadingEdgeStrength () {
        return 0;
    }
}
