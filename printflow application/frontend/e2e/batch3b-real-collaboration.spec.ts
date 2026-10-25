import {expect,test,type BrowserContext, type Page} from '@playwright/test';
import {execFileSync} from 'node:child_process';
import {mkdirSync,readFileSync} from 'node:fs';
import path from 'node:path';

const root=path.resolve('..');
const evidence=path.join(root,'output','batch3b-visual-evidence','connected');
const fixture=JSON.parse(readFileSync(path.join(root,'.local','batch3b-real-acceptance','credentials.json'),'utf8')) as {
 projectId:string;customerId:string;customerUsername:string;customerPassword:string;designerId:string;designerUsername:string;designerPassword:string;ownerUsername:string;ownerPassword:string
};
const proofImage=path.join(root,'.local','batch3b-real-acceptance','proof-sample.png');

async function signInCustomer(page:Page){
 await page.goto('/customer');await page.getByLabel('Email address').fill(fixture.customerUsername);await page.getByLabel('Password').fill(fixture.customerPassword);await page.getByRole('button',{name:'Sign in'}).click();
 await expect(page.getByText('Signed in as '+fixture.customerUsername)).toBeVisible();
}
async function signInDesigner(page:Page){
 await page.goto('/business');await page.getByLabel('Staff username').fill(fixture.designerUsername);await page.getByLabel('Password').fill(fixture.designerPassword);await page.getByRole('button',{name:'Sign in'}).click();
 await expect(page.getByRole('heading',{name:'Design workspace'})).toBeVisible();
}
async function observeRealWebSockets(context:BrowserContext){
 await context.addInitScript(()=>{
  const Native=window.WebSocket;const frames:string[]=[],groups:string[][]=[],sockets:WebSocket[]=[];Object.defineProperty(window,'__collaborationFrames',{value:frames,configurable:true});Object.defineProperty(window,'__collaborationGroups',{value:groups,configurable:true});Object.defineProperty(window,'__collaborationSockets',{value:sockets,configurable:true});
  class ObservedWebSocket extends Native{private group:string[];constructor(...args:ConstructorParameters<typeof WebSocket>){super(...args);this.group=[];groups.push(this.group);sockets.push(this);}send(data:string|ArrayBufferLike|Blob|ArrayBufferView){if(typeof data==='string'){frames.push(data);this.group.push(data);}return super.send(data);}}
  window.WebSocket=ObservedWebSocket;
 });
}
async function hasContextReplay(page:Page){
 return page.evaluate(()=>{
  const groups=(window as Window&{__collaborationGroups?:string[][]}).__collaborationGroups||[];const frames=groups.at(-1)||[];
  const subscribe=frames.findIndex(f=>f.startsWith('SUBSCRIBE\n')&&f.includes('/user/queue/collaboration/'));
  const replay=frames.findIndex((f,i)=>i>subscribe&&f.startsWith('SEND\n')&&f.includes('/app/collaboration/')&&f.includes('"proofRevisionId"'));
  if(subscribe<0||replay<0)return false;
  const raw=frames[replay],body=raw.slice(raw.indexOf('\n\n')+2).split('\u0000')[0];
  try{const value=JSON.parse(body);return value.page===1&&value.hidden===false;}catch{return false;}
 });
}
async function forceWebSocketReconnect(page:Page){await page.evaluate(()=>{const sockets=(window as Window&{__collaborationSockets?:WebSocket[]}).__collaborationSockets||[];sockets.at(-1)?.close(3001,'acceptance reconnect test');});}
function databaseFixtureAction(mode:'unassign'|'expire-customer'|'cleanup'){
 const script=`$ErrorActionPreference='Stop'; foreach($line in Get-Content '../.env'){if($line -match '^([A-Z][A-Z0-9_]*)=(.*)$'){[Environment]::SetEnvironmentVariable($Matches[1],$Matches[2],'Process')}}; $env:APP_MAIL_ENABLED='false'; $env:DB_URL='jdbc:postgresql://127.0.0.1:5432/'+$env:POSTGRES_DB; $env:DB_USERNAME=$env:POSTGRES_USER; $env:DB_PASSWORD=$env:POSTGRES_PASSWORD; $cp=(Get-Content -Raw '../.local/integration-classpath.txt').Trim(); & 'C:\\Users\\usuan\\.jdks\\dragonwell-21.0.12\\bin\\java.exe' -cp $cp com.printflow.CollaborationBrowserFixture ${mode}; if($LASTEXITCODE -ne 0){exit $LASTEXITCODE}`;
 execFileSync('powershell.exe',['-NoProfile','-Command',script],{cwd:path.join(root,'backend'),stdio:'pipe',encoding:'utf8'});
}
async function rawStompError(context:BrowserContext,projectId:string){
 const page=await context.newPage();await page.goto('/customer');
 const result=await page.evaluate(async ({projectId})=>{
  const csrf=await fetch('/api/v1/auth/csrf',{credentials:'same-origin'}).then(r=>r.json());
  return await new Promise<string>((resolve,reject)=>{
   const ws=new WebSocket('ws://127.0.0.1:8080/collaboration/ws',['v12.stomp']);const timer=setTimeout(()=>{ws.close();reject(new Error('Timed out waiting for STOMP authorization response'));},8000);
   ws.onopen=()=>ws.send('CONNECT\naccept-version:1.2\nheart-beat:0,0\n'+csrf.headerName+':'+csrf.token+'\n\n\u0000');
   ws.onmessage=event=>{const text=String(event.data);if(text.startsWith('CONNECTED'))ws.send('SUBSCRIBE\nid:unauthorized\ndestination:/user/queue/collaboration/'+projectId+'\n\n\u0000');else if(text.startsWith('ERROR')){clearTimeout(timer);ws.close();resolve('ERROR');}};
   ws.onerror=()=>{clearTimeout(timer);reject(new Error('WebSocket handshake failed'))};
  });
 },{projectId});await page.close();return result;
}

test('real customer and assigned designer collaborate, recover, and lose access after revocation',async({browser})=>{
 test.setTimeout(180000);mkdirSync(evidence,{recursive:true});
 const customerContext=await browser.newContext({viewport:{width:1440,height:900}}),designerContext=await browser.newContext({viewport:{width:1440,height:900}});
  await observeRealWebSockets(customerContext);await observeRealWebSockets(designerContext);
 const customer=await customerContext.newPage(),designer=await designerContext.newPage();
 try{
  await signInCustomer(customer);await signInDesigner(designer);
  await customer.goto('/customer/work/'+fixture.projectId);await designer.goto('/designer/work/'+fixture.projectId);
  await expect(customer.locator('.collaboration-status')).toContainText('Live collaboration connected.');await expect(designer.locator('.collaboration-status')).toContainText('Live collaboration connected.');

  await designer.getByLabel('Choose artwork').setInputFiles({name:'proof-v1.png',mimeType:'image/png',buffer:readFileSync(proofImage)});await designer.getByRole('button',{name:'Upload file'}).click();
  await expect(designer.locator('#design-files').getByText('proof-v1.png',{exact:true})).toBeVisible();
  await designer.getByLabel('Clean design file').selectOption({label:'proof-v1.png'});await designer.getByLabel('Revision summary').fill('Synthetic first proof for live collaboration acceptance');await designer.getByRole('button',{name:'Share proof'}).click();
  await expect(customer.getByText('A new proof revision is available.')).toBeVisible();await customer.getByRole('button',{name:'Load new revision'}).click();
  const customerCanvas=customer.getByRole('button',{name:'Mark a change on the proof'}),designerCanvas=designer.getByRole('button',{name:'Mark a change on the proof'});
  await expect(customerCanvas).toBeVisible();await expect(designerCanvas).toBeVisible();
  await expect(customer.locator('.presence-pill')).toContainText('Designer');await expect(designer.locator('.presence-pill')).toContainText('Customer');
  await customerCanvas.scrollIntoViewIfNeeded();await designerCanvas.scrollIntoViewIfNeeded();
  const cr=await customerCanvas.boundingBox(),dr=await designerCanvas.boundingBox();if(!cr||!dr)throw new Error('Proof canvas not measurable');
  await customer.mouse.move(cr.x+cr.width*.38,cr.y+cr.height*.43);await designer.mouse.move(dr.x+dr.width*.64,dr.y+dr.height*.57);
  await expect(customer.locator('.proof-remote-cursor')).toHaveCount(1);await expect(designer.locator('.proof-remote-cursor')).toHaveCount(1);
  await expect(customer.locator('.proof-remote-cursor').first()).toContainText('Designer');await expect(designer.locator('.proof-remote-cursor').first()).toContainText('Customer');

  await customer.getByLabel('Comment on revision 1').fill('Synthetic customer comment: please align the title to the floral border.');await customer.getByRole('button',{name:'Add comment'}).click();
  await expect(designer.getByRole('complementary',{name:'Revision 1 comments and approval'}).getByText('Synthetic customer comment: please align the title to the floral border.',{exact:true})).toBeVisible();
  await customer.getByLabel('Message the team').fill('Synthetic customer message for collaboration acceptance.');await customer.getByRole('button',{name:'Send message'}).click();
  await expect(designer.getByRole('region',{name:'Work conversation'}).getByText('Synthetic customer message for collaboration acceptance.',{exact:true})).toBeVisible();

  await customerCanvas.scrollIntoViewIfNeeded();await designerCanvas.scrollIntoViewIfNeeded();
  const cdesktop=await customerCanvas.boundingBox(),ddesktop=await designerCanvas.boundingBox();if(!cdesktop||!ddesktop)throw new Error('Desktop proof view is not visible');
  await customer.mouse.move(cdesktop.x+cdesktop.width*.38,cdesktop.y+cdesktop.height*.43);await designer.mouse.move(ddesktop.x+ddesktop.width*.64,ddesktop.y+ddesktop.height*.57);
  await expect(customer.locator('.proof-remote-cursor')).toHaveCount(1);await expect(designer.locator('.proof-remote-cursor')).toHaveCount(1);
  await customer.screenshot({path:path.join(evidence,'customer-connected-desktop.png'),fullPage:true});await designer.screenshot({path:path.join(evidence,'designer-connected-desktop.png'),fullPage:true});
  await customer.screenshot({path:path.join(evidence,'customer-connected-desktop-viewport.png')});await designer.screenshot({path:path.join(evidence,'designer-connected-desktop-viewport.png')});
  await customer.setViewportSize({width:390,height:844});await designer.setViewportSize({width:390,height:844});
  expect(await customer.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);expect(await designer.evaluate(()=>document.documentElement.scrollWidth)).toBeLessThanOrEqual(390);
  await customerCanvas.scrollIntoViewIfNeeded();await designerCanvas.scrollIntoViewIfNeeded();
  const cmobile=await customerCanvas.boundingBox(),dmobile=await designerCanvas.boundingBox();if(!cmobile||!dmobile)throw new Error('Mobile proof view is not visible');
  await customer.mouse.move(cmobile.x+cmobile.width*.38,cmobile.y+cmobile.height*.43);await designer.mouse.move(dmobile.x+dmobile.width*.64,dmobile.y+dmobile.height*.57);
  await expect(customer.locator('.proof-remote-cursor')).toHaveCount(1);await expect(designer.locator('.proof-remote-cursor')).toHaveCount(1);
  await customer.screenshot({path:path.join(evidence,'customer-connected-mobile.png'),fullPage:true});await designer.screenshot({path:path.join(evidence,'designer-connected-mobile.png'),fullPage:true});
  await customer.screenshot({path:path.join(evidence,'customer-connected-mobile-viewport.png')});await designer.screenshot({path:path.join(evidence,'designer-connected-mobile-viewport.png')});

  await designer.getByLabel('Choose artwork').setInputFiles({name:'proof-v2.png',mimeType:'image/png',buffer:readFileSync(proofImage)});await designer.getByRole('button',{name:'Upload file'}).click();
  await expect(designer.locator('#design-files').getByText('proof-v2.png',{exact:true})).toBeVisible();await designer.getByLabel('Clean design file').selectOption({label:'proof-v2.png'});await designer.getByLabel('Revision summary').fill('Synthetic replacement proof for revision notification');await designer.getByRole('button',{name:'Share proof'}).click();
  await expect(customer.getByText('A new proof revision is available.')).toBeVisible();await customer.getByRole('button',{name:'Load new revision'}).click();await expect(customer.getByRole('heading',{name:'Proof revision 2'})).toBeVisible();

  await forceWebSocketReconnect(customer);await expect.poll(()=>customer.evaluate(()=>((window as Window&{__collaborationGroups?:string[][]}).__collaborationGroups||[]).length)).toBeGreaterThan(1);
  await expect(customer.locator('.collaboration-status')).toContainText('Live collaboration connected.',{timeout:30000});await expect.poll(()=>hasContextReplay(customer)).toBe(true);await expect(customer.locator('.presence-pill')).toContainText('Designer · Page 1');
  await designerCanvas.scrollIntoViewIfNeeded();const replayCursor=await designerCanvas.boundingBox();if(!replayCursor)throw new Error('Designer proof canvas missing after reconnect');await designer.mouse.move(replayCursor.x+replayCursor.width*.52,replayCursor.y+replayCursor.height*.42);await expect(customer.locator('.proof-remote-cursor')).toHaveCount(1);
  await expect(await rawStompError(customerContext,'00000000-0000-4000-8000-000000000001')).toBe('ERROR');
  const crossProjectStatus=await customer.evaluate(async id=>(await fetch('/api/v1/design/enquiries/'+id,{credentials:'same-origin'})).status,'00000000-0000-4000-8000-000000000002');expect(crossProjectStatus).toBe(404);
  databaseFixtureAction('unassign');
  await expect(designer.locator('.collaboration-status')).toContainText(/unavailable|reconnecting|access ended/i,{timeout:30000});
  await designer.goto('/designer/work/'+fixture.projectId);await expect(designer.getByRole('heading',{name:'Project unavailable'})).toBeVisible();

  const ownerContext=await browser.newContext(),owner=await ownerContext.newPage();
  try{await owner.goto('/business');await owner.getByLabel('Staff username').fill(fixture.ownerUsername);await owner.getByLabel('Password').fill(fixture.ownerPassword);await owner.getByRole('button',{name:'Sign in'}).click();await expect(owner.getByText('Set up an authenticator app to protect owner access.')).toBeVisible();const ownerSession=await owner.evaluate(async()=>fetch('/api/v1/auth/session',{credentials:'same-origin'}).then(r=>r.json()));expect(ownerSession).toMatchObject({role:'OWNER',mfaRequired:true,mfaRecent:false});}
  finally{await ownerContext.close();}
  databaseFixtureAction('expire-customer');
  await customer.waitForTimeout(12000);
  await expect(customer.locator('.collaboration-status')).toContainText(/reconnecting|unavailable|access ended/i,{timeout:20000});
 }finally{await customerContext.close();await designerContext.close();databaseFixtureAction('cleanup');}
});
