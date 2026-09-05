/* Shared theme handling: dark/light toggle persisted in localStorage. */
(function () {
    const STORAGE_KEY = 'helloapp-theme';

    function current() {
        return document.documentElement.getAttribute('data-theme') || 'light';
    }

    function apply(theme) {
        document.documentElement.setAttribute('data-theme', theme);
        try { localStorage.setItem(STORAGE_KEY, theme); } catch (e) { /* ignore */ }
        updateToggles(theme);
    }

    function updateToggles(theme) {
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            btn.textContent = theme === 'dark' ? '☀️' : '🌙';
            btn.setAttribute('title', theme === 'dark' ? 'Switch to light mode' : 'Switch to dark mode');
            btn.setAttribute('aria-label', btn.getAttribute('title'));
        });
    }

    function toggle() {
        apply(current() === 'dark' ? 'light' : 'dark');
    }

    // Wire up any toggle buttons once the DOM is ready.
    document.addEventListener('DOMContentLoaded', function () {
        updateToggles(current());
        document.querySelectorAll('[data-theme-toggle]').forEach(function (btn) {
            btn.addEventListener('click', toggle);
        });
    });

    window.HelloTheme = { apply: apply, toggle: toggle, current: current };
})();
