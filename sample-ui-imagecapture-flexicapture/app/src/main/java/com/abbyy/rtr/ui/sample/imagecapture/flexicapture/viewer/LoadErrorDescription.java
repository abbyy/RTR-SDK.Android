// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.viewer;

import android.content.Context;

import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.R;

import androidx.annotation.NonNull;

/**
 * Converts pages loading error to a user-readable format.
 */
public class LoadErrorDescription {

	private final Context context;

	public LoadErrorDescription( @NonNull Context context )
	{
		this.context = context;
	}

	public String getLoadDescription()
	{
		return context.getString( R.string.load_error );
	}

}
