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
import android.os.Environment;
import android.os.Handler;
import android.provider.OpenableColumns;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class EditorActivity extends Activity {
    
    private EditText editText;
    private TextView fileInfo;
    private ProgressBar progressBar;
    private String currentFilePath;
    private File currentFile;
    private boolean isModified = false;
    private Handler autoSaveHandler = new Handler();
    private Runnable autoSaveRunnable;
    private ListView fileListView;
    private LinearLayout fileBrowserLayout;
    private LinearLayout editorLayout;
    private LinearLayout webViewLayout;
    private String currentDirectoryPath;
    private ArrayAdapter<String> fileListAdapter;
    private ArrayList<String> fileListItems;
    private ArrayList<String> fileListPaths;
    private Button btnNewFile, btnSave, btnUndo, btnRedo;
    private EditText searchEditText;
    private int lastSearchIndex = -1;
    private String lastSearchQuery = "";
    private Button btnSearchNext, btnSearchPrev;
    private WebView webView;
    private boolean isWebViewVisible = false;
    private LinearLayout bottomBar;
    private ImageButton btnPlayPreview;
    private static final int REQUEST_IMPORT_FILES = 100;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.editor);
        
        initializeViews();
        setupFileBrowser();
        setupEditor();
        setupAutoSave();
        setupWebView();
        
        String filePath = getIntent().getStringExtra("file_path");
        if (filePath != null && new File(filePath).exists()) {
            openFile(filePath);
        } else {
            showFileBrowser();
            browseDirectory(getFilesDir().getAbsolutePath());
        }
    }
    
    private void initializeViews() {
        editText = findViewById(R.id.editText);
        fileInfo = findViewById(R.id.fileInfo);
        progressBar = findViewById(R.id.progressBar);
        fileListView = findViewById(R.id.fileListView);
        fileBrowserLayout = findViewById(R.id.fileBrowserLayout);
        editorLayout = findViewById(R.id.editorLayout);
        webViewLayout = findViewById(R.id.webViewLayout);
        btnNewFile = findViewById(R.id.btnNewFile);
        btnSave = findViewById(R.id.btnSave);
        btnUndo = findViewById(R.id.btnUndo);
        btnRedo = findViewById(R.id.btnRedo);
        searchEditText = findViewById(R.id.searchEditText);
        btnSearchNext = findViewById(R.id.btnSearchNext);
        btnSearchPrev = findViewById(R.id.btnSearchPrev);
        webView = findViewById(R.id.webView);
        bottomBar = findViewById(R.id.bottomBar);
        btnPlayPreview = findViewById(R.id.btnPlayPreview);
        
        editText.setMovementMethod(new ScrollingMovementMethod());
        editText.setTextSize(14);
        editText.setTextIsSelectable(true);
        editText.setTypeface(Typeface.MONOSPACE);
        
        progressBar.setVisibility(View.GONE);
        
        btnPlayPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleWebView();
            }
        });
    }
    
    private void setupFileBrowser() {
        fileListItems = new ArrayList<String>();
        fileListPaths = new ArrayList<String>();
        
        fileListAdapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, fileListItems) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView textView = (TextView) view;
                String path = fileListPaths.get(position);
                File file = new File(path);
                
                if (file.isDirectory()) {
                    textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_save, 0, 0, 0);
                } else {
                    String fileName = file.getName().toLowerCase();
                    if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                        textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_gallery, 0, 0, 0);
                    } else if (fileName.endsWith(".php")) {
                        textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
                    } else if (fileName.endsWith(".txt")) {
                        textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_info_details, 0, 0, 0);
                    } else {
                        textView.setCompoundDrawablesWithIntrinsicBounds(android.R.drawable.ic_menu_edit, 0, 0, 0);
                    }
                }
                textView.setCompoundDrawablePadding(10);
                return view;
            }
        };
        fileListView.setAdapter(fileListAdapter);
        
        fileListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String path = fileListPaths.get(position);
                File file = new File(path);
                
                if (file.isDirectory()) {
                    browseDirectory(path);
                } else {
                    openFile(path);
                }
            }
        });
        
        fileListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                String path = fileListPaths.get(position);
                showFileOptionsDialog(path);
                return true;
            }
        });
        
        btnNewFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showCreateFileDialog();
            }
        });
        
        btnSave.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editorLayout.getVisibility() == View.VISIBLE) {
                    saveFile();
                }
            }
        });
        
        btnUndo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editText != null) {
                    android.text.method.TextKeyListener.clear(editText.getText());
                }
            }
        });
        
        btnRedo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("temp", "");
                clipboard.setPrimaryClip(clip);
            }
        });
        
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                lastSearchQuery = s.toString();
                lastSearchIndex = -1;
                if (!lastSearchQuery.isEmpty()) {
                    searchNext();
                } else {
                    editText.setSelection(0);
                }
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
        
        btnSearchNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchNext();
            }
        });
        
        btnSearchPrev.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                searchPrev();
            }
        });
    }
    
    private void setupEditor() {
        editText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!isModified) {
                    isModified = true;
                    updateTitle();
                }
                autoSaveHandler.removeCallbacks(autoSaveRunnable);
                autoSaveHandler.postDelayed(autoSaveRunnable, 3000);
            }
            
            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setLoadWithOverviewMode(true);
        webView.getSettings().setUseWideViewPort(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setDisplayZoomControls(false);
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                showProgress(false);
            }
        });
        
        Button btnCloseWebView = findViewById(R.id.btnCloseWebView);
        if (btnCloseWebView != null) {
            btnCloseWebView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    closeWebView();
                }
            });
        }
    }
    
    private void setupAutoSave() {
        autoSaveRunnable = new Runnable() {
            @Override
            public void run() {
                if (isModified && currentFile != null) {
                    saveFile();
                    showMessage("Auto-saved");
                }
            }
        };
    }
    
    private void closeWebView() {
        if (webViewLayout != null) {
            webViewLayout.setVisibility(View.GONE);
            isWebViewVisible = false;
            if (btnPlayPreview != null) {
                btnPlayPreview.setImageResource(android.R.drawable.ic_media_play);
            }
            webView.loadUrl("about:blank");
        }
    }
    
    private void toggleWebView() {
        if (isWebViewVisible) {
            closeWebView();
        } else {
            if (currentFile != null && isHtmlFile(currentFile.getName())) {
                loadHtmlInWebView();
                webViewLayout.setVisibility(View.VISIBLE);
                isWebViewVisible = true;
                btnPlayPreview.setImageResource(android.R.drawable.ic_media_pause);
            } else {
                showMessage("Please open an HTML file to preview");
            }
        }
    }
    
    private boolean isHtmlFile(String fileName) {
        String lowerName = fileName.toLowerCase();
        return lowerName.endsWith(".html") || lowerName.endsWith(".htm");
    }
    
    private void loadHtmlInWebView() {
        String content = editText.getText().toString();
        
        if (!content.toLowerCase().contains("<html")) {
            content = "<!DOCTYPE html>\n<html>\n<head>\n<meta charset=\"UTF-8\">\n" +
                      "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
                      "<title>" + currentFile.getName() + "</title>\n</head>\n<body>\n" +
                      content + "\n</body>\n</html>";
        }
        
        String baseUrl = "file://" + currentFile.getParent() + "/";
        webView.loadDataWithBaseURL(baseUrl, content, "text/html", "UTF-8", null);
        showProgress(true);
    }
    
    private void showFileBrowser() {
        fileBrowserLayout.setVisibility(View.VISIBLE);
        editorLayout.setVisibility(View.GONE);
        if (webViewLayout != null) {
            webViewLayout.setVisibility(View.GONE);
            isWebViewVisible = false;
        }
        updateTitle();
    }
    
    private void showEditor() {
        fileBrowserLayout.setVisibility(View.GONE);
        editorLayout.setVisibility(View.VISIBLE);
        if (webViewLayout != null) {
            webViewLayout.setVisibility(View.GONE);
            isWebViewVisible = false;
            if (btnPlayPreview != null) {
                btnPlayPreview.setImageResource(android.R.drawable.ic_media_play);
            }
        }
        updateTitle();
    }
    
    private void browseDirectory(String path) {
        currentDirectoryPath = path;
        File dir = new File(path);
        
        if (!dir.exists() || !dir.isDirectory()) {
            showMessage("Cannot access directory: " + path);
            return;
        }
        
        fileListItems.clear();
        fileListPaths.clear();
        
        if (!path.equals(Environment.getExternalStorageDirectory().getAbsolutePath()) && 
            !path.equals(getFilesDir().getAbsolutePath())) {
            File parent = dir.getParentFile();
            if (parent != null) {
                fileListItems.add(".. (Parent Directory)");
                fileListPaths.add(parent.getAbsolutePath());
            }
        }
        
        File[] files = dir.listFiles();
        if (files != null) {
            Arrays.sort(files, new Comparator<File>() {
                @Override
                public int compare(File f1, File f2) {
                    if (f1.isDirectory() && !f2.isDirectory()) return -1;
                    if (!f1.isDirectory() && f2.isDirectory()) return 1;
                    return f1.getName().compareToIgnoreCase(f2.getName());
                }
            });
            
            for (File file : files) {
                if (file.canRead()) {
                    String name = file.isDirectory() ? "📁 " + file.getName() : "📄 " + file.getName();
                    fileListItems.add(name);
                    fileListPaths.add(file.getAbsolutePath());
                }
            }
        }
        
        fileListAdapter.notifyDataSetChanged();
        updateTitle();
    }
    
    private void openFile(final String filePath) {
        currentFilePath = filePath;
        currentFile = new File(filePath);
        
        if (!currentFile.exists()) {
            showMessage("File does not exist: " + filePath);
            return;
        }
        
        showEditor();
        showProgress(true);
        closeWebView();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    StringBuilder content = new StringBuilder();
                    BufferedReader reader = new BufferedReader(new FileReader(currentFile));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        content.append(line).append("\n");
                    }
                    reader.close();
                    
                    final String fileContent = content.toString();
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            editText.setText(fileContent);
                            isModified = false;
                            updateTitle();
                            showProgress(false);
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            fileInfo.setText("File: " + currentFile.getName() + " | Size: " + 
                                           formatFileSize(currentFile.length()) + " | Modified: " + 
                                           sdf.format(new Date(currentFile.lastModified())));
                            
                            if (isHtmlFile(currentFile.getName())) {
                                bottomBar.setVisibility(View.VISIBLE);
                            } else {
                                bottomBar.setVisibility(View.GONE);
                            }
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showProgress(false);
                            showMessage("Error opening file: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
    
    private void saveFile() {
        if (currentFile == null) {
            showSaveAsDialog();
            return;
        }
        
        showProgress(true);
        final String content = editText.getText().toString();
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(currentFile));
                    writer.write(content);
                    writer.close();
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            isModified = false;
                            updateTitle();
                            showProgress(false);
                            showMessage("File saved: " + currentFile.getName());
                            
                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                            fileInfo.setText("File: " + currentFile.getName() + " | Size: " + 
                                           formatFileSize(currentFile.length()) + " | Modified: " + 
                                           sdf.format(new Date(currentFile.lastModified())));
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showProgress(false);
                            showMessage("Error saving file: " + e.getMessage());
                        }
                    });
                }
            }
        }).start();
    }
    
    private void showSaveAsDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Save As");
        
        final EditText input = new EditText(this);
        input.setHint("Enter filename");
        builder.setView(input);
        
        builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String filename = input.getText().toString();
                if (filename.isEmpty()) {
                    showMessage("Filename cannot be empty");
                    return;
                }
                currentFile = new File(currentDirectoryPath, filename);
                currentFilePath = currentFile.getAbsolutePath();
                saveFile();
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void importMultipleFiles() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        startActivityForResult(Intent.createChooser(intent, "Select Files"), REQUEST_IMPORT_FILES);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_IMPORT_FILES && resultCode == RESULT_OK && data != null) {
            ClipData clipData = data.getClipData();
            
            if (clipData != null) {
                for (int i = 0; i < clipData.getItemCount(); i++) {
                    Uri uri = clipData.getItemAt(i).getUri();
                    if (uri != null) {
                        copyFileToDirectory(uri);
                    }
                }
                showMessage("Importing " + clipData.getItemCount() + " files...");
            } else {
                Uri uri = data.getData();
                if (uri != null) {
                    copyFileToDirectory(uri);
                }
            }
        }
    }
    
    private void copyFileToDirectory(final Uri uri) {
        showProgress(true);
        
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                  final String fileName = getFileName(uri);
                   final File destFile = new File(currentDirectoryPath, fileName);
                    
                    if (destFile.exists()) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                showOverwriteDialog(destFile, uri);
                            }
                        });
                        return;
                    }
                    
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    OutputStream outputStream = new FileOutputStream(destFile);
                    
                    byte[] buffer = new byte[8192];
                    int length;
                    while ((length = inputStream.read(buffer)) > 0) {
                        outputStream.write(buffer, 0, length);
                    }
                    
                    outputStream.close();
                    inputStream.close();
                    
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMessage("Imported: " + fileName);
                            browseDirectory(currentDirectoryPath);
                            showProgress(false);
                        }
                    });
                } catch (final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            showMessage("Error importing: " + e.getMessage());
                            showProgress(false);
                        }
                    });
                }
            }
        }).start();
    }
    
    private void showOverwriteDialog(final File file, final Uri uri) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("File Exists");
        builder.setMessage(file.getName() + " already exists. Overwrite?");
        builder.setPositiveButton("Overwrite", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                copyFileToDirectory(uri);
            }
        });
        builder.setNegativeButton("Skip", null);
        builder.show();
        showProgress(false);
    }
    
    private String getFileName(Uri uri) {
        String fileName = null;
        if (uri.getScheme().equals("content")) {
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    fileName = cursor.getString(nameIndex);
                }
                cursor.close();
            }
        }
        
        if (fileName == null) {
            fileName = uri.getPath();
            int cut = fileName.lastIndexOf('/');
            if (cut != -1) {
                fileName = fileName.substring(cut + 1);
            }
        }
        
        return fileName;
    }
    
    private void showCreateFileDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New File");
        
        final EditText input = new EditText(this);
        input.setHint("Enter filename (e.g., index.php)");
        builder.setView(input);
        
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final String filename = input.getText().toString();
                if (filename.isEmpty()) {
                    showMessage("Filename cannot be empty");
                    return;
                }
                
                final File newFile = new File(currentDirectoryPath, filename);
                if (newFile.exists()) {
                    showMessage("File already exists");
                    return;
                }
                
                try {
                    newFile.createNewFile();
                    showMessage("File created: " + filename);
                    browseDirectory(currentDirectoryPath);
                    
                    AlertDialog.Builder openBuilder = new AlertDialog.Builder(EditorActivity.this);
                    openBuilder.setTitle("File Created");
                    openBuilder.setMessage("Do you want to open " + filename + "?");
                    openBuilder.setPositiveButton("Open", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            openFile(newFile.getAbsolutePath());
                        }
                    });
                    openBuilder.setNegativeButton("Cancel", null);
                    openBuilder.show();
                } catch (Exception e) {
                    showMessage("Error creating file: " + e.getMessage());
                }
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showCreateDirectoryDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Create New Directory");
        
        final EditText input = new EditText(this);
        input.setHint("Enter directory name");
        builder.setView(input);
        
        builder.setPositiveButton("Create", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String dirname = input.getText().toString();
                if (dirname.isEmpty()) {
                    showMessage("Directory name cannot be empty");
                    return;
                }
                
                File newDir = new File(currentDirectoryPath, dirname);
                if (newDir.exists()) {
                    showMessage("Directory already exists");
                    return;
                }
                
                if (newDir.mkdir()) {
                    showMessage("Directory created: " + dirname);
                    browseDirectory(currentDirectoryPath);
                } else {
                    showMessage("Error creating directory");
                }
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void showFileOptionsDialog(final String filePath) {
        final File file = new File(filePath);
        String[] options;
        
        if (file.isDirectory()) {
            options = new String[]{"Open", "Delete", "Rename", "Create File", "Create Directory", "Import Files"};
        } else {
            options = new String[]{"Open", "Delete", "Rename", "Share", "Get Info", "Copy Path"};
        }
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(file.getName());
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case 0:
                        if (file.isDirectory()) {
                            browseDirectory(filePath);
                        } else {
                            openFile(filePath);
                        }
                        break;
                    case 1:
                        confirmDelete(file);
                        break;
                    case 2:
                        showRenameDialog(file);
                        break;
                    case 3:
                        if (file.isDirectory()) {
                            showCreateFileDialog();
                        } else {
                            shareFile(file);
                        }
                        break;
                    case 4:
                        if (file.isDirectory()) {
                            showCreateDirectoryDialog();
                        } else {
                            showFileInfo(file);
                        }
                        break;
                    case 5:
                        if (!file.isDirectory()) {
                            copyToClipboard(file.getAbsolutePath());
                        } else {
                            importMultipleFiles();
                        }
                        break;
                }
            }
        });
        builder.show();
    }
    
    private void copyToClipboard(String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("path", text);
        clipboard.setPrimaryClip(clip);
        showMessage("Path copied to clipboard");
    }
    
    private void confirmDelete(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Delete");
        builder.setMessage("Are you sure you want to delete " + file.getName() + "?");
        builder.setPositiveButton("Delete", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                deleteFileRecursive(file);
            }
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void deleteFileRecursive(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            if (files != null) {
                for (File child : files) {
                    deleteFileRecursive(child);
                }
            }
        }
        
        if (file.delete()) {
            showMessage("Deleted: " + file.getName());
            browseDirectory(currentDirectoryPath);
        } else {
            showMessage("Error deleting: " + file.getName());
        }
    }
    
    private void showRenameDialog(final File file) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename");
        
        final EditText input = new EditText(this);
        input.setText(file.getName());
        input.selectAll();
        builder.setView(input);
        
        builder.setPositiveButton("Rename", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newName = input.getText().toString();
                if (newName.isEmpty()) {
                    showMessage("Name cannot be empty");
                    return;
                }
                
                File newFile = new File(file.getParent(), newName);
                if (file.renameTo(newFile)) {
                    showMessage("Renamed to: " + newName);
                    browseDirectory(currentDirectoryPath);
                } else {
                    showMessage("Error renaming file");
                }
            }
        });
        
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }
    
    private void shareFile(File file) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(file));
        shareIntent.putExtra(Intent.EXTRA_TEXT, "Check out this file: " + file.getName());
        startActivity(Intent.createChooser(shareIntent, "Share File"));
    }
    
    private void showFileInfo(File file) {
        StringBuilder info = new StringBuilder();
        info.append("Name: ").append(file.getName()).append("\n");
        info.append("Path: ").append(file.getAbsolutePath()).append("\n");
        info.append("Size: ").append(formatFileSize(file.length())).append("\n");
        info.append("Modified: ").append(new Date(file.lastModified())).append("\n");
        info.append("Readable: ").append(file.canRead()).append("\n");
        info.append("Writable: ").append(file.canWrite()).append("\n");
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("File Info");
        builder.setMessage(info.toString());
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    private String formatFileSize(long size) {
        if (size < 1024) return size + " B";
        if (size < 1024 * 1024) return String.format("%.2f KB", size / 1024.0);
        if (size < 1024 * 1024 * 1024) return String.format("%.2f MB", size / (1024.0 * 1024));
        return String.format("%.2f GB", size / (1024.0 * 1024 * 1024));
    }
    
    private void searchNext() {
        if (lastSearchQuery.isEmpty()) return;
        
        String text = editText.getText().toString();
        int start = lastSearchIndex + 1;
        int index = text.indexOf(lastSearchQuery, start);
        
        if (index >= 0) {
            lastSearchIndex = index;
            editText.setSelection(index, index + lastSearchQuery.length());
        } else {
            showMessage("No more matches");
            lastSearchIndex = -1;
        }
    }
    
    private void searchPrev() {
        if (lastSearchQuery.isEmpty()) return;
        
        String text = editText.getText().toString();
        int start = lastSearchIndex - 1;
        int index = text.lastIndexOf(lastSearchQuery, start);
        
        if (index >= 0) {
            lastSearchIndex = index;
            editText.setSelection(index, index + lastSearchQuery.length());
        } else {
            showMessage("No more matches");
            lastSearchIndex = -1;
        }
    }
    
    private void updateTitle() {
        if (currentFile != null) {
            String title = currentFile.getName();
            if (isModified) {
                title = "* " + title;
            }
            setTitle(title);
        } else {
            setTitle("File Editor - " + currentDirectoryPath);
        }
    }
    
    private void showProgress(final boolean show) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (show) {
                    progressBar.setVisibility(View.VISIBLE);
                } else {
                    progressBar.setVisibility(View.GONE);
                }
            }
        });
    }
    
    private void showMessage(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(EditorActivity.this, message, Toast.LENGTH_SHORT).show();
            }
        });
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "New File").setIcon(android.R.drawable.ic_menu_add);
        menu.add(0, 2, 0, "New Folder").setIcon(android.R.drawable.ic_menu_save);
        menu.add(0, 3, 0, "Import Files").setIcon(android.R.drawable.ic_menu_upload);
        menu.add(0, 4, 0, "Refresh").setIcon(android.R.drawable.ic_menu_recent_history);
        menu.add(0, 5, 0, "Exit Editor").setIcon(android.R.drawable.ic_menu_close_clear_cancel);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        switch (id) {
            case 1:
                showCreateFileDialog();
                break;
            case 2:
                showCreateDirectoryDialog();
                break;
            case 3:
                importMultipleFiles();
                break;
            case 4:
                browseDirectory(currentDirectoryPath);
                break;
            case 5:
                finish();
                break;
        }
        
        return super.onOptionsItemSelected(item);
    }
    
    @Override
    public void onBackPressed() {
        if (editorLayout.getVisibility() == View.VISIBLE) {
            if (isWebViewVisible) {
                closeWebView();
            } else if (isModified) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Unsaved Changes");
                builder.setMessage("Do you want to save before leaving?");
                builder.setPositiveButton("Save", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        saveFile();
                        showFileBrowser();
                    }
                });
                builder.setNegativeButton("Discard", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showFileBrowser();
                    }
                });
                builder.setNeutralButton("Cancel", null);
                builder.show();
            } else {
                showFileBrowser();
            }
        } else {
            super.onBackPressed();
        }
    }
}