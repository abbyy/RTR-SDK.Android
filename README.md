
# Samples for ABBYY RTR SDK for Android

This folder contains code samples for [ABBYY Real-Time Recognition SDK](http://rtrsdk.com/) for Android.



## About ABBYY RTR SDK

ABBYY Real-Time Recognition SDK provides technology for recognizing text directly on the smartphone's camera preview screen. Snapping a picture is not required.

The samples cover the following scenarios:

- Text capture (**sample-textcapture**)

  The basic usage scenario. The user simply points their camera at the text, which is then recognized and displayed or saved in memory.

- Data capture (**sample-datacapture**)

  Custom data field capture: only the data that matches the specified regular expression will be extracted.



## Prerequisites

To try out the real-time OCR technology using these samples, first download the library from [our site](http://rtrsdk.com/).

The library is free: a fully-functional version for up to 5000 app downloads via Google Play.

In the download package you will find:

- the library itself (**libs/abbyy-rtr-sdk-1.0.aar**)
- resource files:
  - **assets/dictionaries** — dictionary support for some of the recognition languages; using a dictionary improves the result quality
  - **assets/patterns** — recognition databases
- **License** — your license file and license agreement

**Note:** You are **not allowed** to include assets or license in any branch of this sample in public repositories. This notice must be included in all public branches. Anyone wishing to try out the samples should download their own copy of the library from the above link and use the license and assets from that copy.



## Building the samples

Please change the application ID before building, modifying or otherwise using any of the samples.

The samples should be open and built from the same folder where they are located in the distribution package. All samples work out of the box.



## See also

You can find the full ABBYY Real-Time Recognition SDK documentation [here](http://rtrsdk.com/documentation).
