package org.processmining.specpp.datastructures.transitionSystems;

import org.processmining.specpp.datastructures.log.Activity;

public class PATransition {

    Activity activity;
    PAState pointer;

    public PATransition(Activity activity){
        this.pointer = new PAState();
        this.activity = activity;
    }


    public Activity getActivity() {
        return activity;
    }

    public void setActivity(Activity activity) {
        this.activity = activity;
    }

    public PAState getPointer() {
        return pointer;
    }

    public void setPointer(PAState pointer) {
        this.pointer = pointer;
    }

    public String toString() {
        return "Transition: " + this.activity.toString() +  " ) ";
    }
}
