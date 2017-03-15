/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cca;

import static ca.mcmaster.spccav1_3.Constants.*;
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
                
                //set the ref-counts and child refs for parent
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
    
    public List<CCANode> getCandidateCCANodes (List<String> wantedLeafNodeIDs) {
        buildRefCounts(  wantedLeafNodeIDs);
        printState(root);
        
        //prepare to split tree and find CCA nodes
        candidateCCANodes.clear();
        
        this.splitToCCA(root, wantedLeafNodeIDs.size() );
        return this.candidateCCANodes;
    }
    public List<CCANode> getCandidateCCANodes (int count)   {
        buildRefCounts();
        printState(root);
                
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
        clearRefCounts();
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
        
        if (isSplitNeeded(thisNode,   count)) {
            
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
                populateCCAStatistics(thisNode) ;
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
    
    private void  populateCCAStatistics(NodeAttachment thisNode) {
        
        /*populate  :
        
        
        
        numNodeLPSolvesNeeded
        mapOfNodesWithOneMissingBranch 
        
        depthOfCCANodeBelowRoot;
        
        maxDepthOFTree;
        branchingInstructionList
        
        pruneList
        
        */
        
        
        
        thisNode.ccaInformation.numNodeLPSolvesNeeded=getNodeLPSolvesNeeded(  thisNode);
        getMapOFNodeWithOneMissingBranch(thisNode,  thisNode.ccaInformation.mapOfNodesWithOneMissingBranch);
        
        thisNode.ccaInformation.depthOfCCANodeBelowRoot = thisNode.depthFromSubtreeRoot;
        
        thisNode.ccaInformation.maxDepthOFTree= getMaxDepthOfTree () ;
        getBranchingInstructionForCCANode (  thisNode,    thisNode.ccaInformation.branchingInstructionList);
        
        getPruneList(thisNode, thisNode.ccaInformation.pruneList);
        
    }
    
    private void getMapOFNodeWithOneMissingBranch(NodeAttachment node, Map<Integer, Integer > mapOfNodesWithOneMissingBranch){
        if (node.leftChildRef==null && node.rightChildRef==null){
            //do nothing
        } else if (node.leftChildRef!=null && node.rightChildRef!=null) {
            getMapOFNodeWithOneMissingBranch(node.leftChildRef, mapOfNodesWithOneMissingBranch);
            getMapOFNodeWithOneMissingBranch(node.rightChildRef, mapOfNodesWithOneMissingBranch);
        }else  {
            //only one side is null
            int depth = node.depthFromSubtreeRoot;
            int value = mapOfNodesWithOneMissingBranch.containsKey(depth)? mapOfNodesWithOneMissingBranch.get(depth): ZERO;
            mapOfNodesWithOneMissingBranch.put (depth, value);
            
            if (node.leftChildRef!=null) getMapOFNodeWithOneMissingBranch(node.leftChildRef, mapOfNodesWithOneMissingBranch);
            else                         getMapOFNodeWithOneMissingBranch(node.rightChildRef, mapOfNodesWithOneMissingBranch);
             
        }
    }
    
    private int getNodeLPSolvesNeeded (NodeAttachment node) {
        int count = ONE;
        
        if (node.leftChildRef==null && node.rightChildRef==null) {
            count = ZERO;
        } else if (node.leftChildRef==null) {
            if (!node.rightChildRef.isLeaf()) count +=getNodeLPSolvesNeeded(node.rightChildRef);
        } else if (node.rightChildRef==null ){
            if (!node.leftChildRef.isLeaf()) count +=getNodeLPSolvesNeeded(node.leftChildRef);
        }else {
            //neither side is null
            if (!node.rightChildRef.isLeaf()) count +=getNodeLPSolvesNeeded(node.rightChildRef);
            if (!node.leftChildRef.isLeaf()) count +=getNodeLPSolvesNeeded(node.leftChildRef);
        }
        
        return count;
    }
   
    private int  getMaxDepthOfTree () {
        int max = -ONE;
        for (NodeAttachment leaf : allLeafs){
            if ( leaf.depthFromSubtreeRoot > max ) max = leaf.depthFromSubtreeRoot;
        }
        return max;
    }
    
    //climb up all the way to root
    private void getBranchingInstructionForCCANode (NodeAttachment node,  List<BranchingInstruction> branchingInstructions){
        
        NodeAttachment thisNode = node;
        NodeAttachment parent = node.parentData;
        while (parent !=null){
            
            if (parent.rightChildRef!=null && parent.rightChildNodeID.equals( thisNode.nodeID)) {
                branchingInstructions.add(parent.branchingInstructionForRightChild) ;
            } else {
                //must be the left child
                 branchingInstructions.add(parent.branchingInstructionForLeftChild) ;
            }
            
            thisNode = parent;
            parent = parent.parentData;
        }
    }
    
    //find all leafs below this CCA node
    private void getPruneList(NodeAttachment thisNode, List<String> pruneList) {
        if (thisNode.leftChildRef!=null ){
            if (thisNode.leftChildRef.isLeaf()) {
                pruneList.add(thisNode.leftChildRef.nodeID) ;
            } else {
                getPruneList(  thisNode.leftChildRef,  pruneList)        ;
            }
        }
        if (thisNode.rightChildRef !=null) {
            if (thisNode.rightChildRef.isLeaf()) {
                pruneList.add(thisNode.rightChildRef.nodeID) ;
            } else {
                getPruneList(  thisNode.rightChildRef,  pruneList)        ;
            }
        }            
               
    }
    
    private void clearRefCounts() {
        for (NodeAttachment leaf : this.allLeafs){
            
            //climb up from this leaf  
            NodeAttachment thisNode= leaf ;
            NodeAttachment parent = thisNode.parentData;
            while (parent !=null) {
                
                parent.ccaInformation.refCountLeft=ZERO;
                parent.ccaInformation.refCountRight=ZERO; 
                
                thisNode= parent;
                parent = parent.parentData;
                
            }//end while
        }
    }
    
    //dump status 
    private void printState(NodeAttachment node) {
        if ( node.ccaInformation!=null){
            logger.debug( node.ccaInformation);
        }
        if (node.leftChildRef!=null && !node.leftChildRef.isLeaf()){
            printState( node.leftChildRef);
        }
        if (node.rightChildRef!=null && !node.rightChildRef.isLeaf()){
            printState( node.rightChildRef);
        }
    }
}
