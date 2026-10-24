"use client";
import {ReactNode,useEffect,useRef,useState} from 'react';import {createPortal} from 'react-dom';
export default function Modal({title,closeLabel='Close dialog',onClose,children,layout='default'}:{title:string;closeLabel?:string;onClose:()=>void;children:ReactNode;layout?:'default'|'studio'|'drawer'}){
 const dialog=useRef<HTMLDialogElement>(null),[mounted,setMounted]=useState(false);const close=useRef(onClose);close.current=onClose;
 useEffect(()=>{setMounted(true);},[]);
 useEffect(()=>{if(!mounted||!dialog.current)return;const element=dialog.current,previous=document.activeElement,overflow=document.body.style.overflow;element.showModal();document.body.style.overflow='hidden';return()=>{element.close();document.body.style.overflow=overflow;if(previous instanceof HTMLElement&&previous.isConnected)previous.focus();};},[mounted]);
 if(!mounted)return null;
 return createPortal(<dialog ref={dialog} className={'workspace-dialog dialog-'+layout} aria-label={title} aria-modal="true" onCancel={event=>{event.preventDefault();close.current();}} onClick={event=>{if(event.target===event.currentTarget){const box=event.currentTarget.getBoundingClientRect();if(event.clientX<box.left||event.clientX>box.right||event.clientY<box.top||event.clientY>box.bottom)close.current();}}}><div className="dialog-heading"><p>{title}</p><button type="button" className="dialog-close" autoFocus aria-label={closeLabel} onClick={onClose}>×</button></div><div className="dialog-content">{children}</div></dialog>,document.body);
}
