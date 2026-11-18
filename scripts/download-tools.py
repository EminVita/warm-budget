"""Fetch official build artifacts with resumable, independently validated ranges."""
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path
import subprocess, re, hashlib
root=Path(__file__).resolve().parents[1]/'.tools'
def download(url, target):
    target=root/target
    if target.exists(): return
    head=subprocess.check_output(['curl.exe','-fsSIL','--max-time','40',url],text=True)
    size=int(re.findall(r'(?i)content-length:\s*(\d+)',head)[-1])
    partial=target.with_suffix(target.suffix+'.part')
    start=partial.stat().st_size if partial.exists() else 0
    chunks=[]
    for i,offset in enumerate(range(start,size,4*1024*1024)):
        end=min(offset+4*1024*1024,size)-1
        chunks.append((offset,end,root/f'{target.name}.chunk{i}'))
    def fetch(chunk):
        first,last,path=chunk
        if path.exists() and path.stat().st_size==last-first+1:return
        subprocess.run(['curl.exe','-fsSL','--retry','2','--max-time','300','-r',f'{first}-{last}',url,'-o',str(path)],check=True)
        assert path.stat().st_size==last-first+1
    with ThreadPoolExecutor(max_workers=12) as pool:list(pool.map(fetch,chunks))
    with partial.open('ab') as out:
        for _,_,path in chunks:out.write(path.read_bytes())
    assert partial.stat().st_size==size
    partial.rename(target)
    for _,_,path in chunks:path.unlink()
    print(f'Fetched {target.name}: {size} bytes',flush=True)
download('https://services.gradle.org/distributions/gradle-8.13-bin.zip','gradle.zip')
expected=subprocess.check_output(['curl.exe','-fsSL','--max-time','40','https://services.gradle.org/distributions/gradle-8.13-bin.zip.sha256'],text=True).strip()
assert hashlib.sha256((root/'gradle.zip').read_bytes()).hexdigest()==expected
download('https://dl.google.com/android/repository/commandlinetools-win-13114758_latest.zip','sdk-tools.zip')
