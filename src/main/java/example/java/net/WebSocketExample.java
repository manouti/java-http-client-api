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

import org.apache.log4j.Logger;

public class WebSocketExample {

	static Logger logger = Logger.getLogger(WebSocketExample.class.getName());

    static ExecutorService executor = Executors.newFixedThreadPool(3, new ThreadFactory() {

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
		logger.info("Running WebSocket example...");

		HttpClient httpClient = HttpClient.newBuilder().executor(executor).build();
		Builder webSocketBuilder = httpClient.newWebSocketBuilder();
		WebSocket webSocket = webSocketBuilder.buildAsync(URI.create("wss://echo.websocket.org"), new WebSocketListener()).join();
		logger.info("WebSocket created");

		Thread.sleep(1000);
		webSocket.sendClose(WebSocket.NORMAL_CLOSURE, "ok").thenRun(() -> logger.info("Sent close"));
	}


	private static class WebSocketListener implements Listener {

		@Override
		public void onOpen(WebSocket webSocket) {
			logger.info("CONNECTED");
			webSocket.sendText("This is a message", true);
			Listener.super.onOpen(webSocket);
		}

		@Override
		public void onError(WebSocket webSocket, Throwable error) {
			logger.error("Error occurred", error);
			Listener.super.onError(webSocket, error);
		}
		@Override
		public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
			logger.info("onText received with data " + data);
			if(!webSocket.isOutputClosed()) {
				webSocket.sendText("This is a message", true);
			}
			return Listener.super.onText(webSocket, data, last);
		}
		
		@Override
		public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
			logger.info("Closed with status " + statusCode + ", reason: " + reason);
			executor.shutdown();
			return Listener.super.onClose(webSocket, statusCode, reason);
		}
	}
}
