import type {Metadata} from 'next';
import Nav from '@/components/Nav';
import Footer from '@/components/Footer';
import './globals.css';
import './experience.css';
import './workflow.css';
import './batch5.css';
export const metadata:Metadata={title:'J.D.Printex | Wedding cards, hotel & offset printing',description:'Explore shaadi cards, hotel print materials, pamphlets, leaflets and offset gloss printing with J.D.Printex. Local development preview.',icons:{icon:'/favicon.svg'}};
export default function RootLayout({children}:{children:React.ReactNode}){
 return <html lang="en"><body><Nav/><main>{children}</main><Footer/></body></html>;
}
