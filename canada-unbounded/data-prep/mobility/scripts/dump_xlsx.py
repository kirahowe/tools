import sys, openpyxl
wb=openpyxl.load_workbook(sys.argv[1],data_only=True,read_only=True)
for ws in wb.worksheets:
    print('### sheet',ws.title)
    for r in ws.iter_rows(values_only=True):
        if any(c is not None for c in r): print(r)
