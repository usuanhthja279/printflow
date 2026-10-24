import DesignerProjectWorkspace from '@/components/DesignerProjectWorkspace';

export default async function DesignerWorkPage({params}:{params:Promise<{id:string}>}){
 const {id}=await params;
 return <DesignerProjectWorkspace id={id}/>;
}
