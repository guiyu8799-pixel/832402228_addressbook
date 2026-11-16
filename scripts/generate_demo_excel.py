# -*- coding: utf-8 -*-
"""Generates the demo workbook used for the import demonstration and screenshots.

Run with the bundled Python:
    python scripts/generate_demo_excel.py
"""

import os
from openpyxl import Workbook
from openpyxl.styles import Alignment, Border, Font, PatternFill, Side
from openpyxl.utils import get_column_letter

HEADERS = ["姓名", "分组", "公司", "职位", "收藏", "电话", "邮箱", "社交", "地址", "备注"]

ROWS = [
    ["林晓", "同学", "福州大学", "计算机学院 本科生", "是",
     "手机: 13800138000; 宿舍: 0591-22861234",
     "邮箱: linxiao@example.com", "微信: linxiao_fzu",
     "地址: 福州市闽侯县学园路 2 号", "同班同学，算法课队友"],
    ["陈默", "同事", "示例科技", "后端工程师", "是",
     "手机: 13900139000", "工作邮箱: chenmo@example.com", "企业微信: chenmo_work",
     "地址: 福州市鼓楼区软件园 A 区", "项目搭档"],
    ["王倩", "家人", "社区医院", "护士", "是",
     "手机: 13700137000", "邮箱: wangqian@example.com", "微信: wangqian_home",
     "地址: 福州市仓山区金山街道", "表姐"],
    ["赵磊", "同学", "福州大学", "数学学院 研究生", "否",
     "手机: 13600136000", "邮箱: zhaolei@example.com", "QQ: 123456789",
     "地址: 福州市闽侯县学园路 2 号", "室友"],
    ["孙悦", "同事", "示例科技", "产品经理", "否",
     "手机: 13500135000", "工作邮箱: sunyue@example.com", "企业微信: sunyue_pm",
     "地址: 福州市鼓楼区软件园 B 区", "需求对接人"],
    ["周航", "同学", "福州大学", "计算机学院 本科生", "否",
     "手机: 13400134000", "邮箱: zhouhang@example.com", "微信: zhouhang_fzu",
     "地址: 福州市闽侯县学园路 2 号", "竞赛队友"],
    ["吴敏", "家人", "社区卫生服务中心", "全科医生", "否",
     "手机: 13300133000", "邮箱: wumin@example.com", "微信: wumin_home",
     "地址: 福州市台江区江滨路", "姑姑"],
    ["郑凯", "客户", "蓝海贸易", "采购经理", "否",
     "手机: 13200132000", "工作邮箱: zhengkai@bluesea.com", "企业微信: zhengkai_biz",
     "地址: 厦门市思明区湖滨南路", "合作客户"],
    ["何静", "同事", "示例科技", "前端工程师", "否",
     "手机: 13100131000", "工作邮箱: hejing@example.com", "企业微信: hejing_fe",
     "地址: 福州市鼓楼区软件园 A 区", "前端联调"],
    ["罗宾", "其他", "自由职业", "摄影师", "否",
     "手机: 13000130000", "邮箱: robin@example.com", "微信: robin_photo",
     "地址: 福州市晋安区五四北", "摄影社认识"],
]

HEADER_FILL = PatternFill("solid", fgColor="1F4E9C")
HEADER_FONT = Font(color="FFFFFF", bold=True, size=11)
THIN = Side(style="thin", color="D6DEEA")
BORDER = Border(left=THIN, right=THIN, top=THIN, bottom=THIN)


def build(path):
    workbook = Workbook()
    sheet = workbook.active
    sheet.title = "通讯录"

    for index, header in enumerate(HEADERS, start=1):
        cell = sheet.cell(row=1, column=index, value=header)
        cell.fill = HEADER_FILL
        cell.font = HEADER_FONT
        cell.alignment = Alignment(horizontal="center", vertical="center")
        cell.border = BORDER

    for row_index, row in enumerate(ROWS, start=2):
        for column_index, value in enumerate(row, start=1):
            cell = sheet.cell(row=row_index, column=column_index, value=value)
            cell.alignment = Alignment(vertical="center", wrap_text=True)
            cell.border = BORDER

    widths = [12, 10, 18, 18, 8, 38, 30, 26, 34, 26]
    for index, width in enumerate(widths, start=1):
        sheet.column_dimensions[get_column_letter(index)].width = width
    sheet.freeze_panes = "A2"
    sheet.row_dimensions[1].height = 24

    help_sheet = workbook.create_sheet("填写说明")
    notes = [
        "每行一个联系人，列顺序与表头一致，表头名称不要修改。",
        "姓名必填，其余列可以留空。",
        "同一类联系方式有多条时写在同一格里，用分号分隔，格式为「标签: 内容」。",
        "例如：手机: 13800138000; 宿舍: 0591-22861234",
        "收藏列填「是」或「否」。",
    ]
    for index, note in enumerate(notes, start=1):
        help_sheet.cell(row=index, column=1, value=note)
    help_sheet.column_dimensions["A"].width = 72

    os.makedirs(os.path.dirname(path), exist_ok=True)
    workbook.save(path)
    print("written:", path)


if __name__ == "__main__":
    root = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
    build(os.path.join(root, "docs", "demo-contacts.xlsx"))
