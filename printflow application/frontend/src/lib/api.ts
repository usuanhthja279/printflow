export type Product = {code:string;name:string;description:string};
export type EnquiryStatus='NEW'|'REVIEWING'|'QUOTED'|'IN_PROGRESS'|'CLOSED';
export type CreatedEnquiry={publicId:string;trackingToken:string;status:string;createdAt:string};
export type EventView={status:string;at:string};
export type TrackingView={publicId:string;productCode:string;quantity:number;status:string;createdAt:string;events:EventView[];closureKind?:string|null;closureMessage?:string|null};
export type DemoEnquiry={id:string;publicId:string;customerName:string;email:string;phone:string;productCode:string;quantity:number;size:string|null;paperGsm:number|null;printSides:string|null;finish:string|null;notes:string|null;status:EnquiryStatus;createdAt:string};
export type EnquiryInput={customerName:string;email:string;phone:string;productCode:string;quantity:number;size:string;paperGsm:number|null;printSides:string;finish:string;notes:string;requestedDate?:string};

export class ApiError extends Error {
 constructor(message:string,public readonly status:number){super(message);this.name='ApiError';}
}
export async function api<T>(path:string, init?:RequestInit):Promise<T>{
  const response=await fetch(`/api${path}`,{cache:'no-store',...init,headers:{'Accept':'application/json',...(init?.body?{'Content-Type':'application/json'}:{}),...init?.headers}});
  if(!response.ok){
    let message=`Request failed (${response.status})`;
    try{const data=await response.json();if(data.message)message=data.message;}catch{}
    throw new ApiError(message,response.status);
  }
  return await response.json() as T;
}
export const PRODUCTS_FALLBACK:Product[]=[
 {code:'WEDDING_CARDS',name:'Shaadi & Wedding Cards',description:'Wedding invitations and celebration stationery'},
 {code:'HOTEL_PRINT',name:'Hotel Print Materials',description:'Menus, table cards and other hotel printing requirements'},
 {code:'OFFSET_GLOSS',name:'Offset Gloss Printing',description:'Custom offset print enquiries with gloss finishing requirements'},
 {code:'BROCHURES',name:'Pamphlets & Leaflets',description:'Promotional pamphlets, leaflets and brochures'},
 {code:'CARDS',name:'Business Cards',description:'Identity, premium stationery & networking'},
 {code:'BOOKS',name:'Books & Catalogues',description:'Perfect-bound books, reports & catalogues'},
 {code:'PACKAGING',name:'Packaging',description:'Boxes, sleeves & retail packaging'},
 {code:'CUSTOM',name:'Custom Job',description:'Unique requirements and special projects'},
];
export const PRODUCT_NAME=(code:string)=>PRODUCTS_FALLBACK.find(p=>p.code===code)?.name||code;
