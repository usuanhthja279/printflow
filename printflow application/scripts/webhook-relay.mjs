// Local-only relay for a temporary test tunnel. Exposes only the signed webhook path.
import http from 'node:http';
const path='/api/public/payments/razorpay/webhook';
http.createServer(async(req,res)=>{
 if(req.method!=='POST'||req.url!==path){res.writeHead(404);res.end();return;}
 let size=0;const chunks=[];
 try{for await(const chunk of req){size+=chunk.length;if(size>65536){res.writeHead(413);res.end();return;}chunks.push(chunk);}
  const response=await fetch('http://127.0.0.1:8080'+path,{method:'POST',headers:{'Content-Type':'application/json','X-Razorpay-Signature':String(req.headers['x-razorpay-signature']||'')},body:Buffer.concat(chunks),signal:AbortSignal.timeout(4500)});
  res.writeHead(response.status,{'Content-Type':'application/json','Cache-Control':'no-store'});res.end(await response.text());
  console.log(JSON.stringify({at:new Date().toISOString(),status:response.status}));
 }catch{if(!res.headersSent)res.writeHead(502);res.end();}
}).listen(8091,'127.0.0.1',()=>console.log('Webhook-only relay ready on loopback port 8091'));
