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
 * extract several CCA nodes and CB under them.
 * 
 */
public class TestDriver_ExtractManyCB {
    
    private static  Logger logger = null;
    
    public static void main(String[] args) throws Exception {
            
        logger=Logger.getLogger(TestDriver_ExtractManyCB.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_ExtractManyCB.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
        
        //ActiveSubtree activeSubtreeSimple = new ActiveSubtree () ;
        //activeSubtreeSimple.simpleSolve();
        
        //TEST 1 - solve altanta-ip with backtrack =0 to > 18 leafs
        ActiveSubtree activeSubtree = new ActiveSubtree () ;
        activeSubtree.solve( TOTAL_LEAFS_IN_SOLUTION_TREE_FOR_RAMPUP, PLUS_INFINITY, MILLION, true, false);
        
        logger.debug ("TEST 2 - print CCA nodes having 6 good leafs") ;       
        //TEST 2 - print CCA nodes having 6 good leafs
        List<CCANode> candidateCCANodes =activeSubtree.getCandidateCCANodes( FOUR);
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
        
       

         
        CBInstructionTree tree = activeSubtree.getCBInstructionTree(candidateCCANodes.get(ZERO));         
        logger.debug ("Printing controlled branching instructions") ; 
        tree.print();
        CBInstructionTree tree2 = activeSubtree.getCBInstructionTree(candidateCCANodes.get(ONE));         
        logger.debug ("Printing controlled branching instructions") ; 
        tree2.print();
        
       
        
        logger.debug ("Pruning leafs under CB tree") ;         
        activeSubtree.prune( tree.pruneList, true);
        
         //TEST 8        
        //create new tree using CB instructions
        logger.debug ("create new tree using CB instructions") ;         
        ActiveSubtree activeSubtreeNew = new ActiveSubtree () ;
        activeSubtreeNew.mergeVarBounds(candidateCCANodes.get(ZERO), activeSubtree.instructionsFromOriginalMip, false);
        //here is the call that initiates controlled branching
        logger.debug ("Reincarnating node 19 using CB instructions") ;  
        activeSubtreeNew.reincarnate( tree.asMap(),candidateCCANodes.get(ZERO).nodeID  , PLUS_INFINITY , false);
         
        activeSubtreeNew = new ActiveSubtree () ;
        activeSubtreeNew.mergeVarBounds(candidateCCANodes.get(ONE), activeSubtree.instructionsFromOriginalMip, false);
        //here is the call that initiates controlled branching
        logger.debug ("Reincarnating node 11 using CB instructions") ;  
        activeSubtreeNew.reincarnate( tree2.asMap(),candidateCCANodes.get(ONE).nodeID  , PLUS_INFINITY , false);
         
        
    } //end main
    
}
