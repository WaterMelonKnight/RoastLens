package com.roastlens.safety;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class SafetyPolicy {

    public List<String> globalBoundaries() {
        return List.of(
                "No illegal guidance, no violence encouragement, no hate or discrimination.",
                "No abusive slurs or direct personal humiliation.",
                "Keep satire sharp but not dehumanizing.",
                "For finance content, avoid direct buy/sell/target-price instructions.",
                "Always include an informational disclaimer, not investment advice."
        );
    }

    public String defaultFinanceDisclaimer() {
        return "This content is for informational and educational purposes only and does not constitute investment advice.";
    }

    public List<String> financialRoastBoundaries() {
        return List.of(
                "No direct buy or sell instruction.",
                "No leverage instruction, guaranteed return, or certain target price.",
                "No exchange promotion, referral code, or trading-group solicitation.",
                "No direct personal abuse.",
                "Do not present an unprovided causal explanation or speculation as fact."
        );
    }
}
