/* Welcome dashboard: live uptime ticker + periodic stats refresh via /welcome. */
(function () {
    function pad(n) { return String(n); }

    function formatUptime(ms) {
        if (ms < 0) ms = 0;
        const totalSec = Math.floor(ms / 1000);
        const days = Math.floor(totalSec / 86400);
        const hours = Math.floor((totalSec % 86400) / 3600);
        const minutes = Math.floor((totalSec % 3600) / 60);
        const seconds = totalSec % 60;
        return pad(days) + 'd ' + pad(hours) + 'h ' + pad(minutes) + 'm ' + pad(seconds) + 's';
    }

    function setByKey(key, value) {
        document.querySelectorAll('[data-key="' + key + '"]').forEach(function (el) {
            const textNode = el.querySelector('.value-text') || el;
            textNode.textContent = value;
        });
    }

    // ---- Live uptime ticker (no network needed) ----
    const root = document.getElementById('dashboard');
    const startEpoch = root ? parseInt(root.getAttribute('data-start-epoch'), 10) : NaN;
    if (!isNaN(startEpoch)) {
        const tick = function () { setByKey('Uptime', formatUptime(Date.now() - startEpoch)); };
        tick();
        setInterval(tick, 1000);
    }

    // ---- Periodic stats refresh ----
    function refreshStats() {
        fetch('/welcome', { headers: { 'Accept': 'application/json' }, credentials: 'same-origin' })
            .then(function (r) { return r.ok ? r.json() : Promise.reject(r.status); })
            .then(function (data) {
                if (data.runtime) {
                    setByKey('Total Memory', data.runtime.totalMemory);
                    setByKey('Free Memory', data.runtime.freeMemory);
                    setByKey('Max Memory', data.runtime.maxMemory);
                    setByKey('Available Processors', data.runtime.availableProcessors);
                    updateMemoryBar(data.runtime);
                }
                const stamp = document.getElementById('last-updated');
                if (stamp && data.timestamp) { stamp.textContent = 'Last updated: ' + data.timestamp; }
            })
            .catch(function () { /* silently ignore transient errors */ });
    }

    function parseMb(s) {
        if (typeof s !== 'string') return NaN;
        return parseFloat(s.replace(/[^0-9.]/g, ''));
    }

    function updateMemoryBar(runtime) {
        const bar = document.getElementById('mem-bar-fill');
        const label = document.getElementById('mem-bar-label');
        if (!bar) return;
        const total = parseMb(runtime.totalMemory);
        const free = parseMb(runtime.freeMemory);
        if (isNaN(total) || isNaN(free) || total <= 0) return;
        const usedPct = Math.min(100, Math.max(0, Math.round(((total - free) / total) * 100)));
        bar.style.width = usedPct + '%';
        if (label) label.textContent = usedPct + '% used (' + Math.round(total - free) + ' / ' + Math.round(total) + ' MB)';
    }

    // Kick off once and then every 5 seconds.
    refreshStats();
    setInterval(refreshStats, 5000);

    // ---- Copy-to-clipboard on scalar cards ----
    document.querySelectorAll('.card.copyable').forEach(function (card) {
        card.addEventListener('click', function () {
            const valEl = card.querySelector('.value-text') || card.querySelector('.card-value');
            const text = valEl ? valEl.textContent.trim() : '';
            if (!text) return;
            navigator.clipboard.writeText(text).then(function () {
                const hint = card.querySelector('.copy-hint');
                if (hint) {
                    const original = hint.textContent;
                    hint.textContent = '✓ copied';
                    hint.style.opacity = '1';
                    setTimeout(function () { hint.textContent = original; hint.style.opacity = ''; }, 1200);
                }
            }).catch(function () { /* clipboard blocked; ignore */ });
        });
    });
})();
