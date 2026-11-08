import {JSDOM} from 'jsdom';
import {createFidelityGate} from './browser-fidelity-gate.js';
const document=new JSDOM('<!doctype html><html><body></body></html>').window.document;
export const checkFidelity=createFidelityGate(document);
