"use client";
import {useEffect,useState} from 'react';
import {api} from '@/lib/api';
import {Lead,staffMutation} from '@/lib/staff';
export default function OwnerAssignment({lead,changed}:{lead:Lead;changed:(lead:Lead)=>void}){
 const [staff,setStaff]=useState<{id:string;username:string;role:string;enabled:boolean}[]>([]),[target,setTarget]=useState(lead.assignedStaffId||''),[error,setError]=useState(''),[busy,setBusy]=useState(false);
 useEffect(()=>{api<typeof staff>('/v1/owner/staff').then(setStaff).catch(()=>setError('Staff list is unavailable.'));},[]);
 useEffect(()=>{setTarget(lead.assignedStaffId||'');},[lead.id,lead.assignedStaffId]);
 return <div className="assignment-panel"><h3>Owner assignment</h3><label className="field">Responsible staff<select value={target} onChange={e=>setTarget(e.target.value)}><option value="">Unassigned</option>{staff.filter(s=>s.enabled&&['OWNER','SALES'].includes(s.role)).map(s=><option key={s.id} value={s.id}>{s.username}</option>)}</select></label><button className="btn btn-outline" disabled={busy} onClick={async()=>{setBusy(true);setError('');try{changed(await staffMutation<Lead>(`/v1/owner/enquiries/${lead.id}/assignment`,'PATCH',{revision:lead.revision,staffId:target||null}));}catch{setError('Assignment could not be saved. Refresh the enquiry and choose an enabled sales or owner account.');}finally{setBusy(false);}}}>Save assignment</button>{error&&<p role="alert" className="form-error">{error}</p>}</div>;
}
