import {test,expect} from '@playwright/test';
const leadId='10000000-0000-4000-8000-000000000001',quoteId='10000000-0000-4000-8000-000000000002',ownerId='10000000-0000-4000-8000-000000000003';
const lead={id:leadId,publicId:leadId,customerName:'Synthetic proof customer',email:'proof-ui@example.test',phone:'9000000000',productCode:'WEDDING_CARDS',quantity:250,status:'REVIEWING',source:'WEBSITE',priority:'NORMAL',assignedStaffId:ownerId,nextAction:null,followUpAt:null,revision:0,createdAt:'2026-10-24T00:00:00Z'};
for(const quarantine of [false,true])test(quarantine?'quarantined uploads cannot be downloaded or shared':'design upload, sharing, customer comment and exact proof approval',async({page},info)=>{
 page.setDefaultTimeout(10000);
 let customer=false;const artifacts:any[]=[],proofs:any[]=[],comments:any[]=[];let approved=false;
 const workspace=()=>({artifacts:customer?artifacts.filter(a=>a.kind==='REFERENCE'||proofs.some(p=>p.artifact_id===a.id)):artifacts,proofs,comments,quoteAccepted:true,currentProofApproved:approved,closed:false,designerId:null});
 await page.route('**/api/**',async route=>{const req=route.request(),url=new URL(req.url()),path=url.pathname;let body:unknown={},status=200;if(path.endsWith('/preview')){await route.fulfill({status:200,contentType:'image/png',body:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6XKwAAAAASUVORK5CYII=','base64')});return;}
  if(path==='/api/v1/auth/appearance')body={version:0,has_image:false,public_name:''};
  else if(path.endsWith('/work'))body={liveWork:false,status:'REVIEWING',messages:[],mailConnected:false};
  else if(path==='/api/v1/auth/session')body={id:ownerId,username:'proof-ui@example.test',role:customer?'CUSTOMER':'OWNER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true};
  else if(path==='/api/v1/auth/csrf')body={token:'synthetic-csrf',headerName:'X-CSRF-TOKEN'};
  else if(path==='/api/v1/owner/staff')body=[];
  else if(path==='/api/v1/staff/enquiries')body=[lead];
  else if(path.endsWith('/activities'))body=[];
  else if(path.endsWith('/quotes'))body=[{id:quoteId,version:1,scope:'250 cards',total_paise:125050,terms:'Synthetic agreement',requested_date:null,proposed_date:'2026-11-10',valid_until:'2026-11-01',specification_snapshot:'{}',state:'ACCEPTED',issued_at:'2026-10-24T00:00:00Z',response_note:null}];
  else if(path==='/api/v1/design/enquiries/'+leadId)body=workspace();
  else if(path.endsWith('/artifacts')&&req.method()==='POST'){expect(req.headers()['x-csrf-token']).toBe('synthetic-csrf');expect(req.headers()['content-type']).toBe('image/png');expect(req.postDataBuffer()!.length).toBeGreaterThan(0);const artifact={id:'10000000-0000-4000-8000-000000000004',display_name:'synthetic-proof.png',byte_size:100,sha256:'a'.repeat(64),kind:'DESIGN',state:quarantine?'QUARANTINED':'CLEAN'};artifacts.push(artifact);body=artifact;status=201;}
  else if(path.endsWith('/proofs')&&req.method()==='POST'){const input=req.postDataJSON();expect(input.expectedRevision).toBe(0);proofs.unshift({id:'10000000-0000-4000-8000-000000000005',revision:1,artifact_id:artifacts[0].id,quote_id:quoteId,summary:input.summary,display_name:'synthetic-proof.png',sha256:'a'.repeat(64),scan_state:'CLEAN',decision:null,decision_note:null,decided_at:null});body=workspace();}
  else if(path.endsWith('/comments')){const input=req.postDataJSON();expect(input.pageNumber).toBe(1);comments.push({id:'comment-1',proof_id:proofs[0].id,author_role:'CUSTOMER',body:input.body,page_number:input.pageNumber,created_at:'2026-10-24T00:00:00Z'});body=workspace();}
  else if(path.endsWith('/decision')){const input=req.postDataJSON();expect(input).toMatchObject({action:'APPROVE',artifactHash:'a'.repeat(64),quoteId,reviewed:true});approved=true;proofs[0].decision='APPROVE';proofs[0].decided_at='2026-10-24T00:00:00Z';body=workspace();}
  else if(path==='/api/public/identity/status')body={deliveryEnabled:false};
  else if(path==='/api/v1/customer/profile')body={name:'Synthetic proof customer',email:'proof-ui@example.test',phone:'9000000000',address:'',organization:''};
  else if(path==='/api/v1/customer/enquiries')body=[{id:leadId,public_id:leadId,product_code:'WEDDING_CARDS',quantity:250,status:'REVIEWING'}];
  else if(path==='/api/v1/customer/enquiries/'+leadId)body={id:leadId,public_id:leadId,product_code:'WEDDING_CARDS',quantity:250,status:'REVIEWING',notes:'Synthetic proof job'};
  else if(path==='/api/v1/customer/privacy'||path==='/api/v1/customer/sso')body=[];
  else if(path==='/api/public/sso/providers')body=[];
  else throw Error('Unexpected design UI fixture route: '+path);
  await route.fulfill({status,contentType:'application/json',body:JSON.stringify(body)});
 });
 await page.goto('/business');await page.getByRole('button',{name:'Open enquiry',exact:true}).click();let design=page.getByRole('region',{name:'Design proofs',exact:true});await expect(design.getByText('No proof has been shared yet.')).toBeVisible();
 await design.getByLabel('Choose artwork',{exact:true}).setInputFiles({name:'synthetic-proof.png',mimeType:'image/png',buffer:Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAusB9Wl6XKwAAAAASUVORK5CYII=','base64')});await design.getByRole('button',{name:'Upload file',exact:true}).click();
 if(quarantine){await expect(design.getByRole('status')).toContainText('quarantine');await expect(design.getByRole('link',{name:/Download/})).toHaveCount(0);await expect(design.getByRole('button',{name:'Share proof',exact:true})).toBeDisabled();await page.screenshot({path:info.outputPath('quarantine.png'),fullPage:false});return;}
 await expect(design.getByRole('status')).toContainText('passed scanning');await design.getByRole('combobox',{name:/Clean design file/}).selectOption(artifacts[0].id);await design.getByLabel('Revision summary',{exact:true}).fill('Names and layout ready for customer review');await design.getByRole('button',{name:'Share proof',exact:true}).click();await expect(design.getByRole('heading',{name:'Proof revision 1',exact:true})).toBeVisible();
 customer=true;await page.goto('/customer');await page.getByRole('button',{name:'View enquiry',exact:true}).click();design=page.getByRole('region',{name:'Design proofs',exact:true});await expect(design.getByRole('heading',{name:'Proof revision 1',exact:true})).toBeVisible();await expect(design.getByRole('button',{name:'Approve proof revision 1',exact:true})).toBeDisabled();
 await design.getByLabel('Comment on revision 1',{exact:true}).fill('Names and spelling checked');await design.getByLabel('Page / side (optional)',{exact:true}).fill('1');await design.getByRole('button',{name:'Add comment',exact:true}).click();await expect(design.getByText('Names and spelling checked',{exact:true})).toBeVisible();await design.getByRole('checkbox').check();await design.getByRole('button',{name:'Approve proof revision 1',exact:true}).click();await expect(design.getByRole('status')).toContainText('Proof approved');await expect(design.getByRole('button',{name:'Approve proof revision 1',exact:true})).toHaveCount(0);expect(lead.status).toBe('REVIEWING');await page.screenshot({path:info.outputPath('approved-proof.png'),fullPage:true});
});
