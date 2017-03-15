/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cplex;

import static ca.mcmaster.spccav1_3.Constants.*;
import ca.mcmaster.spccav1_3.cca.CCAFinder;
import ca.mcmaster.spccav1_3.cca.CCANode;
import ca.mcmaster.spccav1_3.cplex.callbacks.*;
import ca.mcmaster.spccav1_3.cplex.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
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
    
    //this is the branch handler for the CPLEX object
    private BranchHandler branchHandler;
    private NodeHandler nodeHandler;
    private LeafFetchingNodeHandler leafFetchNodeHandler;
    
    //our list of active leafs after each solve cycle
    private List<NodeAttachment> allActiveLeafs  ;     
    
    //use this object to run CCA algorithms
    private CCAFinder ccaFinder =new CCAFinder();
    
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
        
        //create all the call back handlers
        //these are used depending on which method is invoked
        
        branchHandler = new BranchHandler(    );
        nodeHandler = new NodeHandler(    );                
        leafFetchNodeHandler = new LeafFetchingNodeHandler();
    
    }
    
    public void solve(int leafCountLimit, double cutoff, int timeLimitMinutes) throws IloException{
        
        //before solving , reset the CCA finder object which 
        //has an index built upon the current solution tree nodes
        this.ccaFinder.close();
        
        //set callbacks for regular solution
        this.cplex.use(branchHandler);
        this.cplex.use(nodeHandler);      
        
        setCutoff(  cutoff);
        setParams (  timeLimitMinutes);
        
        nodeHandler.setLeafCountLimit(leafCountLimit);
        cplex.solve();
        
        //solve complete - now get the active leafs
        this.cplex.use(leafFetchNodeHandler);  
        cplex.solve();
        allActiveLeafs = leafFetchNodeHandler.allLeafs;
        
        //initialize the CCA finder
        ccaFinder .initialize(allActiveLeafs);
        
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
      
    public void setCutoff(double cutoff) throws IloException {
        if (!IS_MAXIMIZATION) {
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.UpperCutoff, cutoff);
        }else {
            cplex.setParam(    IloCplex.Param.MIP.Tolerances.LowerCutoff, cutoff);
        }
    }
    
    public void setParams (int timeLimitMinutes) throws IloException {
        cplex.setParam(IloCplex.Param.MIP.Strategy.File, ZERO); 
        cplex.setParam(IloCplex.Param.TimeLimit, timeLimitMinutes*SIXTY); 
        if (BackTrack) cplex.setParam( IloCplex.Param.MIP.Strategy.Backtrack,  ZERO); 
    }
    
    public double getBestRemaining_LPValue(){
        return this.branchHandler.bestReamining_LPValue;
    }
    
 
}
