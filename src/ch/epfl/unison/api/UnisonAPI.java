package ch.epfl.unison.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;

import android.util.Base64;

public class UnisonAPI {

    private static final String API_ROOT = "https://api.groupstreamer.com";

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
        AsyncRequest.create(url, handler, JsonStruct.User.class)
                .setAuth(this.auth).doGET();
    }

    public void createUser(String email, String password,
            Handler<JsonStruct.User> handler) {
        URL url = urlFor("/users");
        AsyncRequest.create(url, handler, JsonStruct.User.class)
                .addParam("email", email).addParam("password", password).doPOST();
    }

    public void getNickname(long uid, Handler<JsonStruct.User> handler) {
        URL url = urlFor("/users/%d/nickname", uid);
        AsyncRequest.create(url, handler, JsonStruct.User.class)
                .setAuth(this.auth).doGET();
    }

    public void setNickname(long uid, String nickname, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/nickname", uid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .addParam("nickname", nickname).setAuth(this.auth).doPUT();
    }

    public void setEmail(long uid, String email, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/email", uid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .addParam("email", email).setAuth(this.auth).doPUT();
    }

    public void setPassword(long uid, String password, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/password", uid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .addParam("password", password).setAuth(this.auth).doPUT();
    }

    public void joinRoom(long uid, long rid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/room", uid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .addParam("rid", rid).setAuth(this.auth).doPUT();
    }

    public void leaveRoom(long uid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/users/%d/room", uid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .setAuth(this.auth).doDELETE();
    }

    public void listRooms(Handler<JsonStruct.RoomsList> handler) {
        URL url = urlFor("/rooms");
        AsyncRequest.create(url, handler, JsonStruct.RoomsList.class)
                .setAuth(this.auth).doGET();
    }

    public void createRoom(String name, Handler<JsonStruct.RoomsList> handler) {
        URL url = urlFor("/rooms");
        AsyncRequest.create(url, handler, JsonStruct.RoomsList.class)
                .addParam("name", name).setAuth(this.auth).doPOST();
    }

    public void getRoomInfo(long rid, Handler<JsonStruct.Room> handler) {
        URL url = urlFor("/rooms/%d", rid);
        AsyncRequest.create(url, handler, JsonStruct.Room.class)
                .setAuth(this.auth).doGET();
    }

    public void getNextTrack(long rid, Handler<JsonStruct.Track> handler) {
        URL url = urlFor("/rooms/%d", rid);
        AsyncRequest.create(url, handler, JsonStruct.Track.class)
                .setAuth(this.auth).doPOST();
    }

    public void setCurrentTrack(long rid, String artist, String title,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/rooms/%d/current", rid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .addParam("artist", artist).addParam("title", title)
                .setAuth(this.auth).doPOST();
    }

    public void skipTrack(long rid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/rooms/%d/current", rid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .setAuth(this.auth).doDELETE();
    }

    public void instantRate(long rid, String artist, String title, int rating,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/rooms/%d/ratings", rid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .addParam("artist", artist).addParam("title", title).addParam("rating", rating)
                .setAuth(this.auth).doPOST();
    }

    public void becomeMaster(long rid, long uid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/rooms/%d/master", rid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .addParam("uid", uid).setAuth(this.auth).doPUT();
    }

    public void resignMaster(long rid, long uid, Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/rooms/%d/master", rid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class)
                .setAuth(this.auth).doDELETE();
    }

    public void uploadLibrary(long uid, List<JsonStruct.Track> tracks,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/libentries/%d", uid);
        AsyncRequest<JsonStruct.Success> request = AsyncRequest.create(
                url, handler, JsonStruct.Success.class).setAuth(this.auth);
        for (JsonStruct.Track track : tracks) {
            request.addParam("entry", track);
        }
        request.doPUT();
    }

    public void updateLibrary(long uid, List<JsonStruct.Delta> deltas,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/libentries/%d/batch", uid);
        AsyncRequest<JsonStruct.Success> request = AsyncRequest.create(
                url, handler, JsonStruct.Success.class).setAuth(this.auth);
        for (JsonStruct.Delta delta : deltas) {
            request.addParam("delta", delta);
        }
        request.doPOST();
    }

    public void getRatings(long uid, Handler<JsonStruct.RatingsList> handler) {
        URL url = urlFor("/libentries/%d/ratings", uid);
        AsyncRequest.create(url, handler, JsonStruct.RatingsList.class)
                .setAuth(this.auth).doGET();
    }

    public void rate(long uid, String artist, String title, int rating,
            Handler<JsonStruct.Success> handler) {
        URL url = urlFor("/libentries/%d/ratings", uid);
        AsyncRequest.create(url, handler, JsonStruct.Success.class).setAuth(this.auth)
                .addParam("artist", artist).addParam("title", title).addParam("rating", rating);
    }

    private static URL urlFor(String suffix, Object... objects) {
        try {
            return new URL(API_ROOT + String.format(suffix, objects));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    public static interface Handler<S extends JsonStruct> {
        public void callback(S structure);
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
    }

}
