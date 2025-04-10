package denoptim.gui;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.JTextField;

import denoptim.fragmenter.ScaffoldingPolicy;
import denoptim.graph.Template.ContractLevel;
import denoptim.graph.Vertex.BBType;
import denoptim.programs.fragmenter.CuttingRule;
import denoptim.programs.fragmenter.FragmenterParameters;

public class MolToGraphParametersDialog extends CuttingRulesSelectionDialog
{

    /**
     * Version ID
     */
    private static final long serialVersionUID = 1L;
    
    private JPanel lineEmbTmpl;
    private JRadioButton rdbEmbTmpl;

    private JPanel lineScaffoldingPolicy;
    private JLabel lblScaffoldingPolicy;
    private JComboBox<String> cmbScaffoldingPolicy;
    private JTextField txtScaffoldingPolicy;
    
    private JPanel lineTemplateContract;
    private JLabel lblTemplateContract;
    private JComboBox<String> cmbTemplateContract;
    
//------------------------------------------------------------------------------

    public MolToGraphParametersDialog(List<CuttingRule> defaultCuttingRules,
            List<CuttingRule> customCuttingRules, boolean preselectDefault,
            Component refForPlacement, FragmenterParameters settings)
    {
        super(defaultCuttingRules, customCuttingRules, preselectDefault,
                refForPlacement, settings);

        setTitle("Settings for Converting Molecules to Graphs");
        
        appendToCentralPanel(new JSeparator());
        
        String toolTipScaffoldingPolicy = String.format("<html><body width='%1s'>"
                + "Defines the policy used to choose a vertex and make it be "
                + "the scaffold of the graph obtained by converting a "
                + "molecule.</html>", 400);
        lineScaffoldingPolicy = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblScaffoldingPolicy = new JLabel("Policy to identify the "
                + BBType.SCAFFOLD.toString().toLowerCase() 
                + " vertex: ");
        lblScaffoldingPolicy.setToolTipText(toolTipScaffoldingPolicy);
        ScaffoldingPolicy[] policies = ScaffoldingPolicy.values();
        String[] stringArr = new String[policies.length];
        for (int i=0;i<policies.length;i++) 
            stringArr[i]=policies[i].toString();
        cmbScaffoldingPolicy = new JComboBox<String>(stringArr);
        cmbScaffoldingPolicy.setToolTipText(toolTipScaffoldingPolicy);
        cmbScaffoldingPolicy.setSelectedItem(ScaffoldingPolicy.LARGEST_FRAGMENT);
        cmbScaffoldingPolicy.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (cmbScaffoldingPolicy.getSelectedItem().toString().equals(
                        ScaffoldingPolicy.ELEMENT.toString()))
                {
                    txtScaffoldingPolicy.setEnabled(true);
                    txtScaffoldingPolicy.setEditable(true);
                } else {
                    txtScaffoldingPolicy.setEnabled(false);
                    txtScaffoldingPolicy.setEditable(false);
                }
            }
        });
        txtScaffoldingPolicy = new JTextField("Specify label here...");
        txtScaffoldingPolicy.setPreferredSize(strFieldSize);
        txtScaffoldingPolicy.setToolTipText(String.format(
                "<html><body width='%1s'>"
                + "Use this field to provide any additional string that is "
                + "needed to characterized some type of policy.</html>", 400));
        txtScaffoldingPolicy.setEnabled(false);
        txtScaffoldingPolicy.setEditable(false);
        lineScaffoldingPolicy.setToolTipText(toolTipScaffoldingPolicy);
        lineScaffoldingPolicy.add(lblScaffoldingPolicy);
        lineScaffoldingPolicy.add(cmbScaffoldingPolicy);
        lineScaffoldingPolicy.add(txtScaffoldingPolicy);
        appendToCentralPanel(lineScaffoldingPolicy);
        
        String toolTipEmbTmpl = String.format("<html><body width='%1s'>"
                + "Require converting subgraphs that define ring systems into "
                + "Templates that embed the subgraph of an entire ring syste. "
                + "Ring systems that are separated by at least one acyclic "
                + "edge are mbedded into separate Templates.</html>", 400);
        lineEmbTmpl = new JPanel(new FlowLayout(FlowLayout.LEFT));
        rdbEmbTmpl = new JRadioButton("Embed rings into Templates");
        rdbEmbTmpl.setToolTipText(toolTipEmbTmpl);
        rdbEmbTmpl.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                if (rdbEmbTmpl.isSelected())
                    cmbTemplateContract.setEnabled(true);
                else
                    cmbTemplateContract.setEnabled(false);
            }
        });
        lineEmbTmpl.setToolTipText(toolTipEmbTmpl);
        lineEmbTmpl.add(rdbEmbTmpl);
        appendToCentralPanel(lineEmbTmpl);
        
        String toolTipTemplateContract = String.format("<html><body width='%1s'>"
                + "Defines the constraints of the templates used to embed ring"
                + "systems. </html>", 400);
        lineTemplateContract = new JPanel(new FlowLayout(FlowLayout.LEFT));
        lblTemplateContract = new JLabel(
                "Contract for ring-embedding Templates: ");
        lblTemplateContract.setToolTipText(toolTipTemplateContract);
        ContractLevel[] contracts = ContractLevel.values();
        String[] stringContracts = new String[contracts.length];
        for (int i=0;i<contracts.length;i++) 
            stringContracts[i]=contracts[i].toString();
        cmbTemplateContract = new JComboBox<String>(stringContracts);
        cmbTemplateContract.setSelectedItem(ContractLevel.FREE);
        cmbTemplateContract.setToolTipText(toolTipTemplateContract);
        cmbTemplateContract.setEnabled(false);
        lineTemplateContract.setToolTipText(toolTipTemplateContract);
        lineTemplateContract.add(lblTemplateContract);
        lineTemplateContract.add(cmbTemplateContract);
        appendToCentralPanel(lineTemplateContract);
    }
    
//-----------------------------------------------------------------------------

    @Override
    protected void saveResults()
    {
        super.saveResults();
        frgParams.setEmbedRingsInTemplate(getEmbedRingsInTemplate());
        frgParams.setScaffoldingPolicy(getScaffoldingPolicy());
        frgParams.setEmbeddedRingsContract(getEmbeddedRingsContract());
    }
    
//------------------------------------------------------------------------------
    
    public boolean getEmbedRingsInTemplate()
    {
        return rdbEmbTmpl.isSelected();
    }
    
//------------------------------------------------------------------------------
    
    public ScaffoldingPolicy getScaffoldingPolicy()
    {
        ScaffoldingPolicy sp = ScaffoldingPolicy.valueOf(
                cmbScaffoldingPolicy.getSelectedItem().toString());
        sp.label = txtScaffoldingPolicy.getText();
        return sp;
    }  

//------------------------------------------------------------------------------
    
    public ContractLevel getEmbeddedRingsContract()
    {
        return ContractLevel.valueOf(
                cmbTemplateContract.getSelectedItem().toString());
    }

//------------------------------------------------------------------------------

}
