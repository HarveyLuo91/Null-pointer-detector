/* Soot - a J*va Optimization Framework
 * Copyright (C) 1997-2013 Eric Bodden and others
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the
 * Free Software Foundation, Inc., 59 Temple Place - Suite 330,
 * Boston, MA 02111-1307, USA.
 */
package com.bit.cs;

import com.bit.cs.ifds.NPDetector;
import com.bit.cs.model.FlowAbstraction;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import heros.solver.IFDSSolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;

import java.util.Set;


public class NPSolver<D, I extends InterproceduralCFG<Unit, SootMethod>> extends IFDSSolver<Unit, D, SootMethod, I> {

    private static Logger LOGGER = LoggerFactory.getLogger(NPSolver.class);
//	private static Logger LOGGER = Logger.getLogger();

    NPDetector rLeak;

    public NPSolver(IFDSTabulationProblem<Unit, D, SootMethod, I> problem) {
        super(problem);
        if (problem instanceof NPDetector) {
            rLeak = (NPDetector) problem;
        }
    }


    public void dumpResult() {
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod method : sootClass.getMethods()) {
                if (method.hasActiveBody()) {
                    LOGGER.info("-------------------dumpResult start - method:{}-----------------", method.getName());
//					System.out.println("------------------"+method.toString()+"-----------------");
                    Body body = method.getActiveBody();
                    LOGGER.info(body.toString());
                    PatchingChain<Unit> units = body.getUnits();
                    for (Unit unit : units) {
                        LOGGER.info("unit type:{}", unit.getClass());
//						System.out.println(unit.toString());
//                        Set result = this.ifdsResultsAt(unit);
//						System.out.println(result);
                    }
                    LOGGER.info("------------------dumpResult   end  - method:{}-----------------", method.getName());
                }
            }
        }
    }


    public void AnalysisResult() {
        for (SootClass sootClass : Scene.v().getApplicationClasses()) {
            for (SootMethod method : sootClass.getMethods()) {
                if (method.hasActiveBody()) {
                    System.out.println("------------------" + method.toString() + "-----------------");
                    Body body = method.getActiveBody();
                    //System.out.println(body);
                    System.out.println("last unit:" + body.getUnits().getLast());
                    Set<FlowAbstraction> result = (Set<FlowAbstraction>) this.ifdsResultsAt(body.getUnits().getLast());
                    for (FlowAbstraction fa : result) {
//						System.out.print(fa);
                        LOGGER.info("FlowAbstraction:{}", fa);
                        FlowAbstraction ff = fa;
                        while (ff.getPredecessor() != null) {
//							System.out.print(" -> "+ff.getPredecessor());
                            LOGGER.info("->{}", ff.getPredecessor());
                            ff = ff.getPredecessor();
                        }
                        System.out.println();
                    }
                    System.out.println("-----------------------------------------");
                }
            }
        }
    }


    public boolean Isreturn(FlowAbstraction flow, Set<FlowAbstraction> set) {
        if (flow.isIsreturn())
            return true;

        return false;
    }


}
