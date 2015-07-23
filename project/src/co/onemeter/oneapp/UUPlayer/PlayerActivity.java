package co.onemeter.oneapp.UUPlayer;

import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

import co.onemeter.oneapp.R;
import ulucu.api.bean.Device;
import ulucu.api.client.http.HttpClient;
import ulucu.api.client.http.listener.HttpControlViewListener;
import ulucu.api.client.http.listener.HttpDeviceIndexListener;
import ulucu.api.client.http.listener.HttpDeviceViewListener;
import ulucu.api.client.video.PlayListener;

/**
 * Created by hutianfeng on 15-7-21.
 */
public class PlayerActivity extends BaseActivity implements PlayListener,
        HttpDeviceViewListener, HttpControlViewListener,
        HttpDeviceIndexListener {

    private Device device;
    private ImageView playerView;
    private List<Bitmap> bitmaps;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
//        Bundle bundle = getIntent().getExtras();
//        if (bundle != null) {
//            device = AppContext.DEVIES.get(bundle
//                    .getInt(AppContext.DeviceNoTag));
//
//        }
        String device_id = getIntent().getStringExtra("device_id");
        Log.d("---Player_device_id:---",device_id);

        device = new Device();
        device.setDevice_id(device_id);
        device.setChancel_id(1);
        device.setToken(AppContext.TOKEN);
//        device.s
//        device.setDrate();
//        device.setDevice_rate("700");
        bitmaps = new ArrayList<>();
		/*
		 *
		 * 发送消息获取当前设备详细信息
		 */

        AppContext.capi.DeviceView(device.getDevice_id(),
                device.getChancel_id(), AppContext.TOKEN);

//        if (device.getDeviceType().equals(Device.DeviceType.Normal)) {
//            Log.d("---device_token:---",AppContext.TOKEN);
//            AppContext.capi.DeviceView(device.getDevice_id(),
//                    device.getChancel_id(), AppContext.TOKEN);
//
//        }

        device.getPlayClient().setPlayListener(this);
        device.getPlayClient().play(AppContext.USERNAME);


    }

    @Override
    public void onContentChanged() {
        super.onContentChanged();
        playerView = (ImageView) this.findViewById(R.id.player);
    }

    @Override
    protected void onResume() {
        super.onResume();
        HttpClient.instance().setHttpDeviceViewListener(this);
        HttpClient.instance().setHttpControlViewListener(this);
        HttpClient.instance().setHttpDeviceIndexListener(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        HttpClient.instance().setHttpDeviceViewListener(this);
        HttpClient.instance().setHttpControlViewListener(null);
        HttpClient.instance().setHttpDeviceIndexListener(null);
        if (device != null) {

            device.getPlayClient().stop(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (int i = 0; i < bitmaps.size(); i++) {
            if (bitmaps.get(i).isRecycled()) {
                bitmaps.get(i).recycle();
            }
        }
        bitmaps.clear();
    }

    @Override
    public void getBitmap(Bitmap bitmap, String code) {
        if (bitmaps.size() > 3) {
            Bitmap discardBitmap = bitmaps.remove(0);
            if (!discardBitmap.isRecycled()) {

                discardBitmap.recycle();
            }
        }
        bitmaps.add(bitmap);

        if (playerView != null) {
            playerView.setImageBitmap(bitmap);
        }

    }

    @Override
    public void error() {
        Toast.makeText(this, "播放失败", Toast.LENGTH_LONG).show();

    }

    @Override
    public void getPlayBackTimestamp(long timestamp) {

    }

    @Override
    public void httpDeviceViewRecall(Device device) {
        if (device != null) {
            this.device = device;

			/*
			 *
			 * 获取设备Token，打开设备，播放视频
			 */
            if (device != null) {
                Log.d("---DeviceViewRecall:",device.getDevice_name());
                device.getPlayClient().setPlayListener(this);
                device.getPlayClient().play(AppContext.USERNAME);
            }
        }

    }




    @Override
    public void httpControlViewRecall(Device device) {
        if (device != null) {
            Log.d("---ControlViewRecall:","device_id");
            device.setDeviceType(Device.DeviceType.Share);
            this.device = device;
        }
    }

    @Override
    public void httpDeviceIndexRecall(List<Device> devices) {
//        AppContext.DEVIES.clear();
//        AppContext.DEVIES.addAll(devices);
//
//        String[] strings = new String[devices.size()];
//        for (int i = 0; i < devices.size(); i++) {
//            strings[i] = devices.get(i).getDevice_name();
//        }
//        ArrayAdapter adapter = new ArrayAdapter<String>(this,
//                android.R.layout.simple_list_item_1, strings);
    }
}
