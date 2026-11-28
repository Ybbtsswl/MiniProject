import json, math, textwrap
from pathlib import Path
import xml.etree.ElementTree as ET
from reportlab.pdfgen.canvas import Canvas
from reportlab.pdfbase.pdfmetrics import stringWidth
from PIL import Image
import pypdfium2 as pdfium

out=Path('output/pdf')
out.mkdir(parents=True,exist_ok=True)
base=out/'Personal Finance Module Flowcharts'
pdf=Canvas(str(base.with_suffix('.pdf')))
pdf.setTitle('Personal Finance Module Flowcharts')
book=ET.Element('mxfile',host='app.diagrams.net')
def xml(tag,container,**attrs):
    return ET.SubElement(container,tag,{k:str(v) for k,v in attrs.items()})
def lines(value,width,size,font):
    result=[]
    for para in value.split('\n'):
        row=''
        for word in para.split():
            trial=(row+' '+word).strip()
            if row and stringWidth(trial,font,size)>width:
                result.append(row);row=word
            else:row=trial
        result.append(row)
    return result
def box(kind,value,x,y,w,h,fs=14):
    global seq
    seq+=1
    ident='n'+str(seq)
    style={'p':'rounded=0','i':'shape=parallelogram;fixedSize=1;size=12','d':'rhombus','t':'rounded=1;arcSize=80','h':'text;strokeColor=none','a':'rounded=0','s':'rounded=1;arcSize=12','l':'text;strokeColor=none'}[kind]
    border=kind not in ('h','l')
    fill='#E8EEF4' if kind=='a' else '#FFFFFF'
    font='Helvetica-Bold' if kind in ('h','a') else 'Helvetica'
    wrap=w*(0.55 if kind=='d' else 1)-(24 if kind=='i' else 12)
    txt=lines(value,wrap,fs,font)
    assert len(txt)*fs*1.15<h+1,(value,txt,h)
    cell=xml('mxCell',root,id=ident,value='\n'.join(txt),style=style+';whiteSpace=wrap;html=0;fontFamily=Helvetica;fontSize='+str(fs)+';fontStyle='+str(int(font.endswith('Bold')))+';fontColor=#202A32;'+('strokeColor=#394550;' if border else '')+'fillColor='+fill+';',vertex=1,parent=1)
    xml('mxGeometry',cell,x=x-w/2,y=y-h/2,width=w,height=h,as_='geometry').attrib['as']='geometry'
    geom=cell[0];del geom.attrib['as_']
    Y=H-y
    pdf.setLineWidth(1.25);pdf.setStrokeColor('#394550');pdf.setFillColor(fill)
    if kind in ('d','i'):
        pts=([(x-w/2,Y),(x,Y+h/2),(x+w/2,Y),(x,Y-h/2)] if kind=='d' else [(x-w/2,Y-h/2),(x-w/2+12,Y+h/2),(x+w/2,Y+h/2),(x+w/2-12,Y-h/2)])
        p=pdf.beginPath();p.moveTo(*pts[0])
        for pt in pts[1:]:p.lineTo(*pt)
        p.close();pdf.drawPath(p,fill=1)
    elif border:pdf.roundRect(x-w/2,Y-h/2,w,h,h*.44 if kind=='t' else (6 if kind=='s' else 0),fill=1)
    pdf.setFillColor('#202A32');pdf.setFont(font,fs)
    for j,row in enumerate(txt):pdf.drawCentredString(x,Y+(len(txt)-1)*fs*.575-j*fs*1.15-fs*.34,row)
    return (ident,x,y,w,h)
def edge(a,b,side=False,bus=None):
    global seq
    seq+=1
    _,x,y,w,h=a;_,X,Y,W,T=b
    start=(x+w/2,y) if side else (x,y+h/2)
    end=(X-W/2,Y) if side else (X,Y-T/2)
    bends=[] if bus is None else [(x,bus),(X,bus)]
    cell=xml('mxCell',root,id='e'+str(seq),parent=1,edge=1,source=a[0],target=b[0],style='edgeStyle=none;endArrow=block;endFill=1;strokeColor=#394550;'+('exitX=1;exitY=0.5;entryX=0;entryY=0.5;' if side else 'exitX=0.5;exitY=1;entryX=0.5;entryY=0;'))
    geo=xml('mxGeometry',cell,relative=1,**{'as':'geometry'})
    if bends:
        arr=xml('Array',geo,**{'as':'points'})
        for px,py in bends:xml('mxPoint',arr,x=px,y=py)
    pts=[start]+bends+[end];pdf.setStrokeColor('#394550');pdf.setLineWidth(1)
    for (u,v),(p,q) in zip(pts,pts[1:]):pdf.line(u,H-v,p,H-q)
    u,v=end
    offs=[(-6,-3),(-6,3)] if side else [(-3,-6),(3,-6)]
    path=pdf.beginPath();path.moveTo(u,H-v)
    for dx,dy in offs:path.lineTo(u+dx,H-v-dy)
    path.close();pdf.setFillColor('#394550');pdf.drawPath(path,fill=1)
for page,(section,title,refs,load,show,branches) in enumerate(json.loads(Path('tmp/pdfs/module-data.json').read_text(encoding='utf-8-sig'))):
    report=page==2
    W=362*len(branches)+48
    top=696 if report else 342
    H=top+max(len(b)-1 for b in branches)*124+152
    pdf.setPageSize((W,H))
    diagram=xml('diagram',book,id='module-'+section,name=section+' '+title)
    model=xml('mxGraphModel',diagram,page=1,pageWidth=W,pageHeight=H,grid=1,gridSize=10)
    root=xml('root',model);xml('mxCell',root,id=0);xml('mxCell',root,id=1,parent=0);seq=0
    cx=W/2
    box('h',section+'   '+title,cx,28,W-48,42,26)
    box('l',refs+'   |   Personal Finance Tracker',cx,56,W-48,24,13)
    prev=box('t','Main menu',cx,94,144,34)
    def nextbox(kind,label,y,w=410,h=48):
        global prev
        n=box(kind,label,cx,y,w,h);edge(prev,n);prev=n
    nextbox('p',load,152)
    nextbox('i',show,218)
    if report:
        nextbox('d','Dates valid and start ≤ end?',300,238,80)
        def aside(label,y):
            n=box('s',label,cx+234,y,170,68,13);edge(prev,n,True);box('l','No',cx+136,y-12,36,18,11)
            box('l','Yes',cx+20,y+49,36,16,11)
        aside('Show date error. Return to period selection.',300)
        nextbox('p','Generate report: retrieve own income, expense, budget and savings records for the selected period',398,425,64)
        nextbox('d','Any valid records?',488,238,78)
        aside('Show “No financial records”. Return to period selection.',488)
        nextbox('p','Calculate totals, group expenses by category and generate the period summary',575,410,58)
    nextbox('d','Choose report action' if report else 'Choose an action',top-58,220,66)
    dispatch=prev
    for j,(name,*steps) in enumerate(branches):
        x=24+j*362+106
        n=box('a',name,x,top+19,208,40,15);edge(dispatch,n,bus=top-10)
        prev=n
        for k,(kind,label,*alt) in enumerate(steps):
            y=top+110+k*124
            n=box(kind,label,x,y,212,100 if kind=='d' else 92,13)
            edge(prev,n);prev=n
            if alt:
                altbox=box('s',alt[0],x+183,y,107,102,12)
                edge(n,altbox,True);box('l','No',x+117,y-10,28,16,10)
                box('l','Yes',x+20,y+59,30,16,10)
        n=box('t','Report menu' if report else 'Refresh module menu',x,y+94,176,34,13);edge(prev,n)
    note='At the report menu, choose another action, change the period or return to the main menu.' if report else 'After a side outcome, return to the module menu unless a form is specified. From the menu, choose another action or return to the main menu.'
    box('l',note,cx,H-26,W-68,32,13);pdf.showPage()
pdf.save()
ET.ElementTree(book).write(base.with_suffix('.drawio'),encoding='utf-8',xml_declaration=True)
r=pdfium.PdfDocument(str(base.with_suffix('.pdf')))
assert len(r)==len(ET.parse(base.with_suffix('.drawio')).getroot())==3
thumbs=[]
for page in r:
    im=page.render(scale=.65).to_pil().convert('RGB');im.thumbnail((600,600));thumbs.append(im)
sheet=Image.new('RGB',(sum(im.width for im in thumbs),max(im.height for im in thumbs)),'white')
x=0
for im in thumbs:sheet.paste(im,(x,0));x+=im.width
sheet.save('tmp/pdfs/module-review.png')


