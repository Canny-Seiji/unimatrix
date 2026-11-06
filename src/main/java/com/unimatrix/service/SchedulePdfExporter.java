package com.unimatrix.service;

import com.unimatrix.model.*;
import java.awt.Font;
import java.awt.font.FontRenderContext;
import java.awt.geom.PathIterator;
import java.io.*;
import java.nio.charset.*;
import java.nio.file.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Dependency-free PDF 1.4 writer for this fixed table layout. Uses standard Courier
 * (exact 600-unit advances) and vector glyph outlines for characters outside WinAnsi.
 * The latter stay sharp but are not searchable. No screenshots or printer driver needed.
 */
public final class SchedulePdfExporter {
    private static final float WIDTH=841.89f, HEIGHT=595.28f, MARGIN=36f;
    private static final float[] COLS={105,275,105,110,174.89f};
    private static final String[] HEADERS={"Time","Course Code / Title","Room","Block Section","Faculty"};
    private static final Charset WIN=Charset.forName("windows-1252");
    private final List<StringBuilder> pages=new ArrayList<>();
    private StringBuilder content;
    private float y;
    private final ScheduleDocument document;
    private SchedulePdfExporter(ScheduleDocument document){this.document=document;}

    public static void export(ScheduleDocument document,Path destination) throws IOException {
        if(document.exams().isEmpty())throw new IOException("This schedule has no exam rows.");
        SchedulePdfExporter writer=new SchedulePdfExporter(document);writer.layout();
        Path target=destination.toAbsolutePath(),temp=Files.createTempFile(target.getParent(),".unimatrix-",".pdf");
        try {
            writer.write(temp);
            try{Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}
            catch(AtomicMoveNotSupportedException ex){Files.move(temp,target,StandardCopyOption.REPLACE_EXISTING);}
        }finally{Files.deleteIfExists(temp);}
    }
    private void layout() throws IOException {
        TreeMap<LocalDate,List<ExamSchedule>> groups=new TreeMap<>();
        for(ExamSchedule exam:document.exams()) {
            if(exam.getExamDate()==null)throw new IOException("Exam #"+exam.getId()+" has no date. Correct it before export.");
            try{ExamTimes.interval(exam.getTimeSlot());}catch(RuntimeException ex){throw new IOException("Exam #"+exam.getId()+" has an invalid time. Correct it before export.",ex);}
            groups.computeIfAbsent(exam.getExamDate(),k->new ArrayList<>()).add(exam);
        }
        newPage();
        for(var group:groups.entrySet()) {
            group.getValue().sort(Comparator.comparing((ExamSchedule e)->ExamTimes.interval(e.getTimeSlot())[0]).thenComparing(e->safe(e.getCourseCode())));
            if(y<150)newPage();
            dateHeading(group.getKey(),false);
            int row=0;
            for(ExamSchedule exam:group.getValue()) {
                String course=safe(exam.getCourseCode())+" - "+safe(exam.getCourseTitle());
                String[] cells={ExamTimes.slot(ExamTimes.interval(exam.getTimeSlot())[0],ExamTimes.interval(exam.getTimeSlot())[1]),course,safe(exam.getRoomName()),safe(exam.getBlockSection()),safe(exam.getFacultyName())};
                List<List<String>> lines=new ArrayList<>();int count=1;
                for(int i=0;i<cells.length;i++){List<String> wrapped=wrap(cells[i],(int)((COLS[i]-14)/(9.5f*.6f)));lines.add(wrapped);count=Math.max(count,wrapped.size());}
                float height=count*12+14;
                if(y-height<54){newPage();dateHeading(group.getKey(),true);}
                float x=MARGIN;
                rectangle(MARGIN,y-height,WIDTH-2*MARGIN,height,row++%2==0?"0.96 0.97 0.99":"1 1 1");
                for(int col=0;col<5;col++) {
                    float baseline=y-15;
                    for(String line:lines.get(col)){text(line,x+7,baseline,9.5f,false,"0.12 0.16 0.22");baseline-=12;}
                    line(x,y,x,y-height);x+=COLS[col];
                }
                line(x,y,x,y-height);line(MARGIN,y-height,WIDTH-MARGIN,y-height);y-=height;
            }
            y-=18;
        }
        for(int i=0;i<pages.size();i++) {
            content=pages.get(i);
            text("UniMatrix | Schedule #"+document.id(),MARGIN,27,8,false,"0.35 0.40 0.48");
            text("Page "+(i+1)+" of "+pages.size(),WIDTH-120,27,8,false,"0.35 0.40 0.48");
        }
    }
    private void newPage() throws IOException {
        content=new StringBuilder();pages.add(content);y=HEIGHT-MARGIN;
        text("UNIMATRIX  /  EXAMINATION SCHEDULE",MARGIN,y,11,true,"0.10 0.25 0.44");y-=25;
        for(String line:wrap(document.name(),86)){text(line,MARGIN,y,14,true,"0.10 0.15 0.22");y-=18;}
        String meta="Period: "+safe(document.period())+"   Academic year: "+safe(document.academicYear())+"   Semester: "+safe(document.semester());
        for(String line:wrap(meta,125)){text(line,MARGIN,y,9.5f,false,"0.30 0.35 0.42");y-=13;}
        y-=12;
    }
    private void dateHeading(LocalDate date,boolean continued) throws IOException {
        rectangle(MARGIN,y-24,WIDTH-2*MARGIN,24,"0.10 0.25 0.44");
        text(date.format(DateTimeFormatter.ofPattern("EEEE, dd MMMM uuuu",Locale.ENGLISH))+(continued?" (continued)":""),MARGIN+8,y-16,10,true,"1 1 1");y-=24;
        rectangle(MARGIN,y-24,WIDTH-2*MARGIN,24,"0.87 0.91 0.96");
        float x=MARGIN;for(int i=0;i<5;i++){text(HEADERS[i],x+7,y-16,9.5f,true,"0.12 0.16 0.22");x+=COLS[i];}y-=24;
    }
    static List<String> wrap(String source,int limit) {
        String normalized=source==null||source.isBlank()?"Not set":source.replaceAll("\\s+"," ").trim();
        List<String> lines=new ArrayList<>();StringBuilder current=new StringBuilder();
        for(String word:normalized.split(" ")) {
            int[] points=word.codePoints().toArray();
            if(current.codePointCount(0,current.length())+points.length+(current.length()>0?1:0)>limit&&current.length()>0){lines.add(current.toString());current.setLength(0);}
            int pos=0;while(points.length-pos>limit){lines.add(new String(points,pos,limit));pos+=limit;}
            if(current.length()>0)current.append(' ');current.append(new String(points,pos,points.length-pos));
        }
        if(current.length()>0)lines.add(current.toString());return lines;
    }
    private static String safe(String value){return value==null||value.isBlank()?"Not set":value;}
    private void text(String value,float x,float baseline,float size,boolean bold,String color) throws IOException {
        if(WIN.newEncoder().canEncode(value)) {
            StringBuilder escaped=new StringBuilder();
            for(byte b:value.getBytes(WIN)){int v=b&255;if(v=='('||v==')'||v=='\\')escaped.append('\\').append((char)v);else if(v<32||v>=127)escaped.append(String.format(Locale.ROOT,"\\%03o",v));else escaped.append((char)v);}
            content.append("BT /F").append(bold?2:1).append(' ').append(n(size)).append(" Tf ").append(color).append(" rg ").append(n(x)).append(' ').append(n(baseline)).append(" Td (").append(escaped).append(") Tj ET\n");
        } else {
            // Embed glyph paths for non-WinAnsi names instead of dropping/replacing characters.
            Font font=new Font(Font.MONOSPACED,bold?Font.BOLD:Font.PLAIN,1).deriveFont(size);
            if(font.canDisplayUpTo(value)!=-1)throw new IOException("A name contains characters unsupported by installed fonts. Install a font covering those characters before exporting.");
            var glyphs=font.createGlyphVector(new FontRenderContext(null,true,true),value);
            PathIterator path=glyphs.getOutline().getPathIterator(null,.15);double[] p=new double[6];
            content.append(color).append(" rg\n");
            while(!path.isDone()) {
                int type=path.currentSegment(p);
                if(type==PathIterator.SEG_MOVETO||type==PathIterator.SEG_LINETO)content.append(n(x+p[0])).append(' ').append(n(baseline-p[1])).append(type==PathIterator.SEG_MOVETO?" m\n":" l\n");
                else if(type==PathIterator.SEG_CLOSE)content.append("h\n");
                path.next();
            }
            content.append("f*\n");
        }
    }
    private void rectangle(float x,float bottom,float width,float height,String color){content.append(color).append(" rg ").append(n(x)).append(' ').append(n(bottom)).append(' ').append(n(width)).append(' ').append(n(height)).append(" re f\n");}
    private void line(float x,float y,float xx,float yy){content.append("0.80 0.84 0.89 RG 0.4 w ").append(n(x)).append(' ').append(n(y)).append(" m ").append(n(xx)).append(' ').append(n(yy)).append(" l S\n");}
    private static String n(double number){return String.format(Locale.ROOT,"%.3f",number);}
    private void write(Path path) throws IOException {
        List<byte[]> objects=new ArrayList<>();objects.add(bytes("<< /Type /Catalog /Pages 2 0 R >>"));
        StringBuilder kids=new StringBuilder();for(int i=0;i<pages.size();i++)kids.append(5+i*2).append(" 0 R ");
        objects.add(bytes("<< /Type /Pages /Count "+pages.size()+" /Kids ["+kids+"] >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Courier /Encoding /WinAnsiEncoding >>"));
        objects.add(bytes("<< /Type /Font /Subtype /Type1 /BaseFont /Courier-Bold /Encoding /WinAnsiEncoding >>"));
        for(int i=0;i<pages.size();i++) {
            objects.add(bytes("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 "+WIDTH+" "+HEIGHT+"] /Resources << /Font << /F1 3 0 R /F2 4 0 R >> >> /Contents "+(6+i*2)+" 0 R >>"));
            byte[] stream=bytes(pages.get(i).toString());ByteArrayOutputStream data=new ByteArrayOutputStream();data.write(bytes("<< /Length "+stream.length+" >>\nstream\n"));data.write(stream);data.write(bytes("endstream"));objects.add(data.toByteArray());
        }
        ByteArrayOutputStream out=new ByteArrayOutputStream();out.write(bytes("%PDF-1.4\n"));List<Integer> offsets=new ArrayList<>();
        for(int i=0;i<objects.size();i++){offsets.add(out.size());out.write(bytes((i+1)+" 0 obj\n"));out.write(objects.get(i));out.write(bytes("\nendobj\n"));}
        int xref=out.size();out.write(bytes("xref\n0 "+(objects.size()+1)+"\n0000000000 65535 f \n"));
        for(int offset:offsets)out.write(bytes(String.format(Locale.ROOT,"%010d 00000 n \n",offset)));
        out.write(bytes("trailer\n<< /Size "+(objects.size()+1)+" /Root 1 0 R >>\nstartxref\n"+xref+"\n%%EOF\n"));
        Files.write(path,out.toByteArray());
    }
    private static byte[] bytes(String text){return text.getBytes(StandardCharsets.US_ASCII);}
}
