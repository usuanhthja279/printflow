'use client';
import {FormEvent,useState} from 'react';
import {api,TrackingView,PRODUCT_NAME} from '@/lib/api';
import {printReference} from '@/lib/references';
export default function TrackPage(){
 const [id,setId]=useState('');const [token,setToken]=useState('');const [data,setData]=useState<TrackingView|null>(null);const [busy,setBusy]=useState(false);const [error,setError]=useState('');
 async function load(e:FormEvent){e.preventDefault();setError('');setData(null);setBusy(true);try{
  setData(await api<TrackingView>(`/public/enquiries/${encodeURIComponent(id.trim())}`,{headers:{'X-Tracking-Token':token.trim()}}));
 }catch(e){setError(e instanceof Error?e.message:'Tracking unavailable');}finally{setBusy(false);}}
 return <section className="page-section"><div className="container compact-container"><div className="page-intro"><p className="overline">STAY IN THE LOOP</p><h1>Track your <em>enquiry.</em></h1><p>Your print journey, from request to quotation.</p></div>
  <form className="panel track-form" onSubmit={load}><label className="field">Enquiry ID<input required value={id} onChange={e=>setId(e.target.value)} placeholder="Paste the enquiry ID"/></label>
   <label className="field">Secure tracking token<input required type="password" autoComplete="off" value={token} onChange={e=>setToken(e.target.value)} placeholder="Paste the token shown after submitting"/></label>
   <div className="form-bottom"><p>Both values were displayed after submitting. They are not emailed in Phase 0.</p><button className="btn btn-ink" disabled={busy}>{busy?'Checking…':'Check status →'}</button></div>
   {error&&<p role="alert" className="form-error">{error}</p>}
  </form>
  {data?.closureMessage&&<p className="panel" role="status">{data.closureMessage}</p>}{data&&<div className="panel tracking-result"><div className="track-result-head"><div><p className="overline">ENQUIRY RECORD</p><h2>{PRODUCT_NAME(data.productCode)}</h2><p>{printReference(data.publicId)} · {data.quantity.toLocaleString()} copies · {new Date(data.createdAt).toLocaleDateString()}</p></div><span className="status-chip">{data.status.replace('_',' ')}</span></div>
   <h3>Timeline</h3><div className="timeline">{data.events.map((event,index)=><div key={index} className="timeline-event"><div className="timeline-dot"/><div><strong>{event.status.replace('_',' ')}</strong><p>{new Date(event.at).toLocaleString()}</p></div></div>)}</div>
  </div>}
 </div></section>;
}
