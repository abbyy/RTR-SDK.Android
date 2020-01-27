// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

import java.util.List;

import okhttp3.MultipartBody;

/**
 * Parameters of FlexiCapture user.
 * Used in document sending method {@link Api#sendDocument(String, String, MultipartBody)}.
 */
public class Parameters {

	@SerializedName( "projects" )
	@Expose
	private List<Project> projects;

	public List<Project> getProjects()
	{
		return projects;
	}

	public void setProjects( List<Project> projects )
	{
		this.projects = projects;
	}
}
