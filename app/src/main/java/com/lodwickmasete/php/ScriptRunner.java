package com.lodwickmasete.php;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;

/**
 * ScriptRunner — executes shell scripts from assets/scripts/
 * with optional env-var overrides injected from Java (Android settings).
 *
 * Scripts are copied to getFilesDir()/scripts/ on first run.
 * Java passes config values as environment variables so the sh scripts
 * pick them up via ${VAR:-default} syntax.
 */
public class ScriptRunner {

    public interface OutputListener {
        void onLine(String line);
        void onDone(int exitCode);
        void onError(String message);
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Map<String, Process> runningScripts = new HashMap<>();

    // ── Base paths ────────────────────────────────────────────────────────────
    private static final String DATA_DIR = "/data/data/com.lodwickmasete.php/files";

    public ScriptRunner(Context context) {
        this.context = context;
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /** Run a named script (e.g. "start_apache.sh") with optional env overrides. */
    public void run(final String scriptName,
                    final Map<String, String> env,
                    final OutputListener listener) {

        final File scriptFile = ensureScript(scriptName);
        if (scriptFile == null) {
            notifyError(listener, "Script not found: " + scriptName);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    // Make executable
                    scriptFile.setExecutable(true, false);

                    ProcessBuilder pb = new ProcessBuilder("sh", scriptFile.getAbsolutePath());
                    pb.redirectErrorStream(true);
                    pb.directory(new File(DATA_DIR));

                    // Default env from process
                    Map<String, String> procEnv = pb.environment();

                    // Base LD_LIBRARY_PATH so all scripts inherit it
                    procEnv.put("LD_LIBRARY_PATH",
                            DATA_DIR + "/lib/common:" +
                            DATA_DIR + "/lib/httpd:" +
                            DATA_DIR + "/lib/php:" +
                            DATA_DIR + "/lib/php-fpm:" +
                            DATA_DIR + "/lib/mysql");

                    procEnv.put("TMPDIR", DATA_DIR + "/tmp");
                    procEnv.put("TEMP",   DATA_DIR + "/tmp");
                    procEnv.put("TMP",    DATA_DIR + "/tmp");

                    // Caller-supplied overrides (document root, ports, etc.)
                    if (env != null) {
                        procEnv.putAll(env);
                    }

                    final Process process = pb.start();
                    runningScripts.put(scriptName, process);

                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(process.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        final String l = line;
                        mainHandler.post(new Runnable() {
                            @Override
                            public void run() {
                                if (listener != null) listener.onLine(l);
                            }
                        });
                    }

                    final int code = process.waitFor();
                    runningScripts.remove(scriptName);
                    mainHandler.post(new Runnable() {
                        @Override
                        public void run() {
                            if (listener != null) listener.onDone(code);
                        }
                    });

                } catch (final Exception e) {
                    runningScripts.remove(scriptName);
                    notifyError(listener, e.getMessage());
                }
            }
        }).start();
    }

    /** Stop a running script by name (destroys the process). */
    public void stop(String scriptName) {
        Process p = runningScripts.get(scriptName);
        if (p != null) {
            p.destroy();
            runningScripts.remove(scriptName);
        }
    }

    /** Stop every running script. */
    public void stopAll() {
        for (Process p : runningScripts.values()) {
            if (p != null) {
                try { p.destroy(); } catch (Exception ignored) {}
            }
        }
        runningScripts.clear();
    }

    public boolean isRunning(String scriptName) {
        Process p = runningScripts.get(scriptName);
        if (p == null) return false;
        try {
            p.exitValue();
            runningScripts.remove(scriptName);
            return false;
        } catch (IllegalThreadStateException e) {
            return true; // still alive
        }
    }

    // ── Script Installation ───────────────────────────────────────────────────

    /**
     * Copies script from assets/scripts/<name> to getFilesDir()/scripts/<name>
     * only if the destination doesn't exist yet.
     */
    public File ensureScript(String scriptName) {
        File scriptsDir = new File(context.getFilesDir(), "scripts");
        if (!scriptsDir.exists()) scriptsDir.mkdirs();

        File dest = new File(scriptsDir, scriptName);
        if (!dest.exists()) {
            try {
                InputStream is = context.getAssets().open("scripts/" + scriptName);
                FileOutputStream fos = new FileOutputStream(dest);
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                fos.close();
                is.close();
                dest.setExecutable(true, false);
            } catch (IOException e) {
                return null;
            }
        }
        return dest;
    }

    /** Force-reinstall all known scripts from assets (call after app update). */
    public void installAllScripts() {
        String[] scripts = {
            "start_all.sh", "stop_all.sh",
            "start_apache.sh", "stop_apache.sh",
            "start_php_fpm.sh", "stop_php_fpm.sh",
            "start_mariadb.sh", "stop_mariadb.sh"
        };
        File scriptsDir = new File(context.getFilesDir(), "scripts");
        if (!scriptsDir.exists()) scriptsDir.mkdirs();

        for (String name : scripts) {
            File dest = new File(scriptsDir, name);
            try {
                InputStream is = context.getAssets().open("scripts/" + name);
                FileOutputStream fos = new FileOutputStream(dest);
                byte[] buf = new byte[4096];
                int n;
                while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
                fos.close();
                is.close();
                dest.setExecutable(true, false);
            } catch (IOException ignored) {}
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void notifyError(final OutputListener listener, final String msg) {
        mainHandler.post(new Runnable() {
            @Override
            public void run() {
                if (listener != null) listener.onError(msg);
            }
        });
    }
}