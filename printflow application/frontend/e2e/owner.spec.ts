import {test,expect} from '@playwright/test';

test('native owner password login stops at MFA without revealing business data',async({page},info)=>{
 test.skip(!process.env.APP_OWNER_BOOTSTRAP_PASSWORD,'Requires privately loaded native owner credentials');
 await page.goto('/business');
 await page.getByLabel('Staff username').fill(process.env.APP_OWNER_BOOTSTRAP_USERNAME||'');
 await page.getByLabel('Password',{exact:true}).fill(process.env.APP_OWNER_BOOTSTRAP_PASSWORD||'');
 await page.getByRole('button',{name:'Sign in',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Owner verification'})).toBeVisible();
 await expect(page.getByRole('heading',{name:'Enquiry queue'})).toHaveCount(0);
 await expect(page.getByRole('heading',{name:'Staff management'})).toHaveCount(0);
 expect((await page.request.get('/api/v1/owner/staff')).status()).toBe(403);
 // Do not enroll the real local owner or expose its authenticator seed in automated tests.
 await page.screenshot({path:info.outputPath('owner-mfa-gate.png'),fullPage:true});
 await page.reload();
 await expect(page.getByRole('heading',{name:'Owner verification'})).toBeVisible();
 await page.getByRole('button',{name:'Sign out',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Staff sign in'})).toBeVisible();
});


test('owner staff-management UI with synthetic API fixtures',async({page},info)=>{
 const ownerId='00000000-0000-4000-8000-000000000001';
 const staff=[{id:ownerId,username:'owner-ui@example.test',role:'OWNER',enabled:true,mustChangePassword:false,mfaEnrolled:true}];
 const user={id:ownerId,username:staff[0].username,role:'OWNER',mustChangePassword:false,mfaEnrolled:true,mfaRequired:false,mfaRecent:true};
 let resets=0;
 await page.route('**/api/v1/**',async route=>{
  const request=route.request(),path=new URL(request.url()).pathname,method=request.method();
  let result:unknown;
  if(path==='/api/v1/auth/session')result=user;
  else if(path==='/api/v1/auth/csrf')result={token:'synthetic-csrf-fixture',headerName:'X-CSRF-TOKEN'};
  else if(path==='/api/v1/staff/enquiries'||path==='/api/v1/owner/activity')result=[];
  else if(path==='/api/v1/owner/staff'&&method==='GET')result=staff;
  else if(path==='/api/v1/owner/staff'&&method==='POST'){
   expect(request.headers()['x-csrf-token']).toBe('synthetic-csrf-fixture');
   const input=request.postDataJSON();expect(input.role).toBe('SALES');
   staff.push({id:'00000000-0000-4000-8000-000000000002',username:input.username,role:input.role,enabled:true,mustChangePassword:true,mfaEnrolled:false});result={id:staff[1].id};
  }else if(path.endsWith('/access')&&method==='PATCH'){
   const input=request.postDataJSON();staff[1]={...staff[1],...input};result={code:'ACCESS_CHANGED'};
  }else if(path.endsWith('/password')&&method==='POST'){resets++;result={code:'PASSWORD_RESET'};}
  else throw new Error(`Unexpected fixture request: ${method} ${path}`);
  await route.fulfill({status:200,contentType:'application/json',body:JSON.stringify(result)});
 });
 await page.goto('/business');
 await page.getByRole('button',{name:'STAFF',exact:true}).click();
 await expect(page.getByRole('heading',{name:'Staff management',exact:true})).toBeVisible();
 await page.getByLabel('New staff username').fill('sales-ui@example.test');
 await page.getByLabel('Temporary password').fill('Synthetic-fixture-password-123');
 await page.getByRole('button',{name:'Create account',exact:true}).click();
 const row=page.getByRole('row').filter({hasText:'sales-ui@example.test'});
 await expect(row).toContainText('Password change required');
 await expect(page.getByLabel('Temporary password')).toHaveValue('');
 await row.getByRole('button',{name:'Disable',exact:true}).click();await expect(row).toContainText('Disabled');
 await row.getByRole('button',{name:'Enable',exact:true}).click();await expect(row).toContainText('Enabled');
 await page.getByLabel('Role for sales-ui@example.test',{exact:true}).selectOption('OPERATOR');await expect(row.getByRole('combobox')).toHaveValue('OPERATOR');
 await row.getByRole('button',{name:'Reset password for sales-ui@example.test',exact:true}).click();
 await page.getByLabel('Temporary password').fill('Another-synthetic-password-123');
 await page.getByRole('button',{name:'Reset password',exact:true}).click();await expect(page.getByRole('button',{name:'Create account',exact:true})).toBeVisible();expect(resets).toBe(1);
 await page.screenshot({path:info.outputPath('owner-staff-management-fixture.png'),fullPage:true});
});
