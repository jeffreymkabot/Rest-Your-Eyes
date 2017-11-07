package restyoureyes;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import com.google.gson.Gson;

public class Client {
	private static final String PROTOCOL = "http://";
	private static final String HOST = "localhost";
	private static final String PORT = "8080";
	private static final String REMAINING_ENDPOINT = "/remaining";
	private static final String PREFS_ENDPOINT = "/prefs";

	private static URL prefsURL() throws java.net.MalformedURLException {
		return new URL(PROTOCOL + HOST + ":" + PORT + PREFS_ENDPOINT);
	}

	private static URL remainingURL() throws java.net.MalformedURLException {
		return new URL(PROTOCOL + HOST + ":" + PORT + REMAINING_ENDPOINT);
	}

	public static Prefs getPrefs() {
		try {
			InputStream in = prefsURL().openStream();
			Reader reader = new BufferedReader(new InputStreamReader(in));
			Gson gson = new Gson();
			Prefs prefs = gson.fromJson(reader, Prefs.class);
			reader.close();
			return prefs;
		} catch(IOException ex) {
			ex.printStackTrace();
			return Prefs.defaultPrefs();
		}
	}

	// TODO split into individual methods setInterval setDarkTheme?
	public static void setPrefs(Prefs prefs) throws IOException {
		Gson gson = new Gson();
		String json = gson.toJson(prefs);
		HttpURLConnection conn = (HttpURLConnection) prefsURL().openConnection();
		conn.setDoOutput(true);
		conn.setRequestMethod("POST");
		OutputStream os = conn.getOutputStream();
		os.write(json.getBytes());
		os.close();
		String response = "";
//		InputStream in = conn.getInputStream();
		System.out.println(conn.getResponseCode());
	}

	public static long getRemaining() {
		try {
			InputStream in = remainingURL().openStream();
			Reader reader = new BufferedReader(new InputStreamReader(in));
			Gson gson = new Gson();
			long remaining = gson.fromJson(reader, long.class);
			reader.close();
			return remaining;
		} catch (IOException e) {
			e.printStackTrace();
			return Long.MAX_VALUE;
		}
	}
}
