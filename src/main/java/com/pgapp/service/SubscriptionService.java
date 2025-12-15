package com.pgapp.service;

import java.time.LocalDate;
import org.springframework.stereotype.Service;
import com.pgapp.model.Owner;

@Service
public class SubscriptionService {

    /**
     * Check if subscription is currently active
     */
    public boolean isSubscriptionActive(Owner owner) {
        if (owner == null) return false;

        Boolean subscribed = owner.isSubscribed(); // CORRECTED
        LocalDate start = owner.getSubscriptionStart();
        LocalDate end = owner.getSubscriptionEnd();
        LocalDate today = LocalDate.now();

        return subscribed != null && subscribed
                && start != null && end != null
                && (!today.isBefore(start) && !today.isAfter(end));
    }

    /**
     * Check if trial is currently active
     */
    public boolean isTrialActive(Owner owner) {
        if (owner == null) return false;

        Boolean trialExpired = owner.isTrialExpired();
        LocalDate trialEnd = owner.getTrialEndDate();
        LocalDate today = LocalDate.now();

        return trialExpired != null && !trialExpired
                && trialEnd != null && !today.isAfter(trialEnd);
    }
}
