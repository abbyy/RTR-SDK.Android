// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.multipage.utils;

import android.graphics.Bitmap;
import android.graphics.Matrix;

/**
 * Utility class for handling & storing images
 */
public class ImageUtils {

	public static Bitmap getMiniature( Bitmap page, int miniatureSize )
	{
		int width = page.getWidth();
		int height = page.getHeight();
		float scaleWidth = ( (float) miniatureSize ) / width;
		float scaleHeight = ( (float) miniatureSize ) / height;
		float scale = Math.max( scaleWidth, scaleHeight );

		Matrix matrix = new Matrix();
		matrix.postScale( scale, scale );

		return Bitmap.createBitmap( page, 0, 0, width, height, matrix, false );
	}
}
