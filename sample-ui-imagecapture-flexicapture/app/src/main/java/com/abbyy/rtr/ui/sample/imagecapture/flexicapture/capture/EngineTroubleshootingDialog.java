// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture;

import android.app.Application;
import android.app.Dialog;
import android.os.Bundle;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.R;

import java.io.IOException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

public class EngineTroubleshootingDialog {

	private EngineTroubleshootingDialog()
	{
		// Utility class
	}

	/**
	 * Creates error dialog for throwable from {@link SharedEngine#initialize(Application)}.
	 * The dialog is used for troubleshooting.
	 * Do not use in production builds.
	 *
	 * @see SharedEngine
	 */
	public static DialogFragment create( @NonNull Throwable throwable )
	{
		if( throwable instanceof IOException ) {
			return createDialogWithMessage(
				"Could not load some required resource files. Make sure to configure " +
					"'assets' directory in your application and specify correct 'license file name'. " +
					"See logcat for details."
			);
		} else if( throwable instanceof Engine.LicenseException ) {
			return createDialogWithMessage(
				"License not valid. Make sure you have a valid license file in the " +
					"'assets' directory and specify correct 'license file name' and 'application id'. " +
					"See logcat for details."
			);
		} else {
			return createDialogWithMessage(
				"Unspecified error while loading the engine. See logcat for details."
			);
		}
	}

	private static DialogFragment createDialogWithMessage( @NonNull String message )
	{
		return TroubleshootingDialog.newInstance( message );
	}

	public static class TroubleshootingDialog extends DialogFragment {

		private static final String MESSAGE_KEY = "message_key";

		public static DialogFragment newInstance( @NonNull String message )
		{
			DialogFragment dialogFragment = new TroubleshootingDialog();
			dialogFragment.setCancelable( false );

			Bundle arguments = new Bundle();
			arguments.putString( MESSAGE_KEY, message );
			dialogFragment.setArguments( arguments );

			return dialogFragment;
		}

		@NonNull
		@Override
		public Dialog onCreateDialog( @Nullable Bundle savedInstanceState )
		{
			return new AlertDialog.Builder( requireContext() )
				.setTitle( R.string.abbyy_mi_sdk )
				.setMessage( getMessage() )
				.setIcon( android.R.drawable.ic_dialog_alert )
				.setPositiveButton( android.R.string.ok, ( dialog, which ) -> finishActivity() )
				.create();
		}

		private void finishActivity()
		{
			requireActivity().finish();
		}

		private String getMessage()
		{
			Bundle arguments = getArguments();
			if( arguments == null ) {
				throw new IllegalStateException( "null arguments" );
			}

			return arguments.getString( MESSAGE_KEY );
		}
	}
}
