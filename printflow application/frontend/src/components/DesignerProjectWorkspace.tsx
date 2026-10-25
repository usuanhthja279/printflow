"use client";

import {useEffect,useState} from 'react';
import Link from 'next/link';
import {api,ApiError,PRODUCT_NAME} from '@/lib/api';
import {printReference} from '@/lib/references';
import {StaffSession} from '@/lib/staff';
import DesignProofs from './DesignProofs';
import WorkConversation from './WorkConversation';
import {DesignerJob,designerProjectNextAction,designerProjectState} from './DesignerWorkspace';
import {ProjectCollaborationProvider} from './ProjectCollaborationProvider';

export default function DesignerProjectWorkspace({id}:{id:string}){
 const [project,setProject]=useState<DesignerJob|null>(null),[loading,setLoading]=useState(true),[error,setError]=useState('');
 async function load(){
  try{
   const session=await api<StaffSession>('/v1/auth/session');
   if(session.role!=='DESIGNER'||session.mfaRequired||session.mustChangePassword)throw new ApiError('Sign in with the assigned designer account to open this project.',403);
   const current=await api<DesignerJob>('/v1/design/enquiries/'+id+'/designer-project');
   setProject(current);setError('');
  }catch(e){setProject(null);setError(e instanceof ApiError&&e.status===403?e.message:'This project is unavailable or is no longer assigned to your account.');}
  finally{setLoading(false);}
 }
 useEffect(()=>{void load();},[id]);
 if(loading)return <section className="page-section"><div className="container"><p role="status">Opening assigned design project…</p></div></section>;
 if(!project)return <section className="page-section"><div className="container designer-project-unavailable"><p className="overline">J.D.PRINTEX / DESIGN STUDIO</p><h1>Project unavailable</h1><p role={error?'alert':'status'}>{error||'Check the assignment and try again.'}</p><Link className="btn btn-ink" href="/business">Back to design projects</Link></div></section>;
 return <ProjectCollaborationProvider projectId={id}><section className="page-section designer-project-page"><div className="container designer-project-container">
  <Link className="workspace-back" href="/business">← Assigned design projects</Link>
  <header className="designer-project-heading"><div><p className="overline">J.D.PRINTEX / DESIGN WORKSPACE · {printReference(project.public_id)}</p><h1>{PRODUCT_NAME(project.product_code)}</h1><p>{project.quantity.toLocaleString('en-IN')} copies · Customer: {project.customer_name}</p></div><div className="designer-project-status"><span>{designerProjectState(project)}</span><strong>Proof {project.latest_proof_revision?`v${project.latest_proof_revision}`:'not shared'}</strong></div></header>
  <section className="designer-project-next panel" aria-label="Designer next action"><div><small>YOUR NEXT ACTION</small><strong>{designerProjectNextAction(project)}</strong><span>{project.unresolved_feedback_count?`${project.unresolved_feedback_count} unresolved comments`:project.next_task_due_date?`Next task due ${project.next_task_due_date}`:'Review the authorized project record below'}</span></div><button type="button" className="btn btn-outline" disabled={loading} onClick={()=>{setLoading(true);void load();}}>Refresh project</button></section>
  <nav className="designer-workspace-nav" aria-label="Design workspace sections"><a href="#project-overview">Overview</a><a href="#reference-artwork">Customer reference</a><a href="#design-upload">Design files</a><a href={"#current-proof-"+id}>Current proof</a><a href={"#customer-feedback-"+id}>Feedback</a><a href="#designer-tasks">Tasks</a><a href="#proof-history">Revisions</a><a href="#designer-conversation">Conversation</a></nav>
  <div className="designer-project-grid"><main className="designer-project-main">
   <section className="designer-overview-panel panel" id="project-overview"><div><small>Current proof</small><strong>{designerProjectState(project)}</strong></div><div><small>Customer feedback</small><strong>{project.unresolved_feedback_count} unresolved</strong></div><div><small>Assignment</small><strong>Assigned to you</strong></div></section>
   <DesignProofs leadId={id} inlineStudio onChanged={load}/>
  </main><aside className="designer-project-conversation" id="designer-conversation" aria-label="Project conversation"><WorkConversation leadId={id}/></aside></div>
 </div></section></ProjectCollaborationProvider>;
}
