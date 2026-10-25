"use client";
import {useEffect,useRef,useState} from 'react';
import type {PDFDocumentProxy,PDFDocumentLoadingTask,PDFWorker,RenderTask} from 'pdfjs-dist';
export type PdfPoint={x:number;y:number;page:number};
type Pin={id?:string;x:number;y:number;page:number;body:string};type RemoteCursor={label:string;proofRevisionId:string;page:number;x:number;y:number};
export default function ProofPdf({src,pins=[],onPin,onPinSelect,focusPage,focusSequence,point,onReviewed,onView,onCursor,onCursorLeave,remoteCursors=[]}:{src:string;pins?:Pin[];onPin?:(p:PdfPoint)=>void;onPinSelect?:(commentId?:string)=>void;focusPage?:number;focusSequence?:number;point?:PdfPoint|null;onReviewed?:(pages:number[],count:number)=>void;onView?:(page:number)=>void;onCursor?:(point:PdfPoint)=>void;onCursorLeave?:()=>void;remoteCursors?:RemoteCursor[]}){
 const cancel=useRef<()=>void>(()=>{});
 const onViewRef=useRef(onView);onViewRef.current=onView;
 const canvas=useRef<HTMLCanvasElement>(null),frame=useRef<HTMLDivElement>(null),callbacks=useRef({onReviewed});callbacks.current={onReviewed};
 const [doc,setDoc]=useState<PDFDocumentProxy|null>(null),[page,setPage]=useState(1),[zoom,setZoom]=useState(1),[ready,setReady]=useState(false),[error,setError]=useState(''),[retry,setRetry]=useState(0),[reviewed,setReviewed]=useState<number[]>([]),[width,setWidth]=useState(600),[pageSize,setPageSize]=useState({width:0,height:0});
 useEffect(()=>{if(!frame.current)return;const observer=new ResizeObserver(entries=>setWidth(entries[0].contentRect.width));observer.observe(frame.current);return()=>observer.disconnect();},[error]);
 useEffect(()=>{let active=true,task:PDFDocumentLoadingTask|undefined,worker:PDFWorker|undefined,nativeWorker:Worker|undefined;const abort=new AbortController();const stop=()=>{abort.abort();void task?.destroy();worker?.destroy();nativeWorker?.terminate();};cancel.current=stop;setDoc(null);setPage(1);setZoom(1);setReviewed([]);setError('');setReady(false);callbacks.current.onReviewed?.([],0);
  const timeout=setTimeout(()=>{abort.abort();void task?.destroy();nativeWorker?.terminate();if(active){setError('PDF preview timed out. Reopen it or ask the designer to export a simpler PDF.');callbacks.current.onReviewed?.([],0);}},30000);
  (async()=>{const pdfjs=await import('pdfjs-dist');const response=await fetch(src,{credentials:'same-origin',cache:'no-store',signal:abort.signal});if(!response.ok)throw Error('Preview unavailable. Check that this PDF passed scanning and is shared with you.');const bytes=new Uint8Array(await response.arrayBuffer());if(bytes.length>20*1024*1024)throw Error('PDF exceeds the preview limit.');if(!active)return;nativeWorker=new Worker('/pdfjs/pdf.worker.min.mjs',{type:'module'});worker=pdfjs.PDFWorker.create({port:nativeWorker});task=pdfjs.getDocument({data:bytes,worker,enableXfa:false,stopAtErrors:true,canvasMaxAreaInBytes:16*1024*1024,cMapUrl:'/pdfjs/cmaps/',standardFontDataUrl:'/pdfjs/standard_fonts/',wasmUrl:'/pdfjs/wasm/',iccUrl:'/pdfjs/iccs/',verbosity:0});const loaded=await task.promise;if(loaded.numPages<1||loaded.numPages>100)throw Error('Preview supports PDFs with 1–100 pages.');if(active){clearTimeout(timeout);setDoc(loaded);callbacks.current.onReviewed?.([],loaded.numPages);}})().catch(e=>{clearTimeout(timeout);if(active&&!abort.signal.aborted){setError(e instanceof Error?e.message:'PDF preview unavailable.');callbacks.current.onReviewed?.([],0);void task?.destroy();nativeWorker?.terminate();}});
  return()=>{active=false;clearTimeout(timeout);stop();};
 },[src,retry]);
 useEffect(()=>{if(doc&&focusPage&&focusPage<=doc.numPages)setPage(focusPage);},[doc,focusPage,focusSequence]);
 useEffect(()=>{if(ready)onViewRef.current?.(page);},[ready,page]);
 useEffect(()=>{if(!doc||!canvas.current||error)return;let active=true,render:RenderTask|undefined;setReady(false);setPageSize({width:0,height:0});const timer=setTimeout(()=>{render?.cancel();cancel.current();if(active){setError('This page could not be rendered safely. Ask the designer for a simpler PDF.');callbacks.current.onReviewed?.([],0);}},30000);
  (async()=>{const sheet=await doc.getPage(page);if(!active||!canvas.current)return;const natural=sheet.getViewport({scale:1}),scale=Math.min(Math.max(160,width-24)/natural.width,1.4)*zoom;const viewport=sheet.getViewport({scale:Math.min(scale,Math.sqrt(4000000/(natural.width*natural.height)))}),ratio=Math.min(window.devicePixelRatio||1,2,Math.sqrt(4000000/(viewport.width*viewport.height))),target=canvas.current;setPageSize({width:viewport.width,height:viewport.height});target.width=Math.max(1,Math.floor(viewport.width*ratio));target.height=Math.max(1,Math.floor(viewport.height*ratio));target.style.width=viewport.width+'px';target.style.height=viewport.height+'px';render=sheet.render({canvas:target,viewport,transform:ratio===1?undefined:[ratio,0,0,ratio,0,0],background:'#ffffff'});await render.promise;if(active){clearTimeout(timer);setReady(true);}})().catch(e=>{clearTimeout(timer);if(active&&e?.name!=='RenderingCancelledException'){cancel.current();setError('This PDF page could not be displayed. Ask the designer to export it again.');callbacks.current.onReviewed?.([],0);}});
  return()=>{active=false;clearTimeout(timer);render?.cancel();};
 },[doc,page,zoom,width,error]);
 return <section className="pdf-review" aria-label="PDF artwork preview">
  <div className="pdf-toolbar">
   <button type="button" className="btn btn-outline btn-small" disabled={!doc||page===1} onClick={()=>setPage(n=>n-1)}>Previous page</button>
   <label>PDF page <select aria-label="PDF page" disabled={!doc} value={page} onChange={e=>setPage(Number(e.target.value))}>{Array.from({length:doc?.numPages||1},(_,i)=><option key={i} value={i+1}>{i+1}</option>)}</select></label>
   <span>of {doc?.numPages||'…'}</span>
   <button type="button" className="btn btn-outline btn-small" disabled={!doc||page===doc.numPages} onClick={()=>setPage(n=>n+1)}>Next page</button>
   <label>Zoom <select aria-label="PDF zoom" value={zoom} onChange={e=>setZoom(Number(e.target.value))}>{[.75,1,1.5,2].map(n=><option key={n} value={n}>{n===1?'Fit width':n*100+'%'}</option>)}</select></label>
  </div>
  {error ? <div><p role="alert">{error}</p><button type="button" className="btn btn-outline" onClick={()=>setRetry(n=>n+1)}>Retry PDF preview</button></div> : <>
   <div className="pdf-review-layout">
    <nav className="pdf-page-rail" aria-label="Document pages">{Array.from({length:doc?.numPages||0},(_,i)=><button type="button" key={i} aria-label={'View page '+(i+1)} aria-current={page===i+1?'page':undefined} onClick={()=>setPage(i+1)}><span aria-hidden="true">{String(i+1).padStart(2,'0')}</span><small>Page {i+1}{reviewed.includes(i+1)?' · Reviewed':''}</small></button>)}</nav>
    <div className="pdf-rendered-page"><p aria-live="polite">{ready?'Page '+page+' of '+doc?.numPages:'Loading PDF page…'}</p><div className="pdf-page-scroll" ref={frame}>
     <div className="pdf-page-stage" style={{width:pageSize.width||undefined,height:pageSize.height||undefined}}>
      <button type="button" className="pdf-page-canvas" style={{width:pageSize.width||undefined,height:pageSize.height||undefined,minHeight:0}} aria-label={'PDF page '+page+(onPin?' — mark a change':' preview')} disabled={!ready} onPointerMove={e=>{if(!ready||!onCursor)return;const r=e.currentTarget.getBoundingClientRect();onCursor({page,x:Math.max(0,Math.min(1,(e.clientX-r.left)/r.width)),y:Math.max(0,Math.min(1,(e.clientY-r.top)/r.height))});}} onPointerLeave={onCursorLeave} onClick={e=>{if(!onPin||!ready)return;const rect=e.currentTarget.getBoundingClientRect();onPin({page,x:e.detail===0?0.5:Math.max(0,Math.min(1,(e.clientX-rect.left)/rect.width)),y:e.detail===0?0.5:Math.max(0,Math.min(1,(e.clientY-rect.top)/rect.height))});}}><canvas ref={canvas} aria-label={'Artwork page '+page}/></button>
      <div className="pdf-page-annotation-layer" aria-label={'Annotations on page '+page} style={{width:pageSize.width||undefined,height:pageSize.height||undefined}}>
       {pins.map((pin,index)=>pin.page===page&&<button type="button" key={pin.id||index} className="proof-pin proof-pin-select" title={pin.body} aria-label={'Go to comment: page '+page+' review pin '+(index+1)+': '+pin.body} style={{left:pin.x*100+'%',top:pin.y*100+'%'}} onClick={event=>{event.stopPropagation();onPinSelect?.(pin.id);}}>{index+1}</button>)}
       {remoteCursors.filter(cursor=>cursor.page===page).map((cursor,index)=><span aria-hidden="true" className="proof-remote-cursor" key={cursor.label+'-'+index} style={{left:cursor.x*100+'%',top:cursor.y*100+'%'}}><span>{cursor.label}</span></span>)}
       {point?.page===page&&<span aria-hidden="true" className="proof-pin pending" style={{left:point.x*100+'%',top:point.y*100+'%'}}>+</span>}
      </div>
     </div>
    </div></div></div>
   {onPin&&<p className="hint">Click a place on this page, then describe the requested change below. Comments stay linked to this page and revision.</p>}
   {onReviewed&&doc&&<div className="pdf-reviewed"><button type="button" className="btn btn-outline btn-small" disabled={!ready||reviewed.includes(page)} onClick={()=>{const next=[...reviewed,page].sort((a,b)=>a-b);setReviewed(next);callbacks.current.onReviewed?.(next,doc.numPages);}}>Mark page {page} reviewed</button><span>{reviewed.length} of {doc.numPages} pages reviewed</span></div>}
  </>}
 </section>;
}
