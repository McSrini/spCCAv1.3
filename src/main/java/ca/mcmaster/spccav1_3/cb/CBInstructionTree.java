/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3.cb;

import ca.mcmaster.spccav1_3.cca.CCANode;

/**
 *
 * @author tamvadss
 */
public class CBInstructionTree {
    
    public CCANode ccaRoot;
    public CBInstructionTree leftSubTree=null, rightSubtree=null;
    
    public CBInstructionTree (CCANode root){
        ccaRoot=root;
    }
    
}
