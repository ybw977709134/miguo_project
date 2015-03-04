package co.onemeter.oneapp.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.*;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import co.onemeter.oneapp.R;

public class SideBar extends View {
	
	private boolean showBg = false;
	private float textSize;
	private int choose = -1;
	private OnTouchingLetterChangedListener onTouchingLetterChangedListener;
    private Bitmap mStartBmp = null;
	
	private String[] labels = {"Se", "#", "A", "B", "C", "D", "E", "F", "G",
			"H", "I", "J", "K", "L", "M", "N", "O", "P", "Q", "R", "S", "T",
			"U", "V", "W", "X", "Y", "Z"};
	Paint paint = new Paint();
	
	public SideBar(Context context) {
		super(context);
        init();
	}

    public SideBar(Context context, AttributeSet attrs) {
		super(context, attrs);
		TypedArray a = context.obtainStyledAttributes(attrs, co.onemeter.oneapp.R.styleable.SideBar);
		textSize = a.getDimension(co.onemeter.oneapp.R.styleable.SideBar_textSize, 23);
        Drawable d = a.getDrawable(R.styleable.SideBar_drawableStart);
        if (d != null) {
            mStartBmp = drawableToBitmap(d);
        }
        a.recycle();
        init();
	}

    public SideBar(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		TypedArray a = context.obtainStyledAttributes(attrs, co.onemeter.oneapp.R.styleable.SideBar);
		textSize = a.getDimension(co.onemeter.oneapp.R.styleable.SideBar_textSize, 23);
        Drawable d = a.getDrawable(R.styleable.SideBar_drawableStart);
        if (d != null) {
            mStartBmp = drawableToBitmap(d);
        }
		a.recycle();
        init();
	}

    private static Bitmap drawableToBitmap (Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable)drawable).getBitmap();
        }

        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                drawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);

        return bitmap;
    }

    private void init() {
        paint.setAntiAlias(true);
        paint.setTextSize(textSize);
        paint.setFakeBoldText(true);
    }

	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		
		if (showBg) {
			canvas.drawColor(Color.parseColor("#40000000"));
		}

		int height = getHeight();
		int width = getWidth();
		int singleHeight = height / labels.length;
        final int offsetY = 3;

        if(mStartBmp != null) {
            int sz = singleHeight - 4;
            if(sz > mStartBmp.getWidth())
                sz = mStartBmp.getWidth();

            canvas.drawBitmap(mStartBmp, null,
                    new RectF(
                            (width - sz) / 2,
                            (singleHeight - sz) / 2 + offsetY,
                            (width + sz) / 2,
                            (singleHeight + sz) / 2 + offsetY),
                    paint);
        } else {
            singleHeight = height / (labels.length - 1);
        }

		for (int i = 1; i < labels.length; i++) {
			if (i == choose) {
				paint.setColor(Color.parseColor("#3399ff"));
			} else {
                paint.setColor(Color.parseColor("#999999"));
            }
			float xPos = width / 2 - paint.measureText(labels[i]) / 2;
			float yPos = singleHeight * i + offsetY;
			canvas.drawText(labels[i], xPos, yPos, paint);
		}
	}
	
	public void setOnTouchingLetterChangedListener(OnTouchingLetterChangedListener onTouchingLetterChangedListener) {
		this.onTouchingLetterChangedListener = onTouchingLetterChangedListener;
	}
	
	public interface OnTouchingLetterChangedListener {
		public void onTouchingLetterChanged(String s);
        public void onTouchCanceled();
	}

	@Override
	public boolean dispatchTouchEvent(MotionEvent event) {
		final int eventInt = event.getAction();
		final float y = event.getY();
		final int oldChoose = choose;
		final int c = (int) (y / getHeight() * labels.length);
		final OnTouchingLetterChangedListener listener = onTouchingLetterChangedListener;
		switch (eventInt) {
		case MotionEvent.ACTION_DOWN:
			System.out.println("ACTION_DOWN!");
			showBg = true;
			if (oldChoose != c) {
				if (c >= 0 && c < labels.length) {
					if (listener != null) {
						listener.onTouchingLetterChanged(labels[c]);
					}
					choose = c;
					this.invalidate();
				}
			}
			break;
		case MotionEvent.ACTION_MOVE:
			if (oldChoose != c) {
				if (c >= 0 && c < labels.length) {
					if (listener != null) {
						listener.onTouchingLetterChanged(labels[c]);
					}
					choose = c;
					invalidate();
				}
			}
			break;
		case MotionEvent.ACTION_UP:
			showBg = false;
			choose = -1;
			invalidate();
            if (onTouchingLetterChangedListener != null) {
                onTouchingLetterChangedListener.onTouchCanceled();
            }
			break;

		default:
			break;
		}
		return true;
	}
	
	public boolean onTouchEvent(MotionEvent event) {
		return super.onTouchEvent(event);
	}

}
