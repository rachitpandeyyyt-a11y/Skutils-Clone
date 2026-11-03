package rachit.skutils.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import rachit.skutils.R;
import rachit.skutils.util.RuntimeUtil;

/** Requests the storage permissions and then opens the main screen. */
public class SplashActivity extends AppCompatActivity {

    private static final int REQUEST_STORAGE = 100;

    private boolean started;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        new Handler(Looper.getMainLooper()).postDelayed(this::ensurePermissions, 600);
    }

    private void ensurePermissions() {
        if (RuntimeUtil.isAtLeastR()) {
            if (!isAllFilesAccessGranted()) {
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    intent.setData(android.net.Uri.parse("package:" + getPackageName()));
                    startActivityForResult(intent, REQUEST_STORAGE);
                } catch (Exception e) {
                    try {
                        startActivityForResult(
                                new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION),
                                REQUEST_STORAGE);
                    } catch (Exception ignored) {
                        openMain();
                    }
                }
            } else {
                openMain();
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                openMain();
            } else {
                requestPermissions(new String[]{
                        Manifest.permission.READ_EXTERNAL_STORAGE,
                        Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQUEST_STORAGE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_STORAGE) {
            openMain();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_STORAGE) {
            openMain();
        }
    }

    private void openMain() {
        if (isFinishing() || started) {
            return;
        }
        if (RuntimeUtil.isAtLeastR() && !isAllFilesAccessGranted()) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.storage_needed_title)
                    .setMessage(R.string.storage_needed_message)
                    .setPositiveButton(R.string.retry, (d, w) -> ensurePermissions())
                    .setNegativeButton(android.R.string.cancel, (d, w) -> {
                        started = true;
                        startActivity(new Intent(this, MainActivity.class));
                        finish();
                    })
                    .show();
            return;
        }
        Toast.makeText(this, R.string.scanning_projects, Toast.LENGTH_SHORT).show();
        started = true;
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    /** Wrapped so the API 30 constant is only resolved on Android 11+. */
    private boolean isAllFilesAccessGranted() {
        if (!RuntimeUtil.isAtLeastR()) {
            return true;
        }
        try {
            return Environment.isExternalStorageManager();
        } catch (Throwable t) {
            return false;
        }
    }
}
