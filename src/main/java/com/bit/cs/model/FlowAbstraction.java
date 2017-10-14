package com.bit.cs.model;


import soot.*;
import soot.jimple.ArrayRef;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;

import java.util.HashSet;

public class FlowAbstraction {

	private final static FlowAbstraction zeroAbstraction = new FlowAbstraction(null, null, null, null,-1,null,null);

	private final Unit source;
	
	private FlowAbstraction predecessor;
	
	private Unit sink;
	private String type;
	private boolean isreturn = false;
	
	private HashSet<SwrLocal> State;
	private SwrLocal swrLocal;
	public int Level = 0;
	
	public FlowAbstraction(Unit source, Local local, SootMethod method, FlowAbstraction predecessor, Integer index, Unit sink, String type) {
		this(source, local, new SootField[] {}, method, predecessor,index,sink,type);
	}
	
	public FlowAbstraction(Unit source, Local local, SootField[] fields,
                           SootMethod method, FlowAbstraction predecessor,
                           Integer index, Unit sink, String type) {
		this.source = source;
		this.predecessor = predecessor;
		this.sink = sink;
		this.type = type;
		State = new HashSet<SwrLocal>();
		swrLocal = new SwrLocal(local, fields, index,method);
	}
	public static FlowAbstraction zeroAbstraction() {
		return zeroAbstraction;
	}

	public boolean isZeroAbstraction() {
		if (swrLocal.local == null && source == null && swrLocal.fields.length == 0)
			return true;
		return false;
	}

	/***** Getters and setters *****/

	public Unit getSource() {
		return this.source;
	}

	public SootMethod getMethod() {
		return this.swrLocal.method;
	}

	public Local getLocal() {
		return this.swrLocal.local;
	}

	public SootField[] getFields() {
		return this.swrLocal.fields;
	}

	

	public FlowAbstraction predecessor() {
		return predecessor;
	}

	

	/***** Utils *****/

	public boolean exactEquals(FlowAbstraction other) {
		if (this.equals(other)) {
			return this.predecessor.equals(other.predecessor);
		}
		return false;
	}

	

	public String getShortName() {
		if (isZeroAbstraction())
			return "0";

		String res = "";
		if (swrLocal.local != null)
			res += swrLocal.local.getName();
		else
			res += swrLocal.fields[0].getDeclaringClass().getName();
		if (swrLocal.fields != null && swrLocal.fields.length > 0) {
			for (SootField sf : swrLocal.fields) {
				res += "." + sf.getName();
			}
		}
		if(!swrLocal.index.equals(-1)){
			res += "@"+swrLocal.index;
		}
		res += "{";
		for (SwrLocal local : State) {
			res += local+",";
		}
		res+="}";
		
		//res+= sink!=null?"(true)":"(false)";
		// res += "(" + this.hashCode() + ")";

		return res;
	}

	@Override
	public String toString() {
		return getShortName();
	}

	public String toLongString() {
		return getShortName() + " --- " + " --- " + predecessor;
	}

	/**** Abstraction operations ****/

//	public FlowAbstraction deriveWithNewSource(Unit newSource, Unit unit, SootMethod method,
//			FlowAbstraction predecessor) {
//		return new FlowAbstraction(newSource, local, fields, unit, method, predecessor,index,);
//	}

	public FlowAbstraction deriveWithNewStmt(SootMethod method) {
		// Avoid multiple zeroAbstractions with different units
		if (source == null && swrLocal.local == null && swrLocal.fields.length == 0 && this.equals(zeroAbstraction()))
			return zeroAbstraction();
		FlowAbstraction pred = getPredecessor( method, this);
		FlowAbstraction fa = new FlowAbstraction(source, swrLocal.local, swrLocal.fields, method, pred,swrLocal.index,sink,type);
		fa.setState(pred.getState());
		return fa;
	}

	public FlowAbstraction deriveWithrewriting(SootMethod method) {
		// Avoid multiple zeroAbstractions with different units
		if (source == null && swrLocal.local == null && swrLocal.fields == null && this.equals(zeroAbstraction()))
			return zeroAbstraction();
		FlowAbstraction pred = getPredecessor( method, this);
		FlowAbstraction fa = new FlowAbstraction(source, swrLocal.local, swrLocal.fields, method, pred,swrLocal.index,sink,type);
		fa.getState().add(fa.swrLocal);
		return fa;
	}
	
	
	public FlowAbstraction deriveWithNewLocal(Local local, SootMethod method, FlowAbstraction predecessor) {
		if (local == null)
			throw new RuntimeException("Target local may not be null");
		predecessor = getPredecessor( method, predecessor);
		FlowAbstraction fa = new FlowAbstraction(source, local, swrLocal.fields, method, predecessor,swrLocal.index,sink,type);
		fa.setState(predecessor.getState());
		fa.getState().add(fa.getSwrLocal());
		return fa;
	}

	public static FlowAbstraction v(Unit source, Value v, SootMethod method, FlowAbstraction predecessor) {
		predecessor = getPredecessor( method, predecessor);
		if (v instanceof Local) {
			return new FlowAbstraction(source, (Local) v, method, predecessor,-1,predecessor.sink,predecessor.type);
		} else if (v instanceof InstanceFieldRef) {
			InstanceFieldRef ifr = (InstanceFieldRef) v;
			return new FlowAbstraction(source, (Local) ifr.getBase(), new SootField[] { ifr.getField() }, method,
					predecessor,-1,predecessor.sink,predecessor.type);
		} else if (v instanceof StaticFieldRef) {
			StaticFieldRef sfr = (StaticFieldRef) v;
			return new FlowAbstraction(source, null, new SootField[] { sfr.getField() }, method, predecessor,-1,predecessor.sink,predecessor.type);
		} else if (v instanceof ArrayRef) {
			ArrayRef ar = (ArrayRef) v;
			return new FlowAbstraction(source, (Local) ar.getBase(), new SootField[] {}, method, predecessor,Integer.parseInt(ar.getIndex().toString()),predecessor.sink,predecessor.type);
		} else
			throw new RuntimeException("Unexpected left side " + v + " (" + v.getClass() + ")");
	}

	private static FlowAbstraction getPredecessor(SootMethod method, FlowAbstraction wantedPredecessor) {
		// If part of any dummyMain, return predecessor of predecessor
//		if (method.getName().equals(Config.dummyMainMethodName))
//			return wantedPredecessor.predecessor();
		return wantedPredecessor;
	}

	/**** Field operations ****/

	public FlowAbstraction append(SootField[] newFields) {
		SootField[] a = new SootField[swrLocal.fields.length + newFields.length];
		System.arraycopy(swrLocal.fields, 0, a, 0, swrLocal.fields.length);
		System.arraycopy(newFields, 0, a, swrLocal.fields.length, newFields.length);
		this.swrLocal.fields = a;
		return this;
	}

	public boolean hasPrefix(Value v) { // if this has prefix v
		return swrLocal.hasPrefix(v);
	}

	public SootField[] getPostfix(Value v) { // this is longer than v
		return swrLocal.getPostfix(v);
	}



	public boolean same(Value sourceInArgs) {
		return sourceInArgs.toString().equals(this.getShortName());
	}

	public Unit getSink() {
		return sink;
	}

	public void setSink(Unit sink) {
		this.sink = sink;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public Integer getIndex() {
		if(swrLocal.index!=null)
			return swrLocal.index;
		return null;
	}

	public FlowAbstraction getPredecessor() {
		return predecessor;
	}

	public void setPredecessor(FlowAbstraction predecessor) {
		this.predecessor = predecessor;
	}

	public boolean isIsreturn() {
		return isreturn;
	}

	public void setIsreturn(boolean isreturn) {
		this.isreturn = isreturn;
	}

	public HashSet<SwrLocal> getState() {
		return State;
	}

	public void setState(HashSet<SwrLocal> state) {
		State = new HashSet<>();
		for (SwrLocal swrLocal : state) {
			State.add(swrLocal);
		}
	}

	public SwrLocal getSwrLocal() {
		return swrLocal;
	}

	public void setSwrLocal(SwrLocal swrLocal) {
		this.swrLocal = swrLocal;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((State == null) ? 0 : State.hashCode());
		result = prime * result + ((swrLocal == null) ? 0 : swrLocal.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FlowAbstraction other = (FlowAbstraction) obj;
		if (State == null) {
			if (other.State != null)
				return false;
		} else if (!State.equals(other.State))
			return false;
		if (swrLocal == null) {
			if (other.swrLocal != null)
				return false;
		} else if (!swrLocal.equals(other.swrLocal))
			return false;
		return true;
	}	
	
	public boolean IsinState(Value v) {
		for(SwrLocal local : State){
			if(local.hasPrefix(v)){
				return true;
			}
		}
		return false;
	}
	public boolean LocalIsInState() {
		return State.contains(swrLocal);
	}
}

