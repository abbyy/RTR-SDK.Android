// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.send;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * Stores sending preferences.
 */
public class SendPreferences {

	private static final String FILENAME = "send_preferences";

	private static final String PROJECT_KEY = "project_key";
	private static final String IS_SENT_KEY = "is_sent_key";

	private final SharedPreferences sharedPreferences;

	public SendPreferences( @NonNull Context context )
	{
		sharedPreferences = context.getSharedPreferences( FILENAME, Context.MODE_PRIVATE );
	}

	/**
	 * Sets the user project. The project is a parameter of a document sent to server.
	 */
	public void setProject( @NonNull String project )
	{
		sharedPreferences.edit().putString( PROJECT_KEY, project ).apply();
	}

	public void resetProject()
	{
		sharedPreferences.edit().remove( PROJECT_KEY ).apply();
	}

	@NonNull
	public String getProject()
	{
		return sharedPreferences.getString( PROJECT_KEY, "" );
	}

	/**
	 * Returns true if a document was successfully sent to a server, false otherwise.
	 */
	public boolean isDocumentSent()
	{
		return sharedPreferences.getBoolean( IS_SENT_KEY, false );
	}

	public void setDocumentSent( boolean isSent )
	{
		sharedPreferences.edit().putBoolean( IS_SENT_KEY, isSent ).apply();
	}

}
