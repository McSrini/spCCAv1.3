/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cca;

import ca.mcmaster.spccav1_3.utilities.CCAUtilities;
import static ca.mcmaster.spccav1_3.Constants.*;
import static ca.mcmaster.spccav1_3.utilities.CCAUtilities.*;
import ca.mcmaster.spccav1_3.cplex.ActiveSubtree;
import ca.mcmaster.spccav1_3.cplex.datatypes.BranchingInstruction;
import ca.mcmaster.spccav1_3.cplex.datatypes.NodeAttachment;
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
 * runs the CCA algorithm and finds candidate CCA nodes
 */
public class CCAFinder {
      
    private static Logger logger=Logger.getLogger(CCAFinder.class);
    
    private List<NodeAttachment> allLeafs = new ArrayList<NodeAttachment> () ;    
    private  NodeAttachment root = null;
    
    public List <CCANode> candidateCCANodes = new ArrayList <CCANode> ();
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+CCAFinder.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
               
    //prepares an index using solution tree nodes
    //This index is used to execute the CCA algorithm
    public void initialize (   List<NodeAttachment> allActiveLeafs ) {
        
        allLeafs=allActiveLeafs;
         
        for (NodeAttachment leaf : this.allLeafs){
            
            //climb up from this leaf  
            NodeAttachment thisNode= leaf ;
            NodeAttachment parent = thisNode.parentData;
            while (parent !=null) {
                
                if (parent.ccaInformation==null) {
                    parent.ccaInformation=new CCANode();
                    parent.ccaInformation.nodeID = parent.nodeID;
                }
                
                //set the child refs for parent
                if (parent.leftChildNodeID!=null && parent.leftChildNodeID.equals( thisNode.nodeID)) {                    
                    parent.leftChildRef=thisNode;                   
                }else {   
                    parent.rightChildRef=thisNode;
                }
                                
                //if parent has both left and right references set, we need not climb up 
                if (parent.rightChildRef!=null &&parent.leftChildRef!=null ) break;
                
                //climb up
                thisNode= parent;
                parent = parent.parentData;
                
            }//end while
            
            if (parent ==null) root = thisNode;
        }
        
    }//end init 
    
    public void close () {
        //reset CCA information in every node
        if (this.allLeafs!=null){
            for (NodeAttachment leaf : this.allLeafs){

                //climb up from this leaf  
                NodeAttachment thisNode= leaf ;
                NodeAttachment parent = thisNode.parentData;
                while (parent !=null) {

                    //no need to reset parent and climb up , if already traversed
                    if (parent.ccaInformation==null) break;

                    parent.rightChildRef=null;
                    parent.leftChildRef=null;
                    parent.ccaInformation=null;

                    thisNode= parent;
                    parent = parent.parentData;

                }//end while
            }
        }
        
    }
    
    //these getCandidateCCANodes() methods can be invoked multiple times, each time with a differnt argument
    public List<CCANode> getCandidateCCANodes (List<String> wantedLeafNodeIDs) {
        buildRefCounts(  wantedLeafNodeIDs);
        //printState(root);
        
        //prepare to split tree and find CCA nodes
        candidateCCANodes.clear();
        
        this.splitToCCA(root, wantedLeafNodeIDs.size() );
        return this.candidateCCANodes;
    }
    
    public List<CCANode> getCandidateCCANodes (int count)   {
        buildRefCounts();
        //printState(root);
                
        //prepare to split tree and find CCA nodes
        candidateCCANodes.clear();
        
        this.splitToCCA(root, count);
        return this.candidateCCANodes;
    }
     
  
     
    private void buildRefCounts( ){
       buildRefCounts(null);
    }
    
    //pass in null to build counts using isMigratable flag inside the leaf
    private void buildRefCounts(List<String> wantedLeafNodeIDs){
        clearRefCountsAndSkipCounts();
        for (NodeAttachment leaf : this.allLeafs){
            if (wantedLeafNodeIDs!=null && !wantedLeafNodeIDs.contains(leaf.nodeID)) continue;
            
            //climb up from this leaf  
            NodeAttachment thisNode= leaf ;
            NodeAttachment parent = thisNode.parentData;
            while (parent !=null) {
                 
                if (parent.leftChildNodeID!=null && parent.leftChildNodeID.equals( thisNode.nodeID)) {      
                    if (thisNode.isLeaf() ){
                        parent.ccaInformation.refCountLeft =  wantedLeafNodeIDs!=null ? ONE: thisNode.isMigrateable ? ONE : ZERO;
                    } else {
                        parent.ccaInformation.refCountLeft =thisNode.ccaInformation.refCountLeft +  thisNode.ccaInformation.refCountRight;
                    }
                } else {
                    if (thisNode.isLeaf() ){
                        parent.ccaInformation.refCountRight =wantedLeafNodeIDs!=null ? ONE: thisNode.isMigrateable ? ONE : ZERO;
                    } else {
                        parent.ccaInformation.refCountRight =thisNode.ccaInformation.refCountLeft +  thisNode.ccaInformation.refCountRight;
                    }
                }
                
                thisNode= parent;
                parent = parent.parentData;
                
            }//end while
        }
    }
    
    //start from root and split tree into left and right, looking for candidate CCA nodes
    //count can be desired count, or the size of the list of desired leafs
    private void splitToCCA( NodeAttachment thisNode, int count){
        
        if (thisNode.isLeaf()) {
            //not a valid CCA candidate, discard
        } else  if (isSplitNeeded(thisNode,   count)) {
            
            if (thisNode.ccaInformation.refCountLeft>ZERO) {
                splitToCCA(   thisNode.leftChildRef,   count);
            }
            if (thisNode.ccaInformation.refCountRight>ZERO) {
                splitToCCA(   thisNode.rightChildRef,   count);
            }
            
        } else {
            //check if valid CCA candidate, else discard 
            if (thisNode.ccaInformation.refCountLeft+ thisNode.ccaInformation.refCountRight>= count*(ONE-CCA_TOLERANCE_FRACTION)) {
                //found a valid candidate
                //add branching instructions, # of redundant LP solves needed and so on
                CCAUtilities.populateCCAStatistics(thisNode, this.allLeafs) ;
                candidateCCANodes.add(thisNode.ccaInformation);                
            }
        }
        
    }
    
    private boolean isSplitNeeded ( NodeAttachment thisNode, int count) {
        boolean result = false;
               
        if (thisNode.ccaInformation.refCountLeft  >= count ) result = true;
        if ( thisNode.ccaInformation.refCountRight >=count ) result = true;
        
        if (    (thisNode.ccaInformation.refCountLeft  >= count*(ONE-CCA_TOLERANCE_FRACTION) ) && 
                (thisNode.ccaInformation.refCountRight  < count*(CCA_TOLERANCE_FRACTION) )  ) {
            result = true;
        }
        
        if (    (thisNode.ccaInformation.refCountRight  >= count*(ONE-CCA_TOLERANCE_FRACTION) ) && 
                (thisNode.ccaInformation.refCountLeft  < count*(CCA_TOLERANCE_FRACTION) )  ) {
            result = true;
        }
        
        return result;
        
    }
    
    private void clearRefCountsAndSkipCounts() {
        for (NodeAttachment leaf : this.allLeafs){
            
            //climb up from this leaf  
            NodeAttachment thisNode= leaf ;
            NodeAttachment parent = thisNode.parentData;
            while (parent !=null) {
                
                parent.ccaInformation.refCountLeft=ZERO;
                parent.ccaInformation.refCountRight=ZERO; 
                parent.ccaInformation.skipCountLeft=ZERO;
                parent.ccaInformation.skipCountRight=ZERO;
                 
                thisNode= parent;
                parent = parent.parentData;
                
            }//end while
        }
    }
    

}
