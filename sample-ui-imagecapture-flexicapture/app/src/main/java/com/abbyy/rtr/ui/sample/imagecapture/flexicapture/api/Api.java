// ABBYY® Mobile Capture © 2019 ABBYY Production LLC.
// ABBYY is a registered trademark or a trademark of ABBYY Software Ltd.

package com.abbyy.rtr.ui.sample.imagecapture.flexicapture.api;

import androidx.annotation.NonNull;
import okhttp3.MultipartBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.POST;
import retrofit2.http.Query;

/**
 * FlexiCapture Mobile REST API declaration for retrofit library.
 * API documentation: https://help.abbyy.com/en-us/flexicapture/12/developer/mobileapi
 */
public interface Api {

	String AUTH_TICKET_HEADER = "AuthTicket";

	String API_SUFFIX = "flexicapture12/Server/MobileApp/v1/";

	@GET( API_SUFFIX )
	Call<Parameters> getProjects(
		@Query( "Tenant" ) String tenant,
		@Header( "Authorization" ) String basicAuthString
	);

	@POST( API_SUFFIX )
	Call<Void> sendDocument(
		@Query( "Tenant" ) String tenant,
		@Header( "Authorization" ) String basicAuthString,
		@Body MultipartBody files
	);

	/**
	 * Gets authorization header from the auth ticket.
	 */
	static String getBasic( @NonNull String authTicket )
	{
		return "Bearer " + authTicket;
	}

}
