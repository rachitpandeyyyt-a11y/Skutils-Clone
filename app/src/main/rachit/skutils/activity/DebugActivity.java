package rachit.skutils.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import rachit.skutils.R;
import rachit.skutils.model.ActivityItem;
import rachit.skutils.model.Project;
import rachit.skutils.util.FileUtil;
import rachit.skutils.util.SketchwareUtil;

import java.io.File;
import java.util.List;

/** Shows diagnostics about the Sketchware folders and a project. */
public class DebugActivity extends AppCompatActivity {

    private static final String EXTRA_SC_ID = "sc_id";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private TextView output;

    public static Intent intent(Context context, String scId) {
        Intent intent = new Intent(context, DebugActivity.class);
        intent.putExtra(EXTRA_SC_ID, scId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_debug);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.debug);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        output = findViewById(R.id.output);
        String scId = getIntent().getStringExtra(EXTRA_SC_ID);
        new Thread(() -> handler.post(() -> output.setText(buildReport(scId))),
                "skutils-debug").start();
    }

    private String buildReport(String scId) {
        StringBuilder sb = new StringBuilder();
        sb.append("Sketchware root: ").append(SketchwareUtil.sketchwareDir().getAbsolutePath())
                .append(SketchwareUtil.sketchwareDir().exists() ? "  [ok]" : "  [missing]").append('\n');
        sb.append("Data dir: ").append(SketchwareUtil.dataDir().exists() ? "[ok]" : "[missing]").append('\n');
        sb.append("Backup dir: ").append(SketchwareUtil.backupsDir().exists() ? "[ok]" : "[missing]").append('\n');
        sb.append("Projects found: ").append(SketchwareUtil.listProjects().size()).append("\n\n");

        if (scId == null) {
            return sb.toString();
        }

        Project project = SketchwareUtil.readProject(scId);
        sb.append("Project: ").append(project.displayName()).append('\n');
        sb.append("Package: ").append(project.packageName).append('\n');
        sb.append("Version: ").append(project.versionName).append(" (").append(project.versionCode).append(")\n");
        sb.append("Sketchware version: ").append(project.sketchwareVersion).append('\n');
        sb.append("Registered: ").append(project.regDate).append("\n\n");

        File dataDir = SketchwareUtil.projectDataDir(scId);
        String[] files = {"file", "view", "logic", "library", "resource", "project_config",
                "view_root", "permission", "custom_blocks", "local_library"};
        for (String name : files) {
            File file = new File(dataDir, name);
            String content = file.exists() ? FileUtil.readDecrypted(file) : "";
            sb.append(name).append(": ").append(file.exists() ? "yes" : "no")
                    .append("  (").append(FileUtil.humanReadableSize(file.length())).append(")")
                    .append(content.isEmpty() ? "  [could not decrypt]" : "  [decrypted]")
                    .append('\n');
        }

        sb.append("\nActivity containers:\n");
        List<ActivityItem> items = SketchwareUtil.readActivities(scId);
        for (ActivityItem item : items) {
            sb.append("  - ").append(item.fileName).append(" (").append(item.typeLabel()).append(")\n");
        }

        String view = FileUtil.readDecrypted(new File(dataDir, "view"));
        int layouts = 0;
        for (String line : view.split("\n")) {
            if (line.startsWith("@") && line.endsWith(".xml")) {
                layouts++;
            }
        }
        sb.append("\nLayout containers in view: ").append(layouts).append('\n');

        String logic = FileUtil.readDecrypted(new File(dataDir, "logic"));
        int containers = 0;
        for (String line : logic.split("\n")) {
            if (line.startsWith("@")) {
                containers++;
            }
        }
        sb.append("Containers in logic: ").append(containers).append('\n');
        return sb.toString();
    }
}
