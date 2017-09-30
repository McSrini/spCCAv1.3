/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 *
 * @author tamvadss
 */
public class Constants {
        
    public static final String EMPTY_STRING ="";
    public static final String MINUS_ONE_STRING = "-1";
    public static final int ZERO = 0;
    public static final double DOUBLE_ZERO = 0.0;
    public static final int ONE = 1;
    public static final int TWO = 2;    
    public static final int THREE = 3; 
    public static final int FOUR = 4;    
    public static final int FIVE = 5;  
    public static final int TEN = 10;  
    public static final int SIXTY = 60;  
    public static final int HUNDRED = 100 ;  
    public static final int THOUSAND = 1000 ;  
    public static final int MILLION = 1000000;  
    public static final long PLUS_INFINITY = Long.MAX_VALUE;
    public static final long MINUS_INFINITY = Long.MIN_VALUE;
    public static final double EPSILON = 0.0000000001;
    public static final String DELIMITER = "______";
    
    //public static  String MPS_FILE_ON_DISK =  "F:\\temporary files here\\rd-rplusc-21.mps";
    //public static  String MPS_FILE_ON_DISK =  "F:\\temporary files here\\atlanta-ip.mps"; //windows
    public static  String MPS_FILE_ON_DISK =  "p100x588b.mps";  //linux
    

    //public static final String LOG_FOLDER="F:\\temporary files here\\logs\\testing\\ccav1_3\\"; //windows
    public static final String LOG_FOLDER="logs/"; //linux
    public static final String LOG_FILE_EXTENSION = ".log";
    
     
    public static final boolean IS_MAXIMIZATION = false;
     
    //CCA represents this many nodes
    //ignore, unused in experiments
    public static    int NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE  =  6;     
    //remove after testing
    //List<String> BAD_MIGRATION_CANDIDATES_DURING_TESTING =  Arrays.asList("Node25", "Node23", "Node24","Node13", "Node29", "Node30");     
    
    //unused, ignore
    public static  List<String> BAD_MIGRATION_CANDIDATES_DURING_TESTING = new ArrayList<String>(); // Arrays.asList("Node34",   "Node22", "Node13","Node28", "Node29", "Node30");      
    
    //used to reject inferior LCA candidates
    public static final double LCA_CANDIDATE_PACKING_FACTOR_LARGEST_ACCEPTABLE=1.2;
    
    //for testing, grow the tree this big
    //ignore, not used in experiments
    public static  final int TOTAL_LEAFS_IN_SOLUTION_TREE_FOR_RAMPUP =  18 ;
    
    //CCA subtree allowed to have slightly less good leafs than asked for in NUM_LEAFS_FOR_MIGRATION_IN_CCA_SUBTREE 
    public static   double CCA_TOLERANCE_FRACTION =  0.30;
    public static  double CCA_PACKING_FACTOR_MAXIMUM_ALLOWED =  0.0;
    
    //for testing
    //ignore, not used in experiments
    public static   boolean BackTrack = true;
    
    public static   int TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE = 3 ;    
    public static   double MINIMUM_TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE = 0.5 ;//30 seconds
    public static final int SOLUTION_CYCLE_TIME_MINUTES = 6;
    
    //used to skip CB test
    public static final boolean SKIP_CB_FLAG = true;
    
}
