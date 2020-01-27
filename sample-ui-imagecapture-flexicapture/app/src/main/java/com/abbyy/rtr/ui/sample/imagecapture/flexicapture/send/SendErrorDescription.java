// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.send;

import android.content.Context;

import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.R;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.net.NoNetworkConnectionException;

import java.net.ConnectException;
import java.net.UnknownHostException;

import androidx.annotation.NonNull;

/**
 * Converts send error to a user-readable format.
 */
public class SendErrorDescription {

	private final Context context;

	public SendErrorDescription( @NonNull Context context )
	{
		this.context = context;
	}

	public String getRequestThrowableDescription( @NonNull Throwable throwable )
	{
		if( throwable instanceof NoNetworkConnectionException ) {
			return context.getString( R.string.no_network_connection );
		} else if( throwable instanceof UnknownHostException ) {
			return context.getString( R.string.unknown_server_name );
		} else if( throwable instanceof ConnectException ) {
			return context.getString( R.string.connect_error );
		} else {
			return context.getString( R.string.default_error );
		}
	}

	public String getUnknownErrorDescription()
	{
		return context.getString( R.string.default_error );
	}

	public String getSendBodyCreationDescription()
	{
		return context.getString( R.string.send_body_creation_error );
	}

	public String getSignInDescription()
	{
		return context.getString( R.string.send_sign_in_error );
	}

}
