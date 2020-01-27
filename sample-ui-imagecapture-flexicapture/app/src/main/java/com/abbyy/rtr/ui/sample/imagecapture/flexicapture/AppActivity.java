// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture;

import android.os.Bundle;

import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture.EngineTroubleshootingDialog;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture.SharedEngine;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentManager;

/**
 * The activity is responsible for navigation between fragments and {@link com.abbyy.mobile.rtr.Engine} initialization.
 */
public class AppActivity extends AppCompatActivity {

	private static final String CAPTURE_BACK_STACK_NAME = "capture";

	@Override
	protected void onCreate( @Nullable Bundle savedInstanceState )
	{
		super.onCreate( savedInstanceState );
		setContentView( R.layout.activity_app );

		// Call it before if block because the process can be killed while the app is in the recent apps list.
		// Then savedInstanceState will be not null, but the engine won't be initialized.
		boolean isInitialized = initializeEngine();
		if( savedInstanceState == null ) {
			if( isInitialized ) {
				openViewer();
			}
		}
	}

	private boolean initializeEngine()
	{
		try {
			SharedEngine.initialize( getApplication() );
			return true;
		} catch( Throwable throwable ) {
			DialogFragment dialogFragment = EngineTroubleshootingDialog.create( throwable );
			dialogFragment.show( getSupportFragmentManager(), null );
			return false;
		}
	}

	private void openViewer()
	{
		getSupportFragmentManager()
			.beginTransaction()
			.replace( R.id.container, ViewerFragment.newInstance(), ViewerFragment.TAG )
			.addToBackStack( null )
			.commit();
	}

	public void openSignInSettings()
	{
		getSupportFragmentManager()
			.beginTransaction()
			.setCustomAnimations(
				R.anim.fragment_enter,
				R.anim.fragment_exit,
				R.anim.fragment_pop_enter,
				R.anim.fragment_pop_exit
			)
			.replace( R.id.container, SignInFragment.newInstance(), SignInFragment.TAG )
			.addToBackStack( null )
			.commit();
	}

	public void openCaptureScreen( @Nullable String startAsPageId )
	{
		getSupportFragmentManager()
			.beginTransaction()
			.replace( R.id.container, CaptureFragment.newInstance( startAsPageId ), CaptureFragment.TAG )
			.addToBackStack( CAPTURE_BACK_STACK_NAME )
			.commit();
	}

	public void closeCaptureScreen()
	{
		// CaptureView may show additional fragments on capture screen
		getSupportFragmentManager().popBackStack( CAPTURE_BACK_STACK_NAME, FragmentManager.POP_BACK_STACK_INCLUSIVE );
	}

	@Override
	public void onBackPressed()
	{
		if( getSupportFragmentManager().getBackStackEntryCount() == 1 ) {
			finish();
		} else {
			super.onBackPressed();
		}
	}
}
