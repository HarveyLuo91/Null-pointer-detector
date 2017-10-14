package com.bit.cs.model;

import org.jboss.util.Null;
import soot.*;
import soot.jimple.ArrayRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;

public class NullFlowAbstraction {
    private Unit source;
    private Unit sink;
    private String type;
    private boolean isNull = true;
    private Local local;

    public NullFlowAbstraction(Unit source, Local local, Unit sink, String type) {
        this.source = source;
        this.local = local;
        this.type = type;
        this.sink = sink;
    }

    private final static NullFlowAbstraction zeroAbstraction = new NullFlowAbstraction(null, null, null, null);

    public static NullFlowAbstraction zeroAbstraction() {
        return zeroAbstraction;
    }

    public Unit getSource() {
        return this.source;
    }

    public Local getLocal() {
        return this.local;
    }

    public static NullFlowAbstraction v(Unit source, Value v, SootMethod method, NullFlowAbstraction predecessor) {
        if (v instanceof Local) {
            return new NullFlowAbstraction(source, (Local) v, predecessor.sink, predecessor.type);
        } else if (v instanceof InstanceFieldRef) {
            InstanceFieldRef ifr = (InstanceFieldRef) v;
            return new NullFlowAbstraction(source, (Local) ifr.getBase(),
                    predecessor.sink, predecessor.type);
        } else if (v instanceof StaticFieldRef) {
            StaticFieldRef sfr = (StaticFieldRef) v;
            return new NullFlowAbstraction(source, null, predecessor.sink, predecessor.type);
        } else if (v instanceof ArrayRef) {
            ArrayRef ar = (ArrayRef) v;
            return new NullFlowAbstraction(source, (Local) ar.getBase(), predecessor.sink, predecessor.type);
        } else
            throw new RuntimeException("Unexpected left side " + v + " (" + v.getClass() + ")");
    }
}
