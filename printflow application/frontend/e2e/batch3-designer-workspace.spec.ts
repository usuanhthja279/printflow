import {expect,test,type Page} from '@playwright/test';
import {mkdirSync,readFileSync} from 'node:fs';
import path from 'node:path';

const ids=['a1b2c3d4-1000-4000-8000-000000000001','b1b2c3d4-1000-4000-8000-000000000002','c1b2c3d4-1000-4000-8000-000000000003','d1b2c3d4-1000-4000-8000-000000000004','e1b2c3d4-1000-4000-8000-000000000005','f1b2c3d4-1000-4000-8000-000000000006'];
const projectId=ids[2],customerId=ids[1],oldFile='a1b2c3d4-2000-4000-8000-000000000001',currentFile='a1b2c3d4-2000-4000-8000-000000000002',oldProof='a1b2c3d4-2000-4000-8000-000000000003',currentProof='a1b2c3d4-2000-4000-8000-000000000004',quoteId='a1b2c3d4-2000-4000-8000-000000000005';
const now='2026-10-25T09:00:00Z',evidenceRoot=path.resolve('..','output','batch3-visual-evidence','synthetic'),batch3bEvidenceRoot=path.resolve('..','output','batch3b-visual-evidence','ui-fallback');
async function captureViewport(page:Page,name:string){await page.screenshot({path:path.join(evidenceRoot,name),scale:'css'});}
const image=readFileSync(path.resolve('e2e','fixtures','proof-v1.png'));
const pdf=readFileSync(path.resolve('e2e','fixtures','proof-pages-v1.pdf'));
const jobs=[
 {id:ids[0],customer_name:'Aarav Customer',latest_proof_revision:null,latest_proof_name:null,latest_proof_summary:null,latest_proof_scan_state:null,latest_proof_decision:null,latest_proof_current_agreement:null,unresolved_feedback_count:0,next_task_due_date:null},
 {id:ids[1],customer_name:'Bela Customer',latest_proof_revision:1,latest_proof_name:'review-proof.png',latest_proof_summary:'Customer artwork proof',latest_proof_scan_state:'CLEAN',latest_proof_decision:'REQUEST_CHANGES',latest_proof_current_agreement:true,unresolved_feedback_count:1,next_task_due_date:null},
 {id:projectId,customer_name:'Chitra Customer',latest_proof_revision:2,latest_proof_name:'current-proof.png',latest_proof_summary:'Updated invitation design',latest_proof_scan_state:'CLEAN',latest_proof_decision:null,latest_proof_current_agreement:true,unresolved_feedback_count:1,next_task_due_date:'2026-10-28'},
 {id:ids[3],customer_name:'Dev Customer',latest_proof_revision:1,latest_proof_name:'approved.png',latest_proof_summary:'Approved card design',latest_proof_scan_state:'CLEAN',latest_proof_decision:'APPROVE',latest_proof_current_agreement:true,unresolved_feedback_count:0,next_task_due_date:null},
 {id:ids[4],customer_name:'Esha Customer',latest_proof_revision:null,latest_proof_name:null,latest_proof_summary:null,latest_proof_scan_state:null,latest_proof_decision:null,latest_proof_current_agreement:null,unresolved_feedback_count:0,next_task_due_date:null,status:'CLOSED'},
 {id:ids[5],customer_name:'Farah Customer',latest_proof_revision:1,latest_proof_name:'awaiting.png',latest_proof_summary:'Shared for review',latest_proof_scan_state:'CLEAN',latest_proof_decision:null,latest_proof_current_agreement:true,unresolved_feedback_count:0,next_task_due_date:null},
].map((job,index)=>({public_id:job.id,product_code:'WEDDING_CARDS',quantity:250+index*100,status:index===4?'CLOSED':'IN_PROGRESS',quote_accepted:true,...job}));
const oldRevision={id:oldProof,revision:1,artifact_id:oldFile,quote_id:quoteId,summary:'Original two-page proof',display_name:'proof-pages-v1.pdf',sha256:'b'.repeat(64),scan_state:'CLEAN',decision:'REQUEST_CHANGES',decision_note:'Please move the heading on page two.',decided_at:now,decision_source:null,current_agreement:true,page_count:2};
const currentRevision:{id:string;revision:number;artifact_id:string;quote_id:string;summary:string;display_name:string;sha256:string;scan_state:string;decision:string|null;decision_note:string|null;decided_at:string|null;decision_source:string|null;current_agreement:boolean;page_count:number}={id:currentProof,revision:2,artifact_id:currentFile,quote_id:quoteId,summary:'Updated invitation design',display_name:'current-proof.png',sha256:'c'.repeat(64),scan_state:'CLEAN',decision:null,decision_note:null,decided_at:null,decision_source:null,current_agreement:true,page_count:1};
const oldComment={id:'comment-old',proof_id:oldProof,author_role:'CUSTOMER',body:'Move the heading away from the illustration on page two.',page_number:2,anchor_x:.3,anchor_y:.4,resolution_note:null,created_at:now};
const currentComment={id:'comment-current',proof_id:currentProof,author_role:'CUSTOMER',body:'Please keep this floral border.',page_number:1,anchor_x:.55,anchor_y:.35,resolution_note:null,created_at:now};
const artifactRows=[{id:oldFile,display_name:oldRevision.display_name,kind:'DESIGN',state:'CLEAN',byte_size:1207,media_type:'application/pdf',sha256:oldRevision.sha256},{id:currentFile,display_name:currentRevision.display_name,kind:'DESIGN',state:'CLEAN',byte_size:image.length,media_type:'image/png',sha256:currentRevision.sha256}];
const workspace=(customer=false)=>({artifacts:customer?artifactRows.filter(a=>[oldFile,currentFile].includes(a.id)):artifactRows,proofs:[currentRevision,oldRevision],comments:[currentComment,oldComment],quoteAccepted:true,currentProofApproved:currentRevision.decision==='APPROVE',closed:false,designerId:'synthetic-designer',tasks:[{id:'task-1',title:'Check typography',notes:'Internal task',due_date:'2026-10-28',state:'TODO',revision:1}],referenceFeedback:[],referenceEditable:false,artworkRequested:false,productCode:'WEDDING_CARDS'});

async function installFixtures(page:Page,role:'DESIGNER'|'CUSTOMER'){
 let previewUnavailable=false,decisionAttempts=0;
 await page.route('**/api/**',async route=>{
  const req=route.request(),pathname=new URL(req.url()).pathname;let body:unknown={},status=200;
  if(pathname.endsWith('/preview')){if(previewUnavailable){await route.fulfill({status:503,contentType:'text/plain',body:'synthetic preview unavailable'});return;}const isPdf=pathname.includes('/artifacts/'+oldFile+'/');await route.fulfill({status:200,contentType:isPdf?'application/pdf':'image/png',body:isPdf?pdf:image});return;}
  if(pathname==='/api/v1/auth/session')body={id:'synthetic-'+role.toLowerCase(),username:role.toLowerCase()+'@example.test',role,mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true};
  else if(pathname==='/api/v1/auth/appearance')body={version:0,has_image:false,public_name:''};
  else if(pathname==='/api/v1/auth/csrf')body={token:'synthetic-csrf',headerName:'X-CSRF-TOKEN'};
  else if(pathname==='/api/v1/design/queue')body=jobs;
  else if(pathname==='/api/v1/design/intake')body=[];
  else if(pathname==='/api/v1/design/enquiries/'+projectId+'/designer-project')body=jobs[2];
  else if(pathname==='/api/v1/design/enquiries/'+projectId+'/work')body={id:projectId,publicId:projectId,workflow:{},productCode:'WEDDING_CARDS',quantity:450,status:'IN_PROGRESS',liveWork:true,closureMessage:null,stages:{},designer:'Synthetic designer',contact:{email:'designer@example.test',phone:'9000000000'},mailConnected:false,emails:[{id:'email-1',kind:'PROOF_SHARED',subject:'Your Wedding Cards design proof is ready',state:'SENT',thread_phase:'WORK',created_at:now}],logs:[],messages:[{id:'message-1',author_role:'CUSTOMER',channel:'PORTAL',body:'Please keep the floral border.',created_at:now}]};
  else if(pathname==='/api/v1/design/enquiries/'+projectId||pathname==='/api/v1/design/enquiries/'+customerId)body=workspace(role==='CUSTOMER');
  else if(pathname.endsWith('/samples'))body=[];
  else if(pathname==='/api/public/identity/status')body={deliveryEnabled:false};
  else if(pathname==='/api/public/sso/providers')body=[];
  else if(pathname==='/api/v1/customer/profile')body={name:'Bela Customer',email:'customer@example.test',phone:'9000000000',address:'',organization:''};
  else if(pathname==='/api/v1/customer/enquiries')body=[{id:customerId,public_id:customerId,product_code:'WEDDING_CARDS',quantity:350,status:'REVIEWING',live_work:false,customer_stage:'Review quotation and artwork'}];
  else if(pathname==='/api/v1/customer/enquiries/'+customerId)body={id:customerId,public_id:customerId,product_code:'WEDDING_CARDS',quantity:350,status:'REVIEWING',notes:'Synthetic customer proof-review project',live_work:false,size:'Need advice',finish:'MATTE'};
  else if(pathname==='/api/v1/customer/enquiries/'+customerId+'/quotes')body=[{id:quoteId,version:1,scope:'350 cards',total_paise:125050,terms:'Synthetic agreement',requested_date:null,proposed_date:'2026-11-10',valid_until:'2026-11-01',specification_snapshot:'{}',state:'ACCEPTED',issued_at:now,response_note:null}];
  else if(pathname==='/api/v1/customer/privacy'||pathname==='/api/v1/customer/sso')body=[];
  else if(pathname.endsWith('/decision')&&req.method()==='POST'){
   const input=req.postDataJSON();expect(input).toMatchObject({action:'APPROVE',quoteId,artifactHash:currentRevision.sha256,reviewed:true});decisionAttempts++;
   if(decisionAttempts===1){status=409;body={message:'The proof changed while you were reviewing it.'};}
   else{currentRevision.decision='APPROVE';currentRevision.decided_at=now;body=workspace(true);}
  }
  else if(pathname.endsWith('/quotes')||pathname.endsWith('/activities')||pathname.endsWith('/orders'))body=[];
  else throw Error('Unexpected Batch 3 UI fixture request: '+pathname);
  await route.fulfill({status,contentType:'application/json',body:JSON.stringify(body)});
 });
 return {setPreviewUnavailable:(value:boolean)=>{previewUnavailable=value;},decisionAttempts:()=>decisionAttempts};
}

test('designer dashboard and scoped project workspace support revisions, exact annotation navigation and mobile layout',async({page})=>{
 test.setTimeout(120000);page.setDefaultTimeout(10000);const fixture=await installFixtures(page,'DESIGNER');
 await page.setViewportSize({width:1440,height:900});await page.goto('/business',{waitUntil:'domcontentloaded'});
 await expect(page.getByRole('heading',{name:'Assigned design projects'})).toBeVisible();
 await page.getByLabel('Designer project filter').selectOption('CUSTOMER');await expect(page.getByText('Farah Customer')).toBeVisible();
 await page.getByLabel('Designer project filter').selectOption('CHANGES');await expect(page.getByText('Bela Customer')).toBeVisible();
 await page.getByLabel('Designer project filter').selectOption('CLOSED');await expect(page.getByText('Esha Customer')).toBeVisible();
 await page.getByLabel('Designer project filter').selectOption('ALL');await page.getByLabel('Find a project').fill('PF-C1B2C3D4');await expect(page.getByText('Chitra Customer')).toBeVisible();
 await page.screenshot({path:path.join(evidenceRoot,'designer-dashboard-desktop.png'),fullPage:true});await captureViewport(page,'designer-dashboard-desktop-viewport.png');
 await page.goto('/designer/work/'+projectId,{waitUntil:'domcontentloaded'});await expect(page.getByRole('heading',{name:'Shaadi & Wedding Cards'})).toBeVisible();
 await expect(page.locator('.collaboration-status')).toContainText(/Connecting|Reconnecting|unavailable/i);await page.waitForTimeout(1800);await page.evaluate(()=>window.scrollTo(0,0));await page.screenshot({path:path.join(batch3bEvidenceRoot,'designer-collaboration-fallback-desktop.png'),scale:'css'});
 await expect(page.getByRole('region',{name:'Work conversation'})).toBeVisible();await expect(page.getByRole('heading',{name:'Proof revision 2'})).toBeVisible();
 const duplicateIds=await page.locator('[id]').evaluateAll(elements=>{const ids=elements.map(el=>el.id).filter(Boolean);return ids.filter((id,index)=>ids.indexOf(id)!==index);});expect(duplicateIds).toEqual([]);
 const oldFeedback=page.getByRole('button',{name:/Locate annotation on artwork · revision 1/});await oldFeedback.click();
 const pdfViewer=page.getByRole('region',{name:'PDF artwork preview'});await expect(pdfViewer.getByText('Page 2 of 2',{exact:true})).toBeVisible();
 await pdfViewer.getByRole('combobox',{name:'PDF page'}).selectOption('1');await expect(pdfViewer.getByText('Page 1 of 2',{exact:true})).toBeVisible();await oldFeedback.click();await expect(pdfViewer.getByText('Page 2 of 2',{exact:true})).toBeVisible();
 await pdfViewer.getByLabel('PDF zoom').selectOption('1.5');await expect(pdfViewer.getByRole('button',{name:'PDF page 2 preview'})).toBeEnabled();const pdfPin=pdfViewer.getByRole('button',{name:/Go to comment: page 2 review pin/});expect(await pdfPin.evaluate(el=>(el as HTMLElement).style.left)).toBe('30%');expect(await pdfPin.evaluate(el=>(el as HTMLElement).style.top)).toBe('40%');await pdfPin.focus();await page.keyboard.press('Enter');await expect(page.getByRole('complementary',{name:'Revision 1 comments and approval'}).getByText(oldComment.body,{exact:true})).toBeInViewport();
 await expect.poll(()=>pdfViewer.locator('canvas').evaluate(el=>(el as HTMLCanvasElement).width)).toBeGreaterThan(0);await page.screenshot({path:path.join(evidenceRoot,'designer-multiple-revisions-annotation-desktop.png'),fullPage:true});await page.locator('#proof-artwork-'+projectId+'-'+oldProof).scrollIntoViewIfNeeded();await captureViewport(page,'designer-multiple-revisions-annotation-desktop-viewport.png');
 const currentFeedback=page.getByRole('button',{name:/Locate annotation on artwork · revision 2/});await currentFeedback.click();await page.getByLabel('Image zoom').selectOption('2');
 const pin=page.locator('#proof-artwork-'+projectId+'-'+currentProof+' .proof-pin-select').first();const coords=await pin.evaluate(el=>{const r=el.getBoundingClientRect(),stage=el.closest('.proof-canvas')!.getBoundingClientRect();return {x:(r.left+r.width/2-stage.left)/stage.width,y:(r.top+r.height/2-stage.top)/stage.height};});expect(coords.x).toBeCloseTo(.55,1);expect(coords.y).toBeCloseTo(.35,1);
 await page.locator('#proof-artwork-'+projectId+'-'+currentProof).scrollIntoViewIfNeeded();await page.screenshot({path:path.join(evidenceRoot,'designer-project-desktop.png'),fullPage:true});await captureViewport(page,'designer-project-desktop-viewport.png');await page.setViewportSize({width:390,height:844});await page.evaluate(()=>window.scrollTo(0,0));await page.screenshot({path:path.join(batch3bEvidenceRoot,'designer-collaboration-fallback-mobile.png'),scale:'css'});await pin.scrollIntoViewIfNeeded();await page.screenshot({path:path.join(evidenceRoot,'designer-proof-studio-mobile.png'),fullPage:true});await captureViewport(page,'designer-proof-studio-mobile-viewport.png');expect(await page.locator('body').evaluate(el=>el.scrollWidth)).toBeLessThanOrEqual(390);
 fixture.setPreviewUnavailable(true);await page.setViewportSize({width:1440,height:900});await page.reload();const previewError=page.getByText('Preview unavailable. Retry the image or contact the team before approving.');await expect(previewError).toBeVisible();await page.screenshot({path:path.join(evidenceRoot,'designer-preview-error-desktop.png'),fullPage:true});await previewError.scrollIntoViewIfNeeded();await captureViewport(page,'designer-preview-error-desktop-viewport.png');
});

test('customer proof review shows revision history, handles stale approval, and preserves exact approval binding',async({page})=>{
 test.setTimeout(120000);page.setDefaultTimeout(10000);const fixture=await installFixtures(page,'CUSTOMER');
 await page.setViewportSize({width:1440,height:900});await page.goto('/customer',{waitUntil:'domcontentloaded'});await page.getByRole('button',{name:'View enquiry',exact:true}).click();const connectionStatus=page.locator('.collaboration-status');await expect(connectionStatus).toContainText(/Connecting|Reconnecting|unavailable/i);await page.waitForTimeout(1800);await connectionStatus.scrollIntoViewIfNeeded();await page.screenshot({path:path.join(batch3bEvidenceRoot,'customer-collaboration-fallback-desktop.png'),scale:'css'});
 const design=page.getByRole('region',{name:'Design proofs',exact:true});await expect(design.getByRole('heading',{name:'Proof revision 2'})).toBeVisible();await expect(design.getByRole('heading',{name:'Proof revision 1'})).toBeVisible();
 await design.getByRole('button',{name:'Open proof studio'}).click();const modalIds=await page.locator('[id]').evaluateAll(elements=>{const ids=elements.map(el=>el.id).filter(Boolean);return ids.filter((id,index)=>ids.indexOf(id)!==index);});expect(modalIds).toEqual([]);await page.getByRole('button',{name:'Close proof studio'}).click();
 await page.screenshot({path:path.join(evidenceRoot,'customer-multiple-revisions-desktop.png'),fullPage:true});await design.locator('#proof-artwork-'+customerId+'-'+currentProof).scrollIntoViewIfNeeded();await captureViewport(page,'customer-multiple-revisions-desktop-viewport.png');
 const oldFeedback=design.getByRole('button',{name:/Locate annotation on artwork · revision 1/});await oldFeedback.click();const viewer=design.getByRole('region',{name:'PDF artwork preview'});await expect(viewer.getByText('Page 2 of 2',{exact:true})).toBeVisible();
 await page.setViewportSize({width:390,height:844});await connectionStatus.scrollIntoViewIfNeeded();await page.screenshot({path:path.join(batch3bEvidenceRoot,'customer-collaboration-fallback-mobile.png'),scale:'css'});await design.locator('#proof-artwork-'+customerId+'-'+oldProof).scrollIntoViewIfNeeded();await page.screenshot({path:path.join(evidenceRoot,'customer-proof-studio-mobile.png'),fullPage:true});await captureViewport(page,'customer-proof-studio-mobile-viewport.png');expect(await page.locator('body').evaluate(el=>el.scrollWidth)).toBeLessThanOrEqual(390);
 await page.setViewportSize({width:1440,height:900});await design.getByRole('button',{name:/Locate annotation on artwork · revision 2/}).click();
 await design.getByRole('checkbox',{name:/I reviewed proof revision 2/}).check();await design.getByRole('button',{name:'Approve proof revision 2',exact:true}).click();await expect(design.getByRole('alert')).toContainText('proof changed');
 await page.screenshot({path:path.join(evidenceRoot,'customer-stale-approval-desktop.png'),fullPage:true});await captureViewport(page,'customer-stale-approval-desktop-viewport.png');await design.getByRole('checkbox',{name:/I reviewed proof revision 2/}).check();await design.getByRole('button',{name:'Approve proof revision 2',exact:true}).click();await expect(design.getByRole('status')).toContainText('Proof approved');
 await expect.poll(()=>fixture.decisionAttempts()).toBe(2);await page.screenshot({path:path.join(evidenceRoot,'customer-approved-proof-desktop.png'),fullPage:true});await captureViewport(page,'customer-approved-proof-desktop-viewport.png');
});

test.beforeAll(()=>{mkdirSync(evidenceRoot,{recursive:true});mkdirSync(batch3bEvidenceRoot,{recursive:true});});
