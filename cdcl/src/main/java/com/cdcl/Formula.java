package com.cdcl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class Formula {
        private  List<HashSet<Integer>> Clauses = new ArrayList< HashSet<Integer>>();
        private  int NumVariables;



        public Formula(List<HashSet<Integer>> clauses, int numvars){

            Clauses = clauses;
            NumVariables = numvars;

        }




        private void eliminateTautologies(){

            List<HashSet<Integer>> oldClauses = new ArrayList< HashSet<Integer>>(Clauses);

            for( HashSet<Integer> clause: oldClauses){
                
                if ( isTautology(clause) ){
                    Clauses.remove(clause);
                    
                }
            }

        }


        private boolean isTautology(HashSet<Integer> clause){
            for (int var =1; var<=NumVariables;var++){

                if (clause.contains(var) && clause.contains(-var)){
                    return true;
                }
            }
            return false;
    

        }


        public void optimise(){


            eliminateTautologies();;



        }

}
