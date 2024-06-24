package com.cdcl;

import java.io.FileReader;
import java.util.HashSet;

// import org.junit.Test;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;



public class FormulaTest {


  


    @Test
    public void noTautologiesTest() throws Exception{


        Formula f = Formula_IO.ReadFormula(new FileReader("/home/waqee/CDCL2/cdcl/src/Formulas/Test/no_tautologies.cnf"));


        for(HashSet<Integer> clause:f.getClauses()){

            assertFalse(  Formula.isTautology(clause) );


            
        }



    }

    @Test
    public void allTautologiesTest() throws Exception{
        Formula f = Formula_IO.ReadFormula(new FileReader("/home/waqee/CDCL2/cdcl/src/Formulas/Test/all_tautologies.cnf"));


        for(HashSet<Integer> clause:f.getClauses()){

            assertTrue ( Formula.isTautology(clause) );
            
        }
    }


    @Test 
    public void eliminateTautologiesNoTautolgiesTest() throws Exception{

        Formula f = Formula_IO.ReadFormula(new FileReader("/home/waqee/CDCL2/cdcl/src/Formulas/Test/no_tautologies.cnf"));

        

        f.optimise();

        for(HashSet<Integer> clause:f.getClauses()){

            assertFalse ( Formula.isTautology(clause) );
            
        }

        
    }

    @Test 
    public void eliminateTautologiesContainsTautolgiesTest() throws Exception{

        Formula f = Formula_IO.ReadFormula(new FileReader("/home/waqee/CDCL2/cdcl/src/Formulas/Test/all_tautologies.cnf"));

        f.optimise();

        for(HashSet<Integer> clause:f.getClauses()){

            assertFalse ( Formula.isTautology(clause) );
            
        }
    }


    




}
