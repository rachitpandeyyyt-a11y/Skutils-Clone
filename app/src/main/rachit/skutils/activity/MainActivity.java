package rachit.skutils.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import rachit.skutils.R;
import rachit.skutils.adapter.ProjectAdapter;
import rachit.skutils.model.Project;
import rachit.skutils.util.SketchwareUtil;

import java.util.List;

/**
 * Home screen: quick actions, credit line and the list of Sketchware projects.
 */
public class MainActivity extends AppCompatActivity {

    private ProjectAdapter adapter;
    private RecyclerView list;
    private TextView empty;
    private TextView banner;
    private TextView projectCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        list = findViewById(R.id.list);
        empty = findViewById(R.id.empty);
        banner = findViewById(R.id.banner);
        projectCount = findViewById(R.id.project_count);

        list.setLayoutManager(new LinearLayoutManager(this));
        list.setNestedScrollingEnabled(false);
        adapter = new ProjectAdapter(this::showProjectOptions);
        list.setAdapter(adapter);

        findViewById(R.id.fab).setOnClickListener(v -> refresh());

        // quick action cards
        findViewById(R.id.card_backup).setOnClickListener(v -> {
            Project first = firstProject();
            if (first == null) {
                toastNoProjects();
                return;
            }
            startActivity(BackupActivity.intent(this, first.scId));
        });
        findViewById(R.id.card_restore).setOnClickListener(v ->
                startActivity(new Intent(this, RestoreActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void refresh() {
        List<Project> projects = SketchwareUtil.listProjects();
        adapter.submit(projects);

        boolean installed = SketchwareUtil.isSketchwareInstalled();
        banner.setVisibility(installed ? View.GONE : View.VISIBLE);
        banner.setText(R.string.sketchware_not_found);

        if (projects.isEmpty()) {
            projectCount.setText(R.string.project_count_empty);
        } else {
            projectCount.setText(getString(R.string.project_count, projects.size()));
        }

        boolean isEmpty = projects.isEmpty();
        empty.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        list.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        empty.setText(installed ? R.string.no_projects : R.string.no_projects_hint);
    }

    private Project firstProject() {
        return adapter.getProjects().isEmpty() ? null : adapter.getProjects().get(0);
    }

    private void toastNoProjects() {
        android.widget.Toast.makeText(this, R.string.no_projects, android.widget.Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_restore) {
            startActivity(new Intent(this, RestoreActivity.class));
            return true;
        } else if (id == R.id.action_rescan) {
            refresh();
            return true;
        } else if (id == R.id.action_about) {
            showAbout();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showProjectOptions(Project project, int position) {
        final String[] options = {
                getString(R.string.option_edit_activities),
                getString(R.string.option_backup),
                getString(R.string.option_clone),
                getString(R.string.option_project_to_top),
                getString(R.string.option_debug),
        };
        new AlertDialog.Builder(this)
                .setTitle(project.displayName())
                .setMessage(project.packageName + "  •  v" + project.versionName)
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0:
                            startActivity(EditorActivity.intent(this, project.scId));
                            break;
                        case 1:
                            startActivity(BackupActivity.intent(this, project.scId));
                            break;
                        case 2:
                            startActivity(CloneActivity.intent(this, project.scId));
                            break;
                        case 3:
                            if (SketchwareUtil.touchProject(project.scId)) {
                                Toast.makeText(this, R.string.project_moved_top,
                                        Toast.LENGTH_SHORT).show();
                                refresh();
                            }
                            break;
                        case 4:
                            startActivity(DebugActivity.intent(this, project.scId));
                            break;
                    }
                })
                .show();
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.about_title)
                .setMessage(R.string.about_message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}