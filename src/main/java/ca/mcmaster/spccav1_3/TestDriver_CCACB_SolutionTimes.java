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
public class TestDriver_CCACB_SolutionTimes {
    
    private static  Logger logger = null;
    
    public static void main(String[] args) throws Exception {
            
        logger=Logger.getLogger(TestDriver_CCACB_SolutionTimes.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_CCACB_SolutionTimes.class.getSimpleName()+ LOG_FILE_EXTENSION);
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
        
        //TEST 1
        //create node 8 by changing var bounds, and check how long it takes
        //Node 8 is known to produce the solution to the MIP
         
        List<CCANode> ccaList = activeSubtree .getCandidateCCANodes( Arrays.asList(  "Node8", "Node12", "Node18"));
        /*
        ActiveSubtree activeSubtree1 = new ActiveSubtree () ; 
        logger.debug( " Created glass4 CCA node  5 by merging var bounds " );
        activeSubtree1.mergeVarBounds(ccaList.get(ZERO), activeSubtree.instructionsFromOriginalMip, false );
        while (!activeSubtree1.isOptimal()){
            activeSubtree1.solve( -ONE, -ONE,THREE, false, false);
            if (activeSubtree1.isFeasible()||activeSubtree1.isOptimal()) {
                logger.debug( "incumbent is "+ activeSubtree1.getObjectiveValue());
            }else {
                logger.debug( "still l@@king ... " );
            }
            // this test passed in Total time: Total time:  2:47.202s
        }
        */
        
        //TEST 2
        //we will solve glass4 node 8, which is known to contain the winning solution
        //This time we merge by branching
        ActiveSubtree activeSubtreeNew = new ActiveSubtree () ;        
        activeSubtreeNew.mergeVarBounds(ccaList.get(ZERO), activeSubtree.instructionsFromOriginalMip, false );
        CBInstructionTree tree = activeSubtree.getCBInstructionTree(ccaList.get(ZERO),Arrays.asList(  "Node8", "Node12", "Node18") );
        tree.print();
        logger.debug ("Reincarnating node 8 using CB instructions") ;  
        activeSubtreeNew.reincarnate( tree.asMap(),ccaList.get(ZERO).nodeID  , PLUS_INFINITY , false);
        logger.debug ("Solving reincarnated node  ") ; 
        while (!activeSubtreeNew.isOptimal()){
            activeSubtreeNew.solve( -ONE, -ONE,THREE, false, false);
            if (activeSubtreeNew.isFeasible()||activeSubtreeNew.isOptimal()) {
                logger.debug( "incumbent is "+ activeSubtreeNew.getObjectiveValue());
            }else {
                logger.debug( "still l@@king ... " );
            }
            // this test passed in Total time: Total time: 15:23.666s with single branch
            //without single branch, seems to take much longer Total time: 1:21:14.673s
        }
        
        
         
          
        
    } //end main
    
}
