/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cb;

import static ca.mcmaster.spccav1_3.Constants.*;
import ca.mcmaster.spccav1_3.cca.*;
import ca.mcmaster.spccav1_3.cplex.datatypes.NodeAttachment;
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
 * updates skip counts into tree under the CCA node, and 
 * uses the counts to generate the CB tree rooted at the CCA
 * 
 */
public class CBTreeGenerator {
    
    private static Logger logger=Logger.getLogger(CBTreeGenerator.class);
      
    private  List<NodeAttachment> allActiveLeafs;
    private  List<NodeAttachment> wantedActiveLeafs = new ArrayList<NodeAttachment> ();
    
    //node attachment corresponding to CCA root
    private NodeAttachment ccaRootNodeAttachment;
    
    //return value
    public CBInstructionTree cbInstructionTree ;
    
    
    static {
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            logger.addAppender(new  RollingFileAppender(layout,LOG_FOLDER+CBTreeGenerator.class.getSimpleName()+ LOG_FILE_EXTENSION));
            logger.setAdditivity(false);
        } catch (Exception ex) {
            ///
            System.err.println("Exit: unable to initialize logging");       
            exit(1);
        }
          
    }
    
    public CBTreeGenerator(CCANode ccaNode,   List<NodeAttachment> allLeafs, List<String> wantedLeafs) {
        cbInstructionTree = new CBInstructionTree(ccaNode);         
        this.allActiveLeafs=allLeafs;
        for (NodeAttachment leaf : this.allActiveLeafs){
           if ( wantedLeafs.contains(leaf.nodeID)) wantedActiveLeafs.add(leaf );
            
        }
        this.ccaRootNodeAttachment=getCCARootNodeAttachment();
        
        buildSkipcounts();
        this.printState(ccaRootNodeAttachment);
    }
    
    //return node attachment corresponding to CCA root
    private NodeAttachment getCCARootNodeAttachment () {
        
        //pick any wanted leaf
        NodeAttachment currentNode = this.wantedActiveLeafs.get(ZERO);           
        NodeAttachment parentNode = currentNode.parentData;

        //climb up to the CCA node
        while (! currentNode.nodeID.equals(this.cbInstructionTree.ccaRoot.nodeID)){

            currentNode = parentNode;
            parentNode=currentNode.parentData; 
        }
        
        return currentNode;
    }
    
    private void buildSkipcounts () {
        for (NodeAttachment wantedLeaf : this.wantedActiveLeafs){
            
            NodeAttachment currentNode = wantedLeaf;           
            NodeAttachment parentNode = currentNode.parentData;
            
            //this node, and each of its parents, must do the following 
            //   check if self can be skipped over, i.e. if self's refcounts are like (N>=2, 0) or (0, N>=2)
            //if yes, inform parent of direction and cumulative skip count
            
            //climb up to the CCA node
            while (! currentNode.nodeID.equals(this.cbInstructionTree.ccaRoot.nodeID)){
                
                String currentNodeID = currentNode.nodeID;
                    
                boolean canSelfBeSkippedOver = false;
                
                if (!currentNode.isLeaf()){
                    canSelfBeSkippedOver=currentNode.ccaInformation.refCountLeft  ==ZERO && 
                                               currentNode.ccaInformation.refCountRight  >= TWO;
                    canSelfBeSkippedOver = canSelfBeSkippedOver ||  
                                       (currentNode.ccaInformation.  refCountRight ==ZERO && 
                                       currentNode.ccaInformation. refCountLeft  >= TWO);
                }
                
                Boolean amITheLeftChild = parentNode.leftChildNodeID!=null && parentNode.leftChildNodeID.equals(currentNodeID );
                
                if (canSelfBeSkippedOver) {

                    //Recall that , since I am skippable, at most one kid could have sent me a skip count
                    //So only 1 of my skip counts is non-zero 
                    int mySkipCount = currentNode.ccaInformation.skipCountRight+ currentNode.ccaInformation.skipCountLeft   ;

                    //now send the parent the cumulative skip count 
                    if (amITheLeftChild) {
                        parentNode.ccaInformation.skipCountLeft= ONE + mySkipCount;
                    }  else {
                        parentNode.ccaInformation.skipCountRight  = ONE + mySkipCount;
                    }

                } else {
                    //send 0 skip count to parent
                    if (amITheLeftChild) parentNode.ccaInformation.skipCountLeft=ZERO; else parentNode.ccaInformation.skipCountRight=ZERO;
                } 
                
                currentNode = parentNode;
                parentNode=currentNode.parentData;   
            }
            
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
