package com.cdcl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class TwoWatchTest {


    @Test
    void testUpdateWatchedLiterals1() throws Exception {

        Formula formula = Formula_IO.ReadFormula("/home/waqee/CDCL2/cdcl/src/Formulas/Test/two_watch1.cnf");



        // Solver solver = new Solver("/home/waqee/CDCL2/cdcl/src/Formulas/Test/no_tautologies.cnf");

        TwoWatch twowatch = new TwoWatch(formula);

        List<Integer> partial_assignment = new ArrayList<Integer>();

        Set<Integer> units = new HashSet<Integer>();

        assertFalse(twowatch.UpdateWatchedLiterals(-2, partial_assignment, units));
        System.out.println(units);

        assertEquals( units,  new HashSet<Integer>(Arrays.asList(1,-1) )  );

        // assertTrue( units.equals( new HashSet<Integer>(Arrays.asList(1,-1) ) ) );
        


    }

    @Test
    void testUpdateWatchedLiterals2() throws Exception {

        Formula formula = Formula_IO.ReadFormula("/home/waqee/CDCL2/cdcl/src/Formulas/Test/two_watch1.cnf");




        TwoWatch twowatch = new TwoWatch(formula);

        List<Integer> partial_assignment = new ArrayList<Integer>();

        Set<Integer> units = new HashSet<Integer>();

        
        partial_assignment.add(1);
        assertFalse(twowatch.UpdateWatchedLiterals(1, partial_assignment, units));
        partial_assignment.add(-1);
        assertTrue(twowatch.UpdateWatchedLiterals(-1, partial_assignment, units));


        // assertEquals( units,  new HashSet<Integer>(Arrays.asList(1,-1) )  );

        // assertTrue( units.equals( new HashSet<Integer>(Arrays.asList(1,-1) ) ) );
        


    }




}
