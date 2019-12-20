// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.multipage;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.uicomponents.scenario.ImageCaptureSettings;
import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;

import java.io.File;

public class ScenarioFactory {

	private static final String LOG_TAG = "ScenarioFactory";

	@Nullable
	public static MultiPageImageCaptureScenario create(
		Context context,
		Engine engine,
		final CaptureProfile profile,
		String startAsEditorPageId
	)
	{
		MultiPageImageCaptureScenario.Builder builder;
		try {
			String path = getScenarioPath( context, profile );
			builder = new MultiPageImageCaptureScenario.Builder( engine, path );
		} catch( Exception e ) {
			Log.e( LOG_TAG, "Failed to build scenario", e );
			return null;
		}
		builder.setStartAsEditorAtPage( startAsEditorPageId );
		builder.setCaptureSettings( new MultiPageImageCaptureScenario.CaptureSettings() {
			@Override
			public void onConfigureImageCaptureSettings(
				@NonNull ImageCaptureSettings imageCaptureSettings,
				int pageIndex
			)
			{
				imageCaptureSettings.setDocumentSize( profile.documentSize );
				imageCaptureSettings.setAspectRatioMin( profile.aspectRatioMin );
				imageCaptureSettings.setAspectRatioMax( profile.aspectRatioMax );
			}
		} );
		return builder.build();
	}

	private static String getScenarioPath( Context context, CaptureProfile profile )
	{
		File scenarioStorage = new File( context.getFilesDir(), profile.directoryName );
		if( !scenarioStorage.exists() ) {
			scenarioStorage.mkdir();
		}
		return scenarioStorage.getPath();
	}

}
