(function() {
  'use strict';

  /* =========================================================
     CONFIG
  ========================================================= */
  const API = '/ampdroid/file-manager/api/index.php';

  /* =========================================================
     STATE
  ========================================================= */
  let currentPath = '/';
  let currentItems = [];
  let buffer = null;       // { action:'copy'|'cut', path: string, name: string }
  let selectedItems = new Set();
  let sortCol = 'name', sortAsc = true;
  let showHidden = false;
  let ctxTargetName = null;   // the item right-clicked
  let ctxTargetIsZip = false;
  let extractTargetZip = null;
  let selectedExtractDest = null;

  /* =========================================================
     HELPERS
  ========================================================= */
  function dom(id) { return document.getElementById(id); }

  function showFeedback(msg, type) {
    type = type || 'info';
    const bar = dom('feedback-bar');
    const icon = dom('feedback-icon');
    const txt = dom('feedback-msg');
    bar.className = 'feedback-bar ' + type;
    txt.innerText = msg;
    const iconMap = { info: 'fa-info-circle', success: 'fa-check-circle', error: 'fa-exclamation-circle' };
    icon.className = 'fas ' + (iconMap[type] || 'fa-info-circle');
    bar.style.display = 'flex';
    clearTimeout(window._fbTimer);
    window._fbTimer = setTimeout(function() { bar.style.display = 'none'; }, 4000);
  }

  function simulateProgress(cb, duration) {
    duration = duration || 600;
    var prog = dom('addrbar-progress');
    if (!prog) { cb(); return; }
    prog.style.opacity = '1';
    prog.classList.add('active');
    prog.style.width = '0%';
    var val = 0;
    var interval = setInterval(function() {
      val += Math.random() * 25 + 10;
      if (val >= 100) {
        val = 100;
        prog.style.width = '100%';
        clearInterval(interval);
        setTimeout(function() {
          prog.style.opacity = '0';
          setTimeout(function() {
            prog.classList.remove('active');
            prog.style.width = '0%';
            prog.style.opacity = '';
          }, 180);
          cb();
        }, 100);
      } else {
        prog.style.width = val + '%';
      }
    }, duration / 10);
  }

  /* Show a simple input prompt modal */
  function showPrompt(title, msg, defaultVal, cb) {
    dom('modal-prompt-title').innerText = title;
    dom('modal-prompt-msg').innerText = msg;
    var inp = dom('modal-prompt-input');
    inp.value = defaultVal || '';
    dom('modal-prompt').classList.add('show');
    inp.focus();
    inp.select();
    function cleanup() { dom('modal-prompt').classList.remove('show'); }
    dom('modal-prompt-ok').onclick = function() { var v = inp.value.trim(); if (v) { cleanup(); cb(v); } };
    dom('modal-prompt-cancel').onclick = function() { cleanup(); };
    inp.onkeydown = function(e) { if (e.key === 'Enter') dom('modal-prompt-ok').click(); if (e.key === 'Escape') cleanup(); };
  }

  /* Show confirm modal */
  function showConfirm(title, msg, cb) {
    dom('modal-confirm-title').innerText = title;
    dom('modal-confirm-msg').innerText = msg;
    dom('modal-confirm').classList.add('show');
    function cleanup() { dom('modal-confirm').classList.remove('show'); }
    dom('modal-confirm-ok').onclick = function() { cleanup(); cb(); };
    dom('modal-confirm-cancel').onclick = function() { cleanup(); };
  }

  function joinPath(base, name) {
    if (base === '/') return '/' + name;
    return base.replace(/\/+$/, '') + '/' + name;
  }

  function formatSize(bytes) {
    if (bytes === null || bytes === undefined || bytes === '') return '—';
    var n = parseInt(bytes);
    if (isNaN(n)) return bytes;
    if (n < 1024) return n + ' B';
    if (n < 1048576) return (n / 1024).toFixed(1) + ' KB';
    if (n < 1073741824) return (n / 1048576).toFixed(1) + ' MB';
    return (n / 1073741824).toFixed(2) + ' GB';
  }

  function getExtension(name) {
    var dot = name.lastIndexOf('.');
    if (dot < 0 || dot === name.length - 1) return '';
    return name.substring(dot + 1).toLowerCase();
  }

  function isZip(name) { return getExtension(name) === 'zip'; }

  function typeLabel(item) {
    if (item.type === 'dir') return 'Folder';
    var ext = getExtension(item.name);
    return ext ? ext.toUpperCase() + ' File' : 'File';
  }

  function getIconHtml(item) {
    if (item.type === 'dir') return '<i class="fas fa-folder" style="color:#d4a43a;"></i>';
    var ext = getExtension(item.name);
    if (ext === 'zip' || ext === 'tar' || ext === 'gz' || ext === 'rar') return '<i class="fas fa-file-archive" style="color:#b87333;"></i>';
    if (ext === 'pdf') return '<i class="fas fa-file-pdf" style="color:#cc3333;"></i>';
    if (ext === 'jpg' || ext === 'jpeg' || ext === 'png' || ext === 'gif' || ext === 'webp') return '<i class="fas fa-file-image" style="color:#4488cc;"></i>';
    if (ext === 'js' || ext === 'php' || ext === 'py' || ext === 'sh' || ext === 'html' || ext === 'css') return '<i class="fas fa-file-code" style="color:#3399aa;"></i>';
    if (ext === 'mp3' || ext === 'wav' || ext === 'ogg') return '<i class="fas fa-file-audio" style="color:#886633;"></i>';
    if (ext === 'mp4' || ext === 'avi' || ext === 'mkv') return '<i class="fas fa-file-video" style="color:#664499;"></i>';
    if (ext === 'txt' || ext === 'md' || ext === 'log') return '<i class="fas fa-file-alt" style="color:#5577aa;"></i>';
    return '<i class="fas fa-file"></i>';
  }

  /* =========================================================
     API CALLS
  ========================================================= */
  function apiCall(params, cb) {
    var url = API + '?';
    var parts = [];
    for (var k in params) {
      if (k !== 'body') parts.push(encodeURIComponent(k) + '=' + encodeURIComponent(params[k]));
    }
    var method = params.body ? 'POST' : 'GET';
    var opts = { method: method, headers: { 'Accept': 'application/json' } };
    if (params.body) {
      opts.body = params.body;
      url = API;
      var qp = parts.filter(function(p) { return !p.startsWith('body'); });
      if (qp.length) url += '?' + qp.join('&');
    } else {
      url += parts.join('&');
    }
    fetch(url, opts)
      .then(function(r) { return r.json(); })
      .then(function(data) { cb(null, data); })
      .catch(function(e) { cb(e.message || 'Network error', null); });
  }

  /* =========================================================
     SELECTION
  ========================================================= */
  function unselectAll() {
    selectedItems.clear();
    updateSelectionHighlight();
    updateToolbarBySelection();
    updateStatus();
    dom('chk-all').checked = false;
    dom('chk-all').indeterminate = false;
  }

  function selectSingle(name, multi) {
    if (!multi) selectedItems.clear();
    if (name) {
      if (multi && selectedItems.has(name)) selectedItems.delete(name);
      else selectedItems.add(name);
    }
    updateSelectionHighlight();
    updateToolbarBySelection();
    updateStatus();
    syncSelectAllCheckbox();
  }

  function updateSelectionHighlight() {
    document.querySelectorAll('#tbody tr[data-name]').forEach(function(tr) {
      if (selectedItems.has(tr.dataset.name)) tr.classList.add('selected');
      else tr.classList.remove('selected');
    });
  }

  function updateToolbarBySelection() {
    var has = selectedItems.size > 0;
    ['btn-del','btn-rename','btn-copy','btn-cut'].forEach(function(id) {
      var b = dom(id);
      if (b) b.disabled = !has;
    });
  }

  function getOneSelected() {
    return selectedItems.size === 1 ? Array.from(selectedItems)[0] : null;
  }

  function syncSelectAllCheckbox() {
    var visible = filterHidden(currentItems);
    var chk = dom('chk-all');
    if (selectedItems.size === 0) { chk.checked = false; chk.indeterminate = false; }
    else if (selectedItems.size === visible.length) { chk.checked = true; chk.indeterminate = false; }
    else { chk.checked = false; chk.indeterminate = true; }
  }

  /* =========================================================
     FILTER & SORT
  ========================================================= */
  function filterHidden(arr) {
    if (showHidden) return arr;
    return arr.filter(function(it) { return it.name.charAt(0) !== '.'; });
  }

  function sortItems(arr) {
    return arr.slice().sort(function(a, b) {
      if (a.type !== b.type) return a.type === 'dir' ? -1 : 1;
      var va = a.name.toLowerCase(), vb = b.name.toLowerCase();
      if (sortCol === 'type') { va = typeLabel(a).toLowerCase(); vb = typeLabel(b).toLowerCase(); }
      else if (sortCol === 'size') {
        var sa = parseInt(a.size) || 0;
        var sb = parseInt(b.size) || 0;
        return sortAsc ? sa - sb : sb - sa;
      }
      return sortAsc ? va.localeCompare(vb) : vb.localeCompare(va);
    });
  }

  /* =========================================================
     RENDER
  ========================================================= */
  function render(items) {
    if (items !== undefined) currentItems = items;

    dom('path-input').value = currentPath;
    renderBreadcrumb();

    var tbody = dom('tbody');
    tbody.innerHTML = '';

    // Parent directory row
    if (currentPath !== '/') {
      var upTr = document.createElement('tr');
      upTr.className = 'parent-row';
      upTr.style.cursor = 'pointer';
      upTr.innerHTML = '<td class="col-icon"><i class="fas fa-arrow-up"></i></td>' +
        '<td class="col-name" colspan="4"><a class="flink isdir" id="parent-link">[ .. ]</a></td>';
      upTr.addEventListener('click', function(e) {
        e.stopPropagation();
        navigateUp();
      });
      tbody.appendChild(upTr);
    }

    var visible = filterHidden(currentItems);
    var sorted = sortItems(visible);

    sorted.forEach(function(item) {
      var isDir = item.type === 'dir';
      var tr = document.createElement('tr');
      tr.dataset.name = item.name;
      tr.style.cursor = 'pointer';

      var sizeDisplay = isDir ? '—' : formatSize(item.size);
      tr.innerHTML =
        '<td class="col-icon"><input type="checkbox" class="row-chk" data-name="' + escHtml(item.name) + '"></td>' +
        '<td class="col-name"><a class="flink' + (isDir ? ' isdir' : '') + '">' + getIconHtml(item) + ' ' + escHtml(item.name) + '</a></td>' +
        '<td class="col-type">' + typeLabel(item) + '</td>' +
        '<td class="col-size">' + sizeDisplay + '</td>' +
        '<td class="col-menu">' +
          '<button class="btn-item-menu" data-name="' + escHtml(item.name) + '" title="Menu" aria-label="Menu for ' + escHtml(item.name) + '">⋮</button>' +
        '</td>';

      var link = tr.querySelector('.flink');
      var chk = tr.querySelector('.row-chk');
      var menuBtn = tr.querySelector('.btn-item-menu');

      // Set checkbox state immediately - no delay
      chk.checked = selectedItems.has(item.name);

      chk.addEventListener('change', function(e) {
        e.stopPropagation();
        selectSingle(item.name, true);
        // Force immediate checkbox update
        chk.checked = selectedItems.has(item.name);
        syncSelectAllCheckbox();
      });

      chk.addEventListener('click', function(e) {
        e.stopPropagation();
      });

      // Row click - navigate into folders, select files
      tr.addEventListener('click', function(e) {
        // Don't handle if clicking checkbox or menu button
        if (e.target === chk || e.target === menuBtn || menuBtn.contains(e.target)) return;
        
        if (isDir) {
          // Navigate into directory on single click
          navigateTo(joinPath(currentPath, item.name));
        } else {
          // Select file on single click (with multi-select support)
          selectSingle(item.name, e.ctrlKey || e.metaKey || e.shiftKey);
        }
      });

      tr.addEventListener('dblclick', function(e) {
        e.stopPropagation();
        if (e.target === chk || e.target === menuBtn || menuBtn.contains(e.target)) return;
        if (isDir) {
          navigateTo(joinPath(currentPath, item.name));
        } else {
          showFeedback('Opened: ' + item.name + ' (preview not available)', 'info');
/*
          var item = currentItems.find(function(i) { return i.name === name; });
          if (!item) return;
*/
          window.location.href = API + '?action=download&path=' + joinPath(currentPath, item.name);
        }
      });





      // Three-dot menu button click (for mobile)
      menuBtn.addEventListener('click', function(e) {
        e.stopPropagation();
        e.preventDefault();
        
        ctxTargetName = item.name;
        ctxTargetIsZip = isZip(item.name);

        // Select this item if not already selected
        if (!selectedItems.has(item.name)) {
          selectSingle(item.name, false);
        }

        // Update context menu for this item
        updateContextMenuForItem(item);

        // Position context menu near the button
        var rect = menuBtn.getBoundingClientRect();
        showCtx(dom('ctx-files'), rect.left, rect.bottom);
      });

      // Right-click context menu (for desktop)
      tr.addEventListener('contextmenu', function(e) {
        e.preventDefault();
        e.stopPropagation();
        
        ctxTargetName = item.name;
        ctxTargetIsZip = isZip(item.name);

        if (!selectedItems.has(item.name)) {
          selectSingle(item.name, false);
        }

        updateContextMenuForItem(item);
        showCtx(dom('ctx-files'), e.clientX, e.clientY);
      });

      // Drag and drop
      tr.setAttribute('draggable', true);
      tr.addEventListener('dragstart', function(e) {
        e.dataTransfer.setData('text/plain', item.name);
        selectSingle(item.name, false);
      });
      if (isDir) {
        tr.addEventListener('dragover', function(e) { 
          e.preventDefault(); 
          tr.style.outline = '2px dotted #4466aa'; 
        });
        tr.addEventListener('dragleave', function() { 
          tr.style.outline = ''; 
        });
        tr.addEventListener('drop', function(e) {
          e.preventDefault();
          tr.style.outline = '';
          var dragName = e.dataTransfer.getData('text/plain');
          if (dragName && dragName !== item.name) {
            doMove(dragName, joinPath(currentPath, item.name));
          }
        });
      }

      tbody.appendChild(tr);
    });

    if (sorted.length === 0 && currentPath !== '/') {
      var emptyTr = document.createElement('tr');
      emptyTr.innerHTML = '<td colspan="5" style="padding:14px; color:#888; text-align:center; font-style:italic;">Empty folder</td>';
      tbody.appendChild(emptyTr);
    }

    updateSelectionHighlight();
    updateStatus();
    updateSortArrows();
    syncSelectAllCheckbox();
    updateClipboardButtons();
  }

  function updateContextMenuForItem(item) {
    // Show/hide zip-specific menu items
    dom('ctx-zip-section').style.display = ctxTargetIsZip ? '' : 'none';
    dom('ctx-extract-here-li').style.display = ctxTargetIsZip ? '' : 'none';
    dom('ctx-extract-to-li').style.display = ctxTargetIsZip ? '' : 'none';

    // Rename "Create ZIP" label based on selection
    if (item && item.type === 'dir') {
      dom('btn-ctx-zip-folder').innerHTML = '<i class="fas fa-file-archive"></i> Create ZIP from folder';
    } else {
      dom('btn-ctx-zip-folder').innerHTML = '<i class="fas fa-file-archive"></i> Create ZIP';
    }
  }

  function escHtml(s) {
    return String(s)
      .replace(/&/g,'&amp;')
      .replace(/</g,'&lt;')
      .replace(/>/g,'&gt;')
      .replace(/"/g,'&quot;');
  }

  function renderBreadcrumb() {
    var bc = dom('breadcrumb');
    var parts = currentPath === '/' ? [''] : currentPath.split('/');
    var html = '';
    var accumulated = '';
    parts.forEach(function(p, i) {
      if (i === 0) { // root
        if (i === parts.length - 1) html += '<b>root</b>';
        else html += '<a class="bclink" data-path="/">' + 'root' + '</a>';
        return;
      }
      accumulated += '/' + p;
      var label = p;
      if (i === parts.length - 1) html += '<span class="bsep">/</span><b>' + escHtml(label) + '</b>';
      else html += '<span class="bsep">/</span><a class="bclink" data-path="' + escHtml(accumulated) + '">' + escHtml(label) + '</a>';
    });
    bc.innerHTML = html;
    bc.querySelectorAll('.bclink').forEach(function(a) {
      a.addEventListener('click', function() { navigateTo(a.dataset.path); });
    });
  }

  function updateSortArrows() {
    ['name','type','size'].forEach(function(c) {
      var el = dom('sarr-' + c);
      if (el) el.innerHTML = sortCol === c ? (sortAsc ? '▲' : '▼') : '';
    });
  }

  function updateStatus() {
    var visible = filterHidden(currentItems);
    dom('st-count').innerText = visible.length + ' item(s)';
    dom('st-sel').innerText = selectedItems.size ? 'Selected: ' + selectedItems.size : '';

var clipTxt = '';
if (buffer) clipTxt = 'Clipboard: ' + buffer.names.length + ' item(s)' + (buffer.action === 'cut' ? ' (cut)' : ' (copy)');
dom('st-clip').innerText = clipTxt;
  }

  function updateClipboardButtons() {
    var hasBuf = !!buffer;
    if (dom('btn-paste')) dom('btn-paste').disabled = !hasBuf;
    if (dom('btn-modal-paste')) dom('btn-modal-paste').disabled = !hasBuf;
  }

  /* =========================================================
     NAVIGATION
  ========================================================= */
  function navigateTo(path) {
    simulateProgress(function() {
      apiCall({ action: 'list', path: path }, function(err, data) {
        if (err || !data.ok) {
          showFeedback('Error: ' + (data && data.error ? data.error : (err || 'Unknown')), 'error');
          return;
        }
        currentPath = data.path || path;
        unselectAll();
        render(data.items);
      });
    });
  }

  function navigateUp() {
    if (currentPath === '/') return;
    var parts = currentPath.replace(/\/+$/, '').split('/');
    parts.pop();
    var up = parts.join('/') || '/';
    navigateTo(up);
  }

  /* =========================================================
     OPERATIONS
  ========================================================= */
  function doCreateDir(name) {
    apiCall({ action: 'mkdir', path: currentPath, name: name }, function(err, data) {
      if (err || !data.ok) { showFeedback('Error: ' + (data && data.error ? data.error : (err || 'Failed')), 'error'); return; }
      showFeedback('Folder created: ' + name, 'success');
      navigateTo(currentPath);
    });
  }

  function doCreateFile(name) {
    apiCall({ action: 'mkfile', path: currentPath, name: name }, function(err, data) {
      if (err || !data.ok) { showFeedback('Error: ' + (data && data.error ? data.error : (err || 'Failed')), 'error'); return; }
      showFeedback('File created: ' + name, 'success');
      navigateTo(currentPath);
    });
  }

  function doRename(oldName, newName) {
    apiCall({ action: 'rename', path: currentPath, oldName: oldName, newName: newName }, function(err, data) {
      if (err || !data.ok) { showFeedback('Error: ' + (data && data.error ? data.error : (err || 'Failed')), 'error'); return; }
      showFeedback('Renamed to: ' + newName, 'success');
      unselectAll();
      navigateTo(currentPath);
    });
  }

  function doDelete(names) {
    apiCall({
      action: 'delete',
      body: JSON.stringify({ path: currentPath, names: names }),
    }, function(err, data) {
      if (err || !data.ok) { showFeedback('Error: ' + (data && data.error ? data.error : (err || 'Failed')), 'error'); return; }
      showFeedback('Deleted ' + names.length + ' item(s)', 'success');
      unselectAll();
      navigateTo(currentPath);
    });
  }


function doCopy(names) {
    // names: string or array
    var arr = Array.isArray(names) ? names : [names];
    buffer = { action: 'copy', srcPath: currentPath, names: arr };
    updateClipboardButtons();
    updateStatus();
    showFeedback('Copied: ' + arr.length + ' item(s)', 'info');
}


function doCut(names) {
    var arr = Array.isArray(names) ? names : [names];
    buffer = { action: 'cut', srcPath: currentPath, names: arr };
    updateClipboardButtons();
    updateStatus();
    showFeedback('Cut: ' + arr.length + ' item(s)', 'info');
}


function doPaste() {
    if (!buffer) { showFeedback('Nothing to paste', 'error'); return; }
    var isCut = buffer.action === 'cut';
    var names = buffer.names;
    var total = names.length;

    // Ask user: background or show progress
    showPasteModeModal(isCut, total, function(background) {
        if (background) {
            doBulkPasteBackground(isCut, names, total);
        } else {
            doBulkPasteProgress(isCut, names, total);
        }
    });
}

/* Paste mode picker modal */
function showPasteModeModal(isCut, total, cb) {
    var op = isCut ? 'Move' : 'Copy';
    showConfirm(
        op + ' ' + total + ' item(s)',
        'How do you want to ' + op.toLowerCase() + ' ' + total + ' item(s) to "' + currentPath + '"?\n\n' +
        'Choose OK for progress view, Cancel for background (silent).',
        function() { cb(false); },   // OK = show progress
        function() { cb(true); }     // Cancel = background
    );
}

function doBulkPasteBackground(isCut, names, total) {
    var action = isCut ? 'bulk_move' : 'bulk_copy';
    var buf = buffer; // capture before clearing
    buffer = null; updateClipboardButtons(); updateStatus();
    apiCall({
        action: action,
        body: JSON.stringify({ srcPath: buf.srcPath, names: names, destPath: currentPath })
    }, function(err, data) {
        if (err || !data.ok) {
            showFeedback('Error: ' + (data && data.error ? data.error : (err || 'Failed')), 'error');
            return;
        }
        var failed = data.failed || 0;
        if (failed > 0) showFeedback((total - failed) + '/' + total + ' ' + (isCut ? 'moved' : 'copied') + ', ' + failed + ' failed', 'error');
        else showFeedback(total + ' item(s) ' + (isCut ? 'moved' : 'copied'), 'success');
        unselectAll();
        navigateTo(currentPath);
    });
}

function doBulkPasteProgress(isCut, names, total) {
    var buf = buffer; // capture before clearing
    buffer = null; updateClipboardButtons(); updateStatus();

    // Show progress modal
    var overlay = document.createElement('div');
    overlay.style.cssText = 'position:fixed;inset:0;background:rgba(0,0,0,.55);z-index:9999;display:flex;align-items:center;justify-content:center;';
    var box = document.createElement('div');
    box.style.cssText = 'background:#1e2330;border-radius:10px;padding:24px 28px;min-width:320px;max-width:90vw;color:#eee;font-family:inherit;';
    box.innerHTML =
        '<div style="font-weight:600;font-size:1.05em;margin-bottom:12px;" id="bprog-title">' +
        (isCut ? 'Moving' : 'Copying') + ' 0 / ' + total + '</div>' +
        '<div style="background:#333;border-radius:4px;height:10px;margin-bottom:10px;">' +
          '<div id="bprog-bar" style="height:10px;border-radius:4px;background:#4a90d9;width:0%;transition:width .2s;"></div>' +
        '</div>' +
        '<div id="bprog-cur" style="font-size:.85em;color:#aaa;white-space:nowrap;overflow:hidden;text-overflow:ellipsis;max-width:280px;"></div>';
    overlay.appendChild(box);
    document.body.appendChild(overlay);

    var done = 0, failed = 0;
    var action = isCut ? 'move' : 'copy';

    function next(i) {
        if (i >= total) {
            document.body.removeChild(overlay);
            if (failed > 0) showFeedback((total - failed) + '/' + total + ' ' + (isCut ? 'moved' : 'copied') + ', ' + failed + ' failed', 'error');
            else showFeedback(total + ' item(s) ' + (isCut ? 'moved' : 'copied'), 'success');
            unselectAll();
            navigateTo(currentPath);
            return;
        }
        var name = names[i];
        dom('bprog-cur').innerText = name;
        dom('bprog-title').innerText = (isCut ? 'Moving' : 'Copying') + ' ' + (done + 1) + ' / ' + total;
        dom('bprog-bar').style.width = Math.round((i / total) * 100) + '%';

        apiCall({ action: action, srcPath: buf.srcPath, name: name, destPath: currentPath },
        function(err, data) {
            done++;
            if (err || !data.ok) failed++;
            dom('bprog-bar').style.width = Math.round((done / total) * 100) + '%';
            next(i + 1);
        });
    }
    next(0);
}

// REPLACE existing showConfirm
function showConfirm(title, msg, cb, cancelCb) {
    dom('modal-confirm-title').innerText = title;
    dom('modal-confirm-msg').innerText = msg;
    dom('modal-confirm').classList.add('show');
    function cleanup() { dom('modal-confirm').classList.remove('show'); }
    dom('modal-confirm-ok').onclick = function() { cleanup(); cb(); };
    dom('modal-confirm-cancel').onclick = function() { cleanup(); if (cancelCb) cancelCb(); };
}

  function doMove(name, destFullPath) {
    // destFullPath = full absolute destination folder path
    var destParts = destFullPath.split('/');
    var destName = destParts.pop();
    var destParent = destParts.join('/') || '/';
    apiCall({ action: 'move', srcPath: currentPath, name: name, destPath: destFullPath }, function(err, data) {
      if (err || !data.ok) { showFeedback('Error moving: ' + (data && data.error ? data.error : (err || 'Failed')), 'error'); return; }
      showFeedback('Moved "' + name + '" into ' + destName, 'success');
      unselectAll();
      navigateTo(currentPath);
    });
  }

  function doZipSelected() {
    var names = Array.from(selectedItems);
    if (!names.length) { showFeedback('Select items to zip', 'error'); return; }
    showPrompt('Create ZIP', 'ZIP archive name:', 'archive.zip', function(zipName) {
      if (!zipName.endsWith('.zip')) zipName += '.zip';
      apiCall({
        action: 'zip',
        body: JSON.stringify({ path: currentPath, names: names, zipName: zipName })
      }, function(err, data) {
        if (err || !data.ok) { showFeedback('ZIP error: ' + (data && data.error ? data.error : (err || 'Failed')), 'error'); return; }
        showFeedback('Created: ' + zipName, 'success');
        navigateTo(currentPath);
      });
    });
  }

  function doExtractHere(zipName) {
    apiCall({ action: 'extract', path: currentPath, name: zipName, destPath: currentPath }, function(err, data) {
      if (err || !data.ok) { showFeedback('Extract error: ' + (data && data.error ? data.error : (err || 'Failed')), 'error'); return; }
      showFeedback('Extracted: ' + zipName, 'success');
      navigateTo(currentPath);
    });
  }

  function doExtractTo(zipName, destPath) {
    apiCall({ action: 'extract', path: currentPath, name: zipName, destPath: destPath }, function(err, data) {
      if (err || !data.ok) { showFeedback('Extract error: ' + (data && data.error ? data.error : (err || 'Failed')), 'error'); return; }
      showFeedback('Extracted to: ' + destPath, 'success');
      navigateTo(currentPath);
    });
  }

  function doShowProperties(name) {
    var item = currentItems.find(function(i) { return i.name === name; });
    if (!item) return;
    var rows = [
      ['Name', item.name],
      ['Type', typeLabel(item)],
      ['Size', item.type === 'dir' ? '—' : formatSize(item.size)],
      ['Path', joinPath(currentPath, item.name)],
      ['Modified', item.modified || '—'],
      ['Permissions', item.permissions || '—']
    ];
    var html = rows.map(function(r) {
      return '<tr><td>' + escHtml(r[0]) + '</td><td>' + escHtml(String(r[1])) + '</td></tr>';
    }).join('');
    dom('prop-table').innerHTML = html;
    dom('modal-properties').classList.add('show');
  }

  /* =========================================================
     UPLOAD
  ========================================================= */
  function openUploadModal() {
    dom('upload-dest-path').innerText = currentPath;
    dom('upload-file-input').value = '';
    dom('upload-preview').style.display = 'none';
    dom('upload-preview').innerHTML = '';
    dom('upload-confirm-btn').disabled = true;
    dom('upload-progress-wrap').style.display = 'none';
    dom('upload-progress-fill').style.width = '0%';
    dom('upload-progress-fill').innerText = '0%';
    dom('modal-upload').classList.add('show');
  }

  dom('upload-file-input').addEventListener('change', function() {
    var files = this.files;
    if (!files || !files.length) {
      dom('upload-preview').style.display = 'none';
      dom('upload-confirm-btn').disabled = true;
      return;
    }
    var html = '';
    for (var i = 0; i < files.length; i++) {
      html += '<div><span class="fname">' + escHtml(files[i].name) + '</span>' +
        '<span class="fsize">' + formatSize(files[i].size) + '</span></div>';
    }
    dom('upload-preview').innerHTML = html;
    dom('upload-preview').style.display = 'block';
    dom('upload-confirm-btn').disabled = false;
  });

  dom('upload-confirm-btn').addEventListener('click', function() {
    var files = dom('upload-file-input').files;
    if (!files || !files.length) return;

    var formData = new FormData();
    formData.append('action', 'upload');
    formData.append('path', currentPath);
    for (var i = 0; i < files.length; i++) {
      formData.append('files[]', files[i]);
    }

    dom('upload-confirm-btn').disabled = true;
    dom('upload-cancel-btn').disabled = true;
    var wrap = dom('upload-progress-wrap');
    var fill = dom('upload-progress-fill');
    wrap.style.display = 'block';

    var xhr = new XMLHttpRequest();
    xhr.open('POST', API + "?action=upload");
    xhr.setRequestHeader('Accept', 'application/json');
    xhr.upload.addEventListener('progress', function(e) {
      if (e.lengthComputable) {
        var pct = Math.round((e.loaded / e.total) * 100);
        fill.style.width = pct + '%';
        fill.innerText = pct + '%';
      }
    });
    xhr.onload = function() {
      dom('upload-cancel-btn').disabled = false;
      try {
        var data = JSON.parse(xhr.responseText);
        if (data.ok) {
          showFeedback('Uploaded ' + files.length + ' file(s)', 'success');
          dom('modal-upload').classList.remove('show');
          navigateTo(currentPath);
        } else {
          showFeedback('Upload error: ' + (data.error || 'Failed'), 'error');
          dom('upload-confirm-btn').disabled = false;
        }
      } catch (e) {
        showFeedback('Upload error: Invalid response', 'error');
        dom('upload-confirm-btn').disabled = false;
      }
    };
    xhr.onerror = function() {
      dom('upload-cancel-btn').disabled = false;
      dom('upload-confirm-btn').disabled = false;
      showFeedback('Upload failed: Network error', 'error');
    };
    xhr.send(formData);
  });

  dom('upload-cancel-btn').addEventListener('click', function() {
    dom('modal-upload').classList.remove('show');
  });

  /* =========================================================
     EXTRACT-TO FOLDER PICKER
  ========================================================= */
  function openExtractToModal(zipName) {
    extractTargetZip = zipName;
    selectedExtractDest = currentPath;
    // Load top-level dirs for picker
    apiCall({ action: 'list', path: '/' }, function(err, data) {
      var picker = dom('folder-picker');
      picker.innerHTML = '';
      var paths = ['/'];
      if (!err && data.ok) {
        data.items.forEach(function(it) {
          if (it.type === 'dir') paths.push('/' + it.name);
        });
      }
      // Also add current path if not already
      if (paths.indexOf(currentPath) < 0) paths.push(currentPath);
      paths.forEach(function(p) {
        var d = document.createElement('div');
        d.className = 'folder-picker-item' + (p === selectedExtractDest ? ' selected-folder' : '');
        d.innerHTML = '<i class="fas fa-folder"></i> ' + escHtml(p);
        d.addEventListener('click', function() {
          selectedExtractDest = p;
          picker.querySelectorAll('.folder-picker-item').forEach(function(el) { el.classList.remove('selected-folder'); });
          d.classList.add('selected-folder');
        });
        picker.appendChild(d);
      });
    });
    dom('modal-extract-to').classList.add('show');
  }

  dom('extract-to-cancel').addEventListener('click', function() {
    dom('modal-extract-to').classList.remove('show');
  });
  dom('extract-to-ok').addEventListener('click', function() {
    dom('modal-extract-to').classList.remove('show');
    if (extractTargetZip && selectedExtractDest) {
      doExtractTo(extractTargetZip, selectedExtractDest);
    }
  });

  /* =========================================================
     CONTEXT MENUS
  ========================================================= */
  function hideCtx() {
    document.querySelectorAll('.contextmenu').forEach(function(m) { m.style.display = 'none'; });
  }

  function showCtx(menu, x, y) {
    hideCtx();
    if (!menu) return;
    menu.style.display = 'block';
    menu.style.left = x + 'px';
    menu.style.top = y + 'px';
    var r = menu.getBoundingClientRect();
    if (r.right > innerWidth) menu.style.left = (x - r.width) + 'px';
    if (r.bottom > innerHeight) menu.style.top = (y - r.height) + 'px';
  }

  document.addEventListener('click', function(e) {
    if (!e.target.closest('.contextmenu')) hideCtx();
    if (!e.target.closest('.zip-dropdown')) dom('zip-menu').classList.remove('show');
  });

  // Context menu for blank area in tbody
  dom('tbody').addEventListener('contextmenu', function(e) {
    var tr = e.target.closest('tr[data-name]');
    if (!tr) {
      // Only show dir context menu if clicking on empty area (not on a row)
      e.preventDefault();
      ctxTargetName = null;
      ctxTargetIsZip = false;
      unselectAll();
      showCtx(dom('ctx-dir'), e.clientX, e.clientY);
    }
    // Row-specific contextmenu is handled per-row in render()
  });

  // Context menu: file actions
dom('btn-modal-copy').addEventListener('click', function() {
    var names = selectedItems.size > 0 ? Array.from(selectedItems) : (ctxTargetName ? [ctxTargetName] : []);
    if (names.length) doCopy(names);
    hideCtx();
});
dom('btn-modal-cut').addEventListener('click', function() {
    var names = selectedItems.size > 0 ? Array.from(selectedItems) : (ctxTargetName ? [ctxTargetName] : []);
    if (names.length) doCut(names);
    hideCtx();
});
  dom('btn-modal-rename').addEventListener('click', function() {
    var s = ctxTargetName || getOneSelected();
    if (s) {
      showPrompt('Rename', 'New name for "' + s + '":', s, function(newName) {
        doRename(s, newName);
      });
    }
    hideCtx();
  });
  dom('btn-modal-del').addEventListener('click', function() {
    var names = selectedItems.size > 0 ? Array.from(selectedItems) : (ctxTargetName ? [ctxTargetName] : []);
    if (!names.length) { hideCtx(); return; }
    showConfirm('Delete', 'Delete ' + names.length + ' item(s)? This cannot be undone.', function() {
      doDelete(names);
    });
    hideCtx();
  });
  dom('btn-ctx-zip-folder').addEventListener('click', function() {
    var names = selectedItems.size > 0 ? Array.from(selectedItems) : (ctxTargetName ? [ctxTargetName] : []);
    if (!names.length) { hideCtx(); return; }
    var defaultName = names.length === 1 ? names[0].replace(/\.[^.]+$/, '') + '.zip' : 'archive.zip';
    showPrompt('Create ZIP', 'ZIP archive name:', defaultName, function(zipName) {
      if (!zipName.endsWith('.zip')) zipName += '.zip';
      apiCall({
        action: 'zip',
        body: JSON.stringify({ path: currentPath, names: names, zipName: zipName })
      }, function(err, data) {
        if (err || !data.ok) { showFeedback('ZIP error: ' + (data && data.error ? data.error : (err || 'Failed')), 'error'); return; }
        showFeedback('Created: ' + zipName, 'success');
        navigateTo(currentPath);
      });
    });
    hideCtx();
  });
  dom('btn-ctx-extract-here').addEventListener('click', function() {
    if (ctxTargetName) doExtractHere(ctxTargetName);
    hideCtx();
  });
  dom('btn-ctx-extract-to').addEventListener('click', function() {
    if (ctxTargetName) openExtractToModal(ctxTargetName);
    hideCtx();
  });
  dom('btn-ctx-properties').addEventListener('click', function() {
    var s = ctxTargetName || getOneSelected();
    if (s) doShowProperties(s);
    hideCtx();
  });

  // Context menu: blank area
  dom('btn-modal-mkdir').addEventListener('click', function() {
    showPrompt('New Folder', 'Folder name:', '', function(n) { doCreateDir(n); });
    hideCtx();
  });
  dom('btn-modal-mkfile').addEventListener('click', function() {
    showPrompt('New File', 'File name:', '', function(n) { doCreateFile(n); });
    hideCtx();
  });
  dom('btn-modal-paste').addEventListener('click', function() { doPaste(); hideCtx(); });
  dom('btn-import-zip-here').addEventListener('click', function() {
    openUploadModal();
    hideCtx();
  });

  /* =========================================================
     TOOLBAR BUTTONS
  ========================================================= */
  dom('btn-home').addEventListener('click', function() { navigateTo('/'); });
  dom('btn-up').addEventListener('click', function() { navigateUp(); });
  dom('btn-upload').addEventListener('click', function() { openUploadModal(); });
  dom('btn-newdir').addEventListener('click', function() {
    showPrompt('New Folder', 'Folder name:', '', function(n) { doCreateDir(n); });
  });
  dom('btn-newfile').addEventListener('click', function() {
    showPrompt('New File', 'File name:', '', function(n) { doCreateFile(n); });
  });
  dom('btn-del').addEventListener('click', function() {
    var names = Array.from(selectedItems);
    if (!names.length) { showFeedback('Select items first', 'error'); return; }
    showConfirm('Delete', 'Delete ' + names.length + ' item(s)? This cannot be undone.', function() { doDelete(names); });
  });
  dom('btn-rename').addEventListener('click', function() {
    var s = getOneSelected();
    if (s) showPrompt('Rename', 'New name for "' + s + '":', s, function(n) { doRename(s, n); });
    else showFeedback('Select exactly one item to rename', 'error');
  });

dom('btn-copy').addEventListener('click', function() {
    var names = Array.from(selectedItems);
    if (!names.length) { showFeedback('Select item(s) to copy', 'error'); return; }
    doCopy(names);
});
dom('btn-cut').addEventListener('click', function() {
    var names = Array.from(selectedItems);
    if (!names.length) { showFeedback('Select item(s) to cut', 'error'); return; }
    doCut(names);
});
  dom('btn-paste').addEventListener('click', function() { doPaste(); });
  dom('btn-select-all').addEventListener('click', function() {
    filterHidden(currentItems).forEach(function(it) { selectedItems.add(it.name); });
    updateSelectionHighlight();
    updateToolbarBySelection();
    updateStatus();
    syncSelectAllCheckbox();
    // Force update all checkboxes immediately
    document.querySelectorAll('#tbody .row-chk').forEach(function(chk) {
      chk.checked = selectedItems.has(chk.dataset.name);
    });
    showFeedback('Selected ' + selectedItems.size + ' items', 'info');
  });
  dom('btn-clear-sel').addEventListener('click', function() { unselectAll(); showFeedback('Selection cleared', 'info'); });
  dom('btn-hidden-toggle').addEventListener('click', function() {
    showHidden = !showHidden;
    dom('btn-hidden-toggle').innerHTML = showHidden
      ? '<i class="fas fa-eye"></i> <span class="btn-label">Hidden</span>'
      : '<i class="fas fa-eye-slash"></i> <span class="btn-label">Hidden</span>';
    render();
    showFeedback(showHidden ? 'Showing hidden files' : 'Hiding hidden files', 'info');
  });
  dom('btn-disk-usage').addEventListener('click', function() {
    apiCall({ action: 'diskusage', path: currentPath }, function(err, data) {
      if (err || !data.ok) { showFeedback('Could not get disk usage', 'error'); return; }
      showFeedback('Disk usage: ' + (data.human || data.bytes + ' B'), 'info');
    });
  });
  dom('btn-refresh').addEventListener('click', function() { navigateTo(currentPath); });

  /* ===== ZIP dropdown ===== */
  dom('btn-zip-trigger').addEventListener('click', function(e) {
    e.stopPropagation();
    var menu = dom('zip-menu');
    var btn = dom('btn-zip-trigger');
    var rect = btn.getBoundingClientRect();
    menu.style.top = rect.bottom + 'px';
    menu.style.left = rect.left + 'px';
    menu.classList.toggle('show');
  });
  dom('export-zip').addEventListener('click', function() {
    dom('zip-menu').classList.remove('show');
    apiCall({ action: 'exportzip', path: currentPath }, function(err, data) {
      if (err || !data.ok) { showFeedback('Export error: ' + (data && data.error ? data.error : 'Failed'), 'error'); return; }
      // Download via link
      var a = document.createElement('a');
      a.href = API + '?action=download&token=' + data.token;
      a.download = data.zipName || 'export.zip';
      a.click();
      showFeedback('ZIP export ready', 'success');
    });
  });
  dom('import-zip').addEventListener('click', function() {
    dom('zip-menu').classList.remove('show');
    openUploadModal();
  });
  dom('zip-selected').addEventListener('click', function() {
    dom('zip-menu').classList.remove('show');
    doZipSelected();
  });

  /* ===== Address bar ===== */
  dom('path-btn').addEventListener('click', function() { navigateTo(dom('path-input').value); });
  dom('path-input').addEventListener('keydown', function(e) { if (e.key === 'Enter') navigateTo(this.value); });

  dom('web-url').addEventListener('click', function() { window.location.href = dom('path-input').value; });

  /* ===== Sort columns ===== */
  document.querySelectorAll('thead th[data-col]').forEach(function(th) {
    th.addEventListener('click', function() {
      var c = th.dataset.col;
      if (sortCol === c) sortAsc = !sortAsc;
      else { sortCol = c; sortAsc = true; }
      render();
    });
  });

  /* ===== Select-all checkbox ===== */
  dom('chk-all').addEventListener('change', function() {
    if (this.checked) {
      filterHidden(currentItems).forEach(function(it) { selectedItems.add(it.name); });
    } else {
      unselectAll();
    }
    updateSelectionHighlight();
    updateToolbarBySelection();
    updateStatus();
    // Force update all checkboxes immediately
    document.querySelectorAll('#tbody .row-chk').forEach(function(chk) {
      chk.checked = selectedItems.has(chk.dataset.name);
    });
  });

  /* ===== Properties modal close ===== */
  dom('prop-close-btn').addEventListener('click', function() { dom('modal-properties').classList.remove('show'); });

  /* ===== Bottom nav ===== */
  dom('nav-home').addEventListener('click', function() { navigateTo('/'); });
  dom('nav-up').addEventListener('click', function() { navigateUp(); });
  dom('nav-select-all').addEventListener('click', function() { dom('btn-select-all').click(); });
  dom('nav-new-folder').addEventListener('click', function() { dom('btn-newdir').click(); });
  dom('nav-upload').addEventListener('click', function() { openUploadModal(); });

  /* =========================================================
     LOGOUT
  ========================================================= */
  window.doLogout = function() {
    fetch('/ampdroid/file-manager', {
      method: 'GET',
      headers: { 'Authorization': 'Basic logout:logout' }
    }).finally(function() {
      window.location.href = '/';
    });
  };

  /* =========================================================
     INIT
  ========================================================= */
  navigateTo('/');

})();