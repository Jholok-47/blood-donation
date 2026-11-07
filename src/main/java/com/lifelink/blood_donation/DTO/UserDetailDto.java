package com.lifelink.blood_donation.DTO;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Enums.RequestStatus;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/** Read-only profile + activity summary of one user for the admin detail page. */
@Getter
@Builder
@AllArgsConstructor
public class UserDetailDto {
    private final User user;

    // donor side
    private final long assignmentsActive;
    private final long assignmentsCompleted;
    private final long assignmentsWithdrew;
    private final long assignmentsUnresponsive;
    private final long assignmentsAdminCancelled;
    private final long donations;
    private final double averageRating;
    private final long reviewCount;
    private final long callsReceived;
    private final long callsAccepted;
    private final long callsDeclined;
    private final long callsNoAnswer;
    private final List<RequestAssign> recentAssignments;

    // patient side
    private final long requestsTotal;
    private final Map<RequestStatus, Long> requestsByStatus;
    private final List<BloodRequest> recentRequests;
}
