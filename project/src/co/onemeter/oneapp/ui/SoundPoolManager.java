package co.onemeter.oneapp.ui;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import java.util.HashMap;

/**
 * Created with IntelliJ IDEA.
 * User: xuxiaofeng
 * Date: 13-8-5
 * Time: 下午12:00
 * To change this template use File | Settings | File Templates.
 */
public class SoundPoolManager {
    public final static int USED_AUDIO_STREAM_TYPE=AudioManager.STREAM_MUSIC;
    private static SoundPool soundPool = new SoundPool(3, USED_AUDIO_STREAM_TYPE, 0);

    //soundIdInRaw(R.raw.xxx) ----- soundIdInSoundPool
    private static HashMap<Integer, Integer> soundPoolMapForRaw = new HashMap<Integer, Integer>();

    private static void loadSoundFromRaw(Context context,int rawResId) {
        if(!soundPoolMapForRaw.containsKey(rawResId)) {
            soundPoolMapForRaw.put(rawResId, soundPool.load(context, rawResId, 1));
            Log.i("raw id "+rawResId+" loaded");
        } else {
            Log.i("raw id "+rawResId+" already loaded");
        }
    }

    public static void playSoundFromRaw(final Context context,final int rawResId) {
        Integer soundId=soundPoolMapForRaw.get(rawResId);
        if(null == soundId) {
            loadSoundFromRaw(context,rawResId);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        Thread.sleep(500);
                        playSoundFromRaw(context,rawResId);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }).start();
        } else {
            AudioManager mgr = (AudioManager)context.getSystemService(Context.AUDIO_SERVICE);
            int ringtoneMode=mgr.getRingerMode();
            if(AudioManager.RINGER_MODE_NORMAL == ringtoneMode) {
                float streamVolumeCurrent = mgr.getStreamVolume(USED_AUDIO_STREAM_TYPE);

                float streamVolumeMax = mgr.getStreamMaxVolume(USED_AUDIO_STREAM_TYPE);

                float volume = streamVolumeCurrent/streamVolumeMax;

                Log.i("volume cur: "+streamVolumeCurrent+",max:"+streamVolumeMax);
                soundPool.play(soundId, volume, volume, 1, 0, 1f);
            }
        }
    }

    public static void release() {
        soundPool.release();
    }
}
