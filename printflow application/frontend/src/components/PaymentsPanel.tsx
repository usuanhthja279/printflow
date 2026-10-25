"use client";

import { useEffect, useMemo, useState } from 'react';
import { api } from '@/lib/api';
import { staffMutation } from '@/lib/staff';
import Modal from './Modal';
import ProofPdf from './ProofPdf';
import { ReadinessChecklist } from './WorkflowUI';

type Policy = { revision: number; advance_paise: number; credit_due: string | null; reason: string };
type Receipt = { id: string; receipt_number: number; amount_paise: number; method: string; reference: string; paid_on: string; reversal_reason: string | null };
type Invoice = { id: string; quote_id: string; invoice_number: number; issued_on: string; total_paise: number };
type Finance = { quoteId: string; quoteVersion: number; totalPaise: number; paidPaise: number; balancePaise: number; overpaidPaise: number; stage: string; policy: Policy | null; policyCurrent: boolean; owner: boolean; customer: boolean; closed: boolean; released: boolean; receipts: Receipt[]; invoices: Invoice[] };
type CheckoutResult = { razorpay_order_id: string; razorpay_payment_id: string; razorpay_signature: string };
type CheckoutInstance = { open: () => void; on: (event: string, handler: () => void) => void };
declare global { interface Window { Razorpay?: new (options: Record<string, unknown>) => CheckoutInstance } }

const rupees = new Intl.NumberFormat('en-IN', { maximumFractionDigits: 0 });
const money = (value: number) => {
  if (!Number.isSafeInteger(value)) return '₹—';
  const whole = Math.floor(value / 100);
  const remainder = value % 100;
  return `₹${rupees.format(whole)}.${String(remainder).padStart(2, '0')}`;
};
const amountText = (value: number) => {
  if (!Number.isSafeInteger(value)) return '';
  return `${Math.floor(value / 100)}.${String(value % 100).padStart(2, '0')}`;
};
const paise = (s: string) => {
  if (!/^\d+(\.\d{1,2})?$/.test(s)) throw Error('Enter a rupee amount with at most two decimal places.');
  const [r, p = ''] = s.split('.');
  const n = Number(r) * 100 + Number(p.padEnd(2, '0'));
  if (!Number.isSafeInteger(n) || n > 100000000000) throw Error('Amount is too large.');
  return n;
};
const methodName: Record<string, string> = { BANK: 'Bank transfer', UPI: 'UPI', CASH: 'Cash', CARD: 'Card', CHEQUE: 'Cheque', RAZORPAY_TEST: 'Razorpay test checkout' };

export default function PaymentsPanel({ leadId, onChanged }: { leadId: string; onChanged?: () => void | Promise<void> }) {
  const base = '/v1/design/enquiries/' + leadId + '/payments';
  const [gateway, setGateway] = useState<{ configured: boolean; mode: string; webhooksConfigured: boolean } | null>(null);
  const [data, setData] = useState<Finance | null>(null);
  const [error, setError] = useState(''), [message, setMessage] = useState(''), [busy, setBusy] = useState(false);
  const [preview, setPreview] = useState<Invoice | null>(null);
  const [advance, setAdvance] = useState(''), [credit, setCredit] = useState(false), [due, setDue] = useState(''), [reason, setReason] = useState('');
  const [amount, setAmount] = useState(''), [method, setMethod] = useState('BANK'), [reference, setReference] = useState('');
  const [paidOn, setPaidOn] = useState(new Date().toLocaleDateString('en-CA', { timeZone: 'Asia/Kolkata' })), [verified, setVerified] = useState(false), [receiptId, setReceiptId] = useState('');
  const [reverseId, setReverseId] = useState(''), [reverseReason, setReverseReason] = useState('');

  async function load() {
    const [d, g] = await Promise.all([api<Finance>(base), api<{ configured: boolean; mode: string; webhooksConfigured: boolean }>(base + '/gateway')]);
    setGateway(g); setData(d);
    setAdvance(d.policyCurrent && d.policy ? amountText(d.policy.advance_paise) : '');
    setCredit(!!(d.policyCurrent && d.policy?.credit_due)); setDue(d.policyCurrent ? d.policy?.credit_due || '' : '');
    setReason(d.policyCurrent ? d.policy?.reason || '' : '');
  }
  useEffect(() => { setData(null); setReceiptId(crypto.randomUUID()); load().catch(() => setError('Payments unavailable. Refresh to retry.')); }, [base]);
  async function act(path: string, input: unknown, success: string) {
    setBusy(true); setError(''); setMessage('');
    try { setData(await staffMutation<Finance>(base + path, 'POST', input)); setMessage(success); await onChanged?.(); return true; }
    catch (e) { setError(e instanceof Error ? e.message : 'Could not save finances.'); return false; }
    finally { setBusy(false); }
  }
  async function checkout(purpose: string) {
    if (!data) return;
    setBusy(true); setError('');
    try {
      if (!window.Razorpay) await new Promise<void>((resolve, reject) => { const script = document.createElement('script'); script.src = 'https://checkout.razorpay.com/v1/checkout.js'; script.onload = () => resolve(); script.onerror = () => reject(Error('Could not load Razorpay checkout.')); document.head.appendChild(script); });
      if (!window.Razorpay) throw Error('Razorpay checkout unavailable.');
      const order = await staffMutation<{ keyId: string; orderId: string; amountPaise: number; currency: string }>(base + '/gateway/orders', 'POST', { quoteId: data.quoteId, purpose });
      const widget = new window.Razorpay({ key: order.keyId, order_id: order.orderId, amount: order.amountPaise, currency: order.currency, name: 'J.D.Printex', description: 'TEST ' + purpose.toLowerCase() + ' payment', handler: async (result: CheckoutResult) => { const ok = await act('/gateway/verify', { orderId: result.razorpay_order_id, paymentId: result.razorpay_payment_id, signature: result.razorpay_signature }, 'Razorpay test payment verified. No real money moved.'); if (ok) await load(); }, modal: { ondismiss: () => setBusy(false) } });
      widget.on('payment.failed', () => { setBusy(false); setError('Payment failed. No successful payment has been recorded.'); }); widget.open();
    } catch (e) { setBusy(false); setError(e instanceof Error ? e.message : 'Checkout failed.'); }
  }

  const currentInvoices = useMemo(() => data?.invoices.filter(invoice => invoice.quote_id === data.quoteId) || [], [data]);
  const currentInvoice = currentInvoices[0];
  const includesTestPayment = !!data?.receipts.some(receipt => receipt.method === 'RAZORPAY_TEST' && !receipt.reversal_reason);
  const validCredit = !!(data?.policyCurrent && data.policy?.credit_due && data.stage === 'Balance on approved credit');
  const nextAction = !data ? '' : data.overpaidPaise > 0 ? 'The owner needs to reconcile the overpayment.' : !data.policyCurrent ? 'The owner needs to confirm payment terms for this quotation.' : data.policy && data.paidPaise < data.policy.advance_paise ? data.customer ? 'Pay the agreed advance to unlock production review.' : 'Verify the agreed advance before production release.' : !currentInvoice ? data.owner ? 'Issue the commercial invoice for this quotation.' : 'The owner has not issued the current commercial invoice yet.' : data.balancePaise > 0 && !validCredit ? data.customer ? 'Pay the remaining balance before dispatch.' : 'Collect the balance or confirm valid approved credit before dispatch.' : 'Finance checks are satisfied for the current order agreement.';
  const readiness = data ? [
    { label: 'Payment agreement for quotation v' + data.quoteVersion, state: data.policyCurrent && data.policy ? 'ready' as const : 'blocked' as const, detail: data.policyCurrent && data.policy ? 'Agreement revision ' + data.policy.revision + ' is tied to the accepted quotation.' : 'Payment terms must match the current accepted quotation.' },
    { label: 'Required advance verified', state: data.overpaidPaise > 0 ? 'blocked' as const : data.policyCurrent && data.policy && data.paidPaise >= data.policy.advance_paise ? 'ready' as const : 'blocked' as const, detail: data.overpaidPaise > 0 ? 'Owner reconciliation is required before this can pass.' : data.policy ? money(data.policy.advance_paise) + ' required; verified ledger total is ' + money(data.paidPaise) + '.' : 'No current payment agreement is recorded.' },
    { label: 'Invoice for current quotation', state: currentInvoice ? 'ready' as const : 'blocked' as const, detail: currentInvoice ? 'Commercial invoice INV-' + currentInvoice.invoice_number + ' is issued.' : 'Issue the invoice for the accepted quotation before dispatch.' },
    { label: 'Balance settled or current credit approved', state: data.overpaidPaise > 0 ? 'blocked' as const : data.balancePaise === 0 || validCredit ? 'ready' as const : 'blocked' as const, detail: data.overpaidPaise > 0 ? 'Reconcile the overpayment first.' : validCredit ? 'Approved credit due ' + data.policy?.credit_due + '.' : data.balancePaise === 0 ? 'No balance remains.' : money(data.balancePaise) + ' remains before dispatch.' }
  ] : [];

  return <section className="panel payments-panel" id="work-payments" aria-label="Payments and invoices">
    <div className="staff-toolbar finance-heading"><div><p className="overline">ORDER FINANCE</p><h3>Payments & invoices</h3><p>Accepted quotation v{data?.quoteVersion ?? '—'} · secure project records</p></div><button className="text-button" onClick={() => load().catch(() => setError('Could not refresh payments.'))}>Refresh payments</button></div>
    {data && <>
      <div className="finance-summary" aria-label="Finance summary">
        <article className="finance-total"><span>Agreed order total</span><strong>{money(data.totalPaise)}</strong><small>Accepted quotation v{data.quoteVersion}</small></article>
        <article><span>{includesTestPayment ? 'Ledger payments · includes TEST' : 'Verified payments'}</span><strong>{money(data.paidPaise)}</strong><small>{includesTestPayment ? 'Includes simulated Razorpay TEST payments; no real money moved.' : 'Only confirmed ledger receipts count'}</small></article>
        <article><span>Balance outstanding</span><strong>{money(data.balancePaise)}</strong><small>{data.overpaidPaise > 0 ? 'Overpayment needs owner review' : validCredit ? 'Covered by approved credit until ' + data.policy?.credit_due : 'Due before dispatch unless valid credit is approved'}</small></article>
      </div>
      <div className="finance-next-action" role="status"><span className="overline">NEXT FINANCE ACTION</span><p>{nextAction}</p></div>
      {data.overpaidPaise > 0 && <p className="finance-alert" role="alert">Overpaid by {money(data.overpaidPaise)}. Production or dispatch remains subject to owner reconciliation.</p>}
      <div className="finance-details-grid">
        <section className="finance-card" aria-label="Payment agreement"><p className="overline">CURRENT AGREEMENT</p><h4>{data.policyCurrent && data.policy ? 'Payment schedule · revision ' + data.policy.revision : 'Payment terms pending'}</h4>{data.policyCurrent && data.policy ? <><dl><div><dt>Required advance</dt><dd>{money(data.policy.advance_paise)}</dd></div><div><dt>Balance terms</dt><dd>{validCredit ? 'Approved credit until ' + data.policy.credit_due : 'Balance due before dispatch'}</dd></div></dl><p>{data.policy.reason}</p></> : <p>Payment terms need to be confirmed for accepted quotation v{data.quoteVersion} before production can proceed.</p>}</section>
        <section className="finance-card finance-next-card"><p className="overline">ORDER STATUS</p><h4>{data.stage}</h4><p>{data.policyCurrent && data.policy ? 'Terms are linked to quotation v' + data.quoteVersion + '.' : 'The latest accepted quotation does not have matching terms.'}</p>{gateway?.configured && gateway.mode === 'TEST' && <span className="test-mode-badge">RAZORPAY TEST MODE · SIMULATED PAYMENT</span>}</section>
      </div>
      <ReadinessChecklist title="Finance readiness" items={readiness}/>
      <p className="hint">Finance checks are separate from exact proof approval and the owner’s production release. Server-side checks remain authoritative.</p>
      {data.owner && !data.closed && !data.released && !data.invoices.some(invoice => invoice.quote_id === data.quoteId) && <details className="workflow-history finance-disclosure"><summary>Configure advance and payment rules</summary><form className="settings-form" onSubmit={async e => { e.preventDefault(); try { await act('/policy', { quoteId: data.quoteId, expectedRevision: data.policy?.revision || 0, advancePaise: paise(advance), creditDue: credit ? due : null, reason }, 'Payment schedule saved and emails queued.'); } catch (e) { setError(e instanceof Error ? e.message : 'Invalid amount.'); } }}><label className="field">Required advance (INR)<input required inputMode="decimal" value={advance} onChange={e => setAdvance(e.target.value)} /></label><p className="hint">Enter the agreed amount. Zero explicitly allows production without an advance; explain that agreement below.</p><label className="quote-confirm"><input type="checkbox" checked={credit} onChange={e => setCredit(e.target.checked)} /> Owner approves balance credit beyond dispatch</label>{credit && <label className="field">Credit due date<input required type="date" value={due} onChange={e => setDue(e.target.value)} /></label>}<label className="field">Customer-visible payment agreement / exception reason<textarea required minLength={10} maxLength={1000} value={reason} onChange={e => setReason(e.target.value)} /></label><button className="btn btn-ink" disabled={busy}>Save payment rules</button></form></details>}
      {data.owner && data.balancePaise > 0 && <details className="workflow-history finance-disclosure"><summary>Record verified advance or balance payment</summary><form className="settings-form" onSubmit={async e => { e.preventDefault(); try { const ok = await act('/receipts', { id: receiptId, quoteId: data.quoteId, amountPaise: paise(amount), method, reference, paidOn, verified }, 'Verified receipt saved and emails queued.'); if (ok) { setReceiptId(crypto.randomUUID()); setAmount(''); setReference(''); setVerified(false); } } catch (e) { setError(e instanceof Error ? e.message : 'Invalid payment.'); } }}><p className="hint">Record only money received and cleared. This adds an auditable receipt; it does not initiate a refund or payment transfer.</p><label className="field">Amount received (INR)<input required inputMode="decimal" value={amount} onChange={e => { setAmount(e.target.value); setVerified(false); }} /></label><label className="field">Payment method<select value={method} onChange={e => setMethod(e.target.value)}>{['BANK', 'UPI', 'CASH', 'CARD', 'CHEQUE'].map(m => <option key={m} value={m}>{methodName[m]}</option>)}</select></label><label className="field">Transaction / receipt reference<input required minLength={3} maxLength={200} value={reference} onChange={e => setReference(e.target.value)} /></label><label className="field">Payment received on<input required type="date" value={paidOn} onChange={e => setPaidOn(e.target.value)} /></label><label className="quote-confirm"><input type="checkbox" checked={verified} onChange={e => setVerified(e.target.checked)} /> I verified that funds were actually received and cleared.</label><button className="btn btn-ink" disabled={busy || !verified}>Record verified payment</button></form></details>}
      <section className="finance-card payment-gateway" aria-label="Online test payment"><p className="overline">ONLINE CHECKOUT</p><h4>Razorpay {gateway?.mode || 'test'} mode</h4><p>{gateway?.configured ? 'TEST credentials are configured. Test checkout simulates payment; no real money moves.' : 'Razorpay test checkout is unavailable. The owner can still record verified offline payments.'}</p>{gateway?.configured && <><span className="test-mode-badge">TEST ONLY · NOT A LIVE PAYMENT</span><p className="hint">Successful checkout is verified by the server. Webhook status: {gateway.webhooksConfigured ? 'configured' : 'not configured; verified capture responses still update the ledger'}.</p></>}{data.customer && gateway?.configured && data.policyCurrent && data.balancePaise > 0 && !data.closed && <div className="row-actions">{data.policy && data.paidPaise < data.policy.advance_paise && <button className="btn btn-ink" disabled={busy} onClick={() => checkout('ADVANCE')}>Pay advance · TEST</button>}<button className="btn btn-outline" disabled={busy} onClick={() => checkout('BALANCE')}>Pay outstanding balance · TEST</button></div>}</section>
      <section className="finance-card finance-history" aria-label="Payment receipt history"><p className="overline">LEDGER</p><h4>Payment history</h4>{!data.receipts.length ? <p>No receipts recorded for this order.</p> : <ul className="payment-records">{data.receipts.map(r => <li key={r.id} className={r.reversal_reason ? 'receipt-corrected' : ''}><div className="receipt-heading"><strong>Receipt R-{r.receipt_number} · {money(r.amount_paise)}</strong><span className={r.reversal_reason ? 'receipt-state corrected' : 'receipt-state'}>{r.reversal_reason ? 'Corrected' : 'Verified'}</span></div><p>{r.paid_on} · {methodName[r.method] || r.method} · {r.reference}</p>{r.method === 'RAZORPAY_TEST' && <p className="test-mode-badge">TEST SIMULATION · no real money moved</p>}{r.reversal_reason && <p>Correction recorded: {r.reversal_reason}</p>}{data.owner && !r.reversal_reason && <button className="text-button" onClick={() => { setReverseId(r.id); setReverseReason(''); }}>Correct receipt R-{r.receipt_number}</button>}</li>)}</ul>}</section>
      <section className="finance-card finance-invoices" aria-label="Commercial invoices"><p className="overline">DOCUMENTS</p><h4>Commercial invoices</h4><p className="hint">These are commercial invoices based on accepted quotation pricing and entered tax; they are not GST tax invoices. Issued PDFs are immutable.</p>{data.owner && data.policyCurrent && !currentInvoice && <button className="btn btn-outline" disabled={busy} onClick={() => act('/invoices', { quoteId: data.quoteId }, 'Invoice issued and PDF emails queued.')}>Issue invoice</button>}{!data.invoices.length ? <p>No invoices issued yet.</p> : <ul className="invoice-list">{data.invoices.map(i => <li className="payment-invoice" key={i.id}><div><strong>INV-{i.invoice_number}</strong><span>{i.quote_id === data.quoteId ? 'Quotation v' + data.quoteVersion : 'Earlier accepted quotation'}</span><span>Issued {i.issued_on} · {money(i.total_paise)}</span></div><button className="btn btn-outline" onClick={() => setPreview(i)}>Preview</button><a className="btn btn-outline" href={'/api' + base + '/invoices/' + i.id + '/document'} download>Download PDF</a></li>)}</ul>}</section>
    </>}
    {error && <p role="alert">{error}</p>}{message && <p role="status">{message}</p>}
    {preview && <Modal layout="studio" title={'Invoice INV-' + preview.invoice_number} closeLabel="Close invoice preview" onClose={() => setPreview(null)}><ProofPdf src={'/api' + base + '/invoices/' + preview.id + '/document'} /></Modal>}
    {reverseId && <Modal title="Correct a receipt" onClose={() => setReverseId('')}><p>This preserves the original ledger record and excludes the amount from the recognized payment total. It does not refund money through a bank or payment provider. Corrections that would invalidate production or dispatch gates are blocked.</p><label className="field">Reason for receipt correction<textarea minLength={10} maxLength={1000} value={reverseReason} onChange={e => setReverseReason(e.target.value)} /></label><button className="btn btn-ink" disabled={busy || reverseReason.trim().length < 10} onClick={async () => { if (await act('/receipts/' + reverseId + '/reverse', { reason: reverseReason }, 'Receipt correction recorded and email queued.')) setReverseId(''); }}>Confirm receipt correction</button></Modal>}
  </section>;
}
