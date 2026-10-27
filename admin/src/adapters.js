import { yuanToCents } from './utils.js';

const nullable = (value) => value === '' || value === undefined ? null : value;
const numeric = (value) => value === '' || value === undefined || value === null ? null : Number(value);
const clean = (value) => String(value ?? '').trim();

export const reviewPayload = (form) => ({ approved: Boolean(form.approved), reason: clean(form.reason) });
export const orderReviewPayload = (form) => ({ decision: form.decision, reason: clean(form.reason) });
export function reviewRequest(resource, id, form) {
  const approved = form.approved === true || form.approved === 'true';
  if (resource === 'invoices') return approved
    ? { path: `/invoices/${id}/issue`, method: 'POST', data: { invoiceUrl: clean(form.invoiceUrl) } }
    : { path: `/invoices/${id}/reject`, method: 'POST', data: { reason: clean(form.reason) } };
  const paths = {
    orders: `/review/orders/${id}`,
    users: `/review/registrations/${id}`,
    profiles: `/review/profile-changes/${id}`
  };
  if (paths[resource]) return { path: paths[resource], method: 'POST', data: reviewPayload({ approved, reason: form.reason }) };
  return { path: `/admin/hospitals/${id}/review`, method: 'PATCH', data: reviewPayload({ approved, reason: form.reason }) };
}

export const rolePayload = (form) => ({
  roleCode: form.roleCode,
  hospitalId: numeric(form.hospitalId),
  laboratoryId: numeric(form.laboratoryId)
});
export const userStatusPayload = (form) => ({ active: form.active === true || form.active === 'true', reason: clean(form.reason) });
export const pickupPayload = (form) => ({
  trackingNo: nullable(clean(form.trackingNo)) || null,
  carrier: nullable(clean(form.carrier)) || null,
  courierId: numeric(form.courierId)
});
export const resultPayload = (rows) => ({
  results: rows.map((row) => ({
    orderItemId: Number(row.orderItemId), name: clean(row.name), value: clean(row.value),
    unit: clean(row.unit), referenceRange: clean(row.referenceRange),
    abnormalFlag: row.abnormalFlag === '' || row.abnormalFlag === undefined ? null : row.abnormalFlag === true || row.abnormalFlag === 'true'
  }))
});
export const reportPayload = (form) => ({ summary: clean(form.summary), conclusion: clean(form.conclusion) });

export function contractPayload(form) {
  const discount = Number(form.discount);
  if (!Number.isFinite(discount) || discount < 0 || discount > 100) throw new Error('折扣必须在 0 到 100 之间');
  return {
    hospitalId: Number(form.hospitalId), labId: Number(form.labId), name: clean(form.name),
    settlementType: form.settlementType, discountBps: Math.round(discount * 100),
    monthlyCreditLimitCents: yuanToCents(form.monthlyCreditLimit),
    effectiveFrom: form.effectiveFrom, effectiveTo: nullable(form.effectiveTo)
  };
}

export const billPayload = (form) => ({ hospitalId: Number(form.hospitalId), billingMonth: form.billingMonth, dueDate: form.dueDate });
export const invoiceProcessPayload = (form) => ({ action: form.action, reason: clean(form.reason) });
export function pointAdjustmentPayload(form, key) {
  const points = Number(form.points);
  if (!Number.isSafeInteger(points) || points === 0) throw new Error('积分必须为整数且不能为 0');
  return { data: { userId: Number(form.userId), points, reason: clean(form.reason) }, idempotencyKey: key };
}
export const giftPayload = (form) => ({
  name: clean(form.name), pointsCost: Number(form.pointsCost), stock: Number(form.stock),
  description: clean(form.description), imageUrl: nullable(clean(form.imageUrl)), status: form.status
});
export const articlePayload = (form) => ({
  categoryId: numeric(form.categoryId), title: clean(form.title), slug: clean(form.slug),
  summary: clean(form.summary), content: clean(form.content), coverUrl: nullable(clean(form.coverUrl)),
  contentType: form.contentType, liveStartAt: nullable(form.liveStartAt), liveEndAt: nullable(form.liveEndAt),
  popupEnabled: Boolean(form.popupEnabled), status: form.status
});
export const messagePayload = (form) => ({
  userId: Number(form.userId), title: clean(form.title), content: clean(form.content),
  channel: form.channel, messageType: form.messageType
});
export const configPayload = (form) => ({ value: clean(form.value), description: clean(form.description) });

const instant = (value) => value ? new Date(value).toISOString() : null;
export const couponTemplatePayload = (form) => ({
  code: clean(form.code), name: clean(form.name), couponType: form.couponType,
  valueCents: yuanToCents(form.value), minSpendCents: yuanToCents(form.minSpend),
  validFrom: instant(form.validFrom), validTo: instant(form.validTo),
  totalQuantity: Number(form.totalQuantity), status: form.status
});
