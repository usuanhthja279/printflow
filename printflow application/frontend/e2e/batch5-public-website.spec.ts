import {test,expect,Page} from '@playwright/test';
import fs from 'node:fs';import path from 'node:path';
const root=path.resolve(process.cwd(),'../output/batch5-visual-evidence');
const product={code:'WEDDING_CARDS',name:'Shaadi & Wedding Cards',description:'Wedding invitations and celebration stationery',minQuantity:100,maxQuantity:100000,sizes:['A5','A4'],gsmOptions:[170,250,300],finishes:['MATTE','GLOSS','UNSURE'],revision:1,active:true};
async function screenshot(page:Page,name:string){fs.mkdirSync(root,{recursive:true});await page.screenshot({path:path.join(root,name),fullPage:false});}
async function publicMocks(page:Page){let enquiryCalls=0,uploadCalls=0;const idempotency:string[]=[];
 await page.route('**/api/**',async route=>{const req=route.request(),url=new URL(req.url()),p=url.pathname.replace(/^\/api/,'');const send=(body:unknown,status=200)=>route.fulfill({status,json:body});
  if(p==='/public/catalog')return send([product]);
  if(p==='/public/templates/WEDDING_CARDS')return send(product);
  if(p==='/public/whatsapp/contact')return send({available:false,number:'',url:''});
  if(p==='/public/enquiries/policy')return send({challengeRequired:false,siteKey:'',privacyVersion:'2026-10'});
  if(p==='/v1/auth/session')return send({message:'Unauthorized'},401);
  if(p==='/public/enquiries'&&req.method()==='POST'){enquiryCalls++;idempotency.push(req.headers()['idempotency-key']||'');if(enquiryCalls===1)return send({message:'Synthetic interrupted response'},503);return send({publicId:'33333333-2222-4333-8444-555555555555',trackingToken:'synthetic-private-token-not-a-real-credential',status:'NEW',createdAt:'2026-10-25T12:00:00Z'},201);}
  if(req.method()==='POST'&&p.includes('/artwork')){uploadCalls++;if(uploadCalls===1)return send({message:'Synthetic upload interruption'},503);return send({id:'artifact-sample',state:'CLEAN'});}
  if(p==='/v1/auth/csrf')return send({token:'csrf-sample',headerName:'X-CSRF-TOKEN'});
  return send([]);
 });
 return {idempotency,enquiryCount:()=>enquiryCalls,uploadCount:()=>uploadCalls};
}

test('public discovery, four-step quote, private consent and attachment retry',async({page},info)=>{
 const mock=await publicMocks(page);const suffix=info.project.name;
 await page.goto('/');await expect(page.getByRole('heading',{name:/Your ideas/})).toBeVisible();await expect(page.getByRole('main').getByRole('link',{name:/Get a print quote/})).toBeVisible();await expect(page.getByText(product.name,{exact:true})).toBeVisible();await expect(page.getByRole('link',{name:'Ask us on WhatsApp'})).toHaveCount(0);await screenshot(page,`public-home-${suffix}.png`);
 await page.goto('/products');await expect(page.getByRole('heading',{name:/Print with/})).toBeVisible();await expect(page.getByRole('link',{name:/Shaadi & Wedding Cards/})).toBeVisible();await screenshot(page,`public-catalogue-${suffix}.png`);
 await page.goto('/products/WEDDING_CARDS');await expect(page.getByRole('heading',{name:product.name})).toBeVisible();await expect(page.getByText('Illustrative sample. Final artwork and finish are agreed with the team.')).toBeVisible();await screenshot(page,`public-product-detail-${suffix}.png`);
 await page.goto('/request-quote?product=WEDDING_CARDS');await expect(page.getByRole('heading',{name:'What are you printing?'})).toBeVisible();await screenshot(page,`quote-step-1-${suffix}.png`);
 await page.getByRole('checkbox',{name:/I am not sure what I need/}).check();await page.getByRole('button',{name:/Continue to Specifications/}).click();await expect(page.getByRole('heading',{name:'What details do you know?'})).toBeVisible();await screenshot(page,`quote-step-2-${suffix}.png`);
 await page.getByRole('button',{name:/Continue to Artwork & contact/}).click();await expect(page.getByRole('heading',{name:'Artwork and contact details'})).toBeVisible();await page.getByRole('radio',{name:/I have artwork to share/}).check();await page.locator('input[type=file]').setInputFiles({name:'sample-artwork.png',mimeType:'image/png',buffer:Buffer.from('synthetic test artwork')});await page.getByLabel('Your name').fill('Synthetic UI Test');await page.getByLabel('Email address').fill('synthetic@example.test');await page.getByLabel('Phone number').fill('9876543210');await screenshot(page,`quote-step-3-${suffix}.png`);
 await page.getByRole('button',{name:/Continue to Review/}).click();await expect(page.getByRole('heading',{name:'Review your request'})).toBeVisible();await expect(page.getByText('Final pricing will be provided by our team after reviewing your request.')).toBeVisible();await expect(page.getByText('Not sure — please advise')).toBeVisible();await expect(page.getByRole('checkbox',{name:/WhatsApp service updates/})).not.toBeChecked();await screenshot(page,`quote-step-4-${suffix}.png`);
 await page.getByRole('checkbox',{name:/I agree to the/}).check();await page.getByRole('button',{name:/Submit print request/}).click();await expect(page.getByText('Synthetic interrupted response',{exact:true})).toBeVisible();await page.getByRole('button',{name:/Submit print request/}).click();await expect(page.getByRole('heading',{name:'Enquiry saved successfully'})).toBeVisible();await expect(page.getByRole('status')).toContainText('Artwork was not attached');expect(mock.enquiryCount()).toBe(2);expect(mock.idempotency[0]).toBeTruthy();expect(mock.idempotency[0]).toBe(mock.idempotency[1]);expect(mock.uploadCount()).toBe(1);await screenshot(page,`quote-artwork-upload-retry-${suffix}.png`);
 await page.getByRole('button',{name:'Upload artwork attachment'}).click();await expect(page.getByRole('status')).toContainText('Artwork attached and scanned successfully');expect(mock.enquiryCount()).toBe(2);expect(mock.uploadCount()).toBe(2);await screenshot(page,`quote-submission-success-${suffix}.png`);
 if(suffix==='mobile')expect(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1)).toBe(true);
});
