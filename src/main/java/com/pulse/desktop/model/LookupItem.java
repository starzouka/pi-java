package com.pulse.desktop.model;

public class LookupItem {
    private final int id;
    private final String label;

    public LookupItem(int id, String label) {
        this.id = id;
        this.label = label;
    }

    public int getId() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}
