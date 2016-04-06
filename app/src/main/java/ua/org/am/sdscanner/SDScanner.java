package ua.org.am.sdscanner;

import android.content.Context;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Created by andriy on 4/4/16.
 */
public class SDScanner {
    private MediaScannerConnection mMediaConnection;
    final Lock lock = new ReentrantLock();
    final Condition isConnected  = lock.newCondition();
    final Condition isProcessed  = lock.newCondition();
    public SDScanner(Context context) {
        mMediaConnection = new MediaScannerConnection(context, new MediaScannerConnection.MediaScannerConnectionClient() {
            @Override
            public void onMediaScannerConnected() {
                lock.lock();
                try {
                    isConnected.signal();
                }finally {
                    lock.unlock();
                }
            }

            @Override
            public void onScanCompleted(String s, Uri uri) {
                lock.lock();
                try {
                    isProcessed.signal();
                }finally {
                    lock.unlock();
                }
            }
        });
    }

    public interface Listener {
        public void onProgress(String path, int folders_counter, int files_counter);
    }

    private Listener mListener;

    private int mFilesCounter = 0;

    private int mFoldersCounter = 0;

    public void scan(File folder, Listener listener) {
        mListener = listener;

        mMediaConnection.connect();

        lock.lock();
        try {
            isConnected.await();
        }catch(Exception e) {
            e.printStackTrace();
        } finally {
            lock.unlock();
        }

        mFoldersCounter = 0;
        mFilesCounter = 0;

        if(mMediaConnection.isConnected()) {
            scanFolder(folder);
        }

        mMediaConnection.disconnect();
    }

    private void scanFolder(File folder) {
        if(!folder.exists()) {
            return;
        }

        if((new File(folder.getAbsolutePath() + "/.nomedia").exists())) {
            return;
        }

        if(folder.getName().startsWith(".")) {
            return;
        }

        mListener.onProgress(folder.getAbsolutePath(), ++mFoldersCounter, mFilesCounter);
        Log.d("AAA", folder.getAbsolutePath());

        File[] files = folder.listFiles();
        if(files == null) {
            return;
        }

        for(File f : files) {
            if(f.isDirectory()) {
                scanFolder(f);
            } else {
                mListener.onProgress(f.getAbsolutePath(), mFoldersCounter, ++mFilesCounter);
                Log.d("AAA", "scanFile " + f.getAbsolutePath());

                // wait for being processed
                lock.lock();
                try {
                    mMediaConnection.scanFile(f.getAbsolutePath(), null);
                    isProcessed.await();
                }catch(Exception e) {
                    e.printStackTrace();
                } finally {
                    lock.unlock();
                }
            }
        }
    }
}
