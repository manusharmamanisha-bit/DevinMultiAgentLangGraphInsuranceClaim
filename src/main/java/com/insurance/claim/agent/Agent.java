package com.insurance.claim.agent;

import com.insurance.claim.model.AgentResult;
import com.insurance.claim.model.Claim;

public interface Agent {

    /**
     * Executes this agent against the given claim and returns a result.
     */
    AgentResult execute(Claim claim);

    /**
     * The graph node name for this agent.
     */
    default String name() {
        return getClass().getSimpleName();
    }
}
