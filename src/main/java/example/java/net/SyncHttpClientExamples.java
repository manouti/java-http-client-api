package example.java.net;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

public class SyncHttpClientExamples {

	public static void main(String[] args) throws Exception {
		System.out.println("Running...");
		getRequest();
	}

	static void getRequest() throws IOException, InterruptedException {
		HttpClient httpClient = HttpClient.newBuilder()
		              .version(Version.HTTP_2)
		              .proxy(ProxySelector.getDefault())
		              .build();
		
		HttpRequest request = HttpRequest.newBuilder()
				      .uri(URI.create("https://http2.github.io/"))
		              .build();

		long start = System.currentTimeMillis();
		HttpResponse<String> response = httpClient.send(request, BodyHandlers.ofString());
		long end = System.currentTimeMillis();
		
		System.out.println(response.statusCode());
		System.out.println(response.headers());
		System.out.println(response.body());
		System.out.println("Total latency: " + (end - start) + " ms");
	}
}
