package restyoureyes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import com.google.gson.Gson;

import javax.websocket.*;

@ClientEndpoint
public class Client {
	private static final String HTTP_PROTOCOL = "http://";
	private static final String WS_PROTOCOL = "ws://";
	private static final String HOST = "localhost";
	private static final String REMAINING_ENDPOINT = "/remaining";
	private static final String PREFS_ENDPOINT = "/prefs";
	private static final String PAUSE_ENDPOINT = "/toggle";
	private static final String WS_ENDPOINT = "/websocket";

	private URL prefsURL() throws java.net.MalformedURLException {
		return new URL(HTTP_PROTOCOL + HOST + ":" + port + PREFS_ENDPOINT);
	}

	private URL remainingURL() throws java.net.MalformedURLException {
		return new URL(HTTP_PROTOCOL + HOST + ":" + port + REMAINING_ENDPOINT);
	}

	private URL pauseURL() throws java.net.MalformedURLException {
		return new URL(HTTP_PROTOCOL + HOST + ":" + port + PAUSE_ENDPOINT);
	}

	private URI websocketURI() throws java.net.URISyntaxException {
		return new URI(WS_PROTOCOL + HOST + ":" + port + WS_ENDPOINT);
	}

	private String port;
	private Session session;

	public Client(String port) throws java.net.URISyntaxException, java.io.IOException, javax.websocket.DeploymentException {
		this.port = port;
//		WebSocketContainer container = ContainerProvider.getWebSocketContainer();
//		session = container.connectToServer(this, websocketURI());
	}

	public Prefs getPrefs() throws java.io.IOException {
		InputStream in = prefsURL().openStream();
		Reader reader = new BufferedReader(new InputStreamReader(in));
		Gson gson = new Gson();
		Prefs prefs = gson.fromJson(reader, Prefs.class);
		reader.close();
		return prefs;
	}

	// TODO split into individual methods setInterval setDarkTheme?
	public void setPrefs(Prefs prefs) throws IOException {
		Gson gson = new Gson();
		String json = gson.toJson(prefs);
		HttpURLConnection conn = (HttpURLConnection) prefsURL().openConnection();
		conn.setDoOutput(true);
		conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
		conn.setRequestMethod("POST");
		OutputStream os = conn.getOutputStream();
		os.write(json.getBytes());
		os.close();
		System.out.println(conn.getResponseCode());
	}

	public long getRemaining() throws IOException {
		InputStream in = remainingURL().openStream();
		Reader reader = new BufferedReader(new InputStreamReader(in));
		Gson gson = new Gson();
		long remaining = gson.fromJson(reader, long.class);
		reader.close();
		return remaining;
	}

	public void toggleReminders() throws IOException {
		InputStream in = pauseURL().openStream();
		Reader reader = new BufferedReader(new InputStreamReader(in));
		reader.close();
	}

	@OnMessage
	public void onMessage(String message) {
		System.out.println(message);
	}

	@OnClose
	public void onClose() {

	}
}
