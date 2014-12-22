package co.onemeter.oneapp.liveplayer;

import java.io.IOException;
import java.net.HttpURLConnection;

import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * okHttp
 */
public class MyHttpUtil {
	private static OkHttpClient client = new OkHttpClient();

	public static String run(String url) throws IOException {
		Request request = new Request.Builder().url(url).build();

		Response response = client.newCall(request).execute();
		switch (response.code()) {
		case HttpURLConnection.HTTP_OK:
			L.e(response.message());
			break;
		case HttpURLConnection.HTTP_UNAUTHORIZED:
			L.e(response.message());
			break;
		case HttpURLConnection.HTTP_FORBIDDEN:
			L.e(response.message());
			break;
		case HttpURLConnection.HTTP_INTERNAL_ERROR:
			L.e(response.message());
			break;
		default:
			break;
		}
		return response.body().string();
	}

}
