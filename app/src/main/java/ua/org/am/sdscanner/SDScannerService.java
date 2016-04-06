package ua.org.am.sdscanner;

import android.app.IntentService;
import android.content.Intent;
import android.content.Context;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;

public class SDScannerService extends IntentService {
    private static final String ACTION_START = "ua.org.am.sdscanner.action.START";
    private static final String ACTION_STOP = "ua.org.am.sdscanner.action.STOP";

    private static final String PARAM_PATH = "ua.org.am.sdscanner.param.PATH";

    public interface Listener {
        public void onScanStarted();
        public void onScanProgress(String file, int folders_counter, int files_counter);
        public void onScanFinished();
    }

    private static WeakReference<Listener> mListener;

    private static Object lock = new Object();

    private static AtomicBoolean mIsRunning = new AtomicBoolean(false);

    public SDScannerService() {
        super("SDScannerService");
    }

    public static void setListener(Listener value) {
        synchronized (lock) {
            mListener = new WeakReference<Listener>(value);
            if(mIsRunning.get()) {
                mListener.get().onScanStarted();
            }
        }
    }

    public static void startService(Context context, String path) {
        if(isRunning()) {
            return;
        }

        Intent intent = new Intent(context, SDScannerService.class);
        intent.setAction(ACTION_START);
        intent.putExtra(PARAM_PATH, path);
        context.startService(intent);
    }

    public static boolean isRunning() {
        return mIsRunning.get();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mIsRunning.set(true);
        mListener.get().onScanStarted();
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_START.equals(action)) {
                handleStart(intent.getStringExtra(PARAM_PATH));
            }
        }
        mIsRunning.set(false);
        mListener.get().onScanFinished();
    }

    private void handleStart(String path) {
        if(path == null || path.isEmpty()) {
            return;
        }

        SDScanner scanner = new SDScanner(getApplicationContext());
        scanner.scan(new File(path), new SDScanner.Listener() {

            @Override
            public void onProgress(String path, int folders_counter, int files_counter) {
                if (mListener.get() != null) {
                    mListener.get().onScanProgress(path, folders_counter, files_counter);
                }
            }
        });
    }

}
