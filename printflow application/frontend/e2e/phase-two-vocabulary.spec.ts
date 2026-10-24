import {test,expect} from '@playwright/test';import {readFileSync} from 'node:fs';import path from 'node:path';
test('owner print vocabulary preserves existing choices and rejects stale edits',async({page},info)=>{
 test.skip(process.env.PHASE1_BROWSER_FIXTURES!=='true','Use scripts/Test-PhaseTwoBrowser.ps1 -Check vocabulary');
 const fixture=JSON.parse(readFileSync(path.resolve('..','.local','browser-fixtures.json'),'utf8'));
 await page.goto('/business');await page.getByLabel('Staff username').fill(fixture.username);await page.getByLabel('Password',{exact:true}).fill(fixture.password);await page.getByRole('button',{name:'Sign in',exact:true}).click();await page.getByLabel('Authenticator or recovery code').fill(fixture.recoveryCodes[info.project.name]);await page.getByRole('button',{name:'Verify owner access',exact:true}).click();
 await page.getByRole('button',{name:'PRODUCTS',exact:true}).click();const editor=page.getByRole('region',{name:'Print vocabulary',exact:true});
 const before=(await (await page.request.get('/api/v1/owner/print-vocabulary')).json()).find((r:{category:string})=>r.category==='paperType');
 await editor.getByRole('combobox',{name:'Print option category',exact:true}).selectOption('paperType');await expect(editor.getByLabel('Allowed option codes',{exact:true})).toHaveValue(JSON.parse(before.choices).join(', '));
 // Save the same choices: only the revision/audit changes, never business option values.
 await editor.getByRole('button',{name:'Save print vocabulary',exact:true}).click();await expect(editor.getByRole('status')).toContainText('Print vocabulary updated');
 const csrf=await (await page.request.get('/api/v1/auth/csrf')).json(),headers={[csrf.headerName]:csrf.token};
 const stale=await page.request.put('/api/v1/owner/print-vocabulary/paperType',{headers,data:{expectedRevision:before.revision,choices:JSON.parse(before.choices)}});expect(stale.status()).toBe(409);
 const invalid=await page.request.put('/api/v1/owner/print-vocabulary/paperType',{headers,data:{expectedRevision:before.revision+1,choices:['UNSURE',null]}});expect(invalid.status()).toBe(400);
 const after=(await (await page.request.get('/api/v1/owner/print-vocabulary')).json()).find((r:{category:string})=>r.category==='paperType');expect(JSON.parse(after.choices)).toEqual(JSON.parse(before.choices));expect(after.revision).toBe(before.revision+1);
 await page.screenshot({path:info.outputPath('phase-two-vocabulary.png'),fullPage:true});
});
