package com.bit.cs;

import com.bit.cs.ifds.NPDetector;
import heros.IFDSTabulationProblem;
import heros.InterproceduralCFG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.options.Options;

public class Main {

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        G.reset();
        initialiseSoot("example.NullPointerCase1");

        SootMethod mainMethod = Scene.v().getMainMethod();
        //		LOGGER.info(mainMethod.toString());
        SwrOnTheFlyICFG icfg = new SwrOnTheFlyICFG(mainMethod);
//        icfg.InitmethodtoException("Input/MethodException");
        IFDSTabulationProblem<Unit, ?, SootMethod, InterproceduralCFG<Unit, SootMethod>> problem = new NPDetector(icfg);

        @SuppressWarnings({"rawtypes", "unchecked"})
        NPSolver<?, InterproceduralCFG<Unit, SootMethod>> solver = new NPSolver(problem);
        LOGGER.info("start solver---------------");
        solver.solve();
        solver.dumpResult();
        solver.AnalysisResult();
    }

    public static void initialiseSoot(String mainclass) {
        LOGGER.info("initialiseSoot");

//        setClassPath();
        Scene.v().setSootClassPath("target\\classes");//C:\Users\Luo\program\华为项目\src\ifds\bin
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_prepend_classpath(true);
        Options.v().set_validate(true);
        Options.v().set_output_format(Options.output_format_jimple);
        Options.v().set_src_prec(Options.src_prec_java);

        Options.v().set_keep_line_number(true);
        Options.v().set_whole_program(true);

        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_app(true);

        SootClass appclass = Scene.v().loadClassAndSupport(mainclass);

        Scene.v().setMainClass(appclass);
//		Scene.v().addBasicClass("java.io.PrintStream",SootClass.SIGNATURES);
//		Scene.v().addBasicClass("java.io.FileOutputStream",SootClass.SIGNATURES);
//		Scene.v().addBasicClass("java.lang.System",SootClass.SIGNATURES);
//		Scene.v().addBasicClass("java.lang.Thread",SootClass.SIGNATURES);
        Scene.v().loadNecessaryClasses();
    }


    public static void setClassPath() {
        String javapath = System.getProperty("java.class.path");
        String path = javapath;
        Scene.v().setSootClassPath(path);
        //System.out.println("path"+path);
    }

    public static void loadAllClassesOnClassPathToSignatures() {
        for (String path : SourceLocator.explodeClassPath(Scene.v().getSootClassPath())) {
            for (String cl : SourceLocator.v().getClassesUnder(path)) {
                Scene.v().forceResolve(cl, SootClass.SIGNATURES);
            }
        }
    }
}
