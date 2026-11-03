package rachit.skutils.activity;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;
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
import rachit.skutils.util.FileUtil;
import rachit.skutils.util.SketchwareUtil;

import java.util.List;

/** Lists the activities / custom views of a project and edits the file entry. */
public class EditorActivity extends AppCompatActivity {

    private static final String EXTRA_SC_ID = "sc_id";

    private String scId;
    private ActivityAdapter adapter;
    private TextView header;

    public static Intent intent(Context context, String scId) {
        Intent intent = new Intent(context, EditorActivity.class);
        intent.putExtra(EXTRA_SC_ID, scId);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_editor);

        scId = getIntent().getStringExtra(EXTRA_SC_ID);
        if (scId == null) {
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        header = findViewById(R.id.header);
        RecyclerView list = findViewById(R.id.list);
        list.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ActivityAdapter(false, (item, position) -> showItemOptions(item));
        list.setAdapter(adapter);
        toolbar.setNavigationOnClickListener(v -> finish());

        findViewById(R.id.add).setOnClickListener(v -> addActivity());
        load();
    }

    private void load() {
        Project project = SketchwareUtil.readProject(scId);
        header.setText(project.displayName() + "\n" + project.packageName);
        List<ActivityItem> items = SketchwareUtil.readActivities(scId);
        adapter.submit(items);
    }

    private void addActivity() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("MainActivity");
        new AlertDialog.Builder(this)
                .setTitle(R.string.add_activity)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        return;
                    }
                    if (!name.endsWith("Activity")) {
                        name = name + "Activity";
                    }
                    if (!name.endsWith(".java")) {
                        name = name + ".java";
                    }
                    List<ActivityItem> items = adapter.getItems();
                    ActivityItem item = new ActivityItem();
                    item.fileName = name;
                    item.fileType = 0;
                    items.add(item);
                    SketchwareUtil.writeActivities(scId, items);
                    load();
                    Toast.makeText(this, R.string.saved_restart_sketchware, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showItemOptions(ActivityItem item) {
        final String[] options = {
                getString(R.string.rename),
                getString(R.string.clone),
                getString(R.string.delete),
        };
        new AlertDialog.Builder(this)
                .setTitle(item.fileName)
                .setItems(options, (d, which) -> {
                    switch (which) {
                        case 0:
                            rename(item);
                            break;
                        case 1:
                            cloneItem(item);
                            break;
                        case 2:
                            delete(item);
                            break;
                    }
                })
                .show();
    }

    private void rename(ActivityItem item) {
        EditText input = new EditText(this);
        input.setText(item.fileName);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(this)
                .setTitle(R.string.rename)
                .setView(input)
                .setPositiveButton(android.R.string.ok, (d, w) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        return;
                    }
                    if (!name.endsWith(".java")) {
                        name = name + ".java";
                    }
                    List<ActivityItem> items = adapter.getItems();
                    for (ActivityItem candidate : items) {
                        if (candidate.fileName.equals(item.fileName)) {
                            candidate.fileName = name;
                        }
                    }
                    SketchwareUtil.writeActivities(scId, items);
                    load();
                    Toast.makeText(this, R.string.saved_restart_sketchware, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void cloneItem(ActivityItem item) {
        List<ActivityItem> items = adapter.getItems();
        String base = FileUtil.nameWithoutExtension(item.fileName);
        String copyName = base + "Copy.java";
        int counter = 2;
        while (containsName(items, copyName)) {
            copyName = base + "Copy" + counter + ".java";
            counter++;
        }
        ActivityItem copy = new ActivityItem();
        copy.fileName = copyName;
        copy.fileType = item.fileType;
        copy.orientation = item.orientation;
        copy.theme = item.theme;
        copy.keyboardSetting = item.keyboardSetting;
        copy.options = item.options;
        items.add(copy);
        SketchwareUtil.writeActivities(scId, items);
        load();
        Toast.makeText(this, R.string.cloned_hint, Toast.LENGTH_LONG).show();
    }

    private void delete(ActivityItem item) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage(item.fileName)
                .setPositiveButton(R.string.delete, (d, w) -> {
                    List<ActivityItem> items = adapter.getItems();
                    for (int i = items.size() - 1; i >= 0; i--) {
                        if (items.get(i).fileName.equals(item.fileName)) {
                            items.remove(i);
                        }
                    }
                    SketchwareUtil.writeActivities(scId, items);
                    load();
                    Toast.makeText(this, R.string.saved_restart_sketchware, Toast.LENGTH_LONG).show();
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private boolean containsName(List<ActivityItem> items, String name) {
        for (ActivityItem item : items) {
            if (item.fileName.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
