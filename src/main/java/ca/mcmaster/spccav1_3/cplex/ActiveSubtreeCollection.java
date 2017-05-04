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
import ca.mcmaster.spccav1_3.cplex.datatypes.SolutionVector;
import ilog.concert.IloException;
import ilog.cplex.IloCplex.Status;
import java.io.File;
import static java.lang.System.exit;
import java.time.Duration;
import java.time.Instant;
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
        
    private List<ActiveSubtree> activeSubTreeList = new ArrayList<ActiveSubtree>();
    private List<CCANode> rawNodeList = new ArrayList<CCANode>();
    //record the branching instructions required to arrive at the subtree root node under which these CCA nodes lie
    //To promote a CCA node into an IloCplex, we need to apply these branching conditions and then the CCA branching conditions
    // the call is activeSubtree.mergeVarBounds(ccaNode,  instructionsFromOriginalMip, true);  
    private  List<BranchingInstruction> instructionsFromOriginalMIP ;
    
    private double incumbentValue= IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    private SolutionVector incumbentSolution = null;
    
    //astc id
    private int ID;
    
    //keep track of max trees created in this collection during solution
    public    int maxTreesCreatedDuringSolution = ONE;
    
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
    
    public ActiveSubtreeCollection (List<CCANode> ccaNodeList, List<BranchingInstruction> instructionsFromOriginalMip, double cutoff, boolean useCutoff, int id) throws Exception {
        rawNodeList=ccaNodeList;
        this.instructionsFromOriginalMIP = instructionsFromOriginalMip;
        if (useCutoff) this.incumbentValue= cutoff;
        //create 1 tree
        this.promoteCCANodeIntoActiveSubtree( this.getRawNodeWithBestLPRelaxation(), false);
        
        ID=id;
    }
    
    public void setCutoff (double cutoff) {
        this.incumbentValue= cutoff;
    }
    
    public void setMIPStart(SolutionVector solutionVector) throws IloException {
        for (ActiveSubtree ast: activeSubTreeList){
            ast.setMIPStart(solutionVector);
        }        
    }
    


    //calculate MIP gap using global incumbent, which will be updated as this collection' s incumbent, and the best LP relax value
    //invoke this method only if computation has an incumbent
    public double getRelativeMIPGapPercent ()  {
        double result = -ONE;
        
        try {
            double bestInteger=this.incumbentValue;
            double bestBound = this.getBestReaminingLPRElaxValue() ;

            double relativeMIPGap =  bestBound - bestInteger ;        
            if (! IS_MAXIMIZATION)  {
                relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestInteger  ));
            } else {
                relativeMIPGap = relativeMIPGap /(EPSILON + Math.abs(bestBound));
            }

            result = Math.abs(relativeMIPGap)*HUNDRED;
        }catch (Exception ex){
            logger.error("Error calculating mipgap "+ ex.getMessage() );
        }
        
        return  result;
    }
    
    public  long getNumActiveLeafs () throws IloException {
        long count = ZERO;
         
        for (ActiveSubtree ast : this.activeSubTreeList){
            count += ast.numActiveLeafsAfterSimpleSolve;
        }
        return count;
    }
    public  long getNumActiveLeafsWithGoodLP () throws IloException {
        long count = ZERO;
         
        for (ActiveSubtree ast : this.activeSubTreeList){
            count += ast.numActiveLeafsWithGoodLPAfterSimpleSolve;
        }
        return count;
    }
        

    
    public void endAll(){
        for (ActiveSubtree ast : this.activeSubTreeList){
            ast.end();
        }
    }
     
    public void solve (boolean useSimple, double timeLimitMinutes, boolean   useEmptyCallback, int timeSlicePerTreeInMInutes ) throws Exception {
        logger.info(" \n solving ActiveSubtree Collection ... " + ID); 
        Instant startTime = Instant.now();
        
        
        while (activeSubTreeList.size()+ this.rawNodeList.size()>ZERO && Duration.between( startTime, Instant.now()).toMinutes()< timeLimitMinutes){
            
            logger.info("time in minutes left = "+ (timeLimitMinutes -Duration.between( startTime, Instant.now()).toMinutes()));
            if(isHaltFilePresent())  exit(ONE);
                        
            //pick tree with best lp
            ActiveSubtree tree = getTreeWithBestRemaining_LPValue();
            //pick raw node with best LP
            CCANode rawNode = this.getRawNodeWithBestLPRelaxation();
            //check if promotion required
            if (null != rawNode && tree !=null){
                if ((IS_MAXIMIZATION  && rawNode.lpRelaxationValue> tree.getBestRemaining_LPValue() )  || 
                    (!IS_MAXIMIZATION && rawNode.lpRelaxationValue< tree.getBestRemaining_LPValue() ) ){
                    //promotion needed
                    tree = promoteCCANodeIntoActiveSubtree(rawNode, false);
                } 
            }else if (tree ==null){
                //promotion needed
                tree = promoteCCANodeIntoActiveSubtree(rawNode, false);
            }else if (null==rawNode){
                //just solve the best tree available
            }

            //keep track of max trees created on this partition during solution
            maxTreesCreatedDuringSolution = Math.max(maxTreesCreatedDuringSolution ,  activeSubTreeList.size());
            
            
            //set best known solution, if any, as MIP start
            if (incumbentValue != MINUS_INFINITY  && incumbentValue != PLUS_INFINITY){
                if (tree.isFeasible()){
                    if (  (IS_MAXIMIZATION  && incumbentValue> tree.getObjectiveValue())  || (!IS_MAXIMIZATION && incumbentValue< tree.getObjectiveValue()) ) {                
                        //tree.setMIPStart(incumbentSolution);
                        tree.setCutoffValue( incumbentValue);
                    }
                } else{
                    //tree.setMIPStart(incumbentSolution);
                    tree.setCutoffValue( incumbentValue);
                }
            }


            if (useSimple){
                int timeSlice = (int)Math.min( timeSlicePerTreeInMInutes, Duration.between( startTime, Instant.now()).toMinutes() );
                if (timeSlice < ONE) timeSlice =ONE;
                logger.info("Solving tree seeded by cca node "+ tree.seedCCANodeID + " with " + tree.guid  + " for minutes " +  timeSlice);  
                tree.simpleSolve(timeSlice,  useEmptyCallback,  false, null);                
            } else {
                //tree.solve( -ONE,  incumbentValue ,  timeSlicePerTree , false, isCollectionFeasibleOrOptimal());
            }
            
            //update incumbent if needed            
            if (tree.isFeasible()|| tree.isOptimal()){
                double objVal =tree.getObjectiveValue();
                if ((IS_MAXIMIZATION && incumbentValue< objVal)  || (!IS_MAXIMIZATION && incumbentValue> objVal) ){
                    incumbentValue = objVal;
                    this.incumbentSolution=tree.getSolutionVector();
                    logger.info("Incumbent updated to  "+ this.incumbentValue);
                }
            }
            
            //remove   tree from list of jobs, if tree is solved to completion
            if (tree.isUnFeasible()|| tree.isOptimal()) {
                logger.info("Tree completed "+ tree.seedCCANodeID + ", " + tree.guid + ", " +   tree.getStatus()) ;
                tree.end();
                this.activeSubTreeList.remove( tree);

            }           
            logger.info("Number of trees left is "+ this.activeSubTreeList.size());  
            printStatus();
            
        }
        
        logger.info(" ActiveSubtree Collection solved to completion "+ID );
    }
        
    public double getIncumbentValue (){
        return new Double (this.incumbentValue);
    }
    
    
    
    public long getPendingRawNodeCount (){
        return this.rawNodeList.size();
    }
    
     
    
    public int getNumTrees() {
        return  activeSubTreeList.size();
    }
    
    private void printStatus() throws IloException {
        for (ActiveSubtree activeSubtree: this.activeSubTreeList){
            logger.debug( "Active tree " + activeSubtree.seedCCANodeID + ", " + activeSubtree.guid + ", " +   
                           activeSubtree.getStatus() +", " +activeSubtree.getBestRemaining_LPValue() );
        }
        logger.debug("Number of pending raw nodes " + getPendingRawNodeCount());
    }
    
    private double getBestReaminingLPRElaxValue () throws Exception{
        double   bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
        
        if (getTreeWithBestRemaining_LPValue()!=null) bestReamining_LPValue =getTreeWithBestRemaining_LPValue().getBestRemaining_LPValue();
        
        if(getRawNodeWithBestLPRelaxation()!=null){
            if (IS_MAXIMIZATION){
                bestReamining_LPValue = Math.max(bestReamining_LPValue, getRawNodeWithBestLPRelaxation().lpRelaxationValue) ;
            }else {
                bestReamining_LPValue = Math.min(bestReamining_LPValue, getRawNodeWithBestLPRelaxation().lpRelaxationValue) ;
            }
        }
        
        return     bestReamining_LPValue;
    }
    
    private ActiveSubtree getTreeWithBestRemaining_LPValue() throws Exception{
                                                   
        double   bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
        ActiveSubtree result = null;
        
        for (ActiveSubtree activeSubtree: this.activeSubTreeList){
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
    
    private CCANode getRawNodeWithBestLPRelaxation () {
        double   bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
        CCANode result = null;
        
        for (CCANode ccaNode : this.rawNodeList){
            if (IS_MAXIMIZATION) {
                if (bestReamining_LPValue<  ccaNode.lpRelaxationValue) {
                    result = ccaNode;
                    bestReamining_LPValue=ccaNode.lpRelaxationValue;
                }
            }else {
                if (bestReamining_LPValue>  ccaNode.lpRelaxationValue) {
                    result = ccaNode;
                    bestReamining_LPValue=ccaNode.lpRelaxationValue;
                }
            }
        }
        
        return result;
    }
    
    //remove cca node from raw node list and promote it into an active subtree.
    private ActiveSubtree promoteCCANodeIntoActiveSubtree (CCANode ccaNode, boolean useBranch) throws Exception{
        ActiveSubtree activeSubtree  = new ActiveSubtree () ;
        activeSubtree.mergeVarBounds(ccaNode,  this.instructionsFromOriginalMIP, useBranch);  
        activeSubTreeList.add(activeSubtree);      
        this.rawNodeList.remove( ccaNode);
        logger.debug ("promoted raw node "+ ccaNode.nodeID +" into tree"+ activeSubtree.guid) ;
        return activeSubtree;
    }
   
    private static boolean isHaltFilePresent (){
        File file = new File("F:\\temporary files here\\haltfile.txt");
         
        return file.exists();
    }
}
