# Samples for ABBYY RTR SDK for Android

This folder contains code samples for [ABBYY Real-Time Recognition SDK](http://rtrsdk.com/) for Android.

## About ABBYY RTR SDK
ABBYY Real-Time Recognition SDK provides a technology for recognizing text directly on the smartphone's camera preview screen. Snapping a picture is not required.

The samples cover the following scenarios:

* Text Capture (sample-textcapture)

	*the basic usage scenario; the user simply points their camera at the text, which is then recognized and displayed or saved in memory*

## Prerequisites
To try out the real-time OCR technology using these samples, first download the library from [our site](http://rtrsdk.com/). 

The library is free: a fully-functional version for up to 5000 app downloads via Google Play.

In the download package you will find:


- the library itself (**libs/abbyy-rtr-sdk-1.0.aar**)
- the resource files:
	- **assets/dictionaries** — dictionary support for some of the recognition languages; using a dictionary improves the result quality
	- **assets/patterns** — recognition databases
- **license** — your license file and licensing agreement

**Note:** You are **not** allowed to include assets or license in any branch of this sample in public repositories. This notice must be included in all public branches. Anyone wishing to try out the samples should download their own copy of the library from the above link and use the license and assets from that copy.

## Building the samples
The samples need only a little configuring:

1. Move the license file from the **license** folder to the **assets** folder.
2. Check that the license name is specified correctly in the sample code:

        public class MainActivity extends Activity {
            //Licensing
            private static final String licenseFileName = "license";

3. Copy the **assets** folder into the **app/src/main** folder in the sample folder.
4. [optional] To save space, you may also want to remove from **assets/dictionaries** the dictionaries for recognition languages your application does not use.

## See also
You can find extensive documentation on ABBYY Real-Time Recognition SDK [here](http://rtrsdk.com/documentation).