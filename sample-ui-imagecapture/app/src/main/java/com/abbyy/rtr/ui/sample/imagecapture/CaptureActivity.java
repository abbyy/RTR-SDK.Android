// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.widget.Toast;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.uicomponents.CaptureView;
import com.abbyy.mobile.uicomponents.scenario.ImageCaptureScenario;

/** This activity uses UI Component to capture an image **/
public class CaptureActivity extends AppCompatActivity implements ImageCaptureScenario.Callback {

	// Capture view component
	private CaptureView captureView;
	private ImageCaptureScenario imageCaptureScenario;

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_capture );

		// The 'ABBYY MI SDK Engine' to be used in this sample application
		Engine engine = EngineFactory.createEngine( this );

		if( engine != null ) {
			// Engine is required for the scenario
			captureView = findViewById( R.id.captureView );
			imageCaptureScenario = new ImageCaptureScenario( engine );
			imageCaptureScenario.setCallback( this );
			// Image capture scenario is created in STARTED STATE by default,
			// therefore there is no need to call imageCaptureScenario.start() for one image capture.

			captureView.setCaptureScenario( imageCaptureScenario );
		}
	}

	@Override
	protected void onResume()
	{
		super.onResume();
		captureView.startCamera();
	}

	@Override
	protected void onPause()
	{
		captureView.stopCamera();
		super.onPause();
	}

	// This is a callback from UI Component on successful image capture
	@Override
	public void onImageCaptured( @NonNull ImageCaptureScenario.Result documentCaptureResult )
	{
		ImageHolder.setImage( documentCaptureResult.getBitmap() );
		finish();
		// If you need to start capture for next image after onImageCaptured,
		// you should call imageCaptureScenario.start()
	}

	// Error handler for UI Component
	@Override
	public void onError( @NonNull Exception error )
	{
		String errorMessage;
		if( error.getMessage() != null ) {
			errorMessage = error.getMessage();
		} else {
			errorMessage = getString( R.string.unknown_error );
		}
		Toast.makeText( this, errorMessage, Toast.LENGTH_SHORT ).show();
	}

}
