package com.bit.cs.icfg;

import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import heros.SynchronizedBy;
import heros.solver.IDESolver;
import soot.*;
import soot.jimple.*;
import soot.jimple.toolkits.ide.icfg.AbstractJimpleBasedICFG;
import soot.jimple.toolkits.pointer.LocalMustNotAliasAnalysis;
import soot.options.Options;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;

/**
 * This is an implementation of AbstractJimpleBasedICFG that computes the ICFG on-the-fly. In other words,
 * it can be used without pre-computing a call graph. Instead this implementation resolves calls through Class
 * Hierarchy Analysis (CHA), as implemented through FastHierarchy. The CHA is supported by LocalMustNotAliasAnalysis, which
 * is used to determine cases where the concrete type of an object at an InvokeVirtual or InvokeInterface callsite is known.
 * In these cases the call can be resolved concretely, i.e., to a single target.
 * <p>
 * To be sound, for InvokeInterface calls that cannot be resolved concretely, OnTheFlyJimpleBasedICFG requires that
 * all classes on the classpath be loaded at least to signatures. This must be done before the FastHierarchy is computed such
 * that the hierarchy is intact. Clients should call {@link #loadAllClassesOnClassPathToSignatures()} to load all required classes
 * to this level.
 *
 * @see FastHierarchy#resolveConcreteDispatch(SootClass, SootMethod)
 * @see FastHierarchy#resolveAbstractDispatch(SootClass, SootMethod)
 */
public class SwrOnTheFlyICFG extends AbstractJimpleBasedICFG {

    @SynchronizedBy("by use of synchronized LoadingCache class")
    protected final LoadingCache<Body, LocalMustNotAliasAnalysis> bodyToLMNAA = IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Body, LocalMustNotAliasAnalysis>() {
        @Override
        public LocalMustNotAliasAnalysis load(Body body) throws Exception {
            return new LocalMustNotAliasAnalysis(getOrCreateUnitGraph(body), body);
        }
    });

    @SynchronizedBy("by use of synchronized LoadingCache class")
    protected final LoadingCache<Unit, Set<SootMethod>> unitToCallees =
            IDESolver.DEFAULT_CACHE_BUILDER.build(new CacheLoader<Unit, Set<SootMethod>>() {
                @Override
                public Set<SootMethod> load(Unit u) throws Exception {
                    Stmt stmt = (Stmt) u;
                    InvokeExpr ie = stmt.getInvokeExpr();
                    FastHierarchy fastHierarchy = Scene.v().getFastHierarchy();
                    //FIXME Handle Thread.start etc.
                    if (ie instanceof InstanceInvokeExpr) {
                        if (ie instanceof SpecialInvokeExpr) {
                            //special
                            return Collections.singleton(ie.getMethod());
                        } else {
                            //virtual and interface
                            InstanceInvokeExpr iie = (InstanceInvokeExpr) ie;
                            Local base = (Local) iie.getBase();
                            RefType concreteType = bodyToLMNAA.getUnchecked(unitToOwner.get(u)).concreteType(base, stmt);
                            if (concreteType != null) {
                                //the base variable definitely points to a single concrete type
                                SootMethod singleTargetMethod = fastHierarchy.resolveConcreteDispatch(concreteType.getSootClass(), iie.getMethod());
                                return Collections.singleton(singleTargetMethod);
                            } else {
                                SootClass baseTypeClass;
                                if (base.getType() instanceof RefType) {
                                    RefType refType = (RefType) base.getType();
                                    baseTypeClass = refType.getSootClass();
                                } else if (base.getType() instanceof ArrayType) {
                                    baseTypeClass = Scene.v().getSootClass("java.lang.Object");
                                } else if (base.getType() instanceof NullType) {
                                    //if the base is definitely null then there is no call target
                                    return Collections.emptySet();
                                } else {
                                    throw new InternalError("Unexpected base type:" + base.getType());
                                }
                                return fastHierarchy.resolveAbstractDispatch(baseTypeClass, iie.getMethod());
                            }
                        }
                    } else {
                        //static
                        return Collections.singleton(ie.getMethod());
                    }
                }
            });

    @SynchronizedBy("explicit lock on data structure")
    protected Map<SootMethod, Set<Unit>> methodToCallers = new HashMap<SootMethod, Set<Unit>>();

    public SwrOnTheFlyICFG(SootMethod... entryPoints) {
        this(Arrays.asList(entryPoints));
    }

    public SwrOnTheFlyICFG(Collection<SootMethod> entryPoints) {
        for (SootMethod m : entryPoints) {
            initForMethod(m);
        }
    }

    protected Body initForMethod(SootMethod m) {
        assert Scene.v().hasFastHierarchy();
        Body b = null;
        if (m.isConcrete()) {
            SootClass declaringClass = m.getDeclaringClass();
            ensureClassHasBodies(declaringClass);
            synchronized (Scene.v()) {
                b = m.retrieveActiveBody();
            }
            if (b != null) {
                for (Unit u : b.getUnits()) {
                    if (unitToOwner.put(u, b) != null) {
                        //if the unit was registered already then so were all units;
                        //simply skip the rest
                        break;
                    }
                }
            }
        }
        assert Scene.v().hasFastHierarchy();
        return b;
    }

    private synchronized void ensureClassHasBodies(SootClass cl) {
        assert Scene.v().hasFastHierarchy();
        if (cl.resolvingLevel() < SootClass.BODIES) {
            Scene.v().forceResolve(cl.getName(), SootClass.BODIES);
            Scene.v().getOrMakeFastHierarchy();
        }
        assert Scene.v().hasFastHierarchy();
    }

    @Override
    public Set<SootMethod> getCalleesOfCallAt(Unit u) {
        Set<SootMethod> targets = unitToCallees.getUnchecked(u);
        for (SootMethod m : targets) {
            addCallerForMethod(u, m);
            initForMethod(m);
        }
        return targets;
    }

    private void addCallerForMethod(Unit callSite, SootMethod target) {
        synchronized (methodToCallers) {
            Set<Unit> callers = methodToCallers.get(target);
            if (callers == null) {
                callers = new HashSet<Unit>();
                methodToCallers.put(target, callers);
            }
            callers.add(callSite);
        }
    }

    @Override
    public Set<Unit> getCallersOf(SootMethod m) {
        Set<Unit> callers = methodToCallers.get(m);
        return callers == null ? Collections.<Unit>emptySet() : callers;

//		throw new UnsupportedOperationException("This class is not suited for unbalanced problems");
    }

    public boolean UnitIsinApp(Unit unit) {

        if (IsAppMethod(unitToOwner.get(unit).getMethod())) {
            return true;
        }
        return false;
    }

    public boolean IsAppMethod(SootMethod sm) {
        //System.out.println(sm.getDeclaringClass().toString());
        //System.out.println(Scene.v().getMainClass().toString());
        return sm.getDeclaringClass().toString().equals(Scene.v().getMainClass().toString());
    }

    public boolean CalleeIsAppClass(Unit unit) {
        Collection<SootMethod> sms = getCalleesOfCallAt(unit);
        if (sms.size() <= 0) {
            return false;
        }
        for (SootMethod sm : sms) {
            if (!IsAppMethod(sm)) {
                return false;
            }
        }
        return true;
    }

    public HashMap<String, Set<String>> methodtoException;


    public static void loadAllClassesOnClassPathToSignatures() {
        for (String path : SourceLocator.explodeClassPath(Scene.v().getSootClassPath())) {
            for (String cl : SourceLocator.v().getClassesUnder(path)) {
                Scene.v().forceResolve(cl, SootClass.SIGNATURES);
            }
        }
    }

    public static void main(String[] args) {
        PackManager.v().getPack("wjtp").add(new Transform("wjtp.onflyicfg", new SceneTransformer() {

            @Override
            protected void internalTransform(String phaseName, Map<String, String> options) {
                if (Scene.v().hasCallGraph()) throw new RuntimeException("call graph present!");

                loadAllClassesOnClassPathToSignatures();

                SootMethod mainMethod = Scene.v().getMainMethod();
                SwrOnTheFlyICFG icfg = new SwrOnTheFlyICFG(mainMethod);
                Set<SootMethod> worklist = new LinkedHashSet<SootMethod>();
                Set<SootMethod> visited = new HashSet<SootMethod>();
                worklist.add(mainMethod);
                int monomorphic = 0, polymorphic = 0;
                while (!worklist.isEmpty()) {
                    Iterator<SootMethod> iter = worklist.iterator();
                    SootMethod currMethod = iter.next();
                    iter.remove();
                    visited.add(currMethod);
                    System.err.println(currMethod);
                    //MUST call this method to initialize ICFG for every method
                    Body body = currMethod.getActiveBody();
                    if (body == null) continue;
                    for (Unit u : body.getUnits()) {
                        Stmt s = (Stmt) u;
                        if (s.containsInvokeExpr()) {
                            Set<SootMethod> calleesOfCallAt = icfg.getCalleesOfCallAt(s);
                            if (s.getInvokeExpr() instanceof VirtualInvokeExpr || s.getInvokeExpr() instanceof InterfaceInvokeExpr) {
                                if (calleesOfCallAt.size() <= 1) monomorphic++;
                                else polymorphic++;
                                System.err.println("mono: " + monomorphic + "   poly: " + polymorphic);
                            }
                            for (SootMethod callee : calleesOfCallAt) {
                                if (!visited.contains(callee)) {
                                    System.err.println(callee);
                                    //worklist.add(callee);
                                }
                            }
                        }
                    }
                }
            }

        }));
        Options.v().set_on_the_fly(true);
        soot.Main.main(args);
    }

    public void InitmethodtoException(String file) {
        methodtoException = new HashMap<>();
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(file));
            String line = "";
            while ((line = br.readLine()) != null) {
                String[] strings = line.split("\t");
                if (strings.length > 1) {
                    HashSet<String> set = new HashSet<>();
                    for (int i = 1; i < strings.length; i++) {
                        set.add(strings[i]);
                    }
                    methodtoException.put(strings[0], set);
                }
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
//		System.out.println(methodtoException);
    }
}
