"use client";
import {useState} from 'react';import Link from 'next/link';import PrintPreview from './PrintPreview';
const families=[['OFFSET_GLOSS','Business'],['WEDDING_CARDS','Celebrations'],['HOTEL_PRINT','Hospitality'],['BROCHURES','Promotional']];
export default function PrintStudio(){const [code,setCode]=useState('WEDDING_CARDS');return <div className="print-studio"><div className="studio-tabs" aria-label="Explore print families">{families.map(([value,label])=><button key={value} aria-pressed={code===value} onClick={()=>setCode(value)}>{label}</button>)}</div><PrintPreview code={code} interactive/><Link className="studio-link" href={'/products/'+code}>Explore this print family <span>↗</span></Link></div>}
