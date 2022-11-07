package org.processmining.specpp.datastructures.transitionSystems;

import org.processmining.specpp.datastructures.log.Activity;

import java.util.LinkedList;
import java.util.ListIterator;

public class PAState {

    LinkedList<PATransition> outgoingTrans;

    public PAState() {
        outgoingTrans = new LinkedList<>();
    }

    public PAState(LinkedList<PATransition> outgoingTrans) {
        this.outgoingTrans = outgoingTrans;
    }




    public boolean isFinal(){
        return this.outgoingTrans.isEmpty();
    }

    public void addOutgoingTrans(PATransition t) {
        this.outgoingTrans.add(t);
    }

    public boolean checkForOutgoingAct(Activity a) {
        ListIterator<PATransition> it = outgoingTrans.listIterator();
        boolean contains = false;

        while(it.hasNext()) {
            PATransition next = it.next();
            if (next.getActivity().equals(a)) {
                return true;
            }
        }
        return false;
    }

    public LinkedList<PATransition> getOutgoingTrans() {
        return outgoingTrans;
    }

    public PATransition getTrans(Activity a) {
        for(PATransition t : outgoingTrans) {
            if(a.equals(t.getActivity())) {
                return t;
            }
        }
        return null;
    }


}
