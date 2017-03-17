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
 */
public class TestDriver {
    
    private static  Logger logger = null;
    
    public static void main(String[] args) throws Exception {
            
        logger=Logger.getLogger(TestDriver.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
        
        ActiveSubtree activeSubtree = new ActiveSubtree () ;
        activeSubtree.solve(TOTAL_LEAFS_IN_SOLUTION_TREE, PLUS_INFINITY, TEN* TWO);
        
        List<CCANode> candidateCCANodes =activeSubtree.getCandidateCCANodes( NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE);
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
         
        CBInstructionTree tree = activeSubtree.getCBTree(candidateCCANodes.get(ZERO));
        
        tree.print();
        
        logger.debug ("Pruning leafs under CB tree") ;         
        activeSubtree.prune( tree.pruneList);
        
        candidateCCANodes = activeSubtree.getCandidateCCANodes( Arrays.asList(  "Node21", "Node22", "Node25","Node28", "Node29", "Node30"));
         for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
         
        tree = activeSubtree.getCBTree(candidateCCANodes.get(ZERO),Arrays.asList(  "Node21", "Node22", "Node25","Node28", "Node29", "Node30") );
        
        tree.print();
        
    } //end main
    
}
