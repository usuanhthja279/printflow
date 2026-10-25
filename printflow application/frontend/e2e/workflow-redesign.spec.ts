import {test,expect,Page} from '@playwright/test';
import path from 'node:path';
import type {CustomerWorkflow} from '../src/components/WorkflowUI';
// UI boundary fixtures only. Every API request is intercepted; no real mail,
// customer mutations or provider calls are performed by these tests.
const id='11111111-2222-4333-8444-555555555555',artifact='art-1';
const evidenceDir=path.resolve(process.cwd(),'../output/batch4-visual-evidence');
async function captureEvidence(page:Page,name:string){await page.screenshot({path:path.join(evidenceDir,name),fullPage:false});}
const work={id,publicId:id,productCode:'WEDDING_CARDS',quantity:500,status:'IN_PROGRESS',liveWork:true,closureMessage:null,stages:{quotation:'Accepted',design:'Customer proof review',payment:'Advance payment due',production:'Not released',delivery:'Not started'},designer:'Sample designer',contact:{email:'',phone:''},mailConnected:false,emails:[{id:'mail-1',kind:'REVIEW',subject:'Your artwork is ready to review',state:'HELD',thread_phase:'WORK',created_at:'2026-10-24T10:00:00Z'}],logs:[{kind:'REQUEST',message:'Enquiry received',created_at:'2026-10-24T09:00:00Z'}],messages:[{id:'m1',author_role:'TEAM',channel:'PORTAL',body:'Please review both pages of your invitation.',created_at:'2026-10-24T10:00:00Z'}]};
const finance={quoteId:'quote-1',quoteVersion:1,totalPaise:200000,paidPaise:0,balancePaise:200000,overpaidPaise:0,stage:'Advance payment due',policy:{revision:1,advance_paise:100000,credit_due:null,reason:'Half advance; remaining balance before dispatch.'},policyCurrent:true,owner:false,customer:true,closed:false,released:false,receipts:[],invoices:[]};
type Options={role?:string;scan?:string;unpaid?:boolean;delivery?:boolean;noPhotos?:boolean;workState?:string};
async function fixture(page:Page,options:Options={}){
 const role=options.role||'CUSTOMER',scan=options.scan||'CLEAN',requests:{path:string;body:Record<string,unknown>}[]=[];
 let job={stage:options.delivery?'READY':'QC',revision:7,operator_id:'staff-1',operator_name:'Sample printer',machine:'Press label A',priority:'URGENT',due_date:'2026-10-30',hold_reason:null as string|null};
 let workflow:CustomerWorkflow={quotationAccepted:true,amendmentState:'NONE',proofDecision:options.unpaid?'APPROVE':'NONE',proofScanState:scan as CustomerWorkflow['proofScanState'],proofForCurrentAgreement:true,paymentCode:'ADVANCE_DUE',fulfilmentState:options.delivery?'READY':'NOT_STARTED',productionStage:options.delivery?'READY':job.stage as CustomerWorkflow['productionStage'],productionHold:false,deliveryIssueOpen:false};
 const proof={id:'proof-1',revision:2,artifact_id:artifact,quote_id:'quote-1',summary:'Synthetic two-page invitation for interface review.',display_name:'invitation.pdf',sha256:'a'.repeat(64),scan_state:scan,decision:options.unpaid?'APPROVE':null,decision_note:null,decided_at:null,current_agreement:true,page_count:2,media_type:'application/pdf'};
 const design={artifacts:[{id:artifact,display_name:'invitation.pdf',byte_size:5000,sha256:proof.sha256,kind:'DESIGN',state:scan,media_type:'application/pdf'}],proofs:[proof],comments:[{id:'comment-1',proof_id:'proof-1',author_role:'CUSTOMER',body:'Check the heading alignment.',page_number:2,anchor_x:.4,anchor_y:.4,created_at:'2026-10-24T10:00:00Z'}],quoteAccepted:true,currentProofApproved:!!options.unpaid,closed:false,designerId:'staff-2',tasks:[],referenceFeedback:[],productCode:'WEDDING_CARDS',referenceEditable:false};
 const photos={required:true,canUpload:!!options.delivery,owner:false,workState:options.workState||'READY',photos:options.noPhotos?[]:[{id:'photo-1',stage:'PICKUP',caption:'Synthetic packing evidence awaiting scanning',display_name:'packing-evidence.png',state:'QUARANTINED',created_at:'2026-10-24T10:00:00Z'}]};
 await page.route('**/api/**',async route=>{
  const req=route.request(),url=new URL(req.url()),p=url.pathname.replace(/^\/api/,'');
  const send=(body:unknown,status=200)=>route.fulfill({status,json:body});
  if(req.method()!=='GET'){
   const body=req.postDataJSON()||{};requests.push({path:p,body});
   if(p.includes('/proofs/')&&p.endsWith('/decision'))return send({message:'The proof changed. Refresh and review the current revision.'},409);
   if(p.startsWith('/v1/production/enquiries/')){
    if(body.revision!==job.revision)return send({message:'Production revision changed; refresh first.'},409);
    if(body.action==='HOLD')job={...job,revision:job.revision+1,hold_reason:String(body.note)};
    else if(body.action==='RESUME')job={...job,revision:job.revision+1,hold_reason:null};
    else return send({message:'Production revision changed; refresh first.'},409);
    return send({owner:role==='OWNER',job,operators:[]});
   }
   if(p.endsWith('/fulfilment/photos/photo-1/scan')){photos.photos[0].state='CLEAN';return send(photos);}
   if(p.endsWith('/fulfilment'))return send({message:options.delivery?'A clean pickup photo is required before dispatch.':'A verified advance is required before production.'},409);
   return send({message:'Unexpected UI mutation in synthetic fixture'},400);
  }
  if(p==='/v1/auth/session')return send({id:'staff-1',username:'sample@example.test',role,mfaRequired:false,mustChangePassword:false,contactsAccess:false});
  if(p==='/v1/auth/csrf')return send({token:'synthetic-csrf',headerName:'X-CSRF-TOKEN'});
  if(p.endsWith('/artifacts/'+artifact+'/preview'))return route.fulfill({path:path.resolve('e2e/fixtures/proof-pages-v1.pdf'),contentType:'application/pdf'});
  if(p.endsWith('/fulfilment/photos/photo-1/preview'))return route.fulfill({path:path.resolve('e2e/fixtures/proof-v1.png'),contentType:'image/png'});
  if(p.endsWith('/work'))return send({...work,workflow,stages:{...work.stages,...(options.unpaid?{design:'Proof approved'}:scan!=='CLEAN'?{design:'Awaiting file clearance'}:{})}});
  if(p.endsWith('/samples')||p.endsWith('/quotes')||p==='/v1/staff/jobs'||p.includes('/activities'))return send([]);
  if(p.endsWith('/payments/gateway'))return send({configured:false,mode:'TEST',webhooksConfigured:false});
  if(p.endsWith('/payments'))return send({...finance,owner:role==='OWNER',customer:role==='CUSTOMER'});
  if(p.endsWith('/fulfilment/photos'))return send(photos);
  if(p.endsWith('/fulfilment'))return send({state:options.workState|| (options.delivery?'READY':role==='OPERATOR'?'PRODUCTION':'NOT_STARTED'),revision:3,owner:role==='OWNER',printer:role==='OPERATOR',delivery:!!options.delivery,closed:false,issue_open:false,productionStage:job.stage,productionHold:job.hold_reason});
  if(p==='/v1/production/queue')return send({owner:role==='OWNER',jobs:[{id,public_id:id,product_code:'WEDDING_CARDS',quantity:500,stage:job.stage,fulfilment:'PRODUCTION',operator_name:job.operator_name,machine:job.machine,priority:job.priority,due_date:job.due_date,hold_reason:job.hold_reason}]});
  if(p.startsWith('/v1/production/enquiries/'))return send({owner:role==='OWNER',job,operators:[]});
  if(p==='/v1/staff/enquiries')return send([{id,publicId:id,customerName:'Synthetic print customer',email:'sample@example.test',phone:'9000000000',productCode:'WEDDING_CARDS',quantity:500,notes:null,status:'NEW',source:'WEBSITE',priority:'HIGH',assignedStaffId:null,nextAction:null,followUpAt:null,revision:0,createdAt:'2026-10-24T09:00:00Z'}]);
  if(p==='/v1/owner/staff'||p==='/v1/staff/jobs/queue')return send([]);
  if(p==='/v1/design/enquiries/'+id)return send(design);
  return send([]);
 });
 return {requests,setWorkflow:(next:Partial<CustomerWorkflow>)=>{workflow={quotationAccepted:true,amendmentState:'NONE',proofDecision:options.unpaid?'APPROVE':'NONE',proofScanState:scan as CustomerWorkflow['proofScanState'],proofForCurrentAgreement:true,paymentCode:'ADVANCE_DUE',fulfilmentState:options.delivery?'READY':'NOT_STARTED',productionStage:options.delivery?'READY':job.stage as CustomerWorkflow['productionStage'],productionHold:false,deliveryIssueOpen:false,...next};}};
}
async function phoneFits(page:Page){await expect.poll(()=>page.evaluate(()=>document.documentElement.scrollWidth<=window.innerWidth+1)).toBe(true);}

test('project next action, conversation and proof studio preserve page review and stale revision checks',async({page},info)=>{
 await fixture(page);await page.goto('/customer/work/'+id);
 await expect(page.getByRole('region',{name:'Your next action',exact:true})).toContainText('Your artwork is ready to review');
 await expect(page.getByRole('list',{name:'Project stages'}).getByRole('listitem')).toHaveCount(5);
 await expect(page.getByRole('complementary',{name:'Project conversation and email'})).toContainText('Portal');
 await captureEvidence(page,`customer-work-overview-${info.project.name}.png`);
 await page.getByRole('button',{name:'Open proof studio',exact:true}).click();
 const studio=page.getByRole('dialog',{name:'Proof studio — revision 2',exact:true});
 await expect(studio.getByText('Page 1 of 2',{exact:true})).toBeVisible();
 const approve=studio.getByRole('button',{name:'Approve proof revision 2',exact:true});await expect(approve).toBeDisabled();
 await studio.getByRole('button',{name:'Mark page 1 reviewed',exact:true}).click();await studio.getByRole('button',{name:'View page 2',exact:true}).click();
 await expect(studio.getByText('Page 2 of 2',{exact:true})).toBeVisible();await studio.getByRole('button',{name:'Mark page 2 reviewed',exact:true}).click();
 await studio.getByRole('checkbox',{name:'I reviewed proof revision 2 and its file against the accepted quotation.',exact:true}).check();await expect(approve).toBeEnabled();
 await studio.screenshot({path:info.outputPath('02-proof-studio.png')});
 await approve.click();await expect(studio.getByRole('alert')).toContainText('proof changed');await expect(approve).toBeDisabled();
 await studio.getByRole('button',{name:'Close proof studio',exact:true}).click();
 if(info.project.name==='mobile')await phoneFits(page);
 else {const main=await page.locator('.work-primary').boundingBox(),rail=await page.locator('.work-sidebar').boundingBox();expect(rail!.x).toBeGreaterThan(main!.x+main!.width-1);}
});

test('failed scan and unpaid finance remain visibly blocked',async({page},info)=>{
 const {requests}=await fixture(page,{scan:'QUARANTINED'});await page.goto('/customer/work/'+id);
 await expect(page.getByRole('region',{name:'Your next action',exact:true})).toContainText('Your artwork is being checked');
 const financePanel=page.getByRole('region',{name:'Payments and invoices',exact:true});await expect(financePanel.locator('.finance-summary')).toContainText('₹2,000.00');await expect(financePanel.locator('.finance-summary')).toContainText('Balance outstanding');await expect(financePanel.getByText('Configure advance and payment rules',{exact:true})).toHaveCount(0);await expect(financePanel.getByRole('button',{name:'Issue invoice',exact:true})).toHaveCount(0);
 await expect(page.getByRole('region',{name:'Finance readiness',exact:true})).toContainText('Required advance verified');
 await expect(page.getByRole('region',{name:'Finance readiness',exact:true}).locator('li').filter({hasText:'Required advance verified'})).toHaveAttribute('data-state','blocked');
 await expect(page.getByRole('button',{name:'Approve proof revision 2',exact:true})).toBeDisabled();
 expect(requests).toHaveLength(0);await page.locator('#work-payments').scrollIntoViewIfNeeded();await captureEvidence(page,`customer-finance-readiness-${info.project.name}.png`);
 if(info.project.name==='mobile')await phoneFits(page);
});

test('printer lane selection, detail drawer, HOLD/RESUME and stale revision feedback',async({page},info)=>{
 const {requests}=await fixture(page,{role:'OPERATOR'});await page.goto('/business');
 await expect(page.getByRole('heading',{name:'Production queue',exact:true})).toBeVisible();
 await page.getByRole('searchbox',{name:'Search production jobs'}).fill('PF-11111111');await expect(page.locator('.production-card')).toHaveCount(1);await page.getByRole('searchbox',{name:'Search production jobs'}).fill('11111111');await expect(page.locator('.production-card')).toHaveCount(0);await page.getByRole('searchbox',{name:'Search production jobs'}).fill('PF-99999999');await expect(page.locator('.production-card')).toHaveCount(0);await page.getByRole('searchbox',{name:'Search production jobs'}).fill('PF-11111111');
 if(info.project.name==='mobile'){await page.getByRole('combobox',{name:'Production lane',exact:true}).selectOption('QUEUED');await expect(page.locator('.production-card')).toBeHidden();await page.getByRole('combobox',{name:'Production lane',exact:true}).selectOption('QC');await phoneFits(page);}
 if(info.project.name==='mobile')await page.locator('.production-card').scrollIntoViewIfNeeded();await page.screenshot({path:info.outputPath('04-production-queue.png')});await page.locator('.production-card').click();
 const drawer=page.getByRole('dialog',{name:'Production job',exact:true}),job=drawer.getByRole('region',{name:'Production queue job',exact:true});
 const note=job.getByRole('textbox',{name:/^Customer-visible operation note/});
 const confirm=job.getByRole('checkbox',{name:'I confirm the operation and quality information is accurate.',exact:true});
 await expect(job.getByRole('button',{name:'Pass quality check & ready for dispatch',exact:true})).toBeDisabled();
 await note.fill('Synthetic quality issue needs review.');await confirm.check();await job.getByRole('button',{name:'Put production on hold',exact:true}).click();
 await expect(job.getByRole('status').filter({hasText:'On hold:'})).toContainText('Synthetic quality');await expect(job.locator('[aria-current=step]')).toHaveText('Quality check');
 await drawer.screenshot({path:info.outputPath('05-production-hold.png')});
 await note.fill('Synthetic quality issue resolved.');await confirm.check();await job.getByRole('button',{name:'Resume production',exact:true}).click();await expect(job.locator('[aria-current=step]')).toHaveText('Quality check');
 await expect.poll(()=>requests.map(r=>[r.body.action,r.body.revision,r.body.confirmed])).toEqual([['HOLD',7,true],['RESUME',8,true]]);
 await note.fill('Synthetic quality completion confirmation.');await confirm.check();await job.getByRole('button',{name:'Pass quality check & ready for dispatch',exact:true}).click();await expect(job.getByRole('alert')).toContainText('revision changed');await expect(confirm).not.toBeChecked();await expect(job.getByRole('button',{name:'Pass quality check & ready for dispatch',exact:true})).toBeDisabled();
 await page.keyboard.press('Escape');await expect(drawer).toHaveCount(0);await expect(page.locator('.production-card')).toBeFocused();
});

test('delivery scan evidence precedes confirmation and failed dispatch keeps the note',async({page},info)=>{
 const {requests}=await fixture(page,{role:'OWNER',delivery:true});await page.goto('/business');await page.getByRole('button',{name:'PRODUCTION',exact:true}).click();await page.locator('.production-card').click();
 const flow=page.getByRole('region',{name:'Production and delivery',exact:true});
 const steps=flow.getByRole('list',{name:'Order delivery steps',exact:true});await expect(steps.getByRole('listitem')).toHaveCount(7);await expect(steps.getByText('Dispatch',{exact:true})).toBeVisible();await expect(steps.getByRole('listitem').nth(1)).toHaveAttribute('data-state','current');await expect(steps.getByRole('listitem').nth(2)).toHaveAttribute('data-state','pending');
 await flow.scrollIntoViewIfNeeded();await captureEvidence(page,`owner-fulfilment-stepper-${info.project.name}.png`);
 await expect(flow.getByRole('region',{name:'Dispatch readiness',exact:true})).toContainText('Scan pending');
 await expect(flow.getByRole('region',{name:'Delivery confirmation readiness',exact:true})).toContainText('After dispatch');
 await expect(flow.getByText('Awaiting successful scan',{exact:true}).first()).toBeVisible();
 const note=flow.getByRole('textbox',{name:/^Customer-visible progress note/});await note.fill('Synthetic dispatch confirmation must remain blocked.');
 await flow.getByRole('checkbox',{name:'I confirm this action reflects the actual work and the note is suitable for the customer.',exact:true}).check();await flow.getByRole('button',{name:'Confirm dispatch',exact:true}).click();
 await expect(flow.getByRole('alert')).toContainText('clean pickup photo');await expect(note).toHaveValue('Synthetic dispatch confirmation must remain blocked.');expect(requests).toHaveLength(1);
 await flow.scrollIntoViewIfNeeded();await captureEvidence(page,`owner-dispatch-and-delivery-readiness-${info.project.name}.png`);if(info.project.name==='mobile')await phoneFits(page);
 await flow.getByRole('button',{name:'Retry photo scan',exact:true}).click();await expect(flow.getByText('✓ Scan passed',{exact:true})).toBeVisible();await flow.getByRole('button',{name:/Preview pickup photo:/}).click();const photo=page.getByRole('dialog',{name:'Pickup photo',exact:true});await expect(photo.getByRole('img')).toHaveJSProperty('naturalWidth',160);await captureEvidence(page,`secure-original-photo-preview-${info.project.name}.png`);await photo.getByRole('button',{name:'Close dialog',exact:true}).click();
});


test('missing pickup and delivery photos are identified independently after dispatch',async({page})=>{
 await fixture(page,{role:'OWNER',delivery:true,noPhotos:true,workState:'IN_TRANSIT'});await page.goto('/business');await page.getByRole('button',{name:'PRODUCTION',exact:true}).click();await page.locator('.production-card').click();
 const flow=page.getByRole('region',{name:'Production and delivery',exact:true});
 await expect(flow.getByRole('region',{name:'Dispatch readiness',exact:true})).toContainText('No pickup photo has been uploaded');
 await expect(flow.getByRole('region',{name:'Delivery confirmation readiness',exact:true})).toContainText('No delivery photo has been uploaded');
});

test('owner action inbox is scoped and module headings replace repeated enquiry headers',async({page},info)=>{
 await fixture(page,{role:'OWNER'});await page.goto('/business');
 await expect(page.getByRole('region',{name:'Business action inbox',exact:true})).toContainText('Review new enquiry');
 await page.screenshot({path:info.outputPath('07-owner-action-inbox.png')});
 await page.getByLabel('Search enquiries').fill('PF-11111111');await expect(page.locator('.owner-table tbody tr').filter({hasText:'PF-11111111'})).toBeVisible();await page.getByLabel('Search enquiries').fill('11111111');await expect(page.locator('.owner-table tbody tr')).toHaveCount(0);
 await page.getByRole('button',{name:'PRODUCTION',exact:true}).click();await expect(page.getByRole('heading',{level:1})).toHaveText('Print floor');await expect(page.getByLabel('Your visible queue at a glance')).toHaveCount(0);
 if(info.project.name==='mobile')await phoneFits(page);
});

test('approved artwork directs the customer to the verified advance prerequisite',async({page})=>{
 await fixture(page,{unpaid:true});await page.goto('/customer/work/'+id);
 await expect(page.getByRole('region',{name:'Your next action',exact:true})).toContainText('Your advance payment is due');
});

test('customer next action follows workflow codes for amendments, proof rejection, payment, holds and delivery issues',async({page})=>{
 const {setWorkflow}=await fixture(page);await page.goto('/customer/work/'+id);
 const next=page.getByRole('region',{name:'Your next action',exact:true});
 const check=async(state:Partial<CustomerWorkflow>,expected:string)=>{setWorkflow(state);await page.reload();await expect(next).toContainText(expected);};
 await check({proofDecision:'REJECT'},'Your artwork feedback is with the designer');
 await check({amendmentState:'CHANGES_REQUESTED'},'The business is revising your quotation');
 await check({amendmentState:'ISSUED'},'Review your revised quotation');
 await check({proofDecision:'APPROVE',paymentCode:'ADVANCE_DUE'},'Your advance payment is due');
 await check({proofDecision:'APPROVE',paymentCode:'OVERPAID'},'The team is checking payment details');
 await check({proofDecision:'APPROVE',paymentCode:'BALANCE_DUE',fulfilmentState:'READY'},'Your balance is due before dispatch');
 await check({fulfilmentState:'PRODUCTION',productionHold:true},'Production is on hold');
 await check({fulfilmentState:'DELIVERED',deliveryIssueOpen:true},'The team is resolving a delivery issue');
 await check({fulfilmentState:'DELIVERED',deliveryIssueOpen:false},'Check your delivery');
 await check({fulfilmentState:'IN_TRANSIT'},'Your order is on its way');
 await check({proofDecision:'NONE',proofScanState:'QUARANTINED'},'Your artwork is being checked');
 await check({proofDecision:'NONE',proofScanState:'REJECTED'},'The team needs to replace the artwork file');
 await check({proofDecision:'APPROVE',proofForCurrentAgreement:false,proofScanState:'CLEAN'},'Your revised agreement needs an updated proof');
});

test('artwork workspace expands only before fulfilment starts',async({page})=>{
 const {setWorkflow}=await fixture(page);setWorkflow({fulfilmentState:'NOT_STARTED'});await page.goto('/customer/work/'+id);
 const artwork=page.locator('#work-artwork');await expect(artwork.getByRole('region',{name:'Design proofs',exact:true})).toBeVisible();
 setWorkflow({fulfilmentState:'PRODUCTION',productionStage:'PRINTING'});await page.reload();
 const history=artwork.locator('details.workflow-history');await expect(history.locator('summary')).toHaveText('Approved artwork and review history');await expect(history).not.toHaveAttribute('open','');
});

test('ready-for-dispatch payment exceptions explain their blocking reason',async({page})=>{
 const {setWorkflow}=await fixture(page);await page.goto('/customer/work/'+id);
 const next=page.getByRole('region',{name:'Your next action',exact:true});
 const check=async(paymentCode:CustomerWorkflow['paymentCode'],expected:string)=>{setWorkflow({fulfilmentState:'READY',productionStage:'READY',paymentCode});await page.reload();await expect(next).toContainText(expected);};
 await check('BALANCE_DUE','Your balance is due before dispatch');
 await check('OVERPAID','Payment reconciliation is needed before dispatch');
 await check('TERMS_PENDING','Payment terms need confirmation before dispatch');
 await check('PAID_IN_FULL','Preparing your delivery');
});
