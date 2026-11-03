package rachit.skutils.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import rachit.skutils.R;
import rachit.skutils.model.ActivityItem;
import rachit.skutils.model.Project;
import rachit.skutils.util.FileUtil;
import rachit.skutils.util.ProjectCloner;
import rachit.skutils.util.SketchwareUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * Activity copy / clone wizard: duplicate an activity inside the same project
 * or copy it into another project.
 */
public class CloneActivity extends AppCompatActivity {

    private static final String EXTRA_SC_ID = "sc_id";

    private static final int MODE_CLONE = 0;
    private static final int MODE_COPY = 1;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private String scId;
    private List<Project> projects = new ArrayList<>();
    private List<ActivityItem> activities = new ArrayList<>();

    public static Intent intent(Context context, String scId) {
        Intent intent = new Intent(context, CloneActivity.class);
        intent.putExtra(EXTRA_SC_ID, scId);
        return intent;
    }

    /** "Share a project" is really "send a backup", Sketchware cannot import raw folders. */
    public static Intent shareIntent(Context context, String scId) {
        Intent intent = new Intent(context, BackupActivity.class);
        intent.putExtra(EXTRA_SC_ID, scId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_clone);

        scId = getIntent().getStringExtra(EXTRA_SC_ID);
        projects = SketchwareUtil.listProjects();
        if (scId == null && !projects.isEmpty()) {
            scId = projects.get(0).scId;
        }
        if (scId == null) {
            finish();
            return;
        }
        activities = SketchwareUtil.readActivities(scId);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.clone);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        TextView info = findViewById(R.id.info);
        info.setText(String.format(getString(R.string.clone_info),
                SketchwareUtil.readProject(scId).displayName(), activities.size()));

        findViewById(R.id.clone_in_project).setOnClickListener(v -> chooseActivity(MODE_CLONE));
        findViewById(R.id.copy_to_project).setOnClickListener(v -> chooseActivity(MODE_COPY));
        findViewById(R.id.clone_project).setOnClickListener(v -> cloneWholeProject());
    }

    private void cloneWholeProject() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.clone_project)
                .setMessage(R.string.clone_project_confirm)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    Project copy = ProjectCloner.cloneProject(scId);
                    Toast.makeText(this, getString(R.string.cloned_project, copy.workspaceName),
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void chooseActivity(int mode) {
        if (activities.isEmpty()) {
            Toast.makeText(this, R.string.no_activities, Toast.LENGTH_SHORT).show();
            return;
        }
        String[] names = new String[activities.size()];
        for (int i = 0; i < activities.size(); i++) {
            ActivityItem item = activities.get(i);
            names[i] = item.fileName + "   (" + item.typeLabel() + ")";
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_activity)
                .setItems(names, (d, which) -> {
                    if (mode == MODE_CLONE) {
                        askNewName(activities.get(which), null);
                    } else {
                        askTargetProject(activities.get(which));
                    }
                })
                .show();
    }

    private void askTargetProject(ActivityItem source) {
        List<Project> candidates = new ArrayList<>();
        for (Project project : projects) {
            if (!project.scId.equals(scId)) {
                candidates.add(project);
            }
        }
        if (candidates.isEmpty()) {
            Toast.makeText(this, R.string.no_other_projects, Toast.LENGTH_LONG).show();
            return;
        }
        String[] names = new String[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            names[i] = candidates.get(i).displayName();
        }
        Spinner spinner = new Spinner(this);
        spinner.setAdapter(new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, names));
        new AlertDialog.Builder(this)
                .setTitle(R.string.choose_target_project)
                .setView(spinner)
                .setPositiveButton(android.R.string.ok, (d, w) ->
                        askNewName(source, candidates.get(spinner.getSelectedItemPosition()).scId))
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void askNewName(ActivityItem source, String targetScId) {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setText(FileUtil.nameWithoutExtension(source.fileName) + "Copy");
        new AlertDialog.Builder(this)
                .setTitle(R.string.new_activity_name)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        return;
                    }
                    if (!name.endsWith(".java")) {
                        name = name + ".java";
                    }
                    boolean ok;
                    if (targetScId == null) {
                        ok = ProjectCloner.cloneActivityWithin(scId, source.fileName, name);
                    } else {
                        ok = ProjectCloner.copyActivityBetween(scId, source.fileName, targetScId, name);
                    }
                    Toast.makeText(this,
                            ok ? R.string.activity_copied : R.string.activity_copy_failed,
                            Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }
}
