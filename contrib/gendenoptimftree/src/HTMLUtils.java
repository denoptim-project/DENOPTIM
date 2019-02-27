/*******************************************************************************
 *
 * This file may be distributed and/or modified under the terms of the
 * GNU General Public License version 3 as published by the Free Software
 * Foundation and appearing in the file LICENSE.GPL included in the
 * packaging of this file.
 *
 * This file is provided AS IS with NO WARRANTY OF ANY KIND, INCLUDING THE
 * WARRANTY OF DESIGN, MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
 ******************************************************************************/


package gendenoptimftree;

import java.io.IOException;
import java.io.StringWriter;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Writer;

/**
 *
 * @author Vishwesh Venkatraman
 */
public class HTMLUtils 
{
    
//------------------------------------------------------------------------------

    /**
     * Generate the ChemDoodle representation of the molecule
     *
     * @param mol
     * @return molecule as a formatted string
     * @throws java.lang.Exception
     */
    protected static String getChemDoodleString(IAtomContainer mol) throws Exception
    {
        StringWriter stringWriter = new StringWriter();
        MDLV2000Writer mw = null;
        try
        {
            mw = new MDLV2000Writer(stringWriter);
            mw.write(mol);
        }
        catch (CDKException cdke)
        {
            Logger.getLogger(HTMLUtils.class.getName()).log(Level.SEVERE, null, cdke);
            throw cdke;
        }
        finally
        {
            try
            {
                if (mw != null)
                {
                    mw.close();
                }
            }
            catch (IOException ioe)
            {
                Logger.getLogger(HTMLUtils.class.getName()).log(Level.SEVERE, null, ioe);
                throw ioe;
            }
        }

        String MoleculeString = stringWriter.toString();

        //System.out.print(stringWriter.toString());
        //now split MoleculeString into multiple lines to enable explicit printout of \n
        String Moleculelines[] = MoleculeString.split("\\r?\\n");

        StringBuilder sb = new StringBuilder(1024);
        sb.append("var molFile = '");
        for (String Moleculeline : Moleculelines)
        {
            sb.append(Moleculeline);
            sb.append("\\n");
        }
        sb.append("';");
        return sb.toString();
    }
    
    
//------------------------------------------------------------------------------

    /**
     * Creates a ChemDoodle representation of the molecule. Requires ChemDoodle
     * components to be supplied.
     * @param htmlfile the html filename to be written to
     * @param molname the molecule title
     * @param mol the CDK container
     * @param cssFile
     * @param jsFile
     * @throws Exception
     */

    protected static void moleculeToHTML(String htmlfile, String molname, 
        IAtomContainer mol, String cssFile, String jsFile) throws Exception
    {
        StringBuilder sb = new StringBuilder(1024);

        sb.append(Constants.DOCTYPE_TAG).append(System.getProperty("line.separator"));
        sb.append(Constants.HTML_START_TAG).append(System.getProperty("line.separator"));
        sb.append(Constants.HEAD_START_TAG).append(System.getProperty("line.separator"));
        sb.append(Constants.META_TAG).append(System.getProperty("line.separator"));

        sb.append("<link rel=\"stylesheet\" href=\"").append(cssFile)
                .append("\" type=\"text/css\">").append(System.getProperty("line.separator"));
        sb.append("<script type=\"text/javascript\" src=\"").
                append(jsFile).append("\"></script>").append(System.getProperty("line.separator"));

        String htmlTitle = Constants.TITLE_START_TAG + molname +
                    Constants.TITLE_CLOSE_TAG + System.getProperty("line.separator");
        sb.append(htmlTitle);

        for (String NEWSPAPER_CSS_TAG : Constants.NEWSPAPER_CSS_TAG)
        {
            sb.append(NEWSPAPER_CSS_TAG);
        }

        for (String HEADER_CSS_TAG : Constants.HEADER_CSS_TAG)
        {
            sb.append(HEADER_CSS_TAG);
        }

        sb.append(Constants.HEAD_CLOSE_TAG).append(System.getProperty("line.separator"));

        sb.append(Constants.BODY_START_TAG).append(System.getProperty("line.separator"));

        sb.append(Constants.CENTER_TAG_START).append(System.getProperty("line.separator"));
        sb.append(Constants.H1_TAG_START).append(molname).
                append(Constants.H1_TAG_END).append(System.getProperty("line.separator"));
        sb.append(Constants.CENTER_TAG_END).append(System.getProperty("line.separator"));


        String moldata = getChemDoodleString(mol);

        String cmol = "bsmol";

        String DOODLE_START = "var " + cmol +
                    " = new ChemDoodle.TransformCanvas3D('" + cmol + "', 400, 400);\n" +
                    cmol + ".specs.set3DRepresentation('Ball and Stick');\n" +
                    cmol + ".specs.backgroundColor = 'black';\n";

        String DOODLE_END =
                    "var molecule = ChemDoodle.readMOL(molFile, 1);\n" +
                    cmol + ".loadMolecule(molecule);\n";

        String showStr = Constants.CENTER_TAG_START + System.getProperty("line.separator") +
                        Constants.SCRIPT_START + System.getProperty("line.separator") +
                        DOODLE_START + moldata + System.getProperty("line.separator") + DOODLE_END +
                        Constants.SCRIPT_END + System.getProperty("line.separator") +
                        Constants.CENTER_TAG_END + System.getProperty("line.separator");
        sb.append(showStr);

        sb.append(Constants.BR_TAG).append(System.getProperty("line.separator"));
        sb.append(Constants.BR_TAG).append(System.getProperty("line.separator"));
        sb.append(Constants.TABLE_START_TAG_NW).append(System.getProperty("line.separator"));


        Map<Object, Object> properties = mol.getProperties();
        Iterator<Object> propNames = properties.keySet().iterator();
        while (propNames.hasNext())
        {
            String propName = (String) propNames.next();

            if (propName.equalsIgnoreCase("_MOLCOUNT"))
                continue;
            if (propName.equalsIgnoreCase("cdk:Title"))
                continue;
            if (propName.startsWith("cdk:"))
                continue;

            sb.append(Constants.TR_START_TAG).append(System.getProperty("line.separator"));
            sb.append(Constants.TD_START_TAG).append("<b>").
                append(propName).append("</b>");
            sb.append(Constants.TD_CLOSE_TAG).append(System.getProperty("line.separator"));

            if (mol.getProperty(propName) == null)
                continue;

            String str = mol.getProperty(propName).toString();

            if (Utils.isNumeric(str))
            {
                if (propName.equalsIgnoreCase("ROT_BND"))
                {
                    sb.append(Constants.TD_START_TAG).append(str);
                }
                else if (propName.equalsIgnoreCase("GCODE"))
                {
                    sb.append(Constants.TD_START_TAG).append(str);
                }
                else
                    sb.append(Constants.TD_START_TAG).append(String.format("%8.4f", Double.valueOf(str)));
            }
            else
                sb.append(Constants.TD_START_TAG).append(str);
            sb.append(Constants.TD_CLOSE_TAG).append(System.getProperty("line.separator"));
            sb.append(Constants.TR_CLOSE_TAG).append(System.getProperty("line.separator"));
        }


        sb.append(Constants.TABLE_CLOSE_TAG).append(System.getProperty("line.separator"));

        sb.append(Constants.BODY_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append(Constants.HTML_CLOSE_TAG).append(System.getProperty("line.separator"));

        IO.writeFile(htmlfile, sb.toString());

        sb.setLength(0);
        sb = null;
    }
    
//------------------------------------------------------------------------------

    /**
     *
     * @param htmlfile
     * @param title
     * @param lstMols
     * @param dformat
     * @param cssFile
     * @param jsFile
     * @throws Exception
     */

    protected static void createHTML(String htmlfile, String title, ArrayList<MolData> lstMols, 
        DecimalFormat dformat, String cssFile, String jsFile) throws Exception
    {
        StringBuilder sb = new StringBuilder(2048);
        sb.append(Constants.DOCTYPE_TAG).append(System.getProperty("line.separator"));
        sb.append(Constants.HTML_START_TAG).append(System.getProperty("line.separator"));
        sb.append("\t").append(Constants.HEAD_START_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t").append(Constants.META_TAG).append(System.getProperty("line.separator"));

        String htmlTitle = "\t\t" + Constants.TITLE_START_TAG +
                        title + Constants.TITLE_CLOSE_TAG + System.getProperty("line.separator");

        sb.append(htmlTitle);

        for (String CSS_TAG : Constants.CSS_TAG)
        {
            sb.append("\t\t").append(CSS_TAG);
        }

        boolean usedoodle = false;
        if (cssFile.length() > 0 && jsFile.length() > 0)
            usedoodle = true;

        if (usedoodle)
        {
            for (String ANCHOR_CSS : Constants.ANCHOR_CSS)
            {
                sb.append("\t\t").append(ANCHOR_CSS);
            }

            for (String JS_POPUP_SCRIPT : Constants.JS_POPUP_SCRIPT)
            {
                sb.append("\t\t").append(JS_POPUP_SCRIPT);
            }
        }

        for (String D3_TAG : Constants.D3TABLE)
        {
            sb.append(D3_TAG);
        }
        sb.append(System.getProperty("line.separator"));


        sb.append("\t").append(Constants.HEAD_CLOSE_TAG).append(System.getProperty("line.separator"));

        sb.append("\t").append(Constants.BODY_START_TAG).append(System.getProperty("line.separator"));

        sb.append("\t\t").append("<table id=\"example\" class=\"display\" "
                + "cellspacing=\"0\" width=\"100%\">").append(System.getProperty("line.separator"));

        // write the column header/footer block
        sb.append("\t\t\t").append(Constants.THEAD_START_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t").append(Constants.TR_START_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t\t").append(Constants.TH_SCOPE_START_TAG).append("Generation");
        sb.append(Constants.TH_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t\t").append(Constants.TH_SCOPE_START_TAG).append("Molecule");
        sb.append(Constants.TH_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t\t").append(Constants.TH_SCOPE_START_TAG).append("Structure");
        sb.append(Constants.TH_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t\t").append(Constants.TH_SCOPE_START_TAG).append("Fitness");
        sb.append(Constants.TH_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t").append(Constants.TR_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t").append(Constants.THEAD_CLOSE_TAG).append(System.getProperty("line.separator"));
        // footer
        sb.append("\t\t\t").append(Constants.TFOOT_START_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t").append(Constants.TR_START_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t\t").append(Constants.TH_SCOPE_START_TAG).append("Generation");
        sb.append(Constants.TH_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t\t").append(Constants.TH_SCOPE_START_TAG).append("Molecule");
        sb.append(Constants.TH_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t").append(Constants.TH_SCOPE_START_TAG).append("Structure");
        sb.append(Constants.TH_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t\t").append(Constants.TH_SCOPE_START_TAG).append("Fitness");
        sb.append(Constants.TH_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t\t").append(Constants.TR_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t\t\t").append(Constants.TFOOT_CLOSE_TAG).append(System.getProperty("line.separator"));

        sb.append("\t\t\t").append(Constants.TBODY_START_TAG).append(System.getProperty("line.separator"));

        // now fill in the data
        for (MolData mol:lstMols)
        {
            sb.append("\t\t\t\t").append(Constants.TR_START_TAG).append(System.getProperty("line.separator"));
            sb.append("\t\t\t\t\t").append(Constants.TD_START_TAG);
            sb.append(mol.getGeneration());
            sb.append(Constants.TD_CLOSE_TAG).append(System.getProperty("line.separator"));

            sb.append("\t\t\t\t\t").append(Constants.TD_START_TAG);
            if (usedoodle)
            {
                sb.append("<a href=\"javascript:popUp('").append(mol.getHtmlFile())
                    .append("')\">").append(mol.getMolID()).append("</a>");
            }
            else
            {
                sb.append(mol.getMolID());
            }
            sb.append(Constants.TD_CLOSE_TAG).append(System.getProperty("line.separator"));


            sb.append("\t\t\t\t\t").append(Constants.TD_START_TAG);
            sb.append("<object data=\"").append(mol.getImgFile())
                .append("\" type=\"image/svg+xml\" width=\"300\" height=\"300\" "
                + "alt=\\\"Star\\\"></object>");
            sb.append(Constants.TD_CLOSE_TAG).append(System.getProperty("line.separator"));


            sb.append("\t\t\t\t\t").append(Constants.TD_START_TAG);
            sb.append(dformat.format(mol.getFitness()));
            sb.append(Constants.TD_CLOSE_TAG).append(System.getProperty("line.separator"));

            sb.append("\t\t\t\t").append(Constants.TR_CLOSE_TAG).append(System.getProperty("line.separator"));
        }


        sb.append("\t\t\t").append(Constants.TBODY_CLOSE_TAG).append(System.getProperty("line.separator"));


        sb.append("\t\t").append(Constants.TABLE_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append("\t").append(Constants.BODY_CLOSE_TAG).append(System.getProperty("line.separator"));
        sb.append(Constants.HTML_CLOSE_TAG).append(System.getProperty("line.separator"));

        IO.writeFile(htmlfile, sb.toString());

        sb.setLength(0);
        sb = null;

    }
    
//------------------------------------------------------------------------------
    
    
}
