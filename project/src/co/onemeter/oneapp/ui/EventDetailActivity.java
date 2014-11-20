package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import co.onemeter.oneapp.R;
import com.androidquery.AQuery;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.api.NetworkIFDelegate;
import org.wowtalk.api.WEvent;
import org.wowtalk.api.WFile;
import org.wowtalk.api.WowTalkWebServerIF;
import org.wowtalk.ui.GlobalValue;
import org.wowtalk.ui.ImageViewActivity;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

public class EventDetailActivity extends Activity implements OnClickListener {
    private static final int NUM_COLUMNS = 4;

    public static final String INTENT_EXTRA_SIGNUP = "intent_extra_signup";
	
	private ImageButton btnTitleBack;
	private TextView txtDetailIntroduce;
	private WEvent eventDetail;

    private void initView() {
		initGallery();

		btnTitleBack = (ImageButton) findViewById(R.id.title_back);
		txtDetailIntroduce = (TextView) findViewById(R.id.detail_introduce);

		btnTitleBack.setOnClickListener(this);

        AQuery q = new AQuery(this);
        WEventUiHelper helper = new WEventUiHelper(this);

        // title
        q.find(R.id.event_title).text(eventDetail.title);

        // time
        q.find(R.id.event_time).text(helper.formatField(
                getResources().getString(R.string.event_time_label),
                new SimpleDateFormat("MM月dd日 HH:mm").format(eventDetail.startTime)
                        + "-"
                        + new SimpleDateFormat("HH:mm").format(eventDetail.endTime)));

        // place
        q.find(R.id.event_place).text(helper.formatField(
                getResources().getString(R.string.event_place_label),
                eventDetail.address));

        // member count
        q.find(R.id.event_count).text(helper.formatField(
                getResources().getString(R.string.event_member_count_label),
                String.format(getResources().getString(R.string.event_member_count_value), eventDetail.joinedMemberCount),
                getResources().getColor(R.color.text_gray3),
                getResources().getColor(R.color.text_red)));

        // cost
        q.find(R.id.event_cost).text(helper.formatField(
                getResources().getString(R.string.event_cost_label),
                String.format(getResources().getString(R.string.event_cost_value), eventDetail.costGolds),
                getResources().getColor(R.color.text_gray3),
                getResources().getColor(R.color.text_red)));

        // category
        q.find(R.id.event_category).text(helper.formatField(
                getString(R.string.event_category_label),
                WEventUiHelper.getEventCatetoryText(getBaseContext(), eventDetail.category)));

        // host
        q.find(R.id.event_host).text(helper.formatField(
                getString(R.string.event_host_label),
                eventDetail.host));

        // tel
        q.find(R.id.event_tel).text(helper.formatField(
                getString(R.string.event_tel_label),
                eventDetail.contactEmail));

        txtDetailIntroduce.setText(eventDetail.description);
	}

    private void downloadImage(final WFile aFile, final boolean thumbnail, final ImageView imageView) {

        new AsyncTask<Void, Integer, Void> () {
            boolean ok = true;

            @Override
            protected Void doInBackground(Void... arg0) {
                WowTalkWebServerIF.getInstance(EventDetailActivity.this)
                        .fGetFileFromServer(thumbnail ? aFile.thumb_fileid : aFile.fileid,
                                WEvent.MEDIA_FILE_REMOTE_DIR, new NetworkIFDelegate(){
                                    @Override
                                    public void didFailNetworkIFCommunication(int arg0, byte[] arg1) {
                                        ok = false;
                                    }
                                    @Override
                                    public void didFinishNetworkIFCommunication(int arg0, byte[] arg1) {
                                        ok = true;
                                    }
                                    @Override
                                    public void setProgress(int arg0, int arg1) {
                                    }

                                },
                                0,
                                thumbnail ? aFile.localThumbnailPath : aFile.localPath,
                                null);
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
                if(ok) {
                    displayImage(thumbnail ? aFile.localThumbnailPath : aFile.localPath, imageView);
                }
            }

        }.execute((Void)null);
    }

    private boolean isFileAPhoto(String fileName) {
        return fileName.endsWith(".jpg") ||
                fileName.endsWith(".jpeg") ||
                fileName.endsWith(".tif") ||
                fileName.endsWith(".png");
    }

	private void initGallery() {
		ViewGroup gallery = (ViewGroup) findViewById(R.id.images_grid);

        int numImages = 0;

        if(null != eventDetail.multimedias && !eventDetail.multimedias.isEmpty()) {
            ArrayList<String> eventImageList=new ArrayList<String>();

            LinearLayout row = addImageRow(gallery);
            for(WFile aFile : eventDetail.multimedias) {
                String filename = aFile.localThumbnailPath;
                if (!isFileAPhoto(filename))
                    continue;

                // display big image
                if (numImages == 0) {
                    ImageView imageView = (ImageView)findViewById(R.id.image_cover);
                    if (!displayImage(aFile.localPath, imageView)) {
                        downloadImage(aFile, false, imageView);
                    }
                }
                // else
                {
                    eventImageList.add(aFile.localThumbnailPath);

                    // create image view
                    ImageView imageView = addImageView(row, numImages);

                    // set image source
                    if (!displayImage(filename, imageView)) {
                        imageView.setImageResource(R.drawable.feed_default_pic);
                        downloadImage(aFile, true, imageView);
                    }
                }

                if (++numImages % NUM_COLUMNS == 0) {
                    row = addImageRow(gallery);
                }
            }
        }

        if (numImages == 0) {
            new AQuery(this).find(R.id.images_section).visibility(View.GONE);
        }
	}

    private boolean displayImage(String filename, ImageView imageView) {
        boolean ok = false;
        try {
            if (new File(filename).exists()) {
                InputStream in = new FileInputStream(filename);
                Drawable drawable = Drawable.createFromStream(in, null);
                imageView.setImageDrawable(drawable);
                ok = true;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return ok;
    }

    private ImageView addImageView(LinearLayout row, final int position) {
        int padding = 10;
        int width = (GlobalValue.screenW - (NUM_COLUMNS + 1) * padding) / NUM_COLUMNS;
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(width, width);
        lp.setMargins(padding, padding, position < NUM_COLUMNS - 1 ? 0 : padding, padding);

        ImageView imageView = new ImageView(this);
        imageView.setLayoutParams(lp);
        imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
        row.addView(imageView);

        imageView.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                ImageViewActivity.launch(EventDetailActivity.this,
                        position,
                        eventDetail.multimedias,
                        ImageViewActivity.UPDATE_WITH_CHAT_MESSAGE);
            }
        });
        return imageView;
    }

    private LinearLayout addImageRow(ViewGroup gallery) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        gallery.addView(row);
        return row;
    }

    @Override
	public void onClick(View v) {
        switch (v.getId()) {
            case R.id.title_back:
                finish();
                break;
            default:
                break;
        }
    }

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.event_detail);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        eventDetail = getIntent().getExtras().getParcelable(EventActivity.EVENT_DETAIL_BUNDLE);

        if (eventDetail == null)
            return;

		initView();
	}

    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }
}
