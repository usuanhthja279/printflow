import {test,expect} from '@playwright/test';
const id='00000000-0000-4000-8000-000000000001';
const lead={id:'00000000-0000-4000-8000-000000000002',publicId:'00000000-0000-4000-8000-000000000003',customerName:'Synthetic print customer',email:'layout@example.test',phone:'9000000000',productCode:'WEDDING_CARDS',quantity:250,notes:'Synthetic print requirements',status:'REVIEWING',source:'WEBSITE',priority:'HIGH',assignedStaffId:id,nextAction:'Confirm paper selection',followUpAt:'2026-10-22T10:00:00Z',revision:0,createdAt:'2026-10-23T10:00:00Z'};

test('owner header, profile menu, module spacing and accessible enquiry modal',async({page},info)=>{
 await page.route('**/api/**',async route=>{
  const path=new URL(route.request().url()).pathname;
  const user={id,username:'owner-layout@example.test',role:'OWNER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true};
  let body:unknown;
  if(path==='/api/v1/auth/appearance')body={version:0,has_image:false,public_name:''};
  else if(path==='/api/v1/auth/session')body=user;
  else if(path==='/api/v1/staff/enquiries')body=[lead];
  else if(path.endsWith('/work'))body={liveWork:false,status:'REVIEWING',messages:[],mailConnected:false};
  else if(path.startsWith('/api/v1/design/enquiries/'))body={artifacts:[],proofs:[],comments:[],quoteAccepted:false,currentProofApproved:false,closed:false};
  else if(path.endsWith('/activities')||path.endsWith('/quotes'))body=[];
  else if(path==='/api/v1/owner/staff')body=[{...user,enabled:true}];
  else throw new Error('Unexpected layout fixture request: '+path);
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
 });
 await page.goto('/business');const header=page.locator('header');
 await expect(header.getByLabel('Open account menu')).toBeVisible();
 await expect(header.getByRole('link',{name:'Customer account',exact:true})).toHaveCount(0);
 await expect(header.getByRole('link',{name:'Staff / owner sign in'})).toHaveCount(0);
 await expect(header.getByRole('link',{name:'Request a quote'})).toHaveCount(0);
 const gap=await page.getByRole('navigation',{name:'Workspace modules'}).evaluate(e=>parseFloat(getComputedStyle(e).marginBottom));expect(gap).toBeGreaterThanOrEqual(24);
 await page.getByRole('button',{name:'Open enquiry',exact:true}).click();const dialog=page.getByRole('dialog',{name:'Enquiry details',exact:true});await expect(dialog).toBeVisible();expect(await dialog.evaluate(e=>getComputedStyle(e,'::backdrop').backdropFilter)).toContain('blur');expect(await page.locator('body').evaluate(e=>getComputedStyle(e).overflow)).toBe('hidden');await expect(dialog.getByRole('heading',{name:'Synthetic print customer'})).toBeVisible();
 await page.screenshot({path:info.outputPath('enquiry-modal.png'),fullPage:false});await page.keyboard.press('Escape');await expect(dialog).toHaveCount(0);await expect(page.getByRole('button',{name:'Open enquiry',exact:true})).toBeFocused();
 await header.getByLabel('Open account menu').click();await expect(header.getByRole('button',{name:'Sign out',exact:true})).toBeVisible();await header.getByRole('button',{name:'Profile & security',exact:true}).click();await expect(page.getByRole('dialog',{name:'Profile & security',exact:true})).toBeVisible();await page.keyboard.press('Escape');
 await page.screenshot({path:info.outputPath('owner-navigation.png'),fullPage:false});
});

test('customer login spacing, contextual resend and provider setup status',async({page},info)=>{
 await page.route('**/api/**',async route=>{
  const path=new URL(route.request().url()).pathname;
  if(path==='/api/v1/auth/session'){await route.fulfill({status:401,contentType:'application/json',body:'{}'});return;}
  const body=path==='/api/public/sso/providers'?[{id:'google',name:'Google',enabled:false,url:''},{id:'microsoft',name:'Microsoft',enabled:false,url:''}]:{deliveryEnabled:false};
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(body)});
 });
 await page.goto('/customer');const form=page.locator('form.customer-auth');await expect(form.getByRole('heading',{name:'Customer sign in'})).toBeVisible();await expect(page.locator('header').getByRole('link',{name:'Staff / owner sign in'})).toBeVisible();await expect(page.locator('header').getByRole('link',{name:'Request a quote'})).toHaveCount(0);
 const primary=form.getByRole('button',{name:'Sign in',exact:true});expect(await primary.evaluate(e=>getComputedStyle(e).whiteSpace)).toBe('nowrap');const sizes=await form.evaluate(e=>({width:e.clientWidth,pad:parseFloat(getComputedStyle(e).paddingLeft)*2}));expect((await primary.boundingBox())!.width).toBeGreaterThan(sizes.width-sizes.pad-5);
 await expect(form.getByRole('button',{name:'Resend verification',exact:true})).toBeHidden();await expect(form.getByRole('button',{name:/Continue with Google/})).toBeDisabled();await expect(form.getByRole('button',{name:/Continue with Microsoft/})).toBeDisabled();
 await page.screenshot({path:info.outputPath('customer-authentication.png'),fullPage:true});await form.locator('summary').click();await expect(form.getByRole('button',{name:'Resend verification',exact:true})).toBeVisible();
});

test('owner MFA actions have their own spacing',async({page},info)=>{
 await page.route('**/api/v1/auth/session',route=>route.fulfill({status:200,contentType:'application/json',body:JSON.stringify({id,username:'owner-layout@example.test',role:'OWNER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:true,mfaRecent:false})}));
 await page.goto('/business');const panel=page.locator('.verification-card');await expect(panel.getByRole('heading',{name:'Owner verification'})).toBeVisible();const verify=await panel.getByRole('button',{name:'Verify owner access'}).boundingBox(),logout=await panel.getByRole('button',{name:'Sign out',exact:true}).boundingBox();expect(logout!.y).toBeGreaterThan(verify!.y+verify!.height+16);await page.screenshot({path:info.outputPath('owner-verification.png'),fullPage:true});
});
