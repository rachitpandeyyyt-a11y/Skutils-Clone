package rachit.skutils.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.TextView;

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
import rachit.skutils.util.SketchwareUtil;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;

/** Inspects a {@code .swb} backup without restoring it. */
public class ViewBackupedActivity extends AppCompatActivity {

    private static final String EXTRA_PATH = "path";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private File backup;
    private ActivityAdapter adapter;
    private TextView info;
    private TextView empty;

    public static Intent intent(Context context, String path) {
        Intent intent = new Intent(context, ViewBackupedActivity.class);
        intent.putExtra(EXTRA_PATH, path);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_backuped);

        String path = getIntent().getStringExtra(EXTRA_PATH);
        if (path == null) {
            finish();
            return;
        }
        backup = new File(path);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.view_backup);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        info = findViewById(R.id.info);
        empty = findViewById(R.id.empty);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivityAdapter(false, null);
        list.setAdapter(adapter);

        load();
    }

    private void load() {
        new Thread(() -> {
            Project project = null;
            try {
                project = BackupManager.readProjectFromArchive(new FileInputStream(backup));
            } catch (Exception ignored) {
            }

            File scratch = new File(SketchwareUtil.extractedBackupsDir(),
                    FileUtil.nameWithoutExtension(backup.getName()) + "_preview");
            if (scratch.exists()) {
                FileUtil.delete(scratch);
            }
            scratch.mkdirs();
            boolean ok = false;
            try {
                ok = BackupManager.extractTo(new FileInputStream(backup), scratch, null);
            } catch (Exception ignored) {
            }
            List<ActivityItem> activities = ok
                    ? SketchwareUtil.parseActivities(
                            FileUtil.readDecrypted(new File(scratch, "data/file")))
                    : java.util.Collections.emptyList();

            final Project finalProject = project;
            final String size = FileUtil.humanReadableSize(backup.length());
            handler.post(() -> {
                StringBuilder sb = new StringBuilder();
                if (finalProject != null) {
                    sb.append(getString(R.string.app_label)).append(": ")
                            .append(finalProject.appName).append('\n');
                    sb.append(getString(R.string.workspace_label)).append(": ")
                            .append(finalProject.workspaceName).append('\n');
                    sb.append(getString(R.string.package_label)).append(": ")
                            .append(finalProject.packageName).append('\n');
                    sb.append(getString(R.string.version_label)).append(": ")
                            .append(finalProject.versionName).append(" (").append(finalProject.versionCode).append(")\n");
                    sb.append(getString(R.string.project_id_label)).append(": ")
                            .append(finalProject.scId).append('\n');
                }
                sb.append(getString(R.string.backup_size_label)).append(": ").append(size);
                info.setText(sb.toString());
                adapter.submit(activities);
                if (activities.isEmpty()) {
                    empty.setVisibility(View.VISIBLE);
                    empty.setText(R.string.no_activities);
                } else {
                    empty.setVisibility(View.GONE);
                }
            });
        }, "skutils-view").start();
    }
}
