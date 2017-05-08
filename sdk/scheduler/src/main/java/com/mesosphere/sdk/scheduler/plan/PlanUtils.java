package com.mesosphere.sdk.scheduler.plan;

import com.mesosphere.sdk.specification.PodInstance;
import org.apache.mesos.Protos.Offer;
import org.apache.mesos.Protos.OfferID;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Common utility methods for {@link PlanManager}s.
 */
public class PlanUtils {

    private PlanUtils() {
        // do not instantiate
    }

    public static boolean allHaveStatus(Status status, Collection<? extends Element> elements) {
        return elements.stream().allMatch(element -> element.getStatus() == status);
    }

    public static boolean anyHaveStatus(Status status, Collection<? extends Element> elements) {
        return elements.stream().anyMatch(element -> element.getStatus() == status);
    }

    public static List<Offer> filterAcceptedOffers(List<Offer> offers, Collection<OfferID> acceptedOfferIds) {
        return offers.stream().filter(offer -> !acceptedOfferIds.contains(offer.getId())).collect(Collectors.toList());
    }

    public static boolean assetConflicts(PodInstanceRequirement asset, Collection<PodInstanceRequirement> dirtyAssets) {
        return dirtyAssets.stream()
                .filter(dirtyAsset -> asset.conflicts(dirtyAsset))
                .count() > 0;
    }

    public static List<PlanManager> getActivePlanManagers(List<PlanManager> planManagers) {
        return planManagers.stream()
                .filter(planManager -> !planManager.getPlan().isInterrupted())
                .collect(Collectors.toList());
    }
}
