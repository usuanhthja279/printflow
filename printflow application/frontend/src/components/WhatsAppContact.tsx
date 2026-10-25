'use client';
import {useEffect,useState} from 'react';
import {api} from '@/lib/api';
type Contact={available:boolean;number:string;url:string};
export default function WhatsAppContact({label='Ask us on WhatsApp',product}:{label?:string;product?:string}){
 const [contact,setContact]=useState<Contact|null>(null);
 useEffect(()=>{let active=true;api<Contact>('/public/whatsapp/contact').then(x=>{if(active)setContact(x);}).catch(()=>{if(active)setContact(null);});return()=>{active=false;}},[]);
 if(!contact?.available)return null;
 const text=product?`Hello J.D.Printex, I have a question about ${product}.`:'Hello J.D.Printex, I have a printing question.';
 return <a className="btn btn-outline" href={`${contact.url}?text=${encodeURIComponent(text)}`} target="_blank" rel="noreferrer">{label}<span aria-hidden> ↗</span></a>;
}
