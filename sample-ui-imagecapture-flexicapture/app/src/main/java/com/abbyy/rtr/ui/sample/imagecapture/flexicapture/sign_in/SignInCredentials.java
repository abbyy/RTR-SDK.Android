// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.sign_in;

import androidx.annotation.NonNull;

/**
 * Represents a set of credentials that are necessary to authorize a user.
 */
public class SignInCredentials {

	@NonNull
	private final String url;
	@NonNull
	private final String tenant;
	@NonNull
	private final String username;
	@NonNull
	private final String authTicket;

	public SignInCredentials(
		@NonNull String url,
		@NonNull String tenant,
		@NonNull String username,
		// We don't store a password for security reasons.
		@NonNull String authTicket
	)
	{
		this.url = url;
		this.tenant = tenant;
		this.username = username;
		this.authTicket = authTicket;
	}

	@NonNull
	public String getUrl()
	{
		return url;
	}

	@NonNull
	public String getTenant()
	{
		return tenant;
	}

	@NonNull
	public String getUsername()
	{
		return username;
	}

	@NonNull
	public String getAuthTicket()
	{
		return authTicket;
	}
}
