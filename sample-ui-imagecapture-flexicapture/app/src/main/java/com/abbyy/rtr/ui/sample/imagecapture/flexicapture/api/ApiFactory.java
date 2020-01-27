// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api;

import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.net.NetworkConnectionCompat;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.net.NetworkConnectionStateInterceptor;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.net.NoNetworkConnectionException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class ApiFactory {

	private static final String SLASH = "/";

	private ApiFactory()
	{
		// Utility class
	}

	/**
	 * Creates {@link Api} instance
	 * which supports {@link NoNetworkConnectionException} if there is no network connection.
	 * In contrast default Retrofit API instance usually throws {@link java.net.UnknownHostException}.
	 */
	@Nullable
	public static Api create(
		@NonNull String url,
		@NonNull NetworkConnectionCompat networkConnectionCompat
	)
	{
		try {
			return createRetrofit( getPreparedBaseUrl( url ), networkConnectionCompat ).create( Api.class );
		} catch( IllegalArgumentException e ) {
			// Illegal url
			return null;
		}
	}

	@NonNull
	private static String getPreparedBaseUrl( @NonNull String url )
	{
		if( url.endsWith( SLASH ) ) {
			return url;
		} else {
			// Retrofit requires slash at the end for the base url
			return url + SLASH;
		}
	}

	private static Retrofit createRetrofit(
		@NonNull final String url,
		@NonNull NetworkConnectionCompat networkConnectionCompat
	)
	{
		return new Retrofit.Builder()
			.client( createOkHttpClient( networkConnectionCompat ) )
			.addConverterFactory( GsonConverterFactory.create() )
			.baseUrl( url )
			.build();
	}

	private static OkHttpClient createOkHttpClient( @NonNull NetworkConnectionCompat networkConnectionCompat )
	{
		return new OkHttpClient.Builder()
			.addInterceptor( new NetworkConnectionStateInterceptor( networkConnectionCompat ) )
			.build();
	}

}
