package com.insurance.claim.graph;

import com.insurance.claim.agent.Agent;
import com.insurance.claim.model.Claim;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

public class AgentNode implements NodeAction<ClaimGraphState> {

    private final Agent agent;

    public AgentNode(Agent agent) {
        this.agent = agent;
    }

    @Override
    public Map<String, Object> apply(ClaimGraphState state) {
        Claim claim = state.claim();
        if (claim == null) {
            throw new IllegalStateException("Claim not present in graph state");
        }
        agent.execute(claim);
        return Map.of(ClaimGraphState.CLAIM_KEY, claim);
    }
}
