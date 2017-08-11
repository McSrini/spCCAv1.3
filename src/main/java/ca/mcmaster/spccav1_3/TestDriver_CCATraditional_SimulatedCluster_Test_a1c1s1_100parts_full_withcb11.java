/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ca.mcmaster.spccav1_3;

import static ca.mcmaster.spccav1_3.Constants.*;
import static ca.mcmaster.spccav1_3.Parameters.MIP_NAME_UNDER_TEST;
import static ca.mcmaster.spccav1_3.Parameters.RAMP_UP_TO_THIS_MANY_LEAFS;
import ca.mcmaster.spccav1_3.cb.CBInstructionTree;
import ca.mcmaster.spccav1_3.cca.CCANode;
import ca.mcmaster.spccav1_3.cplex.ActiveSubtree;
import ca.mcmaster.spccav1_3.cplex.ActiveSubtreeCollection;
import ca.mcmaster.spccav1_3.cplex.NodeSelectionStartegyEnum;
import static ca.mcmaster.spccav1_3.cplex.NodeSelectionStartegyEnum.STRICT_BEST_FIRST;
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
 *  glass4   , 50000:5000   0.3  - TEST 1 confirmed - took 7 hours
 
 *  glass4 1000000, 100 
 
 
 * 
 * run MIP on 5 simulated partitions
 * sub problems created using variable bound merging, and solved using traditional branch and bound
 * 2 ramp ups, one for using CCA and one without CCA
 */
public class TestDriver_CCATraditional_SimulatedCluster_Test_a1c1s1_100parts_full_withcb11 {
    
    private static  Logger logger = null;
    
    private static  int NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION = ZERO;
    
    //  50(20), 100(40) , 200(90), 250(112) parts is ok
    
    //wnq-n100-mw99-14  ru=5000, pa=100  size >= 50/4 yeilds 85 candidates with home=42   fast
    //p100x588b        ru=15000, pa=100  size >= 50/4 yeilds 97 candidates with home=53   fast
    //b2c1s1 ru=5000, pa=100  size >= 50/3 yeilds 94 candidates with home=48
    //seymour-disj-10 ru=5000, pa=100  size >= 50/4 yeilds 68 candidates with home=74
    //usAbbrv-8-25_70 ru=10000, pa=100  size >= 50/4 yeilds 96 candidates with home=77
    //neos-847302 ru=10000, pa=100  size >= 50/4 yeilds 94 candidates with home=50
    //janos-us-DDM ru=8000, pa=100  size >= 50/4 yeilds 90 candidates with home=30  fast
    //
    //seymour ru=8000, pa=100  size >= 50/4 yeilds 99 candidates with home=40
    //rococoB10-011000 ru=5000, pa=100  size >= 50/4 yeilds 95 candidates with home=19
    //  momentum1  ru=5000, pa=100  size >= 50/4 yeilds 90 candidates with home=88
    
     
    public static   String MIP_NAME_UNDER_TEST ="janos-us-DDM";
    public static   double MIP_WELLKNOWN_SOLUTION =  25687.9;
    public static   int RAMP_UP_TO_THIS_MANY_LEAFS = 8000;
     
 
    private static  int NUM_PARTITIONS = 100;
    private static double EXPECTED_LEAFS_PER_PARTITION = (RAMP_UP_TO_THIS_MANY_LEAFS +DOUBLE_ZERO)/NUM_PARTITIONS;
    
    //private static final int SOLUTION_CYCLE_Tu           fgggd hjhhIME_MINUTES = THREE;
     
    public static void main(String[] args) throws Exception {
        
        if (! isLogFolderEmpty()) {
            System.err.println("\n\n\nClear the log folder before starting the test.");
            exit(ONE);
        }
            
        logger=Logger.getLogger(TestDriver_CCATraditional_SimulatedCluster_Test_a1c1s1_100parts_full_withcb11.class);
        logger.setLevel(Level.DEBUG);
        PatternLayout layout = new PatternLayout("%5p  %d  %F  %L  %m%n");     
        try {
            RollingFileAppender rfa = new  RollingFileAppender(layout,LOG_FOLDER+TestDriver_CCATraditional_SimulatedCluster_Test_a1c1s1_100parts_full_withcb11.class.getSimpleName()+ LOG_FILE_EXTENSION);
            rfa.setMaxBackupIndex(TEN*TEN);
            logger.addAppender(rfa);
            logger.setAdditivity(false);
           
            
        } catch (Exception ex) {
            System.err.println("Exit: unable to initialize logging");        
            exit(1);
        }
         
        //first run 4 identical ramp ups
        MPS_FILE_ON_DISK =  "F:\\temporary files here\\"+MIP_NAME_UNDER_TEST+".mps";
        BackTrack=false;
        BAD_MIGRATION_CANDIDATES_DURING_TESTING = new ArrayList<String>();
        logger.debug ("starting ramp up") ;  
        ActiveSubtree activeSubtreeONE = new ActiveSubtree () ;
        activeSubtreeONE.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false); 
        ActiveSubtree activeSubtreeSBF = new ActiveSubtree () ;
        activeSubtreeSBF.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false); 
        //we do the same ramp up 2 more times        
        ActiveSubtree activeSubtreeBEF = new ActiveSubtree () ;
        activeSubtreeBEF.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false); 
        ActiveSubtree activeSubtreeLSI = new ActiveSubtree () ;
        activeSubtreeLSI.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false); 
        //another ramp up for CB
        ActiveSubtree activeSubtreeCB = new ActiveSubtree () ;
        activeSubtreeCB.solve( RAMP_UP_TO_THIS_MANY_LEAFS, PLUS_INFINITY, MILLION, true, false); 
        
        //verify activeSubtreeONE and activeSubtreeTWO identical ramp up
        logger.debug ("verify activeSubtreeONE and activeSubtreeTWO identical ramp up") ;   
        //there are probably better ways of doing this - I have temporarily edited the branch callback 
        List<String> nodeCreationInfoListONE = activeSubtreeONE.getNodeCreationInfoList();
        List<String> nodeCreationInfoListSBF = activeSubtreeSBF.getNodeCreationInfoList();
        List<String> nodeCreationInfoListBEF = activeSubtreeBEF.getNodeCreationInfoList();
        List<String> nodeCreationInfoListLSI = activeSubtreeLSI.getNodeCreationInfoList();
        List<String> nodeCreationInfoListCB = activeSubtreeCB.getNodeCreationInfoList();
        if (activeSubtreeONE.getMaxBranchingVars()!= activeSubtreeSBF.getMaxBranchingVars() || 
               nodeCreationInfoListONE.size()!= nodeCreationInfoListSBF.size()){
            logger.error ("ramp up not identical");
            exit(ONE);
        }
        if (activeSubtreeONE.getMaxBranchingVars()!= activeSubtreeBEF.getMaxBranchingVars() || 
               nodeCreationInfoListONE.size()!= nodeCreationInfoListBEF.size()){
            logger.error ("ramp up not identical");
            exit(ONE);
        }
        if (activeSubtreeONE.getMaxBranchingVars()!= activeSubtreeLSI.getMaxBranchingVars() || 
               nodeCreationInfoListONE.size()!= nodeCreationInfoListLSI.size()){
            logger.error ("ramp up not identical");
            exit(ONE);
        }        
        if (activeSubtreeONE.getMaxBranchingVars()!= activeSubtreeCB.getMaxBranchingVars() || 
               nodeCreationInfoListONE.size()!= nodeCreationInfoListCB.size()){
            logger.error ("ramp up not identical");
            exit(ONE);
        }
        
        
        //code needs cleanup!
        
        for (int index = ZERO; index < nodeCreationInfoListONE.size(); index ++){
            if (! nodeCreationInfoListONE.get(index).equals(nodeCreationInfoListSBF.get(index) )) {
                logger.error ("ramp up not identical - branching conditions vary");
                exit(ONE);
            }
            if (! nodeCreationInfoListONE.get(index).equals(nodeCreationInfoListBEF.get(index) )) {
                logger.error ("ramp up not identical - branching conditions vary");
                exit(ONE);
            }
            if (! nodeCreationInfoListONE.get(index).equals(nodeCreationInfoListLSI.get(index) )) {
                logger.error ("ramp up not identical - branching conditions vary");
                exit(ONE);
            }
            if (! nodeCreationInfoListONE.get(index).equals(nodeCreationInfoListCB.get(index) )) {
                logger.error ("ramp up not identical - branching conditions vary");
                exit(ONE);
            }
        }
        
        List<NodeAttachment> activeLeaflistONE = activeSubtreeONE.getActiveLeafList();
        List<NodeAttachment> activeLeaflistSBF = activeSubtreeSBF.getActiveLeafList();
        List<NodeAttachment> activeLeaflistBEF = activeSubtreeBEF.getActiveLeafList();
        List<NodeAttachment> activeLeaflistLSI = activeSubtreeLSI.getActiveLeafList();
        List<NodeAttachment> activeLeaflistCB = activeSubtreeCB.getActiveLeafList();
        if (activeLeaflistONE.size()!=activeLeaflistSBF.size()){
            logger.error ("ramp up not identical - active leaf counts vary");
            exit(ONE);
        }
        if (activeLeaflistONE.size()!=activeLeaflistBEF.size()){
            logger.error ("ramp up not identical - active leaf counts vary");
            exit(ONE);
        }
        if (activeLeaflistONE.size()!=activeLeaflistLSI.size()){
            logger.error ("ramp up not identical - active leaf counts vary");
            exit(ONE);
        }
        if (activeLeaflistONE.size()!=activeLeaflistCB.size()){
            logger.error ("ramp up not identical - active leaf counts vary");
            exit(ONE);
        }
        for (int index = ZERO; index < activeLeaflistONE.size(); index ++){
            if (! activeLeaflistSBF.get(index).nodeID.equals(activeLeaflistONE.get(index).nodeID )){
                logger.error ("ramp up not identical - active leaf ids vary");
                exit(ONE);
            }
            if (! activeLeaflistBEF.get(index).nodeID.equals(activeLeaflistONE.get(index).nodeID )){
                logger.error ("ramp up not identical - active leaf ids vary");
                exit(ONE);
            }
            if (! activeLeaflistLSI.get(index).nodeID.equals(activeLeaflistONE.get(index).nodeID )){
                logger.error ("ramp up not identical - active leaf ids vary");
                exit(ONE);
            }
            if (! activeLeaflistCB.get(index).nodeID.equals(activeLeaflistONE.get(index).nodeID )){
                logger.error ("ramp up not identical - active leaf ids vary");
                exit(ONE);
            }
        }
        
        
        logger.info("Ramp ups are identical, can proceed");
         
        //now extract CCA nodes from ramped up tree
        List<CCANode> acceptedCCANodes =new ArrayList<CCANode> () ;
        //here are the CB instructions for the accepted CCA nodes
        List< CBInstructionTree>  acceptedCCANodeInstructionTrees  =new ArrayList<CBInstructionTree > () ;                   
        int leafCountRemainingInHomePartition = (int) activeSubtreeONE.getActiveLeafCount();
          
        // we convert each accepted CCA node into an active subtree collection, for use in the second part of the test
        List<ActiveSubtreeCollection> activeSubtreeCollectionListSBF = new ArrayList<ActiveSubtreeCollection>();
        List<ActiveSubtreeCollection> activeSubtreeCollectionListBEF = new ArrayList<ActiveSubtreeCollection>();
        List<ActiveSubtreeCollection> activeSubtreeCollectionListLSI = new ArrayList<ActiveSubtreeCollection>();
        //here is the lest of leafs to be pruned from the home partition
        List<String> pruneListONE = new ArrayList<String>();
        List<String> pruneListSBF = new ArrayList<String>();
        List<String> pruneListBEF = new ArrayList<String>();
        List<String> pruneListLSI = new ArrayList<String>();
        List<String> pruneListCB  = new ArrayList<String>();
        
        //get CCA condidates
        //List<CCANode> candidateCCANodes = activeSubtreeONE.getCandidateCCANodes( LEAFS_PER_CCA );             
        List<CCANode> candidateCCANodes = activeSubtreeONE.getCandidateCCANodesPostRampup(NUM_PARTITIONS);    
        
        if (candidateCCANodes.size() < NUM_PARTITIONS) {
            logger.error("this splitToCCAPostRampup partitioning cannot be done  , try ramping up to  a larger number of leafs ");
            exit(ZERO);
        }
        
        //for every accepted CCA node, we create a active subtree collection that has all its leafs
        //
        //active subtree collection needs to be formed before the leafs are "pruned"
        for (CCANode ccaNode: candidateCCANodes){

            if (ccaNode.getPackingFactor() < TWO && ccaNode.pruneList.size() > EXPECTED_LEAFS_PER_PARTITION/FOUR ) {
                logger.debug (""+ccaNode.nodeID + " has good packing factor " +ccaNode.getPackingFactor() + 
                        " and prune list size " + ccaNode.pruneList.size() + " depth from root "+ ccaNode.depthOfCCANodeBelowRoot) ; 
                NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION ++;
                //          qxxy               dod       
                acceptedCCANodes.add(ccaNode);
                
                //get the CB instructions for each accepted CCA node
                CBInstructionTree tree = activeSubtreeONE.getCBInstructionTree(ccaNode);
                acceptedCCANodeInstructionTrees.add( tree); 
                tree.print();
                

                //add entry to  active Subtree Collection
                List<CCANode> ccaLeafNodeListSBF = activeSubtreeONE.getActiveLeafsAsCCANodes( ccaNode.pruneList);      
                List<CCANode> ccaLeafNodeListBEF = new ArrayList<CCANode> () ;
                List<CCANode> ccaLeafNodeListLSI = new ArrayList<CCANode> () ;
                ccaLeafNodeListBEF.addAll(ccaLeafNodeListSBF) ;
                ccaLeafNodeListLSI.addAll(ccaLeafNodeListSBF) ;
                
                ActiveSubtreeCollection astc = new ActiveSubtreeCollection (ccaLeafNodeListSBF, activeSubtreeONE.instructionsFromOriginalMip, -ONE, false, NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION) ;
                activeSubtreeCollectionListSBF.add(astc);
                //repeat for BEF and LSI
                astc = new ActiveSubtreeCollection (ccaLeafNodeListBEF, activeSubtreeONE.instructionsFromOriginalMip, -ONE, false, NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION) ;
                activeSubtreeCollectionListBEF .add(astc);
                //
                astc = new ActiveSubtreeCollection (ccaLeafNodeListLSI, activeSubtreeONE.instructionsFromOriginalMip, -ONE, false, NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION) ;
                activeSubtreeCollectionListLSI .add(astc);

                //prune leafs from active subtree
                //activeSubtreeONE.prune( ccaNode.pruneList, true);
                //prune the same leafs from the clone active subtree
                //activeSubtreeSBF.prune( ccaNode.pruneList, true);
                //activeSubtreeBEF.prune( ccaNode.pruneList, true);
                // activeSubtreeLSI.prune( ccaNode.pruneList, true);
                //since we are changing the branch handler, I am supplying the prune list to the new branch handler. This is not how it was initially supposed to be.
                pruneListONE.addAll( ccaNode.pruneList);
                pruneListSBF.addAll( ccaNode.pruneList);
                pruneListBEF.addAll( ccaNode.pruneList);
                pruneListLSI.addAll( ccaNode.pruneList);
                pruneListCB.addAll ( ccaNode.pruneList);

            }   
            if (NUM_CCA_NODES_ACCEPTED_FOR_MIGRATION >=NUM_PARTITIONS-1 )             break; //leave 1 node on home partition

        }
        activeSubtreeONE.prune(pruneListONE , true);
        leafCountRemainingInHomePartition = (int) activeSubtreeONE.getActiveLeafCount();
         
        
        //at this point, we have farmed out CCA nodes, and also
        //have the corresponding subtree collections for comparision [ each subtree collection has all the leafs of the corresponding CCA]                 
        logger.debug ("number of CCA nodes collected = "+acceptedCCANodes.size()) ;            
        for ( int index = ZERO; index <  acceptedCCANodes.size(); index++){
            logger.debug("accepted CCA node is : " + acceptedCCANodes.get(index)) ;
            logger.debug ("number of leafs in corresponding active subtree collection SBF is = " +     
                    (activeSubtreeCollectionListSBF.get(index).getPendingRawNodeCount() + activeSubtreeCollectionListSBF.get(index).getNumTrees()) );        
            logger.debug ("number of leafs in corresponding active subtree collection BEF is = " +     
                    (activeSubtreeCollectionListBEF.get(index).getPendingRawNodeCount() + activeSubtreeCollectionListBEF.get(index).getNumTrees()) );
            logger.debug ("number of leafs in corresponding active subtree collection LSI is = " +     
                    (activeSubtreeCollectionListLSI.get(index).getPendingRawNodeCount() + activeSubtreeCollectionListLSI.get(index).getNumTrees()) );
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
        //SolutionVector  bestKnownSolution = bestKnownSolutionAfterRampup ==null? null : activeSubtreeONE.getSolutionVector();
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
            
            if(isHaltFilePresent())  break; //halt!
            logger.debug("starting iteration Number "+iterationNumber);
                    
            //solve every partition for 3 minutes at a time
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){                                
                logger.debug("Solving partition for "+SOLUTION_CYCLE_TIME_MINUTES+" minutes ... Partition_" + partitionNumber );
                if (partitionNumber == ZERO ) logger.debug(" prune list size before is "+ pruneListONE.size());
                partitionList.get(partitionNumber).simpleSolve( SOLUTION_CYCLE_TIME_MINUTES ,  true,  false, partitionNumber == ZERO ? pruneListONE: null);                
                if (partitionNumber == ZERO ) logger.debug(" prune list size after is "+ pruneListONE.size());
            }
            
            //we are done when every partition has no active leafs left, i.e. its optimal or unfeasible
            greenFlagForIterations=false;
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
                if (!partitionList.get(partitionNumber).isUnFeasible() && !partitionList.get(partitionNumber).isOptimal()) {
                    //logger.debug("partition "+ partitionNumber + " solved. Stopping iterations at " + iterationNumber);
                    greenFlagForIterations=true;
                    break;
                } else {
                    logger.debug("partition "+ partitionNumber + " solved." + " Status is " +  partitionList.get(partitionNumber).getStatus());
                }
            }
                         
            //if better solution found on any partition, update incumbent, and supply MIP start to every partition
            int partitionWithIncumbentUpdate = -ONE;
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
                
                ActiveSubtree tree = partitionList.get(partitionNumber);
            
                if   ( tree.isFeasible()||tree.isOptimal())  {
                    if (  (!IS_MAXIMIZATION  && incumbentValue> tree.getObjectiveValue())  || (IS_MAXIMIZATION && incumbentValue< tree.getObjectiveValue()) ) {     
                        //bestKnownSolution =              tree.getSolutionVector();
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
                double globalMipGapPercent = (incumbentValue < PLUS_INFINITY && incumbentValue > MINUS_INFINITY)?
                                              tree.getRelativeMIPGapPercent(true, incumbentValue):-ONE;
                long numLeafsReamining = tree.numActiveLeafsAfterSimpleSolve;
                long numLeafsReaminingLP = tree.numActiveLeafsWithGoodLPAfterSimpleSolve;
                
                //this is the seed CCA on this partition, home partition is of course seeded by 0
                String ccaSeedNodeID = ""+ZERO;
                if (partitionNumber != ZERO){
                    ccaSeedNodeID = tree.seedCCANodeID;
                } 
                logger.debug ("partition "+partitionNumber + "  has local mipgap " + localMipGapPercent + " global mipgap " + globalMipGapPercent +
                        " and #leafs " + numLeafsReamining + " and good lp #leafs " + numLeafsReaminingLP + 
                        " and was seeded by CCA node " + ccaSeedNodeID  + " and has status "+tree.getStatus());
                tree.end();
        }
        
          
        //test 1A, with CB
        //everything same as CCA, but 1st iteration does reincarnation
        // 
        //we allow upto 100 more iterations to see if a partition completes
        final int maxIterationsAllowedWithIndividualLeafs =  Math.min(TWO*iterationNumber , iterationNumber + TEN*TWO);//+ HUNDRED ;
        //re-init incumbentValue to incumbentValueAfterRampup;
        incumbentValue= incumbentValueAfterRampup;
        iterationNumber=ZERO;
        greenFlagForIterations = true;
        //create 1 tree per partition
        //note that we have the home partition plus as many CCA nodes as we have accepted for migration
        partitionList = new ArrayList<ActiveSubtree> (NUM_PARTITIONS);
        partitionList.add(activeSubtreeCB  ); //home MIP
        //
        //now add the farmed out CCA nodes
        for (CCANode ccaNode :acceptedCCANodes ){
            ActiveSubtree treeStraight = new ActiveSubtree() ;
            treeStraight.mergeVarBounds(ccaNode, activeSubtreeONE.instructionsFromOriginalMip, false );
            if (bestKnownSolutionAfterRampup!=null) treeStraight.setCutoffValue(incumbentValueAfterRampup ); //setMIPStart(bestKnownSolution);
            partitionList.add(treeStraight);
        }
        
        for (; greenFlagForIterations && iterationNumber<maxIterationsAllowedWithIndividualLeafs;iterationNumber++){ //while green flag, i.e. while no partition is complete
            
            if(isHaltFilePresent())  break; //halt!
            logger.debug("starting iteration Number "+iterationNumber);
                    
            //solve every partition for 3 minutes at a time
            for (int partitionNumber = NUM_PARTITIONS-ONE;partitionNumber >= ZERO; partitionNumber-- ){                                
                logger.debug("Solving partition for "+SOLUTION_CYCLE_TIME_MINUTES+" minutes ... Partition_" + partitionNumber );
                if (partitionNumber == ZERO ) {
                    logger.debug(" prune list size before is "+ pruneListCB.size());
                    partitionList.get(partitionNumber).simpleSolve( SOLUTION_CYCLE_TIME_MINUTES ,  true,  false,  pruneListCB );    
                    logger.debug(" prune list size after is "+ pruneListCB.size());
                }else {
                    if (iterationNumber!=ZERO){
                        //just solve
                        partitionList.get(partitionNumber).simpleSolve( SOLUTION_CYCLE_TIME_MINUTES ,  true,  false,  null);
                    }else {
                        // reincarnate with CB
                        CBInstructionTree tree =acceptedCCANodeInstructionTrees.get(partitionNumber-ONE);
                        partitionList.get(partitionNumber).reincarnate( tree.asMap(),acceptedCCANodes.get(partitionNumber-ONE).nodeID  , 
                                PLUS_INFINITY , false);
                        logger.debug("Reincarnated partition " + partitionNumber + " with CCA seed " + acceptedCCANodes.get(partitionNumber-ONE).nodeID +
                                " has this many leafs " + partitionList.get(partitionNumber).getActiveLeafCount());
                    }   
                }
            }
            
            //we are done when every partition has no active leafs left, i.e. its optimal or unfeasible
            greenFlagForIterations=false;
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
                if (!partitionList.get(partitionNumber).isUnFeasible() && !partitionList.get(partitionNumber).isOptimal()) {
                    //logger.debug("partition "+ partitionNumber + " solved. Stopping iterations at " + iterationNumber);
                    greenFlagForIterations=true;
                    break;
                } else {
                    logger.debug("partition "+ partitionNumber + " solved." + " Status is " +  partitionList.get(partitionNumber).getStatus());
                }
            }
                         
            //if better solution found on any partition, update incumbent, and supply MIP start to every partition
            int partitionWithIncumbentUpdate = -ONE;
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
                
                ActiveSubtree tree = partitionList.get(partitionNumber);
            
                if   ( tree.isFeasible()||tree.isOptimal())  {
                    if (  (!IS_MAXIMIZATION  && incumbentValue> tree.getObjectiveValue())  || (IS_MAXIMIZATION && incumbentValue< tree.getObjectiveValue()) ) {     
                        //bestKnownSolution =              tree.getSolutionVector();
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
        
        logger.debug(" CB test ended at iteration Number "+iterationNumber);
        //for every partition , print mip gap and # of leafs reamining, then end every partition
        for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){
                
                ActiveSubtree tree = partitionList.get(partitionNumber);
                
                double localMipGapPercent = (tree.isFeasible()||tree.isOptimal()) ? tree.getRelativeMIPGapPercent(false, -ONE):-ONE;
                double globalMipGapPercent = (incumbentValue < PLUS_INFINITY && incumbentValue > MINUS_INFINITY)?
                                              tree.getRelativeMIPGapPercent(true, incumbentValue):-ONE;
                long numLeafsReamining = tree.numActiveLeafsAfterSimpleSolve;
                long numLeafsReaminingLP = tree.numActiveLeafsWithGoodLPAfterSimpleSolve;
                
                //this is the seed CCA on this partition, home partition is of course seeded by 0
                String ccaSeedNodeID = ""+ZERO;
                if (partitionNumber != ZERO){
                    ccaSeedNodeID = tree.seedCCANodeID;
                } 
                logger.debug ("partition "+partitionNumber + "  has local mipgap " + localMipGapPercent + " global mipgap " + globalMipGapPercent +
                        " and #leafs " + numLeafsReamining + " and good lp #leafs " + numLeafsReaminingLP + 
                        " and was seeded by CCA node " + ccaSeedNodeID  + " and has status "+tree.getStatus());
                tree.end();
        }
               
        
        //HERE is part 2 of the test, where we run individual leafs and compare results with CCA
        //Note that the home partition continues to be a single tree, although the clone is used because the original home partition has already been solved
        //other partitions   are already the created , namely activeSubtreeCollectionList  
        
           
        List<ActiveSubtreeCollection> activeSubtreeCollectionList =null;
        ActiveSubtree homePartitionActiveSubTree = null;
        List<String> pruneList=null;
        //repeat test for all node selection strategies
        for(NodeSelectionStartegyEnum nodeSelectionStrategy  :NodeSelectionStartegyEnum.values()){
            if(NodeSelectionStartegyEnum.STRICT_BEST_FIRST.equals(nodeSelectionStrategy )){
                activeSubtreeCollectionList= activeSubtreeCollectionListSBF;
                homePartitionActiveSubTree= activeSubtreeSBF;
                pruneList=pruneListSBF;
            }else if (NodeSelectionStartegyEnum.BEST_ESTIMATE_FIRST.equals( nodeSelectionStrategy)){
                activeSubtreeCollectionList=activeSubtreeCollectionListBEF;
                homePartitionActiveSubTree= activeSubtreeBEF;
                pruneList=pruneListBEF;
            }else {
                activeSubtreeCollectionList=activeSubtreeCollectionListLSI;
                homePartitionActiveSubTree= activeSubtreeLSI;
                pruneList=pruneListLSI;
            }
            
            //TEST 2 , 3 , 4 are the same, except for node selection strategy
            logger.info(" \n\n\ntest started for Selection Strategy " + nodeSelectionStrategy  );
            
            //re-init incumbent, and set cutoff on each partition
            incumbentValue= incumbentValueAfterRampup;
            for (ActiveSubtreeCollection astc : activeSubtreeCollectionList){
                if (bestKnownSolutionAfterRampup!=null) astc.setCutoff(incumbentValue);
            }
                        
            greenFlagForIterations = true;

            for (iterationNumber=ZERO; greenFlagForIterations &&iterationNumber<maxIterationsAllowedWithIndividualLeafs; iterationNumber++){ //same # of iterations as test 1

                if(isHaltFilePresent())  break;//halt
                logger.debug("starting test2 iteration Number "+iterationNumber);

                //solve every partition for 3 minutes at a time
                for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){                                

                    logger.debug("Solving partition for "+ SOLUTION_CYCLE_TIME_MINUTES+" minutes ... Partition_" + partitionNumber );
                    if(partitionNumber==ZERO){
                        homePartitionActiveSubTree.simpleSolve( SOLUTION_CYCLE_TIME_MINUTES ,  true,  false,   pruneList);  
                    }else{
                        activeSubtreeCollectionList.get(partitionNumber-ONE).solve( true, SOLUTION_CYCLE_TIME_MINUTES  ,     
                                true,    TIME_SLICE_IN_MINUTES_PER_ACTIVE_SUBTREE,  nodeSelectionStrategy  );
                    }                
                }

                //if better solution found on any partition, update incumbent, and supply MIP start to every partition
                int partitionWithIncumbentUpdate = -ONE;
                for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ )/*]]*/{

                    double challengerToIncumbent =ZERO ;

                    if (partitionNumber == ZERO){
                        if(homePartitionActiveSubTree.isFeasible() || homePartitionActiveSubTree.isOptimal())     
                            challengerToIncumbent=   homePartitionActiveSubTree.getObjectiveValue();
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
                            homePartitionActiveSubTree.setCutoffValue( incumbentValue);
                        }else{
                            activeSubtreeCollectionList.get(partitionNumber-ONE).setCutoff(incumbentValue );//   setMIPStart(bestKnownSolution );
                        }                    
                    }
                    logger.debug (" incumbent was updated to " + incumbentValue);
                }

                //if every partition is done, we stop the iterations
                greenFlagForIterations = false;
                if (homePartitionActiveSubTree.isUnFeasible()|| homePartitionActiveSubTree.isOptimal()) {
                     
                    //check all the other partitions
                    for (int partitionNumber = ONE;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){   
                        if (activeSubtreeCollectionList.get(partitionNumber-ONE).getPendingRawNodeCount() + activeSubtreeCollectionList.get(partitionNumber-ONE).getNumTrees() ==ZERO) {
                            logger.info("This partition has no trees or raw nodes reamining: " + partitionNumber);
                        } else {
                            greenFlagForIterations = true;
                            break;
                        }
                    }
                } else {
                    greenFlagForIterations = true;
                }

                //do another iteration involving every partition

            }//end - 100 more iters than test 1      

            logger.debug(" Individual solve test ended at iteration Number "+iterationNumber);
            //print status of every partition
            for (int partitionNumber = ZERO;partitionNumber < NUM_PARTITIONS; partitionNumber++ ){

                if( partitionNumber == ZERO){
                    ActiveSubtree tree = homePartitionActiveSubTree;

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
                    logger.debug ("partition "+partitionNumber + "  has mipgap " + mipGapPercent +
                            " and #leafs " + numLeafsReamining + " and good lp #leafs " + numLeafsReaminingLP +                         
                            " trees count " + astc.getNumTrees()+" raw nodes count "+ astc.getPendingRawNodeCount() + " max trees created " + astc.maxTreesCreatedDuringSolution);
                    astc.endAll();
                }

            }//print status of every partition
            
            logger.info(" test completed Selection Strategy for " + nodeSelectionStrategy);
            
        }//for all node sequencing strategies
        
        
        logger.info("all parts of the test completed");
        
    } //end main
        
    private static boolean isHaltFilePresent (){
        File file = new File("F:\\temporary files here\\haltfile.txt");
         
        return file.exists();
    }
    
    private static boolean isLogFolderEmpty() {
        File dir = new File (LOG_FOLDER );
        return (dir.isDirectory() && dir.list().length==ZERO);
    }
}


