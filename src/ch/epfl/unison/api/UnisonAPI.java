package ch.epfl.unison.api;

import java.net.MalformedURLException;
import java.net.URL;

import android.util.Base64;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class UnisonAPI {

    @SuppressWarnings("unused")
    private static final String TAG = "ch.epfl.unison.UnisonAPI";

    private static final String API_ROOT = "https://api.groupstreamer.com";
    private static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    private String auth;

    /** No-args constructor for use without authentication. */
    public UnisonAPI() {}

    public UnisonAPI(String email, String password) {
        this.setAuth(email, password);
    }

    public UnisonAPI setAuth(String email, String password) {
        // It is safe to call getBytes without charset, as the default (on Android) is UTF-8.
        String encEmail = Base64.encodeToString(email.getBytes(), Base64.NO_WRAP);
        String encPassword = Base64.encodeToString(password.getBytes(), Base64.NO_WRAP);

        // At this point, it should be encoded using ISO-8859-1, but the string is ASCII anyways.
        this.auth = Base64.encodeToString((encEmail + ':' + encPassword).getBytes(), Base64.NO_WRAP);
        return this;
    }

    public void login(Handler<JsonStruct.User> handler) {
        URL url = urlFor("/");
        AsyncRequest.of(url, handler, JsonStruct.User.class)
                .setAuth(this.auth).doGET();
    }

    public void createUser(String email, String password,
            Handler<JsonStruct.User> handler) {
        URL url = urlFor("/users");
        AsyncRequest.of(url, handler, JsonStruct.User.class)
                .addParam("email", email).addParam("password", password).doPOST();
    }

    public void getNickname(long uid, Handler<JsonStruct.User> handler) {
        URL url = urlFor("/users/%d/nickname", uid);
        AsyncRequest.of(url, handler, JsonStruct.User.class)
                .setAuth(this.auth).doGET();
    }

    public void setNickname(long uid, String nickname, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/nickname", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("nickname", nickname).setAuth(this.auth).doPUT();
    }

    public void setEmail(long uid, String email, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/email", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("email", email).setAuth(this.auth).doPUT();
    }

    public void setPassword(long uid, String password, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/password", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("password", password).setAuth(this.auth).doPUT();
    }

    public void joinGroup(long uid, long gid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/group", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("gid", gid).setAuth(this.auth).doPUT();
    }

    public void leaveGroup(long uid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/group", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .setAuth(this.auth).doDELETE();
    }

    public void listGroups(Handler<JsonStruct.GroupsList> handler) {
        URL url = urlFor("/groups");
        AsyncRequest.of(url, handler, JsonStruct.GroupsList.class)
                .setAuth(this.auth).doGET();
    }

    public void listGroups(double lat, double lon, Handler<JsonStruct.GroupsList> handler) {
        URL url = urlFor("/groups?lat=%f&lon=%f", lat, lon);
        AsyncRequest.of(url, handler, JsonStruct.GroupsList.class)
                .setAuth(this.auth).doGET();
    }

    public void createGroup(String name, double lat, double lon,
            Handler<JsonStruct.GroupsList> handler) {
        URL url = urlFor("/groups");
        AsyncRequest.of(url, handler, JsonStruct.GroupsList.class)
                .addParam("name", name).addParam("lat", lat)
                .addParam("lon", lon).setAuth(this.auth).doPOST();
    }

    public void getGroupInfo(long gid, Handler<JsonStruct.Group> handler) {
        URL url = urlFor("/groups/%d", gid);
        AsyncRequest.of(url, handler, JsonStruct.Group.class)
                .setAuth(this.auth).doGET();
    }

    public void getNextTracks(long gid, Handler<JsonStruct.TracksList> handler) {
        URL url = urlFor("/groups/%d/tracks", gid);
        AsyncRequest.of(url, handler, JsonStruct.TracksList.class)
                .setAuth(this.auth).doGET();
    }

    public void getPlaylistId(long gid, final Handler<JsonStruct.TracksList> handler) {
        URL url = urlFor("/groups/%d/playlist", gid);
        AsyncRequest.of(url, handler, JsonStruct.TracksList.class)
                .setAuth(this.auth).doGET();
    }

    public void setCurrentTrack(long gid, String artist, String title,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/current", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("artist", artist).addParam("title", title)
                .setAuth(this.auth).doPUT();
    }

    public void skipTrack(long gid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/current", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .setAuth(this.auth).doDELETE();
    }

    public void instantRate(long gid, String artist, String title, int rating,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/ratings", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("artist", artist).addParam("title", title).addParam("rating", rating)
                .setAuth(this.auth).doPOST();
    }

    public void becomeMaster(long gid, long uid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/master", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .addParam("uid", uid).setAuth(this.auth).doPUT();
    }

    public void resignMaster(long gid, long uid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/groups/%d/master", gid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class)
                .setAuth(this.auth).doDELETE();
    }

    public Request.Result<JsonStruct.Success> uploadLibrarySync(
            long uid, Iterable<JsonStruct.Track> tracks) {
        URL url = urlFor("/libentries/%d", uid);
        Request<JsonStruct.Success> request = Request.of(url, JsonStruct.Success.class)
                .setAuth(this.auth);
        for (JsonStruct.Track track : tracks) {
            request.addParam("entry", GSON.toJson(track));
        }
        return request.doPUT();
    }

    public Request.Result<JsonStruct.Success> updateLibrarySync(
            long uid, Iterable<JsonStruct.Delta> deltas) {
        URL url = urlFor("/libentries/%d/batch", uid);
        Request<JsonStruct.Success> request = Request.of(url, JsonStruct.Success.class)
                .setAuth(this.auth);
        for (JsonStruct.Delta delta : deltas) {
            request.addParam("delta", GSON.toJson(delta));
        }
        return request.doPOST();
    }

    public void getRatings(long uid, Handler<JsonStruct.TracksList> handler) {
        URL url = urlFor("/libentries/%d/ratings", uid);
        AsyncRequest.of(url, handler, JsonStruct.TracksList.class)
                .setAuth(this.auth).doGET();
    }

    public void rate(long uid, String artist, String title, int rating,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/libentries/%d/ratings", uid);
        AsyncRequest.of(url, handler, JsonStruct.Success.class).setAuth(this.auth)
                .addParam("artist", artist).addParam("title", title)
                .addParam("rating", rating).doPOST();
    }

    private static URL urlFor(String suffix, Object... objects) {
        try {
            return new URL(API_ROOT + String.format(suffix, objects));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static interface Handler<S> {
        public void callback(S struct);
        public void onError(UnisonAPI.Error error);
    }

    public static class Error {
        public final int statusCode;
        public final String statusMessage;
        public final String response;

        public final Throwable error;
        public final JsonStruct.Error jsonError;

        public Error(int statusCode, String statusMessage, String response,
                JsonStruct.Error jsonError) {
            this(statusCode, statusMessage, response, jsonError, null);
        }

        public Error(int statusCode, String statusMessage, String response,
                Throwable error) {
            this(statusCode, statusMessage, response, null, error);
        }

        private Error(int statusCode, String statusMessage, String response,
                JsonStruct.Error jsonError, Throwable error) {
            this.statusCode = statusCode;
            this.statusMessage = statusMessage;
            this.response = response;
            this.jsonError = jsonError;
            this.error = error;
        }

        public boolean hasJsonError() {
            return this.jsonError != null;
        }

        @Override
        public String toString() {
            if (this.hasJsonError()) {
                return String.format("JSON error:\ncode: %d\nmessage: %s",
                        this.jsonError.error, this.jsonError.message);
            } else {
                return String.format("Error type: %s\nstatus: %d\nresponse: %s",
                        this.error.getClass().toString(), this.statusCode, this.response);
            }
        }
    }

    /** Corresponds to JSON error codes - synced with back-end. */
    public static interface ErrorCodes {
        public static final int MISSING_FIELD = 1;
        public static final int EXISTING_USER = 2;
        public static final int INVALID_EMAIL = 4;
        public static final int INVALID_PASSWORD = 5;
    }

}
