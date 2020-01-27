package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.net;

import java.io.IOException;

import androidx.annotation.NonNull;
import okhttp3.Interceptor;
import okhttp3.Response;

/**
 * Intercepts network requests and throws {@link NoNetworkConnectionException} if there is no network connection.
 */
public class NetworkConnectionStateInterceptor implements Interceptor {

	private final NetworkConnectionCompat networkConnectionCompat;

	public NetworkConnectionStateInterceptor( @NonNull NetworkConnectionCompat networkConnectionCompat )
	{
		this.networkConnectionCompat = networkConnectionCompat;
	}

	@Override
	public Response intercept( @NonNull Chain chain ) throws IOException
	{
		if( !networkConnectionCompat.isConnected() ) {
			throw new NoNetworkConnectionException();
		}

		return chain.proceed( chain.request() );
	}
}
