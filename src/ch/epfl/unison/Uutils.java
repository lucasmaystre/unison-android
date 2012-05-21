package ch.epfl.unison;

import java.io.InputStream;
import java.net.URL;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

public class Uutils {

    private static final String TAG = "ch.epfl.unison.Uutils";

    public static String distToString(Float dist) {
        if (dist == null) {
            return "";
        }
        if (dist > 999) {
            return String.format("%.2fkm", dist / 1000);
        } else {
            return String.format("%dm", dist.intValue());
        }
    }

    public static void setBitmapFromURL(ImageView image, String url) {
        new BitmapFromURL(image, url).execute();
    }

    private static class BitmapFromURL extends AsyncTask<Void, Void, Bitmap> {

        private ImageView image;
        private String url;

        public BitmapFromURL(ImageView image, String url) {
            this.image = image;
            this.url = url;
        }

        @Override
        protected Bitmap doInBackground(Void... nothing) {
            try {
                InputStream stream = (InputStream) new URL(this.url).getContent();
                return BitmapFactory.decodeStream(stream);
            } catch (Exception e) {
                Log.i(TAG, String.format("couldn't get a bitmap from %s", this.url), e);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Bitmap result) {
            if (result != null) {
                this.image.setImageBitmap(result);
            }
        }
    }
}
