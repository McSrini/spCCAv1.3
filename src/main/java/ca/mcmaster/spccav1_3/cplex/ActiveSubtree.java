/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cplex;

import static ca.mcmaster.spccav1_3.Constants.*;
import ca.mcmaster.spccav1_3.cb.CBInstructionGenerator;
import ca.mcmaster.spccav1_3.cca.CCAFinder;
import ca.mcmaster.spccav1_3.cca.CCANode;
import ca.mcmaster.spccav1_3.cb.CBInstructionTree;
import ca.mcmaster.spccav1_3.cb.ReincarnationMaps;
import ca.mcmaster.spccav1_3.cplex.callbacks.*;
import ca.mcmaster.spccav1_3.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spccav1_3.cplex.datatypes.NodeAttachment;
import static ca.mcmaster.spccav1_3.utilities.BranchHandlerUtilities.getLowerBounds;
import static ca.mcmaster.spccav1_3.utilities.BranchHandlerUtilities.getUpperBounds;
import ca.mcmaster.spccav1_3.utilities.CplexUtilities;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static ilog.cplex.IloCplex.CplexStatus.Feasible;
import static ilog.cplex.IloCplex.CplexStatus.Optimal;
import static java.lang.System.exit;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class ActiveSubtree {
        
    private static Logger logger=Logger.getLogger(ActiveSubtree.class);
        
    private IloCplex cplex   ;
    //vars in the model
    private IloNumVar[]  modelVars;
    
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    private RampUpNodeHandler rampUpNodeHandler;
    private LeafFetchingNodeHandler leafFetchNodeHandler;
    private ReincarnationNodeHandler reincarnationNodeHandler;
    private ReincarnationBranchHandler reincarnationBranchHandler;
    
    //our list of active leafs after each solve cycle
    private List<NodeAttachment> allActiveLeafs  ;     
    
    //use this object to run CCA algorithms
    private CCAFinder ccaFinder =new CCAFinder();
    
    private CBInstructionGenerator cbInstructionGenerator ;
    
    //this IloCplex object, if constructed by merging variable bounds, is differnt from the original MIP by these bounds
    //When extracting a CCA node from this Active Subtree , keep in mind that the CCA node branching instructions should be combined with these instructions
    public List<BranchingInstruction> instructionsFromOriginalMip = new ArrayList<BranchingInstruction>();
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+ActiveSubtree.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
    
    public ActiveSubtree (  ) throws Exception{
        
        this.cplex= new IloCplex();   
        cplex.importModel(MPS_FILE_ON_DISK);
        
        this.modelVars=CplexUtilities.getVariablesInModel(cplex);
        
        //create all the call back handlers
        //these are used depending on which method is invoked
        
        branchHandler = new BranchHandler(    );
        rampUpNodeHandler = new RampUpNodeHandler(    );                
        leafFetchNodeHandler = new LeafFetchingNodeHandler(); 
    
    }
    
    public void mergeVarBounds (CCANode ccaNode, List<BranchingInstruction> instructionsFromOriginalMip) throws IloException {
        List<BranchingInstruction> cumulativeInstructions = new ArrayList<BranchingInstruction>();
        cumulativeInstructions.addAll(ccaNode.branchingInstructionList);
        cumulativeInstructions.addAll(instructionsFromOriginalMip);
        this.instructionsFromOriginalMip =cumulativeInstructions;
        Map< String, Double >   lowerBounds= getLowerBounds(cumulativeInstructions);
        Map< String, Double >   upperBounds= getUpperBounds(cumulativeInstructions);
        CplexUtilities.merge(cplex, lowerBounds, upperBounds);
    }
    
    
    public boolean isFeasible () throws IloException {
        return this.cplex.getStatus().equals(Feasible);
    }
        
    public boolean isOptimal () throws IloException {
        return this.cplex.getStatus().equals(Optimal);
    }
    
    public String getStatus () throws IloException {
        return this.cplex.getStatus().toString();
    }
    
    public double getObjectiveValue() throws IloException {
        return this.cplex.getObjValue();
    }
    
    //for testing
    public void simpleSolve(int timeLimitMinutes) throws IloException{
        logger.debug("simpleSolve Started at "+LocalDateTime.now()) ;
        cplex.clearCallbacks();
        this.cplex.use(new EmptyBranchHandler());  
        setParams (  timeLimitMinutes);
        cplex.solve();
        logger.debug("simpleSolve completed at "+LocalDateTime.now()) ;
    }   
        
    public void solve(long leafCountLimit, double cutoff, int timeLimitMinutes, boolean isRampUp) throws IloException{
        
        logger.debug(" Solve begins at "+LocalDateTime.now()) ;
        
        //before solving , reset the CCA finder object which 
        //has an index built upon the current solution tree nodes
        this.ccaFinder.close();
        
        //set callbacks for regular solution
        cplex.clearCallbacks();
        this.cplex.use(branchHandler);
        
        if (isRampUp) {
            rampUpNodeHandler.setLeafCountLimit(leafCountLimit);
            this.cplex.use(rampUpNodeHandler) ;
        }  
        
        
        //setCutoff(  cutoff);
        setParams (  timeLimitMinutes);
                
        cplex.solve();
        
        //solve complete - now get the active leafs
        this.cplex.use(branchHandler);
        this.cplex.use(leafFetchNodeHandler);  
        cplex.solve();
        allActiveLeafs = leafFetchNodeHandler.allLeafs;
        
        //initialize the CCA finder
        ccaFinder .initialize(allActiveLeafs);
        
        logger.debug(" Solve concludes at "+LocalDateTime.now()) ;
        
    }
 
    //this method is used when reincarnating a tree in a controlled fashion
    //similar to solve(), but we use controlled branching instead of CPLEX default branching
    public    void reincarnate ( Map<String, CCANode> instructionTreeAsMap, String ccaRootNodeID, double cutoff) throws IloException{
        
        //reset CCA finder
        this.ccaFinder.close();
        
        //set callbacks 
        ReincarnationMaps reincarnationMaps=createReincarnationMaps(instructionTreeAsMap,ccaRootNodeID);
        this.cplex.use( new ReincarnationBranchHandler(instructionTreeAsMap,  reincarnationMaps, this.modelVars));
        this.cplex.use( new ReincarnationNodeHandler(reincarnationMaps));      
        
        //setCutoff(  cutoff);
        setParams (  MILLION);//no time limit
         
        cplex.solve();
        
        //solve complete - now get the active leafs
        //restore regular branch handler
        this.cplex.use(branchHandler);
        this.cplex.use(leafFetchNodeHandler);  
        cplex.solve();
        allActiveLeafs = leafFetchNodeHandler.allLeafs;
        
        //initialize the CCA finder
        ccaFinder .initialize(allActiveLeafs);
    }
    
    public void prune(List<String> pruneList, boolean reInitializeCCAFinder) {
        //close CCA finder
        if (reInitializeCCAFinder) this.ccaFinder.close();
        
        //update allActiveLeafs
        List<NodeAttachment> newActiveLeafs = new ArrayList<NodeAttachment> ();
        for (NodeAttachment currentLeaf : this.allActiveLeafs){
            if (!pruneList.contains(currentLeaf.nodeID) ) newActiveLeafs.add(currentLeaf);
        }
        allActiveLeafs=newActiveLeafs;
        
        //these will be removed from the IloCplex object
        this.branchHandler.pruneList.addAll(pruneList);
                
        //re-init the CCA finder
        if (reInitializeCCAFinder) ccaFinder .initialize(allActiveLeafs);
    }
    
    //if wanted leafs are not specified, every migratable leaf under this CCA is assumed to be wanted
    public CBInstructionTree getCBInstructionTree (CCANode ccaNode ) {
        List<String> wantedLeafs = new ArrayList<String> ();
        for (NodeAttachment node :  this.allActiveLeafs){
            if (ccaNode.pruneList.contains(node.nodeID) && node.isMigrateable) wantedLeafs.add(node.nodeID);
        }
        cbInstructionGenerator = new CBInstructionGenerator( ccaNode,     allActiveLeafs,   wantedLeafs) ;
        return cbInstructionGenerator.generateInstructions( );
    }
        
    public CBInstructionTree getCBInstructionTree (CCANode ccaNode, List<String> wantedLeafs) {
        
        cbInstructionGenerator = new CBInstructionGenerator( ccaNode,     allActiveLeafs,   wantedLeafs) ;
        return cbInstructionGenerator.generateInstructions( );
    }
 
    public List<CCANode> getCandidateCCANodes (List<String> wantedLeafNodeIDs)   {         
        return ccaFinder.  getCandidateCCANodes ( wantedLeafNodeIDs);       
    }
                                                                                                             
    public List<CCANode> getCandidateCCANodes (int count)   {
        return ccaFinder.  getCandidateCCANodes ( count);        
    }    
    
    public List<String> getPruneList ( CCANode ccaNode) {
        return ccaNode.pruneList;
    }
      
    public void setCutoffValue(double cutoff) throws IloException {
        if (!IS_MAXIMIZATION) {
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.UpperCutoff, cutoff);
        }else {
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.LowerCutoff, cutoff);
        }
    }
    
    public void setParams (int timeLimitMinutes) throws IloException {
        cplex.setParam(IloCplex.Param.MIP.Strategy.File, ZERO); 
        if (timeLimitMinutes>ZERO) cplex.setParam(IloCplex.Param.TimeLimit, timeLimitMinutes*SIXTY); 
        if (BackTrack) cplex.setParam( IloCplex.Param.MIP.Strategy.Backtrack,  ZERO); 
    }
    
    /*public double getBestRemaining_LPValue(){
        return this.branchHandler.bestReamining_LPValue;
    }*/
    
    private  ReincarnationMaps createReincarnationMaps (Map<String, CCANode> instructionTreeAsMap, String ccaRootNodeID){
        ReincarnationMaps   maps = new ReincarnationMaps ();
                
        for (String key : instructionTreeAsMap.keySet()){
            if (instructionTreeAsMap.get(key).leftChildNodeID!=null){
                //  this  needs to be branched upon , using the branching instructions in the CCA node
                maps.oldToNew_NodeId_Map.put( key,null  );
            }
        }
        
        //both maps can start with original MIP which is always node ID -1
        //but right now we are starting from the root CCA
        maps.oldToNew_NodeId_Map.put( ccaRootNodeID ,MINUS_ONE_STRING  );
        maps.newToOld_NodeId_Map.put( MINUS_ONE_STRING,ccaRootNodeID  );
        
        return maps;
    }
 
}
