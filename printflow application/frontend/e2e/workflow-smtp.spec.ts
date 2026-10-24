import {test,expect} from '@playwright/test';
import {readFileSync} from 'node:fs';
import path from 'node:path';
// Explicit second half of the native workflow check, after routing only its synthetic outbox rows to the owner's test inbox.
test('owner UI reports SMTP acceptance for every synthetic lifecycle email',async({page,browser},info)=>{
 test.skip(process.env.PHASE1_BROWSER_FIXTURES!=='true'||process.env.WORKFLOW_SMTP_CHECK!=='true','Requires the explicit native UI workflow SMTP check');
 test.setTimeout(300000);
 const f=JSON.parse(readFileSync(path.resolve('..','.local','browser-fixtures.json'),'utf8'));
 const evidence=JSON.parse(readFileSync(path.resolve('..','.local','workflow-ui-evidence.json'),'utf8'));
 const recipient=process.env.WORKFLOW_TEST_RECIPIENT;
 expect(recipient).toBeTruthy();
 await page.goto('/business');await page.getByLabel('Staff username',{exact:true}).fill(f.username);await page.getByLabel('Password',{exact:true}).fill(f.password);await page.getByRole('button',{name:'Sign in',exact:true}).click();await page.getByLabel('Authenticator or recovery code').fill(f.recoveryCodes.mobile);await page.getByRole('button',{name:'Verify owner access',exact:true}).click();await page.getByRole('button',{name:'EMAIL',exact:true}).click();
 const activity=page.getByRole('region',{name:'Email activity',exact:true});await expect(activity).toContainText('Delivery is enabled');
 await expect.poll(async()=>{await activity.getByRole('button',{name:'Refresh email activity',exact:true}).click();const status=await(await page.request.get('/api/v1/owner/mail-status')).json();const rows=status.messages.filter((m:{recipient:string})=>m.recipient===recipient);return rows.length>=24&&rows.every((m:{state:string})=>m.state==='SENT');},{timeout:270000,intervals:[5000]}).toBe(true);
 const status=await(await page.request.get('/api/v1/owner/mail-status')).json(),rows=status.messages.filter((m:{recipient:string})=>m.recipient===recipient);
 for(const kind of ['ENQUIRY','NEW_ENQUIRY','ARTWORK','INTAKE','QUOTE','QUOTE_ACCEPT','REVIEW','COMMENT','PROOF_CHANGE','PROOF_OK','PRODUCTION','READY','TRANSIT','DELIVERED','COMPLETED'])expect(rows.some((m:{kind:string;state:string})=>m.kind===kind&&m.state==='SENT')).toBe(true);
 const visibleRows=activity.getByRole('row').filter({hasText:recipient!});expect((await visibleRows.locator('td:nth-child(3)').allTextContents()).every(state=>state==='SENT')).toBe(true);
 await activity.screenshot({path:info.outputPath('workflow-smtp-accepted.png')});
 const customerContext=await browser.newContext({baseURL:info.project.use.baseURL});try{const customer=await customerContext.newPage();await customer.goto('/customer');await customer.getByLabel('Email address',{exact:true}).fill(evidence.email);await customer.getByLabel('Password',{exact:true}).fill('Customer-chosen-local-password-123');await customer.getByRole('button',{name:'Sign in',exact:true}).click();await expect(customer.getByRole('heading',{name:'Live works',exact:true})).toBeVisible();await customer.goto('/customer/work/'+evidence.leadId);await expect(customer.getByRole('region',{name:'Production and delivery',exact:true}).getByText('Completed',{exact:true})).toBeVisible();await expect(customer.getByText('Delivered and completed after the 48-hour review window.',{exact:true})).toBeVisible();await customer.screenshot({path:info.outputPath('workflow-completed.png')});}finally{await customerContext.close();}
 expect(evidence.leadId).toMatch(/^[a-f0-9-]{36}$/);
});
