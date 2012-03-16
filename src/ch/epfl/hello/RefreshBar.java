package ch.epfl.hello;

import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ViewAnimator;

public class RefreshBar extends ViewAnimator implements OnClickListener {

    public static final int REFRESHING = 0;
    public static final int READY = 1;

    OnRefreshListener listener = null;

    public RefreshBar(Context context, AttributeSet attrs) {
        super(context, attrs);

        final float scale = getResources().getDisplayMetrics().density;
        int padding = (int) (5 * scale + 0.5f);
        this.setPadding(padding, padding, padding, 0);

        this.setAnimateFirstView(true);
        this.setInAnimation(getContext(), R.anim.fade_in);
        this.setOutAnimation(getContext(), R.anim.fade_out);

        LayoutInflater.from(context).inflate(R.layout.refresh_bar, this, true);

        this.findViewById(R.id.refreshBarBtn).setOnClickListener(this);
    }

    public void setOnRefreshListener(OnRefreshListener listener) {
        this.listener = listener;
    }

    public void onClick(View v) {
        if (this.listener != null) {
            this.listener.onRefresh();
        }
    }

    public void setState(int state) {
        this.setDisplayedChild(state);
        //this.showNext();
    }

    /** A simple interface to handle clicks on the refresh button. */
    public interface OnRefreshListener {
        public void onRefresh();
    }
}
