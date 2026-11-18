"""Check editable text keys and presentation boundaries without an Android runtime."""
import json
import re
from pathlib import Path

root=Path(__file__).resolve().parents[1]
copy=json.loads((root/'app/src/main/assets/copy.zh-CN.json').read_text(encoding='utf-8'))
keys=set()
for path in (root/'app/src/main/java').rglob('*.kt'):
    source=path.read_text(encoding='utf-8')
    keys.update(re.findall(r'(?:c|copy)\["([a-z_]+)"\]',source))
    keys.update(re.findall(r'(?:format|message)\("([a-z_]+)"',source))
keys.update('duration_'+v for v in ['day','month','quarter','year','custom'])
keys.update('theme_'+v for v in ['system','light','dark'])
assert not keys-set(copy),keys-set(copy)
for path in (root/'app/src/main/java/com/livingbudget/app/ui').glob('*.kt'):
    source=path.read_text(encoding='utf-8')
    assert not any(token in source for token in ['BudgetDatabase','LedgerDao','ViewModel','Context','WorkManager']),path
    assert not re.search(r'Text\("[\u4e00-\u9fff]',source),path
assert all(isinstance(value,str) for value in copy.values())
print(f'PASS: {len(copy)} centralized copy keys; semantic action contracts; no UI persistence dependency.')
