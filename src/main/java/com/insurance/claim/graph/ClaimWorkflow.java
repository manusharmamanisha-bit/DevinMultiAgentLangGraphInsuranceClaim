package com.insurance.claim.graph;

import com.insurance.claim.agent.*;
import com.insurance.claim.model.Claim;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.EdgeAction;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

@Component
public class ClaimWorkflow {

    private final CompiledGraph<ClaimGraphState> fullWorkflow;
    private final CompiledGraph<ClaimGraphState> postReviewWorkflow;

    public ClaimWorkflow(
            IntakeAgent intakeAgent,
            ValidationAgent validationAgent,
            FraudDetectionAgent fraudDetectionAgent,
            ManualReviewAgent manualReviewAgent,
            AssessmentAgent assessmentAgent,
            ApprovalAgent approvalAgent,
            PaymentAgent paymentAgent,
            NotificationAgent notificationAgent) throws Exception {

        this.fullWorkflow = buildFullWorkflow(
                intakeAgent, validationAgent, fraudDetectionAgent, manualReviewAgent,
                assessmentAgent, approvalAgent, paymentAgent, notificationAgent);

        this.postReviewWorkflow = buildPostReviewWorkflow(
                assessmentAgent, approvalAgent, paymentAgent, notificationAgent);
    }

    private CompiledGraph<ClaimGraphState> buildFullWorkflow(
            IntakeAgent intakeAgent,
            ValidationAgent validationAgent,
            FraudDetectionAgent fraudDetectionAgent,
            ManualReviewAgent manualReviewAgent,
            AssessmentAgent assessmentAgent,
            ApprovalAgent approvalAgent,
            PaymentAgent paymentAgent,
            NotificationAgent notificationAgent) throws Exception {

        EdgeAction<ClaimGraphState> validationRouter = state -> {
            Claim claim = state.claim();
            if (claim == null || !claim.isValid()) {
                return "end";
            }
            // Automatically send high-value claims (> 1 lakh / 100,000) for manual review.
            if (claim.getAmount() > 100_000) {
                return "manual_review";
            }
            return "fraud";
        };

        EdgeAction<ClaimGraphState> fraudRouter = state -> {
            double score = state.claim() != null ? state.claim().getFraudScore() : 0.0;
            return score >= 0.7 ? "manual_review" : "assess";
        };

        EdgeAction<ClaimGraphState> approvalRouter = state -> {
            boolean approved = state.claim() != null && state.claim().isApproved();
            return approved ? "pay" : "end";
        };

        return new StateGraph<>(ClaimGraphState.SCHEMA, ClaimGraphState::new)
                .addNode("intake", node_async(new AgentNode(intakeAgent)))
                .addNode("validation", node_async(new AgentNode(validationAgent)))
                .addNode("fraud", node_async(new AgentNode(fraudDetectionAgent)))
                .addNode("manual_review", node_async(new AgentNode(manualReviewAgent)))
                .addNode("assessment", node_async(new AgentNode(assessmentAgent)))
                .addNode("approval", node_async(new AgentNode(approvalAgent)))
                .addNode("payment", node_async(new AgentNode(paymentAgent)))
                .addNode("notification", node_async(new AgentNode(notificationAgent)))

                .addEdge(START, "intake")
                .addEdge("intake", "validation")
                .addConditionalEdges("validation", edge_async(validationRouter),
                        Map.of("fraud", "fraud", "manual_review", "manual_review", "end", END))
                .addConditionalEdges("fraud", edge_async(fraudRouter),
                        Map.of("manual_review", "manual_review", "assess", "assessment"))
                .addEdge("assessment", "approval")
                .addConditionalEdges("approval", edge_async(approvalRouter),
                        Map.of("pay", "payment", "end", END))
                .addEdge("payment", "notification")
                .addEdge("notification", END)
                .addEdge("manual_review", END)
                .compile();
    }

    private CompiledGraph<ClaimGraphState> buildPostReviewWorkflow(
            AssessmentAgent assessmentAgent,
            ApprovalAgent approvalAgent,
            PaymentAgent paymentAgent,
            NotificationAgent notificationAgent) throws Exception {

        EdgeAction<ClaimGraphState> approvalRouter = state -> {
            boolean approved = state.claim() != null && state.claim().isApproved();
            return approved ? "pay" : "end";
        };

        return new StateGraph<>(ClaimGraphState.SCHEMA, ClaimGraphState::new)
                .addNode("assessment", node_async(new AgentNode(assessmentAgent)))
                .addNode("approval", node_async(new AgentNode(approvalAgent)))
                .addNode("payment", node_async(new AgentNode(paymentAgent)))
                .addNode("notification", node_async(new AgentNode(notificationAgent)))

                .addEdge(START, "assessment")
                .addEdge("assessment", "approval")
                .addConditionalEdges("approval", edge_async(approvalRouter),
                        Map.of("pay", "payment", "end", END))
                .addEdge("payment", "notification")
                .addEdge("notification", END)
                .compile();
    }

    public Claim submit(Claim claim) {
        return run(fullWorkflow, claim);
    }

    public Claim resumeAfterReview(Claim claim) {
        return run(postReviewWorkflow, claim);
    }

    private Claim run(CompiledGraph<ClaimGraphState> graph, Claim claim) {
        RunnableConfig config = RunnableConfig.builder().build();
        Optional<ClaimGraphState> result = graph.invoke(Map.of(ClaimGraphState.CLAIM_KEY, claim), config);
        if (result.isEmpty()) {
            throw new IllegalStateException("Graph execution returned empty state");
        }
        Claim processed = result.get().claim();
        if (processed == null) {
            throw new IllegalStateException("Processed claim is null");
        }
        return processed;
    }
}
