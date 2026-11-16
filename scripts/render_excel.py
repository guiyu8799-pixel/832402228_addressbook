# -*- coding: utf-8 -*-
"""Renders an exported .xlsx workbook as a spreadsheet-like HTML page.

The blog shows the content of the file that the export endpoint really produced; this script reads
that file with openpyxl and lays it out like a spreadsheet so it can be screenshotted.

Usage:
    python scripts/render_excel.py docs/downloads/通讯录_2026-11-16.xlsx
"""

import html
import os
import sys

from openpyxl import load_workbook

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
TEMPLATE = """<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="utf-8">
<style>
    body {{ margin: 0; padding: 24px; background: #ffffff;
           font-family: "Microsoft YaHei", "Segoe UI", sans-serif; color: #1f2937; }}
    .caption {{ margin: 0 0 12px; font-size: 13px; color: #6b7a90; }}
    table {{ border-collapse: collapse; font-size: 12.5px; }}
    th, td {{ border: 1px solid #d6deea; padding: 7px 10px; text-align: left;
             vertical-align: top; white-space: nowrap; }}
    th {{ background: #1f4e9c; color: #ffffff; font-weight: 600; }}
    tr:nth-child(even) td {{ background: #f8fafd; }}
    td.wrap {{ white-space: normal; max-width: 320px; }}
</style>
</head>
<body>
<p class="caption">{caption}</p>
<table>
{rows}
</table>
</body>
</html>
"""


def render(path):
    workbook = load_workbook(path, data_only=True)
    sheet = workbook.worksheets[0]
    rows = []
    for row_index, row in enumerate(sheet.iter_rows(values_only=True)):
        cells = []
        for column_index, value in enumerate(row):
            text = "" if value is None else str(value)
            if row_index == 0:
                cells.append("<th>%s</th>" % html.escape(text))
            else:
                css = ' class="wrap"' if column_index in (5, 6, 7, 8, 9) else ""
                cells.append("<td%s>%s</td>" % (css, html.escape(text)))
        rows.append("<tr>%s</tr>" % "".join(cells))
    caption = "导出的 Excel 文件内容（%s，共 %d 行数据）" % (
        os.path.basename(path).replace(".xlsx", ""), sheet.max_row - 1)
    output = os.path.join(ROOT, "docs", "screenshots", "_excel.html")
    os.makedirs(os.path.dirname(output), exist_ok=True)
    with open(output, "w", encoding="utf-8") as handle:
        handle.write(TEMPLATE.format(caption=html.escape(caption), rows="\n".join(rows)))
    print("written:", output)


if __name__ == "__main__":
    target = sys.argv[1] if len(sys.argv) > 1 else os.path.join(
        ROOT, "docs", "downloads", "通讯录_2026-11-16.xlsx")
    render(target)
