// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.widgets;

import android.app.Activity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;

public class KeyboardCloseHelper {

	private KeyboardCloseHelper()
	{
		// Utility class
	}

	/**
	 * Closes soft keyboard.
	 */
	public static void close( @NonNull Activity activity )
	{
		InputMethodManager inputMethodManager = (InputMethodManager) activity
			.getSystemService( Activity.INPUT_METHOD_SERVICE );
		View view = activity.getCurrentFocus();

		if( inputMethodManager != null && view != null ) {
			inputMethodManager.hideSoftInputFromWindow( view.getWindowToken(), 0 );
		}
	}

}
