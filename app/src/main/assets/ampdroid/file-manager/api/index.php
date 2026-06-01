<?php
/**
 * Ampdroid File Manager API
 * /ampdroid/file-manager/api/index.php
 *
 * Reads document_root from app_config.json at:
 *   /data/data/com.lodwickmasete.php/files/app_config.json
 *
 * All responses are JSON.
 * Actions: list, mkdir, mkfile, rename, delete, copy, move, zip, extract,
 *          upload, exportzip, download, diskusage
 */
//sleep(1);
header('Content-Type: application/json; charset=utf-8');
header('X-Content-Type-Options: nosniff');

/* ============================================================
   HELPERS
============================================================ */

function json_ok($extra = []) {
    echo json_encode(array_merge(['ok' => true], $extra));
    exit;
}

function json_err($msg, $code = 400) {
    http_response_code($code);
    echo json_encode(['ok' => false, 'error' => $msg]);
    exit;
}

/* Recursively remove a directory */
function rmdir_recursive($dir) {
    if (!is_dir($dir)) {
        return unlink($dir);
    }
    $items = scandir($dir);
    foreach ($items as $item) {
        if ($item === '.' || $item === '..') continue;
        $full = $dir . DIRECTORY_SEPARATOR . $item;
        if (is_dir($full)) {
            rmdir_recursive($full);
        } else {
            unlink($full);
        }
    }
    return rmdir($dir);
}

/* Human-readable file size */
function human_size($bytes) {
    if ($bytes < 1024)        return $bytes . ' B';
    if ($bytes < 1048576)     return round($bytes / 1024, 1) . ' KB';
    if ($bytes < 1073741824)  return round($bytes / 1048576, 1) . ' MB';
    return round($bytes / 1073741824, 2) . ' GB';
}

/* Get directory size recursively */
function dir_size($path) {
    $size = 0;
    if (!is_dir($path)) return filesize($path) ?: 0;
    $it = new RecursiveIteratorIterator(
        new RecursiveDirectoryIterator($path, FilesystemIterator::SKIP_DOTS)
    );
    foreach ($it as $file) {
        $size += $file->getSize();
    }
    return $size;
}

/* Unix permissions string */
function perms_string($path) {
    $perms = fileperms($path);
    $info  = '';
    $info .= (($perms & 0x0100) ? 'r' : '-');
    $info .= (($perms & 0x0080) ? 'w' : '-');
    $info .= (($perms & 0x0040) ? (($perms & 0x0800) ? 's' : 'x') : (($perms & 0x0800) ? 'S' : '-'));
    $info .= (($perms & 0x0020) ? 'r' : '-');
    $info .= (($perms & 0x0010) ? 'w' : '-');
    $info .= (($perms & 0x0008) ? (($perms & 0x0400) ? 's' : 'x') : (($perms & 0x0400) ? 'S' : '-'));
    $info .= (($perms & 0x0004) ? 'r' : '-');
    $info .= (($perms & 0x0002) ? 'w' : '-');
    $info .= (($perms & 0x0001) ? (($perms & 0x0200) ? 't' : 'x') : (($perms & 0x0200) ? 'T' : '-'));
    return $info;
}

/* ============================================================
   LOAD CONFIG & DETERMINE ROOT
============================================================ */
/*
 sys_get_temp_dir() is not working on android
 so i hard coded tmp folder
 still looking for reliable method
*/
$TEMP_DIR = '/data/data/com.lodwickmasete.php/files/tmp';


$config_path = '/data/data/com.lodwickmasete.php/files/app_config.json';
if (!file_exists($config_path)) {
    json_err('app_config not found at: ' . $config_path, 500);
}

$config_raw = file_get_contents($config_path);
if ($config_raw === false) {
    json_err('Cannot read app_config', 500);
}

$config = json_decode($config_raw, true);
if (!is_array($config)) {
    json_err('app_config is not valid JSON', 500);
}

$document_root = isset($config['document_root']) ? rtrim($config['document_root'], '/') : '';
if (empty($document_root) || !is_dir($document_root)) {
    json_err('document_root is not a valid directory: ' . $document_root, 500);
}

$ROOT = $document_root;

/* ============================================================
   PATH SANITIZATION
   All virtual paths from the client are relative to $ROOT.
   A virtual "/" means $ROOT itself.
============================================================ */

/**
 * Convert a virtual path (as sent by the client, e.g. "/Projects/webapp")
 * into a real filesystem path, ensuring it stays within $ROOT.
 *
 * Returns the real path, or FALSE if the path escapes $ROOT.
 */
function real_path($virtual, $root) {
    // Normalise slashes
    $virtual = str_replace('\\', '/', $virtual);

    // Strip leading slash then rejoin with root
    $stripped = ltrim($virtual, '/');
    if ($stripped === '') {
        $candidate = $root;
    } else {
        $candidate = $root . '/' . $stripped;
    }

    // Resolve real path (resolves .., symlinks, etc.)
    $resolved = realpath($candidate);

    // realpath returns false if the path does not exist yet (e.g. new folder).
    // In that case, resolve the parent and reattach the basename.
    if ($resolved === false) {
        $parent_real = realpath(dirname($candidate));
        if ($parent_real === false) return false;
        $resolved = $parent_real . '/' . basename($candidate);
    }

    // Ensure the resolved path is inside $ROOT
    $root_real = realpath($root);
    if ($root_real === false) return false;
    if (strpos($resolved, $root_real) !== 0) return false;

    return $resolved;
}

/**
 * Convert a real filesystem path back to a virtual path (relative to ROOT).
 */
function virtual_path($real, $root) {
    $root_real = realpath($root);
    $rel = substr($real, strlen($root_real));
    if ($rel === '' || $rel === false) return '/';
    return '/' . ltrim(str_replace('\\', '/', $rel), '/');
}

/* ============================================================
   READ ACTION & BODY
============================================================ */

$action = isset($_GET['action']) ? trim($_GET['action']) : '';

/* For DELETE and multi-item operations sent as JSON body */
$body = [];
$raw_body = file_get_contents('php://input');
if ($raw_body) {
    $decoded = json_decode($raw_body, true);
    if (is_array($decoded)) $body = $decoded;
}

/* Also read from POST and GET for convenience */
function param($key, $fallback = '') {
    global $body;
    if (isset($body[$key]))     return $body[$key];
    if (isset($_POST[$key]))    return $_POST[$key];
    if (isset($_GET[$key]))     return $_GET[$key];
    return $fallback;
}

/* ============================================================
   ACTIONS
============================================================ */

switch ($action) {

    /* ----------------------------------------------------------
       LIST
    ---------------------------------------------------------- */
    case 'list': {
        $vpath = param('path', '/');
        $real  = real_path($vpath, $ROOT);
        if ($real === false || !is_dir($real)) {
            json_err('Directory not found: ' . $vpath);
        }

        $entries = scandir($real);
        $items   = [];
        foreach ($entries as $entry) {
            if ($entry === '.' || $entry === '..') continue;
            $full  = $real . '/' . $entry;
            $isdir = is_dir($full);
            $items[] = [
                'name'        => $entry,
                'type'        => $isdir ? 'dir' : 'file',
                'size'        => $isdir ? null : filesize($full),
                'modified'    => date('Y-m-d H:i:s', filemtime($full)),
                'permissions' => perms_string($full),
            ];
        }
        json_ok(['path' => virtual_path($real, $ROOT), 'items' => $items]);
    }

    /* ----------------------------------------------------------
       MKDIR
    ---------------------------------------------------------- */
    case 'mkdir': {
        $vpath = param('path', '/');
        $name  = basename(param('name', ''));
        if (empty($name)) json_err('Folder name is required');

        $real = real_path($vpath . '/' . $name, $ROOT);
        if ($real === false) json_err('Invalid path');
        if (file_exists($real)) json_err('Already exists: ' . $name);

        if (!mkdir($real, 0755, true)) json_err('Failed to create folder');
        json_ok(['name' => $name]);
    }

    /* ----------------------------------------------------------
       MKFILE
    ---------------------------------------------------------- */
    case 'mkfile': {
        $vpath = param('path', '/');
        $name  = basename(param('name', ''));
        if (empty($name)) json_err('File name is required');

        $real = real_path($vpath . '/' . $name, $ROOT);
        if ($real === false) json_err('Invalid path');
        if (file_exists($real)) json_err('Already exists: ' . $name);

        if (file_put_contents($real, '') === false) json_err('Failed to create file');
        json_ok(['name' => $name]);
    }

    /* ----------------------------------------------------------
       RENAME
    ---------------------------------------------------------- */
    case 'rename': {
        $vpath   = param('path', '/');
        $oldName = basename(param('oldName', ''));
        $newName = basename(param('newName', ''));
        if (empty($oldName) || empty($newName)) json_err('Old and new names are required');

        $src  = real_path($vpath . '/' . $oldName, $ROOT);
        $dest = real_path($vpath . '/' . $newName, $ROOT);
        if ($src === false || $dest === false) json_err('Invalid path');
        if (!file_exists($src)) json_err('Source not found: ' . $oldName);
        if (file_exists($dest)) json_err('Already exists: ' . $newName);

        if (!rename($src, $dest)) json_err('Rename failed');
        json_ok(['oldName' => $oldName, 'newName' => $newName]);
    }

    /* ----------------------------------------------------------
       DELETE
    ---------------------------------------------------------- */
    case 'delete': {
        $vpath = param('path', '/');
        $names = param('names', []);
        if (!is_array($names) || empty($names)) json_err('No items specified');

        $errors = [];
        foreach ($names as $name) {
            $name = basename($name);
            $real = real_path($vpath . '/' . $name, $ROOT);
            if ($real === false || !file_exists($real)) {
                $errors[] = $name . ': not found';
                continue;
            }
            if (is_dir($real)) {
                if (!rmdir_recursive($real)) $errors[] = $name . ': delete failed';
            } else {
                if (!unlink($real)) $errors[] = $name . ': delete failed';
            }
        }

        if (!empty($errors)) json_err(implode('; ', $errors));
        json_ok(['deleted' => count($names)]);
    }

    /* ----------------------------------------------------------
       COPY
    ---------------------------------------------------------- */
    case 'copy': {
        $srcVpath = param('srcPath', '/');
        $name     = basename(param('name', ''));
        $destVpath = param('destPath', '/');
        if (empty($name)) json_err('Name required');

        $src  = real_path($srcVpath . '/' . $name, $ROOT);
        $dest = real_path($destVpath . '/' . $name, $ROOT);
        if ($src === false || $dest === false) json_err('Invalid path');
        if (!file_exists($src)) json_err('Source not found');

        // to Avoid overwriting: append _copy if needed
        $destFinal = $dest;
        if (file_exists($destFinal)) {
            $base  = pathinfo($name, PATHINFO_FILENAME);
            $ext   = pathinfo($name, PATHINFO_EXTENSION);
            $count = 1;
            do {
                $newName   = $base . '_copy' . ($count > 1 ? $count : '') . ($ext ? '.' . $ext : '');
                $destFinal = real_path($destVpath . '/' . $newName, $ROOT);
                $count++;
            } while ($destFinal !== false && file_exists($destFinal));
        }
        if ($destFinal === false) json_err('Invalid destination');

        if (is_dir($src)) {
            // Recursive copy
            $it = new RecursiveIteratorIterator(
                new RecursiveDirectoryIterator($src, FilesystemIterator::SKIP_DOTS),
                RecursiveIteratorIterator::SELF_FIRST
            );
            mkdir($destFinal, 0755, true);
            foreach ($it as $item) {
                $target = $destFinal . DIRECTORY_SEPARATOR . $it->getSubPathName();
                if ($item->isDir()) {
                    mkdir($target, 0755, true);
                } else {
                    copy($item->getRealPath(), $target);
                }
            }
        } else {
            $destDir = dirname($destFinal);
            if (!is_dir($destDir)) mkdir($destDir, 0755, true);
            if (!copy($src, $destFinal)) json_err('Copy failed');
        }
        json_ok(['name' => basename($destFinal)]);
    }

    /* ----------------------------------------------------------
       MOVE
    ---------------------------------------------------------- */
    case 'move': {
        $srcVpath  = param('srcPath', '/');
        $name      = basename(param('name', ''));
        $destVpath = param('destPath', '/');
        if (empty($name)) json_err('Name required');

        $src  = real_path($srcVpath . '/' . $name, $ROOT);
        $dest = real_path($destVpath . '/' . $name, $ROOT);
        if ($src === false || $dest === false) json_err('Invalid path');
        if (!file_exists($src)) json_err('Source not found');
        if ($src === $dest) json_err('Source and destination are the same');

        $destDir = is_dir($dest) ? $dest : dirname($dest);
        if (!is_dir($destDir)) json_err('Destination folder not found');

        // If destPath is a folder, move inside it
        $finalDest = is_dir($dest) ? $dest . '/' . $name : $dest;
        $finalReal = realpath(dirname($finalDest));
        if (!$finalReal) json_err('Destination parent not found');
        $finalDest = $finalReal . '/' . $name;

        if (!rename($src, $finalDest)) json_err('Move failed');
        json_ok(['name' => $name]);
    }

/* ----------------------------------------------------------
   BULK COPY
---------------------------------------------------------- */
case 'bulk_copy': {
    $body      = json_decode(file_get_contents('php://input'), true);
    $srcVpath  = $body['srcPath'] ?? '/';
    $names     = $body['names'] ?? [];
    $destVpath = $body['destPath'] ?? '/';
    if (empty($names) || !is_array($names)) json_err('Names array required');

    $results = [];
    foreach ($names as $rawName) {
        $name = basename($rawName);
        if (empty($name)) continue;

        $src  = real_path($srcVpath . '/' . $name, $ROOT);
        $dest = real_path($destVpath . '/' . $name, $ROOT);
        if ($src === false || $dest === false) { $results[] = ['name'=>$name,'ok'=>false,'error'=>'Invalid path']; continue; }
        if (!file_exists($src))               { $results[] = ['name'=>$name,'ok'=>false,'error'=>'Source not found']; continue; }

        $destFinal = $dest;
        if (file_exists($destFinal)) {
            $base  = pathinfo($name, PATHINFO_FILENAME);
            $ext   = pathinfo($name, PATHINFO_EXTENSION);
            $count = 1;
            do {
                $newName   = $base . '_copy' . ($count > 1 ? $count : '') . ($ext ? '.' . $ext : '');
                $destFinal = real_path($destVpath . '/' . $newName, $ROOT);
                $count++;
            } while ($destFinal !== false && file_exists($destFinal));
        }
        if ($destFinal === false) { $results[] = ['name'=>$name,'ok'=>false,'error'=>'Invalid destination']; continue; }

        try {
            if (is_dir($src)) {
                $it = new RecursiveIteratorIterator(
                    new RecursiveDirectoryIterator($src, FilesystemIterator::SKIP_DOTS),
                    RecursiveIteratorIterator::SELF_FIRST
                );
                mkdir($destFinal, 0755, true);
                foreach ($it as $item) {
                    $target = $destFinal . DIRECTORY_SEPARATOR . $it->getSubPathName();
                    if ($item->isDir()) mkdir($target, 0755, true);
                    else copy($item->getRealPath(), $target);
                }
            } else {
                $destDir = dirname($destFinal);
                if (!is_dir($destDir)) mkdir($destDir, 0755, true);
                copy($src, $destFinal);
            }
            $results[] = ['name' => basename($destFinal), 'ok' => true];
        } catch (Exception $e) {
            $results[] = ['name' => $name, 'ok' => false, 'error' => $e->getMessage()];
        }
    }
    $failed = array_filter($results, fn($r) => !$r['ok']);
    json_ok(['results' => $results, 'failed' => count($failed)]);
}

/* ----------------------------------------------------------
   BULK MOVE
---------------------------------------------------------- */
case 'bulk_move': {
    $body      = json_decode(file_get_contents('php://input'), true);
    $srcVpath  = $body['srcPath'] ?? '/';
    $names     = $body['names'] ?? [];
    $destVpath = $body['destPath'] ?? '/';
    if (empty($names) || !is_array($names)) json_err('Names array required');

    $results = [];
    foreach ($names as $rawName) {
        $name = basename($rawName);
        if (empty($name)) continue;

        $src  = real_path($srcVpath . '/' . $name, $ROOT);
        $dest = real_path($destVpath . '/' . $name, $ROOT);
        if ($src === false || $dest === false) { $results[] = ['name'=>$name,'ok'=>false,'error'=>'Invalid path']; continue; }
        if (!file_exists($src))               { $results[] = ['name'=>$name,'ok'=>false,'error'=>'Source not found']; continue; }
        if ($src === $dest)                   { $results[] = ['name'=>$name,'ok'=>false,'error'=>'Same path']; continue; }

        $finalDest = $destVpath === $srcVpath ? $dest : (real_path($destVpath, $ROOT) . '/' . $name);
        try {
            if (!rename($src, $finalDest)) throw new Exception('rename() failed');
            $results[] = ['name' => $name, 'ok' => true];
        } catch (Exception $e) {
            $results[] = ['name' => $name, 'ok' => false, 'error' => $e->getMessage()];
        }
    }
    $failed = array_filter($results, fn($r) => !$r['ok']);
    json_ok(['results' => $results, 'failed' => count($failed)]);
}

    /* ----------------------------------------------------------
       ZIP (create archive from selected items)
    ---------------------------------------------------------- */
    case 'zip': {
        if (!class_exists('ZipArchive')) json_err('ZipArchive extension not available', 500);

        $vpath   = param('path', '/');
        $names   = param('names', []);
        $zipName = basename(param('zipName', 'archive.zip'));
        if (!is_array($names) || empty($names)) json_err('No items specified');
        if (empty($zipName)) $zipName = 'archive.zip';
        if (strtolower(substr($zipName, -4)) !== '.zip') $zipName .= '.zip';

        $dirReal = real_path($vpath, $ROOT);
        if ($dirReal === false || !is_dir($dirReal)) json_err('Directory not found');

        $zipReal = real_path($vpath . '/' . $zipName, $ROOT);
        if ($zipReal === false) json_err('Invalid zip path');

        $zip = new ZipArchive();
        $res = $zip->open($zipReal, ZipArchive::CREATE | ZipArchive::OVERWRITE);
        if ($res !== true) json_err('Cannot create ZIP file (code: ' . $res . ')');

        foreach ($names as $name) {
            $name = basename($name);
            $src  = real_path($vpath . '/' . $name, $ROOT);
            if ($src === false || !file_exists($src)) continue;

            if (is_dir($src)) {
                $it = new RecursiveIteratorIterator(
                    new RecursiveDirectoryIterator($src, FilesystemIterator::SKIP_DOTS),
                    RecursiveIteratorIterator::SELF_FIRST
                );
                $zip->addEmptyDir($name);
                foreach ($it as $item) {
                    $localPath = $name . '/' . $it->getSubPathName();
                    if ($item->isDir()) {
                        $zip->addEmptyDir($localPath);
                    } else {
                        $zip->addFile($item->getRealPath(), $localPath);
                    }
                }
            } else {
                $zip->addFile($src, $name);
            }
        }
        $zip->close();
        json_ok(['zipName' => $zipName, 'zipPath' => virtual_path($zipReal, $ROOT)]);
    }

    /* ----------------------------------------------------------
       EXTRACT (unzip)
    ---------------------------------------------------------- */
    case 'extract': {
        if (!class_exists('ZipArchive')) json_err('ZipArchive extension not available', 500);

        $vpath     = param('path', '/');
        $name      = basename(param('name', ''));
        $destVpath = param('destPath', $vpath);
        if (empty($name)) json_err('ZIP name required');

        $zipReal  = real_path($vpath . '/' . $name, $ROOT);
        $destReal = real_path($destVpath, $ROOT);
        if ($zipReal === false || !file_exists($zipReal)) json_err('ZIP not found: ' . $name);
        if ($destReal === false) json_err('Destination not found');
        if (!is_dir($destReal)) json_err('Destination is not a directory');

        $zip = new ZipArchive();
        if ($zip->open($zipReal) !== true) json_err('Cannot open ZIP file');
        $zip->extractTo($destReal);
        $zip->close();

        json_ok(['extracted' => $name, 'destPath' => virtual_path($destReal, $ROOT)]);
    }

    /* ----------------------------------------------------------
       UPLOAD
    ---------------------------------------------------------- */
    case 'upload': {
        $vpath = param('path', '/');
        $real  = real_path($vpath, $ROOT);
        if ($real === false || !is_dir($real)) json_err('Directory not found');

        if (empty($_FILES['files'])) json_err('No files uploaded');

        $files   = $_FILES['files'];
        $count   = is_array($files['name']) ? count($files['name']) : 1;
        $errors  = [];
        $uploaded = 0;

        // Normalise single vs multiple files
        $names    = is_array($files['name'])     ? $files['name']     : [$files['name']];
        $tmps     = is_array($files['tmp_name']) ? $files['tmp_name'] : [$files['tmp_name']];
        $errs     = is_array($files['error'])    ? $files['error']    : [$files['error']];

        for ($i = 0; $i < count($names); $i++) {
            if ($errs[$i] !== UPLOAD_ERR_OK) {
                $errors[] = $names[$i] . ': upload error ' . $errs[$i];
                continue;
            }
            $safeName = basename($names[$i]);
            $dest     = $real . '/' . $safeName;

            // Avoid overwriting
            if (file_exists($dest)) {
                $base  = pathinfo($safeName, PATHINFO_FILENAME);
                $ext   = pathinfo($safeName, PATHINFO_EXTENSION);
                $j     = 1;
                do {
                    $newSafe = $base . '_' . $j . ($ext ? '.' . $ext : '');
                    $dest    = $real . '/' . $newSafe;
                    $j++;
                } while (file_exists($dest));
            }

            if (!move_uploaded_file($tmps[$i], $dest)) {
                $errors[] = $safeName . ': save failed';
            } else {
                $uploaded++;
            }
        }

        if (!empty($errors)) json_err(implode('; ', $errors));
        json_ok(['uploaded' => $uploaded]);
    }

    /* ----------------------------------------------------------
       EXPORT ZIP (zip entire current directory for download)
    ---------------------------------------------------------- */
    case 'exportzip': {
try{
        if (!class_exists('ZipArchive')) json_err('ZipArchive extension not available', 500);

        $vpath   = param('path', '/');
        $dirReal = real_path($vpath, $ROOT);
        if ($dirReal === false || !is_dir($dirReal)) json_err('Directory not found');

        $zipName = 'export_' . date('Ymd_His') . '.zip';
        // Store in system temp to avoid polluting the user's dir

        
       // $zipReal = sys_get_temp_dir() . '/' . $zipName;
        
        $zipReal = $TEMP_DIR . '/' . $zipName;
       
                       
        $zip = new ZipArchive();
        $res = $zip->open($zipReal, ZipArchive::CREATE | ZipArchive::OVERWRITE);
        if ($res !== true) json_err('Cannot create ZIP file');

        $it = new RecursiveIteratorIterator(
            new RecursiveDirectoryIterator($dirReal, FilesystemIterator::SKIP_DOTS),
            RecursiveIteratorIterator::SELF_FIRST
        );
        foreach ($it as $item) {
            $localPath = $it->getSubPathName();
            if ($item->isDir()) {
                $zip->addEmptyDir($localPath);
            } else {
                $zip->addFile($item->getRealPath(), $localPath);
            }
        }
        $zip->close();

        // Store temp path in session or return it for immediate download
        // We'll use a temp token approach: store in a transient file
        $token    = bin2hex(random_bytes(16));
       // $manifest = sys_get_temp_dir() . '/ampdroid_dl_' . $token . '.json';
        
        $manifest = $TEMP_DIR . '/ampdroid_dl_' . $token . '.json';
       
        file_put_contents($manifest, json_encode(['file' => $zipReal, 'name' => $zipName, 'expires' => time() + 120]));

        json_ok(['zipName' => $zipName, 'token' => $token]);

    } catch (Exception $e){
       // json_err([$e , 400]);
    }
    }

    /* ----------------------------------------------------------
       DOWNLOAD (serve a file or temp zip)
    ---------------------------------------------------------- */
    case 'download': {
        $token = isset($_GET['token']) ? preg_replace('/[^a-f0-9]/', '', $_GET['token']) : '';
        $vpath = isset($_GET['path'])  ? param('path', '') : '';

        if (!empty($token)) {
            // Serve temp export zip
          //  $manifest = sys_get_temp_dir() . '/ampdroid_dl_' . $token . '.json';
            $manifest = $TEMP_DIR . '/ampdroid_dl_' . $token . '.json';
            if (!file_exists($manifest)) json_err('Download expired or not found', 404);
            $info = json_decode(file_get_contents($manifest), true);
            if (!$info || !file_exists($info['file'])) json_err('File not found', 404);
            if (time() > $info['expires']) {
                @unlink($manifest);
                @unlink($info['file']);
                json_err('Download link expired', 410);
            }
            header('Content-Type: application/zip');
            header('Content-Disposition: attachment; filename="' . addslashes($info['name']) . '"');
            header('Content-Length: ' . filesize($info['file']));
            readfile($info['file']);
            @unlink($manifest);
            @unlink($info['file']);
            exit;

        } elseif (!empty($vpath)) {
            // Serve a file from the filesystem
            $real = real_path($vpath, $ROOT);
            if ($real === false || !file_exists($real) || is_dir($real)) json_err('File not found', 404);
            $mime = mime_content_type($real) ?: 'application/octet-stream';
            header('Content-Type: ' . $mime);
            header('Content-Disposition: attachment; filename="' . addslashes(basename($real)) . '"');
            header('Content-Length: ' . filesize($real));
            readfile($real);
            exit;

        } else {
            json_err('No file specified', 400);
        }
    }

    /* ----------------------------------------------------------
       DISK USAGE
    ---------------------------------------------------------- */
    case 'diskusage': {
        $vpath = param('path', '/');
        $real  = real_path($vpath, $ROOT);
        if ($real === false || !is_dir($real)) json_err('Directory not found');

        $bytes = dir_size($real);
        json_ok(['bytes' => $bytes, 'human' => human_size($bytes), 'path' => $vpath]);
    }


    /* ----------------------------------------------------------
       UNKNOWN
    ---------------------------------------------------------- */
    default:
        json_err('Unknown action: ' . $action, 400);
}