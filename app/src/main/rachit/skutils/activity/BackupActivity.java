package rachit.skutils.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.CheckBox;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import rachit.skutils.R;
import rachit.skutils.model.Project;
import rachit.skutils.util.BackupManager;
import rachit.skutils.util.ProgressListener;
import rachit.skutils.util.SketchwareUtil;

import java.io.File;

/** Creates a {@code .swb} backup of a single project. */
public class BackupActivity extends AppCompatActivity {

    private static final String EXTRA_SC_ID = "sc_id";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String scId;
    private Project project;
    private CheckBox includeFiles;
    private CheckBox includeLibs;
    private TextView target;
    private AlertDialog progressDialog;

    public static Intent intent(Context context, String scId) {
        Intent intent = new Intent(context, BackupActivity.class);
        intent.putExtra(EXTRA_SC_ID, scId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup);

        scId = getIntent().getStringExtra(EXTRA_SC_ID);
        if (scId == null) {
            finish();
            return;
        }
        project = SketchwareUtil.readProject(scId);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.backup);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        includeFiles = findViewById(R.id.include_files);
        includeLibs = findViewById(R.id.include_libs);
        target = findViewById(R.id.target);

        File output = BackupManager.defaultBackupFile(project);
        target.setText(getString(R.string.backup_to, output.getAbsolutePath()));

        findViewById(R.id.start).setOnClickListener(v -> startBackup());
    }

    private void startBackup() {
        File output = BackupManager.defaultBackupFile(project);
        progressDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.backup)
                .setMessage(R.string.preparing)
                .setCancelable(false)
                .create();
        progressDialog.show();

        new Thread(() -> {
            BackupManager.createBackup(project, output,
                    includeFiles.isChecked(), includeLibs.isChecked(),
                    new ProgressListener() {
                        @Override
                        public void onProgress(String stage, int percent) {
                            handler.post(() -> progressDialog.setMessage(
                                    percent < 0 ? stage : stage + "  (" + percent + "%)"));
                        }

                        @Override
                        public void onFinished(boolean success, String message) {
                            handler.post(() -> {
                                dismissProgress();
                                if (!success) {
                                    Toast.makeText(BackupActivity.this, message, Toast.LENGTH_LONG).show();
                                    return;
                                }
                                boolean exists = output.exists();
                                String size = exists
                                        ? rachit.skutils.util.FileUtil.humanReadableSize(output.length())
                                        : "?";
                                new AlertDialog.Builder(BackupActivity.this)
                                        .setTitle(R.string.backup_done)
                                        .setMessage(output.getAbsolutePath() + "\n" + size)
                                        .setPositiveButton(R.string.share, (d, w) ->
                                                share(output))
                                        .setNegativeButton(android.R.string.ok, null)
                                        .show();
                            });
                        }
                    });
        }, "skutils-backup").start();
    }

    private void share(File file) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("application/zip");
        intent.putExtra(Intent.EXTRA_STREAM,
                androidx.core.content.FileProvider.getUriForFile(this,
                        getPackageName() + ".fileprovider", file));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(intent, getString(R.string.share)));
    }

    private void dismissProgress() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}
