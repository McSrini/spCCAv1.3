/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cplex.datatypes;

import static ca.mcmaster.spccav1_3.Constants.*; 
import ca.mcmaster.spccav1_3.cca.*;
import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class NodeAttachment {
    
    //default node ID  for subtree root 
    public String nodeID = EMPTY_STRING + -ONE; 
    public int depthFromSubtreeRoot = ZERO;        
    
    //reference to parent node
    public  NodeAttachment  parentData = null;
    
    //at node creation time, node has no kids, but there can be kids later
    public BranchingInstruction branchingInstructionForLeftChild  ;
    public BranchingInstruction branchingInstructionForRightChild ;
    //ID of kids
    public  String  leftChildNodeID = null, rightChildNodeID=null;
     
    //random for now, this will be determined by node metrics
    public boolean isMigrateable = true;
      
    //place holder
    public WarmStartInformation warmStartInfo  ;
    
    //  information in every node which is populated and used by CCA algorithm
    public CCANode ccaInformation = null;
    public  NodeAttachment leftChildRef = null, rightChildRef = null;
    
    public String toString(){
        String result = EMPTY_STRING;
        result += "NodeID "+ nodeID;
        result += isMigrateable? " Mig":" Un";
        result += " ";
                 
        result += "\n";
        
        
        if (leftChildNodeID!=null) {
            result += branchingInstructionForLeftChild;
            result += "\n";
        }
        if (rightChildNodeID!=null){
            result +=branchingInstructionForRightChild;
            result += "\n";
        }
        
        //result += "\n";
        return result;
    }
 
    public boolean isLeaf () {
        return this.leftChildNodeID==null && this.rightChildNodeID==null;
    }
}
