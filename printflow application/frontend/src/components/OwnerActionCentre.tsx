"use client";
import {useEffect,useState} from 'react';
import type {Lead} from '@/lib/staff';
import {PRODUCT_NAME} from '@/lib/api';
import {api} from '@/lib/api';
import {printReference} from '@/lib/references';

type ProductionJob={id:string;stage:string;fulfilment:string;hold_reason:string|null;rework_count:number;dispatch_blockers?:string[]};
type OwnerSnapshot={jobs:ProductionJob[];pendingAssignmentCount:number};
const statusName:Record<string,string>={NEW:'New enquiry',REVIEWING:'Quotation preparation',QUOTED:'Customer decision',IN_PROGRESS:'Live work',CLOSED:'Closed'};

export default function OwnerActionCentre({items,onOpen,owner=false,onQueueFilter,onProductionFilter,onDesignRouting}:{items:Lead[];onOpen:(lead:Lead)=>void;owner?:boolean;onQueueFilter?:(status:string)=>void;onProductionFilter?:(filter:string)=>void;onDesignRouting?:()=>void}){
 const [snapshot,setSnapshot]=useState<OwnerSnapshot|null>(null),[snapshotError,setSnapshotError]=useState(false);
 useEffect(()=>{if(!owner)return;let active=true;Promise.all([api<{jobs:ProductionJob[]}>('/v1/production/queue'),api<{pendingAssignmentCount:number}>('/v1/owner/design-routing/summary')]).then(([production,design])=>{if(active){setSnapshot({jobs:production.jobs,pendingAssignmentCount:design.pendingAssignmentCount});setSnapshotError(false);}}).catch(()=>{if(active){setSnapshot(null);setSnapshotError(true);}});return()=>{active=false;};},[owner]);
 const overdue=(lead:Lead)=>!!lead.followUpAt&&Date.parse(lead.followUpAt)<Date.now();
 const waiting=items.filter(lead=>lead.status!=='CLOSED'&&(overdue(lead)||['NEW','REVIEWING','QUOTED'].includes(lead.status)||!lead.assignedStaffId)).sort((a,b)=>Number(overdue(b))-Number(overdue(a))||Number(b.priority==='HIGH')-Number(a.priority==='HIGH')||Date.parse(a.createdAt)-Date.parse(b.createdAt));
 const jobs=snapshot?.jobs||[];
 const operational=jobs.filter(j=>j.stage==='WAITING_RELEASE').length;
 const heldQuality=jobs.filter(j=>!!j.hold_reason||j.stage==='QC'||j.rework_count>0).length;
 const dispatchBlocked=jobs.filter(j=>j.fulfilment==='READY'&&(j.dispatch_blockers?.length||0)>0).length;
 const cards=[
  {label:'New enquiries',value:items.filter(x=>x.status==='NEW').length,detail:'Business review',activate:()=>onQueueFilter?.('NEW'),tone:'intake'},
  {label:'Quotation prep',value:items.filter(x=>x.status==='REVIEWING').length,detail:'Scope and pricing',activate:()=>onQueueFilter?.('REVIEWING'),tone:'quote'},
  {label:'Customer decisions',value:items.filter(x=>x.status==='QUOTED').length,detail:'Quotation response',activate:()=>onQueueFilter?.('QUOTED'),tone:'customer'},
  {label:'Designer assignment',value:snapshot?.pendingAssignmentCount,detail:'Accepted work unassigned',activate:()=>onDesignRouting?.(),tone:'design'},
  {label:'Awaiting release',value:snapshot?operational:undefined,detail:'Accepted work not released',activate:()=>onProductionFilter?.('WAITING_RELEASE'),tone:'release'},
  {label:'Holds · QC · rework',value:snapshot?heldQuality:undefined,detail:'Production exceptions',activate:()=>onProductionFilter?.('HOLD'),tone:'hold'},
  {label:'Dispatch blockers',value:snapshot?dispatchBlocked:undefined,detail:'Payment, invoice or evidence',activate:()=>onProductionFilter?.('BLOCKED_DISPATCH'),tone:'dispatch'}
 ];
 return <section className="action-centre" aria-label="Business action inbox">
  {owner&&<section className="owner-command-centre" aria-label="Owner operations overview"><div className="command-heading"><div><p className="overline">OWNER COMMAND CENTRE</p><h2>Work that needs a decision</h2></div><span className="command-scope">Authorized active queues</span></div><div className="owner-command-grid">{cards.map(card=><button key={card.label} type="button" className={'owner-command-card command-'+card.tone} onClick={card.activate} disabled={!card.activate||card.value===undefined}><span>{card.label}</span><strong>{card.value===undefined?'—':card.value.toLocaleString('en-IN')}</strong><small>{card.value===undefined?(snapshotError?'Data unavailable':'Loading live queue'):card.detail}</small></button>)}</div><p className="command-footnote">Counts reflect the latest loaded enquiries and authorized production queue. Unavailable data is shown as a dash, never as zero.</p></section>}
  <header><div><p className="overline">PRIORITIZED WORKLIST</p><h2>Needs attention <span>{waiting.length}</span></h2></div><p>Overdue first, then high priority and oldest request.</p></header>
  {waiting.length?<ul>{waiting.slice(0,8).map(lead=>{const reason=overdue(lead)?'Follow-up overdue':lead.status==='NEW'?'Review new enquiry':lead.status==='REVIEWING'?'Prepare quotation':lead.status==='QUOTED'?'Customer decision pending':!lead.assignedStaffId?'Sales assignment missing':'Follow-up needed';return <li key={lead.id}><span className={'attention-dot'+(overdue(lead)?' attention-overdue':'')} aria-hidden="true"/><div><strong>{lead.customerName}</strong><small>{PRODUCT_NAME(lead.productCode)} · {printReference(lead.publicId)} · {statusName[lead.status]||lead.status}</small></div><span className="attention-reason">{reason}</span>{lead.priority==='HIGH'&&<span className="attention-priority">High</span>}<button className="btn btn-outline btn-small" onClick={()=>onOpen(lead)}>Review request</button></li>;})}</ul>:<p className="hint">No enquiry, customer-decision or follow-up exceptions in the loaded queue.</p>}
  <p className="hint">Customer quotation decisions remain in Enquiries. Production blockers and design assignments link to their source work queues.</p>
 </section>;
}
