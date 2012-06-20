package ch.epfl.unison.api;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.util.Log;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class Request<T extends JsonStruct> {

    private static final String TAG = "ch.epfl.unison.Request";

    static {
        // Close HTTP connections at end of request. Fixes some bugs in early
        // versions of Android.
        System.setProperty("http.keepAlive", "false");
    }

    private static final int CONNECT_TIMEOUT = 30 * 1000;  // In ms.
    private static final int READ_TIMEOUT = 30 * 1000;  // In ms.
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private URL url;
    private Class<T> classOfT;

    private String auth;
    private Map<String, List<String>> data;

    private Request(URL url, Class<T> classOfT) {
        this.url = url;

        // We need this to be able to instantiate the correct JSONStruct.
        this.classOfT = classOfT;
    }

    public static <S extends JsonStruct> Request<S> of(
            URL url, Class<S> classOfS) {
        return new Request<S>(url, classOfS);
    }

    public Request<T> addParam(String key, Object value) {
        if (this.data == null) {
            // This is the first parameter. Initialize the map.
            this.data = new HashMap<String, List<String>>();
        }
        if (!this.data.containsKey(key)) {
            // First value for this key. Initialize the list of values.
            this.data.put(key, new LinkedList<String>());
        }
        this.data.get(key).add(value.toString());
        return this;
    }

    public Request<T> setAuth(String auth) {
        this.auth = auth;
        return this;
    }

    public Result<T> doGET() {
        return this.execute("GET");
    }

    public Result<T> doPOST() {
        return this.execute("POST");
    }

    public Result<T> doPUT() {
        return this.execute("PUT");
    }

    public Result<T> doDELETE() {
        return this.execute("DELETE");
    }

    private Result<T> execute(String method) {
        Log.i(TAG, String.format("%s request to %s", method, this.url.toString()));
        HttpURLConnection conn = null;
        String response = null;

        try {
            conn = (HttpURLConnection) this.url.openConnection();
            conn.setRequestMethod(method);

            // Configure some sensible defaults.
            conn.setConnectTimeout(CONNECT_TIMEOUT);
            conn.setReadTimeout(READ_TIMEOUT);
            conn.setInstanceFollowRedirects(false);

            if (this.auth != null) {
                // Set a raw HTTP Basic Auth header (java.net.Authenticator has issues).
                conn.setRequestProperty("Authorization", "Basic " + this.auth);
            }

            if (this.data != null) {
                // Write out the request body (i.e. the form data).
                conn.setDoOutput(true);
                String data = generateQueryString(this.data);
                OutputStreamWriter out = new OutputStreamWriter(conn.getOutputStream());
                out.write(data);
                out.close();
            }

            try {
                // Get the response as a string.
                response = streamToString(conn.getInputStream());
            } catch(IOException ioe) {
                // Happens when the server returns an error status code.
                response = streamToString(conn.getErrorStream());
            }

            if (conn.getResponseCode() < 200 || conn.getResponseCode() > 299) {
                // We didn't receive a 2xx status code - we treat it as an error.
                JsonStruct.Error jsonError = GSON.fromJson(response, JsonStruct.Error.class);
                return new Result<T>(new UnisonAPI.Error(conn.getResponseCode(),
                        conn.getResponseMessage(), response, jsonError));
            } else {
                // Success.
                T jsonStruct = GSON.fromJson(response, this.classOfT);
                return new Result<T>(jsonStruct);
            }

        } catch (Exception e) {
            // Under this catch-all, we mean:
            // - IOException, thrown by most HttpURLConnection methods,
            // - NullPointerException. if there's not InputStream nor ErrorStream,
            // - JsonSyntaxException, if we fail to decode the server's response.
            Log.w(TAG, "caught exception while handling request", e);
            int statusCode = 0;
            String statusMessage = null;
            try {
                statusCode = conn.getResponseCode();
                statusMessage = conn.getResponseMessage();
            } catch(Exception foobar) {}

            return new Result<T>(new UnisonAPI.Error(statusCode, statusMessage, response, e));

        } finally {
            conn.disconnect();
        }
    }

    private static String generateQueryString(Map<String, List<String>> data) {
        StringBuilder builder = new StringBuilder();
        boolean isFirst = true;
        for (Map.Entry<String, List<String>> entry : data.entrySet()) {
            String encKey = URLEncoder.encode(entry.getKey());
            for (String value : entry.getValue()) {
                String encValue = URLEncoder.encode(value);
                if (isFirst) {
                    builder.append(encKey + '=' + encValue);
                    isFirst = false;
                } else {
                    builder.append('&' + encKey + '=' + encValue);
                }
            }
        }
        return builder.toString();
    }

    private static String streamToString(InputStream stream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
        StringBuilder builder = new StringBuilder();
        String line;
        while((line = reader.readLine()) != null) {
            builder.append(line).append('\n');
        }
        return builder.toString();
    }

    public static class Result<S> {
        public final UnisonAPI.Error error;
        public final S result;

        private Result(S result, UnisonAPI.Error error) {
            this.result = result;
            this.error = error;
        }

        public Result(S result) {
            this(result, null);
        }

        public Result(UnisonAPI.Error error) {
            this(null, error);
        }
    }

}
