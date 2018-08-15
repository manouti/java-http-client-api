package example.java.net;

import java.io.IOException;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class AsyncHttpClientExamples {

	static ExecutorService executor = Executors.newFixedThreadPool(10, new ThreadFactory() {
		
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			thread.setDaemon(false);
			System.err.println("Created new thread with daemon: " + thread.isDaemon() + " and group: " + thread.getThreadGroup());
			return thread;
		}
	});

	public static void main(String[] args) throws Exception {
		System.out.println("Running...");
		try {
			getRequestAsyncWithCustomExecutor();
		} finally {
			executor.shutdown();
		}
	}

	static void getRequestAsyncWithCustomExecutor() throws IOException, InterruptedException {
		HttpClient httpClient = HttpClient.newBuilder()
		              .version(Version.HTTP_2)
		              .proxy(ProxySelector.getDefault())
		              .executor(executor)
		              .build();
		
		HttpRequest request = HttpRequest.newBuilder()
				      .uri(URI.create("https://http2.github.io/"))
		              .build();

		long start = System.currentTimeMillis();
		CompletableFuture<HttpResponse<String>> future = httpClient.sendAsync(request, BodyHandlers.ofString());
		System.out.println("a");

		future.thenApply(response -> {
			long end = System.currentTimeMillis();

			System.out.println("Thread is daemon: " + Thread.currentThread().isDaemon());
			System.out.println(response.statusCode());
			System.out.println(response.headers());
			System.out.println(response.body());
			System.out.println("Total latency: " + (end - start) + " ms");
			return null;
		}).join();
	}
}
