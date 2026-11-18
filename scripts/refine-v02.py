from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
p=root/'app/src/main/assets/copy.zh-CN.json'
c=json.loads(p.read_text(encoding='utf-8'))
c.update(date='日期',monthly_cost='本月成本',required='本月成本',invalid_dates='请检查起止日期，结束日期不能早于开始日期',reminder_enabled='到期提醒')
p.write_text(json.dumps(c,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
p=root/'app/src/main/java/com/livingbudget/app/ui/Panels.kt'
s=p.read_text(encoding='utf-8')
s='\n'.join(line for line in s.splitlines() if 'Text(c["payment_date"]' not in line and 'if(e.category.isNotBlank())' not in line)
s=s.replace('c["end_date"]+" ? "+d.end','c["end_date"]+" · "+d.end')
s=s.replace('    if(d.type==ExpenseType.SUBSCRIPTION) {','    run {')
line='                                    if(!e.reminderHandled)TextButton(onClick={onAction(Action.HandleReminder)},enabled=!s.busy) { Text(c["handled"]) }'
s=s.replace(line,'')
needle='OutlinedButton(onClick={onAction(Action.Renew)}'
lines=s.splitlines()
for i,line in enumerate(lines):
    if needle in line:
        lines.insert(i+2,'                                if(!e.reminderHandled)TextButton(onClick={onAction(Action.HandleReminder)},enabled=!s.busy) { Text(c["handled"]) }')
        break
p.write_text('\n'.join(lines)+'\n',encoding='utf-8')
p=root/'app/src/main/java/com/livingbudget/app/ui/Screens.kt'
s=p.read_text(encoding='utf-8')
s='\n'.join(line for line in s.splitlines() if 'Text(c[if(e.paymentStatus' not in line)
p.write_text(s+'\n',encoding='utf-8')
p=root/'core/src/main/kotlin/com/livingbudget/core/Ledger.kt'
s=p.read_text().replace('type == ExpenseType.SUBSCRIPTION && reminderEnabled && !reminderHandled &&','reminderEnabled && !reminderHandled &&')
p.write_text(s)
p=root/'core/src/test/kotlin/com/livingbudget/core/LedgerTest.kt'
s=p.read_text().replace('assertFalse(e.copy(type=ExpenseType.EXPENSE).shouldRemind','assertTrue(e.copy(type=ExpenseType.EXPENSE).shouldRemind')
p.write_text(s)
changes='''
## 0.2.0 已确认调整

本节优先于早期设计。预算只填金额并保存，不再选择范围；更新当前所选月份并作为后续默认值，之前月份不改写。

订阅与日常花费均保留名称、金额、期限、备注和提醒，删除付款日期、付款状态及分类。旧数据库字段和备份保持兼容，但不参与新界面统计；新保存记录的内部日期取开始日。单日只显示日期；月／季／年只输入开始日期，结束日自动推算；自定义填写起止日期。

今日成本仅累计今天有效记录的日额。结束日包含当天，次日零点归零。本月成本累计全部记录与该月的交集金额，已结束记录的本月成本不删除。首页将本月已付改为本月成本，预算余量按预算减本月成本计算，不再显示待付。

累计分摊金额四舍五入到分，再通过相邻累计值之差得到每日和每月金额，保证总额守恒。¥300、2026-03-20 至 2026-04-02 共 14 天，3 月 ¥257.14、4 月 ¥42.86；4 月 3 日起不贡献今日成本。
'''
for name in ['SPEC.md','README.md']:
    p=root/name;s=p.read_text(encoding='utf-8');p.write_text(s+changes,encoding='utf-8')
p=root/'design/flow/build_flow.py';s=p.read_text(encoding='utf-8')
for a,b in [('选择「本月及以后」或「仅本月」。','不再选择范围，直接修改数值。'),('首屏：名称、总金额、期限、起止日期、付款日期、付款状态。','名称、金额、期限。单日仅日期，月／季／年仅开始日期，自定义起止日期。'),('下方：分类、备注、单项提醒、提醒事项、扣款日期、保存。','下方：备注、提醒事项、可选扣款日期和保存。'),('字段与订阅共用，但没有提醒字段。','字段与订阅一致，保留备注及提醒。'),('名称、金额、起止日期、天数、日均、添加时间、付款信息。','名称、金额、起止日期、天数、日均、添加时间和备注。')]:s=s.replace(a,b)
p.write_text(s,encoding='utf-8')
