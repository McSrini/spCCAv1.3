/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cplex;

import static ca.mcmaster.spccav1_3.Constants.IS_MAXIMIZATION;
import static ca.mcmaster.spccav1_3.Constants.LOG_FILE_EXTENSION;
import static ca.mcmaster.spccav1_3.Constants.LOG_FOLDER;
import static ca.mcmaster.spccav1_3.Constants.MINUS_INFINITY;
import static ca.mcmaster.spccav1_3.Constants.ONE;
import static ca.mcmaster.spccav1_3.Constants.PLUS_INFINITY;
import static ca.mcmaster.spccav1_3.Constants.*;
import ca.mcmaster.spccav1_3.cca.CCANode;
import ca.mcmaster.spccav1_3.cplex.datatypes.BranchingInstruction;
import ilog.concert.IloException;
import static java.lang.System.exit;
import java.util.*;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 * 
 * rd-rplusc-21.mps was solved using round robin in             165395.2753
 * 
 */
public class ActiveSubtreeCollection {
    
    private static Logger logger=Logger.getLogger(ActiveSubtreeCollection.class);
        
    private List<ActiveSubtree> activeSubtreeList = new ArrayList<ActiveSubtree>();
    
    private double incumbentValue= IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtreeCollection.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
    
    public ActiveSubtreeCollection (List<CCANode> ccaNodeList, List<BranchingInstruction> instructionsFromOriginalMip, double cutoff, boolean useCutoff) throws Exception {
        for (CCANode ccaNode: ccaNodeList){
            ActiveSubtree activeSubtree  = new ActiveSubtree () ;
            activeSubtree.mergeVarBounds(ccaNode,  instructionsFromOriginalMip);  
            activeSubtreeList.add(activeSubtree);      
            if (useCutoff) activeSubtree.setCutoffValue(cutoff);
        }
        if (useCutoff) this.incumbentValue= cutoff;
    }
    
    public void solveToCompletion() throws Exception {
        logger.info("  solving ActiveSubtree Collection ... " );        
        
        while (activeSubtreeList.size()>ZERO){
            
            //pick tree with best lp
            ActiveSubtree tree = getTreeWithBestRemaining_LPValue();
            
            //solve it for some time
            logger.info("Solving tree seeded by cca node "+ tree.seedCCANodeID + " with " + tree.guid   );  
            tree.solve( -ONE,  incumbentValue , TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE  , false, isCollectionFeasibleOrOptimal());
            
            //update incumbent if needed            
            if (tree.isFeasible()|| tree.isOptimal()){
                double objVal =tree.getObjectiveValue();
                if ((IS_MAXIMIZATION && incumbentValue< objVal)  || (!IS_MAXIMIZATION && incumbentValue> objVal) ){
                    incumbentValue = objVal;
                    logger.info("Incumbent updated to  "+ this.incumbentValue);
                }
            }
            
            //remove   tree from list of jobs, if tree is solved to completion
            if (tree.isUnFeasible()|| tree.isOptimal()) {
                tree.end();
                this.activeSubtreeList.remove( tree);
                logger.info("Tree completed "+ tree.seedCCANodeID + ", " + tree.guid + ", " +   tree.getStatus()) ;
            }           
            logger.info("Number of trees left is "+ this.activeSubtreeList.size());  
            printStatus();
            
        }
        
        logger.info(" ActiveSubtree Collection solved to completion" );
    }
        
    public double getIncumbentValue (){
        return new Double (this.incumbentValue);
    }
    
    public boolean isCollectionFeasibleOrOptimal() throws IloException{
        boolean status =false;
        for (ActiveSubtree tree: activeSubtreeList){
            if (tree.isFeasible()|| tree.isOptimal()) {
                status=true;
                break;
            }
        }
        return status;
    }
    
    public int getNumTrees() {
        return  activeSubtreeList.size();
    }
    
    private void printStatus() throws IloException {
        for (ActiveSubtree activeSubtree: this.activeSubtreeList){
            logger.debug( "Active tree " + activeSubtree.seedCCANodeID + ", " + activeSubtree.guid + ", " +   
                           activeSubtree.getStatus() +", " +activeSubtree.getBestRemaining_LPValue() );
        }
    }
    
    private ActiveSubtree getTreeWithBestRemaining_LPValue() throws Exception{
                                                   
        double   bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
        ActiveSubtree result = null;
        
        for (ActiveSubtree activeSubtree: this.activeSubtreeList){
            if (IS_MAXIMIZATION) {
                if (bestReamining_LPValue<  activeSubtree.getBestRemaining_LPValue()) {
                    result = activeSubtree;
                    bestReamining_LPValue=activeSubtree.getBestRemaining_LPValue();
                }
            }else {
                if (bestReamining_LPValue>  activeSubtree.getBestRemaining_LPValue()) {
                    result = activeSubtree;
                    bestReamining_LPValue=activeSubtree.getBestRemaining_LPValue();
                }
            }
          
        }
        return result;
    }
   
}
