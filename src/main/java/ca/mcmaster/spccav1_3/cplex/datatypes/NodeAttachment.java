/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cplex.datatypes;

import static ca.mcmaster.spccav1_3.Constants.*; 
import ca.mcmaster.spccav1_3.cca.*;
import ilog.concert.IloNumVar;
import ilog.cplex.IloCplex;
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
    public double     estimatedLPRelaxationValue ;
    
    //reference to parent node
    public  NodeAttachment  parentData = null;
    
    //at node creation time, node has no kids, but there can be kids later
    //Store the brancging information for both kids
    public  IloNumVar[][] branchingVars  ;
    public  double[ ][] branchingBounds  ;
    public  IloCplex.BranchDirection[ ][]  branchingDirections ;
        
    //ID of kids
    public  String  leftChildNodeID = null, rightChildNodeID=null;
     
    //random for now, this will be determined by node metrics
    public boolean isMigrateable = true;
      
    //place holder
    public WarmStartInformation warmStartInfo  ;
    
    //  information in every node which is populated and used by CCA algorithm
    public CCANode ccaInformation = null;
    public  NodeAttachment leftChildRef = null, rightChildRef = null;
    
    //converts vars bounds dirs into java format
    public BranchingInstruction getBranchingInstructionForLeftChild(){
         
        int size = this.branchingVars[ZERO].length;
        String[] names = new String[size];
        Boolean[] dirs = new Boolean[size];
        for(int index= ZERO; index <size; index++){
            names[index]= this.branchingVars[ZERO][index].getName();
            dirs[index] = this.branchingDirections[ZERO][index].equals( IloCplex.BranchDirection.Down );
        }
        
        return new BranchingInstruction( names, dirs, this.branchingBounds[ZERO]);
    }
    public BranchingInstruction getBranchingInstructionForRightChild(){
         
        int size = this.branchingVars[ONE].length;
        String[] names = new String[size];
        Boolean[] dirs = new Boolean[size];
        for(int index= ZERO; index <size; index++){
            names[index]= this.branchingVars[ONE][index].getName();
            dirs[index] = this.branchingDirections[ONE][index].equals( IloCplex.BranchDirection.Down );
        }
        
        return new BranchingInstruction( names, dirs, this.branchingBounds[ONE]);
    }
    
    public String toString(){
        String result = EMPTY_STRING;
        result += "NodeID "+ nodeID;
        result += isMigrateable? " Mig":" Un";
        result += " ";
                 
        result += "\n";
        
        
        if (leftChildNodeID!=null) {
            result += "Left child is " + leftChildNodeID + " \n";
            for (int index = ZERO ; index < this.branchingBounds[ZERO].length; index ++){
                String varname = this.branchingVars[ZERO][index].getName();
                Double varbound = this.branchingBounds[ZERO][index];
                String isDown = this.branchingDirections[ZERO][index].equals( IloCplex.BranchDirection.Down ) ? "U": "L";
                result += "("+varname + "," +varbound+ ","+isDown +") ";
            }
            result += "\n";
        }
        if (rightChildNodeID!=null){
            result += "Right child is " + rightChildNodeID + " \n";
            for (int index = ZERO ; index < this.branchingBounds[ONE].length; index ++){
                String varname = this.branchingVars[ONE][index].getName();
                Double varbound = this.branchingBounds[ONE][index];
                String isDown = this.branchingDirections[ONE][index].equals( IloCplex.BranchDirection.Down ) ? "U": "L";
                result += "("+varname + "," +varbound+ ","+isDown +") ";
            }
            result += "\n";
        }
        
        //result += "\n";
        return result;
    }
 
    public boolean isLeaf () {
        return this.leftChildNodeID==null && this.rightChildNodeID==null;
    }
}
