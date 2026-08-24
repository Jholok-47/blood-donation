package com.lifelink.blood_donation.Services;

import com.lifelink.blood_donation.Entities.BloodRequest;
import com.lifelink.blood_donation.Entities.Notification;
import com.lifelink.blood_donation.Entities.RequestAssign;
import com.lifelink.blood_donation.Entities.User;
import com.lifelink.blood_donation.Entities.Enums.NotificationType;
import com.lifelink.blood_donation.Exceptions.InvalidOperationException;
import com.lifelink.blood_donation.Exceptions.ResourceNotFoundException;
import com.lifelink.blood_donation.Repositories.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
}
