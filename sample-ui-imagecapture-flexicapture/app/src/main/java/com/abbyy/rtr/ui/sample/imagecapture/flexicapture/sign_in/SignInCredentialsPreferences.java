// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.sign_in;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Stores {@link SignInCredentials}.
 */
public class SignInCredentialsPreferences {

	private static final String FILENAME = "sign_in_credentials_preferences";

	private static final String URL_KEY = "url_key";
	private static final String TENANT_KEY = "tenant_key";
	private static final String USERNAME_KEY = "username_key";
	private static final String AUTH_TICKET = "auth_ticket";

	private final SharedPreferences sharedPreferences;

	public SignInCredentialsPreferences( @NonNull Context context )
	{
		sharedPreferences = context.getSharedPreferences( FILENAME, Context.MODE_PRIVATE );
	}

	public void setSignInCredentials( @NonNull SignInCredentials credentials )
	{
		sharedPreferences
			.edit()
			.putString( URL_KEY, credentials.getUrl() )
			.putString( TENANT_KEY, credentials.getTenant() )
			.putString( USERNAME_KEY, credentials.getUsername() )
			.putString( AUTH_TICKET, credentials.getAuthTicket() )
			.apply();
	}

	public void resetSignInCredentials()
	{
		sharedPreferences.edit().clear().apply();
	}

	@Nullable
	public SignInCredentials getSignInCredentials()
	{
		String url = sharedPreferences.getString( URL_KEY, null );
		String tenant = sharedPreferences.getString( TENANT_KEY, null );
		String username = sharedPreferences.getString( USERNAME_KEY, null );
		String authTicket = sharedPreferences.getString( AUTH_TICKET, null );

		if( url != null && tenant != null && username != null && authTicket != null ) {
			return new SignInCredentials( url, tenant, username, authTicket );
		} else {
			return null;
		}
	}

	public boolean isSignedIn()
	{
		return getSignInCredentials() != null;
	}

}
