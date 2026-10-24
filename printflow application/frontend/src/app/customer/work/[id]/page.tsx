import LiveWork from '@/components/LiveWork';
export default async function WorkPage({params}:{params:Promise<{id:string}>}){const {id}=await params;return <LiveWork id={id}/>;}
