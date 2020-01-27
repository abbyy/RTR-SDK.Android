// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.send;

import android.content.Context;
import android.util.Base64;

import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api.Api;
import com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture.ScenarioPages;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import okhttp3.Headers;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;

public class SendBody {

	private static final String PROJECT_PART_NAME = "projectName";

	private static final String CONTENT_MD5_HEADER = "Content-MD5";
	private static final String CONTENT_DISPOSITION_HEADER = "Content-Disposition";

	private static final String FORM_DATA_FORMAT = "form-data; name=\"%s\"; filename=\"%s\"";

	private final ScenarioPages scenarioPages;
	private final SendPreferences sendPreferences;

	public SendBody(
		@NonNull Context context,
		@NonNull MultiPageImageCaptureScenario.Result scenarioResult,
		@NonNull SendPreferences sendPreferences
	)
	{
		this.scenarioPages = new ScenarioPages( context, scenarioResult, "send" );
		this.sendPreferences = sendPreferences;
	}

	/**
	 * Creates request body from {@link MultiPageImageCaptureScenario.Result}
	 * for {@link Api#sendDocument(String, String, MultipartBody)} } method.
	 */
	@NonNull
	public MultipartBody create() throws IOException
	{
		MultipartBody.Builder builder = new MultipartBody.Builder();

		addProject( builder, sendPreferences.getProject() );
		addImages( builder, scenarioPages.getImageFiles() );

		builder.setType( MultipartBody.FORM );

		return builder.build();
	}

	private void addProject(
		@NonNull MultipartBody.Builder builder,
		@NonNull String project
	)
	{
		builder.addPart( MultipartBody.Part.createFormData( PROJECT_PART_NAME, project ) );
	}

	private void addImages(
		@NonNull MultipartBody.Builder builder,
		@NonNull List<File> imageFiles
	) throws IOException
	{
		String namePrefix = "image_%d"; // You can assign any name to the image, but the image names should differ.
		for( int imageIndex = 0; imageIndex < imageFiles.size(); ++imageIndex ) {
			String name = String.format( Locale.US, namePrefix, imageIndex );
			builder.addPart( createImagePart( name, imageFiles.get( imageIndex ) ) );
		}
	}

	private MultipartBody.Part createImagePart(
		@NonNull String name,
		@NonNull File imageFile
	) throws IOException
	{
		RequestBody body = RequestBody.create( null, imageFile );
		Headers headers = createHeadersForImage( name, imageFile );
		return MultipartBody.Part.create( headers, body );
	}

	private Headers createHeadersForImage(
		@NonNull String name,
		@NonNull File imageFile
	) throws IOException
	{
		String md5InBase64 = calculateMd5InBase64Encoding( imageFile );
		if( md5InBase64 == null ) {
			throw new IOException( "Can't calculate md5 from file" );
		}

		Headers.Builder headersBuilder = new Headers.Builder();
		headersBuilder.add( CONTENT_MD5_HEADER, md5InBase64 );
		headersBuilder.add( CONTENT_DISPOSITION_HEADER, getContentDispositionFormData( name, name ) );
		return headersBuilder.build();
	}

	/**
	 * Calculates md5 digest of file content in base 64 encoding.
	 */
	@Nullable
	private String calculateMd5InBase64Encoding( @NonNull File file )
	{
		try( FileInputStream fileInputStream = new FileInputStream( file ) ) {
			byte[] md5Bytes = getMd5Bytes( fileInputStream );
			if( md5Bytes == null ) {
				return null;
			} else {
				return Base64.encodeToString( md5Bytes, Base64.NO_WRAP );
			}
		} catch( IOException e ) {
			return null;
		}
	}

	@Nullable
	private byte[] getMd5Bytes( @NonNull InputStream inputStream ) throws IOException
	{
		MessageDigest digest;
		try {
			byte[] bytes = new byte[4096];
			int read;
			digest = MessageDigest.getInstance( "MD5" );
			while( ( read = inputStream.read( bytes ) ) != -1 ) {
				digest.update( bytes, 0, read );
			}
			return digest.digest();
		} catch( NoSuchAlgorithmException e ) {
			return null;
		}
	}

	private String getContentDispositionFormData( @NonNull String name, @NonNull String filename )
	{
		return String.format( Locale.US, FORM_DATA_FORMAT, name, filename );
	}

}
