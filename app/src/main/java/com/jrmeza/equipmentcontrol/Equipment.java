package com.jrmeza.equipmentcontrol;

/**
 * Created by juanmeza on 4/18/17.
 */

public class Equipment {

    public int status;
    public String activeTM;
    public String alias;
    public LastTransaction lastTransaction;

    public Equipment() {

    }

    public Equipment(int status, String activeTM) {
        this.status = status;
        this.activeTM = activeTM;
    }

    public Equipment(int status, String activeTM, String alias, LastTransaction lastTransaction) {
        this.status = status;
        this.activeTM = activeTM;
        this.alias = alias;
        this.lastTransaction = lastTransaction;
    }

    public static class LastTransaction {

        public long timestamp;
        public String teamMember;

        public LastTransaction() {

        }

        public LastTransaction(long timestamp, String teamMember) {
            this.timestamp = timestamp;
            this.teamMember = teamMember;
        }

    }
}
