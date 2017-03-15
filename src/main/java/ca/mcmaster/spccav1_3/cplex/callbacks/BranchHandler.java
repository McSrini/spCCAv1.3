/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cplex.callbacks;

import static ca.mcmaster.spccav1_3.Constants.*;
import ca.mcmaster.spccav1_3.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spccav1_3.cplex.datatypes.NodeAttachment;
import ilog.concert.IloException;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
import static java.lang.System.exit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.apache.log4j.RollingFileAppender;

/**
 *
 * @author tamvadss
 */
public class BranchHandler extends IloCplex.BranchCallback {
    
    private static Logger logger=Logger.getLogger(BranchHandler.class);
         
    //list of nodes to be pruned
    public List<String> pruneList = new ArrayList<String>();
    
    public double bestReamining_LPValue = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
    
    //remove after testing
    //List<String> BAD_MIGRATION_CANDIDATES_DURING_TESTING =  Arrays.asList("Node25", "Node23", "Node24","Node13", "Node29", "Node30");      
    List<String> BAD_MIGRATION_CANDIDATES_DURING_TESTING =  Arrays.asList("Node15", "Node22", "Node25","Node28", "Node29", "Node30");      
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+BranchHandler.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
          
    }
 
    protected void main() throws IloException {
        
        if ( getNbranches()> 0 ){  
                       
            //get the node attachment for this node, any child nodes will accumulate the branching conditions
            NodeAttachment nodeData = (NodeAttachment) getNodeData();
            if (nodeData==null ) { //it will be null for subtree root
               
                nodeData=new NodeAttachment (      );  
                setNodeData(nodeData);                
                
            } 
            
            if (  pruneList.contains(nodeData.nodeID) ) {
                pruneList.remove( nodeData.nodeID);
                prune();
            }  else {
                
                //get the branches about to be created
                IloNumVar[][] vars = new IloNumVar[TWO][] ;
                double[ ][] bounds = new double[TWO ][];
                IloCplex.BranchDirection[ ][]  dirs = new  IloCplex.BranchDirection[ TWO][];
                getBranches(  vars, bounds, dirs);

                //now allow  both kids to spawn
                for (int childNum = ZERO ;childNum<getNbranches();  childNum++) {   
                    
                    //apply the bound changes specific to this child
                    
                    //first create the child node attachment
                    NodeAttachment thisChild  =  createChildNodeAttachment( nodeData,   childNum ); 
                    //record child node ID
                    IloCplex.NodeId nodeid = makeBranch(childNum,thisChild );
                    thisChild.nodeID =nodeid.toString();
                    
                    logger.debug(" Node "+nodeData.nodeID + " created child "+  thisChild.nodeID + " varname " +   vars[childNum][ZERO].getName() + " bound " + bounds[childNum][ZERO] +   (dirs[childNum][ZERO].equals( IloCplex.BranchDirection.Down) ? " U":" L") ) ;
                    
                    //for testing purposes, mark some nodes as bad choices for migration
                    if ( BAD_MIGRATION_CANDIDATES_DURING_TESTING.contains( thisChild.nodeID))   
                        thisChild.isMigrateable= false;
                         
                                        
                    //convert the branching instructions into java data types, and record child info
                    BranchingInstruction bi = createBranchingInstruction(   dirs[childNum], bounds[childNum], vars[childNum] );                    
                    if (childNum == ZERO) {
                        //update left child info
                        nodeData.leftChildNodeID=thisChild.nodeID;     
                        nodeData.branchingInstructionForLeftChild =bi;
                    }else {
                        nodeData.rightChildNodeID  =thisChild.nodeID ;  
                        nodeData.branchingInstructionForRightChild =bi;
                    }
                    

                    
                }//end for 2 kids
                
                
            }//end if else
            
            this.bestReamining_LPValue = getBestObjValue();
            
        } // end if getNbranches()> 0
        
    }//end main
    
        
    private NodeAttachment createChildNodeAttachment (NodeAttachment parentNodeData,int childNum ){
        NodeAttachment thisChild  = new NodeAttachment (); 
        
        thisChild.parentData = parentNodeData;
        thisChild.depthFromSubtreeRoot=parentNodeData.depthFromSubtreeRoot + ONE;
        

          
        return     thisChild;    
    }
    
    private BranchingInstruction createBranchingInstruction(IloCplex.BranchDirection[ ]  dirs,double[ ] bounds, IloNumVar[] vars ) {
        return new  BranchingInstruction(  getVarnames (  vars),   getVarDirections(dirs),   bounds);
    }
        
    private String[] getVarnames (IloNumVar[] vars) {
        String[] varnames = new  String[vars.length];
        
        int index = ZERO;
        for (IloNumVar var : vars) {
            varnames[index ++] = var.getName();
        }
        return varnames;
    }
    
    private Boolean[] getVarDirections (IloCplex.BranchDirection[ ]  dirs) {
        Boolean[] vardirs = new  Boolean[dirs.length];
        
        int index = ZERO;
        for (IloCplex.BranchDirection dir : dirs) {
            vardirs[index ++] = dir.equals( IloCplex.BranchDirection.Down);
        }
        return vardirs;
    }
    
    
}
