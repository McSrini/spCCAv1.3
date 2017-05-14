/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3;

import static ca.mcmaster.spccav1_3.Constants.*;
import static ca.mcmaster.spccav1_3.Parameters.*; 
import ca.mcmaster.spccav1_3.cb.CBInstructionTree;
import ca.mcmaster.spccav1_3.cca.CCANode;
import ca.mcmaster.spccav1_3.cplex.ActiveSubtree;
import ca.mcmaster.spccav1_3.cplex.ActiveSubtreeCollection;
import ca.mcmaster.spccav1_3.cplex.datatypes.NodeAttachment;
import ca.mcmaster.spccav1_3.cplex.datatypes.SolutionVector;
import java.io.File;
import static java.lang.System.exit;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import org.apache.log4j.*;
  

/**
 *
 * @author tamvadss
 * 
 *  test 3 with atlanta-ip takes 18 hours
 
 * 
 * run MIP on 5 simulated partitions
 * sub problems created using variable bound merging, and solved using traditional branch and bound
 * 2 ramp ups, one for using CCA and one without CCA
 */
public class TestDriver_CCATraditional_SimulatedCluster_Test3 {
    
    private static  Logger logger = null;
    
    private static  int NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION = ZERO;
    
    

    private static  int NUM_PARTITIONS = 100;
    private static double EXPECTED_LEAFS_PER_PARTITION = PLUS_INFINITY;
    
    //private static final int SOLUTION_CYCLE_Tu           fgggd hjhhIME_MINUTES = THREE;
     
    public static void main(String[] args) throws Exception {
            
        logger=Logger.getLogger(TestDriver_CCATraditional_SimulatedCluster_Test3.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_CCATraditional_SimulatedCluster_Test3.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
        
        MIP_NAME_UNDER_TEST = "atlanta-ip";    
        MIP_WELLKNOWN_SOLUTION =  90.009878614 ; 
        RAMP_UP_TO_THIS_MANY_LEAFS = 6000;
        EXPECTED_LEAFS_PER_PARTITION = (RAMP_UP_TO_THIS_MANY_LEAFS +DOUBLE_ZERO)/NUM_PARTITIONS;
         
        //first run 2 identical ramp ups
        MPS_FILE_ON_DISK =  "F:\\temporary files here\\"+MIP_NAME_UNDER_TEST+".mps";
        BackTrack=false;
        BAD_MIGRATION_CANDIDATES_DURING_TESTING = new ArrayList<String>();
        logger.debug ("starting ramp up") ;  
        ActiveSubtree activeSubtreeONE = new ActiveSubtree () ;
        activeSubtreeONE.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false); 
        ActiveSubtree activeSubtreeTWO = new ActiveSubtree () ;
        activeSubtreeTWO.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false); 
        
        //verify activeSubtreeONE and activeSubtreeTWO identical ramp up
        logger.debug ("verify activeSubtreeONE and activeSubtreeTWO identical ramp up") ;   
        //there are probably better ways of doing this - I have temporarily edited the branch callback 
        List<String> nodeCreationInfoListONE = activeSubtreeONE.getNodeCreationInfoList();
        List<String> nodeCreationInfoListTWO = activeSubtreeTWO.getNodeCreationInfoList();
        if (activeSubtreeONE.getMaxBranchingVars()!= activeSubtreeTWO.getMaxBranchingVars() || 
               nodeCreationInfoListONE.size()!= nodeCreationInfoListTWO.size()){
            logger.error ("ramp up not identical");
            exit(ONE);
        }
        
        for (int index = ZERO; index < nodeCreationInfoListONE.size(); index ++){
            if (! nodeCreationInfoListONE.get(index).equals(nodeCreationInfoListTWO.get(index) )) {
                logger.error ("ramp up not identical - branching conditions vary");
                exit(ONE);
            }
        }
        
        List<NodeAttachment> activeLeaflistONE = activeSubtreeONE.getActiveLeafList();
        List<NodeAttachment> activeLeaflistTWO = activeSubtreeONE.getActiveLeafList();
        if (activeLeaflistONE.size()!=activeLeaflistTWO.size()){
            logger.error ("ramp up not identical - active leaf counts vary");
            exit(ONE);
        }
        for (int index = ZERO; index < activeLeaflistONE.size(); index ++){
            if (! activeLeaflistTWO.get(index).nodeID.equals(activeLeaflistONE.get(index).nodeID )){
                logger.error ("ramp up not identical - active leaf ids vary");
                exit(ONE);
            }
        }
        
        
        logger.info("Ramp ups are identical, can proceed");
        
        //now extract CCA nodes from ramped up tree
        int leafCountRemainingInHomePartition = (int) activeSubtreeONE.getActiveLeafCount();
                         
        List<CCANode> acceptedCCANodes =new ArrayList<CCANode> () ;
        // we convert each accepted CCA node into an active subtree collection, for use in the second part of the test
        List<ActiveSubtreeCollection> activeSubtreeCollectionList = new ArrayList<ActiveSubtreeCollection>();
        //here is the lest of leafs to be pruned from the home partition
        List<String> pruneListONE = new ArrayList<String>();
        List<String> pruneListTWO = new ArrayList<String>();
        
        //get CCA condidates
        //List<CCANode> candidateCCANodes = activeSubtreeONE.getCandidateCCANodes( LEAFS_PER_CCA );             
        List<CCANode> candidateCCANodes = activeSubtreeONE.getCandidateCCANodesPostRampup(NUM_PARTITIONS);    
        
        if (candidateCCANodes.size() < NUM_PARTITIONS) {
            logger.error("this splitToCCAPostRampup partitioning cannot be done  , try ramping up to  a larger number of leafs ");
            exit(ZERO);
        }
        
        //for every accepted CCA node, we create a active subtree collection that has all its leafs
        //Then, prune these leafs in preparation for creating the next batch of CCA nodes
        //
        //active subtree collection needs to be formed before the leafs are "pruned"
        for (CCANode ccaNode: candidateCCANodes){

            if (ccaNode.getPackingFactor() < TWO && ccaNode.pruneList.size() > EXPECTED_LEAFS_PER_PARTITION/TWO ) {
                logger.debug (""+ccaNode.nodeID + " has good packing factor " +ccaNode.getPackingFactor() + 
                        " and prune list size " + ccaNode.pruneList.size() + " depth from root "+ ccaNode.depthOfCCANodeBelowRoot) ; 
                NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION ++;
                acceptedCCANodes.add(ccaNode);

                //add entry to  active Subtree Collection
                List<CCANode> ccaLeafNodeList = activeSubtreeONE.getActiveLeafsAsCCANodes( ccaNode.pruneList);        
                ActiveSubtreeCollection astc = new ActiveSubtreeCollection (ccaLeafNodeList, activeSubtreeONE.instructionsFromOriginalMip, -ONE, false, NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION) ;
                activeSubtreeCollectionList.add(astc);

                //prune leafs from active subtree
                activeSubtreeONE.prune( ccaNode.pruneList, true);
                //prune the same leafs from the clone active subtree
                activeSubtreeTWO.prune( ccaNode.pruneList, true);
                //since we are changing the branch handler, I am supplying the prune list to the new branch handler. This is not how it was initially supposed to be.
                pruneListONE.addAll( ccaNode.pruneList);
                pruneListTWO.addAll( ccaNode.pruneList);

            }   
            if (NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION >=NUM_PARTITIONS-1 )             break; //leave 1 node on home partition

        }
        leafCountRemainingInHomePartition = (int) activeSubtreeONE.getActiveLeafCount();
         
        
        //at this point, we have farmed out CCA nodes, and also
        //have the corresponding subtree collections for comparision [ each subtree collection has all the leafs of the corresponding CCA]                 
        logger.debug ("number of CCA nodes collected = "+acceptedCCANodes.size()) ;            
        for ( int index = ZERO; index <  acceptedCCANodes.size(); index++){
            logger.debug("accepted CCA node is : " + acceptedCCANodes.get(index)) ;
            logger.debug ("number of leafs in corresponding active subtree collections is = " +     (activeSubtreeCollectionList.get(index).getPendingRawNodeCount() + activeSubtreeCollectionList.get(index).getNumTrees()) );              
        }
        logger.debug ("NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION "+NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION + " home part left with leafcount "+leafCountRemainingInHomePartition);
        
              
        //find the best known solution after ramp up
        SolutionVector bestKnownSolutionAfterRampup  = null;
        double incumbentValueAfterRampup = IS_MAXIMIZATION ? MINUS_INFINITY : PLUS_INFINITY;
        if (activeSubtreeONE.isFeasible()) {
            bestKnownSolutionAfterRampup =             activeSubtreeONE.getSolutionVector();
            incumbentValueAfterRampup = activeSubtreeONE.getObjectiveValue();
            logger.debug("best known solution after ramp up is "+ activeSubtreeONE.getObjectiveValue()) ;
        } else {
            logger.debug("NO known solution after ramp up   " ) ;
        }
        
        //PREPARATIONS COMPLETE
        
        
        //TEST 1 uses CCA
        //LAter on , TEST 2 will use individual leafs
        
        
        //TEST 1
        
        //init the best known solution value and vector which will be updated as the solution progresses
        //Initialize them to values after ramp up
        SolutionVector  bestKnownSolution = bestKnownSolutionAfterRampup ==null? null : activeSubtreeONE.getSolutionVector();
        double  incumbentValue= incumbentValueAfterRampup;
         
         
        //now init iterations, recall we run iterations until 1 partition is out of work
        //the first test uses CCA , the second test will use raw leafs
        
        //TEST 1 : with CCA
        int iterationNumber=ZERO;
        boolean greenFlagForIterations = true;
        //create 1 tree per partition
        //note that we have the home partition plus as many CCA nodes as we have accepted for migration
        NUM_PARTITIONS= NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION + ONE;
        List<ActiveSubtree> partitionList = new ArrayList<ActiveSubtree> (NUM_PARTITIONS);
        partitionList.add(activeSubtreeONE  ); //home MIP
        //
        //now add the farmed out CCA nodes
        for (CCANode ccaNode :acceptedCCANodes ){
            ActiveSubtree treeStraight = new ActiveSubtree() ;
            treeStraight.mergeVarBounds(ccaNode, activeSubtreeONE.instructionsFromOriginalMip, false );
            if (bestKnownSolutionAfterRampup!=null) treeStraight.setCutoffValue(incumbentValueAfterRampup ); //setMIPStart(bestKnownSolution);
            partitionList.add(treeStraight);
        }
        
        for (; greenFlagForIterations ;iterationNumber++){ //while green flag, i.e. while no partition is complete
            
            if(isHaltFilePresent())  exit(ONE);
            logger.debug("starting iteration Number "+iterationNumber);
                    
            //solve every partition for 3 minutes at a time
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){                                
                logger.debug("Solving partition for 3 minutes ... Partition_" + partitionNumber );
                partitionList.get(partitionNumber).simpleSolve( SOLUTION_CYCLE_TIME_MINUTES ,  true,  false, partitionNumber == ZERO ? pruneListONE: null);                
            }
            
            //we are done when 1 partition has no active leafs left, i.e. its optimal or unfeasible
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
                if (partitionList.get(partitionNumber).isUnFeasible() || partitionList.get(partitionNumber).isOptimal()) {
                    logger.debug("partition "+ partitionNumber + " solved. Stopping iterations at " + iterationNumber);
                    greenFlagForIterations=false;
                }
            }
                         
            //if better solution found on any partition, update incumbent, and supply MIP start to every partition
            int partitionWithIncumbentUpdate = -ONE;
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
                
                ActiveSubtree tree = partitionList.get(partitionNumber);
            
                if   ( tree.isFeasible()||tree.isOptimal())  {
                    if (  (!IS_MAXIMIZATION  && incumbentValue> tree.getObjectiveValue())  || (IS_MAXIMIZATION && incumbentValue< tree.getObjectiveValue()) ) {     
                        bestKnownSolution =              tree.getSolutionVector();
                        incumbentValue =  tree.getObjectiveValue();
                        partitionWithIncumbentUpdate= partitionNumber;
                    }
                }
            }
            //update the MIP start if needed
            if (partitionWithIncumbentUpdate>=ZERO){
                for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
                                     
                    if (partitionNumber==partitionWithIncumbentUpdate) continue;
                    partitionList.get(partitionNumber).setCutoffValue(incumbentValue);//   setMIPStart(bestKnownSolution );
                }
                logger.debug (" incumbent was updated to " + incumbentValue);
            }
            
            //do another iteration involving every partition
            
        }//for greenFlagForIterations
        
        logger.debug(" CCA test ended at iteration Number "+iterationNumber);
        //for every partition , print mip gap and # of leafs reamining, then end every partition
        for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
                
                ActiveSubtree tree = partitionList.get(partitionNumber);
                
                double localMipGapPercent = (tree.isFeasible()||tree.isOptimal()) ? tree.getRelativeMIPGapPercent(false, -ONE):-ONE;
                double globalMipGapPercent = (tree.isFeasible()||tree.isOptimal()) && (incumbentValue < PLUS_INFINITY && incumbentValue > MINUS_INFINITY)?
                                              tree.getRelativeMIPGapPercent(true, incumbentValue):-ONE;
                long numLeafsReamining = tree.numActiveLeafsAfterSimpleSolve;
                long numLeafsReaminingLP = tree.numActiveLeafsWithGoodLPAfterSimpleSolve;
                
                //this is the seed CCA on this partition, home partition is of course seeded by 0
                String ccaSeedNodeID = ""+ZERO;
                if (partitionNumber != ZERO){
                    ccaSeedNodeID = tree.seedCCANodeID;
                } 
                logger.debug ("Partition "+partitionNumber + "  has local mipgap " + localMipGapPercent + " global mipgap " + globalMipGapPercent +
                        " and #leafs " + numLeafsReamining + " and good lp #leafs " + numLeafsReaminingLP + 
                        " and was seeded by CCA node " + ccaSeedNodeID  + " and has status "+tree.getStatus());
                tree.end();
        }
        
        
        
        //HERE is part 2 of the test, where we run individual leafs and compare results with CCA
        //Note that the home partition continues to be a single tree, although the clone is used because the original home partition has already been solved
        //other partitions   are already the created , namely activeSubtreeCollectionList  
        
        //re-init incumbent, and set cutoff on each partition
        incumbentValue= incumbentValueAfterRampup;
        for (ActiveSubtreeCollection astc : activeSubtreeCollectionList){
            if (bestKnownSolutionAfterRampup!=null) astc.setCutoff(incumbentValue);
        }
        
        //we allow upto 100 more iterations to see if a partition completes
        int maxIterationsAllowedWithIndividualLeafs = HUNDRED +iterationNumber;
        greenFlagForIterations = true;
        
        for (iterationNumber=ZERO; greenFlagForIterations &&iterationNumber<maxIterationsAllowedWithIndividualLeafs; iterationNumber++){ //same # of iterations as test 1
            
            if(isHaltFilePresent())  exit(ONE);
            logger.debug("starting test2 iteration Number "+iterationNumber);
                    
            //solve every partition for 3 minutes at a time
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){                                

                logger.debug("Solving partition for 3 minutes ... Partition_" + partitionNumber );
                if(partitionNumber==ZERO){
                    activeSubtreeTWO.simpleSolve( SOLUTION_CYCLE_TIME_MINUTES ,  true,  false, partitionNumber == ZERO ? pruneListTWO: null);  
                }else{
                    activeSubtreeCollectionList.get(partitionNumber-ONE).solve( true, SOLUTION_CYCLE_TIME_MINUTES  ,     true,    TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE  );
                }                
            }
            
            //if better solution found on any partition, update incumbent, and supply MIP start to every partition
            int partitionWithIncumbentUpdate = -ONE;
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ )/*]]*/{
                 
                double challengerToIncumbent =ZERO ;
                 
                if (partitionNumber == ZERO){
                    if(activeSubtreeTWO.isFeasible() || activeSubtreeTWO.isOptimal())      challengerToIncumbent=   activeSubtreeTWO.getObjectiveValue();
                }else {  /*ggg9gj*/
                    ActiveSubtreeCollection astc = activeSubtreeCollectionList.get(partitionNumber-ONE);
                    challengerToIncumbent=astc.getIncumbentValue() ;
                }
                            
                if (  (!IS_MAXIMIZATION  && incumbentValue> challengerToIncumbent)  || (IS_MAXIMIZATION && incumbentValue< challengerToIncumbent) ) {     
                    //bestKnownSolution =   solutionVector;
                    incumbentValue =  challengerToIncumbent;
                    partitionWithIncumbentUpdate= partitionNumber;
                }

            }
            //update the MIP start if needed
            if (partitionWithIncumbentUpdate>=ZERO){
                for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){                                      
                    if (partitionNumber==partitionWithIncumbentUpdate) continue;
                    if (partitionNumber == ZERO){
                        activeSubtreeTWO.setCutoffValue( incumbentValue);
                    }else{
                        activeSubtreeCollectionList.get(partitionNumber-ONE).setCutoff(incumbentValue );//   setMIPStart(bestKnownSolution );
                    }                    
                }
                logger.debug (" incumbent was updated to " + incumbentValue);
            }
            
            //if any partition is done, we stop the iterations
            if (activeSubtreeTWO.isUnFeasible()|| activeSubtreeTWO.isOptimal()) {
                greenFlagForIterations = false;
            }else{
                //check all the other partitions
                for (int partitionNumber = ONE;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){   
                    if (activeSubtreeCollectionList.get(partitionNumber-ONE).getPendingRawNodeCount() + activeSubtreeCollectionList.get(partitionNumber-ONE).getNumTrees() ==ZERO) {
                        greenFlagForIterations = false;
                        logger.info("This partition has no trees or raw nodes reamining: " + partitionNumber);
                    }
                }
            }
            
            //do another iteration involving every partition
            
        }//end - same iters as test 1        
        
        logger.debug(" Individual solve test ended at iteration Number "+iterationNumber);
        //print status of every partition
        for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
             
            if( partitionNumber == ZERO){
                ActiveSubtree tree = activeSubtreeTWO;

                double localMipGapPercent = (tree.isFeasible()||tree.isOptimal()) ? tree.getRelativeMIPGapPercent(false, -ONE):-ONE;
                double globalMipGapPercent = (incumbentValue < PLUS_INFINITY && incumbentValue > MINUS_INFINITY)?tree.getRelativeMIPGapPercent(true, incumbentValue):-ONE;
                long numLeafsReamining = tree.numActiveLeafsAfterSimpleSolve;
                long numLeafsReaminingLP = tree.numActiveLeafsWithGoodLPAfterSimpleSolve;

                //this is the seed CCA on this partition, home partition is of course seeded by 0
                String ccaSeedNodeID = ""+ZERO;
                if (partitionNumber != ZERO){
                    ccaSeedNodeID = tree.seedCCANodeID;
                } 
                logger.debug (" Partition "+partitionNumber + "  has local mipgap " + localMipGapPercent + " global mipgap " + globalMipGapPercent +
                        " and #leafs " + numLeafsReamining + " and good lp #leafs " + numLeafsReaminingLP + 
                        " and was seeded by CCA node " + ccaSeedNodeID  + " and has status "+tree.getStatus());
                tree.end();
            } else {
                ActiveSubtreeCollection astc= activeSubtreeCollectionList.get(partitionNumber-ONE);
                                
                double mipGapPercent =   incumbentValue < PLUS_INFINITY && incumbentValue > MINUS_INFINITY&& 
                                         astc.getNumTrees()+ astc.getPendingRawNodeCount()>ZERO   ? 
                                         astc.getRelativeMIPGapPercent():-ONE;
                long numLeafsReamining = astc.getNumActiveLeafs();
                long numLeafsReaminingLP = astc.getNumActiveLeafsWithGoodLP();
                logger.debug ("Partition "+partitionNumber + "  has mipgap " + mipGapPercent +
                        " and #leafs " + numLeafsReamining + " and good lp #leafs " + numLeafsReaminingLP +                         
                        " trees count " + astc.getNumTrees()+" raw nodes count "+ astc.getPendingRawNodeCount() + " max trees created " + astc.maxTreesCreatedDuringSolution);
                astc.endAll();
            }
            
        }
                
       //best estimate first
                        
       //test with hard mipslike b2c1s1
       //limit iters to 100*iter
       //extract 5 , 10 partitions
        
        logger.info("both parts of the tests completed");
        
    } //end main
        
    private static boolean isHaltFilePresent (){
        File file = new File("F:\\temporary files here\\haltfile.txt");
         
        return file.exists();
    }
}

