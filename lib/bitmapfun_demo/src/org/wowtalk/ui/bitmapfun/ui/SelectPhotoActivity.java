package org.wowtalk.ui.bitmapfun.ui;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.database.DataSetObserver;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import org.wowtalk.ui.bitmapfun.demo.R;
import org.wowtalk.ui.bitmapfun.util.ImageCache;
import org.wowtalk.ui.bitmapfun.util.ImageResizer;

import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: jianxd
 * Date: 3/20/13
 * Time: 2:51 PM
 * To change this template use File | Settings | File Templates.
 */
public class SelectPhotoActivity extends Activity implements View.OnClickListener {

    private class PhotoAdapter implements ListAdapter {
        private Context context;

        private GridView.LayoutParams mImageViewLayoutParams;
        public PhotoAdapter(Context context) {
            this.context = context;
            mImageViewLayoutParams = new GridView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    mImageThumbSize);
        }

        @Override
        public void registerDataSetObserver(DataSetObserver dataSetObserver) {
        }

        @Override
        public void unregisterDataSetObserver(DataSetObserver dataSetObserver) {
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

        @Override
        public boolean hasStableIds() {
            return false;  //To change body of implemented methods use File | Settings | File Templates.
        }

        View.OnClickListener listener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                assert v.getId() == R.id.img_photo;

                Integer position = (Integer)v.getTag();
                FPhoto fPhoto = listFPhoto.get(position);

                //////// test, dump current bitmap
//                try {
//                    ImageView iv = (ImageView)v;
//                    String outpath = "/sdcard/.tmp/save_onclick.jpg";
//                    FileOutputStream fos = new FileOutputStream(new File(outpath));
//                    ((BitmapDrawable) iv.getDrawable()).getBitmap().compress(Bitmap.CompressFormat.JPEG, 0, fos);
//                    fos.close();
//                    String msg = "saved #" + position + " " + fPhoto.path + " to " + outpath;
//                    Toast.makeText(SelectPhotoActivity.this, msg, Toast.LENGTH_LONG).show();
//                    Log.d("bmp", msg);
//                    Log.d("bmp", "bmp object address: " + ((Object)((BitmapDrawable) iv.getDrawable()).getBitmap()).hashCode());
//                } catch (Exception e) {
//                    e.printStackTrace();
//                }
                //////// test/

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
                imgIsSelected.setImageResource(fPhoto.isSelected ?
                        R.drawable.photo_selected : R.drawable.photo_unselected);

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
                holder.rIView = (ImageView)convertView.findViewById(R.id.img_photo);
                holder.rIView.setLayoutParams(new RelativeLayout.LayoutParams(mImageThumbSize, mImageThumbSize));
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder)convertView.getTag();
            }

            holder.rIView.setTag(position);
            holder.rIView.setOnClickListener(listener);
            holder.iView.setImageResource(fPhoto.isSelected ?
                    R.drawable.photo_selected : R.drawable.photo_unselected);

            // load thumbnail with ImageFetcher
            mImageFetcher.loadImage(fPhoto.path, holder.rIView);

            return convertView;
        }

        @Override
        public int getItemViewType(int i) {
            return 0;
        }

        @Override
        public int getViewTypeCount() {
            return 1;
        }

        @Override
        public boolean isEmpty() {
            return false;
        }

        @Override
        public boolean areAllItemsEnabled() {
            return false;
        }

        @Override
        public boolean isEnabled(int i) {
            return false;
        }

        public final class ViewHolder {
            public ImageView iView;
            public ImageView rIView;
        }
    }

    private void notifySelectTooMany() {
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

    private int mTotalPhotoNumAllowed = 900;
    private int photoNum = 0;
    private TextView titleText;
    private ImageButton btnTitleBack;
    private ImageButton btnTitleConfirm;
    private GridView photoGrid;
    private ArrayList<FPhoto> listFPhoto;
    private ArrayList<FPhoto> mSelectedPhoto;
    private ArrayList<String> listSelected;
    private Cursor cursor;

    private int mImageThumbSize;
    private int mImageThumbSpacing;
    private PhotoAdapter photoAdapter;
    private ImageResizer mImageFetcher;

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
        cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, STORE_IMAGES, null, null, "_display_name");
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
        mTotalPhotoNumAllowed = getIntent().getIntExtra("num", mTotalPhotoNumAllowed);
        setContentView(R.layout.select_photo);
        listSelected = getIntent().getStringArrayListExtra("list");
        photoNum = listSelected != null ? listSelected.size() : 0;
        final DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        mImageThumbSpacing = DensityUtil.dip2px(this, 2);
        mImageThumbSize = (getResources().getDisplayMetrics().widthPixels - mImageThumbSpacing * 5) / 4;

        ImageCache.ImageCacheParams cacheParams = new ImageCache.ImageCacheParams(this, IMAGE_CACHE_DIR);
        mImageFetcher = new ImageResizer(this, mImageThumbSize);
        mImageFetcher.setLoadingImage(R.drawable.feed_default_pic);
        mImageFetcher.addImageCache(cacheParams);
        //mImageFetcher.setImageFadeIn(false); // XXX to simplify debugging
        getPhotos();
        initView();
        notifySelectedNumChanged();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mImageFetcher.closeCache();
    }

    public void onImageClick(View v) {
        ++photoNum;
        notifySelectedNumChanged();
    }
}
