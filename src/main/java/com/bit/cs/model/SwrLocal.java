package com.bit.cs.model;

import soot.Local;
import soot.SootField;
import soot.SootMethod;
import soot.Value;
import soot.jimple.ArrayRef;
import soot.jimple.Constant;
import soot.jimple.InstanceFieldRef;
import soot.jimple.StaticFieldRef;

import java.util.Arrays;

public class SwrLocal {
    public Local local;
    public SootField[] fields = new SootField[]{};
    public Integer index = -1;

    public SootMethod method;


    public SwrLocal(Local local, SootField[] fields, Integer index, SootMethod method) {
        super();
        this.local = local;
        this.fields = fields;
        this.index = index;
        this.method = method;
    }

    public Local getLocal() {
        return local;
    }

    public void setLocal(Local local) {
        this.local = local;
    }

    public SootField[] getFields() {
        return fields;
    }

    public void setFields(SootField[] fields) {
        this.fields = fields;
    }

    public Integer getIndex() {
        return index;
    }

    public void setIndex(Integer index) {
        this.index = index;
    }

    public boolean hasPrefix(Value v) { // if this has prefix v
        if (v instanceof Local) {
            if (local == null)
                return false;
            else
                return (local.equals(v));

        } else if (v instanceof InstanceFieldRef) {
            InstanceFieldRef ifr = (InstanceFieldRef) v;
            if (local == null) {
                if (ifr.getBase() != null)
                    return false;
            } else if (!local.equals(ifr.getBase()))
                return false;
            if (fields.length > 0 && ifr.getField() == fields[0])
                return true;
            return false;

        } else if (v instanceof StaticFieldRef) {
            StaticFieldRef sfr = (StaticFieldRef) v;
            if (local != null)
                return false;
            if (fields.length > 0 && sfr.getField() == fields[0])
                return true;
            return false;

        } else if (v instanceof ArrayRef) {
            ArrayRef ar = (ArrayRef) v;
            if (local == null)
                return false;
            else
                return (local.equals(ar.getBase()));

        } else if (v instanceof Constant) {
            return false;
        } else
            throw new RuntimeException("Unexpected left side " + v.getClass());
    }

    public SootField[] getPostfix(Value v) { // this is longer than v
        if (v instanceof InstanceFieldRef || v instanceof StaticFieldRef) {
            if (fields.length > 0)
                return Arrays.copyOfRange(fields, 1, fields.length);
            return new SootField[]{};
        } else if (v instanceof ArrayRef) {
            return new SootField[]{};
        } else
            throw new RuntimeException("Unexpected left side " + v.getClass());
    }

    public static SwrLocal v(Value v, SootMethod method, Integer index) {
        if (v instanceof Local) {
            return new SwrLocal((Local) v, new SootField[]{}, index, method);
        } else if (v instanceof InstanceFieldRef) {
            InstanceFieldRef ifr = (InstanceFieldRef) v;
            return new SwrLocal((Local) ifr.getBase(), new SootField[]{ifr.getField()}, index, method);
        } else if (v instanceof StaticFieldRef) {
            StaticFieldRef sfr = (StaticFieldRef) v;
            return new SwrLocal(null, new SootField[]{sfr.getField()}, index, method);
        } else if (v instanceof ArrayRef) {
            ArrayRef ar = (ArrayRef) v;
            return new SwrLocal((Local) ar.getBase(), new SootField[]{}, index, method);
        } else
            throw new RuntimeException("Unexpected left side " + v + " (" + v.getClass() + ")");
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + Arrays.hashCode(fields);
        result = prime * result + ((index == null) ? 0 : index.hashCode());
        result = prime * result + ((local == null) ? 0 : local.hashCode());
        if (local == null && index > 0) {
            SootField firstField = fields[0];
            result = prime * result + firstField.getDeclaringClass().hashCode();
        }
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
        SwrLocal other = (SwrLocal) obj;
        if (!Arrays.equals(fields, other.fields))
            return false;
        if (index == null) {
            if (other.index != null)
                return false;
        } else if (!index.equals(other.index))
            return false;
        if (local == null) {
            if (other.local != null)
                return false;
        } else if (!local.equals(other.local))
            return false;
        return true;
    }

    public String toString() {

        String res = "";
        if (local != null)
            res += local.getName();
        else
            res += fields[0].getDeclaringClass().getName();
        if (fields != null && fields.length > 0) {
            for (SootField sf : fields) {
                res += "." + sf.getName();
            }
        }
        if (!index.equals(-1)) {
            res += "@" + index;
        }
        return res;
    }

}
