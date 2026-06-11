package android.os;

public class Handler {
    private final Looper mLooper;

    public Handler() {
        this.mLooper = Looper.getMainLooper();
    }

    public Handler(Looper looper) {
        this.mLooper = looper;
    }

    public Looper getLooper() {
        return mLooper;
    }
    
    public boolean post(Runnable r) {
        r.run();
        return true;
    }

    public boolean postDelayed(Runnable r, long delayMillis) {
        r.run();
        return true;
    }
}
