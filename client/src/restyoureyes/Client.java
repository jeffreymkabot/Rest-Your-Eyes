package restyoureyes;

import restyoureyes.Prefs;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;

public class Client {
	private static final String HOST = "localhost";
	private static final String PORT = "8080";
	private static final String REMAINING_ENDPOINT = "/remaining";
	private static final String PREFS_ENDPOINT = "/prefs";

	private static final gson = new Gson();

	private static URL prefsURL() {
		return new URL(HOST + ":" + PORT + PREFS_ENDPOINT);
	}

	private static String remainingURL() {
		return new URL(HOST + ":" + PORT + REMAINING_ENDPOINT);
	}

	public static Prefs getPrefs() {
		InputStream in = prefsURL().openStream();
		Reader reader = new BufferedReader(new InputStreamReader(is));
		Prefs prefs = gson.fromJson(reader, Prefs.class);
		reader.close()
		return prefs;	
	}

	// TODO split into individual methods setInterval setDarkTheme
	public static void setPrefs(Prefs prefs) {
		String json = gson.toJson(prefs);
		HttpURLConnection conn = (HttpURLConnection) prefsURL().openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("PATCH");
		OutputStream os = conn.getOutputStream();
		os.write(json.getBytes());
	}

	public static long getRemaining() {
		InputStream in = remainingURL().openStream();
		Reader reader = new BufferedReader(new InputStreamReader(is));
		long remaining = gson.fromJson(reader, long.class);
		reader.close()
		return remaining;
	}
}