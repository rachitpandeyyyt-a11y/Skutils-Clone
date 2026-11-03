package rachit.skutils.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import rachit.skutils.R;
import rachit.skutils.adapter.BackupAdapter;
import rachit.skutils.model.Project;
import rachit.skutils.util.BackupManager;
import rachit.skutils.util.ProgressListener;
import rachit.skutils.util.SketchwareUtil;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Lists the {@code .swb} backups found on the device and restores them. */
public class RestoreActivity extends AppCompatActivity {

    private final Handler handler = new Handler(Looper.getMainLooper());
    private BackupAdapter adapter;
    private TextView empty;

    public static Intent intent(Context context) {
        return new Intent(context, RestoreActivity.class);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.restore);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        empty = findViewById(R.id.empty);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new BackupAdapter(this::onBackupClick);
        list.setAdapter(adapter);
        load();
    }

    private void load() {
        List<File> backups = new ArrayList<>();
        collect(SketchwareUtil.backupsDir(), backups);
        collect(SketchwareUtil.extractedBackupsDir(), backups);
        adapter.submit(backups);
        empty.setVisibility(backups.isEmpty() ? View.VISIBLE : View.GONE);
        empty.setText(R.string.no_backups);
    }

    private void collect(File root, List<File> out) {
        if (root == null || !root.exists()) {
            return;
        }
        File[] children = root.listFiles();
        if (children == null) {
            return;
        }
        for (File child : children) {
            if (child.isDirectory()) {
                collect(child, out);
            } else if (child.getName().toLowerCase().endsWith(".swb")) {
                out.add(child);
            }
        }
    }

    private void onBackupClick(File backup) {
        new AlertDialog.Builder(this)
                .setTitle(backup.getName())
                .setItems(new String[]{
                        getString(R.string.view_backup),
                        getString(R.string.restore),
                        getString(R.string.share)}, (d, which) -> {
                    switch (which) {
                        case 0:
                            startActivity(ViewBackupedActivity.intent(this, backup.getAbsolutePath()));
                            break;
                        case 1:
                            startActivity(SelectToRestoreActivity.intent(this, backup.getAbsolutePath()));
                            break;
                        case 2:
                            share(backup);
                            break;
                    }
                })
                .show();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, R.string.rescan);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        load();
        return true;
    }
}
