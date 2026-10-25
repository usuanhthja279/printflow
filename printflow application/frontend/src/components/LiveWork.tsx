"use client";
import {useEffect,useState} from 'react';
import Link from 'next/link';
import {api,PRODUCT_NAME} from '@/lib/api';
import FulfilmentPanel from './FulfilmentPanel';
import PaymentsPanel from './PaymentsPanel';
import Quotations from './Quotations';
import DesignProofs from './DesignProofs';
import WorkConversation,{WorkData} from './WorkConversation';
import {NextAction,customerNextAction,ProjectStageStepper,EntityActivity} from './WorkflowUI';
import {printReference} from '@/lib/references';
import {ProjectCollaborationProvider} from './ProjectCollaborationProvider';
const targets={quotation:'work-agreement',design:'work-artwork',payment:'work-payments',production:'work-delivery',delivery:'work-delivery'};
function revealAgreement(){const section=document.getElementById('work-agreement');if(section instanceof HTMLDetailsElement)section.open=true;}
export default function LiveWork({id}:{id:string}){
 const [data,setData]=useState<WorkData|null>(null),[error,setError]=useState('');
 async function load(){setData(await api<WorkData>('/v1/design/enquiries/'+id+'/work'));setError('');}
 useEffect(()=>{load().catch(()=>setError('This work is unavailable. Sign in with the customer account linked to it.'));},[id]);
 useEffect(()=>{const reveal=()=>{if(window.location.hash==='#work-agreement')revealAgreement();};reveal();window.addEventListener('hashchange',reveal);return()=>window.removeEventListener('hashchange',reveal);},[data?.liveWork]);
 const received=data?.logs.find(e=>e.kind==='REQUEST');
 return <section className="page-section live-work-page"><div className="container work-container"><Link className="workspace-back" href="/customer">← All your projects</Link>{error&&<p role="alert">{error}</p>}{!data&&!error&&<p role="status">Opening your project…</p>}{data&&!data.liveWork?<section className="panel"><h1>Review your quotation first</h1><p>Design and fulfilment open after the quotation is accepted.</p><Link href="/customer">Open your enquiries</Link></section>:data&&<ProjectCollaborationProvider projectId={id}>
 <header className="work-page-heading"><div><p className="overline">YOUR PRINT PROJECT · {printReference(data.publicId)}</p><h1>{PRODUCT_NAME(data.productCode)}</h1><p>{data.quantity.toLocaleString('en-IN')} copies · {data.status==='CLOSED'?'Closed work':'Active project'}{received&&' · Enquiry received '+new Date(received.created_at).toLocaleDateString('en-IN')}</p></div><div className="project-responsibility"><span>Design contact</span><strong>{data.designer||'Assignment pending'}</strong><a href="#project-conversation">Message the team ↗</a></div></header>
 {data.closureMessage&&<p className="panel">{data.closureMessage}</p>}
 <nav className="work-page-nav" aria-label="Work sections"><a href="#work-overview">Overview</a><a href="#work-artwork">Artwork & review</a><a href="#work-payments">Payments & invoices</a><a href="#work-delivery">Production & delivery</a><a href="#work-agreement" onClick={revealAgreement}>Quotation</a><a href="#work-history">Work log</a><a className="mobile-conversation-link" href="#project-conversation">Conversation</a></nav>
 <div className="work-page-grid"><div className="work-primary"><section id="work-overview" className="work-overview"><NextAction step={customerNextAction(data)}/><div className="staff-toolbar"><h2>Work progress</h2><button className="text-button" onClick={()=>load().catch(()=>setError('Could not refresh.'))}>Refresh progress</button></div><ProjectStageStepper stages={data.stages} targets={targets} onQuotation={revealAgreement}/><p className="hint">Staff-confirmed milestones · Printing starts after exact proof approval, verified advance and owner release.</p></section>
 <section className="panel" id="work-artwork"><div className="section-eyebrow">01 / CREATIVE WORKSPACE</div><h2>Artwork & design review</h2>{data.workflow.fulfilmentState==='NOT_STARTED'?<DesignProofs leadId={id} customer onChanged={load}/>:<details className="workflow-history"><summary>Approved artwork and review history</summary><DesignProofs leadId={id} customer onChanged={load}/></details>}</section>
 <PaymentsPanel leadId={id} onChanged={load}/><section id="work-delivery"><FulfilmentPanel leadId={id} customer onChanged={load}/></section>
 <details className="panel workflow-history" id="work-agreement"><summary>Quotation and commercial order</summary><Quotations leadId={id} customer closed={data.status==='CLOSED'}/></details>
 <details className="panel workflow-history" id="work-history"><summary>Work log</summary><ol className="work-log">{data.logs.map((event,i)=><EntityActivity key={i} source={event.kind==='PROOF'?'Artwork record':event.kind==='REQUEST'?'Enquiry record':'Project record'} time={event.created_at}><strong>{event.message}</strong></EntityActivity>)}</ol></details></div>
 <aside className="panel work-sidebar" id="project-conversation" aria-label="Project conversation and email"><WorkConversation leadId={id}/>{(data.contact.email||data.contact.phone)&&<address>{data.contact.email&&<p><a href={'mailto:'+data.contact.email}>{data.contact.email}</a></p>}{data.contact.phone&&<p><a href={'tel:'+data.contact.phone}>{data.contact.phone}</a></p>}</address>}</aside></div></ProjectCollaborationProvider>}
 </div></section>;
}
