import {expect,test} from '@playwright/test';
import {mkdirSync} from 'node:fs';
import path from 'node:path';

const ids=['a1b2c3d4-1000-4000-8000-000000000001','b1b2c3d4-1000-4000-8000-000000000002','c1b2c3d4-1000-4000-8000-000000000003','d1b2c3d4-1000-4000-8000-000000000004','e1b2c3d4-1000-4000-8000-000000000005'];
const file='f1b2c3d4-1000-4000-8000-000000000006',proof='f2b2c3d4-1000-4000-8000-000000000007',quote='f3b2c3d4-1000-4000-8000-000000000008';
const now='2026-10-25T09:00:00Z';
const job=(index:number,overrides:Record<string,unknown>={})=>({id:ids[index],public_id:ids[index],customer_name:['Aarav Customer','Bela Customer','Chitra Customer','Dev Customer','Esha Customer'][index],product_code:'WEDDING_CARDS',quantity:250+index*100,status:index===4?'CLOSED':'IN_PROGRESS',quote_accepted:true,latest_proof_revision:index===0||index===4?null:1,latest_proof_name:index===0||index===4?null:'review-proof.png',latest_proof_summary:'Customer artwork proof',latest_proof_scan_state:index===0||index===4?null:'CLEAN',latest_proof_decision:index===2?'REQUEST_CHANGES':index===3?'APPROVE':null,latest_proof_current_agreement:index===0||index===4?null:true,unresolved_feedback_count:index===2?1:0,next_task_due_date:index===2?'2026-10-28':null,...overrides});
const image=Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6XKwAAAAASUVORK5CYII=','base64');
const evidenceRoot=path.resolve('..','output','batch3-visual-evidence','synthetic');

test('designer queue, accessible dedicated workspace, annotated proof navigation and responsive conversation rail',async({page},info)=>{
 test.setTimeout(120000);page.setDefaultTimeout(10000);
 const queue=[job(0),job(1),job(2),job(3),job(4)];
 const comment={id:'comment-synthetic-1',proof_id:proof,author_role:'CUSTOMER',body:'Please move the heading away from the illustration.',page_number:1,anchor_x:.3,anchor_y:.4,resolution_note:null,created_at:now};
 const artifact={id:file,display_name:'review-proof.png',kind:'DESIGN',state:'CLEAN',byte_size:1024,media_type:'image/png',sha256:'a'.repeat(64)};
 const revision={id:proof,revision:1,artifact_id:file,quote_id:quote,summary:'First reviewed design',display_name:artifact.display_name,sha256:artifact.sha256,scan_state:'CLEAN',decision:'REQUEST_CHANGES',decision_note:'Please move the heading away from the illustration.',decided_at:now,decision_source:null,current_agreement:true,page_count:1};
 const design={artifacts:[artifact],proofs:[revision],comments:[comment],quoteAccepted:true,currentProofApproved:false,closed:false,designerId:'synthetic-designer',tasks:[{id:'task-1',title:'Check typography',notes:'Internal task',due_date:'2026-10-28',state:'TODO',revision:1}],referenceFeedback:[],referenceEditable:false,artworkRequested:false,productCode:'WEDDING_CARDS'};
 await page.route('**/api/**',async route=>{
  const req=route.request(),url=new URL(req.url()),pathname=url.pathname;let body:unknown={};
  if(pathname.endsWith('/preview')){await route.fulfill({status:200,contentType:'image/png',body:image});return;}
  if(pathname==='/api/v1/auth/session')body={id:'synthetic-designer',username:'designer@example.test',role:'DESIGNER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true};
  else if(pathname==='/api/v1/design/queue')body=queue;
  else if(pathname==='/api/v1/design/intake')body=[];
  else if(pathname.endsWith('/work'))body={id:ids[1],publicId:ids[1],workflow:{},productCode:'WEDDING_CARDS',quantity:350,status:'IN_PROGRESS',liveWork:true,closureMessage:null,stages:{},designer:'Synthetic designer',contact:{email:'designer@example.test',phone:'9000000000'},mailConnected:false,emails:[{id:'email-1',kind:'PROOF_SHARED',subject:'Your Wedding Cards design proof is ready',state:'SENT',thread_phase:'WORK',created_at:now}],logs:[],messages:[{id:'message-1',author_role:'CUSTOMER',channel:'PORTAL',body:'Please keep the floral border.',created_at:now}]};
  else if(pathname==='/api/v1/design/enquiries/'+ids[1])body=design;
  else if(pathname.endsWith('/samples'))body=[];
  else if(pathname==='/api/v1/auth/appearance')body={version:0,has_image:false,public_name:''};
  else if(pathname==='/api/v1/auth/csrf')body={token:'synthetic-csrf',headerName:'X-CSRF-TOKEN'};
  else throw Error('Unexpected designer workspace fixture request: '+pathname);
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
 });

 await page.setViewportSize({width:1440,height:900});await page.goto('/business');
 await expect(page.getByRole('heading',{name:'Assigned design projects'})).toBeVisible();
 await expect(page.getByText('Needs your action',{exact:true})).toBeVisible();await expect(page.getByText('Awaiting customer',{exact:true})).toBeVisible();
 await page.getByLabel('Project filter').selectOption('CUSTOMER');await expect(page.getByText('Bela Customer')).toBeVisible();await expect(page.getByText('Chitra Customer')).toHaveCount(0);
 await page.getByLabel('Project filter').selectOption('CHANGES');await expect(page.getByText('Chitra Customer')).toBeVisible();await expect(page.getByText('Aarav Customer')).toHaveCount(0);
 await page.getByLabel('Project filter').selectOption('CLOSED');await expect(page.getByText('Esha Customer')).toBeVisible();
 await page.getByLabel('Project filter').selectOption('ALL');await page.getByLabel('Find a project').fill('PF-A1B2C3D4');await expect(page.getByText('Aarav Customer')).toBeVisible();await expect(page.getByText('Bela Customer')).toHaveCount(0);
 await page.screenshot({path:path.join(evidenceRoot,'designer-dashboard-desktop.png'),fullPage:true});
 await page.goto('/designer/work/'+ids[2]);await expect(page.getByRole('heading',{name:'Shaadi & Wedding Cards'})).toBeVisible();
 await expect(page.getByRole('region',{name:'Work conversation'})).toBeVisible();await expect(page.getByText('Please move the heading away from the illustration.')).toBeVisible();
 await page.getByRole('button',{name:'Locate annotation on artwork'}).click();await expect(page.locator('#proof-artwork')).toBeInViewport();
 await expect(page.getByRole('button',{name:'Proof image and review pins'})).toBeVisible();
 await page.screenshot({path:path.join(evidenceRoot,'designer-project-desktop.png'),fullPage:true});
 await page.setViewportSize({width:390,height:844});await page.screenshot({path:path.join(evidenceRoot,'designer-project-mobile.png'),fullPage:true});
 const mobileWidth=await page.locator('body').evaluate(el=>el.scrollWidth);expect(mobileWidth).toBeLessThanOrEqual(390);
});

test.beforeAll(()=>mkdirSync(evidenceRoot,{recursive:true}));
