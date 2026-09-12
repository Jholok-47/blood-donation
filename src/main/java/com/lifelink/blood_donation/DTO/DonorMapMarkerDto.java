package com.lifelink.blood_donation.DTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class DonorMapMarkerDto {
    private Long donorId;
    private String fullName;
    private String bloodGroup;
    private String district;
    private String phone;
    private Double latitude;
    private Double longitude;
}
