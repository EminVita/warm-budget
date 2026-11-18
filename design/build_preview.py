from pathlib import Path
from html import escape
import xml.etree.ElementTree as ET

BASE = Path(__file__).resolve().parent
screens = [
    ('首页', [
        (20, 50, 350, 64, '暖暖生活费\n把日子过得心里有数', '#FAF6F0', 22),
        (20, 128, 350, 232, '2026 年 11 月                   ‹   ›\n本月生活费\n¥ 3,000.00\n预计需准备 ¥ 2,180.00\n预算余量 ¥ 820.00\n按已记录费用估算', '#F2DFC9', 20),
        (20, 374, 169, 108, '今日分摊成本\n¥ 68.50\n含订阅和日常费用', '#FFFCF7', 17),
        (201, 374, 169, 108, '本月实际已付\n¥ 1,860.00\n剩余待付 ¥ 320.00', '#FFFCF7', 17),
        (20, 498, 169, 52, '＋ 添加订阅', '#94583D', 17),
        (201, 498, 169, 52, '＋ 记一笔花费', '#EBDCCB', 17),
        (20, 570, 350, 40, '我的费用       生效中 · 待开始', '#FAF6F0', 18),
        (20, 620, 350, 96, '♫  音乐会员                  ¥ 30.00\n11.01 – 11.30 · 30 天\n日均 ¥ 1.00                   剩余 12 天', '#FFFCF7', 16),
        (20, 728, 350, 96, '⌂  房租                      ¥ 1,500.00\n11.01 – 11.30 · 30 天\n日均 ¥ 50.00                  剩余 12 天', '#FFFCF7', 16),
        (20, 840, 350, 54, '⌂ 首页                         ⚙ 设置', '#EBDCCB', 18),
    ]),
    ('设置', [
        (20, 50, 350, 64, '设置\n让记录更合你的习惯', '#FAF6F0', 22),
        (20, 128, 350, 100, '每月生活费                     ›\n¥ 3,000.00\n默认沿用，可单独调整某月', '#F2DFC9', 18),
        (20, 252, 350, 120, '记账偏好\n记账时区            上海 UTC+8\n默认币种                       人民币', '#FFFCF7', 17),
        (20, 392, 350, 98, '◷  历史记录                     ›\n到期的订阅和花费都在这里\n按添加时间从新到旧排列', '#FFFCF7', 17),
        (20, 510, 350, 100, '备份与恢复\n导出备份                         ›\n从文件恢复                       ›', '#FFFCF7', 17),
        (20, 630, 350, 86, '外观与提醒\n主题跟随系统 · 到期提醒', '#FFFCF7', 17),
        (20, 736, 350, 88, '关于暖暖生活费\n当前版本 1.0.0       检查更新 ›', '#FFFCF7', 17),
        (20, 840, 350, 54, '⌂ 首页                         ⚙ 设置', '#EBDCCB', 18),
    ])
]
root = ET.Element('mxfile', host='app.diagrams.net')
for name, blocks in screens:
    diagram = ET.SubElement(root, 'diagram', name=name, id=name)
    model = ET.SubElement(diagram, 'mxGraphModel', pageWidth='390', pageHeight='920', background='#FAF6F0')
    layer = ET.SubElement(model, 'root')
    ET.SubElement(layer, 'mxCell', id='0')
    ET.SubElement(layer, 'mxCell', id='1', parent='0')
    for i, (x,y,w,h,label,color,size) in enumerate(blocks):
        style = f'rounded=1;arcSize=20;whiteSpace=wrap;html=0;fillColor={color};strokeColor=none;fontColor={"#FFFFFF" if color == "#94583D" else "#3D3B32"};fontSize={size};fontFamily=Microsoft YaHei;align=left;spacingLeft=18;'
        cell = ET.SubElement(layer, 'mxCell', id=str(i+2), value=label, style=style, vertex='1', parent='1')
        ET.SubElement(cell, 'mxGeometry', x=str(x), y=str(y), width=str(w), height=str(h), **{'as':'geometry'})
ET.indent(root)
ET.ElementTree(root).write(BASE / 'warm-budget.drawio', encoding='utf-8', xml_declaration=True)

html = (BASE / 'preview-template.html').read_text(encoding='utf-8')
(BASE / 'preview.html').write_text(html, encoding='utf-8')
print('Generated preview and editable layout.')
