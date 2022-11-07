package org.processmining.specpp.datastructures.transitionSystems;

import org.processmining.specpp.datastructures.log.Activity;
import org.processmining.specpp.datastructures.log.Variant;

import java.util.ListIterator;

public class PrefixAutomaton {

    PAState initial;

    public PrefixAutomaton(PAState initial) {
        this.initial = new PAState();
    }

    public void addVariant(Variant variant) {
        PAState curr = initial;
        for(Activity activity : variant) {

            if(curr.isFinal()){
                PATransition newTrans = new PATransition(activity);
                curr.addOutgoingTrans(newTrans);
                curr = newTrans.getPointer();

            } else {
                ListIterator<PATransition> it = curr.getOutgoingTrans().listIterator();
                boolean contains = false;

                while(it.hasNext()) {
                    PATransition next = it.next();
                    if (next.getActivity().equals(activity)) {
                        // curr has activity as outgoing arc -> update freq
                        contains = true;
                        curr = next.getPointer();
                    }
                }
                if(!contains) {
                    // curr doesn't have activity as outgoing arc -> add trans
                    PATransition newTrans = new PATransition(activity);

                    curr.addOutgoingTrans(newTrans);
                    curr = newTrans.getPointer();
                }
            }

        }
    }

    public PAState getInitial() {
        return initial;
    }




}
