// ABBYY ® Mobile Imaging SDK II © 2018 ABBYY Production LLC.
// ABBYY is either a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.mobile.sample;

import android.app.Activity;
import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.content.pm.PackageManager;
import android.graphics.RectF;
import android.graphics.Region;
import android.media.MediaActionSound;
import android.preference.PreferenceManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.graphics.ImageFormat;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Display;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.rtr.IImageCaptureService;
import com.abbyy.mobile.rtr.IImagingCoreAPI;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {

	// Licensing
	private static final String licenseFileName = "license";

	///////////////////////////////////////////////////////////////////////////////
	// Some application settings that can be changed to modify application behavior:
	// The camera zoom. Optically zooming with a good camera often improves results
	// even at close range and it might be required at longer ranges.
	private static final int cameraZoom = 1;
	// The default behavior in this sample is to start capture when application is started or
	// resumed. You can turn off this behavior or remove it completely to simplify the application
	private static final boolean startCaptureOnAppStart = true;
	// The default behavior is to crop the captured image. You might want to show the captured image
	// with the document boundary instead
	private static final boolean cropCapturedImage = true;

	// A set of available sample document sizes
	private enum SampleDocumentSize {
		// Unknown size but require boundaries
		DocumentWithBoundaries( "Unknown size / Require boundaries", 0f, 0f, 1f ),
		// A4 paper size for office documents (ISO)
		A4( "210×297 mm (ISO A4)", 210f, 297f, 0f ),
		// Letter paper size for office documents (US Letter)
		Letter( "215.9×279.4 mm (US Letter)", 215.9f, 279.4f, 0f ),
		// International Business Card
		BusinessCard( "53.98×85.6 mm (International)", 53.98f, 85.6f, 0f ),
		// Unknown size / Unknown boundaries
		Auto( "Unknown size / Unknown boundaries", 0f, 0f, 0f );

		SampleDocumentSize( String usage, float width, float height, float minAspectRatio )
		{
			Usage = usage;
			Width = width;
			Height = height;
			MinAspectRatio = minAspectRatio;
		}

		public String Usage;
		public float Width;
		public float Height;
		public float MinAspectRatio;
	}
	// By default the document size is unknown
	private SampleDocumentSize documentSize = SampleDocumentSize.DocumentWithBoundaries;

	///////////////////////////////////////////////////////////////////////////////

	// Camera permission request code for Android 6.0 and higher
	private static final int CAMERA_PERMISSION_REQUEST_CODE = 42;

	// The 'ABBYY MI SDK Engine' and 'Image Capture Service' to be used in this sample application
	private Engine engine;
	private IImageCaptureService imageCaptureService;

	// The camera and the preview surface
	private Camera camera;
	private SurfaceViewWithOverlay surfaceViewWithOverlay;
	private SurfaceHolder previewSurfaceHolder;

	// Actual preview size and orientation
	private Camera.Size cameraPreviewSize;
	private int orientation;

	// Auxiliary variables
	private boolean inPreview = false; // Camera preview is started
	private boolean startCaptureWhenReady; // Start capture next time when ready (and reset this flag)
	private Handler handler = new Handler(); // Posting some delayed actions;
	private boolean cameraPermissionRequested = false; // Camera permission request has been sent to user (Android 6+)
	private boolean imageCaptured = false;
	private int skipFramesAfterAutofocus = 0; // We will skip some  frames after autofocus (user tapped the screen)
		// to allow the service to catch frames at the new focus.

	// UI components
	private Button startButton; // The start button
	private TextView warningTextView; // Show warnings from the service
	private TextView errorTextView; // Show errors from the service
	private Spinner documentSizeSpinner; // Document size selection

	// Text displayed on start button
	private static final String BUTTON_TEXT_START = "Start";
	private static final String BUTTON_TEXT_STOP = "Stop";
	private static final String BUTTON_TEXT_STARTING = "Starting...";

	// To communicate with the Image Capture Service we will need this callback:
	private IImageCaptureService.Callback imageCaptureCallback = new IImageCaptureService.Callback() {

		@Override
		public void onRequestLatestFrame( byte[] buffer )
		{
			// The service asks to fill the buffer with image data for the latest frame in NV21 format.
			// Delegate this task to the camera. When the buffer is filled we will receive
			// Camera.PreviewCallback.onPreviewFrame (see below)
			camera.addCallbackBuffer( buffer );
		}

		@Override
		public void onFrameProcessed( IImageCaptureService.Status status,  IImageCaptureService.Result result )
		{
			if( result != null && checkResult( result ) ) {
				playSoundEffect();
				stopCapture();

				try( IImagingCoreAPI coreAPI = engine.createImagingCoreAPI() ) {
					try( IImagingCoreAPI.Image image = coreAPI.loadImage( result.ImageBuffer, result.ImageWidth, result.ImageHeight, orientation ) ) {
						if( cropCapturedImage ) {
							if( result.DocumentBoundary != null ) {
								IImagingCoreAPI.CropOperation crop = coreAPI.createCropOperation();
								crop.DocumentBoundary = result.DocumentBoundary;
								crop.DocumentWidth = result.DocumentWidth;
								crop.DocumentHeight = result.DocumentHeight;
								crop.apply( image );
							}
							// The cropped image will be shown with a boundary drawn around the image
							surfaceViewWithOverlay.setDocumentBoundary( null );
						} else {
							surfaceViewWithOverlay.setDocumentBoundary( result.DocumentBoundary );
						}
						surfaceViewWithOverlay.setBitmap( image.toBitmap() );
					}
				}
				imageCaptured = true;
			} else if( !imageCaptured ){
				checkStatus( status );
				surfaceViewWithOverlay.setDocumentBoundary( status.DocumentBoundary );
				if( status.QualityAssessmentForOcrBlocks != null ) {
					surfaceViewWithOverlay.setQualityAssessmentBlocks( status.QualityAssessmentForOcrBlocks );
				}
			}
		}

		@Override
		public void onError( Exception e )
		{
			// An error occurred while processing. Log it. Processing will continue
			Log.e( getString( R.string.app_name ), "Error: " + e.getMessage() );
			if( BuildConfig.DEBUG ) {
				// Make the error easily visible to the developer
				String message = e.getMessage();
				if( message == null ) {
					message = "Unspecified error while creating the service. See logcat for details.";
				}
				errorTextView.setText( message );
			}
		}

		private MediaActionSound sound;

		protected void playSoundEffect()
		{
			startButton.playSoundEffect( android.view.SoundEffectConstants.CLICK );
			if( sound == null ) {
				sound = new MediaActionSound();
			}
			sound.play( MediaActionSound.SHUTTER_CLICK );
		}

		protected boolean checkResult( IImageCaptureService.Result result )
		{
			if( skipFramesAfterAutofocus > 0 ) {
				// Have just been focusing, wait for next result
				skipFramesAfterAutofocus--;
				return false;
			}
			if( documentSize.Width > 0f && documentSize.Height > 0f ) {
				// Require document boundary
				if( result.DocumentBoundary == null ) {
					warningTextView.setText( "NO DOCUMENT" );
					return false;
				}
			}
			warningTextView.setText( "" );
			return true;
		}

		protected void checkStatus( IImageCaptureService.Status status )
		{
			skipFramesAfterAutofocus--; // See field description
			if( documentSize.Width > 0f && documentSize.Height > 0f ) {
				// Require document boundary
				warningTextView.setText( status.DocumentBoundary == null ? "NO DOCUMENT" : "" );
			}
		}
	};

	// This callback will be used to obtain frames from the camera
	private Camera.PreviewCallback cameraPreviewCallback = new Camera.PreviewCallback() {
		@Override
		public void onPreviewFrame( byte[] data, Camera camera )
		{
			// The buffer that we have given to the camera in IImageCaptureService.Callback.onRequestLatestFrame
			// above have been filled. Send it back to the Image Capture Service
			imageCaptureService.submitRequestedFrame( data );
		}
	};

	// This callback is used to configure preview surface for the camera
	SurfaceHolder.Callback surfaceCallback = new SurfaceHolder.Callback() {

		@Override
		public void surfaceCreated( SurfaceHolder holder )
		{
			// When surface is created, store the holder
			previewSurfaceHolder = holder;
		}

		@Override
		public void surfaceChanged( SurfaceHolder holder, int format, int width, int height )
		{
			// When surface is changed (or created), attach it to the camera, configure camera and start preview
			if( camera != null ) {
				setCameraPreviewDisplayAndStartPreview();
			}
		}

		@Override
		public void surfaceDestroyed( SurfaceHolder holder )
		{
			// When surface is destroyed, clear previewSurfaceHolder
			previewSurfaceHolder = null;
		}
	};

	// Start capture when autofocus completes (used when continuous autofocus is not enabled)
	private Camera.AutoFocusCallback startCaptureCameraAutoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus( boolean success, Camera camera )
		{
			onAutoFocusFinished( success, camera );
			startCapture();
		}
	};

	// Simple autofocus callback
	private Camera.AutoFocusCallback simpleCameraAutoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus( boolean success, Camera camera )
		{
			onAutoFocusFinished( success, camera );
		}
	};

	// Enable 'Start' button and switching to continuous focus mode (if possible) when autofocus completes
	private Camera.AutoFocusCallback finishCameraInitialisationAutoFocusCallback = new Camera.AutoFocusCallback() {
		@Override
		public void onAutoFocus( boolean success, Camera camera )
		{
			onAutoFocusFinished( success, camera );
			startButton.setText( BUTTON_TEXT_START );
			startButton.setEnabled( true );
			if( startCaptureWhenReady ) {
				startCapture();
				startCaptureWhenReady = false;
			}
		}
	};

	// Autofocus by tap
	private View.OnClickListener clickListener = new View.OnClickListener() {
		@Override public void onClick( View v )
		{
			// if BUTTON_TEXT_STARTING autofocus is already in progress, it is incorrect to interrupt it
			if( !startButton.getText().equals( BUTTON_TEXT_STARTING ) ) {
				autoFocus( simpleCameraAutoFocusCallback );
			}
		}
	};

	private void onAutoFocusFinished( boolean success, Camera camera )
	{
		if( isContinuousVideoFocusModeEnabled( camera ) ) {
			setCameraFocusMode( Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO );
		} else {
			if( !success ) {
				autoFocus( simpleCameraAutoFocusCallback );
				return;
			}
		}
		skipFramesAfterAutofocus = 6; // Skip some frames to allow the service to catch frames at the new focus
	}

	// Start autofocus (used when continuous autofocus is disabled)
	private void autoFocus( Camera.AutoFocusCallback callback )
	{
		if( camera != null ) {
			try {
				setCameraFocusMode( Camera.Parameters.FOCUS_MODE_AUTO );
				camera.autoFocus( callback );
				skipFramesAfterAutofocus = 6; // Skip some frames to allow the service to catch frames at the new focus
			} catch( Exception e ) {
				Log.e( getString( R.string.app_name ), "Error: " + e.getMessage() );
			}
		}
	}

	// Checks that FOCUS_MODE_CONTINUOUS_VIDEO supported
	private boolean isContinuousVideoFocusModeEnabled( Camera camera )
	{
		return camera.getParameters().getSupportedFocusModes().contains( Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO );
	}

	// Sets camera focus mode and focus area
	private void setCameraFocusMode( String mode )
	{
		// Camera sees it as rotated 90 degrees, so there's some confusion with what is width and what is height)
		int width = 0;
		int height = 0;
		int halfCoordinates = 1000;
		int lengthCoordinates = 2000;
		Rect area = surfaceViewWithOverlay.getAreaOfInterest();
		switch( orientation ) {
			case 0:
			case 180:
				height = cameraPreviewSize.height;
				width = cameraPreviewSize.width;
				break;
			case 90:
			case 270:
				width = cameraPreviewSize.height;
				height = cameraPreviewSize.width;
				break;
		}

		camera.cancelAutoFocus();
		Camera.Parameters parameters = camera.getParameters();
		// Set focus and metering area equal to the area of interest. This action is essential because by defaults camera
		// focuses on the center of the frame, while the area of interest in this sample application is at the top
		List<Camera.Area> focusAreas = new ArrayList<>();
		Rect areasRect;

		switch( orientation ) {
			case 0:
				areasRect = new Rect(
					-halfCoordinates + area.left * lengthCoordinates / width,
					-halfCoordinates + area.top * lengthCoordinates / height,
					-halfCoordinates + lengthCoordinates * area.right / width,
					-halfCoordinates + lengthCoordinates * area.bottom / height
				);
				break;
			case 180:
				areasRect = new Rect(
					halfCoordinates - area.right * lengthCoordinates / width,
					halfCoordinates - area.bottom * lengthCoordinates / height,
					halfCoordinates - lengthCoordinates * area.left / width,
					halfCoordinates - lengthCoordinates * area.top / height
				);
				break;
			case 90:
				areasRect = new Rect(
					-halfCoordinates + area.top * lengthCoordinates / height,
					halfCoordinates - area.right * lengthCoordinates / width,
					-halfCoordinates + lengthCoordinates * area.bottom / height,
					halfCoordinates - lengthCoordinates * area.left / width
				);
				break;
			case 270:
				areasRect = new Rect(
					halfCoordinates - area.bottom * lengthCoordinates / height,
					-halfCoordinates + area.left * lengthCoordinates / width,
					halfCoordinates - lengthCoordinates * area.top / height,
					-halfCoordinates + lengthCoordinates * area.right / width
				);
				break;
			default:
				throw new IllegalArgumentException();
		}

		focusAreas.add( new Camera.Area( areasRect, 800 ) );
		if( parameters.getMaxNumFocusAreas() >= focusAreas.size() ) {
			parameters.setFocusAreas( focusAreas );
		}
		if( parameters.getMaxNumMeteringAreas() >= focusAreas.size() ) {
			parameters.setMeteringAreas( focusAreas );
		}

		parameters.setFocusMode( mode );

		// Commit the camera parameters
		camera.setParameters( parameters );
	}

	// Attach the camera to the surface holder, configure the camera and start preview
	private void setCameraPreviewDisplayAndStartPreview()
	{
		try {
			camera.setPreviewDisplay( previewSurfaceHolder );
		} catch( Throwable t ) {
			Log.e( getString( R.string.app_name ), "Exception in setPreviewDisplay()", t );
		}
		configureCameraAndStartPreview( camera );
	}

	// Stop preview and release the camera
	private void stopPreviewAndReleaseCamera()
	{
		if( camera != null ) {
			camera.setPreviewCallbackWithBuffer( null );
			stopPreview();
			camera.release();
			camera = null;
		}
	}

	// Stop preview if it is running
	private void stopPreview()
	{
		if( inPreview ) {
			camera.stopPreview();
			inPreview = false;
		}
	}

	// Show error on startup if any
	private void showStartupError( String message )
	{
		new AlertDialog.Builder( this )
			.setTitle( "ABBYY MI SDK" )
			.setMessage( message )
			.setIcon( android.R.drawable.ic_dialog_alert )
			.show()
			.setOnDismissListener( new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss( DialogInterface dialog )
				{
					MainActivity.this.finish();
				}
			} );
	}

	// Load ABBYY MI SDK engine and configure the image capture service
	private boolean createImageCaptureService()
	{
		// Initialize the engine and image capture service
		try {
			engine = Engine.load( this, licenseFileName );
			imageCaptureService = engine.createImageCaptureService( imageCaptureCallback );
			imageCaptureService.setDocumentSize( documentSize.Width, documentSize.Height );
			imageCaptureService.setAspectRatioMin( documentSize.MinAspectRatio );

			return true;
		} catch( java.io.IOException e ) {
			// Troubleshooting for the developer
			Log.e( getString( R.string.app_name ), "Error loading ABBYY MI SDK:", e );
			showStartupError( "Could not load some required resource files. Make sure to configure " +
				"'assets' directory in your application and specify correct 'license file name'. See logcat for details." );
		} catch( Engine.LicenseException e ) {
			// Troubleshooting for the developer
			Log.e( getString( R.string.app_name ), "Error loading ABBYY MI SDK:", e );
			showStartupError( "License not valid. Make sure you have a valid license file in the " +
				"'assets' directory and specify correct 'license file name' and 'application id'. See logcat for details." );
		} catch( Throwable e ) {
			// Troubleshooting for the developer
			Log.e( getString( R.string.app_name ), "Error loading ABBYY MI SDK:", e );
			showStartupError( "Unspecified error while loading the engine. See logcat for details." );
		}

		return false;
	}

	// Start capture
	private void startCapture()
	{
		// Do not switch off the screen while the capture service is running
		previewSurfaceHolder.setKeepScreenOn( true );
		// Get area of interest (in coordinates of preview frames)
		Rect areaOfInterest = new Rect( surfaceViewWithOverlay.getAreaOfInterest() );
		// Clear error message
		errorTextView.setText( "" );
		warningTextView.setText( "" );
		// Start the service
		imageCaptureService.start( cameraPreviewSize.width, cameraPreviewSize.height, orientation, areaOfInterest );
		// Change the text on the start button to 'Stop'
		startButton.setText( BUTTON_TEXT_STOP );
		startButton.setEnabled( true );
	}

	// Stop capture
	void stopCapture()
	{
		// Disable the 'Stop' button
		startButton.setEnabled( false );

		// Stop the service asynchronously to make application more responsive. Stopping can take some time
		// waiting for all processing threads to stop
		new AsyncTask<Void, Void, Void>() {
			protected Void doInBackground( Void... params )
			{
				imageCaptureService.stop();
				return null;
			}

			protected void onPostExecute( Void result )
			{
				if( previewSurfaceHolder != null ) {
					// Restore normal power saving behaviour
					previewSurfaceHolder.setKeepScreenOn( false );
				}
				// Change the text on the stop button back to 'Start'
				startButton.setText( BUTTON_TEXT_START );
				startButton.setEnabled( true );
			}
		}.execute();
	}

	// Clear capture results
	private void clearCaptureResults()
	{
		surfaceViewWithOverlay.setDocumentBoundary( null );
		surfaceViewWithOverlay.setBitmap( null );
		surfaceViewWithOverlay.setQualityAssessmentBlocks( null );
		warningTextView.setText( "" );
		imageCaptured = false;
	}

	// Returns orientation of camera
	private int getCameraOrientation()
	{
		Display display = getWindowManager().getDefaultDisplay();
		int orientation = 0;
		switch( display.getRotation() ) {
			case Surface.ROTATION_0:
				orientation = 0;
				break;
			case Surface.ROTATION_90:
				orientation = 90;
				break;
			case Surface.ROTATION_180:
				orientation = 180;
				break;
			case Surface.ROTATION_270:
				orientation = 270;
				break;
		}
		for( int i = 0; i < Camera.getNumberOfCameras(); i++ ) {
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo( i, cameraInfo );
			if( cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK ) {
				return ( cameraInfo.orientation - orientation + 360 ) % 360;
			}
		}
		// If Camera.open() succeeds, this code is never reached
		return -1;
	}

	private void configureCameraAndStartPreview( Camera camera )
	{
		// Setting camera parameters when preview is running can cause crashes on some android devices
		stopPreview();

		// Configure camera orientation. This is needed for both correct preview orientation and capture
		orientation = getCameraOrientation();
		camera.setDisplayOrientation( orientation );

		// Configure camera parameters
		Camera.Parameters parameters = camera.getParameters();

		// Select preview size. The minimal size for Image Capture scenario is FullHD in general case. For faster devices
		// 4K or higher (supported in Camera2) is preferable (better recognition quality but slower processing and larger
		// image size)
		cameraPreviewSize = null;
		for( Camera.Size size : parameters.getSupportedPreviewSizes() ) {
			if( cameraPreviewSize == null ) {
				cameraPreviewSize = size;
			} else {
				int resultArea = cameraPreviewSize.width * cameraPreviewSize.height;
				int newArea = size.width * size.height;
				if( newArea > resultArea ) {
					cameraPreviewSize = size;
				}
			}
		}
		parameters.setPreviewSize( cameraPreviewSize.width, cameraPreviewSize.height );

		// Zoom
		parameters.setZoom( cameraZoom );
		// Buffer format. The only currently supported format is NV21
		parameters.setPreviewFormat( ImageFormat.NV21 );
		// Default focus mode
		parameters.setFocusMode( Camera.Parameters.FOCUS_MODE_AUTO );

		// Done
		camera.setParameters( parameters );

		// The camera will fill the buffers with image data and notify us through the callback.
		// The buffers will be sent to camera on requests from the capture service (see implementation
		// of IImageCaptureService.Callback.onRequestLatestFrame above)
		camera.setPreviewCallbackWithBuffer( cameraPreviewCallback );

		// Clear the previous capture results if any
		clearCaptureResults();

		// Width and height of the preview according to the current screen rotation
		int width = 0;
		int height = 0;
		switch( orientation ) {
			case 0:
			case 180:
				width = cameraPreviewSize.width;
				height = cameraPreviewSize.height;
				break;
			case 90:
			case 270:
				width = cameraPreviewSize.height;
				height = cameraPreviewSize.width;
				break;
		}

		// Configure the view scale and area of interest (camera sees it as rotated 90 degrees, so
		// there's some confusion with what is width and what is height)
		surfaceViewWithOverlay.setTransform( width, height, orientation );
		// Area of interest
		surfaceViewWithOverlay.setAreaOfInterest( new Rect( 0, 0, width, height ) );

		// Start preview
		camera.startPreview();

		setCameraFocusMode( Camera.Parameters.FOCUS_MODE_AUTO );
		autoFocus( finishCameraInitialisationAutoFocusCallback );

		inPreview = true;
	}


	// The 'Start' and 'Stop' button
	public void onStartButtonClick( View view )
	{
		if( startButton.getText().equals( BUTTON_TEXT_STOP ) ) {
			stopCapture();
		} else {
			clearCaptureResults();
			startButton.setEnabled( false );
			startButton.setText( BUTTON_TEXT_STARTING );
			if( !isContinuousVideoFocusModeEnabled( camera ) ) {
				autoFocus( startCaptureCameraAutoFocusCallback );
			} else {
				startCapture();
			}
		}
	}

	// Camera permission request handler for Android 6.0 and higher
	@Override
	public void onRequestPermissionsResult( int requestCode, String[] permissions, int[] grantResults )
	{
		switch( requestCode ) {
			case CAMERA_PERMISSION_REQUEST_CODE:
				if( grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED ) {
					if( camera == null ) {
						camera = Camera.open();
					}
				} else {
					showStartupError( "Camera is essential for this application." );
				}
				break;
			default:
				throw new IllegalArgumentException();
		}
	}

	// Initialize documentSize spinner in the UI with available languages
	private void initializeDocumentSizeSpinner()
	{
		final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences( this );

		// Make the collapsed spinner the size of the selected item
		ArrayAdapter<String> adapter = new ArrayAdapter<String>( MainActivity.this, R.layout.spinner_item, R.id.spinner_item_text ) {
			@Override
			public View getView( int position, View convertView, ViewGroup parent )
			{
				View view = super.getView( position, convertView, parent );
				RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
					RelativeLayout.LayoutParams.WRAP_CONTENT,
					RelativeLayout.LayoutParams.WRAP_CONTENT );
				view.setLayoutParams( params );

				return setUsage( view, position );
			}

			@Override
			public View getDropDownView( int position, View convertView, ViewGroup parent )
			{
				return setUsage( super.getDropDownView( position, convertView, parent ), position );
			}

			View setUsage( View view, int position )
			{
				SampleDocumentSize documentSize = SampleDocumentSize.values()[position];
				TextView usage = (TextView) view.findViewById( R.id.spinner_item_usage );
				usage.setText( documentSize.Usage );
				return view;
			}
		};

		// Stored preference
		final String sampeDocumentSizeKey = "SampleDocumentSize";
		String selectedSample = preferences.getString( sampeDocumentSizeKey, SampleDocumentSize.values()[0].toString() );

		// Fill the spinner with available document sizes selecting the previously chosen size
		int selectedIndex = -1;
		int defaultIndex = -1;
		for( int i = 0; i < SampleDocumentSize.values().length; i++ ) {
			SampleDocumentSize scenario = SampleDocumentSize.values()[i];
			String name = scenario.toString();
			adapter.add( name );
			if( name.equalsIgnoreCase( selectedSample ) ) {
				selectedIndex = i;
			}
			if( name.equalsIgnoreCase( documentSize.toString() ) ) {
				defaultIndex = i;
			}
		}
		if( selectedIndex == -1 ) {
			selectedIndex = defaultIndex;
		} else {
			documentSize = SampleDocumentSize.valueOf( selectedSample );
		}

		documentSizeSpinner.setAdapter( adapter );

		if( selectedIndex != -1 ) {
			documentSizeSpinner.setSelection( selectedIndex );
		}

		// The callback to be called when a size is selected
		documentSizeSpinner.setOnItemSelectedListener( new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected( AdapterView<?> parent, View view, int position, long id )
			{
				String selectedSize = (String) parent.getItemAtPosition( position );
				if( !preferences.getString( sampeDocumentSizeKey, "" ).equalsIgnoreCase( selectedSize ) ) {
					documentSize = SampleDocumentSize.valueOf( selectedSize );
					if( imageCaptureService != null ) {
						imageCaptureService.setDocumentSize( documentSize.Width, documentSize.Height );
					}
					// Store the selection in preferences
					SharedPreferences.Editor editor = preferences.edit();
					editor.putString( sampeDocumentSizeKey, selectedSize );
					editor.commit();
				}
			}

			@Override
			public void onNothingSelected( AdapterView<?> parent )
			{
			}
		} );
	}


	@Override
	public void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_main );

		// Retrieve some ui components
		warningTextView = (TextView) findViewById( R.id.warningText );
		errorTextView = (TextView) findViewById( R.id.errorText );
		startButton = (Button) findViewById( R.id.startButton );
		documentSizeSpinner = (Spinner) findViewById( R.id.documentSizeSpinner );

		// Initialize the documentSizes spinner
		initializeDocumentSizeSpinner();

		// Manually create preview surface. The only reason for this is to
		// avoid making it public top level class
		RelativeLayout layout = (RelativeLayout) startButton.getParent();

		surfaceViewWithOverlay = new SurfaceViewWithOverlay( this );
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
			RelativeLayout.LayoutParams.MATCH_PARENT,
			RelativeLayout.LayoutParams.MATCH_PARENT );
		surfaceViewWithOverlay.setLayoutParams( params );
		// Add the surface to the layout as the bottom-most view filling the parent
		layout.addView( surfaceViewWithOverlay, 0 );

		// Create capture service
		if( createImageCaptureService() ) {
			// Set the callback to be called when the preview surface is ready.
			// We specify it as the last step as a safeguard so that if there are problems
			// loading the engine the preview will never start and we will never attempt calling the service
			surfaceViewWithOverlay.getHolder().addCallback( surfaceCallback );
		}

		layout.setOnClickListener( clickListener );
	}

	@Override
	public void onResume()
	{
		super.onResume();
		// Reinitialize the camera, restart the preview and capture if required
		startButton.setEnabled( false );
		clearCaptureResults();
		startCaptureWhenReady = startCaptureOnAppStart;

		if( ContextCompat.checkSelfPermission( this, Manifest.permission.CAMERA ) == PackageManager.PERMISSION_GRANTED ) {
			if( camera == null ) {
				camera = Camera.open();
			}
		} else {
			if( !cameraPermissionRequested ) {
				ActivityCompat.requestPermissions( this, new String[] { Manifest.permission.CAMERA }, CAMERA_PERMISSION_REQUEST_CODE );
			}
			// After permission dialog is dismissed, onResume will be invoked again
			cameraPermissionRequested = true;
		}
		if( previewSurfaceHolder != null && camera != null ) {
			setCameraPreviewDisplayAndStartPreview();
		}
	}

	@Override
	public void onPause()
	{
		// Clear all pending actions
		handler.removeCallbacksAndMessages( null );
		// Stop the capture capture service
		if( imageCaptureService != null ) {
			imageCaptureService.stop();
		}
		startButton.setText( BUTTON_TEXT_START );
		// Clear capture results
		clearCaptureResults();
		stopPreviewAndReleaseCamera();
		super.onPause();
	}

	// Surface View combined with an overlay showing capture results and 'progress'
	static class SurfaceViewWithOverlay extends SurfaceView {
		private Point[] corners;
		private Bitmap bitmap;
		private Rect areaOfInterest;
		private int scaleNominatorX = 1;
		private int scaleDenominatorX = 1;
		private int scaleNominatorY = 1;
		private int scaleDenominatorY = 1;
		private Paint boundariesPaint;
		private Paint backgroundPaint;
		private Paint outsideBoundariesPaint;
		private int width;
		private int height;
		private int orientation;

		Paint textPaint = new Paint();
		Paint reservedPaint = new Paint();

		class Block {
			Rect Rect;
			Paint Paint;
			int Alpha;
			Block( Rect rect, Paint paint, int alpha ) {
				this.Rect = rect;
				this.Paint = paint;
				this.Alpha = alpha;
			}
		}
		private Block[] blocks;

		public SurfaceViewWithOverlay( Context context )
		{
			super( context );
			this.setWillNotDraw( false );

			boundariesPaint = new Paint();
			boundariesPaint.setStyle( Paint.Style.STROKE );
			boundariesPaint.setStrokeWidth( 3 );
			boundariesPaint.setARGB( 255, 0, 129, 0 );
			outsideBoundariesPaint = new Paint();
			outsideBoundariesPaint.setARGB( 150, 0, 0, 0 );
			outsideBoundariesPaint.setStyle( Paint.Style.FILL );
			backgroundPaint = new Paint();
			backgroundPaint.setStyle( Paint.Style.FILL );
			backgroundPaint.setARGB( 150, 0, 0, 0 );

			textPaint.setStyle( Paint.Style.FILL );
			textPaint.setARGB( 50, 0, 0, 0 );

			reservedPaint.setStyle( Paint.Style.STROKE );
			reservedPaint.setARGB( 255, 129, 129, 129 );
		}

		public void setTransform( int width, int height, int orientation )
		{
			this.width = width;
			this.height = height;
			this.orientation = orientation;
			scaleNominatorX = getWidth();
			scaleDenominatorX = width;
			scaleNominatorY = getHeight();
			scaleDenominatorY = height;
		}

		public void setAreaOfInterest( Rect newValue )
		{
			areaOfInterest = newValue;
			invalidate();
		}

		public Rect getAreaOfInterest()
		{
			return areaOfInterest;
		}

		public void setDocumentBoundary( Point[] corners )
		{
			if( corners != null ) {
				this.corners = new Point[corners.length];
				for( int i = 0; i < corners.length; i++ ) {
					this.corners[i] = cameraToScreen( corners[i] );
				}
			} else {
				this.corners = null;
			}
			this.invalidate();
		}

		public void setBitmap( Bitmap bitmap )
		{
			this.bitmap = bitmap;
			this.invalidate();
		}

		public void setQualityAssessmentBlocks( IImageCaptureService.QualityAssessmentForOcrBlock[] qualityAssesmentBlocks )
		{
			if( qualityAssesmentBlocks != null ) {
				this.blocks = new Block[qualityAssesmentBlocks.length];

				for( int i = 0; i < qualityAssesmentBlocks.length; i++ ) {
					IImageCaptureService.QualityAssessmentForOcrBlock block = qualityAssesmentBlocks[i];
					switch( block.Type ) {
						case Text:
							this.blocks[i] = new Block( cameraToScreen( block.Rect ), textPaint, ( 50 * block.Quality ) / 100 );
							break;
						default:
							this.blocks[i] = new Block( cameraToScreen( block.Rect ), reservedPaint, 100 );
					}
				}
			} else {
				this.blocks = null;
			}
			this.invalidate();
		}

		private Point cameraToScreen( Point p )
		{
			switch( orientation ) {
				case 0: return new Point( ( p.x * scaleNominatorX ) / scaleDenominatorX,
					( p.y * scaleNominatorY ) / scaleDenominatorY );
				case 90: return new Point( ( ( width - p.y ) * scaleNominatorX ) / scaleDenominatorX,
					( p.x * scaleNominatorY ) / scaleDenominatorY );
				case 180: return new Point( ( ( width - p.x ) * scaleNominatorX ) / scaleDenominatorX,
					( ( height - p.y ) * scaleNominatorY ) / scaleDenominatorY );
				case 270: return new Point( ( p.y * scaleNominatorX ) / scaleDenominatorX,
					( ( height - p.x ) * scaleNominatorY ) / scaleDenominatorY );
				default:
					throw new IllegalArgumentException();
			}
		}

		private Rect cameraToScreen( Rect r )
		{
			Point p1 = cameraToScreen( new Point( r.left, r.top ) );
			Point p2 = cameraToScreen( new Point( r.right, r.bottom ) );
			return new Rect( Math.min( p1.x, p2.x ), Math.min( p1.y, p2.y ), Math.max( p1.x, p2.x ), Math.max( p1.y, p2.y ) );
		}

		private int scaleWidth( int width )
		{
			switch( orientation ) {
				case 0: return width * scaleNominatorY / scaleDenominatorY;
				case 90: return width * scaleNominatorX / scaleDenominatorX;
				case 180: return width * scaleNominatorY / scaleDenominatorY;
				case 270: return width * scaleNominatorX / scaleDenominatorX;
				default:
					throw new IllegalArgumentException();
			}
		}

		private int scaleHeight( int height )
		{
			switch( orientation ) {
				case 0: return height * scaleNominatorX / scaleDenominatorX;
				case 90: return height * scaleNominatorY / scaleDenominatorY;
				case 180: return height * scaleNominatorX / scaleDenominatorX;
				case 270: return height * scaleNominatorY / scaleDenominatorY;
				default:
					throw new IllegalArgumentException();
			}
		}

		@Override
		protected void onDraw( Canvas canvas )
		{
			super.onDraw( canvas );

			if( bitmap != null ) {
				canvas.drawRect( 0, 0, getWidth(), getHeight(), backgroundPaint );

				int bitmapWidth = bitmap.getWidth();
				int bitmapHeight = bitmap.getHeight();
				int scaledBitmapWidth = scaleWidth( bitmapWidth );
				int scaledBitmapHeight = scaleHeight( bitmapHeight );
				int dX = ( getWidth() - scaledBitmapWidth ) / 2;
				int dY = ( getHeight() - scaledBitmapHeight ) / 2;
				canvas.drawBitmap( bitmap, new Rect( 0, 0, bitmapWidth, bitmapHeight ), new RectF( dX, dY, getWidth() - dX, getHeight() - dY ), null );
				canvas.drawRect( dX, dY, getWidth() - dX - 1, getHeight() - dY - 1, boundariesPaint );
			}

			if( corners != null ) {
				Path path = new Path();
				for( int i = 0; i < 4 ; i++ ) {
					Point p = corners[i];
					if( i == 0 ) {
						path.moveTo( p.x, p.y );
					} else {
						path.lineTo( p.x, p.y );
					}
				}
				path.close();

				canvas.save();
				canvas.clipPath( path, Region.Op.DIFFERENCE );
				canvas.drawRect( 0, 0, getWidth(), getHeight(), outsideBoundariesPaint );
				canvas.restore();

				canvas.drawPath( path, boundariesPaint );
				canvas.clipPath( path );
			}

			if( bitmap == null && blocks != null ) {
				for( Block block : blocks ) {
					if( block != null ) {
						block.Paint.setAlpha( block.Alpha );
						if( block.Alpha != 100 ) {
							block.Paint.setARGB( 50, 255 * (50 - block.Alpha) / 50, 255 * block.Alpha / 50, 0  );
							canvas.drawRect( block.Rect, block.Paint );
						}
						canvas.drawRect( block.Rect, reservedPaint );
					}
				}
			}
		}
	}
}