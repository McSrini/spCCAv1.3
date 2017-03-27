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
public class TestDriver_CCACB_MergeWithSingleBranch {
    
    private static  Logger logger = null;
    
    public static void main(String[] args) throws Exception {
            
        logger=Logger.getLogger(TestDriver_CCACB_MergeWithSingleBranch.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_CCACB_MergeWithSingleBranch.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
        
        //ActiveSubtree activeSubtreeSimple = new ActiveSubtree () ;
        //activeSubtreeSimple.simpleSolve();
        
        MPS_FILE_ON_DISK =  "F:\\temporary files here\\glass4.mps";
        BackTrack=false;
        
        //TEST 1 - solve altanta-ip with backtrack =0 to > 18 leafs
        ActiveSubtree activeSubtree = new ActiveSubtree () ;
        activeSubtree.solve( TOTAL_LEAFS_IN_SOLUTION_TREE_FOR_RAMPUP, PLUS_INFINITY, MILLION, true, false);
        
        //we will solve glass4 node 8, which is known to contain the winning solution
        List<CCANode> ccaList = activeSubtree.getActiveLeafsAsCCANodes(Arrays.asList("Node8")  );
        ActiveSubtree activeSubtree8 = new ActiveSubtree () ;        
        activeSubtree8.mergeVarBounds(ccaList.get(ZERO), activeSubtree.instructionsFromOriginalMip, true );
        while (!activeSubtree8.isOptimal()){
            activeSubtree8.solve( -ONE, -ONE,THREE, false, false);
            if (activeSubtree8.isFeasible()||activeSubtree8.isOptimal()) {
                logger.debug( "incumbent is "+ activeSubtree8.getObjectiveValue());
            }else {
                logger.debug( "still l@@king ... " );
            }
            // this test passed in Total time: 6:09:32.124s
        }
        
        exit(0);
        
        logger.debug ("TEST 2 - print CCA nodes having 6 good leafs") ;       
        //TEST 2 - print CCA nodes having 6 good leafs
        List<CCANode> candidateCCANodes =activeSubtree.getCandidateCCANodes( NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE);
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
        
        logger.debug ("TEST 3 - print CCA nodes having 3 good leafs") ;       
        //TEST 3 - print CCA nodes having 3 good leafs
        candidateCCANodes =activeSubtree.getCandidateCCANodes( THREE);
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
        
        //TEST 4 - print CCA nodes having 4 good leafs
        logger.debug ("TEST 4 - print CCA nodes having 4 good leafs") ;       
        candidateCCANodes =activeSubtree.getCandidateCCANodes( FOUR);
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
        

        
        //TEST 6 - create a new Active subtree with this CCA node, and solve for some time
        /*logger.debug ("TEST 6 ") ; 
        ActiveSubtree activeSubtreeT6 = new ActiveSubtree () ;
        activeSubtreeT6.mergeVarBounds(candidateCCANodes.get(ZERO), activeSubtree.instructionsFromOriginalMip);  
        activeSubtreeT6.solve( TWO*TWO*THREE, PLUS_INFINITY, TEN, false);*/
        
        //test 7 
        logger.debug ("TEST 7 ") ; 
        candidateCCANodes =activeSubtree.getCandidateCCANodes( FOUR);
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
        ActiveSubtree activeSubtreeT7 = new ActiveSubtree () ;
        activeSubtreeT7.mergeVarBounds(candidateCCANodes.get(ZERO), activeSubtree.instructionsFromOriginalMip, false);  
        //solve it for some time
        //activeSubtreeT7.solve( TWO*TWO*THREE, PLUS_INFINITY, TEN, false);
        
        logger.debug ("CCA TESTS COMPLETE, now test CB ") ; 
        
                
        //TEST 5 - print CCA nodes having 8 good leafs, CCA_TOLERANCE_FRACTION = 0.15
        logger.debug ("TEST 5 - print CCA nodes having 8 good leafs and CCA_TOLERANCE_FRACTION =0.15") ; 
        CCA_TOLERANCE_FRACTION= 0.15;
        candidateCCANodes =activeSubtree.getCandidateCCANodes( TWO*FOUR);
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
        
        //create a new active subtree rooted at node 12
        /*logger.debug ("Creating new IloCplex rooted at node 4") ;     
        ActiveSubtree activeSubtreeFour = new ActiveSubtree () ;
        activeSubtreeFour.mergeVarBounds(candidateCCANodes.get(ZERO));        
        exit(0);*/

         
        CBInstructionTree tree = activeSubtree.getCBInstructionTree(candidateCCANodes.get(ZERO));
         
        logger.debug ("Printing controlled branching instructions") ; 
        tree.print();
        
       
        
        logger.debug ("Pruning leafs under CB tree") ;         
        activeSubtree.prune( tree.pruneList, true);
        
         //TEST 8        
        //create new tree using CB instructions
        logger.debug ("create new tree using CB instructions") ;         
        ActiveSubtree activeSubtreeNew = new ActiveSubtree () ;
        activeSubtreeNew.mergeVarBounds(candidateCCANodes.get(ZERO), activeSubtree.instructionsFromOriginalMip, false);
        //here is the call that initiates controlled branching
        logger.debug ("Reincarnating node 8 using CB instructions") ;  
        activeSubtreeNew.reincarnate( tree.asMap(),candidateCCANodes.get(ZERO).nodeID  , PLUS_INFINITY , false);
        logger.debug ("Solving reincarnated node  ") ;  
        activeSubtreeNew.solve(PLUS_INFINITY,PLUS_INFINITY,MILLION, false, false);
        logger.debug ("Solution for node 8 reincarnated using CB instructions" +
                 ( activeSubtreeNew.isFeasible()||activeSubtreeNew.isOptimal()? activeSubtreeNew.getObjectiveValue():-ONE) );    
        
         
        exit(0);
        
        //TEST 9 create CCA using explicit leafs
        logger.debug ("Test 9 - CCA and CB for 2 leafs" );
        candidateCCANodes = activeSubtree.getCandidateCCANodes( Arrays.asList(  "Node27", "Node33"));
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
        tree = activeSubtree.getCBInstructionTree(candidateCCANodes.get(ZERO),Arrays.asList(  "Node27", "Node33") );
        tree.print();
          
        
    } //end main
    
}
