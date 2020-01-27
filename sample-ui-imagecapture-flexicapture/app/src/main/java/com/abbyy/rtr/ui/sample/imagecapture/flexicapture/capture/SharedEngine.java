// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture;

import android.app.Application;
import android.util.Log;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.R;

import androidx.annotation.NonNull;

/**
 * Shared Engine holder which reuses engine instance.
 * There is no need to create engine every time.
 */
public class SharedEngine {

	private static final String LICENSE_FILE_NAME = "license";

	private static Engine ENGINE = null;

	private SharedEngine()
	{
		// Utility class
	}

	/**
	 * Initializes Engine. If Engine is already initialized nothing happens.
	 *
	 * @param applicationContext Application instance is required to avoid Activity Context memory leak.
	 *
	 * @throws Throwable Throwable that may be thrown during initialization.
	 * Use {@link EngineTroubleshootingDialog } to show error description.
	 * @see EngineTroubleshootingDialog
	 */
	public static synchronized void initialize( @NonNull Application applicationContext ) throws Throwable
	{
		if( ENGINE != null ) {
			// An engine should be initialized only once during Application lifecycle.
			return;
		}

		try {
			ENGINE = Engine.load( applicationContext, LICENSE_FILE_NAME );
		} catch( Throwable e ) {
			// Troubleshooting for the developer
			Log.e( applicationContext.getString( R.string.app_name ), "Error loading ABBYY Mobile Capture SDK:", e );
			throw e;
		}
	}

	@NonNull
	public static synchronized Engine get()
	{
		if( ENGINE == null ) {
			throw new IllegalStateException( "Engine isn't initialized" );
		} else {
			return ENGINE;
		}
	}

}
