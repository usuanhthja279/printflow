"use client";
import {useEffect,useState} from 'react';
import {api,PRODUCT_NAME} from '@/lib/api';
import Modal from './Modal';
import FulfilmentPanel from './FulfilmentPanel';
import StaffJobAssignments from './StaffJobAssignments';
import {ProductionJobCard} from './WorkflowUI';
import {matchesQueueSearch,printReference} from '@/lib/references';
type Job={id:string;public_id:string;product_code:string;quantity:number;stage:string;fulfilment:string;operator_name:string|null;machine:string|null;priority:string|null;due_date:string|null;hold_reason:string|null};
type Board={owner:boolean;jobs:Job[]};
const columns=[['WAITING_RELEASE','Awaiting release'],['QUEUED','Queued'],['PRINTING','Printing'],['FINISHING','Finishing'],['QC','Quality check'],['READY','Ready / delivery']];
export default function ProductionBoard(){
 const [data,setData]=useState<Board|null>(null),[search,setSearch]=useState(''),[filter,setFilter]=useState('ALL'),[lane,setLane]=useState('ALL'),[selected,setSelected]=useState<Job|null>(null),[error,setError]=useState('');
 async function load(){setData(await api<Board>('/v1/production/queue'));setError('');}
 useEffect(()=>{load().catch(()=>setError('Production queue unavailable.'));},[]);
 const jobs=(data?.jobs||[]).filter(j=>matchesQueueSearch(search,j.public_id,[PRODUCT_NAME(j.product_code),j.operator_name||'',j.machine||''])&&(filter==='ALL'||filter==='HOLD'&&!!j.hold_reason||filter==='URGENT'&&j.priority==='URGENT'));
 return <section className="panel production-board" aria-label="Production queue"><div className="staff-toolbar"><div><p className="overline">PRINT FLOOR / STAFF-CONFIRMED</p><h2>Production queue</h2></div><button className="btn btn-outline" onClick={()=>load().catch(()=>setError('Could not refresh production.'))}>Refresh production queue</button></div>
 <p className="hint">{data?.owner?'Release approved work and coordinate each handoff.':'Only jobs assigned to your printer account appear here.'} Latest 200 visible active projects; machine labels do not reserve capacity.</p>
 <div className="queue-summary"><span><strong>{jobs.length}</strong> shown</span><span><strong>{jobs.filter(j=>!!j.hold_reason).length}</strong> on hold</span><span><strong>{jobs.filter(j=>j.priority==='URGENT').length}</strong> urgent</span><span>Hold pauses a job in its current lane</span></div>
 <div className="catalogue-tools"><label className="catalogue-search">Search production jobs<input type="search" value={search} onChange={e=>setSearch(e.target.value)} placeholder="Product, reference, printer or machine"/></label><label className="field">Production filter<select value={filter} onChange={e=>setFilter(e.target.value)}><option value="ALL">All visible</option><option value="HOLD">On hold</option><option value="URGENT">Urgent</option></select></label><label className="field mobile-lane-select">Production lane<select value={lane} onChange={e=>setLane(e.target.value)}><option value="ALL">All lanes ({jobs.length})</option>{columns.map(([stage,label])=><option value={stage} key={stage}>{label} ({jobs.filter(j=>j.stage===stage).length})</option>)}</select></label></div>
 <div className="production-lanes">{columns.map(([stage,label])=><section className={'production-lane lane-'+stage.toLowerCase()} key={stage} data-mobile-visible={lane==='ALL'||lane===stage} aria-label={label}><header><h3>{label}</h3><span>{jobs.filter(j=>j.stage===stage).length}</span></header><div className="production-lane-cards">{jobs.filter(j=>j.stage===stage).map(j=><ProductionJobCard key={j.id} reference={printReference(j.public_id)} product={PRODUCT_NAME(j.product_code)} quantity={j.quantity} operator={j.operator_name} machine={j.machine} due={j.due_date} hold={j.hold_reason} urgent={j.priority==='URGENT'} onOpen={()=>setSelected(j)}/>)}{!jobs.some(j=>j.stage===stage)&&<p className="lane-empty">No jobs here</p>}</div></section>)}</div>
 {!data&&!error&&<p role="status">Loading production…</p>}{error&&<p role="alert">{error}</p>}
 {selected&&<Modal layout="drawer" title="Production job" closeLabel="Close production job" onClose={()=>{setSelected(null);load().catch(()=>{});}}><header className="job-drawer-heading"><p className="overline">{printReference(selected.public_id)}</p><h2>{PRODUCT_NAME(selected.product_code)}</h2><p>{selected.quantity.toLocaleString('en-IN')} copies · Staff-confirmed operations</p></header><FulfilmentPanel leadId={selected.id} onChanged={load}/>{data?.owner&&<details className="workflow-history"><summary>Assign delivery or other staff tasks</summary><StaffJobAssignments leadId={selected.id}/></details>}</Modal>}
 </section>;
}
