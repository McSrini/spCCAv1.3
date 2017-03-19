/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.utilities;

import static ca.mcmaster.spccav1_3.Constants.*;
import ca.mcmaster.spccav1_3.cplex.datatypes.*;
import java.util.*; 

/**
 *
 * @author tamvadss
 */
public class CCAUtilities {
    
    public static void  populateCCAStatistics(NodeAttachment thisNode, List<NodeAttachment> allLeafs) {
        
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
        
        thisNode.ccaInformation.maxDepthOFTree= getMaxDepthOfTree (  allLeafs) ;
        
        thisNode.ccaInformation.branchingInstructionList.clear();//reset
        getBranchingInstructionForCCANode (  thisNode,    thisNode.ccaInformation.branchingInstructionList);
        
        thisNode.ccaInformation.pruneList.clear();//reset
        getPruneList(thisNode, thisNode.ccaInformation.pruneList);
        
    }
        
    private static void getMapOFNodeWithOneMissingBranch(NodeAttachment node, Map<Integer, Integer > mapOfNodesWithOneMissingBranch){
        if (node.leftChildRef==null && node.rightChildRef==null){
            //do nothing
        } else if (node.leftChildRef!=null && node.rightChildRef!=null) {
            getMapOFNodeWithOneMissingBranch(node.leftChildRef, mapOfNodesWithOneMissingBranch);
            getMapOFNodeWithOneMissingBranch(node.rightChildRef, mapOfNodesWithOneMissingBranch);
        }else  {
            //only one side is null
            int depth = node.depthFromSubtreeRoot;
            int value = mapOfNodesWithOneMissingBranch.containsKey(depth)? mapOfNodesWithOneMissingBranch.get(depth): ZERO;
            mapOfNodesWithOneMissingBranch.put (depth,ONE+ value);
            
            if (node.leftChildRef!=null) getMapOFNodeWithOneMissingBranch(node.leftChildRef, mapOfNodesWithOneMissingBranch);
            else                         getMapOFNodeWithOneMissingBranch(node.rightChildRef, mapOfNodesWithOneMissingBranch);
             
        }
    }
        
    private static int getNodeLPSolvesNeeded (NodeAttachment node) {
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
   
    private static int  getMaxDepthOfTree (List<NodeAttachment> allLeafs) {
        int max = -ONE;
        for (NodeAttachment leaf : allLeafs){
            if ( leaf.depthFromSubtreeRoot > max ) max = leaf.depthFromSubtreeRoot;
        }
        return max;
    }
        
    //climb up all the way to root
    private static void getBranchingInstructionForCCANode (NodeAttachment node,  List<BranchingInstruction> branchingInstructions){
        
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
    private static void getPruneList(NodeAttachment thisNode, List<String> pruneList) {
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
    
}
