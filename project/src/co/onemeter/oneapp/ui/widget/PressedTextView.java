package co.onemeter.oneapp.ui.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.TextView;

/**
 * Created by jacky on 15-3-26.
 */
public class PressedTextView extends TextView{

    public PressedTextView(Context context){
        super(context);
    }

    public PressedTextView(Context context, AttributeSet attrs){
        super(context, attrs);
    }

    float startX;
    float startY;
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()){
            case MotionEvent.ACTION_DOWN:
                startX = event.getX();
                startY = event.getY();
                setTextColor(0xcccccccc);
                break;
            case MotionEvent.ACTION_MOVE:
                float potX = event.getX();
                float potY = event.getY();
                if(Math.abs(potX - startX) > 100 || Math.abs(potY - startY) > 100){
                    setTextColor(0xffffffff);
                }
                break;
            case MotionEvent.ACTION_UP:
                setTextColor(0xffffffff);
                break;
        }
        return super.onTouchEvent(event);
    }
}
