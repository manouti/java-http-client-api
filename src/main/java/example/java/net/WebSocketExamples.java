package example.java.net;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.net.http.WebSocket.Builder;
import java.net.http.WebSocket.Listener;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

public class WebSocketExamples {

    static ExecutorService executor = Executors.newFixedThreadPool(3, new ThreadFactory() {
		
		@Override
		public Thread newThread(Runnable r) {
			Thread thread = new Thread(r);
			System.err.println("Created new thread with daemon: " + thread.isDaemon() + " and group: " + thread.getThreadGroup());
			return thread;
		}
	});

	public static void main(String[] args) throws Exception {
		System.out.println("Running...");
		webSocketExample();
	}

	static void webSocketExample() throws InterruptedException {
		HttpClient httpClient = HttpClient.newBuilder().executor(executor).build();
		Builder webSocketBuilder = httpClient.newWebSocketBuilder();
		WebSocket webSocket = webSocketBuilder.buildAsync(URI.create("wss://echo.websocket.org"), new WebSocketListener()).join();
		System.out.println("WebSocket created");
		
		Thread.sleep(10000);
		webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok").thenRun(() -> System.err.println("Sent close"));
	}

	private static class WebSocketListener implements Listener {

		@Override
		public void onOpen(WebSocket webSocket) {
			System.out.println("CONNECTED");
			webSocket.sendText("This is a message", true);
			Listener.super.onOpen(webSocket);
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			error.printStackTrace();
			Listener.super.onError(webSocket, error);
		}
		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			System.out.println(data);
			if(!webSocket.isOutputClosed()) {
				webSocket.sendText("This is a message", true);
			}
			return Listener.super.onText(webSocket, data, last);
		}
		
		@Override
		public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			System.out.println("Closed with status " + statusCode + ", reason: " + reason);
			executor.shutdown();
			return Listener.super.onClose(webSocket, statusCode, reason);
		}
	}
}
