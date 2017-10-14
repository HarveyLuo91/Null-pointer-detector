package com.bit.cs.ifds;

import com.bit.cs.SwrOnTheFlyICFG;
import com.bit.cs.model.FlowAbstraction;
import com.bit.cs.model.NullFlowAbstraction;
import com.bit.cs.model.SwrLocal;
import heros.DefaultSeeds;
import heros.FlowFunction;
import heros.FlowFunctions;
import heros.InterproceduralCFG;
import heros.flowfunc.KillAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import soot.*;
import soot.jimple.*;
import soot.jimple.internal.*;
import soot.jimple.toolkits.ide.DefaultJimpleIFDSTabulationProblem;

import java.util.*;


public class NPDetector extends DefaultJimpleIFDSTabulationProblem<NullFlowAbstraction, InterproceduralCFG<Unit, SootMethod>> {

    private static Logger LOGGER = LoggerFactory.getLogger(NPDetector.class);

    public SwrOnTheFlyICFG icfg;

    public NPDetector(InterproceduralCFG<Unit, SootMethod> icfg) {
        super(icfg);
        if (icfg instanceof SwrOnTheFlyICFG) {
            this.icfg = (SwrOnTheFlyICFG) icfg;
        }


    }

    @Override
    public Map<Unit, Set<NullFlowAbstraction>> initialSeeds() {
        return DefaultSeeds.make(Collections.singleton(Scene.v().getMainMethod().getActiveBody().getUnits().getFirst()), zeroValue());
    }

    @Override
    protected NullFlowAbstraction createZeroValue() {
        return NullFlowAbstraction.zeroAbstraction();
    }

    @Override
    protected FlowFunctions<Unit, NullFlowAbstraction, SootMethod> createFlowFunctionsFactory() {
        return new FlowFunctions<Unit, NullFlowAbstraction, SootMethod>() {

            @Override
            public FlowFunction<NullFlowAbstraction> getNormalFlowFunction(Unit curr, Unit succ) {
                return DealNormalFlowFunction(curr, succ);
            }

            @Override
            public FlowFunction<NullFlowAbstraction> getCallFlowFunction(Unit callStmt, SootMethod destinationMethod) {
                return DealCallFlowFunction(callStmt, destinationMethod);
            }

            @Override
            public FlowFunction<NullFlowAbstraction> getReturnFlowFunction(Unit callSite, SootMethod calleeMethod,
                                                                           Unit exitStmt, Unit returnSite) {
                return DealReturnFlowFunction(callSite, calleeMethod, exitStmt, returnSite);
            }

            @Override
            public FlowFunction<NullFlowAbstraction> getCallToReturnFlowFunction(Unit callSite, Unit returnSite) {
                return DealCallToReturnFlowFunction(callSite, returnSite);
            }

        };
    }


    public FlowFunction<NullFlowAbstraction> DealNormalFlowFunction(final Unit src, Unit dest) {
        final Stmt stmt = (Stmt) src;
        final Stmt stmtd = (Stmt) dest;

//        LOGGER.info("normal Flow Funtion!");
//        LOGGER.info("src stmt:{}  type:{}", src.toString(), src.getClass());
//        LOGGER.info("dest stmt:{} type:{}", dest.toString(), dest.getClass());

        if (stmt instanceof AssignStmt) {
            final AssignStmt assignStmt = (AssignStmt) stmt;
            final Value left = assignStmt.getLeftOp();
            final Value right = assignStmt.getRightOp();

            return new FlowFunction<NullFlowAbstraction>() {

                @Override
                public Set<NullFlowAbstraction> computeTargets(NullFlowAbstraction source) {

                    Set<NullFlowAbstraction> outSet = new HashSet<NullFlowAbstraction>();

                    // If the right side is tainted, we need to taint the left side as well
                    // A a; Local ; Static ; Array

                    NullFlowAbstraction fa = getTaint(right, left, source, src);
                    if (fa != null) {
//                        fa.getState().addAll(source.getState()); //for 别名分析
//                        fa.getState().add(fa.getSwrLocal());
                        outSet.add(fa);
                    }
                    return outSet;
//                    //别名分析删掉被重写的fact
//                    SwrLocal frm = null;
//                    SwrLocal feq = null;
//                    boolean rightSideMatche = false;
//                    boolean leftSideMatche = false;
//                    for (SwrLocal f : source.getState()) {
//                        if (left instanceof Local && f.getLocal() == left) {
//                            leftSideMatche = true;
//                            frm = f;
//                        } else if (left instanceof InstanceFieldRef) {
//                            InstanceFieldRef ifr = (InstanceFieldRef) left;
//                            if (f.hasPrefix(ifr)) {
//                                leftSideMatche = true;
//                                frm = f;
//                            }
//                        } else if (left instanceof StaticFieldRef) {
//                            StaticFieldRef sfr = (StaticFieldRef) left;
//                            if (f.hasPrefix(sfr)) {
//                                leftSideMatche = true;
//                                frm = f;
//                            }
//                        } else if (left instanceof ArrayRef) {
//                            ArrayRef arrayRef = (ArrayRef) left;
//                            if (f.getLocal() != null && arrayRef.getBase().equals(f.getLocal()) && f.getIndex().equals(Integer.parseInt(arrayRef.getIndex().toString()))) {
//                                leftSideMatche = true;
//                                frm = f;
//                            }
//                        }
//
//                        if (right instanceof Local && f.getLocal() == right) {
//                            rightSideMatche = true;
//                            feq = f;
//                        } else if (right instanceof InstanceFieldRef) {
//                            InstanceFieldRef ifr = (InstanceFieldRef) right;
//                            if (f.hasPrefix(ifr)) {
//                                rightSideMatche = true;
//                                feq = f;
//                            }
//                        } else if (right instanceof StaticFieldRef) {
//                            StaticFieldRef sfr = (StaticFieldRef) right;
//                            if (f.hasPrefix(sfr)) {
//                                rightSideMatche = true;
//                                feq = f;
//                            }
//                        } else if (right instanceof ArrayRef) {
//                            ArrayRef arrayRef = (ArrayRef) right;
//                            if (f.getLocal() != null && arrayRef.getBase().equals(f.getLocal()) && f.getIndex().equals(Integer.parseInt(arrayRef.getIndex().toString()))) {
//                                rightSideMatche = true;
//                                feq = f;
//                            }
//                        }
//                        if (leftSideMatche && rightSideMatche) break;
//                    }
//                    if (leftSideMatche && !rightSideMatche) {
//                        source.getState().remove(frm);
//                    }
//                    if (rightSideMatche && !leftSideMatche) {
//                        source.getState().add(SwrLocal.v(left, icfg.getMethodOf(src), feq.getIndex()));
//                    }
//                    outSet.add(source.deriveWithNewStmt(icfg.getMethodOf(src)));
//                    return outSet;
                }
            };

//        if (stmt instanceof AssignStmt) {
//            Value left = ((AssignStmt) stmt).getLeftOp();
//            if (left instanceof Local) {
//                LOGGER.info(((Local) left).getName());
//            }
////            LOGGER.info(((AssignStmt) stmt).getLeftOp());
//        }
//
//        if (dest instanceof JThrowStmt) {
//            JThrowStmt jThrowStmt = (JThrowStmt) stmt;
//            LOGGER.info(jThrowStmt.toString());
//        }


//
//        if(stmt instanceof )
//
//
//        if (stmt instanceof JIfStmt) {
//            final JIfStmt IfStmt = (JIfStmt) stmt;
//            if (IfStmt.getTarget().equals(stmtd)) {
//                List<ValueBox> valueBoxs = IfStmt.getCondition().getUseBoxes();
//                if (valueBoxs.size() == 2 && valueBoxs.get(1).getValue().toString().equals("null")) {
//                    final Value left = valueBoxs.get(0).getValue();
//                    return new FlowFunction<NullFlowAbstraction>() {
//
//                        @Override
//                        public Set<NullFlowAbstraction> computeTargets(NullFlowAbstraction source) {
//                            Set<NullFlowAbstraction> outSet = new HashSet<NullFlowAbstraction>();
//                            if (!source.IsinState(left)) {
//                                outSet.add(source.deriveWithNewStmt(icfg.getMethodOf(src)));
//                            }
//                            return outSet;
//                        }
//                    };
//                }
//            }
//        } else if (stmt instanceof AssignStmt) {
//            final AssignStmt assignStmt = (AssignStmt) stmt;
//            final Value left = assignStmt.getLeftOp();
//            final Value right = assignStmt.getRightOp();
//
//            return new FlowFunction<FlowAbstraction>() {
//
//                @Override
//                public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//
//                    Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//
//                    // If the right side is tainted, we need to taint the left side as well
//                    // A a; Local ; Static ; Array
//
//                    FlowAbstraction fa = getTaint(right, left, source, src);
//                    if (fa != null) {
//                        fa.getState().addAll(source.getState()); //for 别名分析
//                        fa.getState().add(fa.getSwrLocal());
//                        outSet.add(fa);
//                    }
//                    //别名分析删掉被重写的fact
//                    SwrLocal frm = null;
//                    SwrLocal feq = null;
//                    boolean rightSideMatche = false;
//                    boolean leftSideMatche = false;
//                    for (SwrLocal f : source.getState()) {
//                        if (left instanceof Local && f.getLocal() == left) {
//                            leftSideMatche = true;
//                            frm = f;
//                        } else if (left instanceof InstanceFieldRef) {
//                            InstanceFieldRef ifr = (InstanceFieldRef) left;
//                            if (f.hasPrefix(ifr)) {
//                                leftSideMatche = true;
//                                frm = f;
//                            }
//                        } else if (left instanceof StaticFieldRef) {
//                            StaticFieldRef sfr = (StaticFieldRef) left;
//                            if (f.hasPrefix(sfr)) {
//                                leftSideMatche = true;
//                                frm = f;
//                            }
//                        } else if (left instanceof ArrayRef) {
//                            ArrayRef arrayRef = (ArrayRef) left;
//                            if (f.getLocal() != null && arrayRef.getBase().equals(f.getLocal()) && f.getIndex().equals(Integer.parseInt(arrayRef.getIndex().toString()))) {
//                                leftSideMatche = true;
//                                frm = f;
//                            }
//                        }
//
//                        if (right instanceof Local && f.getLocal() == right) {
//                            rightSideMatche = true;
//                            feq = f;
//                        } else if (right instanceof InstanceFieldRef) {
//                            InstanceFieldRef ifr = (InstanceFieldRef) right;
//                            if (f.hasPrefix(ifr)) {
//                                rightSideMatche = true;
//                                feq = f;
//                            }
//                        } else if (right instanceof StaticFieldRef) {
//                            StaticFieldRef sfr = (StaticFieldRef) right;
//                            if (f.hasPrefix(sfr)) {
//                                rightSideMatche = true;
//                                feq = f;
//                            }
//                        } else if (right instanceof ArrayRef) {
//                            ArrayRef arrayRef = (ArrayRef) right;
//                            if (f.getLocal() != null && arrayRef.getBase().equals(f.getLocal()) && f.getIndex().equals(Integer.parseInt(arrayRef.getIndex().toString()))) {
//                                rightSideMatche = true;
//                                feq = f;
//                            }
//                        }
//                        if (leftSideMatche && rightSideMatche) break;
//                    }
//                    if (leftSideMatche && !rightSideMatche) {
//                        source.getState().remove(frm);
//                    }
//                    if (rightSideMatche && !leftSideMatche) {
//                        source.getState().add(SwrLocal.v(left, icfg.getMethodOf(src), feq.getIndex()));
//                    }
//                    outSet.add(source.deriveWithNewStmt(icfg.getMethodOf(src)));
//                    return outSet;
//                }
//            };
//        }
//        return new FlowFunction<FlowAbstraction>() {
//            @Override
//            public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//                Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//                FlowAbstraction newAbs = source.deriveWithNewStmt(icfg.getMethodOf(src));
//                outSet.add(newAbs);
//                return outSet;
//            }
//        };

        }

        return new FlowFunction<NullFlowAbstraction>() {
            @Override
            public Set<NullFlowAbstraction> computeTargets(NullFlowAbstraction source) {
                Set<NullFlowAbstraction> outSet = new HashSet<NullFlowAbstraction>();
                return outSet;
            }
        };

    }


    public FlowFunction<NullFlowAbstraction> DealCallFlowFunction(final Unit src, final SootMethod dest) {

        LOGGER.info("call Flow Funtion!");
        LOGGER.info("src unit:{}  type:{}", src.toString(), src.getClass());
        LOGGER.info("dest SootMethod:{} type:{}", dest.getName(), dest.getClass());

        if (src instanceof InvokeStmt) {
            Local l = null;
//            List<ValueBox> useBox = ((InvokeStmt) src).getInvokeExpr().getUseBoxes();
            List<Value> args = ((InvokeStmt) src).getInvokeExpr().getArgs();
            LOGGER.info(((InvokeStmt) src).getInvokeExpr().getArgs().toString());
            for (ValueBox valueBox : ((InvokeStmt) src).getInvokeExpr().getUseBoxes()) {
                if(!args.contains(valueBox.getValue()))
                    l = (Local)valueBox.getValue();
                    LOGGER.info("value:{} type:{}",valueBox.getValue(),valueBox.getValue().getType());
                    break;
            }
            final Local local = l;
            return new FlowFunction<NullFlowAbstraction>() {
                @Override
                public Set<NullFlowAbstraction> computeTargets(NullFlowAbstraction source) {
                    if(local != null){
                        if(source.getLocal() != null){
                            if(source.getLocal().equals(local)){
                                LOGGER.info("NULL POINTER DEFERENCE DETECT!!!");
                            }
                        }

                    }

                    Set<NullFlowAbstraction> outSet = new HashSet<NullFlowAbstraction>();
                    return outSet;
                }
            };

        }

//        if (!icfg.IsAppMethod(dest)) {
//            return KillAll.v();
//        }
//        final Stmt stmt = (Stmt) src;
//        final InvokeExpr ie = stmt.getInvokeExpr();
//        // Get the formal parameter locals in the callee
//        final List<Local> paramLocals = new ArrayList<Local>();
//        if (!dest.getName().equals("<init>"))
//            for (int i = 0; i < dest.getParameterCount(); i++)
//                paramLocals.add(dest.getActiveBody().getParameterLocal(i));
//
//        return new FlowFunction<FlowAbstraction>() {
//
//            @Override
//            public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//
//                Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//
//                // Static fields
//                if (source.getLocal() == null) {
//                    outSet.add(source.deriveWithNewStmt(icfg.getMethodOf(src)));
//                    // Map the "this" value
//                } else if (ie instanceof InstanceInvokeExpr
//                        && ((InstanceInvokeExpr) ie).getBase() == source.getLocal() && source.LocalIsInState()) {
//                    for (Value v : ie.getArgs()) { //情况比较特殊，后期可以考虑删除
//                        if (source.IsinState(v)) {
//                            int index = ie.getArgs().indexOf(v);
//                            source.getState().add(SwrLocal.v(paramLocals.get(index), dest, -1));
//                        }
//                    }
//                    outSet.add(source.deriveWithNewLocal(dest.getActiveBody().getThisLocal(),
//                            icfg.getMethodOf(src), source));
//                    // Map the parameters
//                } else if (ie.getArgs().contains(source.getLocal()) && source.LocalIsInState()) {
//                    int argIndex = ie.getArgs().indexOf(source.getLocal());
////					FlowAbstraction fa = source.deriveWithNewLocal(paramLocals.get(argIndex),
////							icfg.getMethodOf(src), source);
//                    FlowAbstraction fa = source.deriveWithNewLocal(paramLocals.get(argIndex),
//                            dest, source);
//                    return Collections.singleton(fa);
//                }
//                return outSet;
//            }
//        };
        return new FlowFunction<NullFlowAbstraction>() {
            @Override
            public Set<NullFlowAbstraction> computeTargets(NullFlowAbstraction source) {
                Set<NullFlowAbstraction> outSet = new HashSet<NullFlowAbstraction>();
                return outSet;
            }
        };
    }

    public FlowFunction<NullFlowAbstraction> DealReturnFlowFunction(final Unit callSite, final SootMethod callee,
                                                                    final Unit exitStmt, final Unit returnSite) {
//        if (!icfg.CalleeIsAppClass(callSite)) {
//            return KillAll.v();
//        }
//
//        if (returnSite instanceof JIdentityStmt && returnSite.toString().contains("@caughtexception")) {
//            JIdentityStmt Idstmt = (JIdentityStmt) returnSite;
//            Value left = Idstmt.leftBox.getValue();
//            if (!IsRightException(left.getType(), callSite)) {
//                return new FlowFunction<FlowAbstraction>() {
//                    @Override
//                    public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//                        Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//                        return outSet;
//                    }
//                };
//            }
//        }
//
//        final Value retOp = (exitStmt instanceof ReturnStmt) ? ((ReturnStmt) exitStmt).getOp() : null;
//        final Value tgtOp = (callSite instanceof DefinitionStmt) ? ((DefinitionStmt) callSite).getLeftOp() : null;
//        final InvokeExpr invExpr = ((Stmt) callSite).getInvokeExpr();
//
//        // Get the formal parameter locals in the callee
//        final List<Local> paramLocals = new ArrayList<Local>();
//        final SootMethod method = icfg.getMethodOf(callSite);
//        if (!callee.getName().equals("<init>"))
//            for (int i = 0; i < callee.getParameterCount(); i++)
//                paramLocals.add(callee.getActiveBody().getParameterLocal(i));
//
//        return new FlowFunction<FlowAbstraction>() {
//
//            @Override
//            public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//                Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//                FlowAbstraction sources = source.deriveWithNewStmt(icfg.getMethodOf(exitStmt));
//                // Map the return value
//                if (retOp != null && source.getLocal() == retOp && tgtOp != null && !(returnSite instanceof JIdentityStmt)) {
//                    FlowAbstraction fa = source.deriveWithNewLocal((Local) tgtOp, icfg.getMethodOf(callSite), source);
//                    System.out.println(returnSite);
//                    source.setIsreturn(true);
//                    outSet.add(fa);
//                } else if (invExpr instanceof InstanceInvokeExpr
//                        && source.getLocal() == callee.getActiveBody().getThisLocal()) {
//                    Set<SwrLocal> locals = new HashSet<>();
//                    for (SwrLocal local : sources.getState()) {
//                        if (!local.method.equals(method)) locals.add(local);
//                    }
//                    sources.getState().removeAll(locals);
//                    for (SwrLocal local : sources.getState()) {
//                        FlowAbstraction fa = sources.deriveWithNewLocal(local.getLocal(), icfg.getMethodOf(callSite),
//                                source);
//                        outSet.add(fa);
//                    }
//                }
//                // Map the parameters
//                else if (paramLocals.contains(source.getLocal())) {
//                    int paramIdx = paramLocals.indexOf(sources.getLocal());
//                    if (!(invExpr.getArg(paramIdx) instanceof Constant)) {
//                        Set<SwrLocal> locals = new HashSet<>();
//                        for (SwrLocal local : sources.getState()) {
//                            if (!local.method.equals(method)) locals.add(local);
//                        }
//                        sources.getState().removeAll(locals);
//                        for (SwrLocal local : sources.getState()) {
//                            FlowAbstraction fa = sources.deriveWithNewLocal((Local) invExpr.getArg(paramIdx),
//                                    icfg.getMethodOf(callSite), source);
//                            outSet.add(fa);
//                        }
//                    }
//                }
//                // Static variables
//                if (source.getLocal() == null) {
//                    FlowAbstraction newAbs = source.deriveWithNewStmt(icfg.getMethodOf(callSite));
//                    outSet.add(newAbs);
//                }
//                return outSet;
//            }
//        };
        return new FlowFunction<NullFlowAbstraction>() {
            @Override
            public Set<NullFlowAbstraction> computeTargets(NullFlowAbstraction source) {
                Set<NullFlowAbstraction> outSet = new HashSet<NullFlowAbstraction>();
                return outSet;
            }
        };
    }

    public boolean IsRightException(Type exception, Unit unit) {
        String method = ((Stmt) unit).getInvokeExpr().getMethod().getSignature();
        if (icfg.methodtoException.containsKey(method)) {
            if (icfg.methodtoException.get(method).contains(exception.toString())) {
                return true;
            }
        }
        if (exception.toString().equals("java.lang.Throwable")) {
            if (!icfg.methodtoException.containsKey(method)) {
                return false;
            }
            List<Unit> units = icfg.getSuccsOf(unit);
            for (Unit u : units) {
                if (u instanceof JIdentityStmt && ((JIdentityStmt) u).getRightOp().toString().contains("caughtexception")) {
                    String excs = ((JIdentityStmt) u).getLeftOp().getType().toString();
                    if (icfg.methodtoException.get(method).contains(excs)) {
                        return false;
                    }
                }
            }
            return true;
        }
        return false;
    }

    public FlowFunction<NullFlowAbstraction> DealCallToReturnFlowFunction(final Unit call, Unit returnSite) {
//        final Stmt stmt = (Stmt) call;
//
//        if (returnSite instanceof JIdentityStmt && returnSite.toString().contains("@caughtexception")) {
//            JIdentityStmt Idstmt = (JIdentityStmt) returnSite;
//            Value left = Idstmt.leftBox.getValue();
//            if (!IsRightException(left.getType(), call)) {
//                return new FlowFunction<FlowAbstraction>() {
//                    @Override
//                    public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//                        Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//                        return outSet;
//                    }
//                };
//            }
//        }
//
//        String target = stmt.getInvokeExpr().getMethod().getSignature();
//        if (acquireStream(target)) {
//            if ((returnSite instanceof JIdentityStmt)) {
//                return new FlowFunction<FlowAbstraction>() {
//                    @Override
//                    public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//                        Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//                        FlowAbstraction newAbs = source.deriveWithNewStmt(icfg.getMethodOf(call));
//                        outSet.add(newAbs);
//                        return outSet;
//                    }
//                };
//            } else {
//                final JInvokeStmt invokeStmt = (JInvokeStmt) stmt;
//                final JSpecialInvokeExpr jie = (JSpecialInvokeExpr) invokeStmt.getInvokeExpr();
//                return new FlowFunction<FlowAbstraction>() {
//                    @Override
//                    public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//                        Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//                        FlowAbstraction flowAbstraction = source.deriveWithNewStmt(icfg.getMethodOf(call));
//                        if (source.equals(FlowAbstraction.zeroAbstraction())) {
//                            FlowAbstraction fa = new FlowAbstraction(invokeStmt, (Local) jie.getBase(),
//                                    icfg.getMethodOf(call), FlowAbstraction.zeroAbstraction(),
//                                    -1, null, null);
//                            fa.getState().add(fa.getSwrLocal()); // 别名分析先把本身加进去
//                            // use for 嵌套分析
//                            for (Value arg : stmt.getInvokeExpr().getArgs()) {
//                                if (Stream.contains(arg.getType().toString())) {
//                                    fa.getState().add(SwrLocal.v(arg, icfg.getMethodOf(call), -1));
//                                }
//                            }
//                            outSet.add(fa);
//                        } else {
//                            // use for 嵌套分析
//                            for (Value arg : stmt.getInvokeExpr().getArgs()) {
//                                if (flowAbstraction.IsinState(arg)) {
//                                    flowAbstraction.getState().add(SwrLocal.v(jie.getBase(), icfg.getMethodOf(call), -1));
//                                    break;
//                                }
//                            }
//                        }
//                        outSet.add(flowAbstraction);
//                        return outSet;
//                    }
//                };
//            }
//
//        } else if (closeStream(target)) {
//
//            return new FlowFunction<FlowAbstraction>() {
//                @Override
//                public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//                    Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//                    boolean baseIsSource = false;
//                    if (stmt.getInvokeExpr() instanceof JVirtualInvokeExpr) {
//                        JVirtualInvokeExpr jie = (JVirtualInvokeExpr) stmt.getInvokeExpr();
//                        //baseIsSource = source.hasPrefix(jie.getBase());
//                        for (SwrLocal local : source.getState()) {
//                            if (local.hasPrefix(jie.getBase())) {
//                                baseIsSource = true;
//                                break;
//                            }
//                        }
//                    }
//
//                    if (baseIsSource) {
//                        return outSet;
//                    }
//                    FlowAbstraction fa = source.deriveWithNewStmt(icfg.getMethodOf(call));
//                    outSet.add(fa);
//                    return outSet;
//                }
//            };
//        } else if (icfg.UnitIsinApp(call) && !icfg.CalleeIsAppClass(call)) {
//            return new FlowFunction<FlowAbstraction>() {
//                @Override
//                public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//
//                    Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//                    FlowAbstraction fa = source.deriveWithNewStmt(icfg.getMethodOf(call));
//                    outSet.add(fa);
//                    return outSet;
//
//                }
//            };
//
//        }
//
//        return new FlowFunction<FlowAbstraction>() {
//
//            @Override
//            public Set<FlowAbstraction> computeTargets(FlowAbstraction source) {
//                Set<FlowAbstraction> outSet = new HashSet<FlowAbstraction>();
//                FlowAbstraction newAbs = source.deriveWithNewStmt(icfg.getMethodOf(call));
//                Value sourceInArgs = null;
//                for (Value arg : stmt.getInvokeExpr().getArgs()) {
//                    if (source.IsinState(arg)) {
//                        sourceInArgs = arg;
//                        break;
//                    }
//                }
//                boolean sourceIsCallerObj = false;
//                if (stmt.getInvokeExpr() instanceof InstanceInvokeExpr) {
//                    InstanceInvokeExpr iie = (InstanceInvokeExpr) stmt.getInvokeExpr();
//                    if (source.IsinState(iie.getBase()))
//                        sourceIsCallerObj = true;
//                }
//                boolean staticField = false;
//                if (source.getLocal() == null && !source.equals(FlowAbstraction.zeroAbstraction()))
//                    staticField = true;
//
//                // ID
//                if (call instanceof AssignStmt) {
//                    AssignStmt assignStmt = (AssignStmt) call;
//                    final Local leftLocal = (Local) assignStmt.getLeftOp();
//                    SwrLocal frm = null;
//                    for (SwrLocal local : newAbs.getState()) {
//                        if (local.hasPrefix(leftLocal)) {
//                            frm = local;
//                        }
//                    }
//                    if (frm != null)
//                        newAbs.getState().remove(frm);
//                }
//
//
//                if (sourceInArgs == null && !sourceIsCallerObj && !staticField)
//                    outSet.add(newAbs);
//
//                return outSet;
//            }
//
//        };
        return new FlowFunction<NullFlowAbstraction>() {
            @Override
            public Set<NullFlowAbstraction> computeTargets(NullFlowAbstraction source) {
                Set<NullFlowAbstraction> outSet = new HashSet<NullFlowAbstraction>();
                return outSet;
            }
        };
    }


    /***** Taints *****/

    private NullFlowAbstraction getTaint(Value right, Value left, NullFlowAbstraction source, Unit src) {
        NullFlowAbstraction fa = null;
        if (right instanceof CastExpr)
            right = ((CastExpr) right).getOp();
        if (right instanceof NullType) {
            fa = NullFlowAbstraction.v(source.getSource(), left, icfg.getMethodOf(src), source);
        } else if (right instanceof Local && source.getLocal() == right) {
            fa = NullFlowAbstraction.v(source.getSource(), left, icfg.getMethodOf(src), source);
//            fa = fa.append(source.getFields());
        } else if (right instanceof InstanceFieldRef) {
            InstanceFieldRef ifr = (InstanceFieldRef) right;
//            if (source.hasPrefix(ifr)) {
//                fa = FlowAbstraction.v(source.getSource(), left, icfg.getMethodOf(src), source);
//                fa = fa.append(source.getPostfix(ifr));
//            }
        } else if (right instanceof StaticFieldRef) {
            StaticFieldRef sfr = (StaticFieldRef) right;
//            if (source.hasPrefix(sfr)) {
//                fa = FlowAbstraction.v(source.getSource(), left, icfg.getMethodOf(src), source);
//                fa = fa.append(source.getPostfix(sfr));
//            }
        } else if (right instanceof ArrayRef) {
            ArrayRef ar = (ArrayRef) right;
//            if (ar.getBase() == source.getLocal())
//                fa = FlowAbstraction.v(source.getSource(), left, icfg.getMethodOf(src), source);
        }
        return fa;
    }

    static HashSet<String> Stream = new HashSet<String>() {{
        add("java.io.FileInputStream");
        add("java.io.FileOutputStream");
        add("java.io.OutputStreamWriter");
        add("java.io.PrintWriter");
        add("java.io.FileWriter");
    }};

    static HashSet<String> initStream = new HashSet<String>() {{
        add("<java.io.FileInputStream: void <init>(java.io.File)>");
        add("<java.io.FileOutputStream: void <init>(java.io.File)>");
        add("<java.io.OutputStreamWriter: void <init>(java.io.OutputStream,java.lang.String)>");
        add("<java.io.OutputStreamWriter: void <init>(java.io.OutputStream)>");
        add("<java.io.PrintWriter: void <init>(java.io.Writer)>");
        add("<java.io.FileWriter: void <init>(java.io.File)>");
    }};
    static HashSet<String> closeStream = new HashSet<String>() {{
        add("<java.io.FileInputStream: void close()>");
        add("<java.io.PrintWriter: void close()>");
        add("<java.io.OutputStreamWriter: void close()>");
        add("<java.io.FileOutputStream: void close()>");
    }};

    protected static boolean acquireStream(String target) {
        if (initStream.contains(target)) {
            return true;
        }
        return false;
    }

    protected static boolean closeStream(String target) {
        if (closeStream.contains(target)) {
            return true;
        }
        return false;
    }


}
