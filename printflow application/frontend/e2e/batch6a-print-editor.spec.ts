import {expect,test} from '@playwright/test';
import {mkdirSync,readFileSync} from 'node:fs';
import path from 'node:path';

const id='ab6a0000-0000-4000-8000-000000000001';
const evidence=path.resolve('..','output','batch6a-visual-evidence');
const png=readFileSync(path.resolve('e2e','fixtures','proof-v1.png'));
const blank={schemaVersion:1 as const,templateId:'WEDDING_INVITATION',widthMm:148,heightMm:210,bleedMm:3,safeMarginMm:5,elements:[]};

test('print editor supports templates, canvas layers, ordering, undo/redo, save conflict and proof publishing',async({page},testInfo)=>{
 test.setTimeout(90000);page.setDefaultTimeout(10000);await page.setViewportSize(testInfo.project.name==='mobile'?{width:390,height:844}:{width:1440,height:900});
 let draft:{revision:number;document:{schemaVersion:1;templateId:string;widthMm:number;heightMm:number;bleedMm:number;safeMarginMm:number;elements:unknown[]};updatedAt:string|null}={revision:0,document:blank,updatedAt:null},conflictOnce=false,published=0,exported=0;
 await page.route('**/api/**',async route=>{
  const req=route.request(),url=new URL(req.url()),p=url.pathname;let body:unknown={};
  if(p==='/api/v1/auth/session')body={id:'synthetic-designer',username:'designer@example.test',role:'DESIGNER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true};
  else if(p==='/api/v1/auth/appearance')body={version:0,has_image:false,public_name:''};
  else if(p==='/api/v1/auth/csrf')body={token:'test-csrf',headerName:'X-CSRF-TOKEN'};
  else if(p==='/api/v1/design/enquiries/'+id+'/designer-project')body={id,public_id:id,customer_name:'Synthetic customer',product_code:'WEDDING_CARDS',quantity:250,status:'IN_PROGRESS',quote_accepted:true,latest_proof_revision:0,unresolved_feedback_count:0};
  else if(p==='/api/v1/design/enquiries/'+id+'/work')body={id,publicId:id,workflow:{},productCode:'WEDDING_CARDS',quantity:250,status:'IN_PROGRESS',liveWork:true,stages:{},designer:'Test Designer',contact:{email:'designer@example.test',phone:'9000000000'},mailConnected:false,emails:[],logs:[],messages:[]};
  else if(p==='/api/v1/design/enquiries/'+id+'/editor-draft'&&req.method()==='GET')body=draft;
  else if(p==='/api/v1/design/enquiries/'+id+'/editor-draft'&&req.method()==='PUT'){
   const input=req.postDataJSON();if(conflictOnce){conflictOnce=false;await route.fulfill({status:409,contentType:'application/json',body:JSON.stringify({message:'stale revision'})});return;}
   expect(input.expectedRevision).toBe(draft.revision);draft={revision:draft.revision+1,document:input.document,updatedAt:new Date().toISOString()};body=draft;
  }
  else if(p==='/api/v1/design/enquiries/'+id+'/editor-export'&&req.method()==='POST'){exported++;body={artifact:{id:'design-pdf-'+exported,display_name:'Editor-proof.pdf',kind:'DESIGN',state:'CLEAN',media_type:'application/pdf',byte_size:1200,sha256:'a'.repeat(64)},expectedProofRevision:0,summary:'Generated from editor draft'};}
  else if(p==='/api/v1/design/enquiries/'+id+'/artifacts'&&req.method()==='POST')body={id:'synthetic-image',display_name:'sample.png',kind:'DESIGN',state:'CLEAN',media_type:'image/png',byte_size:png.length,sha256:'b'.repeat(64)};
  else if(p==='/api/v1/design/enquiries/'+id+'/proofs'&&req.method()==='POST'){published++;body={};}
  else if(p==='/api/v1/design/enquiries/'+id)body={artifacts:[],proofs:[],comments:[],quoteAccepted:true,currentProofApproved:false,closed:false,referenceEditable:true,artworkRequested:false,referenceFeedback:[],productCode:'WEDDING_CARDS',designerId:'synthetic-designer',tasks:[]};
  else if(p==='/api/v1/design/enquiries/'+id+'/tasks')body=[];
  else if(p==='/api/v1/auth/csrf')body={token:'test-csrf',headerName:'X-CSRF-TOKEN'};
  else if(p.includes('/collaboration/')){await route.fulfill({status:403,contentType:'application/json',body:'{}'});return;}
  else if(p.endsWith('/preview')){await route.fulfill({status:200,contentType:'image/png',body:png});return;}
  else if(p.startsWith('/api/v1/')){body=[];}
  else{await route.fulfill({status:200,contentType:'application/json',body:'{}'});return;}
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
 });
 await page.goto('/designer/work/'+id,{waitUntil:'domcontentloaded'});
 const editor=page.getByRole('region',{name:'Interactive print design editor'});await expect(editor).toBeVisible();
 await expect(editor.getByRole('img',{name:/Print canvas 148 by 210 millimetres/})).toBeVisible();
 await editor.getByRole('button',{name:'Add text'}).click();await expect(editor.locator('svg').getByText('Your text',{exact:true})).toBeVisible();
 await editor.getByRole('button',{name:'Add shape'}).click();await editor.getByRole('button',{name:'Add circle'}).click();
 await expect(editor.locator('.print-editor-layer-list li')).toHaveCount(3);
 await editor.getByRole('button',{name:'Move layer down'}).click();await editor.getByRole('button',{name:'Undo'}).click();await editor.getByRole('button',{name:'Redo'}).click();
 await editor.getByLabel('Width (mm)').fill('90');await editor.getByLabel('Height (mm)').fill('54');await expect(editor.getByRole('img',{name:/Print canvas 90 by 54 millimetres/})).toBeVisible();
 await editor.getByLabel('Starter layout').selectOption('A4_FLYER');await expect(editor.getByRole('img',{name:/Print canvas 210 by 297 millimetres/})).toBeVisible();
 await editor.getByRole('button',{name:'Add text'}).click();await editor.getByRole('button',{name:'Add image'}).click();await editor.locator('input[type=file]').setInputFiles({name:'sample.png',mimeType:'image/png',buffer:png});await expect(editor.locator('svg image')).toHaveCount(1);
 await editor.getByRole('button',{name:'Save draft'}).click();await expect(editor.getByRole('status')).toContainText('Draft saved · revision 1');expect(draft.revision).toBe(1);
 await editor.getByRole('button',{name:'Export and share proof'}).click();await expect.poll(()=>published).toBe(1);await expect.poll(()=>exported).toBe(1);
 await editor.getByRole('button',{name:'Add text'}).click();
 await editor.getByRole('button',{name:'Save draft'}).click();await expect(editor.getByRole('status')).toContainText('Draft saved · revision 2');
 conflictOnce=true;await editor.getByRole('button',{name:'Add text'}).click();await editor.getByRole('button',{name:'Save draft'}).click();await expect(editor.getByRole('alert')).toContainText('Another session saved this design first');
 await editor.getByRole('button',{name:'Reload latest draft'}).click();await expect(editor.getByRole('img',{name:/Print canvas 210 by 297 millimetres/})).toBeVisible();
 const box=await editor.boundingBox();if(box)await page.evaluate(y=>window.scrollTo({top:Math.max(0,y+window.scrollY-90),behavior:'instant'}),box.y);await page.screenshot({path:path.join(evidence,`designer-editor-${testInfo.project.name}.png`),fullPage:false});
 if(testInfo.project.name==='mobile')expect(await page.locator('body').evaluate(el=>el.scrollWidth)).toBeLessThanOrEqual(390);
});

test('customer can save a private artwork draft but cannot publish designer proofs',async({page},testInfo)=>{
 page.setDefaultTimeout(10000);await page.setViewportSize(testInfo.project.name==='mobile'?{width:390,height:844}:{width:1440,height:900});let draft:{revision:number;document:{schemaVersion:1;templateId:string;widthMm:number;heightMm:number;bleedMm:number;safeMarginMm:number;elements:unknown[]};updatedAt:string|null}={revision:0,document:blank,updatedAt:null};
 await page.route('**/api/**',async route=>{const req=route.request(),p=new URL(req.url()).pathname;let body:unknown={};
  if(p==='/api/v1/auth/session')body={id:'synthetic-customer',username:'customer@example.test',role:'CUSTOMER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true};
  else if(p==='/api/v1/auth/csrf')body={token:'test-csrf',headerName:'X-CSRF-TOKEN'};
  else if(p==='/api/v1/auth/appearance')body={version:0,has_image:false,public_name:''};
  else if(p==='/api/public/identity/status')body={deliveryEnabled:false};
  else if(p==='/api/public/sso/providers'||p==='/api/v1/customer/privacy'||p==='/api/v1/customer/sso')body=[];
  else if(p==='/api/v1/customer/profile')body={name:'Synthetic customer',email:'customer@example.test',phone:'9000000000',organization:'',address:''};
  else if(p==='/api/v1/customer/enquiries')body=[{id,public_id:id,product_code:'WEDDING_CARDS',quantity:250,status:'IN_PROGRESS',live_work:true}];
  else if(p==='/api/v1/customer/enquiries/'+id)body={id,public_id:id,product_code:'WEDDING_CARDS',quantity:250,status:'IN_PROGRESS',notes:'Synthetic invitation brief',live_work:true,size:'148 × 210 mm',finish:'MATTE'};
  else if(p==='/api/v1/customer/enquiries/'+id+'/quotes')body=[{id:'synthetic-quote',version:1,scope:'250 invitation cards',total_paise:100000,terms:'Synthetic terms',requested_date:null,proposed_date:'2026-11-10',specification_snapshot:'{}',state:'ACCEPTED',issued_at:'2026-10-25T00:00:00Z'}];
  else if(p==='/api/v1/design/enquiries/'+id+'/payments')body={quoteId:'synthetic-quote',quoteVersion:1,totalPaise:100000,paidPaise:0,balancePaise:100000,overpaidPaise:0,testPaymentsIncludedInTotals:false,stage:'Advance required',policy:null,policyCurrent:false,owner:false,customer:true,closed:false,released:false,receipts:[],invoices:[]};
  else if(p==='/api/v1/design/enquiries/'+id+'/payments/gateway')body={configured:false,mode:'test',webhooksConfigured:false};
  else if(p==='/api/v1/design/enquiries/'+id+'/fulfilment')body={state:'NOT_STARTED',revision:0,owner:false,printer:false,delivery:false,closed:false,issue_open:false,evidenceReadiness:{pickup:'PENDING',delivery:'PENDING'}};
  else if(p==='/api/v1/design/enquiries/'+id+'/editor-draft'&&req.method()==='GET')body=draft;
  else if(p==='/api/v1/design/enquiries/'+id+'/editor-draft'&&req.method()==='PUT'){const input=req.postDataJSON();expect(input.expectedRevision).toBe(draft.revision);draft={revision:draft.revision+1,document:input.document,updatedAt:new Date().toISOString()};body=draft;}
  else if(p==='/api/v1/design/enquiries/'+id)body={artifacts:[],proofs:[],comments:[],quoteAccepted:true,currentProofApproved:false,closed:false,referenceEditable:true,artworkRequested:false,referenceFeedback:[],productCode:'WEDDING_CARDS',designerId:'synthetic-designer',tasks:[]};
  else if(p.endsWith('/samples'))body=[];
  else if(p.endsWith('/work'))body={id,publicId:id,productCode:'WEDDING_CARDS',quantity:250,status:'IN_PROGRESS',liveWork:true,closureMessage:null,workflow:{fulfilmentState:'NOT_STARTED'},stages:{},designer:'Test Designer',contact:{email:'',phone:''},mailConnected:false,logs:[],messages:[]};
  else if(p.startsWith('/api/v1/'))body=[];
  else{await route.fulfill({status:200,contentType:'application/json',body:'{}'});return;}
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
 });
 await page.goto('/customer',{waitUntil:'domcontentloaded'});await page.getByRole('link',{name:/Open work/}).click();
 const editor=page.getByRole('region',{name:'Interactive print design editor'});await expect(editor).toBeVisible();await editor.getByRole('button',{name:'Add text'}).click();await editor.getByRole('button',{name:'Save draft'}).click();await expect(editor.getByRole('status')).toContainText('Draft saved · revision 1');expect(draft.revision).toBe(1);await expect(editor.getByRole('button',{name:'Export and share proof'})).toHaveCount(0);const box=await editor.boundingBox();if(box)await page.evaluate(y=>window.scrollTo({top:Math.max(0,y+window.scrollY-90),behavior:'instant'}),box.y);await page.screenshot({path:path.join(evidence,`customer-editor-${testInfo.project.name}.png`),fullPage:false});if(testInfo.project.name==='mobile')expect(await page.locator('body').evaluate(el=>el.scrollWidth)).toBeLessThanOrEqual(390);
});

test.beforeAll(()=>mkdirSync(evidence,{recursive:true}));
