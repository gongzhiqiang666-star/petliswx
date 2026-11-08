import test from 'node:test';
import assert from 'node:assert/strict';
import {mainOrderValues} from '../src/components/legacy-main-order.js';
test('main order form keeps original IDs/text and shows legacy unset dates without exposing financial write fields',()=>{
 const values=mainOrderValues({InfoHospitalId:12,AnimalName:' 原名称 ',CollectDt:'1900-01-01 00:00:00',ReceiveSampleDt:'2026-11-08T12:30:00',TestFee:100,IsPayMoney:1,editToken:'private-token'});
 assert.equal(values.InfoHospitalId,'12');assert.equal(values.AnimalName,' 原名称 ');assert.equal(values.CollectDt,'');assert.equal(values.ReceiveSampleDt,'2026-11-08 12:30:00');assert.equal(values.TestFee,undefined);assert.equal(values.IsPayMoney,undefined);assert.equal(values.editToken,undefined);
});
