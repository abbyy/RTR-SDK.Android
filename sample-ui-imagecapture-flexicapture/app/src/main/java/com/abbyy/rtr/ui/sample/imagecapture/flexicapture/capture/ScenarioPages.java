// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.capture;

import android.content.Context;
import android.graphics.Bitmap;

import com.abbyy.mobile.rtr.Engine;
import com.abbyy.mobile.rtr.IImagingCoreAPI;
import com.abbyy.mobile.rtr.IImagingCoreAPI.ExportOperation.Compression;
import com.abbyy.mobile.rtr.IImagingCoreAPI.ExportToJpgOperation;
import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario;
import com.abbyy.mobile.uicomponents.scenario.MultiPageImageCaptureScenario.Result;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import androidx.annotation.NonNull;
import androidx.annotation.WorkerThread;

/**
 * Provides easy access to {@link MultiPageImageCaptureScenario.Result} pages.
 */
public class ScenarioPages {

	/**
	 * Simplified representation of a page from {@link MultiPageImageCaptureScenario.Result}.
	 */
	public class Page {
		private String id;
		private File imageFile;

		private Page( String id, File imageFile )
		{
			this.id = id;
			this.imageFile = imageFile;
		}

		public String getId()
		{
			return id;
		}

		public File getImageFile()
		{
			return imageFile;
		}
	}

	public interface Listener {
		void onNextPage( @NonNull Page page );
	}

	private final Context context;
	private final MultiPageImageCaptureScenario.Result scenarioResult;
	private final String directoryName;

	public ScenarioPages(
		@NonNull Context context,
		@NonNull Result scenarioResult,
		@NonNull String directoryName
	)
	{
		this.context = context;
		this.scenarioResult = scenarioResult;
		this.directoryName = directoryName;
	}

	/**
	 * Gets image files for all pages from {@link MultiPageImageCaptureScenario.Result}.
	 */
	@WorkerThread
	@NonNull
	public List<File> getImageFiles() throws IOException
	{
		List<File> imageFiles = new ArrayList<>();
		loadPagesBlocking( page -> imageFiles.add( page.getImageFile() ) );
		return imageFiles;
	}

	/**
	 * Loads {@link Page} instances for all {@link MultiPageImageCaptureScenario.Result} pages.
	 * The method calls {@link Listener#onNextPage(Page)} when the next page is loaded.
	 * The method is synchronous.
	 */
	@WorkerThread
	public void loadPagesBlocking( @NonNull Listener listener ) throws IOException
	{
		try {
			File directory = getPreparedDirectoryFile();
			writePagesToDirectory( directory, listener );
		} catch( IOException e ) {
			throw e;
		} catch( Exception e ) {
			throw new IOException( "Exception while working with scenario result", e );
		}
	}

	private void writePagesToDirectory( @NonNull File directory, @NonNull Listener listener ) throws Exception
	{
		Engine engine = SharedEngine.get();
		try( IImagingCoreAPI coreAPI = engine.createImagingCoreAPI() ) {
			for( String pageId : scenarioResult.getPages() ) {
				File imageFile = writePageToDirectory( pageId, directory, coreAPI );
				listener.onNextPage( new Page( pageId, imageFile ) );
			}
		}
	}

	private File writePageToDirectory(
		@NonNull String pageId,
		@NonNull File directory,
		@NonNull IImagingCoreAPI coreAPI
	) throws Exception
	{
		Bitmap bitmap = scenarioResult.loadImage( pageId );
		if( bitmap == null ) {
			throw new IOException( "Page bitmap doesn't exist" );
		}

		String filename = String.format( Locale.US, "file_%s", pageId );
		File file = new File( directory, filename );
		try( OutputStream outputStream = new BufferedOutputStream( new FileOutputStream( file ) ) ) {
			try( ExportToJpgOperation exportToJpgOperation = coreAPI.createExportToJpgOperation( outputStream ) ) {
				exportToJpgOperation.Compression = Compression.Low;
				exportToJpgOperation.addPage( bitmap );
				return file;
			}
		}
	}

	@NonNull
	private File getPreparedDirectoryFile() throws IOException
	{
		File directory = new File( context.getCacheDir(), directoryName );
		if( directory.exists() ) {
			deleteFilesInDirectory( directory );
		} else {
			if( !directory.mkdir() ) {
				throw new IOException( "Can't create directory in cache directory" );
			}
		}

		return directory;
	}

	private void deleteFilesInDirectory( @NonNull File directory ) throws IOException
	{
		File[] files = directory.listFiles();
		if( files == null ) {
			throw new IOException( "Supplied file does not refer to a directory" );
		}

		for( File file : files ) {
			boolean isFileDeleted = file.delete();
			if( !isFileDeleted ) {
				throw new IOException( "Can't delete file" );
			}
		}
	}
}
