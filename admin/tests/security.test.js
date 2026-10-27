import test from 'node:test';
import assert from 'node:assert/strict';
import { escapeHtml } from '../src/utils.js';
import { badge, field } from '../src/components/ui.js';

test('动态状态和表单值不能注入 HTML 属性', () => {
  const status = badge('ACTIVE"><img src=x onerror=alert(1)>');
  assert.ok(!status.includes('<img'));
  const input = field({ name: 'title', label: '标题', value: '"><script>alert(1)</script>' });
  assert.ok(!input.includes('<script>'));
  assert.ok(input.includes('&quot;&gt;&lt;script&gt;'));
});

test('XSS escaping 覆盖反引号模板中的服务文本', () => {
  assert.equal(escapeHtml('`><svg/onload=alert(1)>'), '`&gt;&lt;svg/onload=alert(1)&gt;');
});
