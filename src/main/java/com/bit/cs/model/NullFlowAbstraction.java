package com.bit.cs.model;

import soot.Local;
import soot.Unit;

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
}
