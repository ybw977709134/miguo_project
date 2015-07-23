package co.onemeter.oneapp.UUPlayer;

import android.app.Application;
import android.os.Environment;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import ulucu.api.ClientAPI;
import ulucu.api.bean.Device;
import ulucu.api.client.http.HttpClient;

/**
 * Created by hutianfeng on 15-7-21.
 */
public class AppContext extends Application{
    public static List<Device> DEVIES = new ArrayList<Device>();
    public static List<Device> PLAYBACKDEVICES = new ArrayList<Device>();
    public static String TOKEN;
    public static String USERNAME;
    public static String DEVICE_ID;
    public static final String DeviceNoTag = "DeviceNoTag";
    public static ClientAPI capi = new ClientAPI();

    @Override
    public void onCreate() {
        // TODO Auto-generated method stub
        super.onCreate();

        // NetStatus is Ok?
        HttpClient.isNetStatus = true;
		/*
		 *
		 * 创建本地文件夹，用于保存信息
		 */
        String Dir = Environment.getExternalStorageDirectory().toString();
        File dbp = new File(Dir + "/ts/");
        if (!dbp.exists()) {
            dbp.mkdirs();
        }

		/*
		 *
		 * 用于初始化网络库和解码库
		 */
        capi.StartUp(Dir + "/ts/", 0);
    }
}
