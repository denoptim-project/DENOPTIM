#!/bin/bash
#
# This script adds the code creating and handling a parameter.
#
# Usage:
# ./thisScript.sh
# 
#
if [ $# -lt 6 ]
then
    echo "Please give me: "
    echo " "
    echo "<name> <type> <keyword> <label> <adv> <help> <file.java>"
    echo " "
    echo " where:"
    echo " <name> is the nameroot used for the java variables"
    echo " <type> sets the type of parameter:"
    echo "        'F' for file, "
    echo "        'S' for string, "
    echo "        'P' for pre-set choice"
    echo " <key>  sets the DENOPTIM keyword"
    echo " <adv>  sets the destination of the parameter: "
    echo "        0 for standard"
    echo "        1 for advanced settings block"
    echo " <help> if the help-tip message in between double quotes"
    echo " <file.java> is the name of the java cource code file to edit"
    exit 1
fi

name="$1"
typ="$2"
key="$3"
label="$4"
adv="$5"
helpMsg="$6"
file="$7"

placeHolder="        \/\/HEREGOESIMPLEMENTATION"
targetPanel="block"
case "$adv" in
    '1')
        placeHolder="        \/\/HEREGOESADVIMPLEMENTATION"
        targetPanel="advOptsBlock"
        ;;

    '2')
        placeHolder="        \/\/HEREGOESADVIMPLEMENTATION"
        targetPanel="localBlock2"
        ;;

    '3')
        placeHolder="        \/\/HEREGOESADVIMPLEMENTATION"
        targetPanel="localBlock3"
        ;;

    '4')
        placeHolder="        \/\/HEREGOESADVIMPLEMENTATION"
        targetPanel="localBlock4"
        ;;

    '5')
        placeHolder="        \/\/HEREGOESADVIMPLEMENTATION"
        targetPanel="localBlock5"
        ;;

esac

# Read furter arguments
args=("$@")
preSets=''
if [ "$typ" == 'P' ]
then
    if [ "$#" -lt 8 ]
    then
	echo " "
	echo "Please append each option of the pre-set lint as argument at the end of the arguments list."
	echo " "
        exit -1
    fi
    for i in $(seq 8 $# )
    do
	if [ "$preSets" == '' ]
	then
	    preSets="\"${args[$i-1]}\""
	else
            preSets="${preSets}, \"${args[$i-1]}\""
	fi
    done
fi

# Prepare strings with multiple lines of Java code
case "$typ" in
    'F')
        fields="    String key$name \= \"$key\";\n    JPanel line$name;\n    JLabel lbl$name;\n    JTextField txt$name;\n    JButton btn$name;\n\n    //HEREGOFIELDS"

        code="        String toolTip$name = \"$helpMsg\";\n        line$name = new JPanel(new FlowLayout(FlowLayout.LEFT));\n        lbl$name = new JLabel(\"$label\", SwingConstants.LEFT);\n        lbl$name.setPreferredSize(fileLabelSize);\n        lbl$name.setToolTipText(toolTip$name);\n        txt$name = new JTextField();\n        txt$name.setToolTipText(toolTip$name);\n        txt$name.setPreferredSize(fileFieldSize);\n        btn$name = new JButton(\"Browse\");\n        btn$name.addActionListener(new ActionListener() {\n        public void actionPerformed(ActionEvent e) {\n                DenoptimGUIFileOpener.pickFile(txt$name);\n           }\n        });\n        line$name.add(lbl$name);\n        line$name.add(txt$name);\n        line$name.add(btn$name);\n        $targetPanel.add(line$name);\n\n$placeHolder"

        outForm="        sb.append(getStringIfNotEmpty(key$name,txt$name));\n        //HEREGOESPRINT"
	;;

    'B')
        fields="    String key$name \= \"$key\";\n    JPanel line$name;\n    JRadioButton rdb$name;\n\n    //HEREGOFIELDS"

        code="        String toolTip$name = \"$helpMsg\";\n        line$name = new JPanel(new FlowLayout(FlowLayout.LEFT));\n        rdb$name = new JRadioButton(\"$label\");\n        rdb$name.setToolTipText(toolTip$name);\n        line$name.add(rdb$name);\n        $targetPanel.add(line$name);\n\n$placeHolder"

        outForm="        sb.append(getStringIfSelected(key$name,rdb$name));\n        //HEREGOESPRINT"
        ;;

    'P')
	fields="    String key$name \= \"$key\";\n    JPanel line$name;\n    JLabel lbl$name;\n    JComboBox<String> cmb$name;\n\n    //HEREGOFIELDS"

	code="        String toolTip$name = \"$helpMsg\";\n        line$name = new JPanel(new FlowLayout(FlowLayout.LEFT));\n        lbl$name = new JLabel(\"$label\", SwingConstants.LEFT);\n        lbl$name.setPreferredSize(fileLabelSize);\n        lbl$name.setToolTipText(toolTip$name);\n        cmb$name = new JComboBox<String>(new String[] {$preSets});\n        cmb$name.setToolTipText(toolTip$name);\n        line$name.add(lbl$name);\n        line$name.add(cmb$name);\n        $targetPanel.add(line$name);\n\n$placeHolder"

        outForm="        sb.append(key$name).append(\"=\").append(cmb$name.getSelectedItem()).append(NL);\n        //HEREGOESPRINT"
	;;

    'S')
	fields="    String key$name \= \"$key\";\n    JPanel line$name;\n    JLabel lbl$name;\n    JTextField txt$name;\n\n    //HEREGOFIELDS"

	code="        String toolTip$name = \"$helpMsg\";\n        line$name = new JPanel(new FlowLayout(FlowLayout.LEFT));\n        lbl$name = new JLabel(\"$label\", SwingConstants.LEFT);\n        lbl$name.setPreferredSize(fileLabelSize);\n        lbl$name.setToolTipText(toolTip$name);\n        txt$name = new JTextField();\n        txt$name.setToolTipText(toolTip$name);\n        txt$name.setPreferredSize(strFieldSize);\n        line$name.add(lbl$name);\n        line$name.add(txt$name);\n        $targetPanel.add(line$name);\n\n$placeHolder"

	outForm="        sb.append(getStringIfNotEmpty(key$name,txt$name));;\n        //HEREGOESPRINT"
	;;

    *)
        echo "Parameter type not understood."
        exit -1
        ;;
esac

awk -v input="$fields" '{ sub(/    \/\/HEREGOFIELDS/, input) }1' "$file" > /tmp/HGHGHG
awk -v input="$code" -v ph="$placeHolder"  '{ sub(ph, input) }1' /tmp/HGHGHG > "$file"
awk -v input="$outForm"   '{ sub(/        \/\/HEREGOESPRINT/, input) }1' "$file" > /tmp/HGHGHG
mv /tmp/HGHGHG "$file"


