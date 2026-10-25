import {expect,test} from '@playwright/test';
import {mkdirSync,readFileSync} from 'node:fs';
import path from 'node:path';

const id='ab6a0000-0000-4000-8000-000000000001';
const evidence=path.resolve('..','output','batch6b-visual-evidence');
const png=readFileSync(path.resolve('e2e','fixtures','proof-v1.png'));
const printFont=readFileSync(path.resolve('node_modules/pdfjs-dist/standard_fonts/LiberationSans-Regular.ttf'));
const pdfjsModule=readFileSync(path.resolve('node_modules/pdfjs-dist/build/pdf.mjs'),'utf8');
const pdfjsWorker=readFileSync(path.resolve('node_modules/pdfjs-dist/build/pdf.worker.mjs'),'utf8');
const comparisonPdf=readFileSync(path.resolve('..','backend','build','test-output','editor-layout-comparison.pdf')).toString('base64');
const blank={schemaVersion:1 as const,templateId:'A4_FLYER',widthMm:210,heightMm:297,bleedMm:3,safeMarginMm:5,elements:[
 {id:'top',type:'TEXT' as const,xMm:12,yMm:8,widthMm:34,heightMm:42,rotationDegrees:0,text:'TOP LINE THAT WRAPS\nSecond line',fontSizePt:14,fill:'#183b43',stroke:'#183b43',strokeMm:0,assetId:null},
 {id:'bottom',type:'TEXT' as const,xMm:12,yMm:266,widthMm:60,heightMm:20,rotationDegrees:0,text:'BOTTOM',fontSizePt:10,fill:'#183b43',stroke:'#183b43',strokeMm:0,assetId:null},
]};

test('print editor supports templates, canvas layers, ordering, undo/redo, save conflict and proof publishing',async({page},testInfo)=>{
 test.setTimeout(90000);page.setDefaultTimeout(10000);page.setDefaultNavigationTimeout(30000);await page.setViewportSize(testInfo.project.name==='mobile'?{width:390,height:844}:{width:1440,height:900});
 let draft:{revision:number;document:{schemaVersion:number;templateId:string;widthMm:number;heightMm:number;bleedMm:number;safeMarginMm:number;elements:unknown[];pages?:{id:string;name:string;elements:unknown[]}[];activePageId?:string};updatedAt:string|null}={revision:0,document:blank,updatedAt:null},conflictOnce=false,published=0,exported=0;
 await page.route('**/api/**',async route=>{
  const req=route.request(),url=new URL(req.url()),p=url.pathname;let body:unknown={};
  if(p==='/api/v1/design/editor-font'){await route.fulfill({status:200,contentType:'font/ttf',body:printFont});return;}
  else if(p==='/api/v1/auth/session')body={id:'synthetic-designer',username:'designer@example.test',role:'DESIGNER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true};
  else if(p==='/api/v1/auth/appearance')body={version:0,has_image:false,public_name:''};
  else if(p==='/api/v1/auth/csrf')body={token:'test-csrf',headerName:'X-CSRF-TOKEN'};
  else if(p==='/api/v1/design/enquiries/'+id+'/designer-project')body={id,public_id:id,customer_name:'Synthetic customer',product_code:'WEDDING_CARDS',quantity:250,status:'IN_PROGRESS',quote_accepted:true,latest_proof_revision:0,unresolved_feedback_count:0};
  else if(p==='/api/v1/design/enquiries/'+id+'/work')body={id,publicId:id,workflow:{},productCode:'WEDDING_CARDS',quantity:250,status:'IN_PROGRESS',liveWork:true,stages:{},designer:'Test Designer',contact:{email:'designer@example.test',phone:'9000000000'},mailConnected:false,emails:[],logs:[],messages:[]};
  else if(p==='/api/v1/design/enquiries/'+id+'/editor-draft'&&req.method()==='GET')body=draft;
  else if(p==='/api/v1/design/enquiries/'+id+'/editor-draft'&&req.method()==='PUT'){
   const input=req.postDataJSON();if(conflictOnce){conflictOnce=false;await route.fulfill({status:409,contentType:'application/json',body:JSON.stringify({message:'stale revision'})});return;}
   expect(input.expectedRevision).toBe(draft.revision);expect(input.document.safeMarginMm).toBeLessThanOrEqual(50);draft={revision:draft.revision+1,document:input.document,updatedAt:new Date().toISOString()};body=draft;
  }
  else if(p==='/api/v1/design/enquiries/'+id+'/editor-assistant/suggestions'&&req.method()==='POST')body={provider:'LOCAL SUGGESTION',externalCallsEnabled:false,draftRevision:draft.revision,suggestions:[{id:'center-front-title',provider:'LOCAL SUGGESTION',title:'Center this text block',rationale:'Synthetic deterministic fixture.',pageId:'page-1',layerId:'template-title',field:'xMm',before:'18',after:'46'}]};
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
 await page.route('**/__pdfjs.mjs',route=>route.fulfill({status:200,contentType:'text/javascript',body:pdfjsModule}));
 await page.route('**/__pdfjs.worker.mjs',route=>route.fulfill({status:200,contentType:'text/javascript',body:pdfjsWorker}));
 await page.goto('/designer/work/'+id,{waitUntil:'domcontentloaded'});
 const editor=page.getByRole('region',{name:'Interactive print design editor'});await expect(editor).toBeVisible();
 await expect(editor.getByRole('img',{name:/Print canvas 210 by 297 millimetres/})).toBeVisible();
 await page.evaluate(()=>document.fonts.ready);
 await page.evaluate(async()=>{const load=new Function("return import('/__pdfjs.mjs')") as ()=>Promise<any>;const lib=await load();lib.GlobalWorkerOptions.workerSrc='/__pdfjs.worker.mjs';(window as unknown as {pdfjsLib:any}).pdfjsLib=lib;});
 const rasterComparison=await page.evaluate(async({pdfBase64,fontBase64})=>{
  const lib=(window as unknown as {pdfjsLib:any}).pdfjsLib,bytes=Uint8Array.from(atob(pdfBase64),c=>c.charCodeAt(0)),documentPdf=await lib.getDocument({data:bytes}).promise,pdfPage=await documentPdf.getPage(1),viewport=pdfPage.getViewport({scale:1});
  const pdfCanvas=document.createElement('canvas');pdfCanvas.width=Math.ceil(viewport.width);pdfCanvas.height=Math.ceil(viewport.height);const pdfContext=pdfCanvas.getContext('2d')!;await pdfPage.render({canvas:pdfCanvas,canvasContext:pdfContext,viewport}).promise;
  const svg=document.querySelector('.print-editor-svg')!.cloneNode(true) as SVGSVGElement;Array.from(svg.children).forEach(child=>{if(!['defs','g'].includes(child.tagName.toLowerCase()))child.remove();});svg.setAttribute('width',String(viewport.width));svg.setAttribute('height',String(viewport.height));const style=document.createElementNS('http://www.w3.org/2000/svg','style');style.textContent=`@font-face{font-family:'J.D. Print Sans';src:url(data:font/ttf;base64,${fontBase64}) format('truetype')} text{font-family:'J.D. Print Sans'}`;svg.insertBefore(style,svg.firstChild);
  const svgCanvas=document.createElement('canvas');svgCanvas.width=pdfCanvas.width;svgCanvas.height=pdfCanvas.height;const svgContext=svgCanvas.getContext('2d')!;svgContext.fillStyle='#fff';svgContext.fillRect(0,0,svgCanvas.width,svgCanvas.height);const blob=new Blob([new XMLSerializer().serializeToString(svg)],{type:'image/svg+xml'}),url=URL.createObjectURL(blob),image=new Image();image.src=url;await image.decode();svgContext.drawImage(image,0,0,svgCanvas.width,svgCanvas.height);URL.revokeObjectURL(url);
  const pdfData=pdfContext.getImageData(0,0,pdfCanvas.width,pdfCanvas.height).data,svgData=svgContext.getImageData(0,0,svgCanvas.width,svgCanvas.height).data;let overlap=0,union=0;for(let i=0;i<pdfData.length;i+=4){const p=pdfData[i]<235&&pdfData[i+1]<235&&pdfData[i+2]<235,s=svgData[i]<235&&svgData[i+1]<235&&svgData[i+2]<235;if(p||s)union++;if(p&&s)overlap++;}return {iou:union?overlap/union:0,pdfWidth:pdfCanvas.width,svgWidth:svgCanvas.width};
 },{pdfBase64:comparisonPdf,fontBase64:printFont.toString('base64')});
 expect(rasterComparison.pdfWidth).toBe(rasterComparison.svgWidth);expect(rasterComparison.iou).toBeGreaterThan(.72);
 await editor.getByRole('button',{name:'Add text'}).click();await expect(editor.locator('svg').getByText('Your text',{exact:true})).toBeVisible();
 if(testInfo.project.name==='desktop'){
  const x=editor.getByLabel('X (mm)'),originalX=Number(await x.inputValue()),layer=editor.locator('.print-editor-layer').last();await layer.scrollIntoViewIfNeeded();const box=await layer.boundingBox();expect(box).not.toBeNull();
  await page.mouse.move(box!.x+box!.width/2,box!.y+box!.height/2);await page.mouse.down();await page.mouse.move(box!.x+box!.width/2+36,box!.y+box!.height/2+18,{steps:4});await page.mouse.up();
  await expect.poll(async()=>Number(await x.inputValue())).toBeGreaterThan(originalX+10);const movedX=Number(await x.inputValue());
  await editor.getByRole('button',{name:'Undo'}).click();await expect(x).toHaveValue(String(originalX));
  await editor.getByRole('button',{name:'Redo'}).click();await expect(x).toHaveValue(String(movedX));
  const resize=editor.getByLabel('Resize selected layer'),width=editor.getByLabel('Width',{exact:true}),originalWidth=Number(await width.inputValue());await resize.scrollIntoViewIfNeeded();const resizeBox=await resize.boundingBox();expect(resizeBox).not.toBeNull();
  await page.mouse.move(resizeBox!.x+resizeBox!.width/2,resizeBox!.y+resizeBox!.height/2);await page.mouse.down();await page.mouse.move(resizeBox!.x+resizeBox!.width/2+30,resizeBox!.y+resizeBox!.height/2+20,{steps:3});await page.mouse.up();
  await expect.poll(async()=>Number(await width.inputValue())).toBeGreaterThan(originalWidth+5);const resizedWidth=Number(await width.inputValue());
  await editor.getByRole('button',{name:'Undo'}).click();await expect(width).toHaveValue(String(originalWidth));await editor.getByRole('button',{name:'Redo'}).click();await expect(width).toHaveValue(String(resizedWidth));
 }
 await editor.getByRole('button',{name:'Add shape'}).click();await editor.getByRole('button',{name:'Add circle'}).click();
 await expect(editor.locator('.print-editor-layer-list li')).toHaveCount(5);
 await editor.getByRole('button',{name:'Move layer down'}).click();await editor.getByRole('button',{name:'Undo'}).click();await editor.getByRole('button',{name:'Redo'}).click();
 await editor.getByLabel('Width (mm)').fill('90');await editor.getByLabel('Height (mm)').fill('54');await expect(editor.getByRole('img',{name:/Print canvas 90 by 54 millimetres/})).toBeVisible();
 await editor.getByLabel('Starter layout').selectOption('A4_FLYER');await expect(editor.getByRole('img',{name:/Print canvas 210 by 297 millimetres/})).toBeVisible();
 await test.step('A4 safe-margin input clamps to the backend 50 mm maximum',async()=>{const safe=editor.getByLabel('Safe margin');await safe.fill('75');await expect(safe).toHaveValue('50');});
 await editor.getByRole('button',{name:'Add page'}).click();await editor.getByLabel('Page name').fill('Inside cover');await editor.getByRole('button',{name:'Add text'}).click();expect(await editor.locator('.print-editor-page-thumb').count()).toBe(2);await editor.getByRole('button',{name:/1\. Page 1/}).click();await expect(editor.locator('.print-editor-layer-list li')).toHaveCount(2);await editor.getByRole('button',{name:'Duplicate'}).click();expect(await editor.locator('.print-editor-page-thumb').count()).toBe(3);await editor.getByRole('button',{name:'Delete'}).click();await expect(editor.locator('.print-editor-page-thumb')).toHaveCount(2);await editor.getByRole('button',{name:'Add image'}).click();await editor.locator('input[type=file]').setInputFiles({name:'sample.png',mimeType:'image/png',buffer:png});await expect(editor.locator('svg image')).toHaveCount(1);await editor.getByLabel('Image fit').selectOption('COVER');await expect(editor.locator('svg image')).toHaveAttribute('preserveAspectRatio','xMidYMid slice');
 await editor.getByRole('button',{name:'Save draft'}).click();await expect(editor.getByRole('status')).toContainText('Draft saved · revision 1');expect(draft.revision).toBe(1);expect(draft.document.safeMarginMm).toBe(50);expect(draft.document.schemaVersion).toBe(2);expect(draft.document.pages).toHaveLength(2);expect(draft.document.pages?.[1]?.name).toBe('Inside cover');expect(draft.document.pages?.[1]?.elements).toHaveLength(2);
 await editor.getByRole('button',{name:'Export and share proof'}).click();await expect.poll(()=>published).toBe(1);await expect.poll(()=>exported).toBe(1);
 await editor.getByRole('button',{name:'Add text'}).click();
 await editor.getByRole('button',{name:'Save draft'}).click();await expect(editor.getByRole('status')).toContainText('Draft saved · revision 2');
 conflictOnce=true;await editor.getByRole('button',{name:'Add text'}).click();await editor.getByRole('button',{name:'Save draft'}).click();await expect(editor.getByRole('alert')).toContainText('Another session saved this design first');
 await editor.getByRole('button',{name:'Reload latest draft'}).click();await expect(editor.getByRole('img',{name:/Print canvas 210 by 297 millimetres/})).toBeVisible();await editor.getByRole('button',{name:'Suggest improvements'}).click();await expect(editor.getByText(/LOCAL SUGGESTION · based on saved draft/)).toBeVisible();await editor.getByRole('button',{name:'Preview',exact:true}).click();await editor.getByRole('button',{name:'Apply',exact:true}).click();await expect(editor.getByLabel('X (mm)')).toHaveValue('46');await editor.getByRole('button',{name:'Undo'}).click();await expect(editor.getByLabel('X (mm)')).toHaveValue('18');
 const box=await editor.boundingBox();if(box)await page.evaluate(y=>window.scrollTo({top:Math.max(0,y+window.scrollY-90),behavior:'instant'}),box.y);await page.screenshot({path:path.join(evidence,`designer-editor-${testInfo.project.name}.png`),fullPage:false});
 if(testInfo.project.name==='mobile')expect(await page.locator('body').evaluate(el=>el.scrollWidth)).toBeLessThanOrEqual(390);
});

test('customer can save a private artwork draft but cannot publish designer proofs',async({page},testInfo)=>{
 page.setDefaultTimeout(10000);await page.setViewportSize(testInfo.project.name==='mobile'?{width:390,height:844}:{width:1440,height:900});let draft:{revision:number;document:{schemaVersion:number;templateId:string;widthMm:number;heightMm:number;bleedMm:number;safeMarginMm:number;elements:unknown[];pages?:{id:string;name:string;elements:unknown[]}[];activePageId?:string};updatedAt:string|null}={revision:0,document:blank,updatedAt:null};
 await page.route('**/api/**',async route=>{const req=route.request(),p=new URL(req.url()).pathname;let body:unknown={};
  if(p==='/api/v1/design/editor-font'){await route.fulfill({status:200,contentType:'font/ttf',body:printFont});return;}
  else if(p==='/api/v1/auth/session')body={id:'synthetic-customer',username:'customer@example.test',role:'CUSTOMER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true};
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
