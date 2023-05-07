/*
 *   DENOPTIM
 *   Copyright (C) 2022 Marco Foscato <marco.foscato@uib.no>
 *
 *   This program is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU Affero General Public License as published
 *   by the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   This program is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU Affero General Public License for more details.
 *
 *   You should have received a copy of the GNU Affero General Public License
 *   along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package denoptim.logging;

/**
 * Identifier of a counter. A printable description is given by method
 * {@link CounterID#getDescription()}.
 */

public enum CounterID 
{
    //GENERATEDGRAPHS, 
    NEWCANDIDATEATTEMPTS, 
    
    XOVERATTEMPTS, 
    XOVERPARENTSEARCH,
    FAILEDXOVERATTEMPTS,
    FAILEDXOVERATTEMPTS_FINDPARENTS, 
    FAILEDXOVERATTEMPTS_PERFORM,
    FAILEDXOVERATTEMPTS_SETUPRINGS, 
    FAILEDXOVERATTEMPTS_EVAL, 
    FAILEDXOVERATTEMPTS_FORBENDS,
    
    MUTATTEMPTS, 
    MUTPARENTSEARCH, 
    FAILEDMUTATTEMTS,
    FAILEDMUTATTEMTS_PERFORM,
    FAILEDMUTATTEMTS_PERFORM_NOMUTSITE,
    FAILEDMUTATTEMTS_PERFORM_NOOWNER,
    FAILEDMUTATTEMTS_PERFORM_BADMUTTYPE,
    FAILEDMUTATTEMTS_PERFORM_NOCHANGEBRANCH,
    FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK,
    FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_FIND,
    FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_EDIT,
    FAILEDMUTATTEMTS_PERFORM_NODELLINK_FINDPARENT,
    FAILEDMUTATTEMTS_PERFORM_NODELLINK_EDIT,
    FAILEDMUTATTEMTS_PERFORM_NODELETECHAIN,
    FAILEDMUTATTEMTS_PERFORM_NOADDLINK,
    FAILEDMUTATTEMTS_PERFORM_NOADDLINK_FIND,
    FAILEDMUTATTEMTS_PERFORM_NOADDLINK_EDIT,
    FAILEDMUTATTEMTS_PERFORM_NOEXTEND,
    FAILEDMUTATTEMTS_PERFORM_NOADDRING,
    FAILEDMUTATTEMTS_PERFORM_NODELETE,
    FAILEDMUTATTEMTS_SETUPRINGS, 
    FAILEDMUTATTEMTS_EVAL, 
    FAILEDMUTATTEMTS_FORBENDS,
    
    BUILDANEWATTEMPTS, 
    FAILEDBUILDATTEMPTS, 
    FAILEDBUILDATTEMPTS_GRAPHBUILD,
    FAILEDBUILDATTEMPTS_EVAL, 
    FAILEDBUILDATTEMPTS_SETUPRINGS, 
    FAILEDBUILDATTEMPTS_FORBIDENDS,
    
    MANUALADDATTEMPTS,
    FAILEDMANUALADDATTEMPTS, 
    FAILEDMANUALADDATTEMPTS_EVAL, 
    
    CONVERTBYFRAGATTEMPTS,
    FAILEDCONVERTBYFRAGATTEMPTS,
    FAILEDCONVERTBYFRAGATTEMPTS_FRAGMENTATION,
    FAILEDCONVERTBYFRAGATTEMPTS_EVAL,
    FAILEDCONVERTBYFRAGATTEMPTS_TMPLEMBEDDING,
    
    FITNESSEVALS, FAILEDFITNESSEVALS,
    
    DUPLICATEPREFITNESS,
    FAILEDDUPLICATEPREFITNESSDETECTION;
    
    private String description = "";
    
    static {
        //GENERATEDGRAPHS.description = "Graphs generated";
        
        NEWCANDIDATEATTEMPTS.description = "Attempts to generate a new "
                + "candidate";
        
        XOVERATTEMPTS.description = "Attempts to build graph by crossover";
        FAILEDXOVERATTEMPTS.description = "Failed attempts to do build a graph "
                + "by crossover";
        XOVERPARENTSEARCH.description = "Attempts to find a pairs of parents "
                + "compatible with crossover";
        FAILEDXOVERATTEMPTS_FINDPARENTS.description = "Failed attemtps to find "
                + "crossover partners";
        FAILEDXOVERATTEMPTS_PERFORM.description = "Failed crossover operations "
                + "on compatible parents";
        FAILEDXOVERATTEMPTS_SETUPRINGS.description = "Failed attempts to setup "
                + "rings in a crossover offspring";
        FAILEDXOVERATTEMPTS_EVAL.description = "Failed attempt to pass graph "
                + "evaluation test from crossover offspring";
        FAILEDXOVERATTEMPTS_FORBENDS.description = "Crossover offsprings that "
                + "let to forbidden ends";
        
        MUTATTEMPTS.description = "Attempts to do build graph by mutation";
        FAILEDMUTATTEMTS.description = "Failed attempts to do build a graph "
                + "by Mutation";
        MUTPARENTSEARCH.description = "Attempts to find a parent that supports "
                + " mutation";
        FAILEDMUTATTEMTS_PERFORM.description = "Failed mutation operation of "
                + "parent "
                + "that supports mutation";
        FAILEDMUTATTEMTS_PERFORM_NOMUTSITE.description = "Mutation cannot be "
                + "done because graph declares no mutation site";
        FAILEDMUTATTEMTS_PERFORM_NOOWNER.description = "Mutation cannot take "
                + "place on a vertex that has no owner";
        FAILEDMUTATTEMTS_PERFORM_BADMUTTYPE.description = "Mutation type is "
                + "not availaable on the requested vertex";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGEBRANCH.description = "Mutation did "
                + "not replace the branch of a graph";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK.description = "Mutation did not "
                + "replace a vertex in a chain";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_FIND.description = "Failed to "
                + "find an alternative link vertex";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_EDIT.description = "Failed to "
                + "replace old link with new one";
        FAILEDMUTATTEMTS_PERFORM_NODELLINK_FINDPARENT.description = "Failed to "
                + "identify the parent of a link selected for removal.";
        FAILEDMUTATTEMTS_PERFORM_NODELLINK_EDIT.description = "Failed to "
                + "remove vertex and weld remaining parts";
        FAILEDMUTATTEMTS_PERFORM_NODELETECHAIN.description = "Failed to "
                + "remove a chain of vertexes";
        FAILEDMUTATTEMTS_PERFORM_NOADDLINK.description = "Mutation did not "
                + "introduce a verted between a pairs of previously "
                + "connected vertexes";
        FAILEDMUTATTEMTS_PERFORM_NOADDLINK_FIND.description = "Failed to "
                + "find an linking vertex";
        FAILEDMUTATTEMTS_PERFORM_NOADDLINK_EDIT.description = "Failed to "
                + "replace edge with with new vertex and edges";
        FAILEDMUTATTEMTS_PERFORM_NOEXTEND.description = "Mutation did not "
                + "extend the graph";
        FAILEDMUTATTEMTS_PERFORM_NOADDRING.description = "Mutation did not "
                + "close a ring in the graph";
        FAILEDMUTATTEMTS_PERFORM_NODELETE.description = "Mutation did not "
                + "delete vertex";
        FAILEDMUTATTEMTS_SETUPRINGS.description = "Failed attempts to setup "
                + "rings "
                + "in a mutated offspring";
        FAILEDMUTATTEMTS_EVAL.description = "Failed attempt to pass graph "
                + "evaluation test from mutated offspring";
        FAILEDMUTATTEMTS_FORBENDS.description = "Mutated offsprings that led "
                + "to forbidden ends";
        
        BUILDANEWATTEMPTS.description = "Attempts to do build graph from "
                + "scratch";
        FAILEDBUILDATTEMPTS.description = "Failed attempts to do build a graph "
                + "from scratch";
        FAILEDBUILDATTEMPTS_GRAPHBUILD.description = "Failed attempts to "
                + "generate a graph";
        FAILEDBUILDATTEMPTS_EVAL.description = "Failed attempts to pass graph "
                + "evaluation test from newly built graphs";
        FAILEDBUILDATTEMPTS_SETUPRINGS.description = "Failed attempt to setup "
                + "rings in a newly generated graph";
        FAILEDBUILDATTEMPTS_FORBIDENDS.description = "Construction of new "
                + "graphs that led to forbidden ends";
        
        MANUALADDATTEMPTS.description = "Number of attempts to provide a "
                + "manually built candidate";
        FAILEDMANUALADDATTEMPTS.description = "Failed attempts to import "
                + "a manually built cadidate";
        FAILEDMANUALADDATTEMPTS_EVAL.description = "Failed attempts to pass "
                + "graph evaluation test from manually added candidates";
        
        CONVERTBYFRAGATTEMPTS.description = "Number of attempts to import a "
                + "candidate by converting a given molecule into a graph";
        FAILEDCONVERTBYFRAGATTEMPTS.description = "Failed attempts to import "
                + "a cadidate by converting a given molecule into a graph";
        FAILEDCONVERTBYFRAGATTEMPTS_FRAGMENTATION.description = "Failed "
                + "attempts to do fragmentation while generating a candidate "
                + "by conversion of molecules to graphs";
        FAILEDCONVERTBYFRAGATTEMPTS_EVAL.description = "Failed attempts to "
                + "pass graph evaluation test from candidates imported from "
                + "conversion of molecules to graphs";
        FAILEDCONVERTBYFRAGATTEMPTS_TMPLEMBEDDING.description = "Failed "
                + "attempts to "
                + "embedd patterns in templates while generating candidates "
                + "by conversion of molecules to graphs";
        
        FITNESSEVALS.description = "Number of fitness evaluations";
        FAILEDFITNESSEVALS.description = "Number of failed fitness evaluations";
        
        DUPLICATEPREFITNESS.description = "Number of duplicate candidates "
                + " detected prior to considering their fitness evaluation";
        FAILEDDUPLICATEPREFITNESSDETECTION.description = "Number of failed "
                + "attempts to compare UID with known UIDs prior to considering "
                + "the fitness evaluation of a candidate";
    }
    
    private String prettyName = "";
    
    static {
        NEWCANDIDATEATTEMPTS.prettyName =
                "#Attempts New Candidate";

        XOVERATTEMPTS.prettyName =
                "#Xover";
        XOVERPARENTSEARCH.prettyName =
                "#Xover Parents Search";
        FAILEDXOVERATTEMPTS.prettyName =
                "#Failed Xovers";
        FAILEDXOVERATTEMPTS_FINDPARENTS.prettyName =
                "#Failed Xovers_Find Parents";
        FAILEDXOVERATTEMPTS_PERFORM.prettyName =
                "#Failed Xovers_Perform";
        FAILEDXOVERATTEMPTS_SETUPRINGS.prettyName =
                "#Failed Xovers_Setup Rings";
        FAILEDXOVERATTEMPTS_EVAL.prettyName =
                "#Failed Xovers_Graph Filter";
        FAILEDXOVERATTEMPTS_FORBENDS.prettyName =
                "#Failed Xovers_Forbidden Ends";

        MUTATTEMPTS.prettyName =
                "#Mutation";
        MUTPARENTSEARCH.prettyName =
                "#Mutation Parent Search";
        FAILEDMUTATTEMTS.prettyName =
                "#Failed Mutation";
        FAILEDMUTATTEMTS_PERFORM.prettyName =
                "#Failed Mut_Perform";
        FAILEDMUTATTEMTS_PERFORM_NOMUTSITE.prettyName =
                "#Failed Mut_noMutSite";
        FAILEDMUTATTEMTS_PERFORM_NOOWNER.prettyName =
                "#Failed Mut_noOwner";
        FAILEDMUTATTEMTS_PERFORM_BADMUTTYPE.prettyName =
                "#Failed Mut_bad Mut Type";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGEBRANCH.prettyName =
                "#Failed Mut Change Branch";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK.prettyName =
                "#Failed Mut Change Link";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_FIND.prettyName =
                "#Failed Mut Change Link_Find";
        FAILEDMUTATTEMTS_PERFORM_NOCHANGELINK_EDIT.prettyName =
                "#Failed Mut Change Link_Edit";
        FAILEDMUTATTEMTS_PERFORM_NODELLINK_FINDPARENT.prettyName =
                "#Failed Mut Delete Link_Find Parent";
        FAILEDMUTATTEMTS_PERFORM_NODELLINK_EDIT.prettyName =
                "#Failed Mut Delete Link_Edit";
        FAILEDMUTATTEMTS_PERFORM_NODELETECHAIN.prettyName =
                "#Failed Mut Delete Chain";
        FAILEDMUTATTEMTS_PERFORM_NOADDLINK.prettyName =
                "#Failed Mut Add Link";
        FAILEDMUTATTEMTS_PERFORM_NOADDLINK_FIND.prettyName =
                "#Failed Mut Add Link_Find";
        FAILEDMUTATTEMTS_PERFORM_NOADDLINK_EDIT.prettyName =
                "#Failed Mut Add Link_Edit";
        FAILEDMUTATTEMTS_PERFORM_NOEXTEND.prettyName =
                "#Failed Mut Extend";
        FAILEDMUTATTEMTS_PERFORM_NOADDRING.prettyName =
                "#Failed Mut AddRing";
        FAILEDMUTATTEMTS_PERFORM_NODELETE.prettyName =
                "#Failed Mut Delete";
        FAILEDMUTATTEMTS_SETUPRINGS.prettyName =
                "#Failed Mut_Setup Rings";
        FAILEDMUTATTEMTS_EVAL.prettyName =
                "#Failed Mut_Graph Filter";
        FAILEDMUTATTEMTS_FORBENDS.prettyName =
                "#Failed Mut_Forbidden Ends";

        BUILDANEWATTEMPTS.prettyName =
                "#Build Anew";
        FAILEDBUILDATTEMPTS.prettyName =
                "#Failed Build Anew";
        FAILEDBUILDATTEMPTS_GRAPHBUILD.prettyName =
                "#FailedBuild_GraphBuild";
        FAILEDBUILDATTEMPTS_EVAL.prettyName =
                "#FailedBuild_Graph Filter";
        FAILEDBUILDATTEMPTS_SETUPRINGS.prettyName =
                "#FailedBuild_Setup Rings";
        FAILEDBUILDATTEMPTS_FORBIDENDS.prettyName =
                "#FailedBuild_Forbidden Ends";

        MANUALADDATTEMPTS.prettyName =
                "#Manual Add";
        FAILEDMANUALADDATTEMPTS.prettyName =
                "#Failed Manual Add";
        FAILEDMANUALADDATTEMPTS_EVAL.prettyName =
                "#Failed Manual Add_Eval";
        
        CONVERTBYFRAGATTEMPTS.prettyName =
                "#MolToGraph Add";
        FAILEDCONVERTBYFRAGATTEMPTS.prettyName =
                "#Failed MolToGraph Add";
        FAILEDCONVERTBYFRAGATTEMPTS_FRAGMENTATION.prettyName =
                "#Failed MolToGraph Add_Frag";
        FAILEDCONVERTBYFRAGATTEMPTS_EVAL.prettyName =
                "#Failed MolToGraph Add_Eval";
        FAILEDCONVERTBYFRAGATTEMPTS_TMPLEMBEDDING.prettyName =
                "#Failed MolToGraph Add_TmplEmbed";

        FITNESSEVALS.prettyName =
                "#Fitness";
        FAILEDFITNESSEVALS.prettyName =
                "#Fitness_Failed Fitness Eval.";

        DUPLICATEPREFITNESS.prettyName =
                "#Duplicates Pre-Fitness";
        FAILEDDUPLICATEPREFITNESSDETECTION.prettyName =
                "#Failed Duplicate Pre-Fitness Detection";
    }

//------------------------------------------------------------------------------
    
    /**
     * @return a printable description of what this counter identifier 
     * refers to.
     */
    public String getDescription()
    {
        return description;
    }
    
//------------------------------------------------------------------------------
    
    /**
     * Returns a string representing the mane of this counter in a way that is
     * pretty enough to be shown in user manuals or graphical interfaces
     * @return a nice-looking name of this counter
     */
    public String getPrettyName()
    {
        return prettyName;
    }
    
//------------------------------------------------------------------------------
      
}
