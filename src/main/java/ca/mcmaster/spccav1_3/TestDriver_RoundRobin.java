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
import ca.mcmaster.spccav1_3.cplex.ActiveSubtreeCollection;
import ca.mcmaster.spccav1_3.cplex.datatypes.BranchingInstruction;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.*;
 

/**
 *
 * @author tamvadss
 * 
 * rd-rplusc-21.mps empty bh 1 hr 40 min
 * rd-rplusc-21.mps my bh same time
 */
public class TestDriver_RoundRobin {
    
    private static  Logger logger = null;
    
    public static void main(String[] args) throws Exception {
            
        logger=Logger.getLogger(TestDriver_RoundRobin.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_RoundRobin.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
        
        //change the MIP, backtrack 
        MPS_FILE_ON_DISK =  "F:\\temporary files here\\glass4.mps"; 
        BackTrack=false;
        
        //TEST 1 - solve altanta-ip with backtrack =0 to > 18 leafs
        ActiveSubtree activeSubtree = new ActiveSubtree () ;
        activeSubtree.solve( TOTAL_LEAFS_IN_SOLUTION_TREE_FOR_RAMPUP, PLUS_INFINITY, MILLION, true, false);
        
        
        
        double cutoff =ZERO;
        boolean useCutoff = false;
        if (activeSubtree.isFeasible()){
            cutoff=activeSubtree.getObjectiveValue();
            useCutoff=true;
            logger.info("Solution after rampup is "+cutoff);
        }
        
        List<CCANode> ccaNodeList = activeSubtree.getActiveLeafsAsCCANodes(null);        
        
        ActiveSubtreeCollection astc = new ActiveSubtreeCollection (ccaNodeList, activeSubtree.instructionsFromOriginalMip, cutoff, useCutoff, 0) ;
        astc.solve (false, MILLION, false, TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE);
         
    } //end main
    
}
