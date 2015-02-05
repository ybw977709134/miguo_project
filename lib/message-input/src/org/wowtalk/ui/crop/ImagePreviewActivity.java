package org.wowtalk.ui.crop;

/**
 * creaeted by jacky on 2/4/2015
 * to show photo picked ,need uri
 */
import java.io.File;

import org.wowtalk.ui.msg.R;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.Toast;

public class ImagePreviewActivity extends Activity implements OnClickListener {
	private ImageView resultView;
	public static final String INPUTURI = "uri";
	private Uri imagePickedUri = null;
	private Uri resultUri = null;
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image_preview);
        resultView = (ImageView) findViewById(R.id.result_image);
        Intent intent = getIntent();
        imagePickedUri = intent.getData();
        if(imagePickedUri != null){
        	resultView.setImageURI(imagePickedUri);
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
        Uri outputUri = Uri.fromFile(new File(getCacheDir(), "cropped"));
        new Crop(source).output(outputUri).asSquare().start(this);
    }

    private void handleCrop(int resultCode, Intent result) {
        if (resultCode == RESULT_OK) {
        	resultUri = Crop.getOutput(result);
            resultView.setImageURI(Crop.getOutput(result));
        } else if (resultCode == Crop.RESULT_ERROR) {
            Toast.makeText(this, Crop.getError(result).getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if(id == R.id.btn_send){
			if(imagePickedUri != null){
				if(resultUri != null){
					setResult(RESULT_OK, new Intent().setData(resultUri));
				}else{
					setResult(RESULT_OK, new Intent().setData(imagePickedUri));
				}
				finish();
			}
		}else if(id == R.id.btn_cancel){
			finish();
		}else if(id == R.id.btn_crop){
			if(imagePickedUri != null){
					beginCrop(imagePickedUri);
			}
		}
	}
}
