package com.imageapp;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class PipelineConfig {
    public static class Entry {
        public String op;
        public Map<String, Object> params;

        public Entry() {}

        public Entry(String op, Map<String, Object> params) {
            this.op = op;
            this.params = params;
        }
    }

    public List<Entry> pipeline = new ArrayList<>();

    public PipelineConfig() {}

    public void add(String op, Map<String, Object> params) {
        pipeline.add(new Entry(op, params));
    }
}
