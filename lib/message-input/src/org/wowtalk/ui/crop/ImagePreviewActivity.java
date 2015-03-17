package org.wowtalk.ui.crop;

/**
 * creaeted by jacky on 2/4/2015
 * to show photo picked ,need uri
 */
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import org.wowtalk.ui.msg.R;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ImagePreviewActivity extends Activity implements OnClickListener {
	public final static String INPUTURI = "uri";
	public final static String OUTPUTPATH = "outpath";
	public final static String ISDOODLE = "doodle";
	
	private ImageView resultView;
	private Uri imagePickedUri = null;
	private Uri resultUri = null;
	private String outpath = null;
	
	private Bitmap bm = null;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        resultView = (ImageView) findViewById(R.id.result_image);
        Intent intent = getIntent();
        imagePickedUri = intent.getData();
        //android.util.Log.i("--uri--", imagePickedUri.toString());
        outpath = intent.getStringExtra(OUTPUTPATH);
        if(intent.getBooleanExtra(ISDOODLE, false)){
	        TextView txt_doneorsend = (TextView) findViewById(R.id.txt_sendordone);
	        txt_doneorsend.setText(R.string.done);
        }
        if(imagePickedUri != null){
			try {
				//避免OOM发生
				BitmapFactory.Options option = new BitmapFactory.Options();
				option.inPreferredConfig = Bitmap.Config.ARGB_8888;
				option.inPurgeable = true;
				option.inInputShareable = true;
				option.inSampleSize = 4;
				//bm = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imagePickedUri);//将uri转换bitmap
				bm = BitmapFactory.decodeStream(getContentResolver().openInputStream(imagePickedUri), null, option);
				//bm = bm.copy(Bitmap.Config.ARGB_4444, false);
	        	resultView.setImageBitmap(bm);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
        }
        findViewById(R.id.btn_crop).setOnClickListener(this);
        findViewById(R.id.btn_send).setOnClickListener(this);
        findViewById(R.id.btn_cancel).setOnClickListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent result) {
        if (requestCode == Crop.REQUEST_PICK && resultCode == RESULT_OK) {
            beginCrop(result.getData());
        } else if (requestCode == Crop.REQUEST_CROP) {
            handleCrop(resultCode, result);
        }
    }

    private void beginCrop(Uri source) {
    	int ran = new Random().nextInt(100);
    	File file = new File(getCacheDir(), "cropped" + ran);
    	if(file.exists()){
    		file.delete();
    	}
        Uri outputUri = Uri.fromFile(file);
        new Crop(source).output(outputUri).start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
        	resultUri = Crop.getOutput(result);
            resultView.setImageURI(resultUri);
            if(resultUri != null){
                try {
                    //避免OOM发生
                    BitmapFactory.Options option = new BitmapFactory.Options();
                    option.inPreferredConfig = Bitmap.Config.ARGB_8888;
                    option.inPurgeable = true;
                    option.inInputShareable = true;
                    option.inSampleSize = 4;
                    bm = BitmapFactory.decodeStream(getContentResolver().openInputStream(resultUri), null, option);
                    resultView.setImageBitmap(bm);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            }
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
    	super.onDestroy();
    	if(bm != null && !bm.isRecycled()){
    		bm.recycle();
    		System.gc();
    	}
    }
    
	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(id == R.id.btn_send){
			 if (outpath != null ) {
	                Bitmap outBmp = null;
                    InputStream is = null;
	                try {
	                	Uri returnUri = imagePickedUri;
						if(resultUri != null){
							//outBmp = MediaStore.Images.Media.getBitmap(getContentResolver(), resultUri);
                            is = getContentResolver().openInputStream(resultUri);
							outBmp = BitmapFactory.decodeStream(is);
							returnUri = resultUri;
		                }else if(imagePickedUri != null){
		                	//outBmp = MediaStore.Images.Media.getBitmap(getContentResolver(), imagePickedUri);
                            is = getContentResolver().openInputStream(imagePickedUri);
							outBmp = BitmapFactory.decodeStream(is);
		                }
                        if(outBmp != null) {
                            outBmp.compress(Bitmap.CompressFormat.JPEG, 90, new FileOutputStream(outpath));
                        }
						Intent data = new Intent();
						data.setData(returnUri);
		                setResult(RESULT_OK, data);
					} catch (Exception e) {
						e.printStackTrace();
					}finally{
                        if(outBmp != null && !outBmp.isRecycled()){
                             outBmp.recycle();
                        }
                        if(is != null){
                            try {
                                is.close();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
		                
	            } else {
	                setResult(RESULT_CANCELED, null);
	            }
				finish();
		}else if(id == R.id.btn_cancel){
			finish();
		}else if(id == R.id.btn_crop){
			if(imagePickedUri != null){
					beginCrop(imagePickedUri);
			}
		}
	}
	
}
