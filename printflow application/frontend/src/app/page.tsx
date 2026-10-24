import Link from 'next/link';import PrintStudio from '@/components/PrintStudio';import PrintPreview from '@/components/PrintPreview';
const categories=[
 {tag:'01',code:'WEDDING_CARDS',symbol:'▦',title:'Shaadi & wedding cards',desc:'Invitations for your celebration. Share your design, quantity and preferred finish.',color:'mint'},
 {tag:'02',code:'HOTEL_PRINT',symbol:'▤',title:'Hotel print materials',desc:'Menus, table cards and print requirements for your hotel or hospitality business.',color:'peach'},
 {tag:'03',code:'BROCHURES',symbol:'▧',title:'Pamphlets & leaflets',desc:'Promotional printing to put your business, offers and events in people’s hands.',color:'lilac'},
 {tag:'04',code:'OFFSET_GLOSS',symbol:'▣',title:'Offset gloss printing',desc:'Tell us about your offset print project and the gloss finish you have in mind.',color:'yellow'},
];
export default function Home(){
 return <>
  <section className="hero studio-hero"><div className="container hero-grid"><div className="hero-copy">
   <div className="eyebrow"><span className="eyebrow-dot"/> WELCOME TO J.D.PRINTEX</div>
   <h1>Your ideas.<br/><em>Beautifully printed.</em></h1>
   <p>From a first invitation to your next business launch. Explore the possibilities, shape your brief and review every detail before print.</p>
   <div className="hero-actions"><Link className="btn btn-accent" href="/request-quote">Get a print quote <span aria-hidden>↗</span></Link><Link className="inline-link" href="/track">Already enquired? Track here →</Link></div>
   <div className="hero-note"><span className="circle-check">✓</span> From your first idea to your final proof</div>
  </div><PrintStudio/></div></section>
  <section id="services" className="section services-section"><div className="container">
   <div className="section-header"><div><p className="overline">WHAT WE PRINT</p><h2>From everyday essentials<br/>to the extraordinary.</h2></div><p>Whether it&apos;s a simple business card or a large print run, everything begins with your requirements.</p></div>
   <div className="product-grid visual-product-grid">{categories.map(c=><Link key={c.tag} href={`/products/${c.code}`} className="visual-product-card"><div className="product-top"><span>{c.tag} / PRINT</span><span>↗</span></div><PrintPreview code={c.code}/><div><h3>{c.title}</h3><p>{c.desc}</p></div></Link>)}</div>
  </div></section>
  <section id="process" className="section process-section"><div className="container"><div className="section-header"><div><p className="overline">YOUR CONNECTED PROJECT</p><h2>Clear from start<br/>to finish.</h2></div><p>Keep the quotation, artwork feedback, verified payments and recorded delivery progress together in your project workspace.</p></div>
   <div className="steps-grid"><div><span className="step-index">01</span><h3>Share your idea</h3><p>Pick a product, add quantity and details, and tell us about your artwork.</p></div><div><span className="step-index">02</span><h3>Review together</h3><p>Receive a quote, exchange changes and approve the final design.</p></div><div><span className="step-index">03</span><h3>Print with confidence</h3><p>Follow the progress as your order moves through production and delivery.</p></div></div>
  </div></section>
  <section className="cta-section"><div className="container"><div className="cta-box"><div><p className="overline">LET'S MAKE SOMETHING</p><h2>Have a project in mind?</h2><p>Start a print enquiry. Bring a reference image, a print-ready design, or simply an idea.</p></div><Link href="/request-quote" className="btn btn-white">Start your enquiry ↗</Link></div></div></section>
 </>;
}
