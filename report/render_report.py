"""Render the report narrative and readable landscape diagram appendices.
Run with reportlab, markdown, beautifulsoup4, pillow and pypdf installed.
"""
from pathlib import Path
import re,html,json,xml.etree.ElementTree as ET
import markdown
from bs4 import BeautifulSoup,Tag
from PIL import Image as PILImage
from reportlab.platypus import BaseDocTemplate,PageTemplate,Frame,Paragraph,Spacer,Table,TableStyle,PageBreak,NextPageTemplate,Image,KeepTogether,Preformatted
from reportlab.lib.pagesizes import A4,landscape
from reportlab.lib.styles import getSampleStyleSheet,ParagraphStyle
from reportlab.lib import colors
from reportlab.pdfbase import pdfmetrics
from reportlab.pdfbase.ttfonts import TTFont
from reportlab.lib.enums import TA_LEFT
B=Path(__file__).resolve().parent;OUT=B/'Restaurant_Rush_Group_Report.pdf'
for name,file in [('Arial','Arial.ttf'),('Arial-Bold','Arial Bold.ttf'),('Arial-Italic','Arial Italic.ttf'),('Arial-BoldItalic','Arial Bold Italic.ttf')]:pdfmetrics.registerFont(TTFont(name,'/System/Library/Fonts/Supplemental/'+file))
pdfmetrics.registerFontFamily('Arial',normal='Arial',bold='Arial-Bold',italic='Arial-Italic',boldItalic='Arial-BoldItalic')
styles=getSampleStyleSheet();styles.add(ParagraphStyle(name='Body',fontName='Arial',fontSize=10.3,leading=14.7,spaceAfter=8))
for name,size,leading in [('H1',23,28),('H2',17,22),('H3',14,19),('H4',12,17),('H5',11,16)]:styles.add(ParagraphStyle(name=name,fontName='Arial-Bold',fontSize=size,leading=leading,spaceBefore=13,spaceAfter=8,keepWithNext=True,textColor=colors.HexColor('#183447')))
styles.add(ParagraphStyle(name='Cell',fontName='Arial',fontSize=8.3,leading=11.4,spaceAfter=0))
styles.add(ParagraphStyle(name='Caption',fontName='Arial',fontSize=9,leading=12,textColor=colors.HexColor('#435966'),spaceAfter=8))
styles.add(ParagraphStyle(name='Mono',fontName='Courier',fontSize=8.4,leading=11,spaceAfter=9))
def footer(canvas,doc):
 w,h=canvas._pagesize;canvas.setFont('Arial',8);canvas.setFillColor(colors.HexColor('#53616b'));canvas.drawString(36,22,'Restaurant Rush · Group report · 15 November 2026');canvas.drawRightString(w-36,22,str(doc.page))
pw,ph=A4;lw,lh=landscape(A4)
doc=BaseDocTemplate(str(OUT),pagesize=A4,leftMargin=40,rightMargin=40,topMargin=38,bottomMargin=38,title='Restaurant Rush — Group report',author='Victor Chua, Danielle, Abraham and Talon')
doc.addPageTemplates([PageTemplate(id='portrait',frames=[Frame(40,40,pw-80,ph-80,id='p',leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)],onPage=footer,pagesize=A4),PageTemplate(id='landscape',frames=[Frame(32,38,lw-64,lh-70,id='l',leftPadding=0,rightPadding=0,topPadding=0,bottomPadding=0)],onPage=footer,pagesize=landscape(A4))])
FIGURE_PAGES=json.loads((B/'evidence/figure-pages.json').read_text()) if (B/'evidence/figure-pages.json').exists() else {}
NEW_FIGURE_PAGES={}
# Canonical diagram layout: diagrams/stage N/... (see diagrams/README.md).
DIAGRAMS=B.parent/'diagrams'
REPORT_PNG_DIRS=[DIAGRAMS/'stage 1/report-print/png',DIAGRAMS/'stage 2/report-print/png',DIAGRAMS/'stage 3/report-print/class/png']
SEQ_PNG=DIAGRAMS/'stage 3/report-print/sequence/png'
def class_png(stem):
 found=[d/(stem+'.png') for d in REPORT_PNG_DIRS if (d/(stem+'.png')).exists()]
 if len(found)!=1:raise RuntimeError(f'Expected exactly one report-print PNG for {stem}: {found}')
 return found[0]
CLASS_INFO=[('class-stage1-control-report','1','Stage 1 control'),('class-stage2-customer-report','2','Stage 2 customer extension'),('class-control-report-1','3','Stage 3 controller collaboration'),('class-control-report-2','3b','Interaction contract'),('class-entities-report-1','4','Customer, order and table'),('class-entities-report-3','4b','Status and action values'),('class-ownership-report','4c','Restaurant ownership'),('class-staff-report','5','Staff capacity'),('class-support-report-2','5b','Restaurant staff references'),('class-menu-report-1','6','Menu and shared combo components'),('class-menu-report-2','6b','Pricing strategies'),('class-support-report-1','7','Money, results and EOF')]
SEQ_NAMES={'01':'Turn 13: scheduled arrival','02':'Turn 13: activation and recovery alternatives','03':'Turn 13: Host seating and retries','04':'Turn 13: Pasta order and price lock','05':'Turn 13: first preparation unit','06':'Turn 14: Burger order and second recovery','06b-ready':'Turn 14: Pasta becomes READY','07':'Turn 15: serving Pasta and kitchen choice','07b-ready':'Turn 15: Burger becomes READY','08':'Turn 16: serving Burger and eating','09':'Turn 17: payment and table release','10':'Turn 18: payment and final result'}
FIG_META={stem:(num,label) for stem,num,label in CLASS_INFO}
for image in sorted(SEQ_PNG.glob('sequence-selected-*.png')):
 match=re.match(r'sequence-selected-(\d+b-ready|\d+)(?:-(\d+))?$',image.stem)
 if match:
  part,panel=match.groups();FIG_META[image.stem]=('S'+part+'.'+(panel or '1'),SEQ_NAMES.get(part,part)+' — panel '+(panel or '1'))
outline_counter=[0]
def after_flowable(flowable):
 if isinstance(flowable,Paragraph) and (flowable.style.name in ['H1','H2','H3'] or flowable.getPlainText().startswith('Figure ')):
  outline_counter[0]+=1;key='section'+str(outline_counter[0]);doc.canv.bookmarkPage(key);doc.canv.addOutlineEntry(flowable.getPlainText(),key,level=0,closed=False)
  if hasattr(flowable,'figure_stem'):NEW_FIGURE_PAGES[flowable.figure_stem]=doc.page
doc.afterFlowable=after_flowable
def inline(node):
 out=''
 for c in node.children:
  if not isinstance(c,Tag):out+=html.escape(str(c));continue
  content=inline(c)
  if c.name in ['strong','b']:out+='<b>'+content+'</b>'
  elif c.name in ['em','i']:out+='<i>'+content+'</i>'
  elif c.name=='code':out+='<font name="Courier">'+content+'</font>'
  elif c.name=='br':out+='<br/>'
  elif c.name=='a' and c.get('href','').startswith('http'):out+='<a href="'+html.escape(c['href'],quote=True)+'" color="#27695f">'+content+'</a>'
  else:out+=content
 return out
soup=BeautifulSoup(markdown.markdown((B/'GROUP_REPORT.md').read_text(),extensions=['tables','fenced_code']),'html.parser');story=[]
for n in soup.children:
 if not isinstance(n,Tag):continue
 if n.name=='p' and n.find('img'):
  for im in n.find_all('img'):
   stem=Path(im.get('src','')).stem;page=FIGURE_PAGES.get(stem);where='Appendix A, p. '+str(page) if page else 'Appendix A';story.append(Paragraph(html.escape(im.get('alt','Figure'))+' — '+where+'.',styles['Caption']))
 elif n.name in ['h1','h2','h3','h4','h5','h6']:
  story.append(Paragraph(inline(n),styles['H'+str(min(5,int(n.name[1:])))]))
 elif n.name=='p':story.append(Paragraph(inline(n),styles['Body']))
 elif n.name=='table':
  rows=[]
  for tr in n.find_all('tr'):rows.append([Paragraph(inline(c),styles['Cell']) for c in tr.find_all(['th','td'])])
  cols=max(map(len,rows));width=pw-80
  if cols==2:ws=[width*.34,width*.66]
  elif cols==3:ws=[width*.22,width*.39,width*.39]
  elif cols>=5:ws=[width*.30]+[width*.70/(cols-1)]*(cols-1)
  else:ws=[width/cols]*cols
  t=Table(rows,colWidths=ws,repeatRows=1,hAlign='LEFT');t.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e8ecdf')),('VALIGN',(0,0),(-1,-1),'TOP'),('LINEBELOW',(0,0),(-1,0),.8,colors.HexColor('#183447')),('LINEBELOW',(0,1),(-1,-1),.3,colors.HexColor('#ccd1cd')),('TOPPADDING',(0,0),(-1,-1),7),('BOTTOMPADDING',(0,0),(-1,-1),7)]));story.extend([t,Spacer(1,10)])
 elif n.name in ['ul','ol']:
  for i,li in enumerate(n.find_all('li',recursive=False)):story.append(Paragraph(('• ' if n.name=='ul' else str(i+1)+'. ')+inline(li),styles['Body']))
 elif n.name=='pre':
  # Wrap console/code lines to avoid clipping, preserving tokens and line order.
  import textwrap
  txt='\n'.join('\n'.join(textwrap.wrap(line,width=98,replace_whitespace=False,drop_whitespace=False) or ['']) for line in n.get_text().splitlines());story.append(Preformatted(txt,styles['Mono']))
 elif n.name=='blockquote':story.append(Paragraph(inline(n),styles['Caption']))
# One-page guide; page numbers stabilise on a second render pass.
story.append(PageBreak());story.append(Paragraph('Diagram guide and appendix navigation',styles['H2']))
guide=[[Paragraph('Figure / part',styles['Cell']),Paragraph('Coverage',styles['Cell']),Paragraph('Page',styles['Cell'])]]
for stem,num,label in CLASS_INFO:guide.append([Paragraph('Figure '+num,styles['Cell']),Paragraph(label,styles['Cell']),Paragraph(str(FIGURE_PAGES.get(stem,'—')),styles['Cell'])])
for part,label in SEQ_NAMES.items():
 candidates=[stem for stem in FIG_META if stem.startswith('sequence-selected-'+part) and FIG_META[stem][1].startswith(label)]
 page=min([FIGURE_PAGES[x] for x in candidates if x in FIGURE_PAGES],default='—')
 guide.append([Paragraph('Sequence '+part,styles['Cell']),Paragraph(label,styles['Cell']),Paragraph(str(page),styles['Cell'])])
g=Table(guide,colWidths=[95,350,50],repeatRows=1);g.setStyle(TableStyle([('BACKGROUND',(0,0),(-1,0),colors.HexColor('#e8ecdf')),('VALIGN',(0,0),(-1,-1),'TOP'),('TOPPADDING',(0,0),(-1,-1),4),('BOTTOMPADDING',(0,0),(-1,-1),4),('LINEBELOW',(0,0),(-1,-1),.25,colors.HexColor('#ccd1cd'))]));story.append(g)
story.append(Paragraph('PDF bookmarks link directly to each figure. Continued panels repeat the same objects and active guards. READY detail panels expand the preceding kitchen action; they do not add an extra staff task.',styles['Caption']))
story.extend([NextPageTemplate('landscape'),PageBreak()]);audit=[]
classes=[class_png(stem) for stem,num,label in CLASS_INFO];seq=sorted(SEQ_PNG.glob('sequence-selected-*.png'))
for k,p in enumerate(classes+seq):
 if k:story.append(PageBreak())
 num,label=FIG_META.get(p.stem,(p.stem,p.stem));heading=Paragraph('Figure '+num+' · '+label,styles['H4']);heading.figure_stem=p.stem;story.append(heading)
 w,h=PILImage.open(p).size;maxw=lw-70;maxh=lh-130;scale=min(maxw/w,maxh/h);fontpt=18*1.5*scale
 if fontpt<7:raise RuntimeError(f'Figure text below 7 pt: {p.name}: {fontpt:.2f}')
 story.append(Image(str(p),width=w*scale,height=h*scale,hAlign='CENTER'))
 story.append(Paragraph(f'Editable native draw.io source · selected members / repeated participants · minimum source text renders at approximately {fontpt:.1f} pt.',styles['Caption']))
 audit.append({'figure':p.name,'number':num,'caption':label,'image_size':[w,h],'placed_points':[round(w*scale,1),round(h*scale,1)],'minimum_source_font_px':18,'nominal_minimum_pdf_font_pt':round(fontpt,2)})
doc.build(story)
(B/'evidence/figure-pages.json').write_text(json.dumps(NEW_FIGURE_PAGES,indent=2))
(B/'evidence/figure-size-audit.json').write_text(json.dumps(audit,indent=2));print(OUT);print('Figures',len(audit),'minimumpt',min(x['nominal_minimum_pdf_font_pt'] for x in audit))
