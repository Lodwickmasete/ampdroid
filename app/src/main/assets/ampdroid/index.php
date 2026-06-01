<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0, user-scalable=yes">
<title>Ampdroid Server Dashboard</title>
<link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css">
<style>
/* ===== BASE ===== */
*, *::before, *::after {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
  font-size: 13px;
  background: #f2f4f8;
  color: #111;
  height: 100vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ===== HEADER ===== */
.app-header {
  background: #1e3a5f;
  color: #fff;
  padding: 8px 12px;
  display: flex;
  align-items: center;
  border-bottom: 2px solid #0a2a44;
  flex-shrink: 0;
}

.brand {
  display: flex;
  align-items: center;
  gap: 10px;
}

.brand i {
  color: #ffcc66;
  font-size: 16px;
}

.brand-title {
  font-size: 15px;
  font-weight: bold;
  letter-spacing: 0.5px;
}

.header-right {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: 10px;
}

.status {
  font-size: 11px;
  color: #a8c8ec;
  display: flex;
  align-items: center;
  gap: 6px;
}

.status i {
  color: #2ecc71;
}

.logout {
  background: #0a2a44;
  border: 1px solid #2a5a8f;
  color: #fff;
  font-size: 11px;
  padding: 4px 10px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 3px;
  transition: 0.1s;
}

.logout:hover {
  background: #143a60;
  border-color: #4a88cc;
}

/* ===== TABS ===== */
.tabs {
  display: flex;
  background: #e6e9ef;
  border-bottom: 1px solid #bbb;
  flex-wrap: nowrap;
  overflow-x: auto;
  scrollbar-width: thin;
  flex-shrink: 0;
}

.tab {
  flex: 1;
  padding: 8px 6px;
  text-align: center;
  cursor: pointer;
  font-size: 12px;
  border-right: 1px solid #c7cbd3;
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 6px;
  background: #e6e9ef;
  transition: 0.1s;
  text-decoration: none;
  color: #1e2a36;
  white-space: nowrap;
}
.tab:last-child {
  border-right: none;
}
.tab.active {
  background: #ffffff;
  font-weight: bold;
  border-bottom: 2px solid #1e3a5f;
  margin-bottom: -1px;
}
.tab:hover:not(.active) {
  background: #d0d6e2;
}

/* ===== CONTENT (scrollable) ===== */
.content {
  flex: 1;
  overflow-y: auto;
  padding: 14px 16px;
  background: #f2f4f8;
}

/* ===== CARDS ===== */
.grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(310px, 1fr));
  gap: 14px;
}

.card {
  background: #fff;
  border: 1px solid #ccc;
  border-radius: 4px;
  padding: 0;
  overflow: hidden;
  box-shadow: 0 1px 2px rgba(0,0,0,0.03);
}
.card h3 {
  font-size: 12px;
  font-weight: 600;
  background: #eef2f7;
  padding: 8px 12px;
  margin: 0;
  color: #1e3a5f;
  display: flex;
  align-items: center;
  gap: 8px;
  border-bottom: 1px solid #ddd;
}
.card-body {
  padding: 12px;
  font-size: 12px;
}
.card-body .value-large {
  font-size: 18px;
  font-weight: bold;
  color: #1e3a5f;
  margin-bottom: 5px;
}
.card-body .info-row {
  display: flex;
  padding: 5px 0;
  border-bottom: 1px solid #eee;
  font-size: 12px;
}
.card-body .info-label {
  font-weight: 600;
  width: 130px;
  flex-shrink: 0;
  color: #3a5a7a;
}
.card-body .info-value {
  color: #111;
  word-break: break-word;
}
.link-group {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-top: 6px;
}
.button-small {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  background: #f0f2f5;
  border: 1px solid #bbb;
  padding: 5px 12px;
  font-size: 11px;
  cursor: pointer;
  text-decoration: none;
  color: #1e2a36;
  border-radius: 3px;
}
.button-small:hover {
  background: #e2e6ed;
  border-color: #8a9cbb;
}
.button {
  display: block;
  background: #eef2f7;
  text-align: center;
  padding: 8px;
  border: 1px solid #aaa;
  border-radius: 3px;
  text-decoration: none;
  color: #1e3a5f;
  font-size: 12px;
  margin-top: 6px;
}
.button i, .button-small i {
  font-size: 11px;
}
.badge-ext {
  display: inline-block;
  background: #eef2f7;
  border: 1px solid #ccc;
  padding: 3px 8px;
  font-size: 11px;
  border-radius: 12px;
  margin: 3px;
}
.grid-2col {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
small.error-text {
  color: #c0392b;
  display: block;
  margin-top: 4px;
}

/* ===== BOTTOM NAV ===== */
.bottom-nav {
  background: #e0e4ec;
  border-top: 1px solid #aaa;
  display: flex;
  justify-content: space-around;
  padding: 6px 8px;
  flex-shrink: 0;
}
.bottom-nav button {
  background: #eceef5;
  border: 1px solid #8a8ea0;
  font-size: 11px;
  padding: 6px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  border-radius: 3px;
  font-family: inherit;
  transition: 0.1s;
}
.bottom-nav button.active {
  background: #cdd6e8;
  font-weight: bold;
  border-color: #667799;
}
.bottom-nav button i {
  font-size: 12px;
}
.bottom-nav button:hover:not(.active) {
  background: #d8dfec;
}

/* responsive adjustments */
@media (max-width: 640px) {
  .grid { grid-template-columns: 1fr; }
  .tab { font-size: 10px; padding: 8px 3px; gap: 3px; }
  .card-body .info-label { width: 105px; }
  .bottom-nav button span { display: none; }
  .bottom-nav button i { margin: 0; font-size: 14px; }
  .bottom-nav button { padding: 6px 14px; }
}
@media (max-width: 480px) {
  .tabs { flex-wrap: wrap; }
  .tab { flex: auto; border-bottom: 1px solid #ccc; }
}
</style>
</head>
<body>

<?php
$host = "127.0.0.1";
$user = "root";
$pass = "";
$db   = "test";

// Determine which view to show
$view = isset($_GET['view']) ? $_GET['view'] : 'dashboard';

// MySQLi test
$mysqli_status = "Failed";
$mysqli_error = "";

try {
    $conn = @new mysqli($host, $user, $pass, $db);
    if (!$conn->connect_error) {
        $mysqli_status = "Connected";
    } else {
        $mysqli_error = $conn->connect_error;
    }
} catch (Exception $e) {
    $mysqli_error = $e->getMessage();
}

// PDO test
$pdo_status = "Failed";
$pdo_error = "";

try {
    $pdo = new PDO("mysql:host=$host;dbname=$db", $user, $pass);
    $pdo_status = "Connected";
} catch (Exception $e) {
    $pdo_error = $e->getMessage();
}

function ext($name) {
    return extension_loaded($name) ? "Yes" : "No";
}

// Server info vars
$server_software = $_SERVER['SERVER_SOFTWARE'] ?? 'Unknown';
$server_protocol = $_SERVER['SERVER_PROTOCOL'] ?? 'Unknown';
$server_name = $_SERVER['SERVER_NAME'] ?? 'Unknown';
$server_addr = $_SERVER['SERVER_ADDR'] ?? 'Unknown';
$remote_addr = $_SERVER['REMOTE_ADDR'] ?? 'Unknown';
$document_root = $_SERVER['DOCUMENT_ROOT'] ?? 'Unknown';
$max_execution_time = ini_get('max_execution_time');
$memory_limit = ini_get('memory_limit');
$upload_max_filesize = ini_get('upload_max_filesize');
$post_max_size = ini_get('post_max_size');
$display_errors = ini_get('display_errors');
$date_timezone = date_default_timezone_get();
$current_time = date('Y-m-d H:i:s');
$server_timezone = date_default_timezone_get();
$php_sapi = php_sapi_name();
$os = PHP_OS;
$loaded_extensions = get_loaded_extensions();
$disabled_functions = ini_get('disable_functions') ?: 'None';

$mysql_version = '';
$mysql_charset = '';
if ($mysqli_status === "Connected") {
    $mysql_version = $conn->server_info;
    $mysql_charset = $conn->character_set_name();
    $conn->close();
}


$disk_free = function_exists('disk_free_space') ? @disk_free_space('/') : false;
$disk_total = function_exists('disk_total_space') ? @disk_total_space('/') : false;
$disk_free_str = ($disk_free !== false) ? round($disk_free / 1024 / 1024 / 1024, 2) . ' GB' : 'N/A';
$disk_total_str = ($disk_total !== false) ? round($disk_total / 1024 / 1024 / 1024, 2) . ' GB' : 'N/A';
$load_avg = function_exists('sys_getloadavg') ? implode(', ', sys_getloadavg()) : 'Not available';

if ($view === 'phpinfo') {
    phpinfo();
    exit;
}
?>

<!-- HEADER -->
<div class="app-header">
  <div class="brand">
    <i class="fa-solid fa-server"></i>
    <div class="brand-title">
      Ampdroid Server
    </div>
  </div>
  <div class="header-right">
    <div class="status">
      <i class="fa-solid fa-circle"></i>
      Online
    </div>
    <button class="logout" id="logoutMockBtn">
      <i class="fa-solid fa-right-from-bracket"></i>
      Logout
    </button>
  </div>
</div>

<!-- TABS -->
<div class="tabs">
  <a href="?view=dashboard" class="tab <?php echo $view == 'dashboard' ? 'active' : ''; ?>"><i class="fa-solid fa-house"></i> Dashboard</a>
  <a href="?view=server" class="tab <?php echo $view == 'server' ? 'active' : ''; ?>"><i class="fa-solid fa-microchip"></i> Server</a>
  <a href="?view=database" class="tab <?php echo $view == 'database' ? 'active' : ''; ?>"><i class="fa-solid fa-database"></i> Database</a>
  <a href="?view=php" class="tab <?php echo $view == 'php' ? 'active' : ''; ?>"><i class="fa-solid fa-code"></i> Config</a>
  <a href="?view=extensions" class="tab <?php echo $view == 'extensions' ? 'active' : ''; ?>"><i class="fa-solid fa-puzzle-piece"></i> Extensions</a>
  <a href="?view=phpinfo" class="tab"><i class="fa-solid fa-info-circle"></i> phpinfo()</a>
</div>

<!-- CONTENT -->
<div class="content">
    <?php if ($view == 'dashboard'): ?>
    <div class="grid">
        <div class="card"><h3><i class="fa-solid fa-code"></i> PHP Version</h3><div class="card-body"><div class="value-large"><?php echo phpversion(); ?></div></div></div>
        <div class="card"><h3><i class="fa-solid fa-database"></i> MySQLi Connection</h3><div class="card-body"><div class="value-large"><?php echo $mysqli_status; ?></div><?php if ($mysqli_error) echo '<small class="error-text">'.$mysqli_error.'</small>'; ?></div></div>
        <div class="card"><h3><i class="fa-solid fa-database"></i> PDO Connection</h3><div class="card-body"><div class="value-large"><?php echo $pdo_status; ?></div><?php if ($pdo_error) echo '<small class="error-text">'.$pdo_error.'</small>'; ?></div></div>
        <div class="card"><h3><i class="fa-solid fa-link"></i> Quick Links</h3><div class="card-body"><div class="link-group"><a class="button-small" href="?view=phpinfo"><i class="fa-solid fa-info-circle"></i> phpinfo()</a><a class="button-small" href="?view=server"><i class="fa-solid fa-server"></i> Server Info</a><a class="button-small" href="?view=php"><i class="fa-solid fa-code"></i> PHP Config</a><a class="button-small" href="?view=extensions"><i class="fa-solid fa-puzzle-piece"></i> Extensions</a></div></div></div>
        <div class="card"><h3><i class="fa-solid fa-desktop"></i> System Quick Info</h3><div class="card-body"><div class="grid-2col"><div>OS: <?php echo $os; ?></div><div>Server: <?php echo $server_software; ?></div><div>Memory Limit: <?php echo $memory_limit; ?></div><div>Max Upload: <?php echo $upload_max_filesize; ?></div><div>Server Time: <?php echo $current_time; ?></div><div>Timezone: <?php echo $date_timezone; ?></div></div></div></div>
        <div class="card"><h3><i class="fa-solid fa-puzzle-piece"></i> Extensions (common)</h3><div class="card-body"><div class="grid-2col"><div>zip: <?php echo ext("zip"); ?></div><div>gd: <?php echo ext("gd"); ?></div><div>curl: <?php echo ext("curl"); ?></div><div>json: <?php echo ext("json"); ?></div><div>mbstring: <?php echo ext("mbstring"); ?></div><div>pdo: <?php echo ext("pdo"); ?></div><div>mysqli: <?php echo ext("mysqli"); ?></div><div>openssl: <?php echo ext("openssl"); ?></div></div></div></div>
    </div>
    <?php endif; ?>

    <?php if ($view == 'server'): ?>
    <div class="grid">
        <div class="card"><h3><i class="fa-solid fa-server"></i> Server Information</h3><div class="card-body"><div class="info-row"><span class="info-label">Server Software:</span><span class="info-value"><?php echo $server_software; ?></span></div><div class="info-row"><span class="info-label">Server Protocol:</span><span class="info-value"><?php echo $server_protocol; ?></span></div><div class="info-row"><span class="info-label">Server Name:</span><span class="info-value"><?php echo $server_name; ?></span></div><div class="info-row"><span class="info-label">Server Address:</span><span class="info-value"><?php echo $server_addr; ?></span></div><div class="info-row"><span class="info-label">Remote Address:</span><span class="info-value"><?php echo $remote_addr; ?></span></div><div class="info-row"><span class="info-label">Document Root:</span><span class="info-value"><?php echo $document_root; ?></span></div><div class="info-row"><span class="info-label">Operating System:</span><span class="info-value"><?php echo $os; ?></span></div><div class="info-row"><span class="info-label">PHP SAPI:</span><span class="info-value"><?php echo $php_sapi; ?></span></div><div class="info-row"><span class="info-label">Server Time:</span><span class="info-value"><?php echo $current_time; ?></span></div><div class="info-row"><span class="info-label">Server Timezone:</span><span class="info-value"><?php echo $server_timezone; ?></span></div></div></div>
        <div class="card"><h3><i class="fa-solid fa-chart-line"></i> Load & Resource Info</h3><div class="card-body"><div class="info-row"><span class="info-label">CPU Load Average:</span><span class="info-value"><?php echo $load_avg; ?></span></div><div class="info-row"><span class="info-label">Disk Free Space (root):</span><span class="info-value"><?php echo $disk_free_str; ?></span></div><div class="info-row"><span class="info-label">Disk Total Space (root):</span><span class="info-value"><?php echo $disk_total_str; ?></span></div></div></div>
        <div class="card"><h3><i class="fa-solid fa-folder-open"></i> Ampdroid WWW/htdocs</h3><div class="card-body"><a class="button" href="/ampdroid/file-manager/"><i class="fa-solid fa-folder"></i> File Manager</a></div></div>
    </div>
    <?php endif; ?>

    <?php if ($view == 'database'): ?>
    <div class="grid">
        <div class="card"><h3><i class="fa-solid fa-database"></i> MySQL / MariaDB Information</h3><div class="card-body"><div class="info-row"><span class="info-label">MySQLi Status:</span><span class="info-value"><?php echo $mysqli_status; ?></span></div><?php if ($mysql_version): ?><div class="info-row"><span class="info-label">MySQL Version:</span><span class="info-value"><?php echo $mysql_version; ?></span></div><div class="info-row"><span class="info-label">Default Charset:</span><span class="info-value"><?php echo $mysql_charset; ?></span></div><?php endif; ?><div class="info-row"><span class="info-label">PDO Status:</span><span class="info-value"><?php echo $pdo_status; ?></span></div><div class="info-row"><span class="info-label">Host:</span><span class="info-value"><?php echo $host; ?></span></div><div class="info-row"><span class="info-label">Database:</span><span class="info-value"><?php echo $db; ?></span></div><div class="info-row"><span class="info-label">User:</span><span class="info-value"><?php echo $user; ?></span></div></div></div>
        <div class="card"><h3><i class="fa-solid fa-link"></i> Database Quick Links</h3><div class="card-body"><a class="button" href="/phpmyadmin/"><i class="fa-solid fa-database"></i> Open phpMyAdmin</a><a class="button" href="/adminer/" style="margin-top:8px;"><i class="fa-solid fa-database"></i> Open Adminer</a></div></div>
    </div>
    <?php endif; ?>

    <?php if ($view == 'php'): ?>
    <div class="grid">
        <div class="card"><h3><i class="fa-solid fa-gear"></i> PHP Core Settings</h3><div class="card-body"><div class="info-row"><span class="info-label">PHP Version:</span><span class="info-value"><?php echo phpversion(); ?></span></div><div class="info-row"><span class="info-label">Max Execution Time:</span><span class="info-value"><?php echo $max_execution_time; ?> sec</span></div><div class="info-row"><span class="info-label">Memory Limit:</span><span class="info-value"><?php echo $memory_limit; ?></span></div><div class="info-row"><span class="info-label">Upload Max Filesize:</span><span class="info-value"><?php echo $upload_max_filesize; ?></span></div><div class="info-row"><span class="info-label">Post Max Size:</span><span class="info-value"><?php echo $post_max_size; ?></span></div><div class="info-row"><span class="info-label">Display Errors:</span><span class="info-value"><?php echo $display_errors; ?></span></div><div class="info-row"><span class="info-label">Disabled Functions:</span><span class="info-value"><?php echo $disabled_functions; ?></span></div><div class="info-row"><span class="info-label">Timezone:</span><span class="info-value"><?php echo $date_timezone; ?></span></div></div></div>
        <div class="card"><h3><i class="fa-solid fa-cogs"></i> Key PHP Variables</h3><div class="card-body"><div class="info-row"><span class="info-label">Document Root:</span><span class="info-value"><?php echo $_SERVER['DOCUMENT_ROOT'] ?? 'N/A'; ?></span></div><div class="info-row"><span class="info-label">Script Filename:</span><span class="info-value"><?php echo $_SERVER['SCRIPT_FILENAME'] ?? 'N/A'; ?></span></div><div class="info-row"><span class="info-label">Gateway Interface:</span><span class="info-value"><?php echo $_SERVER['GATEWAY_INTERFACE'] ?? 'N/A'; ?></span></div></div></div>
    </div>
    <?php endif; ?>

    <?php if ($view == 'extensions'): ?>
    <div class="grid">
        <div class="card"><h3><i class="fa-solid fa-puzzle-piece"></i> All Loaded PHP Extensions</h3><div class="card-body"><div><?php $ext_list = get_loaded_extensions(); sort($ext_list); foreach ($ext_list as $ext) { echo '<span class="badge-ext">' . $ext . '</span> '; } ?><br><br><strong>Total:</strong> <?php echo count($ext_list); ?> extensions</div></div></div>
        <div class="card"><h3><i class="fa-solid fa-check-circle"></i> Extension Status (Key Extensions)</h3><div class="card-body"><div class="grid-2col"><div>zip: <?php echo ext("zip"); ?></div><div>gd: <?php echo ext("gd"); ?></div><div>curl: <?php echo ext("curl"); ?></div><div>json: <?php echo ext("json"); ?></div><div>mbstring: <?php echo ext("mbstring"); ?></div><div>pdo: <?php echo ext("pdo"); ?></div><div>mysqli: <?php echo ext("mysqli"); ?></div><div>openssl: <?php echo ext("openssl"); ?></div><div>fileinfo: <?php echo ext("fileinfo"); ?></div><div>session: <?php echo ext("session"); ?></div><div>xml: <?php echo ext("xml"); ?></div><div>bcmath: <?php echo ext("bcmath"); ?></div></div></div></div>
    </div>
    <br>
    <?php endif; ?>
</div>

<!-- BOTTOM NAVIGATION -->
<div class="bottom-nav">
  <button class="<?php echo $view == 'dashboard' ? 'active' : ''; ?>" onclick="location.href='?view=dashboard'"><i class="fa-solid fa-house"></i><span>Dashboard</span></button>
  <button class="<?php echo $view == 'server' ? 'active' : ''; ?>" onclick="location.href='?view=server'"><i class="fa-solid fa-microchip"></i><span>Server</span></button>
  <button class="<?php echo $view == 'database' ? 'active' : ''; ?>" onclick="location.href='?view=database'"><i class="fa-solid fa-database"></i><span>Database</span></button>
  <button class="<?php echo $view == 'php' ? 'active' : ''; ?>" onclick="location.href='?view=php'"><i class="fa-solid fa-code"></i><span>Config</span></button>
  <button class="<?php echo $view == 'extensions' ? 'active' : ''; ?>" onclick="location.href='?view=extensions'"><i class="fa-solid fa-puzzle-piece"></i><span>Extensions</span></button>
  <button onclick="location.href='?view=phpinfo'"><i class="fa-solid fa-info-circle"></i><span>PHP Info</span></button>
</div>

<script>
// Simple logout simulation (no emojis, clean)
document.getElementById('logoutMockBtn')?.addEventListener('click', function(e) {
    e.preventDefault();
    alert("Logout simulation - session would be destroyed in production.");
});
</script>
</body>
</html>