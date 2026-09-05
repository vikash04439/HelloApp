/* Employee Management UI — consumes the Employee REST API via fetch. */
(function () {
    'use strict';

    var state = { employees: [], filter: 'all', search: '' };

    // ------------------------------ helpers ------------------------------
    var $ = function (id) { return document.getElementById(id); };

    function escapeHtml(s) {
        if (s == null) return '';
        return String(s)
            .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;').replace(/'/g, '&#39;');
    }

    function initials(name) {
        if (!name) return '?';
        var parts = name.trim().split(/\s+/);
        return (parts[0][0] + (parts.length > 1 ? parts[parts.length - 1][0] : '')).toUpperCase();
    }

    function formatDate(iso) {
        if (!iso) return '—';
        var d = new Date(iso);
        if (isNaN(d.getTime())) return iso;
        return d.toLocaleDateString(undefined, { year: 'numeric', month: 'short', day: 'numeric' });
    }

    function toast(message, type) {
        var wrap = $('toasts');
        var el = document.createElement('div');
        el.className = 'toast ' + (type || 'info');
        var icon = type === 'success' ? '✅' : type === 'error' ? '⚠️' : 'ℹ️';
        el.innerHTML = '<span class="toast-icon">' + icon + '</span><span>' + escapeHtml(message) + '</span>';
        wrap.appendChild(el);
        setTimeout(function () {
            el.style.transition = 'opacity 0.3s, transform 0.3s';
            el.style.opacity = '0';
            el.style.transform = 'translateX(20px)';
            setTimeout(function () { el.remove(); }, 300);
        }, 3200);
    }

    function api(url, options) {
        options = options || {};
        options.headers = Object.assign({ 'Accept': 'application/json' }, options.headers || {});
        options.credentials = 'same-origin';
        return fetch(url, options).then(function (res) {
            if (res.status === 401 || res.status === 403) {
                toast('Not authenticated. Please refresh the page and sign in.', 'error');
                return Promise.reject(new Error('unauthorized'));
            }
            var isJson = (res.headers.get('content-type') || '').indexOf('application/json') !== -1;
            return (isJson ? res.json() : res.text().then(function () { return {}; }))
                .then(function (body) {
                    if (!res.ok) {
                        var msg = (body && body.message) ? body.message : ('Request failed (' + res.status + ')');
                        return Promise.reject(new Error(msg));
                    }
                    return body;
                });
        });
    }

    // ------------------------------ data loading ------------------------------
    function loadEmployees() {
        $('loading').style.display = '';
        $('empty').style.display = 'none';
        $('emp-tbody').innerHTML = '';

        Promise.all([
            api('/allemployee').catch(function () { return { data: [] }; }),
            api('/allemployee-notactive').catch(function () { return { data: [] }; })
        ]).then(function (results) {
            var active = (results[0] && results[0].data) || [];
            var inactive = (results[1] && results[1].data) || [];
            var byId = {};
            active.concat(inactive).forEach(function (e) { byId[e.id] = e; });
            state.employees = Object.keys(byId).map(function (k) { return byId[k]; })
                .sort(function (a, b) { return (a.id || 0) - (b.id || 0); });
            $('loading').style.display = 'none';
            render();
        }).catch(function (err) {
            $('loading').style.display = 'none';
            $('empty').style.display = '';
            $('empty-text').textContent = 'Could not load employees. ' + err.message;
        });
    }

    // ------------------------------ rendering ------------------------------
    function currentList() {
        var q = state.search.toLowerCase();
        return state.employees.filter(function (e) {
            if (state.filter === 'active' && !e.isActive) return false;
            if (state.filter === 'inactive' && e.isActive) return false;
            if (!q) return true;
            return [e.name, e.email, e.department].some(function (v) {
                return v && String(v).toLowerCase().indexOf(q) !== -1;
            });
        });
    }

    function render() {
        var total = state.employees.length;
        var active = state.employees.filter(function (e) { return e.isActive; }).length;
        $('stat-total').textContent = total;
        $('stat-active').textContent = active;
        $('stat-inactive').textContent = total - active;

        var list = currentList();
        var tbody = $('emp-tbody');
        if (list.length === 0) {
            tbody.innerHTML = '';
            $('empty').style.display = '';
            $('empty-text').textContent = total === 0
                ? 'No employees yet. Click “Add Employee” to create one.'
                : 'No employees match your filters.';
            return;
        }
        $('empty').style.display = 'none';

        tbody.innerHTML = list.map(function (e) {
            var badge = e.isActive
                ? '<span class="badge badge-active">Active</span>'
                : '<span class="badge badge-inactive">Inactive</span>';
            return '' +
                '<tr>' +
                    '<td data-label="Name"><div class="name-cell"><span class="avatar">' + escapeHtml(initials(e.name)) + '</span>' +
                        '<span class="cell-name">' + escapeHtml(e.name) + '</span></div></td>' +
                    '<td data-label="Email" class="cell-muted">' + escapeHtml(e.email) + '</td>' +
                    '<td data-label="Department">' + escapeHtml(e.department || '—') + '</td>' +
                    '<td data-label="Status">' + badge + '</td>' +
                    '<td data-label="Created" class="cell-muted">' + escapeHtml(formatDate(e.createdOn)) + '</td>' +
                    '<td data-label="Actions" class="cell-actions">' +
                        '<button class="btn btn-ghost btn-sm" data-edit="' + e.id + '" type="button">Edit</button>' +
                        '<button class="btn btn-danger btn-sm" data-del="' + e.id + '" type="button">Delete</button>' +
                    '</td>' +
                '</tr>';
        }).join('');

        tbody.querySelectorAll('[data-edit]').forEach(function (btn) {
            btn.addEventListener('click', function () { openModal('edit', findById(btn.getAttribute('data-edit'))); });
        });
        tbody.querySelectorAll('[data-del]').forEach(function (btn) {
            btn.addEventListener('click', function () { removeEmployee(findById(btn.getAttribute('data-del'))); });
        });
    }

    function findById(id) {
        return state.employees.filter(function (e) { return String(e.id) === String(id); })[0];
    }

    // ------------------------------ modal / form ------------------------------
    function openModal(mode, emp) {
        clearErrors();
        $('modal-title').textContent = mode === 'edit' ? 'Edit Employee' : 'Add Employee';
        $('f-id').value = mode === 'edit' && emp ? emp.id : '';
        $('f-name').value = emp ? (emp.name || '') : '';
        $('f-email').value = emp ? (emp.email || '') : '';
        $('f-department').value = emp ? (emp.department || '') : '';
        $('f-active').value = emp ? String(!!emp.isActive) : 'true';
        $('f-status').value = emp && emp.status ? emp.status : 'Y';
        $('modal').classList.add('open');
        setTimeout(function () { $('f-name').focus(); }, 50);
    }

    function closeModal() { $('modal').classList.remove('open'); }

    function clearErrors() {
        ['e-name', 'e-email', 'e-status'].forEach(function (id) { $(id).textContent = ''; });
    }

    function validate() {
        clearErrors();
        var ok = true;
        var name = $('f-name').value.trim();
        var email = $('f-email').value.trim();
        var status = $('f-status').value.trim();
        if (!name) { $('e-name').textContent = 'Name is required.'; ok = false; }
        if (!email) { $('e-email').textContent = 'Email is required.'; ok = false; }
        else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) { $('e-email').textContent = 'Enter a valid email.'; ok = false; }
        if (status.length > 1) { $('e-status').textContent = 'Status must be a single character.'; ok = false; }
        return ok;
    }

    function save() {
        if (!validate()) return;
        var id = $('f-id').value;
        var payload = {
            name: $('f-name').value.trim(),
            email: $('f-email').value.trim(),
            department: $('f-department').value.trim() || null,
            isActive: $('f-active').value === 'true',
            status: $('f-status').value.trim() || 'Y'
        };
        var btn = $('btn-save');
        btn.disabled = true;
        var req = id
            ? api('/employee/' + id, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) })
            : api('/addemployee', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(payload) });
        req.then(function () {
            toast(id ? 'Employee updated successfully.' : 'Employee created successfully.', 'success');
            closeModal();
            loadEmployees();
        }).catch(function (err) {
            toast(err.message || 'Save failed.', 'error');
        }).finally(function () { btn.disabled = false; });
    }

    function removeEmployee(emp) {
        if (!emp) return;
        if (!window.confirm('Delete "' + emp.name + '"? This cannot be undone.')) return;
        api('/employee/' + emp.id, { method: 'DELETE' })
            .then(function () { toast('Employee deleted.', 'success'); loadEmployees(); })
            .catch(function (err) { toast(err.message || 'Delete failed.', 'error'); });
    }

    // ------------------------------ wire up ------------------------------
    document.addEventListener('DOMContentLoaded', function () {
        $('btn-add').addEventListener('click', function () { openModal('add', null); });
        $('btn-refresh').addEventListener('click', loadEmployees);
        $('btn-save').addEventListener('click', save);
        $('btn-cancel').addEventListener('click', closeModal);
        $('modal-close').addEventListener('click', closeModal);
        $('modal').addEventListener('click', function (ev) { if (ev.target === $('modal')) closeModal(); });
        document.addEventListener('keydown', function (ev) { if (ev.key === 'Escape') closeModal(); });

        $('search').addEventListener('input', function (ev) { state.search = ev.target.value; render(); });

        $('filter').querySelectorAll('button').forEach(function (btn) {
            btn.addEventListener('click', function () {
                $('filter').querySelectorAll('button').forEach(function (b) { b.classList.remove('active'); });
                btn.classList.add('active');
                state.filter = btn.getAttribute('data-filter');
                render();
            });
        });

        loadEmployees();
    });
})();
