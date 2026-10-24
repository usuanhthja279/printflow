import Link from 'next/link';
export default function Footer(){
 return <footer className="site-footer"><div className="container footer-flex">
  <div><strong>J.D.<span style={{fontWeight:400}}>Printex</span></strong><p>Wedding, hospitality and promotional printing.</p></div>
  <div className="footer-link"><Link href="/request-quote">Request a quote</Link><Link href="/track">Track enquiry</Link><Link href="/business">Staff workspace</Link></div>
  <div className="footnote">J.D.Printex · Local development preview · Online checkout uses test payments.</div>
 </div></footer>
}
