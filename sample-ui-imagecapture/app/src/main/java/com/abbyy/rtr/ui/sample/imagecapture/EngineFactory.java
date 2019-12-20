// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture;

import android.app.Activity;
import android.content.DialogInterface;
import android.support.annotation.MainThread;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.util.Log;

import com.abbyy.mobile.rtr.Engine;

import java.lang.ref.WeakReference;

// Engine factory which reuses engine instance.
// There is no need to create engine every time.
public class EngineFactory {

	// Licensing
	private static final String LICENSE_FILE_NAME = "license";

	private static Engine ENGINE = null;

	@MainThread
	@Nullable
	public static Engine createEngine( @NonNull Activity activity )
	{
		// Initialize the engine and image capture service
		try {
			if( ENGINE == null ) {
				ENGINE = Engine.load( activity.getApplicationContext(), LICENSE_FILE_NAME );
			}

			return ENGINE;
		} catch( java.io.IOException e ) {
			// Troubleshooting for the developer
			Log.e( activity.getString( R.string.app_name ), "Error loading ABBYY MI SDK:", e );
			showStartupError(
				activity,
				"Could not load some required resource files. Make sure to configure " +
					"'assets' directory in your application and specify correct 'license file name'. " +
					"See logcat for details."
			);
		} catch( Engine.LicenseException e ) {
			// Troubleshooting for the developer
			Log.e( activity.getString( R.string.app_name ), "Error loading ABBYY MI SDK:", e );
			showStartupError(
				activity,
				"License not valid. Make sure you have a valid license file in the " +
					"'assets' directory and specify correct 'license file name' and 'application id'. " +
					"See logcat for details."
			);
		} catch( Throwable e ) {
			// Troubleshooting for the developer
			Log.e( activity.getString( R.string.app_name ), "Error loading ABBYY MI SDK:", e );
			showStartupError(
				activity,
				"Unspecified error while loading the engine. See logcat for details."
			);
		}

		return null;
	}

	// Show error on startup if any
	private static void showStartupError( @NonNull Activity activity, @NonNull String message )
	{
		final WeakReference<Activity> activityRef = new WeakReference<>( activity );
		new AlertDialog.Builder( activity )
			.setTitle( R.string.abbyy_mi_sdk )
			.setMessage( message )
			.setIcon( android.R.drawable.ic_dialog_alert )
			.show()
			.setOnDismissListener( new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss( DialogInterface dialog )
				{
					Activity activity = activityRef.get();
					if( activity != null ) {
						activity.finish();
					}
				}
			} );
	}

}
