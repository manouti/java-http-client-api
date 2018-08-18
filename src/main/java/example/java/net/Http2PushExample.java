package example.java.net;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandler;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.http.HttpResponse.PushPromiseHandler;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.function.Function;

import org.apache.log4j.Logger;

public class Http2PushExample {

	static Logger logger = Logger.getLogger(Http2PushExample.class.getName());

    static ExecutorService executor = Executors.newFixedThreadPool(20, new ThreadFactory() {
		
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			if(logger.isDebugEnabled()) {
				logger.debug("Created new executor " + (thread.isDaemon() ? "daemon " : "") + "thread with thread group: " + thread.getThreadGroup());
			}
			return thread;
		}
	});

	public static void main(String[] args) throws Exception {
		logger.info("Running HTTP/2 example with push promises...");
		try {
			HttpClient httpClient = HttpClient.newBuilder()
			              .version(Version.HTTP_2)
			              .proxy(ProxySelector.getDefault())
			              .build();

			long start = System.currentTimeMillis();

			HttpRequest mainRequest = HttpRequest.newBuilder()
					      .uri(URI.create("https://http2.akamai.com/demo/h2_demo_frame_sp2.html?pushnum=20"))
			              .build();

			List<Future<?>> futures = new ArrayList<>();

			Set<String> pushedImages = new HashSet<>();

			httpClient.sendAsync(mainRequest, BodyHandlers.ofString(), new PushPromiseHandler<String>() {

				@Override
				public void applyPushPromise(HttpRequest initiatingRequest, HttpRequest pushPromiseRequest,
						Function<BodyHandler<String>, CompletableFuture<HttpResponse<String>>> acceptor) {
					logger.info("Got promise request " + pushPromiseRequest.uri());
					acceptor.apply(BodyHandlers.ofString()).thenAccept(resp -> {
						logger.info("Got pushed response " + resp.uri());
						pushedImages.add(resp.uri().toString());
					});
				}
			}).thenAccept(mainResponse -> {

				logger.info("Main response status code: " + mainResponse.statusCode());
				logger.info("Main response headers: " + mainResponse.headers());
				String responseBody = mainResponse.body();
				logger.info(responseBody);

				// For each image resource in the main HTML, send a request on a separate thread
				responseBody.lines()
				            .filter(line -> line.trim().startsWith("<img height"))
				            .map(line -> line.substring(line.indexOf("src='") + 5, line.indexOf("'/>")))
				            .filter(image -> !pushedImages.contains("https://http2.akamai.com" + image))
				            .forEach(image -> {

						Future<?> imgFuture = executor.submit(() -> {
							HttpRequest imgRequest = HttpRequest.newBuilder()
								      .uri(URI.create("https://http2.akamai.com" + image))
						              .build();
							try {
								HttpResponse<String> imageResponse = httpClient.send(imgRequest, BodyHandlers.ofString());
								logger.info("Loaded " + image + ", status code: " + imageResponse.statusCode());
							} catch (IOException | InterruptedException ex) {
								logger.error("Error during image request for " + image, ex);
							}
						});
						futures.add(imgFuture);
						logger.info("Submitted future for image " + image);
					});

			}).join();

			// Wait for all submitted image loads to be completed
			futures.forEach(f -> {
				try {
					f.get();
				} catch (InterruptedException | ExecutionException ex) {
					logger.error("Error waiting for image load", ex);
				}
			});

			long end = System.currentTimeMillis();
			logger.info("Total load time: " + (end - start) + " ms");

		} finally {
			executor.shutdown();
		}
	}

}
