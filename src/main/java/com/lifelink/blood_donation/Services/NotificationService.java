package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Notification;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Entities.Enums.NotificationType;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.NotificationRepository;
import com.lifelink.blood_donation.Utils.BloodGroupFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public void notify(User recipient, BloodRequest bloodRequest, RequestAssign requestAssign,
                       NotificationType type, String message) {
        Notification notification = Notification.builder()
                .recipient(recipient)
                .bloodRequest(bloodRequest)
                .requestAssign(requestAssign)
                .type(type)
                .message(message)
                .isRead(false)
                .build();
        notificationRepository.save(notification);
    }

    public List<Notification> getMyNotifications(Long donorId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(donorId);
    }

    public long getUnreadCount(Long donorId) {
        return notificationRepository.countByRecipientIdAndIsReadFalse(donorId);
    }

    public void markAsRead(Long notificationId, Long requesterId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));

        if (!notification.getRecipient().getId().equals(requesterId)) {
            throw new InvalidOperationException("You cannot modify another user's notification.");
        }

        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyBroadcast(User donor, BloodRequest bloodRequest) {
        String message = "Blood needed: %s, %s unit(s) in %s (%s). Tap to respond."
                .formatted(BloodGroupFormatter.display(bloodRequest.getBloodGroup()),
                        bloodRequest.getQuantity(), bloodRequest.getDistrict(), bloodRequest.getUrgency());
        notificationRepository.save(Notification.builder()
                .recipient(donor)
                .bloodRequest(bloodRequest)
                .type(NotificationType.REQUEST_BROADCAST)
                .message(message)
                .build());
    }

    @Transactional
    public void notifySelected(RequestAssign assignment) {
        BloodRequest br = assignment.getBloodRequest();
        String message = "You have been selected for a %s request in %s. Please confirm you are on your way."
                .formatted(BloodGroupFormatter.display(br.getBloodGroup()), br.getDistrict());
        notificationRepository.save(Notification.builder()
                .recipient(assignment.getDonor())
                .bloodRequest(br)
                .requestAssign(assignment)
                .type(NotificationType.ASSIGNMENT)
                .message(message)
                .build());
    }
}
