"use client";

import ProductionJobPanel from './ProductionJobPanel';
import DeliveryPhotoPanel from './DeliveryPhotoPanel';
import { RoleActionBar } from './WorkflowUI';
import { useEffect, useState } from 'react';
import { api } from '@/lib/api';
import { staffMutation } from '@/lib/staff';

type Flow = { state: string; revision: number; owner: boolean; printer: boolean; delivery: boolean; closed: boolean; issue_open: boolean; productionStage?: string; productionHold?: string | null; delivered_at?: string };
type Step = { title: string; state: 'complete' | 'current' | 'blocked' | 'pending'; detail: string };
const names: Record<string, string> = { NOT_STARTED: 'Awaiting owner production release', PRODUCTION: 'In production', READY: 'Ready for dispatch', IN_TRANSIT: 'In transit', DELIVERED: 'Delivered · 48-hour review', COMPLETED: 'Completed' };

function deliverySteps(data: Flow): Step[] {
  const f = data.state;
  const released = f !== 'NOT_STARTED';
  const productionDone = ['READY', 'IN_TRANSIT', 'DELIVERED', 'COMPLETED'].includes(f);
  const pickupDone = ['IN_TRANSIT', 'DELIVERED', 'COMPLETED'].includes(f);
  const dispatched = pickupDone;
  const delivered = ['DELIVERED', 'COMPLETED'].includes(f);
  const completed = f === 'COMPLETED';
  let productionDetail = !released ? 'Awaiting an authorized owner release.' : productionDone ? 'Production and quality handoff confirmed.' : data.productionHold ? 'On hold: ' + data.productionHold : data.productionStage === 'QC' ? 'Quality check is pending.' : 'Production underway · ' + (data.productionStage || 'queue stage pending');
  return [
    { title: 'Production ready', state: productionDone ? 'complete' : data.productionHold ? 'blocked' : released ? 'current' : 'pending', detail: productionDetail },
    { title: 'Pickup evidence', state: pickupDone ? 'complete' : f === 'READY' ? 'current' : 'pending', detail: pickupDone ? 'Dispatch was confirmed after the server readiness checks.' : f === 'READY' ? 'Check the scanned pickup evidence and dispatch checklist below.' : 'A scanned pickup photo is checked when dispatch is ready.' },
    { title: 'Dispatch', state: dispatched ? 'complete' : f === 'READY' && pickupDone ? 'current' : 'pending', detail: dispatched ? 'The server confirmed dispatch.' : f === 'READY' ? 'Payment, invoice, production and evidence requirements are rechecked by the server.' : 'Available after production is ready.' },
    { title: 'Transit', state: f === 'IN_TRANSIT' ? 'current' : delivered ? 'complete' : 'pending', detail: f === 'IN_TRANSIT' ? 'Work is confirmed in transit. This page does not provide GPS tracking.' : delivered ? 'Delivery confirmation has been recorded.' : 'Shown after dispatch is confirmed.' },
    { title: 'Delivery evidence', state: delivered ? 'complete' : f === 'IN_TRANSIT' ? 'current' : 'pending', detail: delivered ? 'The delivery milestone was confirmed with the applicable evidence policy.' : f === 'IN_TRANSIT' ? 'Upload and scan any required delivery photo before confirmation.' : 'Available after dispatch.' },
    { title: 'Customer review', state: completed ? 'complete' : f === 'DELIVERED' ? 'current' : 'pending', detail: completed ? 'The server has confirmed completion.' : f === 'DELIVERED' ? data.issue_open ? 'An issue is open; automatic completion is paused.' : data.delivered_at ? 'Review period ends ' + new Date(new Date(data.delivered_at).getTime() + 48 * 3600000).toLocaleString('en-IN') + '.' : 'Delivered; review timing is unavailable.' : 'The review window begins after confirmed delivery.' },
    { title: 'Completed', state: completed ? 'complete' : 'pending', detail: completed ? 'Completed by the server after the review policy.' : 'Completion is recorded by the server; this screen cannot complete work.' }
  ];
}

export default function FulfilmentPanel({ leadId, customer = false, onChanged }: { leadId: string; customer?: boolean; onChanged?: () => Promise<void> }) {
  const [data, setData] = useState<Flow | null>(null), [note, setNote] = useState(''), [confirmed, setConfirmed] = useState(false), [busy, setBusy] = useState(false), [error, setError] = useState(''), [message, setMessage] = useState('');
  const base = '/v1/design/enquiries/' + leadId + '/fulfilment';
  async function load(clearError = true) { setData(await api<Flow>(base)); if (clearError) setError(''); }
  async function changed() { await load(); await onChanged?.(); }
  useEffect(() => { load().catch(() => setError('Progress unavailable. Refresh to retry.')); }, [base]);
  async function act(action: string) {
    if (!data) return;
    setBusy(true); setError(''); setMessage('');
    try { setData(await staffMutation<Flow>(base, 'POST', { revision: data.revision, action, note, confirmed })); setNote(''); setConfirmed(false); await onChanged?.(); setMessage('Progress saved. The server confirmed the milestone.'); }
    catch (e) { setConfirmed(false); setError(e instanceof Error ? e.message : 'Could not save this stage.'); await load(false).catch(() => {}); }
    finally { setBusy(false); }
  }
  const actions = data && !data.closed ? (data.state === 'NOT_STARTED' && data.owner ? [['PRODUCTION', 'Start production']] : data.state === 'READY' && (data.owner || data.delivery) ? [['IN_TRANSIT', 'Confirm dispatch']] : data.state === 'IN_TRANSIT' && (data.owner || data.delivery) ? [['DELIVERED', 'Confirm delivered']] : data.state === 'DELIVERED' && data.issue_open && data.owner ? [['RESOLVE', 'Resolve issue and restart review']] : data.state === 'DELIVERED' && !data.issue_open && (customer || data.owner) ? [['ISSUE', 'Report delivery issue']] : []) : [];
  return <section className="panel fulfilment-panel" aria-label="Production and delivery">
    <div className="staff-toolbar"><div><p className="overline">ORDER HANDOFF</p><h3>Production & delivery</h3></div><button className="text-button" onClick={() => load().catch(() => setError('Could not refresh progress.'))}>Refresh delivery progress</button></div>
    {data && <>
      <div className="fulfilment-current"><span className="overline">CURRENT SERVER STAGE</span><strong>{names[data.state] || data.state}</strong>{data.state === 'PRODUCTION' && data.productionStage && <span className={'queue-pill' + (data.productionHold ? ' hold' : '')}>{data.productionHold ? 'On hold' : data.productionStage === 'QC' ? 'Quality check pending' : 'Production · ' + data.productionStage.toLowerCase()}</span>}</div>
      <ol className="fulfilment-stepper" aria-label="Order delivery steps">{deliverySteps(data).map((step, index) => <li key={step.title} className={'fulfilment-step step-' + step.state} data-state={step.state} aria-current={step.state === 'current' ? 'step' : undefined}><span className="fulfilment-step-marker">{step.state === 'complete' ? '✓' : String(index + 1).padStart(2, '0')}</span><div><strong>{step.title}</strong><small>{step.detail}</small></div><em>{step.state === 'complete' ? 'Complete' : step.state === 'current' ? 'Current' : step.state === 'blocked' ? 'Blocked' : 'Pending'}</em></li>)}</ol>
      {data.state === 'PRODUCTION' && (data.owner || data.printer) && <ProductionJobPanel leadId={leadId} onChanged={changed} />}
      {data.delivered_at && <p className="delivery-confirmed-time">Delivered {new Date(data.delivered_at).toLocaleString('en-IN')}. The 48-hour review period starts from this server-confirmed time.</p>}
      {data.issue_open && <p className="production-hold" role="status">Delivery issue open. Automatic completion is paused until the owner resolves it.</p>}
      {data.state !== 'NOT_STARTED' && <DeliveryPhotoPanel key={data.state} leadId={leadId} onChanged={changed} />}
      {actions.length > 0 && <div className="settings-form fulfilment-action"><h4>{actions.some(([action]) => action === 'IN_TRANSIT') ? 'Confirm dispatch' : actions.some(([action]) => action === 'DELIVERED') ? 'Confirm delivery' : 'Delivery review action'}</h4><p className="hint">Before confirming, review the production, invoice, payment and scanned evidence checks shown above. The server validates the current revision and all applicable rules.</p><label className="field">Customer-visible progress note<textarea minLength={10} maxLength={500} required value={note} onChange={e => { setNote(e.target.value); setConfirmed(false); }} /></label><label className="quote-confirm"><input type="checkbox" checked={confirmed} onChange={e => setConfirmed(e.target.checked)} /> I confirm this action reflects the actual work and the note is suitable for the customer.</label><RoleActionBar>{actions.map(([action, label]) => <button key={action} className="btn btn-ink" disabled={busy || !confirmed || note.trim().length < 10} onClick={() => act(action)}>{busy ? 'Saving…' : label}</button>)}</RoleActionBar></div>}
      {!customer && data.state === 'NOT_STARTED' && <p className="hint">Owner release requires the accepted quotation, exact clean approved proof and verified advance under current payment rules. Dispatch also requires the current invoice and balance or valid approved credit. Task assignment alone does not advance work.</p>}
      <p className="hint">Milestones record staff confirmations. Photo times show when files were uploaded; GPS tracking is not enabled. Progress and completion are recorded by the server.</p>
    </>}
    {!data && !error && <p role="status">Loading delivery progress…</p>}{message && <p role="status">{message}</p>}{error && <p role="alert">{error}</p>}
  </section>;
}
