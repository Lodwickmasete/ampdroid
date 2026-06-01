package com.lodwickmasete.php;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditorActivity extends Activity {

    // ── Views ────────────────────────────────────────────────────────────────
    private EditText  editText;
    private EditText  searchEditText;
    private TextView  fileInfo;
    private ProgressBar progressBar;
    private ListView  fileListView;
    private WebView   webView;

    private LinearLayout fileBrowserLayout;
    private LinearLayout editorLayout;
    private LinearLayout webViewLayout;
    private LinearLayout bottomBar;

    private Button      btnNewFile;
    private Button      btnSave;
    private Button      btnUndo;
    private Button      btnRedo;
    private Button      btnSearchNext;
    private Button      btnSearchPrev;
    private ImageButton btnPlayPreview;

    // ── State ────────────────────────────────────────────────────────────────
    private File    currentFile;
    private String  currentFilePath;
    private String  currentDirectoryPath;
    private boolean isModified      = false;
    private boolean isWebViewVisible = false;

    // ── Search ───────────────────────────────────────────────────────────────
    private int    lastSearchIndex = -1;
    private String lastSearchQuery = "";

    // ── File list ────────────────────────────────────────────────────────────
    private ArrayList<String>    fileListItems;
    private ArrayList<String>    fileListPaths;
    private ArrayAdapter<String> fileListAdapter;

    // ── Auto-save ────────────────────────────────────────────────────────────
    private final Handler  autoSaveHandler  = new Handler();
    private final Runnable autoSaveRunnable = new Runnable() {
        @Override public void run() {
            if (isModified && currentFile != null) {
                saveFile();
                toast("Auto-saved");
            }
        }
    };

    // ── Undo stack ───────────────────────────────────────────────────────────
    private final Deque<String> undoStack = new ArrayDeque<String>();
    private final Deque<String> redoStack = new ArrayDeque<String>();
    private boolean isUndoRedoAction = false;

    private static final int MAX_STACK   = 50;
    private static final int REQUEST_IMPORT = 100;

    // ────────────────────────────────────────────────────────────────────────
    //  Lifecycle
    // ────────────────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor);
        bindViews();
        setupFileBrowser();
        setupEditor();
        setupWebView();

        String path = getIntent().getStringExtra("file_path");
        if (path != null && new File(path).exists()) {
            openFile(path);
        } else {
            showFileBrowser();
            browseDirectory(getFilesDir().getAbsolutePath());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        autoSaveHandler.removeCallbacks(autoSaveRunnable);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  View binding
    // ────────────────────────────────────────────────────────────────────────

    private void bindViews() {
        editText          = (EditText)     findViewById(R.id.editText);
        searchEditText    = (EditText)     findViewById(R.id.searchEditText);
        fileInfo          = (TextView)     findViewById(R.id.fileInfo);
        progressBar       = (ProgressBar)  findViewById(R.id.progressBar);
        fileListView      = (ListView)     findViewById(R.id.fileListView);
        webView           = (WebView)      findViewById(R.id.webView);
        fileBrowserLayout = (LinearLayout) findViewById(R.id.fileBrowserLayout);
        editorLayout      = (LinearLayout) findViewById(R.id.editorLayout);
        webViewLayout     = (LinearLayout) findViewById(R.id.webViewLayout);
        bottomBar         = (LinearLayout) findViewById(R.id.bottomBar);
        btnNewFile        = (Button)       findViewById(R.id.btnNewFile);
        btnSave           = (Button)       findViewById(R.id.btnSave);
        btnUndo           = (Button)       findViewById(R.id.btnUndo);
        btnRedo           = (Button)       findViewById(R.id.btnRedo);
        btnSearchNext     = (Button)       findViewById(R.id.btnSearchNext);
        btnSearchPrev     = (Button)       findViewById(R.id.btnSearchPrev);
        btnPlayPreview    = (ImageButton)  findViewById(R.id.btnPlayPreview);

        editText.setTypeface(Typeface.MONOSPACE);
        editText.setTextSize(12);
        progressBar.setVisibility(View.GONE);
    }

    // ────────────────────────────────────────────────────────────────────────
    //  File browser
    // ────────────────────────────────────────────────────────────────────────

    private void setupFileBrowser() {
        fileListItems = new ArrayList<String>();
        fileListPaths = new ArrayList<String>();


fileListAdapter = new ArrayAdapter<String>(
        this, R.layout.item_file, R.id.fileName, fileListItems) {

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = getLayoutInflater().inflate(R.layout.item_file, parent, false);
        }

        ImageView icon     = (ImageView) convertView.findViewById(R.id.fileIcon);
        TextView  name     = (TextView)  convertView.findViewById(R.id.fileName);
        TextView  meta     = (TextView)  convertView.findViewById(R.id.fileMeta);

        String path = fileListPaths.get(position);
        File   f    = new File(path);

        name.setText(fileListItems.get(position));

        if (f.isDirectory()) {
            icon.setImageResource(R.drawable.ic_folder);
            meta.setText("");
        } else {
            String n = f.getName().toLowerCase();
            if      (n.endsWith(".html") || n.endsWith(".htm")) icon.setImageResource(R.drawable.ic_web);
            else if (n.endsWith(".php"))                        icon.setImageResource(R.drawable.ic_php);
            else                                                icon.setImageResource(R.drawable.ic_menu);
            meta.setText(formatSize(f.length()));
        }

        return convertView;
    }
};

        fileListView.setAdapter(fileListAdapter);

        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                File f = new File(fileListPaths.get(pos));
                if (f.isDirectory()) browseDirectory(f.getAbsolutePath());
                else                  openFile(f.getAbsolutePath());
            }
        });

        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                showFileOptionsDialog(fileListPaths.get(pos));
                return true;
            }
        });

        btnNewFile.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showCreateFileDialog(); }
        });

        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                if (editorLayout.getVisibility() == View.VISIBLE) saveFile();
            }
        });

        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { performUndo(); }
        });

        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { performRedo(); }
        });

        btnSearchNext.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { searchNext(); }
        });

        btnSearchPrev.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { searchPrev(); }
        });

        btnPlayPreview.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { toggleWebView(); }
        });

        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                lastSearchQuery = s.toString();
                lastSearchIndex = -1;
                if (!lastSearchQuery.isEmpty()) searchNext();
            }
        });
    }


private void browseDirectory(String path) {
    currentDirectoryPath = path;
    File dir = new File(path);
    if (!dir.exists() || !dir.isDirectory()) {
        toast("Cannot access: " + path);
        return;
    }

    fileListItems.clear();
    fileListPaths.clear();

    File parent = dir.getParentFile();
    if (parent != null) {
        fileListItems.add(".. (Parent Directory)");
        fileListPaths.add(parent.getAbsolutePath());
    }

    File[] files = dir.listFiles();
    if (files != null) {
        Arrays.sort(files, new Comparator<File>() {
            @Override public int compare(File a, File b) {
                if (a.isDirectory() != b.isDirectory())
                    return a.isDirectory() ? -1 : 1;
                return a.getName().compareToIgnoreCase(b.getName());
            }
        });
        for (File f : files) {
            if (f.canRead()) {
                fileListItems.add(f.getName());   // plain name, no emoji
                fileListPaths.add(f.getAbsolutePath());
            }
        }
    }

    fileListAdapter.notifyDataSetChanged();
    updateTitle();
}

    // ────────────────────────────────────────────────────────────────────────
    //  Editor
    // ────────────────────────────────────────────────────────────────────────

    private void setupEditor() {
        editText.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                if (!isUndoRedoAction) {
                    pushUndo(s.toString());
                }
            }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) {
                if (!isModified) { isModified = true; updateTitle(); }
                autoSaveHandler.removeCallbacks(autoSaveRunnable);
                autoSaveHandler.postDelayed(autoSaveRunnable, 3000);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void pushUndo(String state) {
        if (undoStack.size() >= MAX_STACK) undoStack.pollFirst();
        undoStack.push(state);
        redoStack.clear();
    }

    private void performUndo() {
        if (undoStack.isEmpty()) { toast("Nothing to undo"); return; }
        isUndoRedoAction = true;
        redoStack.push(editText.getText().toString());
        String prev = undoStack.pop();
        editText.setText(prev);
        editText.setSelection(prev.length());
        isUndoRedoAction = false;
    }

    private void performRedo() {
        if (redoStack.isEmpty()) { toast("Nothing to redo"); return; }
        isUndoRedoAction = true;
        undoStack.push(editText.getText().toString());
        String next = redoStack.pop();
        editText.setText(next);
        editText.setSelection(next.length());
        isUndoRedoAction = false;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  File I/O
    // ────────────────────────────────────────────────────────────────────────

    private void openFile(final String filePath) {
        currentFilePath = filePath;
        currentFile = new File(filePath);
        if (!currentFile.exists()) { toast("File not found: " + filePath); return; }

        showEditor();
        showProgress(true);
        undoStack.clear();
        redoStack.clear();

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final StringBuilder sb = new StringBuilder();
                    BufferedReader br = new BufferedReader(new FileReader(currentFile));
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line).append('\n');
                    br.close();
                    final String content = sb.toString();

                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            isUndoRedoAction = true;
                            editText.setText(content);
                            isUndoRedoAction = false;
                            isModified = false;
                            updateTitle();
                            updateFileInfo();
                            showProgress(false);
                            bottomBar.setVisibility(isHtmlFile(currentFile.getName())
                                    ? View.VISIBLE : View.GONE);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            showProgress(false);
                            toast("Error opening: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void saveFile() {
        if (currentFile == null) { showSaveAsDialog(); return; }
        showProgress(true);
        final String content = editText.getText().toString();

        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(currentFile));
                    bw.write(content);
                    bw.close();

                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            isModified = false;
                            updateTitle();
                            updateFileInfo();
                            showProgress(false);
                            toast("Saved: " + currentFile.getName());
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            showProgress(false);
                            toast("Error saving: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void showSaveAsDialog() {
        final EditText input = new EditText(this);
        input.setHint("Enter filename");
        new AlertDialog.Builder(this)
                .setTitle("Save As")
                .setView(input)
                .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) { toast("Filename cannot be empty"); return; }
                        currentFile = new File(currentDirectoryPath, name);
                        currentFilePath = currentFile.getAbsolutePath();
                        saveFile();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Search
    // ────────────────────────────────────────────────────────────────────────

    private void searchNext() {
        if (lastSearchQuery.isEmpty()) return;
        String text = editText.getText().toString();
        int idx = text.indexOf(lastSearchQuery, lastSearchIndex + 1);
        if (idx >= 0) {
            lastSearchIndex = idx;
            editText.setSelection(idx, idx + lastSearchQuery.length());
        } else {
            toast("No more matches");
            lastSearchIndex = -1;
        }
    }

    private void searchPrev() {
        if (lastSearchQuery.isEmpty()) return;
        String text = editText.getText().toString();
        int end = lastSearchIndex < 1 ? text.length() : lastSearchIndex - 1;
        int idx = text.lastIndexOf(lastSearchQuery, end);
        if (idx >= 0) {
            lastSearchIndex = idx;
            editText.setSelection(idx, idx + lastSearchQuery.length());
        } else {
            toast("No more matches");
            lastSearchIndex = -1;
        }
    }

    // ────────────────────────────────────────────────────────────────────────
    //  WebView / Preview
    // ────────────────────────────────────────────────────────────────────────

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override public void onPageFinished(WebView v, String url) {
                showProgress(false);
            }
        });

        Button btnClose = (Button) findViewById(R.id.btnCloseWebView);
        if (btnClose != null) {
            btnClose.setOnClickListener(new View.OnClickListener() {
                @Override public void onClick(View v) { closeWebView(); }
            });
        }
    }

    private void toggleWebView() {
        if (isWebViewVisible) {
            closeWebView();
        } else {
            if (currentFile != null && isHtmlFile(currentFile.getName())) {
                loadHtmlPreview();
                webViewLayout.setVisibility(View.VISIBLE);
                isWebViewVisible = true;
                btnPlayPreview.setImageResource(R.drawable.ic_pause);
            } else {
                toast("Open an HTML file to preview");
            }
        }
    }

    private void closeWebView() {
        webViewLayout.setVisibility(View.GONE);
        isWebViewVisible = false;
        btnPlayPreview.setImageResource(R.drawable.ic_play);
        webView.loadUrl("about:blank");
    }

    private void loadHtmlPreview() {
        String content = editText.getText().toString();
        if (!content.toLowerCase().contains("<html")) {
            content = "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n"
                    + "<meta name=\"viewport\" content=\"width=device-width,initial-scale=1\">\n"
                    + "<title>" + currentFile.getName() + "</title>\n</head>\n<body>\n"
                    + content + "\n</body>\n</html>";
        }
        showProgress(true);
        webView.loadDataWithBaseURL(
                "file://" + currentFile.getParent() + "/",
                content, "text/html", "UTF-8", null);
    }

    private boolean isHtmlFile(String name) {
        String n = name.toLowerCase();
        return n.endsWith(".html") || n.endsWith(".htm");
    }

    // ────────────────────────────────────────────────────────────────────────
    //  File operations dialogs
    // ────────────────────────────────────────────────────────────────────────

    private void showCreateFileDialog() {
        final EditText input = new EditText(this);
        input.setHint("e.g. index.php");
        new AlertDialog.Builder(this)
                .setTitle("New File")
                .setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) { toast("Name cannot be empty"); return; }
                        final File f = new File(currentDirectoryPath, name);
                        if (f.exists()) { toast("Already exists"); return; }
                        try {
                            f.createNewFile();
                            browseDirectory(currentDirectoryPath);
                            askOpenNewFile(f);
                        } catch (Exception e) { toast("Error: " + e.getMessage()); }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void askOpenNewFile(final File f) {
        new AlertDialog.Builder(this)
                .setTitle("File Created")
                .setMessage("Open " + f.getName() + "?")
                .setPositiveButton("Open", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { openFile(f.getAbsolutePath()); }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showCreateDirectoryDialog() {
        final EditText input = new EditText(this);
        input.setHint("Directory name");
        new AlertDialog.Builder(this)
                .setTitle("New Folder")
                .setView(input)
                .setPositiveButton("Create", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) { toast("Name cannot be empty"); return; }
                        File dir = new File(currentDirectoryPath, name);
                        if (dir.exists()) { toast("Already exists"); return; }
                        if (dir.mkdir()) { toast("Folder created"); browseDirectory(currentDirectoryPath); }
                        else              toast("Failed to create folder");
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showFileOptionsDialog(final String filePath) {
        final File f = new File(filePath);
        String[] opts = f.isDirectory()
                ? new String[]{"Open", "Rename", "Delete", "New File", "New Folder", "Import Files"}
                : new String[]{"Open", "Rename", "Delete", "Share", "Info", "Copy Path"};

        new AlertDialog.Builder(this)
                .setTitle(f.getName())
                .setItems(opts, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int which) {
                        if (f.isDirectory()) {
                            switch (which) {
                                case 0: browseDirectory(filePath);        break;
                                case 1: showRenameDialog(f);             break;
                                case 2: confirmDelete(f);                break;
                                case 3: showCreateFileDialog();          break;
                                case 4: showCreateDirectoryDialog();     break;
                                case 5: importMultipleFiles();           break;
                            }
                        } else {
                            switch (which) {
                                case 0: openFile(filePath);              break;
                                case 1: showRenameDialog(f);             break;
                                case 2: confirmDelete(f);                break;
                                case 3: shareFile(f);                    break;
                                case 4: showFileInfo(f);                 break;
                                case 5: copyToClipboard(f.getAbsolutePath()); break;
                            }
                        }
                    }
                })
                .show();
    }

    private void showRenameDialog(final File f) {
        final EditText input = new EditText(this);
        input.setText(f.getName());
        input.selectAll();
        new AlertDialog.Builder(this)
                .setTitle("Rename")
                .setView(input)
                .setPositiveButton("Rename", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) {
                        String name = input.getText().toString().trim();
                        if (name.isEmpty()) { toast("Name cannot be empty"); return; }
                        File dest = new File(f.getParent(), name);
                        if (f.renameTo(dest)) {
                            toast("Renamed to: " + name);
                            browseDirectory(currentDirectoryPath);
                        } else {
                            toast("Rename failed");
                        }
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void confirmDelete(final File f) {
        new AlertDialog.Builder(this)
                .setTitle("Delete")
                .setMessage("Delete " + f.getName() + "?")
                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { deleteRecursive(f); }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteRecursive(File f) {
        if (f.isDirectory()) {
            File[] children = f.listFiles();
            if (children != null) for (File c : children) deleteRecursive(c);
        }
        if (f.delete()) {
            toast("Deleted: " + f.getName());
            browseDirectory(currentDirectoryPath);
        } else {
            toast("Delete failed: " + f.getName());
        }
    }

    private void shareFile(File f) {
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/plain");
        i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(f));
        i.putExtra(Intent.EXTRA_TEXT, "File: " + f.getName());
        startActivity(Intent.createChooser(i, "Share"));
    }

    private void showFileInfo(File f) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String msg = "Name: "     + f.getName()              + "\n"
                   + "Path: "     + f.getAbsolutePath()      + "\n"
                   + "Size: "     + formatSize(f.length())   + "\n"
                   + "Modified: " + sdf.format(new Date(f.lastModified())) + "\n"
                   + "Read: "     + f.canRead()              + " | Write: " + f.canWrite();
        new AlertDialog.Builder(this)
                .setTitle("File Info")
                .setMessage(msg)
                .setPositiveButton("OK", null)
                .show();
    }

    private void copyToClipboard(String text) {
        ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        cm.setPrimaryClip(ClipData.newPlainText("path", text));
        toast("Path copied");
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Import files
    // ────────────────────────────────────────────────────────────────────────

    private void importMultipleFiles() {
        Intent i = new Intent(Intent.ACTION_GET_CONTENT);
        i.setType("*/*");
        i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        i.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(i, "Select Files"), REQUEST_IMPORT);
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req != REQUEST_IMPORT || res != RESULT_OK || data == null) return;

        ClipData clip = data.getClipData();
        if (clip != null) {
            for (int i = 0; i < clip.getItemCount(); i++) {
                Uri uri = clip.getItemAt(i).getUri();
                if (uri != null) copyUriToDirectory(uri);
            }
        } else {
            Uri uri = data.getData();
            if (uri != null) copyUriToDirectory(uri);
        }
    }

    private void copyUriToDirectory(final Uri uri) {
        showProgress(true);
        new Thread(new Runnable() {
            @Override public void run() {
                try {
                    final String name = resolveFileName(uri);
                    final File dest  = new File(currentDirectoryPath, name);

                    if (dest.exists()) {
                        runOnUiThread(new Runnable() {
                            @Override public void run() {
                                showProgress(false);
                                showOverwriteDialog(dest, uri);
                            }
                        });
                        return;
                    }
                    writeUriToFile(uri, dest);
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            showProgress(false);
                            toast("Imported: " + name);
                            browseDirectory(currentDirectoryPath);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override public void run() {
                            showProgress(false);
                            toast("Import failed: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }

    private void writeUriToFile(Uri uri, File dest) throws IOException {
        InputStream  in  = getContentResolver().openInputStream(uri);
        OutputStream out = new FileOutputStream(dest);
        byte[] buf = new byte[8192];
        int len;
        while ((len = in.read(buf)) > 0) out.write(buf, 0, len);
        out.close();
        in.close();
    }

    private void showOverwriteDialog(final File dest, final Uri uri) {
        new AlertDialog.Builder(this)
                .setTitle("File Exists")
                .setMessage(dest.getName() + " already exists. Overwrite?")
                .setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface d, int w) { copyUriToDirectory(uri); }
                })
                .setNegativeButton("Skip", null)
                .show();
    }

    private String resolveFileName(Uri uri) {
        if ("content".equals(uri.getScheme())) {
            Cursor c = getContentResolver().query(uri, null, null, null, null);
            if (c != null && c.moveToFirst()) {
                int col = c.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (col != -1) {
                    String name = c.getString(col);
                    c.close();
                    return name;
                }
                c.close();
            }
        }
        String path = uri.getPath();
        int cut = path.lastIndexOf('/');
        return cut >= 0 ? path.substring(cut + 1) : path;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  UI helpers
    // ────────────────────────────────────────────────────────────────────────

    private void showFileBrowser() {
        fileBrowserLayout.setVisibility(View.VISIBLE);
        editorLayout.setVisibility(View.GONE);
        webViewLayout.setVisibility(View.GONE);
        isWebViewVisible = false;
        currentFile = null;
        updateTitle();
    }

    private void showEditor() {
        fileBrowserLayout.setVisibility(View.GONE);
        editorLayout.setVisibility(View.VISIBLE);
        webViewLayout.setVisibility(View.GONE);
        isWebViewVisible = false;
        btnPlayPreview.setImageResource(R.drawable.ic_play);
        updateTitle();
    }

    private void updateTitle() {
        if (currentFile != null) {
            setTitle((isModified ? "* " : "") + currentFile.getName());
        } else {
            setTitle("Editor — " + (currentDirectoryPath != null
                    ? new File(currentDirectoryPath).getName() : ""));
        }
    }

    private void updateFileInfo() {
        if (currentFile == null) { fileInfo.setText(""); return; }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        fileInfo.setText(currentFile.getName()
                + "  |  " + formatSize(currentFile.length())
                + "  |  " + sdf.format(new Date(currentFile.lastModified())));
    }

    private void showProgress(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
            }
        });
    }

    private void toast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override public void run() {
                Toast.makeText(EditorActivity.this, msg, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatSize(long bytes) {
        if (bytes < 1024)            return bytes + " B";
        if (bytes < 1024 * 1024)    return String.format("%.1f KB", bytes / 1024.0);
        return                              String.format("%.1f MB", bytes / (1024.0 * 1024));
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Menu
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "New File")    .setIcon(R.drawable.ic_add);
        menu.add(0, 2, 0, "New Folder")  .setIcon(R.drawable.ic_menu_folder);
        menu.add(0, 3, 0, "Import Files").setIcon(R.drawable.ic_upload);
        menu.add(0, 4, 0, "Refresh")     .setIcon(R.drawable.ic_history);
        menu.add(0, 5, 0, "Exit Editor") .setIcon(R.drawable.ic_close);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1: showCreateFileDialog();      break;
            case 2: showCreateDirectoryDialog(); break;
            case 3: importMultipleFiles();        break;
            case 4: if (currentDirectoryPath != null) browseDirectory(currentDirectoryPath); break;
            case 5: finish();                    break;
        }
        return true;
    }

    // ────────────────────────────────────────────────────────────────────────
    //  Back press
    // ────────────────────────────────────────────────────────────────────────

    @Override
    public void onBackPressed() {
        if (isWebViewVisible) {
            closeWebView();
            return;
        }
        if (editorLayout.getVisibility() == View.VISIBLE) {
            if (isModified) {
                new AlertDialog.Builder(this)
                        .setTitle("Unsaved Changes")
                        .setMessage("Save before leaving?")
                        .setPositiveButton("Save", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                saveFile();
                                showFileBrowser();
                                browseDirectory(currentDirectoryPath);
                            }
                        })
                        .setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                            @Override public void onClick(DialogInterface d, int w) {
                                showFileBrowser();
                                browseDirectory(currentDirectoryPath);
                            }
                        })
                        .setNeutralButton("Cancel", null)
                        .show();
            } else {
                showFileBrowser();
                browseDirectory(currentDirectoryPath);
            }
        } else {
            super.onBackPressed();
        }
    }
}
