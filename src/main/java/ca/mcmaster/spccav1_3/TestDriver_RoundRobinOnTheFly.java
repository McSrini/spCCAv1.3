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
import static ca.mcmaster.spccav1_3.cplex.NodeSelectionStartegyEnum.STRICT_BEST_FIRST;
import ca.mcmaster.spccav1_3.cplex.datatypes.SolutionVector;
import java.io.File;
import static java.lang.System.exit;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.log4j.*;
  

/**
 *
 * @author tamvadss
 * 
 * run b2c1s1 until it grows to 10 thousand leafs
 * Then get CCA nodes with 500 leafs each
 * Some of them are selected, the others are controlled branched
 */
public class TestDriver_RoundRobinOnTheFly {
    
    private static  Logger logger = null;
    
    public static void main(String[] args) throws Exception {
            
        logger=Logger.getLogger(TestDriver_RoundRobinOnTheFly.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_RoundRobinOnTheFly.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
        
         
        //TEST 1 - solve b2c1s1 with backtrack =false to ten-thousand leafs
        MPS_FILE_ON_DISK =  "F:\\temporary files here\\atlanta-ip.mps";
        BackTrack=false;
        BAD_MIGRATION_CANDIDATES_DURING_TESTING = new ArrayList<String>();
        ActiveSubtree activeSubtree = new ActiveSubtree () ;
        activeSubtree.solve( 600, PLUS_INFINITY, MILLION, true, false); //500 50 atalanta-ip
        
        logger.debug ("TEST 2 - print CCA nodes having approx 250 good leafs") ;       
        //TEST 2 - print CCA nodes having approx 250 good leafs
        List<CCANode> candidateCCANodes =activeSubtree.getCandidateCCANodes(   80 ); ///2000  150
        for (CCANode ccaNode :candidateCCANodes ){
            logger.debug (ccaNode) ;              
        }
        
        //we have about 20 CCA nodes, select some for controlled branching
        for (CCANode ccaNode :candidateCCANodes ){
            if (ccaNode.getPackingFactor() >= TWO) {
                logger.debug (""+ccaNode.nodeID + " has bad packing factor " +ccaNode.getPackingFactor() + " and prune list size " + ccaNode.pruneList.size() ) ;       
            }               
        }
        for (CCANode ccaNode :candidateCCANodes ){
            if (ccaNode.getPackingFactor() < TWO) {
                logger.debug (""+ccaNode.nodeID + " has good packing factor " +ccaNode.getPackingFactor() + " and prune list size " + ccaNode.pruneList.size() ) ;       
            }               
        }
       
        //find the best known solution after ramp up
        SolutionVector bestKnownSolution = null;
        if (activeSubtree.isFeasible()) {
            bestKnownSolution =             activeSubtree.getSolutionVector();
            logger.debug("best known solution after ramp up is "+ activeSubtree.getObjectiveValue()) ;
        } else {
            logger.debug("NO known solution after ramp up   " ) ;
        }
        
        
        
        //now solve each CCA node , and then its component leafs in a round robin fashion, then compare which is more effective
        for (CCANode ccaNode :candidateCCANodes ){
            
            Instant startTime = Instant.now();
            logger.debug (""+ccaNode.nodeID + " straight solve started for node " );
            ActiveSubtree treeStraight = new ActiveSubtree() ;
            treeStraight.mergeVarBounds(ccaNode, activeSubtree.instructionsFromOriginalMip, true );
            if (bestKnownSolution!=null) treeStraight.setMIPStart(bestKnownSolution);
            treeStraight.simpleSolve(-ONE, false, false, null);
            double straightSolveTimeTakenInMinutes = Duration.between( startTime, Instant.now()).toMinutes();
            logger.debug (""+ccaNode.nodeID + " straight solve ended for node in minutes " +  straightSolveTimeTakenInMinutes);
            if (treeStraight.isFeasible()|| treeStraight.isOptimal()) logger.debug (" cca node straight solve solution is "+treeStraight.getObjectiveValue());
            treeStraight.end();
            if(isHaltFilePresent())  exit(ONE);
             
             
            //now solve included leafs in round robin fashion for the same time
            //note ending # of leafs left and MIP gap
            List<CCANode> ccaNodeList = activeSubtree.getActiveLeafsAsCCANodes( ccaNode.pruneList);        
            ActiveSubtreeCollection astc = new ActiveSubtreeCollection (ccaNodeList, activeSubtree.instructionsFromOriginalMip, -ONE, false, 0) ;
            if (bestKnownSolution!=null) astc.setMIPStart(  bestKnownSolution);
            logger.debug("Started active subtree collection solve for leafs under " +ccaNode.nodeID );
            logger.debug("Starting Mip gap percentage is " + astc.getRelativeMIPGapPercent());
            astc.solve (true,  TEN*straightSolveTimeTakenInMinutes , false, TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE, STRICT_BEST_FIRST);
            logger.debug("Ended active subtree collection solve for leafs under " +ccaNode.nodeID );
            logger.debug("Mip gap reamining percentage is " + astc.getRelativeMIPGapPercent());
            //logger.debug("Count of nodes reamining is " + astc.getNumActiveLeafs());
            logger.debug("Count of trees reamining is " + astc.getNumTrees() + " and raw nodes reamining is "+ astc.getPendingRawNodeCount());
            /*if(astc.isCollectionFeasibleOrOptimal())*/ logger.debug("Collection best known soln is  "+astc.getIncumbentValue());
            astc.endAll();
             
            if(isHaltFilePresent())  exit(ONE);
            
        }
        
        
    } //end main
        
    private static boolean isHaltFilePresent (){
        File file = new File("F:\\temporary files here\\haltfile.txt");
         
        return file.exists();
    }
}
