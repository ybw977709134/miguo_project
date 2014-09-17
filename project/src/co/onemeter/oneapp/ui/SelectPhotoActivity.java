package co.onemeter.oneapp.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.umeng.analytics.MobclickAgent;
import org.wowtalk.ui.MessageBox;
import org.wowtalk.ui.bitmapfun.ui.RecyclingImageView;
import org.wowtalk.ui.bitmapfun.util.ImageCache;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;
import org.wowtalk.ui.msg.BmpUtils;
import co.onemeter.oneapp.R;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 3/20/13
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class SelectPhotoActivity extends Activity implements View.OnClickListener {

    private class PhotoAdapter extends BaseAdapter {
        private Context context;

        private RelativeLayout.LayoutParams mImageViewLayoutParams;
        public PhotoAdapter(Context context) {
            this.context = context;
            mImageViewLayoutParams = new RelativeLayout.LayoutParams(mImageThumbSize, mImageThumbSize);
        }
        @Override
        public int getCount() {
            return listFPhoto.size();
        }

        @Override
        public Object getItem(int position) {
            return listFPhoto.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assert v.getId() == R.id.img_photo;

                Integer position = (Integer)v.getTag();
                FPhoto fPhoto = listFPhoto.get(position);

                if (fPhoto.isSelected) {
                    fPhoto.isSelected = false;
                    photoNum--;
                } else {
                    if (photoNum < mTotalPhotoNumAllowed) {
                        fPhoto.isSelected = true;
                        photoNum++;
                    } else {
                        notifySelectTooMany();
                    }
                }

                // toggle selection mark
                //photoAdapter.notifyDataSetChanged();
                ImageView imgIsSelected = (ImageView)((View)v.getParent()).findViewById(R.id.img_isSelected);
//                imgIsSelected.setImageResource(fPhoto.isSelected ?
//                        R.drawable.photo_selected : R.drawable.photo_unselected);
                if(fPhoto.isSelected) {
                    imgIsSelected.setVisibility(View.VISIBLE);
                    imgIsSelected.setImageResource(R.drawable.photo_selected);
                } else {
                    imgIsSelected.setVisibility(View.INVISIBLE);
                }

                notifySelectedNumChanged();
            }
        };

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            FPhoto fPhoto = listFPhoto.get(position);
            ViewHolder holder;

            if (convertView == null) {
                convertView = LayoutInflater.from(context).inflate(R.layout.listitem_select_photo, parent,false);
                holder = new ViewHolder();
                holder.iView = (ImageView)convertView.findViewById(R.id.img_isSelected);
                holder.rIView = (RecyclingImageView)convertView.findViewById(R.id.img_photo);

                /**
                 * 设置单元格的尺寸：width = height = screen_width / 4
                 *
                 * 不要调用 convertView.setLayoutParams()，否则会遇到这样的现象：个别 cell 的 onClick 不会立即触发，而是堆积起来，
                 * 等到其它某个 cell 被 click 、并且相应的 onClick() 调用完成后，那些堆积起来的 onClick() 才会依次执行。
                 * 除非禁用 convertView。
                 */
                holder.rIView.setLayoutParams(mImageViewLayoutParams);

                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            holder.rIView.setTag(position);
            holder.rIView.setOnClickListener(listener);
            if(fPhoto.isSelected) {
                holder.iView.setVisibility(View.VISIBLE);
                holder.iView.setImageResource(R.drawable.photo_selected);
            } else {
                holder.iView.setVisibility(View.INVISIBLE);
            }

            // load thumbnail with ImageFetcher
            mImageFetcher.loadImage(fPhoto.path, holder.rIView);

            return convertView;
        }

        public final class ViewHolder {
            public ImageView iView;
            public RecyclingImageView rIView;
        }
    }

    private void notifySelectTooMany() {
        mMsgBox.show(null, String.format(getString(R.string.moments_create_too_many_photo), mTotalPhotoNumAllowed));
    }

    private class FPhoto {
        String path;
        boolean isSelected = false;
    }

    private static final String[] STORE_IMAGES = {
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.LATITUDE,
            MediaStore.Images.Media.LONGITUDE,
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DATA
    };
    private static final String IMAGE_CACHE_DIR = "thumbs";

    private int mTotalPhotoNumAllowed = 9;
    private int photoNum = 0;
    private TextView titleText;
    private ImageButton btnTitleBack;
    private ImageButton btnTitleConfirm;
    private GridView photoGrid;
    private ArrayList<FPhoto> listFPhoto;
    private ArrayList<String> listSelected;
    private Cursor cursor;

    private int mImageThumbSize;
    private int mImageThumbSpacing;
    private PhotoAdapter photoAdapter;
    private ImageResizer mImageFetcher;
    private MessageBox mMsgBox;

    private void initView() {
        titleText = (TextView) findViewById(R.id.title_text);
        btnTitleBack = (ImageButton) findViewById(R.id.title_back);
        btnTitleConfirm = (ImageButton) findViewById(R.id.title_confirm);
        photoGrid = (GridView) findViewById(R.id.photo_grid);

        btnTitleBack.setOnClickListener(this);
        btnTitleConfirm.setOnClickListener(this);
        photoGrid.setColumnWidth(mImageThumbSize);
        photoGrid.setHorizontalSpacing(mImageThumbSpacing);
        photoGrid.setVerticalSpacing(mImageThumbSpacing);
        photoAdapter = new PhotoAdapter(this);
        photoGrid.setAdapter(photoAdapter);
        photoGrid.setOnScrollListener(new AbsListView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                if (scrollState == AbsListView.OnScrollListener.SCROLL_STATE_FLING) {
                    mImageFetcher.setPauseWork(true);
                } else {
                    mImageFetcher.setPauseWork(false);
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
            }
        });
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.title_back:
                finish();
                break;
            case R.id.title_confirm:
                Intent intent = new Intent(SelectPhotoActivity.this, CreateMomentActivity.class);
                ArrayList<String> listPath = new ArrayList<String>();
                for (FPhoto photo : listFPhoto) {
                    if (!photo.isSelected)
                        continue;
                    listPath.add(photo.path);
                }
                intent.putStringArrayListExtra("list", listPath);
                setResult(RESULT_OK, intent);
                finish();
                break;
            default:
                break;
        }

    }

    private void notifySelectedNumChanged() {
        titleText.setText(String.format("%s/%s", photoNum, mTotalPhotoNumAllowed));
    }

    private void getPhotos() {
        listFPhoto = new ArrayList<FPhoto>();
        ContentResolver contentResolver = getContentResolver();
        cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, STORE_IMAGES, null, null, "date_modified desc");
        if (cursor.moveToFirst()) {
            do {
                FPhoto fPhoto = new FPhoto();
                fPhoto.path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                fPhoto.isSelected = false;
                if (listSelected != null && listSelected.contains(fPhoto.path)) {
                    fPhoto.isSelected = true;
                }
                listFPhoto.add(fPhoto);
            } while(cursor.moveToNext());
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // fix problem on displaying gradient bmp
        getWindow().setFormat(android.graphics.PixelFormat.RGBA_8888);

        mMsgBox = new MessageBox(this);
        mTotalPhotoNumAllowed = getIntent().getIntExtra("num", mTotalPhotoNumAllowed);
        setContentView(R.layout.select_photo);
        listSelected = getIntent().getStringArrayListExtra("list");
        photoNum = listSelected.size();
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        mImageThumbSpacing = DensityUtil.dip2px(this, 2);
        mImageThumbSize = (org.wowtalk.ui.GlobalValue.screenW - mImageThumbSpacing * 5) / 4;

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        mImageFetcher = new ImageResizer(this, mImageThumbSize);
        mImageFetcher.setLoadingImage(R.drawable.feed_default_pic);
        mImageFetcher.addImageCache(cacheParams);
        getPhotos();
        initView();
        notifySelectedNumChanged();
    }

    @Override
    public void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
        mImageFetcher.setExitTasksEarly(false);
    }

    @Override
    public void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
        mImageFetcher.setPauseWork(false);
        mImageFetcher.setExitTasksEarly(true);
        mImageFetcher.flushCache();
        mImageFetcher.clearCacheInMem();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();

        BmpUtils.trigerGC();
    }

}
