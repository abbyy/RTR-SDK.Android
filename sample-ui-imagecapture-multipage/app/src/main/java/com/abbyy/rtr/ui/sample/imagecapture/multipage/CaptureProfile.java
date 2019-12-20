// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.multipage;

import com.abbyy.mobile.uicomponents.scenario.ImageCaptureScenario;

public enum CaptureProfile {
	ANY_DOCUMENTS( ImageCaptureScenario.DocumentSize.ANY, "Any", "any", 0f, 0f ), // 0f is default aspect ratio value
	A4( ImageCaptureScenario.DocumentSize.A4, "A4", "a4", 0f, 0f ),
	LETTER( ImageCaptureScenario.DocumentSize.LETTER, "Letter", "letter", 0f, 0f ),
	// Business cards have different aspect ratios
	BUSINESS_CARD( ImageCaptureScenario.DocumentSize.BUSINESS_CARD, "Business card", "business_card", 1.4f, 1.9f );

	CaptureProfile(
		ImageCaptureScenario.DocumentSize documentSize,
		String displayText,
		String directoryName,
		float aspectRatioMin,
		float aspectRatioMax
	)
	{
		this.documentSize = documentSize;
		this.displayText = displayText;
		this.directoryName = directoryName;
		this.aspectRatioMin = aspectRatioMin;
		this.aspectRatioMax = aspectRatioMax;
	}

	public final ImageCaptureScenario.DocumentSize documentSize;
	public final String displayText;
	public final String directoryName;
	public final float aspectRatioMin;
	public final float aspectRatioMax;

}
