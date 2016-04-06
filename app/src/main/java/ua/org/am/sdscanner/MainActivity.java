package ua.org.am.sdscanner;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Environment;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.security.Permission;

public class MainActivity extends AppCompatActivity implements SDScannerService.Listener {

    private View mButtonLayout;
    private View mProgressLayout;
    private ProgressBar mProgressBar;
    private TextView mProgressText;
    private Button mStartButton;
    private TextView mFilesCountText;
    private TextView mFoldersCountText;
    private TextView mAgainText;

    private boolean isActive = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButtonLayout = findViewById(R.id.buttonLayout);
        mProgressLayout = findViewById(R.id.progressLayout);
        mStartButton = (Button) findViewById(R.id.button);
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        mProgressText = (TextView) findViewById(R.id.progressText);
        mFilesCountText = (TextView) findViewById(R.id.filesCount);
        mFoldersCountText = (TextView) findViewById(R.id.foldersCount);
        mAgainText = (TextView) findViewById(R.id.againText);

        mProgressBar.setIndeterminate(true);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start scanning
                SDScannerService.startService(getApplicationContext(), Environment.getExternalStorageDirectory().getAbsolutePath());
            }
        });

        mAgainText.setText(Html.fromHtml("<u>" + getString(R.string.scan_again) + "</u>"));
        mAgainText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // start scanning
                SDScannerService.startService(getApplicationContext(), Environment.getExternalStorageDirectory().getAbsolutePath());
            }
        });

        showProgressLayout(false);
    }

    @Override
    public void onResume() {
        super.onResume();

        isActive = true;

        SDScannerService.setListener(this);

        // check permissions
        if (android.os.Build.VERSION.SDK_INT >= 23){
            if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 0);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case 0: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {
                    finish();
                }
                return;
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        isActive = false;

        SDScannerService.setListener(null);
    }

    private void showProgressLayout(final boolean value) {
        mProgressLayout.setVisibility(value ? View.VISIBLE : View.GONE);
        mButtonLayout.setVisibility(!value ? View.VISIBLE : View.GONE);
        mAgainText.setVisibility(View.GONE);
        mProgressText.setVisibility(View.VISIBLE);
    }

    @Override
    public void onScanStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActive) {
                    showProgressLayout(true);
                }
            }
        });
    }

    @Override
    public void onScanProgress(final String file, final int folders_counter, final int files_counter) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActive) {
                    mProgressText.setText(file);
                    mFoldersCountText.setText(Integer.toString(folders_counter));
                    mFilesCountText.setText(Integer.toString(files_counter));
                }
            }
        });
    }

    @Override
    public void onScanFinished() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (isActive) {
                    mAgainText.setVisibility(View.VISIBLE);
                    mProgressText.setVisibility(View.GONE);
                    mProgressBar.setVisibility(View.INVISIBLE);
                }
            }
        });
    }

}
