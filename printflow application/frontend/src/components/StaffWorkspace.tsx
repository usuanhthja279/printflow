'use client';
import OwnerActionCentre from './OwnerActionCentre';import ProductionBoard from './ProductionBoard';import DesignerRoutingPanel from './DesignerRoutingPanel';
import WorkspaceOverview from './WorkspaceOverview';import EnquiryStages from './EnquiryStages';import MailActivity from './MailActivity';
import IntakeActions from './IntakeActions';
import AppearanceEditor from './AppearanceEditor';
import WorkConversation from './WorkConversation';
import EnquiryResolution from './EnquiryResolution';
import DesignProofs from './DesignProofs';
import SpecialistWorkspace from './SpecialistWorkspace';
import StaffJobAssignments from './StaffJobAssignments';
import Modal from './Modal';
import ContactsWorkspace from './ContactsWorkspace';
import ProductSettings from './ProductSettings';
import OwnerPrivacy from './OwnerPrivacy';
import LocalMailPreview from './LocalMailPreview';
import OwnerAssignment from './OwnerAssignment';
import AccountAccess from './AccountAccess';
import StaffAdministration from './StaffAdministration';
import {FormEvent,useEffect,useState} from 'react';
import {api,ApiError,EnquiryInput,PRODUCT_NAME,PRODUCTS_FALLBACK} from '@/lib/api';
import {createStaffEnquiry,Lead,LeadActivity,signIn,StaffSession,staffMutation} from '@/lib/staff';

const statuses=['NEW','REVIEWING','QUOTED','CLOSED'] as const;
const sources=['WALK_IN','PHONE','EMAIL','WHATSAPP','SOCIAL','REFERRAL','WEBSITE','OTHER'];
const blank:EnquiryInput={customerName:'',email:'',phone:'',productCode:'WEDDING_CARDS',quantity:250,size:'',paperGsm:null,printSides:'DOUBLE',finish:'UNSURE',notes:''};
function localInput(value:string|null){if(!value)return '';const d=new Date(value);return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,16);}
function date(value:string){return new Date(value).toLocaleString('en-IN',{timeZone:'Asia/Kolkata',dateStyle:'medium',timeStyle:'short'})+' IST';}

export default function StaffWorkspace(){
 const [checked,setChecked]=useState<string[]>([]);
 const [staff,setStaff]=useState<StaffSession|null>(null),[loading,setLoading]=useState(true);
 const [username,setUsername]=useState(''),[password,setPassword]=useState('');
 const [items,setItems]=useState<Lead[]>([]),[selected,setSelected]=useState<Lead|null>(null);
 const [activities,setActivities]=useState<LeadActivity[]>([]),[error,setError]=useState(''),[busy,setBusy]=useState(false);
 const [detailOpen,setDetailOpen]=useState(false),[profileOpen,setProfileOpen]=useState(false);
 const [tab,setTab]=useState('ENQUIRIES');const [notice,setNotice]=useState('');
 const [statusFilter,setStatusFilter]=useState('ALL');
 const [filter,setFilter]=useState('ALL'),[search,setSearch]=useState('');
 const [action,setAction]=useState(''),[due,setDue]=useState(''),[priority,setPriority]=useState<Lead['priority']>('NORMAL');
 const [adding,setAdding]=useState(false),[newLead,setNewLead]=useState(blank),[source,setSource]=useState('WALK_IN');
 const [created,setCreated]=useState<{lead:Lead;trackingToken:string}|null>(null);
 function clearWorkspace(){setDetailOpen(false);setProfileOpen(false);setStaff(null);setItems([]);setSelected(null);setCreated(null);setActivities([]);setAdding(false);setNewLead(blank);setAction('');setDue('');setPassword('');setSearch('');setFilter('ALL');setStatusFilter('ALL');}
 function failure(e:unknown){
  if(e instanceof ApiError&&e.status===401){clearWorkspace();setError('Please sign in to continue.');}
  else if(e instanceof ApiError&&e.status===409)setError(e.message||'This enquiry changed. Refresh and try again.');
  else if(e instanceof ApiError&&e.status===403)setError('You do not have permission for this action.');
  else if(e instanceof ApiError&&e.status===429)setError('Too many sign-in attempts. Try again in 15 minutes.');
  else setError(e instanceof Error?e.message:'The request could not be completed.');
 }
 const queuePath=(before?:string)=>'/v1/staff/enquiries?'+new URLSearchParams({search,filter,status:statusFilter,...(before?{before}:{})});
 async function refresh(){const leads=await api<Lead[]>(queuePath());setItems(leads);return leads;}
 useEffect(()=>{const open=()=>setProfileOpen(true);window.addEventListener('jdprintex:profile',open);if(new URLSearchParams(window.location.search).get('profile'))setProfileOpen(true);return()=>window.removeEventListener('jdprintex:profile',open);},[]);
 useEffect(()=>{let active=true;api<StaffSession>('/v1/auth/session').then(async user=>{
  const leads=user.mfaRequired||user.mustChangePassword||!['SALES','OWNER'].includes(user.role)?[]:await api<Lead[]>('/v1/staff/enquiries');if(active){setStaff(user);setItems(leads);}
 }).catch(e=>{if(active&&!(e instanceof ApiError&&e.status===401))setError('Staff workspace is unavailable. Please try again.');}).finally(()=>{if(active)setLoading(false);});return()=>{active=false;};},[]);
 useEffect(()=>{if(!staff||staff.mfaRequired||staff.mustChangePassword||!['OWNER','SALES'].includes(staff.role))return;let active=true;const timer=setTimeout(()=>{api<Lead[]>(queuePath()).then(rows=>{if(active)setItems(rows);}).catch(e=>{if(active)failure(e);});},300);return()=>{active=false;clearTimeout(timer);};},[staff,search,filter,statusFilter]);
 useEffect(()=>{let active=true;setActivities([]);if(selected){setAction(selected.nextAction||'');setDue(localInput(selected.followUpAt));setPriority(selected.priority);
  api<LeadActivity[]>(`/v1/staff/enquiries/${selected.id}/activities`).then(data=>{if(active)setActivities(data);}).catch(e=>{if(active)failure(e);});
 }return()=>{active=false;};},[selected]);
 async function login(e:FormEvent){e.preventDefault();setBusy(true);setError('');try{const user=await signIn(username,password);setPassword('');setStaff(user);if(!user.mfaRequired&&!user.mustChangePassword&&['OWNER','SALES'].includes(user.role))await refresh();}catch(e){setPassword('');failure(e);}finally{setBusy(false);}}
 async function acceptIntake(){if(!selected)return;setBusy(true);try{await staffMutation('/v1/design/intake/'+selected.id+'/accept','POST',{revision:selected.revision});const next=await refresh();setSelected(next.find(l=>l.id===selected.id)||null);}catch(e){failure(e);}finally{setBusy(false);}}
 async function dismiss(lead:Lead){setBusy(true);setError('');try{const result=await staffMutation<{notification:string;deliveryEnabled:boolean}>('/v1/owner/enquiries/'+lead.id+'/dismiss','POST',{revision:lead.revision});setNotice(result.notification==='NOT_CONFIGURED'?'Enquiry removed. Email could not be prepared: email storage is not configured. Consider calling the customer.':result.deliveryEnabled?'Enquiry removed. Rejection email queued for delivery. Consider calling the customer.':'Enquiry removed. Rejection email prepared in Local Email; real delivery is disabled. Consider calling the customer.');await refresh();}catch(e){failure(e);}finally{setBusy(false);}}
 async function dismissSelected(){setBusy(true);setError('');try{const result=await staffMutation<{removed:number;deliveryEnabled:boolean;notification:string}>('/v1/owner/enquiries/dismiss-selected','POST',{enquiries:items.filter(l=>checked.includes(l.id)).map(l=>({id:l.id,revision:l.revision}))});setNotice(result.removed+' enquiries archived. '+(result.notification==='NOT_CONFIGURED'?'Email storage is not configured.':result.deliveryEnabled?'Customer emails queued.':'Customer emails prepared; real delivery is disabled.'));setChecked([]);await refresh();}catch(e){failure(e);}finally{setBusy(false);}}
 async function logout(){setBusy(true);setError('');try{await staffMutation('/v1/auth/logout','POST');clearWorkspace();}catch(e){failure(e);}finally{setBusy(false);}}
 async function change(suffix:string,body:object){if(!selected)return;setBusy(true);setError('');try{
  const updated=await staffMutation<Lead>(`/v1/staff/enquiries/${selected.id}/${suffix}`,'PATCH',{revision:selected.revision,...body});
  setItems(old=>old.map(item=>item.id===updated.id?updated:item));setSelected(updated);
 }catch(e){failure(e);}finally{setBusy(false);}}
 async function saveFollowUp(e:FormEvent){e.preventDefault();await change('follow-up',{priority,nextAction:action.trim()||null,followUpAt:due?new Date(due).toISOString():null});}
 async function add(e:FormEvent){e.preventDefault();setBusy(true);setError('');try{const result=await createStaffEnquiry(newLead,source);setCreated(result);setSelected(result.lead);setAdding(false);setNewLead(blank);await refresh();}catch(e){failure(e);}finally{setBusy(false);}}
 const overdue=(lead:Lead)=>lead.status!=='CLOSED'&&!!lead.followUpAt&&Date.parse(lead.followUpAt)<Date.now();
 const visible=items.filter(lead=>(filter==='ALL'||filter==='OVERDUE'&&overdue(lead)||filter==='UNASSIGNED'&&!lead.assignedStaffId||filter==='MINE'&&lead.assignedStaffId===staff?.id)&&`${lead.customerName} ${lead.email} ${lead.phone} ${PRODUCT_NAME(lead.productCode)}`.toLowerCase().includes(search.toLowerCase()));
 const owned=selected?.assignedStaffId===staff?.id;
 if(loading)return <section className="page-section"><div className="container"><p role="status">Opening staff workspace…</p></div></section>;
 return <section className="page-section business-bg"><div className="container">
  <header className="page-intro business-intro"><p className="overline">J.D.PRINTEX / {staff?.role||'STAFF'}</p><h1>{staff&&['OWNER','SALES'].includes(staff.role)?({ENQUIRIES:'Enquiries & follow-ups',PRODUCTION:'Print floor',CONTACTS:'Customer contacts','DESIGN ROUTING':'Design coordination',STAFF:'Team & access',PRODUCTS:'Product catalogue',EMAIL:'Email activity'}[tab]||'Workspace'):staff?.role==='DESIGNER'?'Design workspace':staff?.role==='OPERATOR'?'Printing workspace':staff?.role==='DELIVERY'?'Delivery workspace':staff?'Your assigned work':'Welcome to your workspace'}</h1><p>{staff&&['OWNER','SALES'].includes(staff.role)?'One project record. Clear responsibilities at every handoff.':'Your role, assigned tasks and authorized tools.'}</p></header>
  {!staff?<form onSubmit={login} className="panel owner-login auth-card"><h2>Staff sign in</h2><p>Use your individual staff credentials to open your workspace.</p>
   <label className="field">Staff username<input required maxLength={140} autoComplete="username" value={username} onChange={e=>setUsername(e.target.value)}/></label>
   <label className="field">Password<input required type="password" maxLength={72} autoComplete="current-password" value={password} onChange={e=>setPassword(e.target.value)}/></label>
   <button className="btn btn-ink" disabled={busy}>{busy?'Signing in…':'Sign in'}</button>
  </form>:staff.mfaRequired||staff.mustChangePassword?<AccountAccess staff={staff} logout={logout} done={async user=>{setStaff(user);if(!user.mfaRequired&&!user.mustChangePassword&&['OWNER','SALES'].includes(user.role))await refresh();}}/>:!['OWNER','SALES'].includes(staff.role)?<SpecialistWorkspace staff={staff}/>:<>
   {tab==='ENQUIRIES'&&<WorkspaceOverview label="Your visible queue at a glance" items={[
    {label:'New intake',value:items.filter(i=>i.status==='NEW').length,detail:'Awaiting business review',action:()=>{setTab('ENQUIRIES');setStatusFilter('NEW');}},
    {label:'Preparing quotes',value:items.filter(i=>i.status==='REVIEWING').length,detail:'Scope and pricing',action:()=>{setTab('ENQUIRIES');setStatusFilter('REVIEWING');}},
    {label:'With customers',value:items.filter(i=>i.status==='QUOTED').length,detail:'Quotation decisions',action:()=>{setTab('ENQUIRIES');setStatusFilter('QUOTED');}},
    {label:'Live work',value:items.filter(i=>i.status==='IN_PROGRESS').length,detail:'Design to delivery',action:()=>{setTab('ENQUIRIES');setStatusFilter('IN_PROGRESS');}}
   ]}/>}
   <div className="staff-toolbar"><p>Signed in as <strong>{staff.username}</strong> · {staff.role}</p><div className="row-actions"><button className="btn btn-outline btn-small" disabled={busy} onClick={async()=>{setError('');try{const leads=await refresh();setSelected(old=>old?leads.find(x=>x.id===old.id)||null:null);}catch(e){failure(e);}}}>Refresh enquiries</button><button className="btn btn-ink btn-small" disabled={busy} onClick={()=>{setTab('ENQUIRIES');setAdding(!adding);}}>{adding?'Cancel new enquiry':'Add enquiry'}</button></div></div>
   <div className="workspace-modules" role="navigation" aria-label="Workspace modules">{['ENQUIRIES',...(staff.role==='OWNER'||staff.contactsAccess?['CONTACTS']:[]),...(staff.role==='OWNER'?['PRODUCTION','DESIGN ROUTING','STAFF','PRODUCTS','EMAIL']:[])].map(t=><button key={t} className={tab===t?'btn btn-ink':'btn btn-outline'} onClick={()=>setTab(t)}>{t}</button>)}</div>
   {tab==='CONTACTS'&&(staff.role==='OWNER'||staff.contactsAccess)&&<ContactsWorkspace lead={selected} linked={async()=>{const leads=await refresh();setSelected(old=>old?leads.find(x=>x.id===old.id)||null:null);}}/>}
   {staff.role==='OWNER'&&tab==='PRODUCTION'&&<ProductionBoard/>}{staff.role==='OWNER'&&tab==='DESIGN ROUTING'&&<DesignerRoutingPanel/>}
   {staff.role==='OWNER'&&tab==='STAFF'&&<StaffAdministration ownerId={staff.id} reauthenticate={()=>setStaff({...staff,mfaRequired:true})}/>}
   {staff.role==='OWNER'&&tab==='PRODUCTS'&&<ProductSettings/>}
   {staff.role==='OWNER'&&tab==='STAFF'&&<details className="workflow-history"><summary>Administration · privacy requests</summary><p>Review customer information export and erasure requests.</p><OwnerPrivacy/></details>}
   {staff.role==='OWNER'&&tab==='EMAIL'&&<MailActivity/>}
   <div hidden={tab!=='ENQUIRIES'}>
   {notice&&<p className="panel" role="status">{notice}</p>}<OwnerActionCentre items={items} onOpen={lead=>{setSelected(lead);setDetailOpen(true);setError('');}}/>
   {adding&&<form onSubmit={add} className="panel staff-new"><h2>Add an enquiry</h2><p>Record a walk-in, call or message. Channel selection records the source; no message is sent.</p><div className="form-grid">
    <label className="field">Customer name<input required maxLength={140} value={newLead.customerName} onChange={e=>setNewLead({...newLead,customerName:e.target.value})}/></label>
    <label className="field">Enquiry source<select value={source} onChange={e=>setSource(e.target.value)}>{sources.map(s=><option key={s}>{s}</option>)}</select></label>
    <label className="field">Customer email<input required type="email" maxLength={254} value={newLead.email} onChange={e=>setNewLead({...newLead,email:e.target.value})}/></label>
    <label className="field">Customer phone<input required minLength={7} maxLength={32} value={newLead.phone} onChange={e=>setNewLead({...newLead,phone:e.target.value})}/></label>
    <label className="field">Requested product<select value={newLead.productCode} onChange={e=>setNewLead({...newLead,productCode:e.target.value})}>{PRODUCTS_FALLBACK.filter(p=>['WEDDING_CARDS','HOTEL_PRINT','BROCHURES','OFFSET_GLOSS'].includes(p.code)).map(p=><option key={p.code} value={p.code}>{p.name}</option>)}</select></label>
    <label className="field">Requested quantity<input required type="number" min={1} max={10000000} value={newLead.quantity} onChange={e=>setNewLead({...newLead,quantity:Number(e.target.value)})}/></label>
    <label className="field field-wide">Customer requirements<textarea rows={3} maxLength={2000} value={newLead.notes} onChange={e=>setNewLead({...newLead,notes:e.target.value})}/></label>
   </div><button className="btn btn-ink" disabled={busy}>Save enquiry</button></form>}
   {created&&<div className="panel staff-new" role="status"><h2>Enquiry created</h2><p>Save these tracking credentials before dismissing. Share them only with the customer.</p><div className="credential"><span>Enquiry ID</span><code>{created.lead.publicId}</code></div><div className="credential"><span>Tracking token</span><code className="break-anywhere">{created.trackingToken}</code></div><button className="btn btn-outline btn-small" onClick={()=>setCreated(null)}>Dismiss credentials</button></div>}
   <div className="owner-table-wrap"><div className="owner-table-top"><div><h2>Enquiry queue</h2><p>Latest 100 visible enquiries. Owner/designers accept new intake. Sales sees assigned reviewed enquiries only.</p></div><div className="staff-filters"><label className="field">Search enquiries<input value={search} onChange={e=>setSearch(e.target.value)} placeholder="Name, contact or product"/></label><label className="field">Queue filter<select value={filter} onChange={e=>setFilter(e.target.value)}><option value="ALL">All visible</option><option value="OVERDUE">Overdue</option><option value="UNASSIGNED">Unassigned</option><option value="MINE">Assigned to me</option></select></label><label className="field">Status filter<select aria-label="Status filter" value={statusFilter} onChange={e=>setStatusFilter(e.target.value)}>{[["ALL","All statuses"],["NEW","New intake"],["REVIEWING","Preparing quotation"],["QUOTED","Quotation sent"],["IN_PROGRESS","Live work"],["CLOSED","Closed"]].map(([v,label])=><option key={v} value={v}>{label}</option>)}</select></label></div></div>
    <div className="row-actions">{staff.role==='OWNER'&&<><label className="quote-confirm"><input type="checkbox" aria-label="Select all shown enquiries" checked={visible.length>0&&visible.every(l=>checked.includes(l.id))} onChange={e=>setChecked(e.target.checked?visible.map(l=>l.id):[])}/> Select shown enquiries</label><button className="btn btn-outline" disabled={busy||!checked.length} onClick={dismissSelected}>Delete selected ({checked.length})</button></>}</div><div className="table-scroll"><table className="owner-table"><thead><tr>{staff.role==='OWNER'&&<th>Select</th>}<th>Customer</th><th>Print request</th><th>Status</th><th>Next follow-up</th><th>Assignment</th><th>Action</th></tr></thead><tbody>{visible.map(lead=><tr key={lead.id}>{staff.role==='OWNER'&&<td><input type="checkbox" aria-label={'Select enquiry '+lead.publicId} checked={checked.includes(lead.id)} onChange={e=>setChecked(e.target.checked?[...checked,lead.id]:checked.filter(id=>id!==lead.id))}/></td>}<td><strong>{lead.customerName}</strong><small>{lead.email}<br/>{lead.phone}</small></td><td>{PRODUCT_NAME(lead.productCode)}<small>{lead.quantity.toLocaleString()} copies · {lead.source}</small></td><td>{lead.status}<small>{lead.priority} priority</small></td><td>{lead.followUpAt?<><strong className={overdue(lead)?'staff-overdue':''}>{date(lead.followUpAt)}</strong><small>{lead.nextAction}</small></>:'No follow-up scheduled'}</td><td>{lead.assignedStaffId===staff.id?'Me':lead.assignedStaffId?'Another staff member':'Unassigned'}</td><td><button className="btn btn-outline btn-small" onClick={()=>{setSelected(lead);setDetailOpen(true);setError('');}}>Open enquiry</button>{staff.role==='OWNER'&&<button className="text-button" disabled={busy} title="Archive this enquiry and prepare a customer explanation" onClick={()=>dismiss(lead)}>Delete enquiry</button>}</td></tr>)}</tbody></table></div>
    {items.length>=100&&<button className="btn btn-outline" disabled={busy} onClick={async()=>{try{const rows=await api<Lead[]>(queuePath(items[items.length-1].id));setItems(old=>[...old,...rows.filter(x=>!old.some(y=>y.id===x.id))]);}catch(e){failure(e);}}}>Load more enquiries</button>}
    {visible.length===0&&<div className="empty-state"><strong>No matching enquiries</strong><p>Adjust the filters or add an enquiry.</p></div>}
   </div>
   {selected&&detailOpen&&<Modal title="Enquiry details" closeLabel="Close enquiry details" onClose={()=>setDetailOpen(false)}><section className="staff-detail"><div className="staff-toolbar"><div><h2>{selected.customerName}</h2><p>{PRODUCT_NAME(selected.productCode)} · {selected.quantity.toLocaleString()} copies</p></div>{(staff.role==='OWNER'||staff.contactsAccess)&&<button className="btn btn-outline btn-small" onClick={()=>{setDetailOpen(false);setTab('CONTACTS');}}>Link a customer contact</button>}</div>
    {staff.role==='OWNER'&&selected.status!=='NEW'&&<details className="workflow-history"><summary>Responsibility and assignment</summary><OwnerAssignment lead={selected} changed={updated=>{setSelected(updated);setItems(old=>old.map(x=>x.id===updated.id?updated:x));}}/></details>}
    {selected.notes&&<p className="staff-notes">{selected.notes}</p>}
    {selected.status==='NEW'?<section className="panel"><h3>Incoming enquiry review</h3><p>Accept this enquiry before quotation, follow-up or design work becomes available.</p><IntakeActions leadId={selected.id} revision={selected.revision} owner onDone={async()=>{const next=await refresh();setSelected(next.find(l=>l.id===selected.id)||null);}}/></section>:<>
    {!owned?<div className="staff-toolbar"><p>Claim this enquiry before updating its status or follow-up.</p><button disabled={busy} className="btn btn-ink" onClick={()=>change('assignment',{claim:true})}>Assign to me</button></div>:<>
     <div className="staff-toolbar"><div><strong>Enquiry status: {selected.status.replaceAll('_',' ')}</strong><p className="hint">Sending a quotation sets Quoted. Acceptance moves it to In progress.</p></div><button disabled={busy} className="btn btn-outline btn-small" onClick={()=>change('assignment',{claim:false})}>Return to unassigned</button></div>
     <details className="workflow-history"><summary>Follow-up reminder</summary><form onSubmit={saveFollowUp}><fieldset className="staff-fieldset" disabled={busy||selected.status==='CLOSED'}><legend>Follow-up plan</legend><div className="form-grid"><label className="field">Priority<select value={priority} onChange={e=>setPriority(e.target.value as Lead['priority'])}><option>LOW</option><option>NORMAL</option><option>HIGH</option></select></label><label className="field">Follow-up time (your local time)<input type="datetime-local" value={due} onChange={e=>setDue(e.target.value)}/></label><label className="field field-wide">Next action<textarea maxLength={500} rows={2} value={action} onChange={e=>setAction(e.target.value)}/></label></div><p className="hint">Provide a next action and time together. Clear both to remove the reminder. Reminders appear in this workspace.</p><button className="btn btn-ink">Save follow-up</button></fieldset></form></details>
    </>}
    <EnquiryStages lead={selected} owner={staff.role==='OWNER'} canEdit={owned||staff.role==='OWNER'} onChanged={async()=>{const leads=await refresh();setSelected(leads.find(x=>x.id===selected.id)||null);}}/>{(owned||staff.role==='OWNER')&&selected.status!=='CLOSED'&&<EnquiryResolution owner={staff.role==='OWNER'} lead={selected} onChanged={async()=>{await refresh();setDetailOpen(false);setSelected(null);}}/>}<details className="workflow-history"><summary>Activity log · {activities.length} updates</summary>{activities.length>0?<ol className="staff-activity">{activities.map(activity=><li key={activity.id}><strong>{activity.action.replaceAll('_',' ')}</strong><small>{date(activity.createdAt)}</small><p>{activity.details}</p></li>)}</ol>:<p>No staff activity recorded yet.</p>}</details>
    </>}{error&&<p className="form-error" role="alert">{error}</p>}
   </section></Modal>}
   </div>
  </>}
  {staff&&profileOpen&&<Modal title="Profile & security" onClose={()=>setProfileOpen(false)}><div className="profile-summary"><span className="account-avatar">{staff.username.charAt(0).toUpperCase()}</span><h2>{staff.username}</h2><p>{staff.role} account</p><AppearanceEditor seed={staff.username} publicName={staff.role==='DESIGNER'}/>{staff.role==='OWNER'&&<p>{staff.mfaRequired?'Complete owner verification to continue.':'Owner MFA is enabled for this session.'}</p>}<button className="btn btn-ink" onClick={()=>{setProfileOpen(false);setStaff({...staff,mustChangePassword:true});}}>Change password</button></div></Modal>}
  {error&&!detailOpen&&<p className="form-error staff-error" role="alert">{error}</p>}
 </div></section>;
}
