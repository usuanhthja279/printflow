"use client";
import {useEffect,useMemo,useState} from 'react';
import {api,PRODUCT_NAME} from '@/lib/api';
import Modal from './Modal';
import FulfilmentPanel from './FulfilmentPanel';
import StaffJobAssignments from './StaffJobAssignments';
import {ProductionJobCard,ReadinessChecklist} from './WorkflowUI';
import {matchesQueueSearch,printReference} from '@/lib/references';

type Job={id:string;public_id:string;product_code:string;quantity:number;stage:string;fulfilment:string;operator_name:string|null;operator_id?:string|null;machine:string|null;priority:string|null;due_date:string|null;hold_reason:string|null;rework_count?:number;payment_code?:string|null;payment_terms_current?:boolean;release_blockers?:string[];dispatch_blockers?:string[];current_proof_approved?:boolean;current_invoice_issued?:boolean;photos_required?:boolean;pickup_evidence_state?:string|null;quotation_amendment_open?:boolean};
type Board={owner:boolean;jobs:Job[]};
const columns=[['WAITING_RELEASE','Awaiting release'],['QUEUED','Queued'],['PRINTING','Printing'],['FINISHING','Finishing'],['QC','Quality check'],['READY','Ready / delivery']];
const filters=[['ALL','All jobs'],['WAITING_RELEASE','Awaiting release'],['UNASSIGNED','Unassigned'],['HOLD','On hold'],['QC','QC / rework'],['BLOCKED_DISPATCH','Dispatch blocked'],['URGENT','Urgent']];
function isDispatchBlocked(j:Job){return j.stage==='READY'&&!!j.dispatch_blockers?.length;}
function releaseReadiness(j:Job){
 const messages=j.release_blockers,blocked=new Set(messages||[]),unknown={state:'unknown' as const,detail:'Owner readiness data is unavailable. Refresh the queue.'};
 const item=(label:string,blocker:string,known:boolean,detail?:string)=>!messages||!known?{label,...unknown}:{label,state:blocked.has(blocker)?'blocked' as const:'ready' as const,detail:blocked.has(blocker)?blocker:detail};
 const paymentBlocker=['Confirm payment terms for the current quotation','Verify the agreed advance payment','Reconcile the recorded overpayment'].find(x=>blocked.has(x));
 return [item('Current quotation accepted','Accept a quotation',true),item('No quotation amendment pending','Resolve the open quotation amendment',j.quotation_amendment_open!==undefined),item('Latest clean proof approved for this quotation','Approve the latest clean proof for the current quotation',j.current_proof_approved!==undefined),!messages||j.payment_terms_current===undefined||j.payment_code===undefined?{label:'Current payment rules and advance verified',...unknown}:{label:'Current payment rules and advance verified',state:paymentBlocker?'blocked' as const:'ready' as const,detail:paymentBlocker||`Payment status: ${j.payment_code}`}];
}
function dispatchReadiness(j:Job){
 const messages=j.dispatch_blockers,blocked=new Set(messages||[]),unknown={state:'unknown' as const,detail:'Owner readiness data is unavailable. Refresh the queue.'};
 if(!messages)return [{label:'Payment terms and balance / credit',...unknown},{label:'Invoice for current quotation',...unknown},{label:'Pickup photo requirement',...unknown}];
 const paymentMessages=['Receive the balance or approve valid credit','Reconcile the recorded overpayment','Confirm payment terms for the current quotation','Verify the agreed advance payment'],payment=paymentMessages.find(x=>blocked.has(x));
 const paymentUnknown=j.payment_terms_current===undefined||j.payment_code===undefined;
 const photoMessage=Array.from(blocked).find(x=>/pickup photo/i.test(x));
 const photoKnown=j.photos_required!==undefined&&(!j.photos_required||j.pickup_evidence_state!==undefined);
 const photoState=!photoKnown?'unknown' as const:!j.photos_required||j.pickup_evidence_state==='CLEAN'?'ready' as const:'blocked' as const;
 return [{label:'Current payment agreement and balance / credit',state:paymentUnknown?'unknown' as const:payment?'blocked' as const:'ready' as const,detail:paymentUnknown?'Owner readiness data is unavailable. Refresh the queue.':payment||`Payment status: ${j.payment_code}`},{label:j.current_invoice_issued===false?'Invoice for current quotation':'Current quotation invoice issued',state:j.current_invoice_issued===undefined?'unknown' as const:j.current_invoice_issued?'ready' as const:'blocked' as const,detail:j.current_invoice_issued===undefined?'Invoice status is unavailable to this role.':j.current_invoice_issued?undefined:'Issue the invoice for the current quotation'},{label:'Pickup photo requirement',state:photoState,detail:!photoKnown?'Owner evidence policy is unavailable. Refresh the queue.':!j.photos_required?'No pickup photo required by policy.':j.pickup_evidence_state==='CLEAN'?'Pickup photo scan passed.':photoMessage||'Add a scanned pickup photo',statusText:photoState==='ready'&&!j.photos_required?'Waived':photoState==='ready'?'Scanned':undefined},...Array.from(blocked).filter(x=>![...paymentMessages,'Issue the invoice for the current quotation',photoMessage].includes(x)).map(label=>({label,state:'blocked' as const}))];
}

export default function ProductionBoard({initialFilter='ALL'}:{initialFilter?:string}){
 const [data,setData]=useState<Board|null>(null),[search,setSearch]=useState(''),[filter,setFilter]=useState(initialFilter),[lane,setLane]=useState('ALL'),[selected,setSelected]=useState<Job|null>(null),[error,setError]=useState('');
 async function load(){const fresh=await api<Board>('/v1/production/queue');setData(fresh);setSelected(current=>current?(fresh.jobs.find(job=>job.id===current.id)||null):null);setError('');}
 useEffect(()=>{load().catch(()=>setError('Production queue unavailable.'));},[]);
 useEffect(()=>{setFilter(initialFilter);},[initialFilter]);
 const searchable=useMemo(()=>data?.jobs||[],[data]);
 const searchResults=useMemo(()=>searchable.filter(j=>matchesQueueSearch(search,j.public_id,[PRODUCT_NAME(j.product_code),j.operator_name||'',j.machine||''])),[searchable,search]);
 const jobs=useMemo(()=>searchResults.filter(j=>{
  if(filter==='WAITING_RELEASE')return j.stage==='WAITING_RELEASE';
  if(filter==='UNASSIGNED')return j.stage!=='WAITING_RELEASE'&&!j.operator_id&&!j.operator_name;
  if(filter==='HOLD')return !!j.hold_reason;
  if(filter==='QC')return !j.hold_reason&&(j.stage==='QC'||(j.rework_count||0)>0);
  if(filter==='BLOCKED_DISPATCH')return isDispatchBlocked(j);
  if(filter==='URGENT')return j.priority==='URGENT';
  return true;
 }),[searchResults,filter]);
 const counts=useMemo(()=>({release:searchResults.filter(j=>j.stage==='WAITING_RELEASE').length,unassigned:searchResults.filter(j=>j.stage!=='WAITING_RELEASE'&&!j.operator_id&&!j.operator_name).length,hold:searchResults.filter(j=>!!j.hold_reason).length,qc:searchResults.filter(j=>!j.hold_reason&&(j.stage==='QC'||(j.rework_count||0)>0)).length,dispatch:searchResults.filter(isDispatchBlocked).length,urgent:searchResults.filter(j=>j.priority==='URGENT').length}),[searchResults]);
 function selectFilter(value:string){setFilter(value);setLane('ALL');}
 return <section className="panel production-board" aria-label="Production queue"><div className="staff-toolbar"><div><p className="overline">PRINT FLOOR / STAFF-CONFIRMED</p><h2>Production queue</h2></div><button className="btn btn-outline" onClick={()=>load().catch(()=>setError('Could not refresh production.'))}>Refresh production queue</button></div>
 <p className="hint">{data?.owner?'Release approved work and coordinate each handoff.':'Only jobs assigned to your printer account appear here.'} Latest 200 visible active projects; machine labels do not reserve capacity.</p>
 {data?.owner&&<nav className="production-exceptions" aria-label="Production exceptions">{filters.slice(1).map(([value,label])=><button key={value} type="button" className={filter===value?'active':''} aria-pressed={filter===value} onClick={()=>selectFilter(value)}><strong>{value==='WAITING_RELEASE'?counts.release:value==='UNASSIGNED'?counts.unassigned:value==='HOLD'?counts.hold:value==='QC'?counts.qc:value==='BLOCKED_DISPATCH'?counts.dispatch:counts.urgent}</strong><span>{label}</span></button>)}</nav>}
 <div className="queue-summary"><span><strong>{jobs.length}</strong> shown</span><span><strong>{counts.hold}</strong> on hold</span><span><strong>{counts.qc}</strong> QC / rework</span><span><strong>{counts.dispatch}</strong> dispatch blocked</span></div>
 <div className="catalogue-tools"><label className="catalogue-search">Search production jobs<input type="search" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Product, reference, printer or machine"/></label><label className="field">Production filter<select value={filter} onChange={e=>selectFilter(e.target.value)}>{filters.map(([value,label])=><option value={value} key={value}>{label}</option>)}</select></label></div>
 <nav className="mobile-production-lanes" aria-label="Choose production lane"><button type="button" aria-pressed={lane==='ALL'} onClick={()=>setLane('ALL')}>All <span>{jobs.length}</span></button>{columns.map(([stage,label])=><button key={stage} type="button" aria-pressed={lane===stage} onClick={()=>setLane(stage)}>{label}<span>{jobs.filter(j=>j.stage===stage).length}</span></button>)}</nav>
 <div className="production-lanes">{columns.map(([stage,label])=><section className={'production-lane lane-'+stage.toLowerCase()} key={stage} data-mobile-visible={lane==='ALL'||lane===stage} aria-label={label}><header><h3>{label}</h3><span>{jobs.filter(j=>j.stage===stage).length}</span></header><div className="production-lane-cards">{jobs.filter(j=>j.stage===stage).map(j=><ProductionJobCard key={j.id} reference={printReference(j.public_id)} product={PRODUCT_NAME(j.product_code)} quantity={j.quantity} operator={j.operator_name} machine={j.machine} due={j.due_date} hold={j.hold_reason} urgent={j.priority==='URGENT'} reworkCount={j.rework_count||0} dispatchBlocked={isDispatchBlocked(j)} onOpen={()=>setSelected(j)}/>)}{!jobs.some(j=>j.stage===stage)&&<p className="lane-empty">No jobs here</p>}</div></section>)}</div>
 {!data&&!error&&<p role="status">Loading production…</p>}{data&&!jobs.length&&!error&&<p className="production-empty" role="status">No jobs match this queue. Try another filter or search term.</p>}{error&&<p role="alert">{error}</p>}
 {selected&&<Modal layout="drawer" title="Production job" closeLabel="Close production job" onClose={()=>{setSelected(null);load().catch(()=>{});}}><header className="job-drawer-heading"><p className="overline">{printReference(selected.public_id)}</p><h2>{PRODUCT_NAME(selected.product_code)}</h2><p>{selected.quantity.toLocaleString('en-IN')} copies · {selected.operator_name||'Printer unassigned'} · {selected.machine||'Machine not set'} · {selected.stage==='READY'?(data?.owner?'Ready for dispatch':'Production complete'):selected.stage==='WAITING_RELEASE'?'Awaiting release':selected.stage}</p>{selected.hold_reason&&<p className="production-hold" role="status">On hold: {selected.hold_reason}</p>}</header>{data?.owner&&selected.stage==='WAITING_RELEASE'&&<ReadinessChecklist title="Production release checks" items={releaseReadiness(selected)}/ >}{data?.owner&&selected.stage==='READY'&&<ReadinessChecklist title="Dispatch readiness" items={dispatchReadiness(selected)}/ >}{!data?.owner&&(selected.stage==='WAITING_RELEASE'||selected.stage==='READY')&&<p className="owner-readiness-notice" role="status">Financial and invoice checks are owner-only. This view does not infer readiness from omitted queue fields; permitted photo evidence, if any, appears separately.</p>}{data?.owner&&<details className="workflow-history"><summary>Assign delivery or other staff tasks</summary><StaffJobAssignments leadId={selected.id}/></details>}<FulfilmentPanel leadId={selected.id} onChanged={load}/></Modal>}
 </section>;
}
