import {test,expect,Page} from '@playwright/test';

// Browser evidence fixture: all API traffic is intercepted and labeled synthetic.
// It never calls mail, payment gateways, or writes to the local business database.
const leadId='9b10206a-3c40-45f6-a1a2-13f223bfe2bd';
const emptyJobs:Job[]=[];
type Job={id:string;public_id:string;product_code:string;quantity:number;stage:string;fulfilment:string;operator_id:string|null;operator_name:string|null;machine:string|null;priority:string|null;due_date:string|null;hold_reason:string|null;revision:number;rework_count:number;payment_code:string|null;release_blockers:string[];dispatch_blockers:string[];current_proof_approved:boolean;current_invoice_issued:boolean;photos_required:boolean;pickup_evidence_state:string|null;quotation_amendment_open:boolean};
type Scenario={stage:string;fulfilment:string;hold:string|null;rework:number;photos:'NONE'|'QUARANTINED'|'CLEAN';empty?:boolean};
const job:Job={id:leadId,public_id:leadId,product_code:'WEDDING_CARDS',quantity:1200,stage:'PRINTING',fulfilment:'PRODUCTION',operator_id:'operator-1',operator_name:'Sample printer',machine:'Press 1',priority:'URGENT',due_date:'2026-11-02',hold_reason:null,revision:4,rework_count:0,payment_code:'PAID_IN_FULL',release_blockers:[],dispatch_blockers:[],current_proof_approved:true,current_invoice_issued:true,photos_required:true,pickup_evidence_state:'CLEAN',quotation_amendment_open:false};
const enquiry={id:leadId,publicId:leadId,customerName:'Synthetic customer',email:'synthetic@example.test',phone:'9000000000',productCode:'WEDDING_CARDS',quantity:1200,notes:'Synthetic print enquiry for visual QA.',status:'IN_PROGRESS',source:'WEBSITE',priority:'HIGH',assignedStaffId:'owner-1',nextAction:'Confirm proof and production timing',followUpAt:null,revision:1,createdAt:'2026-10-25T09:00:00Z'};
const photo={id:'photo-1',stage:'PICKUP',caption:'Synthetic packed order ready for dispatch',display_name:'synthetic-pickup.png',state:'QUARANTINED',created_at:'2026-10-25T09:15:00Z'};
async function installFixture(page:Page){
 let state:Scenario={stage:'PRINTING',fulfilment:'PRODUCTION',hold:null,rework:0,photos:'QUARANTINED'};
 let record:Job={...job};
 await page.route('**/api/**',async route=>{
  const request=route.request(),path=new URL(request.url()).pathname.replace(/^\/api/,''),send=(body:unknown,status=200)=>route.fulfill({status,json:body});
  if(request.method()!=='GET'){
   const body=request.postDataJSON()||{};
   if(path.startsWith('/v1/production/enquiries/')){
    const action=String(body.action||'');
    if(body.revision!==record.revision)return send({message:'Queue changed; refresh first.'},409);
    if(action==='HOLD')record={...record,hold_reason:String(body.note),revision:record.revision+1};
    else if(action==='RESUME')record={...record,hold_reason:null,revision:record.revision+1};
    else if(action==='REWORK')record={...record,stage:'PRINTING',rework_count:record.rework_count+1,revision:record.revision+1};
    else if(action==='PRINTING'||action==='FINISHING'||action==='QC'||action==='READY')record={...record,stage:action,revision:record.revision+1};
    return send({owner:true,job:record,operators:[{id:'operator-1',name:'Sample printer'}]});
   }
   return send({message:'Unexpected synthetic UI mutation'},400);
  }
  if(path==='/v1/auth/session')return send({id:'owner-1',username:'owner@example.test',role:'OWNER',mfaRequired:false,mustChangePassword:false,contactsAccess:false});
  if(path==='/v1/auth/csrf')return send({token:'synthetic',headerName:'X-CSRF-TOKEN'});
  if(path==='/v1/staff/enquiries')return send([enquiry]);
  if(path==='/v1/owner/design-routing/summary')return send({pendingAssignmentCount:2});
  if(path==='/v1/production/queue')return send({owner:true,jobs:state.empty?emptyJobs:[{...record,stage:state.stage,fulfilment:state.fulfilment,hold_reason:state.hold,rework_count:state.rework,pickup_evidence_state:state.photos,dispatch_blockers:state.fulfilment==='READY'&&state.photos!=='CLEAN'?[state.photos==='NONE'?'Add a scanned pickup photo':'Pickup photo scan is pending']:[]}]});
  if(path==='/v1/design/enquiries/'+leadId+'/fulfilment')return send({state:state.fulfilment,revision:2,owner:true,printer:false,delivery:false,closed:false,issue_open:false,productionStage:record.stage,productionHold:record.hold_reason});
  if(path==='/v1/production/enquiries/'+leadId)return send({owner:true,job:record,operators:[{id:'operator-1',name:'Sample printer'}]});
  if(path==='/v1/design/enquiries/'+leadId+'/fulfilment/photos')return send({required:true,canUpload:state.fulfilment==='READY',owner:true,workState:state.fulfilment,photos:state.photos==='NONE'?[]:[{...photo,state:state.photos}]});
  if(path==='/v1/owner/staff')return send([]);
  if(path.startsWith('/v1/staff/enquiries/')&&path.endsWith('/activities'))return send([]);
  return send([]);
 });
 return {set(next:Partial<Scenario>){state={...state,...next};record={...record,stage:state.stage,fulfilment:state.fulfilment,hold_reason:state.hold,rework_count:state.rework};},getJob(){return record;}};
}
async function openBoard(page:Page){await page.goto('/business');await expect(page.getByRole('heading',{name:'Enquiries & follow-ups',exact:true})).toBeVisible();await page.getByRole('button',{name:'PRODUCTION',exact:true}).click();await expect(page.getByRole('heading',{name:'Production queue',exact:true})).toBeVisible();}
async function drawer(page:Page){await page.locator('.production-card').first().click();return page.getByRole('dialog',{name:'Production job',exact:true});}
async function capture(page:Page,path:string){await page.screenshot({path,animations:'disabled',style:'nextjs-portal{display:none!important}'});}

test('Batch 2 visual evidence — owner and production command centre scenarios',async({page},info)=>{
 const fixture=await installFixture(page);await page.setViewportSize({width:info.project.name==='mobile'?390:1440,height:info.project.name==='mobile'?844:900});
 const out=(name:string)=>info.outputPath(name);
 fixture.set({stage:'PRINTING',fulfilment:'PRODUCTION',hold:null,rework:0,photos:'QUARANTINED'});
 await page.goto('/business');await expect(page.getByRole('region',{name:'Owner operations overview'})).toBeVisible();await capture(page,out('01-owner-dashboard-overview.png'));
 await page.locator('.action-centre').screenshot({path:out('02-owner-action-inbox.png'),animations:'disabled',style:'nextjs-portal{display:none!important}'});
 if(info.project.name==='desktop'){fixture.set({stage:'WAITING_RELEASE',fulfilment:'NOT_STARTED',hold:null,rework:0});await page.getByRole('button',{name:/Awaiting release/}).click();await expect(page.getByRole('heading',{name:'Production queue',exact:true})).toBeVisible();await expect(page.getByRole('combobox',{name:'Production filter'})).toHaveValue('WAITING_RELEASE');await expect(page.locator('.production-card')).toBeVisible();}
 await openBoard(page);
 if(info.project.name==='mobile'){await expect(page.locator('.production-card')).toBeVisible();await capture(page,out('04-production-board-mobile.png'));await page.getByRole('combobox',{name:'Production lane'}).selectOption('PRINTING');await expect(page.locator('.production-card')).toBeVisible();await expect.poll(()=>page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);}
 else await capture(page,out('03-production-board-desktop.png'));
 const search=page.getByRole('searchbox',{name:'Search production jobs'});await search.fill('Wedding');await expect(page.locator('.production-card')).toHaveCount(1);await search.fill('no-such-production-job');await expect(page.locator('.production-card')).toHaveCount(0);await search.fill('');await page.getByRole('combobox',{name:'Production filter'}).selectOption('URGENT');await expect(page.locator('.production-card')).toHaveCount(1);await page.getByRole('combobox',{name:'Production filter'}).selectOption('ALL');
 if(info.project.name==='desktop'){fixture.set({stage:'QC',fulfilment:'PRODUCTION',hold:null,rework:1});await openBoard(page);await page.getByRole('combobox',{name:'Production filter'}).selectOption('QC');await expect(page.locator('.production-card')).toHaveCount(1);fixture.set({stage:'READY',fulfilment:'READY',hold:null,rework:1,photos:'NONE'});await openBoard(page);await page.getByRole('combobox',{name:'Production filter'}).selectOption('BLOCKED_DISPATCH');await expect(page.locator('.production-card')).toHaveCount(1);}
 await fixture.set({stage:'QC',fulfilment:'PRODUCTION',hold:null,rework:0});await openBoard(page);let panel=await drawer(page);await capture(page,out('05-production-job-drawer.png'));await page.keyboard.press('Escape');
 await fixture.set({stage:'WAITING_RELEASE',fulfilment:'NOT_STARTED',hold:null,rework:0});await openBoard(page);panel=await drawer(page);await expect(panel).toContainText('Production release checks');await capture(page,out('06-awaiting-release.png'));await page.keyboard.press('Escape');
 await fixture.set({stage:'QUEUED',fulfilment:'PRODUCTION',hold:null,rework:0});await openBoard(page);panel=await drawer(page);await panel.getByText('Printer, machine and scheduling',{exact:true}).click();await expect(panel.getByLabel('Production printer')).toBeVisible();await panel.getByLabel('Production printer').scrollIntoViewIfNeeded();await capture(page,out('07-printer-assignment.png'));await page.keyboard.press('Escape');
 await fixture.set({stage:'PRINTING',fulfilment:'PRODUCTION',hold:null,rework:0});await openBoard(page);panel=await drawer(page);await expect(panel.locator('[aria-current=step]')).toHaveText('printing');await capture(page,out('08-printing-in-progress.png'));
 const jobPanel=panel.getByRole('region',{name:'Production queue job',exact:true});await jobPanel.getByLabel('Customer-visible operation note').fill('Synthetic hold for visual review.');await jobPanel.getByLabel('I confirm the operation and quality information is accurate.').check();await jobPanel.getByRole('button',{name:'Put production on hold',exact:true}).click();await expect(jobPanel.locator('.production-hold[role="status"]')).toContainText('On hold:');await jobPanel.locator('.production-hold').scrollIntoViewIfNeeded();await capture(page,out('09-production-hold.png'));
 await jobPanel.getByLabel('Customer-visible operation note').fill('Synthetic hold has been resolved.');await jobPanel.getByLabel('I confirm the operation and quality information is accurate.').check();await jobPanel.getByRole('button',{name:'Resume production',exact:true}).click();await expect(jobPanel.locator('[aria-current=step]')).toHaveText('printing');await capture(page,out('10-resumed-production.png'));await page.keyboard.press('Escape');
 await fixture.set({stage:'QC',fulfilment:'PRODUCTION',hold:null,rework:0});await openBoard(page);panel=await drawer(page);await expect(panel.locator('[aria-current=step]')).toHaveText('Quality check');await capture(page,out('11-quality-check.png'));
 const qc=panel.getByRole('region',{name:'Production queue job',exact:true});await qc.getByLabel('Customer-visible operation note').fill('Synthetic print quality requires another pass.');await qc.getByLabel('I confirm the operation and quality information is accurate.').check();await qc.getByRole('button',{name:'Return to printing for rework',exact:true}).click();await expect(qc.locator('[aria-current=step]')).toHaveText('printing');await capture(page,out('12-qc-rework.png'));await page.keyboard.press('Escape');
 await fixture.set({stage:'READY',fulfilment:'READY',hold:null,rework:1,photos:'CLEAN'});await openBoard(page);panel=await drawer(page);await expect(panel.getByRole('region',{name:'Dispatch readiness'})).toBeVisible();await capture(page,out('13-ready-for-dispatch.png'));await page.keyboard.press('Escape');
 await fixture.set({stage:'READY',fulfilment:'READY',hold:null,rework:1,photos:'NONE'});await openBoard(page);panel=await drawer(page);await expect(panel.getByRole('region',{name:'Dispatch readiness'}).first()).toContainText('Add a scanned pickup photo');await capture(page,out('14-dispatch-blocked-missing-photo.png'));await page.keyboard.press('Escape');
 await fixture.set({stage:'READY',fulfilment:'READY',hold:null,rework:1,photos:'QUARANTINED'});await openBoard(page);panel=await drawer(page);const photoEvidence=panel.getByRole('region',{name:'Delivery photo evidence'});await expect(photoEvidence).toContainText('Awaiting successful scan');await photoEvidence.scrollIntoViewIfNeeded();await capture(page,out('15-delivery-photo-scan-status.png'));await page.keyboard.press('Escape');
 fixture.set({stage:'READY',fulfilment:'READY',hold:null,rework:0,photos:'NONE',empty:true});await openBoard(page);await expect(page.getByRole('status')).toContainText('No jobs match this queue');await capture(page,out('16-empty-production-queue.png'));
});
