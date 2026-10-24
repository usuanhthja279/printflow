"use client";
import {useEffect,useRef,useState} from 'react';import Script from 'next/script';
type Turnstile={render:(element:HTMLElement,options:Record<string,unknown>)=>string;remove:(id:string)=>void};
declare global {interface Window {turnstile?:Turnstile}}
export default function GuestVerification({siteKey,tokenChanged}:{siteKey:string;tokenChanged:(token:string)=>void}){
 const target=useRef<HTMLDivElement>(null),[ready,setReady]=useState(false);const callback=useRef(tokenChanged);callback.current=tokenChanged;
 useEffect(()=>{if(!ready||!target.current||!window.turnstile)return;const api=window.turnstile;const id=api.render(target.current,{sitekey:siteKey,action:'enquiry',callback:(value:string)=>callback.current(value),'expired-callback':()=>callback.current(''),'error-callback':()=>callback.current('')});return()=>{api.remove(id);};},[siteKey,ready]);
 return <div><Script src="https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit" onReady={()=>setReady(true)}/><div ref={target}/><p>If verification is unavailable, contact J.D.Printex through your established contact channel so staff can record the enquiry for you.</p></div>;
}
