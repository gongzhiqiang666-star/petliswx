import test from 'node:test';import assert from 'node:assert/strict';
import {profileReviewSummary,profileReviewDetails} from '../src/components/profile-review.js';
test('资料审核显示修改前后医院姓名，不把原始JSON和头像外链当作页面内容',()=>{
 const row={previousData:JSON.stringify({nickname:'原姓名',hospitalId:1,hospitalName:'原医院'}),requestedData:JSON.stringify({nickname:'<script>x</script>',title:'新职称',hospitalId:2,avatar:'https://outside.example.test/a.png',qualificationFiles:['/files/qualifications/'+'a'.repeat(32)+'.pdf','https://outside.example.test/private.pdf']}),requestedHospitalName:'新医院'};
 const html=profileReviewDetails(row);for(const text of ['原姓名','原医院','新医院','新职称','跨医院','下载资质 1'])assert.ok(html.includes(text));assert.ok(!html.includes('<script>'));assert.ok(!html.includes('src='));assert.ok(!html.includes('outside.example.test'));assert.ok(profileReviewSummary(row).includes('&lt;script&gt;'));
});
test('损坏历史快照不导致审核列表崩溃或产生下载外链',()=>{const row={previousData:'broken',requestedData:'[]'};assert.equal(profileReviewSummary(row),'');assert.ok(!profileReviewDetails(row).includes('data-profile-qualification='));});
