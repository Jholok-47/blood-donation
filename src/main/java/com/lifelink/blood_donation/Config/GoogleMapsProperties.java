package com.lifelink.blood_donation.Config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleMapsProperties {

    @Value("${google.maps.api.key:}")
    private String apiKey;

    @Value("${lifelink.maps.default-lat:23.8103}")
    private double defaultLat;

    @Value("${lifelink.maps.default-lng:90.4125}")
    private double defaultLng;

    @Value("${lifelink.maps.default-zoom:7}")
    private int defaultZoom;

    public String getApiKey() { return apiKey; }
    public double getDefaultLat() { return defaultLat; }
    public double getDefaultLng() { return defaultLng; }
    public int getDefaultZoom() { return defaultZoom; }
}