package restyoureyes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

class Client extends WebSocketClient {
	private static final String HTTP_PROTOCOL = "http";
	private static final String WS_PROTOCOL = "ws";
	private static final String HOST = "localhost";
	private static final String REMAINING_ENDPOINT = "/remaining";
	private static final String PREFS_ENDPOINT = "/prefs";
	private static final String PAUSE_ENDPOINT = "/toggle";
	private static final String WS_ENDPOINT = "/websocket";

	private static URL prefsURL(int port) throws MalformedURLException {
		return new URL(HTTP_PROTOCOL, HOST, port, PREFS_ENDPOINT);
	}

	private static URL remainingURL(int port) throws MalformedURLException {
		return new URL(HTTP_PROTOCOL, HOST, port, REMAINING_ENDPOINT);
	}

	private static URL pauseURL(int port) throws MalformedURLException {
		return new URL(HTTP_PROTOCOL, HOST, port, PAUSE_ENDPOINT);
	}

	private static URI websocketURI(int port) {
		return URI.create(WS_PROTOCOL + "://" + HOST + ":" + port + WS_ENDPOINT);
	}

	private Gson gson;
	private int port;
	private closer closer;
	private messager messager;

	public void setCloser(Client.closer closer) {
		this.closer = closer;
	}

	public void setMessager(Client.messager messager) {
		this.messager = messager;
	}

	Client(int port) throws IOException {
		super(websocketURI(port));
		this.port = port;
		this.gson = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.UPPER_CAMEL_CASE).create();
		try {
			if (!connectBlocking()) {
				throw new IOException("could not connect to host");
			}
		} catch(InterruptedException iex) {
			throw new IOException("could not connect to host");
		}
	}

	Prefs getPrefs() throws java.io.IOException {
		InputStream in = prefsURL(port).openStream();
		Reader reader = new BufferedReader(new InputStreamReader(in));
		Prefs prefs = gson.fromJson(reader, Prefs.class);
		reader.close();
		return prefs;
	}

	// TODO split into individual methods setInterval setDarkTheme?
	void setPrefs(Prefs prefs) throws IOException {
		String json = gson.toJson(prefs);
		HttpURLConnection conn = (HttpURLConnection) prefsURL(port).openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
		conn.setRequestMethod("POST");
		OutputStream os = conn.getOutputStream();
		os.write(json.getBytes());
		os.close();
		System.out.println(conn.getResponseCode());
	}

	long getRemaining() throws IOException {
		InputStream in = remainingURL(port).openStream();
		Reader reader = new BufferedReader(new InputStreamReader(in));
		long remaining = gson.fromJson(reader, long.class);
		reader.close();
		return remaining;
	}

	void toggleReminders() throws IOException {
		InputStream in = pauseURL(port).openStream();
		Reader reader = new BufferedReader(new InputStreamReader(in));
		reader.close();
	}

	@Override
	public void onOpen(ServerHandshake handshakedata) {
		System.out.println("Connected to websocket host");
	}

	@Override
	public void onError(Exception ex) {
		ex.printStackTrace();
	}

	@Override
	public void onClose(int code, String reason, boolean remote) {
		if (closer != null) {
			closer.onClose(code, reason, remote);
		}
	}

	@Override
	public void onMessage(String message) {
		if (messager != null) {
			messager.onMessage(message);
		}
	}

	interface closer {
		void onClose(int code, String reason, boolean remote);
	}

	interface messager {
		void onMessage(String message);
	}
}
