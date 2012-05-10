package ch.epfl.unison.api;

import java.net.URL;

import android.os.AsyncTask;


public class AsyncRequest<T extends JsonStruct>
        extends AsyncTask<AsyncRequest.Method, Void, Request.Result<T>> {

    private Request<T> request;
    private UnisonAPI.Handler<T> handler;

    public static enum Method {
        GET,
        POST,
        PUT,
        DELETE,
    }


    private AsyncRequest(URL url, UnisonAPI.Handler<T> handler, Class<T> classOfT) {
        this.request = Request.of(url, classOfT);
        this.handler = handler;
    }

    public static <S extends JsonStruct> AsyncRequest<S> of(
            URL url, UnisonAPI.Handler<S> handler, Class<S> classOfS) {
        return new AsyncRequest<S>(url, handler, classOfS);
    }

    public AsyncRequest<T> addParam(String key, Object value) {
        this.request.addParam(key, value);
        return this;
    }

    public AsyncRequest<T> setAuth(String auth) {
        this.request.setAuth(auth);
        return this;
    }

    public void doGET() {
        this.execute(Method.GET);
    }

    public void doPOST() {
        this.execute(Method.POST);
    }

    public void doPUT() {
        this.execute(Method.PUT);
    }

    public void doDELETE() {
        this.execute(Method.DELETE);
    }

    @Override
    protected Request.Result<T> doInBackground(Method... method) {
        switch(method[0]) {
        case GET:
            return this.request.doGET();
        case POST:
            return this.request.doPOST();
        case PUT:
            return this.request.doPUT();
        case DELETE:
            return this.request.doDELETE();
        default:
            return null;  // Should never happen.
        }
    }

    @Override
    protected void onPostExecute(Request.Result<T> res) {
        if (res.result != null) {
            this.handler.callback(res.result);
        } else {
            this.handler.onError(res.error);
        }
    }
}
