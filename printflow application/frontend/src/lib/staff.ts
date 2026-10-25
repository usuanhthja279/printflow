import {api,EnquiryInput,EnquiryStatus} from './api';
export type StaffSession={id:string;username:string;role:string;mustChangePassword:boolean;mfaEnrolled:boolean;mfaRequired:boolean;mfaRecent:boolean;contactsAccess?:boolean};
export type Lead={id:string;publicId:string;customerName:string;email:string;phone:string;productCode:string;quantity:number;notes:string|null;status:EnquiryStatus;source:string;priority:'LOW'|'NORMAL'|'HIGH';assignedStaffId:string|null;nextAction:string|null;followUpAt:string|null;revision:number;createdAt:string;requestedDate?:string|null;closureKind?:string|null;closureMessage?:string|null};
export type LeadActivity={id:string;actorId:string;action:string;details:string;createdAt:string};
type Csrf={token:string;headerName:string};
export async function staffMutation<T>(path:string,method:string,body?:unknown,extraHeaders:Record<string,string>={}):Promise<T>{
 const csrf=await api<Csrf>('/v1/auth/csrf');
 const result=await api<T>(path,{method,credentials:'same-origin',headers:{[csrf.headerName]:csrf.token,...extraHeaders},...(body===undefined?{}:{body:JSON.stringify(body)})});
 if(path.startsWith('/v1/auth/'))window.dispatchEvent(new Event('jdprintex:session'));
 return result;
}
export async function signIn(username:string,password:string){
 const csrf=await api<Csrf>('/v1/auth/csrf');
 await api('/v1/auth/session',{method:'POST',credentials:'same-origin',headers:{'Content-Type':'application/x-www-form-urlencoded',[csrf.headerName]:csrf.token},body:new URLSearchParams({username,password}).toString()});
 const user=await api<StaffSession>('/v1/auth/session');window.dispatchEvent(new Event('jdprintex:session'));return user;
}
export const createStaffEnquiry=(enquiry:EnquiryInput,source:string)=>staffMutation<{lead:Lead;trackingToken:string}>('/v1/staff/enquiries','POST',{enquiry,source});
