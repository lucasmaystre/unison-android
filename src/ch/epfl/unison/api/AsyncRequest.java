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

import android.os.AsyncTask;

import com.google.gson.Gson;


public class AsyncRequest<T extends JsonStruct>
        extends AsyncTask<Void, Void, AsyncRequest.Result<T>> {

    static {
        // Close HTTP connections at end of request. Fixes some bugs in early
        // versions of Android.
        System.setProperty("http.keepAlive", "false");
    }

    private static final int CONNECT_TIMEOUT = 30 * 1000;  // In ms.
    private static final int READ_TIMEOUT = 30 * 1000;  // In ms.
    private static final Gson GSON = new Gson();

    private URL url;
    private UnisonAPI.Handler<T> handler;
    private Class<T> classOfT;

    private String method;
    private String auth;
    private Map<String, List<String>> data;

    private AsyncRequest(URL url, UnisonAPI.Handler<T> handler, Class<T> classOfT) {
        this.url = url;
        this.handler = handler;

        // We need this to be able to instantiate the correct JSONStruct.
        this.classOfT = classOfT;
    }

    public static <S extends JsonStruct> AsyncRequest<S> create(
            URL url, UnisonAPI.Handler<S> handler, Class<S> classOfS) {
        return new AsyncRequest<S>(url, handler, classOfS);
    }

    public AsyncRequest<T> addParam(String key, Object value) {
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

    public AsyncRequest<T> setAuth(String auth) {
        this.auth = auth;
        return this;
    }

    public void doGET() {
        this.method = "GET";
        this.execute();
    }

    public void doPOST() {
        this.method = "POST";
        this.execute();
    }

    public void doPUT() {
        this.method = "PUT";
        this.execute();
    }

    public void doDELETE() {
        this.method = "DELETE";
        this.execute();
    }

    @Override
    protected Result<T> doInBackground(Void...nothing) {
        HttpURLConnection conn = null;
        String response = null;

        try {
            conn = (HttpURLConnection) this.url.openConnection();
            conn.setRequestMethod(this.method);

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

    @Override
    protected void onPostExecute(Result<T> res) {
        if (res.result != null) {
            this.handler.callback(res.result);
        } else {
            this.handler.onError(res.error);
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
        private UnisonAPI.Error error;
        private S result;

        public Result(S result) {
            this.result = result;
        }

        public Result(UnisonAPI.Error error) {
            this.error = error;
        }
    }
}
