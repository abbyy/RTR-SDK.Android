// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.sign_in;

import android.content.Context;

import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.R;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.net.NoNetworkConnectionException;

import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.UnknownHostException;

import androidx.annotation.NonNull;

/**
 * Converts sign in error to a user-readable format.
 */
public class SignInErrorDescription {

	private final Context context;

	public SignInErrorDescription( @NonNull Context context )
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

	public String getRequestCodeDescription( int code )
	{
		switch( code ) {
			case HttpURLConnection.HTTP_UNAUTHORIZED:
				return context.getString( R.string.credentials_error );
			default:
				return context.getString( R.string.default_error );
		}
	}

	public String getIncorrectUrlDescription()
	{
		return context.getString( R.string.incorrect_url );
	}

	public String getEmptyProjectsDescription()
	{
		return context.getString( R.string.empty_projects );
	}

}
