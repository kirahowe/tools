import openpyxl,csv,sys
src,out=sys.argv[1:3]
wb=openpyxl.load_workbook(src,data_only=True); ws=wb.active
w=csv.writer(open(out,'w',newline=''))
w.writerow(['year','province','municipality','measure','rate_pct_of_assessed_value','representative_house_value_cad','variable_rate_taxes_cad','total_taxes_and_charges_cad','method','source'])
# Ontario statutory 2025 residential total rates (municipal+upper tier+education)
for m,r,s in [('Toronto',0.754087,'https://www.toronto.ca/services-payments/property-taxes-utilities/property-tax/property-tax-rates-and-fees/2025-property-tax-rates/'),
              ('Mississauga',1.033864,'https://peelregion.ca/sites/default/files/2025-07/Appendix-1-%202025-tax-rate_0.pdf'),
              ('Brampton',1.200644,'https://peelregion.ca/sites/default/files/2025-07/Appendix-1-%202025-tax-rate_0.pdf'),
              ('Caledon',0.935400,'https://peelregion.ca/sites/default/files/2025-07/Appendix-1-%202025-tax-rate_0.pdf')]:
    w.writerow([2025,'Ontario',m,'statutory residential (RT) total tax rate',r,'','','','official rate; applied to MPAC CVA (2016 base valuation date in Ontario)',s])
for row in list(ws.iter_rows(values_only=True))[2:]:
    if not row[0] or not isinstance(row[3],(int,float)) or not row[3]: continue
    hv,var,tot=row[3],row[9],row[12]
    w.writerow([2025,'British Columbia',row[0].strip(),'effective residential rate on representative house (variable-rate taxes / house value)',round(100*var/hv,4),hv,var,tot,'computed from BC Schedule 704 (2025)','https://www2.gov.bc.ca/assets/gov/british-columbians-our-governments/local-governments/finance/local-government-statistics/schedule704_2025.xlsx'])
