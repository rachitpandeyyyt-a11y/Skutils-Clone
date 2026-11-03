package rachit.skutils.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import rachit.skutils.R;
import rachit.skutils.adapter.ActivityAdapter;
import rachit.skutils.model.ActivityItem;
import rachit.skutils.model.Project;
import rachit.skutils.util.BackupManager;
import rachit.skutils.util.FileUtil;
import rachit.skutils.util.ProgressListener;
import rachit.skutils.util.SketchwareUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/**
 * Two step restore: first extract the {@code .swb} into a scratch folder to
 * inspect it, then let the user pick the activities to keep before restoring.
 */
public class SelectToRestoreActivity extends AppCompatActivity {

    private static final String EXTRA_PATH = "path";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private File backup;
    private File extractedDir;
    private Project project;
    private ActivityAdapter adapter;
    private TextView header;
    private AlertDialog progress;

    public static Intent intent(Context context, String path) {
        Intent intent = new Intent(context, SelectToRestoreActivity.class);
        intent.putExtra(EXTRA_PATH, path);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_select_to_restore);

        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null) {
            finish();
            return;
        }
        backup = new File(path);
        extractedDir = new File(SketchwareUtil.extractedBackupsDir(),
                FileUtil.nameWithoutExtension(backup.getName()));

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.select_to_restore);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        header = findViewById(R.id.header);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivityAdapter(true, null);
        list.setAdapter(adapter);

        findViewById(R.id.restore).setOnClickListener(v -> restore());
        findViewById(R.id.restore_all).setOnClickListener(v -> restoreAll());

        extract();
    }

    private void extract() {
        progress = new AlertDialog.Builder(this)
                .setTitle(R.string.restore)
                .setMessage(R.string.extracting)
                .setCancelable(false)
                .create();
        progress.show();

        new Thread(() -> {
            if (extractedDir.exists()) {
                FileUtil.delete(extractedDir);
            }
            extractedDir.mkdirs();
            boolean ok = false;
            try {
                ok = BackupManager.extractTo(new FileInputStream(backup), extractedDir, null);
            } catch (Exception e) {
                ok = false;
            }
            final boolean success = ok;
            handler.post(() -> {
                dismissProgress();
                if (!success) {
                    Toast.makeText(this, R.string.extract_failed, Toast.LENGTH_LONG).show();
                    finish();
                    return;
                }
                project = BackupManager.readProjectFromExtracted(extractedDir);
                List<ActivityItem> activities =
                        SketchwareUtil.parseActivities(FileUtil.readDecrypted(new File(extractedDir, "data/file")));
                adapter.submit(activities);
                header.setText(project.displayName() + "\n" + project.packageName
                        + "\nID " + project.scId + "  •  " + activities.size() + " activities");
            });
        }, "skutils-extract").start();
    }

    private void restore() {
        List<ActivityItem> selected = adapter.getSelected();
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.select_at_least_one, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore)
                .setItems(new String[]{
                        getString(R.string.restore_replace),
                        getString(R.string.restore_as_new)}, (d, which) -> {
                    if (which == 1) {
                        // keep the archive id for the resource folders, allocate a fresh target id
                        Project copy = copyWithNewId(project);
                        doRestore(copy, project.scId, false, selected);
                    } else {
                        doRestore(project, project.scId, true, selected);
                    }
                })
                .show();
    }

    private Project copyWithNewId(Project source) {
        Project copy = source.copy();
        copy.scId = SketchwareUtil.newProjectId();
        copy.regDate = SketchwareUtil.timestamp();
        return copy;
    }

    private void restoreAll() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.restore)
                .setMessage(R.string.restore_all_confirm)
                .setPositiveButton(android.R.string.ok,
                        (d, w) -> doRestore(project, project.scId, true, adapter.getItems()))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void doRestore(Project target, String sourceScId, boolean replace,
                           List<ActivityItem> selected) {
        progress = new AlertDialog.Builder(this)
                .setTitle(R.string.restore)
                .setMessage(R.string.restoring)
                .setCancelable(false)
                .create();
        progress.show();

        new Thread(() -> {
            // keep only the selected activities in the extracted data/file entry
            writeActivityFilter(selected);
            ProgressListener listener = new ProgressListener() {
                @Override
                public void onProgress(String stage, int percent) {
                    handler.post(() -> progress.setMessage(stage));
                }

                @Override
                public void onFinished(boolean success, String message) {
                    handler.post(() -> {
                        dismissProgress();
                        Toast.makeText(SelectToRestoreActivity.this, message, Toast.LENGTH_LONG).show();
                        if (success) {
                            finish();
                        }
                    });
                }
            };
            BackupManager.restoreFromExtracted(extractedDir, target, sourceScId, replace, listener);
        }, "skutils-restore").start();
    }

    private void writeActivityFilter(List<ActivityItem> selected) {
        StringBuilder sb = new StringBuilder();
        sb.append("@activity\n");
        for (ActivityItem item : selected) {
            if (item.isActivity()) {
                sb.append(item.toJson()).append('\n');
            }
        }
        sb.append("@customview\n");
        for (ActivityItem item : selected) {
            if (item.isCustomView()) {
                sb.append(item.toJson()).append('\n');
            }
        }
        FileUtil.writeEncrypted(new File(extractedDir, "data/file"), sb.toString());
    }

    private void dismissProgress() {
        if (progress != null && progress.isShowing()) {
            progress.dismiss();
        }
    }
}
