package com.sagar.easylock.screenlock;

import eu.chainfire.libsuperuser.Shell;

/**
 * Created by aravind on 1/7/15.
 */
public class RootHelper {

    private RootHelper(){}

    public static boolean hasRootAccess(){
        return Shell.SU.available();
    }

    public static boolean lockNow() {
        if(!hasRootAccess()) {
            return false;
        }
        Shell.SU.run("input keyevent 26");
        return true;
    }
}
