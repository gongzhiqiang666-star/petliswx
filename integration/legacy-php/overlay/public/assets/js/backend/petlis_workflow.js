// PETLIS_LOCAL_WORKFLOW: keep drafts in the current DOM only; no clinical data in browser storage.
define([], function () {
    return { index: function () {
        if (!window.fetch || !window.FormData || !window.URLSearchParams || !window.AbortController) return;
        var pending = false;
        document.querySelectorAll('form[action="/laboklin.php/petlis_workflow/operate"]').forEach(function (form) {
            var notice = document.createElement('div');
            notice.setAttribute('role', 'status');
            notice.setAttribute('aria-live', 'polite');
            notice.style.marginTop = '12px';
            form.appendChild(notice);
            form.addEventListener('submit', async function (event) {
                event.preventDefault();
                if (pending) return;
                var body = new URLSearchParams(new FormData(form));
                var id = body.get('order_id');
                if (!/^[1-9][0-9]*$/.test(id || '')) return;
                pending = true;
                var buttons = Array.from(document.querySelectorAll('button[type="submit"]'));
                var disabled = buttons.map(function (button) { return button.disabled; });
                buttons.forEach(function (button) { button.disabled = true; });
                notice.textContent = '正在提交，请勿重复操作…';
                var controller = new AbortController();
                var timer = setTimeout(function () { controller.abort(); }, 35000);
                var retainLock = false;
                try {
                    var response = await fetch('/laboklin.php/petlis_workflow/operate', {
                        method: 'POST', body: body, credentials: 'same-origin', redirect: 'error',
                        headers: {'X-Requested-With': 'XMLHttpRequest'}, signal: controller.signal
                    });
                    if (!response.ok) throw new Error('unconfirmed');
                    var result = await response.json();
                    var expected = '/laboklin.php/petlis_workflow/index?id=' + id;
                    if (result.code === 1 && result.url === expected) {
                        retainLock = true;
                        window.location.assign(expected);
                        return;
                    }
                    if (result.code !== 0 || typeof result.message !== 'string') throw new Error('unconfirmed');
                    notice.textContent = result.message + '。本页填写内容已保留，请核对当前进度后再操作。';
                } catch (error) {
                    retainLock = true;
                    notice.textContent = '提交结果尚未确认，填写内容仍保留。本页不会自动重试，请先核对当前进度。';
                    var link = document.createElement('a');
                    link.href = '/laboklin.php/petlis_workflow/index?id=' + id;
                    link.target = '_blank';
                    link.rel = 'noopener';
                    link.textContent = ' 在新窗口核对进度';
                    notice.appendChild(link);
                } finally {
                    clearTimeout(timer);
                    // Unknown outcomes stay blocked until state is checked/refreshed; editable fields remain available to copy.
                    if (!retainLock) {
                        pending = false;
                        buttons.forEach(function (button, index) { button.disabled = disabled[index]; });
                    }
                }
            });
        });
    }, operate: function () {}, label: function () {}, report: function () {} };
});
