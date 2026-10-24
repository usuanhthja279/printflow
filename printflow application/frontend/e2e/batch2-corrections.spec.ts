import {test,expect,Page} from '@playwright/test';

// Synthetic UI/API fixture only. No local business records, mail or payment provider is touched.
const enquiryId='9b10206a-3c40-45f6-a1a2-13f223bfe2bd';
type Job={id:string;public_id:string;product_code:string;quantity:number;stage:string;fulfilment:string;operator_id:string|null;operator_name:string|null;machine:string|null;priority:string|null;due_date:string|null;hold_reason:string|null;revision:number;rework_count:number;active_rework?:boolean;payment_code?:string;payment_terms_current?:boolean;release_blockers?:string[];dispatch_blockers?:string[];current_proof_approved?:boolean;current_invoice_issued?:boolean;photos_required?:boolean;pickup_evidence_state?:string|null;quotation_amendment_open?:boolean};
const makeJob=(overrides:Partial<Job>={}):Job=>({id:enquiryId,public_id:enquiryId,product_code:'WEDDING_CARDS',quantity:1200,stage:'READY',fulfilment:'READY',operator_id:'printer-1',operator_name:'Sample printer',machine:'Press 1',priority:'NORMAL',due_date:'2026-11-02',hold_reason:null,revision:4,rework_count:0,active_rework:false,payment_code:'PAID_IN_FULL',payment_terms_current:true,release_blockers:[],dispatch_blockers:[],current_proof_approved:true,current_invoice_issued:true,photos_required:true,pickup_evidence_state:'CLEAN',quotation_amendment_open:false,...overrides});
type Fixture={role:'OWNER'|'OPERATOR';jobs:Job[];pendingAssignments:number};
const lead={id:enquiryId,publicId:enquiryId,customerName:'Synthetic customer',email:'synthetic@example.test',phone:'9000000000',productCode:'WEDDING_CARDS',quantity:1200,notes:'Synthetic production QA enquiry.',status:'IN_PROGRESS',source:'WEBSITE',priority:'NORMAL',assignedStaffId:'owner-1',nextAction:'',followUpAt:null,revision:1,createdAt:'2026-10-25T09:00:00Z'};

async function installFixture(page:Page,fixture:Fixture){
 await page.route('**/api/**',async route=>{
  const request=route.request(),path=new URL(request.url()).pathname.replace(/^\/api/,''),send=(body:unknown,status=200)=>route.fulfill({status,json:body});
  if(request.method()!=='GET'){
   const body=request.postDataJSON()||{};
   if(path==='/v1/production/enquiries/'+enquiryId){
    const current=fixture.jobs.find(job=>job.id===enquiryId)||makeJob(),action=String(body.action||'');
    if(Number(body.revision)!==current.revision)return send({message:'Queue changed; refresh first.'},409);
    const updated={...current,revision:current.revision+1};
    if(action==='HOLD')updated.hold_reason=String(body.note);
    if(action==='RESUME')updated.hold_reason=null;
    if(['PRINTING','FINISHING','QC','READY'].includes(action))updated.stage=action;
    if(action==='REWORK'){updated.stage='PRINTING';updated.rework_count+=1;}
    fixture.jobs=fixture.jobs.map(job=>job.id===enquiryId?updated:job);
    return send({owner:fixture.role==='OWNER',job:updated,operators:[{id:'printer-1',name:'Sample printer'}]});
   }
   return send({message:'Unexpected synthetic mutation'},400);
  }
  if(path==='/v1/auth/session')return send({id:fixture.role==='OWNER'?'owner-1':'printer-1',username:fixture.role==='OWNER'?'owner@example.test':'printer@example.test',role:fixture.role,mfaRequired:false,mustChangePassword:false,contactsAccess:false});
  if(path==='/v1/auth/csrf')return send({token:'synthetic',headerName:'X-CSRF-TOKEN'});
  if(path==='/v1/staff/enquiries')return send([lead]);
  if(path==='/v1/owner/design-routing/summary')return send({pendingAssignmentCount:fixture.pendingAssignments});
  if(path==='/v1/production/queue'){
   const jobs=fixture.role==='OWNER'?fixture.jobs:fixture.jobs.filter(job=>job.operator_id==='printer-1').map(({payment_code,payment_terms_current,release_blockers,dispatch_blockers,current_proof_approved,current_invoice_issued,photos_required,pickup_evidence_state,quotation_amendment_open,rework_count,active_rework,...job})=>job);
   return send({owner:fixture.role==='OWNER',jobs,operators:fixture.role==='OWNER'?[{id:'printer-1',name:'Sample printer'}]:[]});
  }
  if(path==='/v1/production/enquiries/'+enquiryId){const job=fixture.jobs.find(item=>item.id===enquiryId)||makeJob();return send({owner:fixture.role==='OWNER',job,operators:[{id:'printer-1',name:'Sample printer'}]});}
  if(path==='/v1/design/enquiries/'+enquiryId+'/fulfilment'){const job=fixture.jobs.find(item=>item.id===enquiryId);return send({state:job?.fulfilment||'READY',revision:2,owner:fixture.role==='OWNER',printer:fixture.role==='OPERATOR',delivery:false,closed:false,issue_open:false,productionStage:job?.stage||'READY',productionHold:job?.hold_reason});}
  if(path==='/v1/design/enquiries/'+enquiryId+'/fulfilment/photos/photo-1/preview')return route.fulfill({path:'e2e/fixtures/proof-v1.png',contentType:'image/png'});
  if(path==='/v1/design/enquiries/'+enquiryId+'/fulfilment/photos'){const job=fixture.jobs.find(item=>item.id===enquiryId),photos=job?.pickup_evidence_state==='CLEAN'?[{id:'photo-1',stage:'PICKUP',caption:'Synthetic scanned pickup evidence',display_name:'synthetic-pickup.png',state:'CLEAN',created_at:'2026-10-25T09:15:00Z'}]:[];return send({required:job?.photos_required??true,canUpload:false,owner:fixture.role==='OWNER',workState:job?.fulfilment||'READY',photos});}
  if(path==='/v1/owner/staff')return send([]);
  if(path.startsWith('/v1/staff/enquiries/')&&path.endsWith('/activities'))return send([]);
  return send([]);
 });
}
async function openBoard(page:Page,role:'OWNER'|'OPERATOR'='OWNER'){
 await page.goto('/business');
 if(role==='OWNER'){
  await expect(page.getByRole('heading',{name:'Enquiries & follow-ups',exact:true})).toBeVisible();
  await page.getByRole('button',{name:'PRODUCTION',exact:true}).click();
 }
 await expect(page.getByRole('heading',{name:'Production queue',exact:true})).toBeVisible();
}
async function openDrawer(page:Page){await page.locator('.production-card').first().click();return page.getByRole('dialog',{name:'Production job',exact:true});}
async function capture(page:Page,info:{outputPath:(name:string)=>string},name:string){await page.screenshot({path:info.outputPath(name),animations:'disabled',style:'nextjs-portal{display:none!important}'});}

test('Batch 2 corrections: scoped readiness, queue exceptions and refresh',async({page},info)=>{
 await page.setViewportSize({width:info.project.name==='mobile'?390:1440,height:info.project.name==='mobile'?844:900});
 const owner:Fixture={role:'OWNER',jobs:[makeJob()],pendingAssignments:2};await installFixture(page,owner);

 // Owner dashboard values can be refreshed after another user changes queue state.
 await page.goto('/business');await expect(page.getByRole('region',{name:'Owner operations overview'})).toBeVisible();
 if(info.project.name==='desktop'){
  const releaseCard=page.getByRole('button',{name:/Awaiting release/});
  owner.jobs=[makeJob({stage:'WAITING_RELEASE',fulfilment:'NOT_STARTED'})];await page.getByRole('button',{name:'Refresh owner counts'}).click();
  await expect(releaseCard).toContainText('1');owner.jobs=[];await page.getByRole('button',{name:'Refresh owner counts'}).click();await expect(releaseCard).toContainText('0');
  owner.jobs=[makeJob({id:'held-qc-job',public_id:'held-qc-job',stage:'QC',fulfilment:'PRODUCTION',hold_reason:'Waiting for stock',rework_count:1,active_rework:true}),makeJob({id:'qc-job',public_id:'qc-job',stage:'QC',fulfilment:'PRODUCTION'}),makeJob({id:'active-rework-job',public_id:'active-rework-job',stage:'PRINTING',fulfilment:'PRODUCTION',rework_count:1,active_rework:true}),makeJob({id:'completed-rework-job',public_id:'completed-rework-job',stage:'READY',fulfilment:'READY',rework_count:2,active_rework:false})];await page.getByRole('button',{name:'Refresh owner counts'}).click();await expect(page.getByRole('button',{name:/QC \/ rework attention/})).toContainText('2');await expect(page.getByRole('button',{name:/Production holds/})).toContainText('1');
 }

 // Accurate dispatch checks and a specific explanation for a policy waiver.
 owner.jobs=[makeJob({pickup_evidence_state:'CLEAN'})];await openBoard(page);let dialog=await openDrawer(page);
 await expect(dialog.getByRole('region',{name:'Dispatch readiness'}).first()).toContainText('Pickup photo scan passed');await expect(dialog.getByRole('region',{name:'Dispatch readiness'}).last()).toContainText('Scan passed');
 if(info.project.name==='desktop')await capture(page,info,'01-owner-ready-for-dispatch.png');
 await page.keyboard.press('Escape');
 owner.jobs=[makeJob({payment_code:'TERMS_PENDING',payment_terms_current:false,dispatch_blockers:['Confirm payment terms for the current quotation']})];await openBoard(page);dialog=await openDrawer(page);
 await expect(dialog.getByRole('region',{name:'Dispatch readiness'}).first()).toContainText('Confirm payment terms for the current quotation');
 await expect(dialog.getByRole('region',{name:'Dispatch readiness'}).first()).toContainText('Required');
 if(info.project.name==='desktop')await capture(page,info,'02-missing-payment-agreement.png');
 await page.keyboard.press('Escape');
 owner.jobs=[makeJob({photos_required:false,pickup_evidence_state:'NONE'})];await openBoard(page);dialog=await openDrawer(page);
 const waiver=dialog.getByRole('region',{name:'Dispatch readiness'});await expect(waiver.first()).toContainText('Waived');await expect(waiver.last()).toContainText('Waived');await expect(waiver.first()).not.toContainText('scan passed');
 if(info.project.name==='desktop')await capture(page,info,'03-waived-pickup-photo.png');
 await page.keyboard.press('Escape');

 // Missing owner-only API fields must never look like successful printer checks.
 const printer:Fixture={role:'OPERATOR',jobs:[makeJob({pickup_evidence_state:'NONE'})],pendingAssignments:0};
 await page.unroute('**/api/**');await installFixture(page,printer);await openBoard(page,'OPERATOR');dialog=await openDrawer(page);
 await expect(dialog.locator('.owner-readiness-notice')).toContainText('does not infer readiness');await expect(dialog.locator('.job-drawer-heading')).toContainText('Production complete');await expect(dialog.locator('.job-drawer-heading')).not.toContainText('Ready for dispatch');await expect(dialog).not.toContainText('Production complete — ready for dispatch');await expect(dialog.getByText('Current payment agreement and balance / credit')).toHaveCount(0);await expect(dialog.getByText('Current quotation invoice issued')).toHaveCount(0);await expect(dialog.locator('.delivery-readiness-grid').getByRole('region',{name:'Dispatch readiness'})).toContainText('Missing');
 if(info.project.name==='desktop')await capture(page,info,'04-printer-ready-scoped.png');
 await page.keyboard.press('Escape');

 // HOLD is disjoint from QC/rework; labels, result sets and counts agree.
 await page.unroute('**/api/**');const owner2:Fixture={role:'OWNER',jobs:[makeJob({id:'held-qc-job',public_id:'held-qc-job',stage:'QC',fulfilment:'PRODUCTION',hold_reason:'Waiting for stock',rework_count:1,active_rework:true}),makeJob({id:'qc-job',public_id:'qc-job',stage:'QC',fulfilment:'PRODUCTION',rework_count:0}),makeJob({id:'active-rework-job',public_id:'active-rework-job',stage:'PRINTING',fulfilment:'PRODUCTION',rework_count:1,active_rework:true}),makeJob({id:'completed-rework-job',public_id:'completed-rework-job',stage:'READY',fulfilment:'READY',rework_count:2,active_rework:false})],pendingAssignments:0};await installFixture(page,owner2);await openBoard(page);
 const exceptionNav=page.getByRole('navigation',{name:'Production exceptions'});const holdFilter=exceptionNav.getByRole('button',{name:/On hold/});await expect(holdFilter).toContainText('1');await holdFilter.click();await expect(page.locator('.production-card')).toHaveCount(1);await expect(page.locator('.queue-summary')).toContainText('1 shown');
 await page.locator('.production-card').first().scrollIntoViewIfNeeded();await capture(page,info,'05-hold-filter.png');
 const qcFilter=exceptionNav.getByRole('button',{name:/QC \/ rework/});await expect(qcFilter).toContainText('2');await qcFilter.click();await expect(page.locator('.production-card')).toHaveCount(2);await expect(page.locator('.queue-summary')).toContainText('2 shown');await expect(page.locator('.production-card').filter({hasText:'PF-QCJOB'})).toHaveCount(1);await expect(page.locator('.production-card').filter({hasText:'PF-ACTIVERE'})).toHaveCount(1);await expect(page.locator('.production-card').filter({hasText:'PF-HELDQCJ'})).toHaveCount(0);await expect(page.locator('.production-card').filter({hasText:'PF-COMPLETE'})).toHaveCount(0);
 await page.locator('.production-card').first().scrollIntoViewIfNeeded();await capture(page,info,'06-qc-rework-filter.png');
 await page.getByLabel('Production filter').selectOption('ALL');if(info.project.name==='mobile')await page.getByRole('navigation',{name:'Choose production lane'}).getByRole('button',{name:/Ready/}).click();const completed=page.locator('.production-card').filter({hasText:'PF-COMPLETE'});await expect(completed).toBeVisible();await expect(completed).toContainText('Rework history ×2');await expect(qcFilter).toContainText('2');await completed.scrollIntoViewIfNeeded();await capture(page,info,'07-completed-rework-history.png');

 // Waiting-release jobs are not countable/eligible as production-printer unassigned jobs.
 owner2.jobs=[makeJob({id:'release-job',public_id:'release-job',stage:'WAITING_RELEASE',fulfilment:'NOT_STARTED',operator_id:null,operator_name:null}),makeJob({id:'queued-unassigned',public_id:'queued-unassigned',stage:'QUEUED',fulfilment:'PRODUCTION',operator_id:null,operator_name:null}),makeJob({id:'assigned-job',public_id:'assigned-job',stage:'PRINTING',fulfilment:'PRODUCTION'})];await openBoard(page);
 const unassigned=page.getByRole('button',{name:/Unassigned/});await expect(unassigned).toContainText('1');await unassigned.click();await expect(page.locator('.production-card')).toHaveCount(1);await expect(page.locator('.queue-summary')).toContainText('1 shown');

 // Queue refresh propagates a successful operation back into the selected drawer.
 owner2.jobs=[makeJob({stage:'QUEUED',fulfilment:'PRODUCTION'})];await openBoard(page);dialog=await openDrawer(page);const operations=dialog.getByRole('region',{name:'Production queue job'});
 await operations.getByLabel('Customer-visible operation note').fill('Synthetic printing started by the operator.');await operations.getByLabel('I confirm the operation and quality information is accurate.').check();await operations.getByRole('button',{name:'Start printing'}).click();
 await expect(dialog.locator('.job-drawer-heading')).toContainText('PRINTING');await expect(operations.locator('[aria-current=step]')).toHaveText('printing');
 if(info.project.name==='desktop')await capture(page,info,'07-refreshed-selected-job.png');
 await page.keyboard.press('Escape');

 // Compact horizontal mobile lane navigation stays inside the phone viewport.
 if(info.project.name==='mobile'){
  await expect(page.getByRole('navigation',{name:'Choose production lane'})).toBeVisible();await expect.poll(()=>page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);
  await page.getByRole('navigation',{name:'Choose production lane'}).getByRole('button',{name:/Printing/}).click();await expect(page.getByRole('region',{name:'Printing'})).toBeVisible();await capture(page,info,'08-mobile-production-lane.png');
 }
});
