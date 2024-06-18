package com.cdcl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

public class Formula {

        private  List<HashSet<Integer>> Clauses = new ArrayList< HashSet<Integer>>();
        private  int NumVariables;

        private List<Integer> InitialUnits = new ArrayList<>();



        public Formula(List<HashSet<Integer>> clauses, int numvars){

            Clauses = clauses;
            NumVariables = numvars;

            for(HashSet<Integer> clause: Clauses){
                if (clause.size()==1){
                   
                    InitialUnits.addAll(clause);

                }
            }

        }

       



        private void eliminateTautologies(){

            List<HashSet<Integer>> oldClauses = new ArrayList< HashSet<Integer>>(Clauses);

            for( HashSet<Integer> clause: oldClauses){
                
                if ( isTautology(clause) ){
                    Clauses.remove(clause);
                    
                }
            }

        }


        public static boolean isTautology(HashSet<Integer> clause){
            // for (int var =1; var<=NumVariables;var++){

            //     if (clause.contains(var) && clause.contains(-var)){
            //         return true;
            //     }
            // }
            // return false;


            Iterator<Integer> iterator = clause.iterator();


            while (iterator.hasNext()){

                if ( clause.contains(-iterator.next()) ){
                    return true;
                }

            }

            return false;
    

        }


        public void optimise(){


            eliminateTautologies();;



        }


        public List<HashSet<Integer>> getClauses(){
            return Clauses;
        }

        public int getNumVariables(){
            return NumVariables;
        }

        public void OutputClauses(){
            System.out.println("CLAUSES");
            for( HashSet<Integer> clause : Clauses){
                System.out.println(clause);
                
            }
            System.out.println("=============================================================================");
        }

}
