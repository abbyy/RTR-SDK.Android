// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture;

import android.graphics.Bitmap;

/**
 * The captured image is stored application-wide to be retained across activities
 */
public class ImageHolder {
	private static Bitmap image;

	public static Bitmap getImage()
	{
		return image;
	}

	public static void setImage( Bitmap image )
	{
		ImageHolder.image = image;
	}
}
