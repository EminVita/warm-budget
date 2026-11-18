from pathlib import Path
import base64, json, html
import xml.etree.ElementTree as ET

BASE=Path(__file__).resolve().parent
PAGES=[
('记账与预算',[
 ('A1','首页','home','月份左右切换；点击月份回本月。\n今日成本与当前列表始终按今天判断。\n点击预算卡、添加按钮或费用卡进入对应内容。'),
 ('A2','预算设置','budget','输入金额。\n不再选择范围，直接修改数值。\n保存后关闭表单，返回首页。'),
 ('A3','添加订阅','subscription','名称、金额、期限。单日仅日期，月／季／年仅开始日期，自定义起止日期。\n下方：备注、提醒事项、可选扣款日期和保存。\n日期当前手动输入。'),
 ('A4','添加花费','expense','默认单日，也支持月、季、年、自定义。\n字段与订阅一致，保留备注及提醒。\n点击截图可查看完整表单。'),
 ('A5','费用详情','detail','名称、金额、起止日期、天数、日均、添加时间和备注。\n可更正；订阅可续订、停止本轮提醒。\n保存新增后先返回首页，再点击费用进入这里。'),
 ('A6','保存后返回首页','home','保存更新金额和列表。\n返回直接关闭表单，当前没有放弃修改确认。\n无记录显示「暂无记录」。')],
 [('A1','A2','点击预算卡'),('A1','A3','点击添加订阅'),('A1','A4','点击添加花费'),('A3','A5','保存 → 首页 → 点击费用'),('A4','A5','保存 → 首页 → 点击费用'),('A2','A6','保存'),('A5','A6','返回关闭详情')]),
('历史、更正与续订',[
 ('B1','设置','settings','从底部导航进入设置。\n历史入口在「通用」分组。'),
 ('B2','历史记录','history','仅显示已结束记录。\n按添加时间从新到旧。\n搜索名称或备注；可按类型筛选。'),
 ('B3','历史费用详情','detail','详情结构与当前费用相同，图中以示例记录展示。\n结束次日自动归入历史，数据不删除。\n注意：当前返回会关闭弹层，回到设置页。'),
 ('B4','更正记录','edit','原值预填，修改后保存。\n保留修改前快照，重算统计。\n尚无查看快照的页面，也无删除按钮。'),
 ('B5','续订','renew','创建新记录，不覆盖旧记录。\n默认新建一个月，不沿用原季度／年度期限。\n开始日取旧期限结束次日与今天中较晚的一天。'),
 ('B6','保存后关闭弹层','settings','从历史进入并保存后，回到设置页。\n旧记录继续保留；续订新项到首页查看。\n旧订阅的本轮提醒随续订停止。')],
 [('B1','B2','点击历史记录'),('B2','B3','点击费用'),('B3','B4','点击更正记录'),('B3','B5','点击续订'),('B4','B6','保存更正'),('B5','B6','保存新记录')]),
('到期提醒',[
 ('C1','开启全局提醒','settings','设置 → 到期提醒。\n初始关闭；需要系统允许通知。\n没有权限时显示「通知未启用」。'),
 ('C2','每日提醒时间','time','默认上海时间 09:00。\n手动输入 HH:mm 后保存。\n系统省电可能延迟任务。'),
 ('C3','单项订阅提醒','subscription','表单下半部分设置：提醒此项订阅、取消自动续费／准备缴费。\n可指定实际扣款日期，留空用结束日期。\n点击截图查看完整字段。'),
 ('C4','系统通知','', '这是系统界面流程说明，未取得通知栏截图。\n基准日前第 3、2、1 天，每天一次。\n内容为订阅名称＋剩余天数＋取消续费或备款提示。\n全局、单项开关和权限均满足后才通知。'),
 ('C5','点击通知进入详情','detail','点击通知打开对应订阅。\n可点击「本轮不再提醒」。\n操作不会替用户取消服务商的续费。'),
 ('C6','本轮停止提醒','', '处理完成提示：\n「已停止本轮提醒，订阅期限保持不变」。\n本轮后续提醒停止，费用及到期时间不变。\n续订会创建下一条记录及其提醒。')],
 [('C1','C2','设置每日时间'),('C1','C3','录入订阅时设置单项提醒'),('C3','C4','进入提醒日期且条件满足'),('C4','C5','点击系统通知'),('C5','C6','点击本轮不再提醒')]),
('设置、备份与更新',[
 ('D1','设置入口','settings','「数据管理」：导出备份、恢复数据。\n「页面文案」：导出、导入、恢复默认。\n「关于」：检查更新。\n点击截图查看设置页完整内容。'),
 ('D2','外观','theme','跟随系统／浅色／深色。\n选择后立即应用，关闭弹层。'),
 ('D3','恢复数据确认','restore','先通过系统选择文件。\n校验后展示费用数量和覆盖提醒。\n确认后整体替换费用、预算及更正记录。\n文案与偏好不包含在备份中。'),
 ('D4','文件与文案操作','', '系统文件选择器：未截取系统界面。\n导出备份／文案 → 选择保存位置。\n导入文案 → 选择 JSON → 校验并立即应用。\n恢复默认文案 → 立即执行，目前没有二次确认。'),
 ('D5','检查更新：当前状态','update','当前未配置真实更新地址。\n点击后提示「尚未配置更新地址」。\n截图为实际已实现的提示界面。'),
 ('D6','配置发布地址后的更新','', '此节点说明已实现的条件分支，非当前系统截图。\n有新版本 → 显示版本及更新说明 → 前往下载。\n使用系统浏览器下载，安卓确认安装。\n无新版本 → 提示「当前已是最新版本」。')],
 [('D1','D2','点击外观'),('D1','D3','恢复数据 → 系统选择文件'),('D1','D4','导出／导入文案或备份'),('D1','D5','点击检查更新'),('D5','D6','配置更新地址后才有此分支')])
]
W,H=1790,1660
POS=[(55,160),(655,160),(1255,160),(55,905),(655,905),(1255,905)]
CW,CH=475,640
mx=ET.Element('mxfile',host='app.diagrams.net')
sections=[]
for pi,(title,nodes,edges) in enumerate(PAGES):
    dia=ET.SubElement(mx,'diagram',id=f'p{pi}',name=title)
    model=ET.SubElement(dia,'mxGraphModel',pageWidth=str(W),pageHeight=str(H),background='#F6F1E8')
    root=ET.SubElement(model,'root');ET.SubElement(root,'mxCell',id='0');ET.SubElement(root,'mxCell',id='1',parent='0')
    def cell(id,value,x,y,w,h,style):
        el=ET.SubElement(root,'mxCell',id=id,value=value,vertex='1',parent='1',style=style)
        ET.SubElement(el,'mxGeometry',x=str(x),y=str(y),width=str(w),height=str(h),attrib={'as':'geometry'})
    cell('heading',title,55,25,1500,50,'text;html=0;align=left;fontSize=30;fontColor=#42372D;')
    cell('caption','当前应用代码渲染截图 · 示例数据 · 非真机截图；点击 HTML 中的截图可放大，长表单有完整截图。',55,85,1650,35,'text;align=left;fontSize=16;fontColor=#806F5C;')
    loc={node[0]:POS[i] for i,node in enumerate(nodes)}
    cards=[]
    for i,(id,label,img,note) in enumerate(nodes):
        x,y=POS[i]
        cell(id,'',x,y,CW,CH,'rounded=1;arcSize=5;fillColor=#FFFCF7;strokeColor=#DDCFBC;')
        cell(id+'title',id+'  '+label,x+18,y+15,CW-36,40,'text;align=left;fontSize=21;fontStyle=1;fontColor=#493A2D;')
        image_markup=''
        if img:
            imagepath=BASE/'screens'/f'{img}.png'
            data=base64.b64encode(imagepath.read_bytes()).decode()
            cell(id+'image','',x+18,y+72,210,455,f'shape=image;imageAspect=1;aspect=fixed;image=data:image/png,{data};')
            cell(id+'note',note,x+242,y+75,210,480,'text;whiteSpace=wrap;align=left;verticalAlign=top;fontSize=17;fontColor=#705D49;')
            long=BASE/'screens'/f'{img}-long.png'
            link=f'screens/{img}-long.png' if long.exists() else f'screens/{img}.png'
            image_markup=f'<a class="shot" href="{link}" target="_blank"><img src="screens/{img}.png" alt="{html.escape(label)}"><small>点击查看'+('完整长截图' if long.exists() else '原图')+'</small></a>'
        else:
            cell(id+'note',note,x+25,y+85,CW-50,460,'text;whiteSpace=wrap;align=left;verticalAlign=top;fontSize=23;fontColor=#705D49;')
        cards.append(f'<article style="left:{x}px;top:{y}px" id="{id}"><h2>{id}　{label}</h2><div class="inside {"plain" if not img else ""}">{image_markup}<p>{html.escape(note)}</p></div><div class="review">审阅：□ 保留　□ 删除　□ 修改</div></article>')
    arrows=[]
    for ei,(source,target,label) in enumerate(edges):
        sx,sy=loc[source];tx,ty=loc[target]
        if sy==ty and abs(tx-sx)<700:
            start=(sx+CW,sy+CH/2);end=(tx,ty+CH/2)
            pts=[start,end];lx=(start[0]+end[0])/2;ly=start[1]-12
        elif sy==ty:
            lane=sy-35
            pts=[(sx+CW/2,sy),(sx+CW/2,lane),(tx+CW/2,lane),(tx+CW/2,ty)]
            lx=(sx+tx+CW)/2;ly=lane-10
        else:
            lane=sy+CH+40+(ei%3)*20
            pts=[(sx+CW/2,sy+CH),(sx+CW/2,lane),(tx+CW/2,lane),(tx+CW/2,ty)]
            lx=(sx+tx+CW)/2;ly=lane-9
        el=ET.SubElement(root,'mxCell',id=f'edge{ei}',value=label,edge='1',parent='1',source=source,target=target,style='edgeStyle=orthogonalEdgeStyle;rounded=1;endArrow=block;strokeColor=#A56B45;strokeWidth=2;fontSize=15;labelBackgroundColor=#F6F1E8;')
        geo=ET.SubElement(el,'mxGeometry',relative='1',attrib={'as':'geometry'})
        arr=ET.SubElement(geo,'Array',attrib={'as':'points'})
        for px,py in pts[1:-1]:ET.SubElement(arr,'mxPoint',x=str(px),y=str(py))
        pointstr=' '.join(f'{a},{b}' for a,b in pts)
        if sy==ty and abs(tx-sx)<700 and len(label)>8:
            parts=[label[j:j+8] for j in range(0,len(label),8)]
            ly-=17*(len(parts)-1)
            markup=''.join(f'<tspan x="{lx}" dy="{0 if j==0 else 17}">{html.escape(part)}</tspan>' for j,part in enumerate(parts))
        else: markup=html.escape(label)
        arrows.append(f'<polyline points="{pointstr}"/><text x="{lx}" y="{ly}">{markup}</text>')
    sections.append(f'<section class="board" id="page{pi}"><h1>{pi+1:02d} / {title}</h1><div class="caption">当前应用代码渲染截图 · 示例数据 · 非真机截图；系统界面仅用文字说明。</div><svg viewBox="0 0 {W} {H}"><defs><marker id="arrow{pi}" markerWidth="9" markerHeight="9" refX="8" refY="4" orient="auto"><path d="M0 0L8 4L0 8Z" fill="#a56b45"/></marker></defs><g style="marker-end:url(#arrow{pi})">'+''.join(arrows)+'</g></svg>'+''.join(cards)+'</section>')
ET.indent(mx);ET.ElementTree(mx).write(BASE/'interaction-flow.drawio',encoding='utf-8',xml_declaration=True)
nav=''.join(f'<a href="#page{i}">{i+1}. {p[0]}</a>' for i,p in enumerate(PAGES))
doc='''<!doctype html><html lang="zh-CN"><meta charset="utf-8"><title>生活费 · 交互流程审阅</title><style>
*{box-sizing:border-box}body{margin:0;background:#e7e0d5;color:#493a2d;font-family:'Microsoft YaHei',sans-serif}nav{position:sticky;top:0;z-index:5;background:#fffcf7f5;padding:16px 24px;display:flex;gap:20px;box-shadow:0 2px 10px #65472b15}nav a{color:#8d5738;text-decoration:none;font-size:14px}.intro{padding:20px 55px;font-size:14px;line-height:1.9}.board{position:relative;width:1790px;height:1660px;background:#f6f1e8;margin:0 20px 32px;border-radius:20px;scroll-margin-top:60px}.board>h1{position:absolute;left:55px;top:25px;margin:0;font-size:30px}.caption{position:absolute;left:55px;top:84px;color:#806f5c;font-size:15px}article{position:absolute;width:475px;height:640px;background:#fffcf7;border:1px solid #ddcfbc;border-radius:20px;box-shadow:0 5px 20px #8162430a;padding:18px}h2{margin:0 0 25px;font-size:21px}.inside{display:flex;gap:16px}.shot{width:210px;flex-shrink:0;color:#94583d;text-decoration:none}.shot img{width:210px;height:455px;object-fit:contain;border:1px solid #e7ded2;border-radius:12px}.shot small{font-size:11px;display:block;text-align:center;margin-top:8px}.inside p{margin:0;font-size:16px;line-height:1.9;white-space:pre-line;color:#705d49}.plain p{font-size:21px;padding:12px;line-height:2}.review{position:absolute;bottom:18px;font-size:13px;color:#9d8165;border-top:1px solid #eee5d8;padding-top:14px;width:437px}svg{position:absolute;inset:0;width:100%;height:100%;overflow:visible}polyline{fill:none;stroke:#a56b45;stroke-width:2}svg text{font:14px 'Microsoft YaHei';fill:#94583d;text-anchor:middle;paint-order:stroke;stroke:#f6f1e8;stroke-width:6px;stroke-linejoin:round;marker-end:none} @media print{nav,.intro{display:none}.board{break-after:page;transform-origin:top left;margin:0}}</style>
<nav>'''+nav+'''</nav><div class="intro">按节点编号反馈即可，例如「A3 删除分类」「B5 续订沿用原期限」。截图来自现有 Compose 页面代码的 Robolectric 渲染，未连接手机；示例值只用于审阅。长表单点击图片打开完整截图。<br>每个流程单独成页；请横向滚动查看右侧节点。箭头标注当前点击或保存行为，不代表新增功能。</div>'''+''.join(sections)+'</html>'
(BASE/'index.html').write_text(doc,encoding='utf-8')
print('Generated four-page HTML flow and embedded-image editable drawio.')
