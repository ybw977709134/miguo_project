package com.example.panzy.sockettester;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import org.wowtalk.api.NetworkIFDelegate;
import org.wowtalk.api.OssClient;

import javax.net.SocketFactory;
import java.io.*;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Calendar;


public class MainActivity extends Activity implements NetworkIFDelegate {

    private static final String TAG = "SocketTester";

    TextView logView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        logView = (TextView)findViewById(R.id.txt_log);
        logView.setText("");

        log("Android " + Build.VERSION.RELEASE);
        log("");

        oss_upload();
    }

    private void oss_upload() {
        final String dataFilename = "dummy_data.txt";
        final int contentLength = 60 * 1024 * 1024;
        final byte[] timestampBytes = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ")
                .format(Calendar.getInstance().getTime()) + "\n").getBytes();

        try {
            FileOutputStream fos = openFileOutput(dataFilename, Context.MODE_PRIVATE);
            fos.write(timestampBytes);

            byte[] piece = new byte[1024];
            for (int i = 0; i < 8; ++i) {
                for (int j = 0; j < 127; ++j)
                    piece[i] = '0';
                piece[127] = '\n';
            }

            for (int i = 0; i < contentLength / 1024; ++i) {
                fos.write(piece);
            }
            fos.close();
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        catch (IOException e) {
            e.printStackTrace();
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                log("will post " + (contentLength + timestampBytes.length) + " bytes");
                log("");

                new OssClient("u9HhKKGaN779cDQQ", "E67LFFrjqeSQ4FKmVFhXgpACUrSgEf", "om-im-dev01")
                        .setRemoteDir("test/")
                        .setCallback(MainActivity.this, 0)
                        .upload("oss_client_test.txt", getFilesDir() + "/" + dataFilename);
            }
        }).start();
    }

    private void socket_http_post() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                int contentLength = 50 * 1024 * 1024;
                Socket socket = null;
                OutputStream outs = null;
                InputStream ins = null;

                try {
                    socket = SocketFactory.getDefault().createSocket("dev01-websrv.onemeter.co", 80);
                    outs = socket.getOutputStream();
                    ins = socket.getInputStream();
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                if (outs == null)
                    return;

                log("output stream: " + outs.getClass().getCanonicalName());
                log("");
                log("will post " + contentLength + " bytes");
                log("");

                try {
                    outs.write("POST /om_im_api.php HTTP/1.1\r\n".getBytes());
                    outs.write("Host: dev01-websrv.onemeter.co\r\n".getBytes());
                    outs.write(("Content-Length: " + contentLength + "\r\n").getBytes());
                    outs.write(("\r\n").getBytes());

                    byte[] piece = new byte[1024];
                    for (int n = contentLength, i = 0; n > 0; n -= piece.length) {
                        outs.write(piece, 0, Math.min(piece.length, n));
                        if (++i % (contentLength / piece.length / 100) == 1)
                            log(">>> " + (contentLength - n));
                    }
                    log(">>> " + contentLength, true);

                    BufferedReader r = new BufferedReader(new InputStreamReader(ins, "UTF-8"));

                    log("");
                    while (true) {
                        String line = r.readLine();
                        if (line == null)
                            break;
                        log("<<< " + line);
                    }

                    socket.close();
                }
                catch (IOException e) {
                    e.printStackTrace();
                    log("\n" + e.getClass().getCanonicalName() + ": " + e.getMessage());
                }
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    void log(final String msg) {
        log(msg, true);
    }

    void log(final String msg, final boolean printOnScreen) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (printOnScreen)
                    logView.append(msg + "\n");
                Log.i(TAG, msg);
            }
        });
    }

    @Override
    public void setProgress(int callbackTag, int newProgress) {
        log(">>> " + newProgress + "%");
    }

    @Override
    public void didFinishNetworkIFCommunication(int callbackTag, byte[] bytes) {
        log("upload done, " + new String(bytes));
    }

    @Override
    public void didFailNetworkIFCommunication(int callbackTag, byte[] bytes) {
        log("upload failed, " + new String(bytes));
    }
}
