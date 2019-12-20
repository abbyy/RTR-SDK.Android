// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.multipage;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.UiThread;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.abbyy.mobile.uicomponents.CaptureView;
import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario.Result;
import com.abbyy.rtr.ui.sample.imagecapture.multipage.utils.BackgroundWorker;

import java.lang.ref.WeakReference;

/**
 * This activity uses UI Component to capture pages, shows a dialog to confirm or discard
 * the result
 */
public class CaptureActivity extends AppCompatActivity implements MultiPageImageCaptureScenario.Callback {

	private static final String LOG_TAG = "CaptureActivity";
	private static final String PROFILE_KEY = "ProfileKey";
	private static final String START_AS_PAGE_ID_KEY = "StartAsPageIdKey";

	public static Intent getIntent(
		Context context,
		CaptureProfile profile,
		String startAsPageId
	)
	{
		Intent intent = new Intent( context, CaptureActivity.class );
		intent.putExtra( PROFILE_KEY, profile );
		intent.putExtra( START_AS_PAGE_ID_KEY, startAsPageId );
		return intent;
	}

	// Capture view component
	private CaptureView captureView;
	private MultiPageImageCaptureScenario imageCaptureScenario;

	// This dialog is shown when user wants to leave without saving
	private AlertDialog discardPagesDialog;

	// Working with the result on a background thread
	private BackgroundWorker.Callback<Void, Boolean> resultProcessCallback = new BackgroundWorker.Callback<Void, Boolean>() {
		@Override
		public Boolean doWork( Void none ) throws Exception
		{
			Result result = imageCaptureScenario.getResult();
			return result.getPages().isEmpty();
		}

		@UiThread
		@Override
		public void onDone( Boolean isResultEmpty )
		{
			if( isResultEmpty == null || isResultEmpty ) {
				finishWithResult();
			} else {
				showDiscardPagesDialog();
			}
		}

		@Override public void onError( Exception exception )
		{
			Toast.makeText( CaptureActivity.this, R.string.unknown_error, Toast.LENGTH_LONG ).show();
		}
	};

	@Override
	protected void onDestroy()
	{
		resultProcessCallback = null;
		super.onDestroy();
	}

	@Override
	protected void onCreate( Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );

		requestWindowFeature( Window.FEATURE_NO_TITLE );
		getWindow().setFlags(
			WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN
		);

		setContentView( R.layout.activity_capture );

		initCaptureView();
	}

	private void initCaptureView()
	{
		captureView = findViewById( R.id.captureView );
		captureView.getUISettings().setTheme( CaptureView.UISettings.Theme.DARK );
		imageCaptureScenario = getScenario();
		if( imageCaptureScenario != null ) {
			imageCaptureScenario.setCallback( this );

			captureView.setCaptureScenario( imageCaptureScenario );
		}
	}

	@Nullable
	private MultiPageImageCaptureScenario getScenario()
	{
		String startAsPageId = getIntent().getStringExtra( START_AS_PAGE_ID_KEY );
		CaptureProfile profile = (CaptureProfile) getIntent().getSerializableExtra( PROFILE_KEY );
		// Start page id should be used only once
		getIntent().putExtra( START_AS_PAGE_ID_KEY, (String) null );
		return ScenarioFactory.create(
			this,
			EngineFactory.getEngine(),
			profile,
			startAsPageId
		);
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
		if( discardPagesDialog != null ) {
			discardPagesDialog.dismiss();
		}
		super.onPause();
	}

	// Error handler for UI Component
	@Override
	public void onError( @NonNull Exception error, @NonNull Result result )
	{
		String errorMessage;
		if( error.getMessage() != null ) {
			errorMessage = error.getMessage();
		} else {
			errorMessage = getString( R.string.unknown_error );
		}
		Toast.makeText( this, errorMessage, Toast.LENGTH_SHORT ).show();
	}

	// Close handler
	@Override
	public void onClose( @NonNull Result result )
	{
		new BackgroundWorker<>( new WeakReference<>( resultProcessCallback ) ).execute();
	}

	// Capture finish handler
	@Override
	public void onFinished( @NonNull Result result )
	{
		finishWithResult();
	}

	// Show discard pages confirmation dialog
	private void showDiscardPagesDialog()
	{
		AlertDialog.Builder builder = new AlertDialog.Builder( this, R.style.AlertDialog )
			.setMessage( R.string.captured_pages_delete_warning )
			.setTitle( R.string.discard_pages )
			.setPositiveButton( R.string.discard, new DialogInterface.OnClickListener() {
				@Override
				public void onClick( DialogInterface dialog, int which )
				{
					clearAndFinish();
				}
			} )
			.setNegativeButton( R.string.cancel, null )
			.setOnDismissListener( new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss( DialogInterface dialog )
				{
					imageCaptureScenario.start();
				}
			} );
		discardPagesDialog = builder.show();
	}

	private void clearAndFinish()
	{
		// Clearing the result in background
		AsyncTask.execute( new Runnable() {
			@Override public void run()
			{
				try {
					imageCaptureScenario.getResult().clear();
				} catch( Exception e ) {
					Log.e( LOG_TAG, "Failed to clear pages", e );
				}
			}
		} );
		setResult( RESULT_CANCELED, null );
		finish();
	}

	private void finishWithResult()
	{
		Intent intent = new Intent();
		setResult( RESULT_OK, intent );

		finish();
	}
}
