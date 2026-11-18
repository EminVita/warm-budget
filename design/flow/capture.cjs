const { chromium } = require(process.env.PLAYWRIGHT_MODULE || 'playwright');
const {pathToFileURL}=require('url');const path=require('path');
(async()=>{const browser=await chromium.launch({headless:true,channel:'msedge'});const page=await browser.newPage({viewport:{width:1840,height:1800},deviceScaleFactor:1});
await page.goto(pathToFileURL(path.resolve('design/flow/index.html')).href);await page.evaluate(()=>Promise.all([...document.images].map(i=>i.decode())));
for(let i=0;i<4;i++){await page.locator('#page'+i).screenshot({path:'design/flow/flow-'+(i+1)+'.png'});}
console.log('Rendered four flow sheets; screenshots decoded.');await browser.close()})().catch(e=>{console.error(e);process.exit(1)});
