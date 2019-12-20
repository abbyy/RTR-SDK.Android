// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

	private ImageView emptyDocumentView;
	private ImageView pageView;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		emptyDocumentView = findViewById( R.id.emptyDocument );
		TextView buildNumberView = findViewById( R.id.buildNumber );
		// retrieve build number of UI Components SDK
		buildNumberView.setText( getString( R.string.build_number, com.abbyy.mobile.uicomponents.BuildConfig.VERSION_NAME ) );

		Button captureNewPages = findViewById( R.id.captureNew );
		captureNewPages.setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick( View v )
			{
				startCapture();
			}
		} );
		pageView = findViewById( R.id.pageView );
	}

	@Override protected void onResume()
	{
		super.onResume();
		loadPage();
	}

	private void loadPage()
	{
		Bitmap capturedImage = ImageHolder.getImage();
		if( capturedImage != null ) {
			emptyDocumentView.setVisibility( View.GONE );
			pageView.setImageBitmap( capturedImage );
		}
	}

	private void startCapture()
	{
		Intent intent = new Intent( MainActivity.this, CaptureActivity.class );
		startActivity( intent );
	}

}
