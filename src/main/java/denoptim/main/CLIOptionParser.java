package denoptim.main;


import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * A command line parser that allows to have a single arguments without option 
 * name. For example, it allows to call the program as:
 * <pre>program input/file.dat</pre>
 * besides the normal option-controlled behavior:
 * <pre>program -f input/file.dat</pre>
 * It also allows to omit the option name for the input-defining option, as long 
 * as its argument is the first. For example:
 * <pre>program -f input/file.dat -r GA</pre>
 * is equivalent to
 * <pre>program input/file.dat -r GA</pre>
 * 
 * The unlabeled argument, however, is always parsed into the command as the
 * {@link CLIOptions#input} option.
 */
public class CLIOptionParser extends DefaultParser implements CommandLineParser
{
    @Override
    public CommandLine parse(final Options options, final String[] arguments) 
            throws ParseException 
    {
        String[] editedArguments = null;
        if (arguments.length < 1) {
            return super.parse(options, arguments);
        } else if (arguments.length == 1)
        {
            if (arguments[0].startsWith("-"))
            {
                return super.parse(options, arguments);
            } else {
                editedArguments = new String[2];
                editedArguments[0] = "-" + CLIOptions.input.getOpt();
                editedArguments[1] = arguments[0];
                return super.parse(options, editedArguments);
            }
        } else {
            if (!arguments[0].startsWith("-"))
            {
                editedArguments = new String[arguments.length+1];
                editedArguments[0] = "-" + CLIOptions.input.getOpt();
                for (int i=0; i<arguments.length; i++)
                    editedArguments[i+1] = arguments[i];

                return super.parse(options, editedArguments);
            } else {
                return super.parse(options, arguments);
            }
        }
    }
}
