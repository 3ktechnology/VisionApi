package com.example.models;

import com.google.gson.annotations.SerializedName;

public class SegmentResponseModel {
    @SerializedName("id")
    private String id;
    @SerializedName("support_info")
    private SupportInfo support_info;

    public SegmentResponseModel() {
    }

    public SegmentResponseModel(String id, SupportInfo support_info) {
        this.id = id;
        this.support_info = support_info;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SupportInfo getSupport_info() {
        return support_info;
    }

    public void setSupport_info(SupportInfo support_info) {
        this.support_info = support_info;
    }

    public class SupportInfo {
        @SerializedName("internal_trace_id")
        private String internal_trace_id;

        public SupportInfo(String internal_trace_id) {
            this.internal_trace_id = internal_trace_id;
        }

        public String getInternal_trace_id() {
            return internal_trace_id;
        }

        public void setInternal_trace_id(String internal_trace_id) {
            this.internal_trace_id = internal_trace_id;
        }
    }
}
