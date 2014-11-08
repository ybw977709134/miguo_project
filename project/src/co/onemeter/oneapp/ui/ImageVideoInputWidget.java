package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import org.wowtalk.api.WFile;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.msg.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 允许用户输入多个图片和视频的小部件。
 * Created by pzy on 11/8/14.
 */
public class ImageVideoInputWidget extends HorizontalScrollView {

    // key for saving instance state of MediaInputHelper.
    private static final String INSTANCE_STATE_KEY_MEDIA_INPUT_HELPER = "465d472ae381d";
    private static final int VIEW_TAG_FILEID = 1;

    private static final int PHOTO_SEND_WIDTH = 600;
    private static final int PHOTO_SEND_HEIGHT = 600;
    private static final int PHOTO_THUMB_WIDTH = 200;
    private static final int PHOTO_THUMB_HEIGHT = 200;

    private LinearLayout gridContainer;
    private ImageButton dummyGrid;

    private Activity activity;
    private MediaInputHelper inputHelper;
    private MediaType mediaType = MediaType.Photo;
    private int requestCode;
    private ArrayList<WFile> files = new ArrayList<WFile>();
    private HashMap<String, ImageView> grids = new HashMap<String, ImageView>();

    public enum MediaType {
        Photo,
        Video
    }

    public ImageVideoInputWidget(Context context) {
        this(context, null, 0);
    }

    public ImageVideoInputWidget(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public ImageVideoInputWidget(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        initUi(context, attrs, defStyle);
    }

    public void setup(Activity activity, MediaType mediaType, int requestCode) {
        this.activity = activity;
        this.mediaType = mediaType;
        this.requestCode = requestCode;
    }

    private void initUi(Context context, AttributeSet attrs, int defStyle) {
        gridContainer = new LinearLayout(context, attrs, defStyle);
        gridContainer.setLayoutParams(new LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        addView(gridContainer);

        dummyGrid = new ImageButton(context, attrs, defStyle);
        MarginLayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );
        lp.rightMargin = (int) getResources().getDimension(R.dimen.padding_small);
        dummyGrid.setLayoutParams(lp);
        dummyGrid.setImageResource(R.drawable.share_new_add_photo);
        dummyGrid.setBackgroundResource(0);
        dummyGrid.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                addItem();
            }
        });
        gridContainer.addView(dummyGrid);
    }

    // Interactively Add new photo or video.
    public void addItem() {
        if (inputHelper == null) {
            inputHelper = new MediaInputHelper();
        }
        if (mediaType == MediaType.Photo) {
            inputHelper.inputImage(activity, requestCode, null);
        } else {
            inputHelper.inputVideo(activity, requestCode);
        }
    }

    public void addItem(WFile f) {
        files.add(f);
        appendThumbnail(f);
    }

    public void removeItem(WFile f) {
        files.remove(f);
        ImageView v = grids.get(f.fileid);
        if (v != null) {
            gridContainer.removeView(v);
        }
    }

    public int getItemCount() {
        return files.size();
    }

    public WFile getItem(int position) {
        return files.get(position);
    }

    /**
     * 请在 {@link android.app.Activity#onSaveInstanceState(android.os.Bundle)} 中调用。
     * @param outState
     */
    public void saveInstanceState(Bundle outState) {
        outState.putParcelable(INSTANCE_STATE_KEY_MEDIA_INPUT_HELPER, inputHelper);
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        inputHelper = savedInstanceState.getParcelable(INSTANCE_STATE_KEY_MEDIA_INPUT_HELPER);
    }

    /**
     * 请在 {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * 中调用。
     * @param requestCode
     * @param resultCode
     * @param data
     * @return true: consumed.
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == this.requestCode) {
            if (mediaType == MediaType.Photo) {
                String[] path = new String[2];
                if (inputHelper.handleImageResult(
                        activity,
                        data,
                        PHOTO_SEND_WIDTH, PHOTO_SEND_WIDTH,
                        PHOTO_THUMB_WIDTH, PHOTO_THUMB_HEIGHT,
                        path)) {
                    WFile f = new WFile(
                            FileUtils.getExt(path[0]),
                            String.valueOf(Math.random()),
                            String.valueOf(Math.random()),
                            path[0]
                    );
                    f.localThumbnailPath = path[1];
                    addItem(f);
                } else {
                    Toast.makeText(activity, R.string.operation_failed, Toast.LENGTH_SHORT).show();
                }
                return true;
            } else if (mediaType == MediaType.Video) {
                return true;
            }
        }

        return false;
    }

    private void appendThumbnail(final WFile f) {
        ImageView v = new ImageView(getContext());
        v.setImageBitmap(BitmapFactory.decodeFile(f.localThumbnailPath));

        // set the same size as dummy icon
        Rect rect = dummyGrid.getDrawable().getBounds();
        MarginLayoutParams lp = new LinearLayout.LayoutParams(
                rect.width(),
                rect.height()
        );
        lp.rightMargin = ((MarginLayoutParams)dummyGrid.getLayoutParams()).rightMargin;
        v.setLayoutParams(lp);
        v.setScaleType(ImageView.ScaleType.CENTER_CROP);
        int n = gridContainer.getChildCount();

        // click to remove
        v.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                removeItem(f);
            }
        });

        gridContainer.addView(v, n - 1);
        grids.put(f.fileid, v);
        scrollBy(0, lp.width + lp.rightMargin); // TODO not work
    }
}
