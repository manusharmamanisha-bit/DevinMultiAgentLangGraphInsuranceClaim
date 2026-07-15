package com.insurance.claim.graph;

import com.insurance.claim.model.Claim;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ClaimGraphState extends AgentState {

    public static final String CLAIM_KEY = "claim";
    public static final String AUDIT_KEY = "auditTrail";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(
            CLAIM_KEY, Channels.base((oldValue, newValue) -> newValue, Claim::new),
            AUDIT_KEY, Channels.<String>appender(ArrayList::new)
    );

    public ClaimGraphState(Map<String, Object> initData) {
        super(initData);
    }

    public Claim claim() {
        return this.<Claim>value(CLAIM_KEY).orElse(null);
    }

    @SuppressWarnings("unchecked")
    public List<String> auditTrail() {
        return this.<List<String>>value(AUDIT_KEY).orElse(List.of());
    }
}
