import test from 'node:test';
import assert from 'node:assert/strict';
import {hospitalCityOptions} from '../src/components/hospital-profile.js';
test('hospital regions reuse existing IDs and show ancestry without modifying address text',()=>{
 const options=hospitalCityOptions([{cityId:1,cityName:'北京市',belongCityCityId:0},{cityId:2,cityName:'市辖区',belongCityCityId:1},{cityId:4,cityName:'海淀区',belongCityCityId:2}],4);
 assert.deepEqual(options.find(row=>row.value===4),{value:4,label:'北京市 / 市辖区 / 海淀区'});assert.equal(options[0].value,0);
});
test('historical selected missing region remains selectable; malformed cycles terminate',()=>{
 const options=hospitalCityOptions([{cityId:1,cityName:'A',belongCityCityId:2},{cityId:2,cityName:'B',belongCityCityId:1}],99);
 assert.equal(options.length,4);assert.ok(options.some(row=>row.value===99));assert.ok(hospitalCityOptions(null,0).length===1);
});
