/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3;

import static ca.mcmaster.spccav1_3.Constants.*;
import ca.mcmaster.spccav1_3.cb.CBInstructionTree;
import ca.mcmaster.spccav1_3.cca.CCANode;
import ca.mcmaster.spccav1_3.cplex.ActiveSubtree;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.*;
 

/**
 *
 * @author tamvadss
 * 
 * rdplu empty bh 1 hr 40 min
 * rdplu my bh 
 */
public class TestDriver_CCACB_NodeGeneration {
    
    private static  Logger logger = null;
    
    public static void main(String[] args) throws Exception {
            
        logger=Logger.getLogger(TestDriver_CCACB_NodeGeneration.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_CCACB_NodeGeneration.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
        
        //ActiveSubtree activeSubtreeSimple = new ActiveSubtree () ;
        //activeSubtreeSimple.simpleSolve();
        
        ActiveSubtree activeSubtree = new ActiveSubtree () ;
        activeSubtree.solve( TOTAL_LEAFS_IN_SOLUTION_TREE, PLUS_INFINITY, MILLION);
        
        List<CCANode> candidateCCANodes =activeSubtree.getCandidateCCANodes( NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE);
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
        
        //create a new active subtree wrooted at node 12
        /*logger.debug ("Creating new IloCplex rooted at node 4") ;     
        ActiveSubtree activeSubtreeFour = new ActiveSubtree () ;
        activeSubtreeFour.mergeVarBounds(candidateCCANodes.get(ZERO));        
        exit(0);*/

         
        CBInstructionTree tree = activeSubtree.getCBInstructionTree(candidateCCANodes.get(ZERO));
        
        tree.print();
        
        logger.debug ("Pruning leafs under CB tree") ;         
        activeSubtree.prune( tree.pruneList, true);
        
        //create new tree using CB instructions
        logger.debug ("create new tree using CB instructions") ;         
        ActiveSubtree activeSubtreeNew = new ActiveSubtree () ;
        activeSubtreeNew.mergeVarBounds(candidateCCANodes.get(ZERO), activeSubtree.instructionsFromOriginalMip);
        activeSubtreeNew.reincarnate( tree.asMap(),candidateCCANodes.get(ZERO).nodeID  , PLUS_INFINITY );
        logger.debug ("Solving node 8 reincarnated using CB instructions") ;    
        activeSubtreeNew.solve(PLUS_INFINITY,PLUS_INFINITY,TEN*TEN*TWO);
        logger.debug ("Solution for node 8 reincarnated using CB instructions" +
                 ( activeSubtreeNew.isFeasible()||activeSubtreeNew.isOptimal()? activeSubtreeNew.getObjectiveValue():-ONE) );    
        exit(0);
        
        
        candidateCCANodes = activeSubtree.getCandidateCCANodes( Arrays.asList(  "Node21", "Node22", "Node25","Node28", "Node29", "Node30"));
         for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
         

         
        tree = activeSubtree.getCBInstructionTree(candidateCCANodes.get(ZERO),Arrays.asList(  "Node21", "Node22", "Node25","Node28", "Node29", "Node30") );
        
        tree.print();
        
    } //end main
    
}
