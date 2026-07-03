# AMPDroid

**Android-based local server stack - Apache · PHP · MySQL - on your phone.**

AMPDroid lets you run a full web development environment directly on an Android device. No PC required. Serve PHP pages, manage databases, edit files, and access your local server from any device on the same network.

[![Latest Release](https://img.shields.io/github/v/release/Lodwickmasete/ampdroid?label=latest&color=1e3a5f)](https://github.com/Lodwickmasete/ampdroid/releases/tag/v2.0)
[![Platform](https://img.shields.io/badge/platform-Android-3ddc84?logo=android)](https://github.com/Lodwickmasete/ampdroid/releases)
[![License](https://img.shields.io/github/license/Lodwickmasete/ampdroid)](LICENSE)

---

## What's Included

| Component | Description |
|---|---|
| **Apache** | HTTP & HTTPS web server |
| **PHP** | Server-side scripting engine |
| **MySQL** | Relational database server |
| **File Manager** | Web-based file browser and editor |
| **Adminer** | Database management UI (like phpMyAdmin) |
| **Code Editor** | Syntax-highlighted in-browser editor |

---

## Requirements

- Android 7.0 (Nougat) or higher
- ~200 MB free storage *(offline installer)* or internet connection *(net installer)*
- Allow installation from unknown sources *(see step 2 below)*

---

## Download

Go to the [**Releases page**](https://github.com/Lodwickmasete/ampdroid/releases/tag/v2.0) and download one of the two APKs:

| APK | When to use |
|---|---|
| `ampdroid-net-installer.apk` | You have a stable internet connection. The app downloads server binaries during first-time setup. Smaller download up front. |
| `ampdroid-offline-installer.apk` | No internet available, or you want everything bundled. Larger file but fully self-contained. |

---

## Installation

### Step 1 - Download the APK

Download either APK from the [releases page](https://github.com/Lodwickmasete/ampdroid/releases/tag/v2.0) directly on your Android device, or transfer it from a PC via USB.

### Step 2 - Allow unknown sources

Android blocks APKs from outside the Play Store by default. To allow installation:

- **Android 8.0+:** When you open the APK, Android will ask *"Allow from this source?"* - tap **Allow**, then **Install**.
- **Android 7.x:** Go to **Settings → Security → Unknown sources** and enable it, then open the APK.

### Step 3 - Install

Tap the downloaded APK file (usually in your **Downloads** folder) and follow the on-screen prompts to install.

### Step 4 - First-time setup

Open **AMPDroid** from your app drawer. On first launch, the app will run a setup wizard:

- **Net installer:** The wizard downloads and installs Apache, PHP, and MySQL binaries. Keep your internet connection active until setup completes.
- **Offline installer:** The wizard extracts the bundled binaries. No internet needed.

Setup typically takes 1-3 minutes. Do not close the app during this process.

### Step 5 - Start the server

Once setup is complete, you'll see the AMPDroid dashboard. Tap **Start** to bring up Apache, PHP, and MySQL.

When all three services show a green indicator, your server is running.

---

## Accessing Your Server

### From the device itself

Open any browser on the Android device and go to:

```
http://localhost:8080
```

### From another device on the same network

Find your device's local IP address in AMPDroid's dashboard (shown as *Server address*), then open it from any browser on the same Wi-Fi:

```
http://192.168.x.x:8080
```

### HTTPS

AMPDroid v2.0 includes HTTPS support. Access your server securely at:

```
https://localhost:8443
```

> A self-signed certificate is used by default. Your browser may show a security warning - this is expected on a local server. Accept the certificate to proceed.

---

## Web Tools

Once the server is running, these tools are available in the browser:

### File Manager

```
http://localhost:8080/ampdroid/file-manager/
```

Browse, upload, rename, move, delete, zip, and extract files. Includes a built-in code editor with syntax highlighting for HTML, PHP, JS, CSS, SQL, Python, and more.

### Adminer (Database Manager)

```
http://localhost:8080/ampdroid/adminer/
```

A lightweight database management interface. Use it to create databases, run SQL queries, import/export data, and manage users - similar to phpMyAdmin.

**Default MySQL credentials:**

| Field | Value |
|---|---|
| Server | `localhost` |
| Username | `root` |
| Password | *(leave blank or set during setup)* |

### Your web root

Place your PHP/HTML project files in the document root (visible in the File Manager as `/`). They are served at:

```
http://localhost:8080/your-project/
```

---

## What's New in v2.0

- **Web File Manager** - manage files entirely from the browser
- **Adminer** - full database UI, no phpMyAdmin setup needed
- **HTTPS support** - serve over SSL with a self-signed certificate
- **Shortcut toolbar** - quick access to common actions from the dashboard
- Fixed offline and online installation flows
- Fixed editor showing duplicate file icons
- Removed FTP stub

---

## Troubleshooting

**Server won't start**
Make sure no other app is using ports 8080 or 3306. Some Android versions restrict port binding - try restarting the app with the device's Battery Optimization disabled for AMPDroid.

**Net installer gets stuck**
Check your internet connection. If the download fails partway, restart the app - setup will resume from where it left off.

**Can't access from another device**
Confirm both devices are on the same Wi-Fi network. Some routers have client isolation enabled, which blocks device-to-device traffic - check your router settings.

**Database connection refused**
MySQL may still be starting up. Wait a few seconds after tapping Start, then try again.

---

## License

[MIT](LICENSE)
