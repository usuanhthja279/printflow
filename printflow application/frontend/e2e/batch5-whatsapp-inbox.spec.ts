import {test,expect,Page} from '@playwright/test';
import fs from 'node:fs';import path from 'node:path';
const id='55555555-1111-4222-8333-444444444444',root=path.resolve(process.cwd(),'../output/batch5-visual-evidence');
type Message={id:string;direction:string;type:string;text:string;status:string;at:string;providerErrorCode:string|null};
type Conversation={id:string;phone:string;contactName:string;handoffStatus:string;assignedStaffId:string|null;suppressed:boolean;lastInboundAt:string;updatedAt:string;lastMessage:string;messages:Message[]};
async function shot(page:Page,name:string){fs.mkdirSync(root,{recursive:true});await page.screenshot({path:path.join(root,name),fullPage:false});}

test('synthetic owner inbox covers handoff, replies, simulator and opt-out',async({page},info)=>{
 const now=new Date().toISOString();let selected:Conversation={id,phone:'+919000000128',contactName:'Synthetic print customer',handoffStatus:'BOT',assignedStaffId:null,suppressed:false,lastInboundAt:now,updatedAt:now,lastMessage:'I need help with my wedding cards.',messages:[{id:'in-1',direction:'INBOUND',type:'text',text:'I need help with my wedding cards.',status:'RECEIVED',at:now,providerErrorCode:null}]};
 await page.route('**/api/**',async route=>{const req=route.request(),url=new URL(req.url()),p=url.pathname.replace(/^\/api/,'');const send=(body:unknown,status=200)=>route.fulfill({status,json:body});
  if(req.method()==='GET'){
   if(p==='/v1/auth/session')return send({id:'synthetic-owner',username:'owner@example.test',role:'OWNER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true});
   if(p==='/v1/auth/csrf')return send({token:'synthetic-csrf',headerName:'X-CSRF-TOKEN'});
   if(p==='/v1/owner/whatsapp/simulator')return send({enabled:true});
   if(p.startsWith('/v1/staff/enquiries'))return send([]);
   if(p==='/v1/staff/whatsapp')return send([{...selected,messages:[]}]);
   if(p===`/v1/staff/whatsapp/${id}`)return send({...selected});
   return send([]);
  }
  if(p===`/v1/staff/whatsapp/${id}/claim`){selected={...selected,handoffStatus:'HUMAN',assignedStaffId:'synthetic-owner',updatedAt:new Date().toISOString()};return send(selected);}
  if(p===`/v1/staff/whatsapp/${id}/resume`){selected={...selected,handoffStatus:'BOT',assignedStaffId:null,updatedAt:new Date().toISOString()};return send({status:'resumed'});}
  if(p===`/v1/staff/whatsapp/${id}/reply`){const body=req.postDataJSON();const msg:Message={id:'out-'+selected.messages.length,direction:'OUTBOUND',type:'text',text:body.text,status:'QUEUED',at:new Date().toISOString(),providerErrorCode:null};selected={...selected,messages:[...selected.messages,msg],lastMessage:msg.text,updatedAt:msg.at};return send({status:'queued'},202);}
  if(p==='/v1/owner/whatsapp/simulate'){const input=req.postDataJSON();const inbound:Message={id:'sim-in',direction:'INBOUND',type:'text',text:input.text,status:'RECEIVED',at:new Date().toISOString(),providerErrorCode:null};const stopped=input.text==='STOP';const outbound:Message={id:'sim-out',direction:'OUTBOUND',type:'text',text:stopped?'You are unsubscribed from business-initiated WhatsApp updates.':'Welcome to J.D.Printex. Reply with a menu number to continue.',status:'QUEUED',at:new Date().toISOString(),providerErrorCode:null};selected={...selected,suppressed:stopped,handoffStatus:stopped?'HUMAN':'BOT',messages:[...selected.messages,inbound,outbound],lastMessage:outbound.text,updatedAt:outbound.at};return send({status:'accepted'},202);}
  return send({message:'Unexpected synthetic mutation'},400);
 });
 await page.goto('/business');await page.getByRole('button',{name:'WHATSAPP',exact:true}).click();await expect(page.getByRole('heading',{name:'WhatsApp inbox'})).toBeVisible();await expect(page.getByRole('button',{name:/Synthetic print customer/})).toBeVisible();await shot(page,`whatsapp-inbox-${info.project.name}.png`);
 await page.getByRole('button',{name:/Synthetic print customer/}).click();await expect(page.getByRole('heading',{name:'Synthetic print customer'})).toBeVisible();await expect(page.getByRole('region',{name:'Conversation details'}).getByText('I need help with my wedding cards.')).toBeVisible();await shot(page,`whatsapp-conversation-${info.project.name}.png`);
  const conversation=page.getByRole('region',{name:'Conversation details'});await page.getByRole('button',{name:'Take over'}).click();await expect(conversation.getByText('Staff handling')).toBeVisible();await page.getByLabel('Reply to customer').fill('Our team can help with the card specifications.');await page.getByRole('button',{name:'Queue reply'}).click();await expect(conversation.getByText('Our team can help with the card specifications.')).toBeVisible();await shot(page,`whatsapp-human-handoff-${info.project.name}.png`);
  await page.getByRole('button',{name:'Return to assistant'}).click();await expect(conversation.getByText('Assistant handling')).toBeVisible();await page.getByText('Local simulator · no provider messages are sent').click();await page.getByLabel('Synthetic phone number').fill('919000000128');await page.getByLabel('Simulated inbound message').fill('STOP');await page.getByRole('button',{name:'Simulate inbound'}).click();await expect(conversation.getByText('This contact opted out. Business-initiated replies are blocked.')).toBeVisible();await expect(page.getByLabel('Reply to customer')).toBeDisabled();await expect(conversation.getByText(/unsubscribed from business-initiated WhatsApp updates/)).toBeVisible();await shot(page,`whatsapp-opt-out-${info.project.name}.png`);
 if(info.project.name==='mobile')expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);
});
