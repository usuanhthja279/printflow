import ProductDetail from '@/components/ProductDetail';
export default async function Page({params}:{params:Promise<{code:string}>}){const {code}=await params;return <ProductDetail code={code}/>;}
