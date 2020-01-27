// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.net;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

public class NetworkConnectionCompat {

	private final ConnectivityManager connectivityManager;

	public NetworkConnectionCompat( @NonNull Context context )
	{
		this.connectivityManager = getConnectivityManager( context );
	}

	@NonNull
	private ConnectivityManager getConnectivityManager( @NonNull Context context )
	{
		ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(
			Context.CONNECTIVITY_SERVICE
		);
		if( connectivityManager == null ) {
			throw new IllegalStateException( "Null connectivity manager" );
		}
		return connectivityManager;
	}

	/**
	 * Checks network connection state.
	 * <p>
	 * Before API 23 it uses {@link ConnectivityManager#getActiveNetwork()}.
	 * In Android 23+ it uses {@link ConnectivityManager#getNetworkCapabilities(Network)}}.
	 *
	 * @return true if network is connected, false otherwise.
	 */
	public boolean isConnected()
	{
		if( Build.VERSION.SDK_INT >= Build.VERSION_CODES.M ) {
			return isConnectedUsingNewApi();
		} else {
			return isConnectedUsingDeprecatedApi();
		}
	}

	@RequiresApi( Build.VERSION_CODES.M )
	private boolean isConnectedUsingNewApi()
	{
		Network network = connectivityManager.getActiveNetwork();
		if( network == null ) {
			return false;
		}

		NetworkCapabilities networkCapabilities = connectivityManager.getNetworkCapabilities( network );
		if( networkCapabilities == null ) {
			return false;
		}

		return networkCapabilities.hasCapability( NetworkCapabilities.NET_CAPABILITY_INTERNET );
	}

	@SuppressWarnings( "Deprecation" )
	private boolean isConnectedUsingDeprecatedApi()
	{
		NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
		return networkInfo != null && networkInfo.isConnectedOrConnecting();
	}
}
