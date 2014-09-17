/**
 * Display auto increased elapsed time.
 */
package org.wowtalk.ui.msg;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.widget.TextView;

import java.util.Timer;
import java.util.TimerTask;

public class TimerTextView extends TextView {

	private Timer timer;
	private int elapsed = 0;
    private int maxElapse=-1;

    private TimerTextView updateTarget;

    public final static String VOICE_LEN_DEF_FORMAT="%01d:%02d";
	
	public TimerTextView(Context context) {
		super(context);
	}
	
	public TimerTextView(Context context, AttributeSet attrs) {
		super(context, attrs);
	}
	
	public TimerTextView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
	}

    public void setMaxElapse(int max) {
        maxElapse=max;
    }

    public void setMaxTime() {
        elapsed=maxElapse;
        update();
    }

	public void reset() {
		elapsed = 0;
		update();
	}
	
	public void start() {
		reset();
		if(timer != null)
			timer.cancel();
		timer = new Timer();
		timer.schedule(new Task(), 1000, 1000);
	}
	
	public void stop() {
		if(timer != null) {
			timer.cancel();
            timer=null;
        }
	}
	
	/**
	 * @return seconds
	 */
	public int getElapsed() {
		return elapsed;
	}

    public void setUpdateTarget(TimerTextView ttv) {
        updateTarget=ttv;
    }

    private boolean isWithAppendAll=false;
    private void setTimerText(TextView tv,long time) {
        if(isWithAppendAll) {
            tv.setText(String.format(VOICE_LEN_DEF_FORMAT+"/"+VOICE_LEN_DEF_FORMAT, elapsed / 60, elapsed % 60,maxElapse / 60, maxElapse % 60));
        } else {
            tv.setText(String.format(VOICE_LEN_DEF_FORMAT, elapsed / 60, elapsed % 60));
        }
    }
	
	private void update() {
        if(null == timer) {
            return;
        }
        if(null == updateTarget) {
            this.post(new Runnable() {
                @Override
                public void run() {
                    setTimerText(TimerTextView.this,elapsed);
                    Log.i("TimerTextView","timer update to "+getText());
                }
            });
        } else {
            updateTarget.post(new Runnable() {
                @Override
                public void run() {
                    setTimerText(updateTarget,elapsed);
                    Log.i("TimerTextView","timer update to "+updateTarget.getText());
                }
            });
        }
    }
	
	private class Task extends TimerTask {
		@Override
		public void run() {
			++elapsed;
            if(maxElapse>0 && elapsed>maxElapse) {
                elapsed=maxElapse;
            }

            Log.i("TimerTextView","elapse to "+elapsed);
            update();
		}
	}
}
