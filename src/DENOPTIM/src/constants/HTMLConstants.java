package constants;

/**
 * HTML tags used for creating the output results page.
 * @author Vishwesh Venkatraman
 */


public final class HTMLConstants 
{
    public static final String DOCTYPE_TAG = 
    "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\"\n" +
    "          \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">";

    public static final String HTML_START_TAG = 
        "<html xmlns=\"http://www.w3.org/1999/xhtml\" xml:lang=\"en-GB\">";

    public static final String HEAD_START_TAG = "<head>";

    public static final String META_TAG = 
    "<meta http-equiv=\"X-UA-Compatible\" content=\"chrome=1\">\n" +
    "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\" />";


    public static final String HEAD_CLOSE_TAG = "</head>";


    public static final String TITLE_START_TAG = "<title>";
    public static final String TITLE_CLOSE_TAG = "</title>";

    public static final String HTML_CLOSE_TAG = "</html>";

    public static final String TABLE_START_TAG_HB = "<table id=\"hor-minimalist-b\">";
    public static final String TABLE_START_TAG_NW = "<table id=\"newspaper-b\">";

    public static final String TABLE_CLOSE_TAG = "</table>";

    public static final String BODY_START_TAG = "<body>";

    public static final String BODY_CLOSE_TAG = "</body>";

    public static final String CAPTION_START_TAG = "<caption>";

    public static final String CAPTION_CLOSE_TAG = "</caption>";

    public static final String THEAD_START_TAG = "<thead>";	

    public static final String THEAD_CLOSE_TAG = "</thead>";

    public static final String TH_SCOPE_START_TAG = "<th scope=\"col\">";

    public static final String TH_CLOSE_TAG = "</th>";

    public static final String TD_START_TAG = "<td>";

    public static final String TD_CLOSE_TAG = "</td>";

    public static final String TR_START_TAG = "<tr>";

    public static final String TR_CLOSE_TAG = "</tr>";

    public static final String TBODY_START_TAG = "<tbody>";

    public static final String TBODY_CLOSE_TAG = "</tbody>";

    public static final String SCRIPT_START = "<script>";
    public static final String SCRIPT_END = "</script>";

    public static final String H1_TAG_START = "<h1>";
    public static final String H1_TAG_END = "</h1>";
    
    public static final String CENTER_TAG_START = "<center>";
    public static final String CENTER_TAG_END = "</center>";

    public static final String BR_TAG = "</br>";


    public static final String[] CSS_TAG = 
    {
        "<style type=\"text/css\">\n",
        "body\n",
        "{\n",
        "   line-height: 1.6em;\n",
        "}\n",
        "#hor-minimalist-b\n",
        "{\n",
        "	font-family: \"Lucida Sans Unicode\", \"Lucida Grande\", Sans-Serif;\n",
        "	font-size: 12px;\n",
        "	background: #fff;\n",
        "	margin: 45px;\n",
        "	margin-left: auto;\n",
        "   margin-right: auto;\n",
        "	width: 80%;\n",
        "	border-collapse: collapse;\n",
        "	text-align: center;\n",
        "}\n",
        "#hor-minimalist-b caption\n",  
        "{\n",
        "   padding: 0 0 5px 0;\n",
        "   font: bold 12px \"Trebuchet MS\", Verdana, Arial, Helvetica, Sans-serif;\n",
        "   text-align: center;\n",
        "}\n",
        "#hor-minimalist-b th\n",
        "{\n",
        "	font-size: 14px;\n",
        "	font-weight: normal;\n",
        "	color: #039;\n",
        "	padding: 10px 8px;\n",
        "	border-bottom: 2px solid #6678b1;\n",
        "}\n",
        "#hor-minimalist-b td\n",
        "{\n",
//		"   border-right: 1px solid #C1DAD7;",
//		"   border-left: 1px solid #C1DAD7;",
        "   border-bottom: 1px solid #ccc;\n",
        "   color: #669;\n",
        "   padding: 6px 8px;\n",
        "}\n",
        "#hor-minimalist-b tbody tr:hover td\n",
        "{\n",
        "	color: #009;\n",
        "}\n",
        "</style>\n"
    };
    
    
    public static final String[] NEWSPAPER_CSS_TAG = 
    {
        "<style type=\"text/css\">\n",
        "#newspaper-b\n",
        "{\n",
	"   font-family: \"Lucida Sans Unicode\", \"Lucida Grande\", Sans-Serif;\n",
	"   font-size: 12px;\n",
	"   margin: 0px;\n",
	"   margin-left: auto;\n",
        "   margin-right: auto;\n",
	"   text-align: left;\n",
	"   border-collapse: collapse;\n",
	"   border: 1px solid #69c;\n",
	"   margin-left: auto;\n",
        "   margin-right: auto;\n",
        "}\n",
        "#newspaper-b th\n",
        "{\n",
	"   padding: 15px 10px 10px 10px;\n",
	"   font-weight: normal;\n",
	"   font-size: 14px;\n",
	"   color: #039;\n",
        "}\n",
        "#newspaper-b tbody\n",
        "{\n",
	"   background: #e8edff;\n",
        "}\n",
        "#newspaper-b td\n",
        "{\n",
	"   padding: 10px;\n",
	"   color: #669;\n",
	"   border-top: 1px dashed #fff;\n",
        "}\n",
        "#newspaper-b tbody tr:hover td\n",
        "{\n",
	"   color: #339;\n",
	"   background: #d0dafd;\n",
        "}\n",
        "</style>\n"
    };
    
    public static final String[] HEADER_CSS_TAG = 
    {
        "<style type=\"text/css\">\n",
        "H1 {\n",
	"   font-family: \"Lucida Sans Unicode\", \"Lucida Grande\", Sans-Serif;\n",
	"   font-size: 18px;\n",
	"   font-weight: bold;\n",
	"   color: #006600;\n",
	"   letter-spacing: 1.4px;\n",
	"   border-bottom: solid 1px #006600;\n",
	"   text-transform: uppercase;\n",
        "}\n\n",

        "H2 {\n",
	"   font-family: \"Lucida Sans Unicode\", \"Lucida Grande\", Sans-Serif;\n",
	"   font-size: 14px;\n",
	"   font-weight: normal;\n",
	"   letter-spacing: 1.2px;\n",
	"   color: #009900;\n",
        "}\n",
        "</style>\n"
    };
    
}