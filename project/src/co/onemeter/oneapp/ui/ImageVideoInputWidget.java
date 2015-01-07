package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import co.onemeter.oneapp.R;
import co.onemeter.oneapp.utils.ThemeHelper;
import org.wowtalk.api.WFile;
import org.wowtalk.ui.MediaInputHelper;
import org.wowtalk.ui.msg.FileUtils;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * 允许用户输入多个图片和视频的小部件。用法：
 * <ul>
 *     <li>{@link #setup(android.app.Activity, co.onemeter.oneapp.ui.ImageVideoInputWidget.MediaType, int)}</li>
 *     <li>in {@link Activity#onSaveInstanceState(android.os.Bundle)} call {@link #saveInstanceState(android.os.Bundle)}</li>
 *     <li>in {@link Activity#onCreate(android.os.Bundle)} call {@link #restoreInstanceState(android.os.Bundle)}</li>
 *     <li>in {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)} ALWAYS call
 *     {@link #handleActivityResult(int, int, android.content.Intent)} (even if resultCode != RESULT_OK)</li>
 * </ul>
 *
 * Created by pzy on 11/8/14.
 *
 * TODO
 *  # 可输入 TOTAL_PHOTO_ALLOWED
 *  # 已经输入的照片数达到上限时，隐藏添加按钮
 *  # 在添加按钮旁边显示提示语，比如“添加照片”
 *  # 过滤重复选择的照片
 *  # 从相册选择的照片，要按照 PHOTO_SEND_WIDTH/HEIGHT 尺寸压缩
 */
public class ImageVideoInputWidget extends HorizontalScrollView {

    private static final String TAG = "ImageVideoInputWidget";

    // key for saving instance state of MediaInputHelper.
    private static final String INSTANCE_STATE_KEY_MEDIA_INPUT_HELPER = "465d472ae381d_mihelper";
    private static final String INSTANCE_STATE_KEY_MEDIA_TYPE = "465d472ae381d_mtype";
    private static final String INSTANCE_STATE_KEY_FILES = "465d472ae381d_files";
    private static final String INSTANCE_STATE_KEY_GRID_SIZE = "465d472ae381d_gridsize";
    private static final String INSTANCE_STATE_KEY_REQ_TYPE = "465d472ae381d_reqtype";

    private static final int PHOTO_SEND_WIDTH = 600;
    private static final int PHOTO_SEND_HEIGHT = 600;
    private static final int PHOTO_THUMB_WIDTH = 200;
    private static final int PHOTO_THUMB_HEIGHT = 200;
    private static final int TOTAL_PHOTO_ALLOWED = 9;

    private LinearLayout gridContainer;
    private ImageButton dummyGrid;

    private Activity activity;
    private MediaInputHelper inputHelper;
    private MediaType mediaType = MediaType.Photo;
    private int requestCode;
    private RequestType requestType = RequestType.Unknown;
    private boolean activityRequestInProgress = false;
    private ArrayList<WFile> files = new ArrayList<>();
    private HashMap<String, ImageView> grids = new HashMap<>();
    private int gridSize;

    public enum MediaType {
        Photo,
        Video
    }

    /**
     * 由于一个 {@link #requestCode} 会用于不同的 intent，需要 RequestType 在
     * {@link #handleActivityResult} 中帮助区分。
     */
    private enum RequestType {
        TakePhoto,
        PickPhoto,
        Unknown
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

        determineGridSize();
    }

    /**
     * 处理“添加图片”按钮的点击事件。
     * <p>缺省行为是弹出列表对话框，供用户在“拍摄”和“相册”之间选择。
     * 若想定制菜单，可以重写该方法，在自定义的菜单事件中调用
     * {@link #takePhoto} 或 {@link #pickPhoto}。</p>
     */
    public void onAddPhotoPressed() {
        new AlertDialog.Builder(activity)
                .setItems(R.array.image_video_input_widget_dlg_items_0_take_1_pick,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int position) {
                                if (inputHelper == null) {
                                    inputHelper = new MediaInputHelper();
                                }
                                if (position == 0) {
                                    takePhoto();
                                } else if (position == 1) {
                                    pickPhoto();
                                }
                            }
                        })
                .create().show();
    }

    public void pickPhoto() {
        requestType = RequestType.PickPhoto;
        Intent pickerIntent = new Intent(activity, SelectPhotoActivity.class);
        pickerIntent.putExtra("num", TOTAL_PHOTO_ALLOWED - files.size());
        ThemeHelper.putExtraCurrThemeResId(pickerIntent, activity);
        activity.startActivityForResult(pickerIntent, requestCode);
    }

    public void takePhoto() {
        requestType = RequestType.TakePhoto;
        inputHelper.takePhoto(activity, requestCode);
    }

    /**
     * 处理“添加”按钮的点击事件。
     */
    private void addItem() {
        if (activityRequestInProgress) {
            Log.d(TAG, " addItem cancelled since a former request is still in progress.");
            return;
        }

        if (inputHelper == null) {
            inputHelper = new MediaInputHelper();
        }
        if (mediaType == MediaType.Photo) {
            onAddPhotoPressed();
        } else {
            inputHelper.inputVideo(activity, requestCode);
        }
        activityRequestInProgress = true;
    }

    private void addItem(WFile f) {
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
        outState.putString(INSTANCE_STATE_KEY_MEDIA_TYPE, mediaType.name());
        outState.putParcelableArrayList(INSTANCE_STATE_KEY_FILES, files);
        outState.putInt(INSTANCE_STATE_KEY_GRID_SIZE, gridSize);
        outState.putString(INSTANCE_STATE_KEY_REQ_TYPE, requestType.name());
    }

    public void restoreInstanceState(Bundle savedInstanceState) {
        inputHelper = savedInstanceState.getParcelable(INSTANCE_STATE_KEY_MEDIA_INPUT_HELPER);
        mediaType = MediaType.valueOf(savedInstanceState.getString(INSTANCE_STATE_KEY_MEDIA_TYPE));
        files = savedInstanceState.getParcelableArrayList(INSTANCE_STATE_KEY_FILES);
        gridSize = savedInstanceState.getInt(INSTANCE_STATE_KEY_GRID_SIZE, 0);
        for (WFile f : files) {
            appendThumbnail(f);
        }
        requestType = RequestType.valueOf(savedInstanceState.getString(INSTANCE_STATE_KEY_REQ_TYPE));
    }

    /**
     * 请在 {@link android.app.Activity#onActivityResult(int, int, android.content.Intent)}
     * 中调用。
     * <p>注意：即时 resultCode != RESULT_OK，也要调用该方法。</p>
     * @param requestCode
     * @param resultCode
     * @param data
     * @return true: consumed.
     */
    public boolean handleActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == this.requestCode) {
            Log.d(TAG, " activity request done");
            activityRequestInProgress = false;
        }

        if (resultCode == Activity.RESULT_OK && requestCode == this.requestCode) {
            if (mediaType == MediaType.Photo) {
                if (requestType == RequestType.TakePhoto) { // delegated to MediaInputHelper
                    String[] path = new String[2];
                    if (inputHelper.handleImageResult(
                            activity,
                            data,
                            PHOTO_SEND_WIDTH, PHOTO_SEND_HEIGHT,
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
                } else if (requestType == RequestType.PickPhoto) { // delegated to SelectPhotoActivity
                    ArrayList<String> listPath = data.getStringArrayListExtra("list");
                    for (String path : listPath) {
                        WFile f = new WFile(
                                FileUtils.getExt(path),
                                String.valueOf(Math.random()),
                                String.valueOf(Math.random()),
                                path
                        );

                        f.localThumbnailPath = MediaInputHelper.generateThumbnailForImage(
                                path, PHOTO_THUMB_WIDTH, PHOTO_THUMB_HEIGHT);
                        addItem(f);
                    }
                }
            } else if (mediaType == MediaType.Video) {
                // TODO not implemented
            }
        }

        return (requestCode == this.requestCode);
    }

    private void appendThumbnail(final WFile f) {
        ImageView v = new ImageView(getContext());
        v.setImageBitmap(BitmapFactory.decodeFile(f.localThumbnailPath));

        MarginLayoutParams lp = new LinearLayout.LayoutParams(gridSize, gridSize);
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

    // set the same size as dummy icon
    private void determineGridSize() {
        if (gridSize == 0) {
            gridSize = dummyGrid.getDrawable().getIntrinsicWidth();
        }
        if (gridSize == 0)
            gridSize = 120;
    }
}
