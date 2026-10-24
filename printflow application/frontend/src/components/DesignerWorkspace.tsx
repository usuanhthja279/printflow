"use client";

import IntakeQueue from './IntakeQueue';
import Link from 'next/link';
import WorkspaceOverview from './WorkspaceOverview';
import {useEffect,useMemo,useState} from 'react';
import {api,PRODUCT_NAME} from '@/lib/api';
import {matchesQueueSearch,printReference} from '@/lib/references';

export type DesignerJob={
 id:string;public_id:string;customer_name:string;product_code:string;quantity:number;status:string;
 quote_accepted:boolean;latest_proof_revision:number|null;latest_proof_name:string|null;latest_proof_summary:string|null;
 latest_proof_scan_state:string|null;latest_proof_decision:string|null;latest_proof_current_agreement:boolean|null;
 unresolved_feedback_count:number;next_task_due_date:string|null;
};
type Filter='ALL'|'ACTION'|'CUSTOMER'|'CHANGES'|'APPROVED'|'CLOSED';
const filterOptions:[Filter,string][]=[['ALL','All assigned work'],['ACTION','Needs designer action'],['CUSTOMER','Awaiting customer'],['CHANGES','Changes requested'],['APPROVED','Approved'],['CLOSED','Closed / history']];
const decision=(job:DesignerJob)=>job.latest_proof_decision?.toUpperCase()||'';
function needsAction(job:DesignerJob){
 if(job.status==='CLOSED'||!job.quote_accepted)return false;
 if(!job.latest_proof_revision||job.latest_proof_current_agreement===false||job.latest_proof_scan_state!=='CLEAN')return true;
 return ['REQUEST_CHANGES','REJECT'].includes(decision(job))||(job.unresolved_feedback_count>0&&decision(job)!=='APPROVE');
}
function awaitingCustomer(job:DesignerJob){return job.status!=='CLOSED'&&job.quote_accepted&&!!job.latest_proof_revision&&job.latest_proof_scan_state==='CLEAN'&&job.latest_proof_current_agreement!==false&&!decision(job)&&job.unresolved_feedback_count===0;}
function changesRequested(job:DesignerJob){return job.status!=='CLOSED'&&['REQUEST_CHANGES','REJECT'].includes(decision(job));}
function approved(job:DesignerJob){return job.status!=='CLOSED'&&decision(job)==='APPROVE'&&job.latest_proof_current_agreement!==false;}
export function designerProjectState(job:DesignerJob){
 if(job.status==='CLOSED')return 'Closed project';
 if(!job.quote_accepted)return 'Waiting for quotation agreement';
 if(!job.latest_proof_revision)return 'Prepare artwork';
 if(job.latest_proof_current_agreement===false)return 'New proof required for current agreement';
 if(job.latest_proof_scan_state==='QUARANTINED')return 'Artwork awaiting scan';
 if(job.latest_proof_scan_state==='REJECTED')return 'Artwork scan rejected';
 if(decision(job)==='REQUEST_CHANGES')return 'Customer requested changes';
 if(decision(job)==='REJECT')return 'Proof rejected · replacement needed';
 if(decision(job)==='APPROVE')return 'Proof approved';
 if(job.unresolved_feedback_count>0)return 'Customer feedback to review';
 return 'Awaiting customer review';
}
export function designerProjectNextAction(job:DesignerJob){
 if(job.status==='CLOSED')return 'Review project history';
 if(!job.quote_accepted)return 'Wait for accepted quotation';
 if(!job.latest_proof_revision)return 'Prepare and publish the first proof';
 if(job.latest_proof_current_agreement===false)return 'Publish a proof for the current agreement';
 if(job.latest_proof_scan_state==='QUARANTINED')return 'Wait for scan or retry it';
 if(job.latest_proof_scan_state==='REJECTED')return 'Upload a clean replacement design';
 if(changesRequested(job))return 'Review feedback and prepare a revision';
 if(approved(job))return 'Review the exact approved proof';
 if(job.unresolved_feedback_count>0)return 'Review unresolved proof comments';
 return 'Wait for the customer’s review';
}
function includes(job:DesignerJob,filter:Filter){
 if(filter==='ACTION')return needsAction(job);
 if(filter==='CUSTOMER')return awaitingCustomer(job);
 if(filter==='CHANGES')return changesRequested(job);
 if(filter==='APPROVED')return approved(job);
 if(filter==='CLOSED')return job.status==='CLOSED';
 return true;
}

export default function DesignerWorkspace(){
 const [jobs,setJobs]=useState<DesignerJob[]>([]),[error,setError]=useState(''),[search,setSearch]=useState(''),[filter,setFilter]=useState<Filter>('ACTION'),[loading,setLoading]=useState(true);
 async function load(){try{setJobs(await api<DesignerJob[]>('/v1/design/queue'));setError('');}catch{setError('Assigned design work could not be loaded. Refresh to retry.');}finally{setLoading(false);}}
 useEffect(()=>{void load();},[]);
 const counts=useMemo(()=>Object.fromEntries(filterOptions.map(([key])=>[key,jobs.filter(job=>includes(job,key)).length])) as Record<Filter,number>,[jobs]);
 const visible=useMemo(()=>jobs.filter(job=>includes(job,filter)&&matchesQueueSearch(search,job.public_id,[job.customer_name,PRODUCT_NAME(job.product_code),job.latest_proof_name||'',job.latest_proof_summary||'',designerProjectState(job)])).sort((a,b)=>Number(needsAction(b))-Number(needsAction(a))||a.customer_name.localeCompare(b.customer_name)),[jobs,filter,search]);
 return <div className="designer-dashboard">
  <header className="designer-dashboard-heading"><div><p className="overline">J.D.PRINTEX / DESIGN STUDIO</p><h2>Assigned design projects</h2><p>Review the next action, customer feedback and proof state for work assigned to you.</p></div><button className="btn btn-outline" disabled={loading} onClick={()=>{setLoading(true);void load();}}>{loading?'Refreshing…':'Refresh projects'}</button></header>
  <WorkspaceOverview label="Your assigned design work" items={[
   {label:'Needs your action',value:counts.ACTION,detail:'Prepare artwork or respond to feedback',action:()=>setFilter('ACTION')},
   {label:'Awaiting customer',value:counts.CUSTOMER,detail:'Clean proof shared for review',action:()=>setFilter('CUSTOMER')},
   {label:'Changes requested',value:counts.CHANGES,detail:'Review current proof feedback',action:()=>setFilter('CHANGES')},
   {label:'Approved',value:counts.APPROVED,detail:'Exact current proof approved',action:()=>setFilter('APPROVED')}
  ]}/>
  <section className="panel designer-project-queue" aria-label="Assigned design projects">
   <div className="designer-queue-tools"><label className="field">Find a project<input type="search" value={search} onChange={event=>setSearch(event.target.value)} placeholder="Public reference, customer or product"/></label><label className="field">Project filter<select aria-label="Designer project filter" value={filter} onChange={event=>setFilter(event.target.value as Filter)}>{filterOptions.map(([key,label])=><option key={key} value={key}>{label} · {counts[key]}</option>)}</select></label></div>
   {error&&<p role="alert">{error}</p>}{loading&&!jobs.length?<p role="status">Loading assigned projects…</p>:visible.length===0?<div className="empty-state"><strong>{jobs.length?'No projects match this view':'No assigned design projects yet'}</strong><p>{jobs.length?'Try another filter or search term.':'New assignments will appear here after a quotation is accepted.'}</p></div>:<ul className="designer-project-cards">{visible.map(job=><li className="designer-project-card" key={job.id}>
    <div className="designer-project-card-top"><span className="public-reference">{printReference(job.public_id)}</span><span className={'designer-state '+(needsAction(job)?'needs-action':approved(job)?'is-approved':job.status==='CLOSED'?'is-closed':'awaiting-customer')}>{designerProjectState(job)}</span></div>
    <div className="designer-project-card-main"><div><h3>{PRODUCT_NAME(job.product_code)}</h3><p>{job.quantity.toLocaleString('en-IN')} copies · {job.customer_name}</p>{job.latest_proof_revision&&<p className="designer-proof-context">Proof v{job.latest_proof_revision}{job.latest_proof_name?' · '+job.latest_proof_name:''}{job.latest_proof_scan_state==='CLEAN'?' · Scan passed':job.latest_proof_scan_state==='QUARANTINED'?' · Scan pending':job.latest_proof_scan_state==='REJECTED'?' · Scan rejected':''}</p>}</div><p className="designer-next-action"><small>Next action</small><strong>{designerProjectNextAction(job)}</strong></p></div>
    <div className="designer-project-card-footer"><span>{job.unresolved_feedback_count?`${job.unresolved_feedback_count} unresolved proof comment${job.unresolved_feedback_count===1?'':'s'}`:'No unresolved proof comments'}</span>{job.next_task_due_date&&<span>Task due {new Date(job.next_task_due_date+'T12:00:00').toLocaleDateString('en-IN')}</span>}<Link className="btn btn-ink" href={'/designer/work/'+job.id}>Open design workspace</Link></div>
   </li>)}</ul>}
  </section>
  <section className="panel designer-intake"><h3>New design intake</h3><p>Review incoming work before it enters your assigned projects.</p><IntakeQueue onAccepted={load}/></section>
 </div>;
}
