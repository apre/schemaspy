package net.sourceforge.schemaspy.view;

import java.io.*;
import java.util.*;
import net.sourceforge.schemaspy.model.*;
import net.sourceforge.schemaspy.util.*;

public class HtmlGraphFormatter extends HtmlFormatter {
    private static boolean printedNoDotWarning = false;

    public boolean write(Table table, File graphDir, WriteStats stats, LineWriter html) {
        File oneDegreeDotFile = new File(graphDir, table.getName() + ".1degree.dot");
        File oneDegreeGraphFile = new File(graphDir, table.getName() + ".1degree.png");
        File impliedDotFile = new File(graphDir, table.getName() + ".implied2degrees.dot");
        File impliedGraphFile = new File(graphDir, table.getName() + ".implied2degrees.png");
        File twoDegreesDotFile = new File(graphDir, table.getName() + ".2degrees.dot");
        File twoDegreesGraphFile = new File(graphDir, table.getName() + ".2degrees.png");

        try {
            if (!DotRunner.generateGraph(oneDegreeDotFile, oneDegreeGraphFile))
                return false;

            html.write("<br/><b>Close relationships");
            if (stats.wroteTwoDegrees()) {
                html.writeln("</b><span class='degrees' id='degrees'>");
                html.write("&nbsp;within <input type='radio' name='degrees' id='oneDegree' onclick=\"");
                html.write("if (!this.checked)");
                html.write(" selectGraph('../graphs/" + twoDegreesGraphFile.getName() + "', '#twoDegreesRelationshipsGraph');");
                html.write("else");
                html.write(" selectGraph('../graphs/" + oneDegreeGraphFile.getName() + "', '#oneDegreeRelationshipsGraph'); ");
                html.writeln("\" checked>one");
                html.write("  <input type='radio' name='degrees' id='twoDegrees' onclick=\"");
                html.write("if (this.checked)");
                html.write(" selectGraph('../graphs/" + twoDegreesGraphFile.getName() + "', '#twoDegreesRelationshipsGraph');");
                html.write("else");
                html.write(" selectGraph('../graphs/" + oneDegreeGraphFile.getName() + "', '#oneDegreeRelationshipsGraph'); ");
                html.writeln("\">two degrees of separation");
                html.writeln("</span><b>:</b>");
            } else {
                html.write(":</b>");
            }
            html.writeln("  <a name='graph'><img src='../graphs/" + oneDegreeGraphFile.getName() + "' usemap='#oneDegreeRelationshipsGraph' id='relationships' border='0' alt='' align='left'></a>");
            DotRunner.writeMap(oneDegreeDotFile, html);
            if (stats.wroteImplied()) {
                DotRunner.generateGraph(impliedDotFile, impliedGraphFile);
                DotRunner.writeMap(impliedDotFile, html);
            } else {
                impliedDotFile.delete();
                impliedGraphFile.delete();
            }
            if (stats.wroteTwoDegrees()) {
                DotRunner.generateGraph(twoDegreesDotFile, twoDegreesGraphFile);
                DotRunner.writeMap(twoDegreesDotFile, html);
            } else {
                twoDegreesDotFile.delete();
                twoDegreesGraphFile.delete();
            }
        } catch (IOException noDot) {
            printNoDotWarning();
            return false;
        }

        return true;
    }

    public boolean write(Database db, File graphDir, String dotBaseFilespec, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        File relationshipsDotFile = new File(graphDir, dotBaseFilespec + ".real.dot");
        File relationshipsGraphFile = new File(graphDir, dotBaseFilespec + ".real.png");
        File impliedDotFile = new File(graphDir, dotBaseFilespec + ".implied.dot");
        File impliedGraphFile = new File(graphDir, dotBaseFilespec + ".implied.png");

        try {
            if (!DotRunner.generateGraph(relationshipsDotFile, relationshipsGraphFile))
                return false;

            writeRelationshipsHeader(db, relationshipsGraphFile, impliedGraphFile, "Relationships Graph", hasImpliedRelationships, html);
            html.writeln("  <a name='graph'><img src='graphs/summary/" + relationshipsGraphFile.getName() + "' usemap='#relationshipsGraph' id='relationships' border='0' alt=''></a>");
            DotRunner.writeMap(relationshipsDotFile, html);

            if (hasImpliedRelationships) {
                DotRunner.generateGraph(impliedDotFile, impliedGraphFile);
                DotRunner.writeMap(impliedDotFile, html);
            }

            writeFooter(html);
        } catch (IOException noDot) {
            printNoDotWarning();
            return false;
        }

        return true;
    }

    public boolean writeOrphans(Database db, List orphanTables, File graphDir, LineWriter html) throws IOException {
        Set orphansWithImpliedRelationships = new HashSet();
        Iterator iter = orphanTables.iterator();
        while (iter.hasNext()) {
            Table table = (Table)iter.next();
            if (!table.isOrphan(true)){
                orphansWithImpliedRelationships.add(table);
            }
        }

        writeOrphansHeader(db, "Utility Tables Graph", !orphansWithImpliedRelationships.isEmpty(), html);

        html.writeln("<a name='graph'>");
        try {
            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".1degree.dot");
                File graphFile = new File(graphDir, dotBaseFilespec + ".1degree.png");

                LineWriter dot = new LineWriter(new FileWriter(dotFile));
                new DotFormatter().writeOrphan(table, dot);
                dot.close();
                try {
                    if (!DotRunner.generateGraph(dotFile, graphFile))
                        return false;
                } catch (IOException noDot) {
                    printNoDotWarning();
                    return false;
                }

                html.write("  <img src='graphs/summary/" + graphFile.getName() + "' usemap='#" + table + "' border='0' alt='' align='top'");
                if (orphansWithImpliedRelationships.contains(table))
                    html.write(" class='impliedNotOrphan'");
                html.writeln(">");
            }

            iter = orphanTables.iterator();
            while (iter.hasNext()) {
                Table table = (Table)iter.next();
                String dotBaseFilespec = table.getName();

                File dotFile = new File(graphDir, dotBaseFilespec + ".1degree.dot");
                DotRunner.writeMap(dotFile, html);
            }

            return true;
        } finally {
            html.writeln("</a>");
            writeFooter(html);
        }
    }

    private void writeRelationshipsHeader(Database db, File relationshipsGraphFile, File impliedGraphFile, String title, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, html);
        html.writeln("<table width='100%'><tr><td class='tableHolder' align='left' valign='top'>");
        html.writeln("<br/><a href='index.html'>Back to Tables</a>");
        if (hasImpliedRelationships) {
            html.writeln("<p/><form name='options' action=''>");
            html.write("  <input type='checkbox' id='graphType' onclick=\"");
            html.write("if (!this.checked)");
            html.write(" selectGraph('graphs/summary/" + relationshipsGraphFile.getName() + "', '#relationshipsGraph'); ");
            html.write("else");
            html.write(" selectGraph('graphs/summary/" + impliedGraphFile.getName() + "', '#impliedRelationshipsGraph');");
            html.write("\">");
            html.writeln("Include implied relationships");
            html.writeln("</form>");
        }

        html.writeln("<td class='tableHolder' align='right' valign='top'>");
        writeLegend(false, html);
        html.writeln("</td></tr></table>");
    }

    private void writeOrphansHeader(Database db, String title, boolean hasImpliedRelationships, LineWriter html) throws IOException {
        writeHeader(db, null, title, html);
        html.writeln("<table width='100%'><tr><td class='tableHolder' align='left' valign='top'>");
        html.writeln("<br/><a href='index.html'>Back to Tables</a>");
        if (hasImpliedRelationships) {
            html.writeln("<p/><form action=''>");
            html.writeln(" <input type=checkbox onclick=\"toggle(" + StyleSheet.getOffsetOf(".impliedNotOrphan") + ");\" id=removeImpliedOrphans>");
            html.writeln("  Hide tables with implied relationships");
            html.writeln("</form>");
        }

        html.writeln("<td class='tableHolder' align='right' valign='top'>");
        writeLegend(false, html);
        html.writeln("</td></tr></table>");
    }

    private void printNoDotWarning() {
        if (!printedNoDotWarning) {
            printedNoDotWarning = true;
            System.err.println();
            System.err.println("Warning: Failed to run dot.");
            System.err.println("   Download it from www.graphviz.org and make sure dot is in your path.");
            System.err.println("   Generated pages will not contain a graphical view of table relationships.");
        }
    }
}