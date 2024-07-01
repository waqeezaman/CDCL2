package com.cdcl;

import java.util.ArrayList;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;



public class TwoWatch {
    

    // which stores the literals in the formula and the clauses that they are being watched in 
    private static HashMap<Integer,ArrayList<Integer>> Literal_To_Clause= new HashMap<Integer,ArrayList<Integer>>();

    // which stores clauses and the literals in them that are being watched
    private static HashMap<Integer, int[]> Clause_To_Literal= new HashMap<Integer,int[]>();

    private Formula Formula;




    public TwoWatch(Formula formula){

        Formula = formula;

        // for each literal, initialise list of clauses, where literal is being watched 
        for(int l=1; l<=formula.getNumVariables(); l++){
            Literal_To_Clause.put(l, new ArrayList<>());
            Literal_To_Clause.put(-l, new ArrayList<>());
        }

        for( int c=0; c<formula.getClauses().size(); c++){

            int[] watch_literals = new int[2]  ;
            
            List<Integer> literals_in_clause = new ArrayList<>(formula.getClauses().get(c));

            if (literals_in_clause.size()>=2){
                watch_literals[0] = literals_in_clause.get(0);
                watch_literals[1] = literals_in_clause.get(1);
            }
            else{

                // if clause is a unit clause
                watch_literals[0] = literals_in_clause.get(0);
                watch_literals[1] = literals_in_clause.get(0);

            }

            Clause_To_Literal.put(c,watch_literals);

            Literal_To_Clause.get(watch_literals[0]).add(c);
            Literal_To_Clause.get(watch_literals[1]).add(c);



        }


      
        
        

    }

    




    /**
     * @param added_literal , literal that has just been made true
     * @param partial_assignment 
     * @param new_units , pass in empty list, will be filled with new unit literals that are found, if a conflict occurs, this can be ignored
     * @return  , boolean indicating whether a conflict has occurred
     */
    public Integer UpdateWatchedLiterals(  Integer added_literal,
                                                    HashSet<Integer> partial_assignment_set,
                                                    List<Integer> new_units,
                                                    HashMap<Integer, List<Integer> > literal_to_inference_clauses){


        int affected_literal = -added_literal;

        
        for( int i = Literal_To_Clause.get(affected_literal).size()-1; i >= 0; i--){
        
            Integer affected_clause = Literal_To_Clause.get(affected_literal).get(i);
            int watch_literal_2 = 0;
            
            if (affected_literal == Clause_To_Literal.get(affected_clause)[0]){
                watch_literal_2 =   Clause_To_Literal.get(affected_clause)[1];  
            }
            else if (affected_literal == Clause_To_Literal.get(affected_clause)[1]){
                watch_literal_2 =   Clause_To_Literal.get(affected_clause)[0];  
            }
            

            int[] two_watch_update = findUnwatchedLiteral(              affected_literal,
                                                                        watch_literal_2,
                                                                        Formula.getClauses().get(affected_clause),
                                                                        affected_clause,
                                                                        partial_assignment_set
                                                                    );

            
            // conflict has occured
            if( two_watch_update[0] == 1 ){
               
                

                // returning clause responsible for conflict 
                return affected_clause;
            } 

            // check if new unit literal found 
            else if( two_watch_update[1] != 0 ){

                // if we havent already found this unit literal already, add it to the set of new units
                if ( !new_units.contains(two_watch_update[1]) ){
                    new_units.add( two_watch_update[1] );
                }
                
                // add the clause in which it was found, as in inference clause
                literal_to_inference_clauses.get(two_watch_update[1]).add(affected_clause);

            }
          





     



        }


   

        // no conflict found 
        return null;


    }




    /**
     * @param watched_literal1
     * @param watched_literal2
     * @param clause
     * @param partial_assignment
     * 
     * Method to reassign watched literals after a literal has been made False
     * 
     * If the first element of the returned array is a 1, that means a conflct has occurred
     * The second element of the array is used to specify a new unit literal, if one has been found 
     * If no new unit literal has been found the second element is set to zero 
     * 
     * @return returns 2 element array, indicating whether a conflict has occured and new unit literals
     */
    private int[] findUnwatchedLiteral( int watched_literal1,
                                        int watched_literal2,
                                        HashSet<Integer> clause,
                                        Integer affected_clause_index,
                                        HashSet<Integer> partial_assignment_set){

        for(int l: clause){

            // this where we find a new watch literal 
            if( !partial_assignment_set.contains(-l) && watched_literal1!=l && watched_literal2!=l ){
                
                // need to update the two watch structure here 
                Literal_To_Clause.get(watched_literal1).remove(affected_clause_index);
                Literal_To_Clause.get(l).add(affected_clause_index);
                
                if ( Clause_To_Literal.get(affected_clause_index)[0] == watched_literal1){
                    Clause_To_Literal.get(affected_clause_index)[0] = l;
                }
                else{
                    Clause_To_Literal.get(affected_clause_index)[1] = l;
                }
                
                return new int[]{0,0};

            }

        }

        // check that watch literal 2 is unassigned, if so this clause is now a unit clause
        // since all literals must be false except watch literal 2
        if( !partial_assignment_set.contains(watched_literal2) && !partial_assignment_set.contains(-watched_literal2) ){

            // this is a new unit clause
            return new int[]{0,watched_literal2};
        }

        if( partial_assignment_set.contains(-watched_literal2)){

            
            // a conflict has occurred
            return new int[]{1,0};
        }

        


        // this is the case where watched_literal_2 is set to true, and all variables are false
        return new int[]{0,0};
    }



    public void AddLearnedClause( Integer clause_index, Integer last_decision){

        


        // clause is a unit clause 
        if( Formula.getClauses().get(clause_index).size() == 1 ){
            
            Clause_To_Literal.put(clause_index, new int[]{last_decision,last_decision} );
            Literal_To_Clause.get(last_decision).add(clause_index);
        }
        else{

            Integer other_literal = 0; 

            for (Integer literal : Formula.getClauses().get(clause_index)) {
                
                if ( literal!= last_decision){
                    other_literal = literal;
                    break;
                }

            }


            Clause_To_Literal.put(clause_index, new int[]{last_decision, other_literal});
            
            Literal_To_Clause.get(last_decision).add( clause_index);
            Literal_To_Clause.get(other_literal).add(clause_index);



        }


    }



}
