package com.mobilewalla.eventtracking.model;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Data
@ToString
public class Event {
    @NonNull
    @PrimaryKey()
    public String uuid;
    @ColumnInfo(name = "USER_ID")
    public String userId;
    @ColumnInfo(name = "EVENT_ID")
    public long eventId;
    @ColumnInfo(name = "EVENT_TYPE")
    public String eventType;
    @ColumnInfo(name = "EVENT_PROPERTIES")
    public String eventProperties;
    @ColumnInfo(name = "USER_PROPERTIES")
    public String userProperties;
    @ColumnInfo(name = "GLOBAL_USER_PROPERTIES")
    public String globalUserProperties;
    @ColumnInfo(name = "GROUP_PROPERTIES")
    public String groupProperties;
    @ColumnInfo(name = "APP")
    public long app;
    @ColumnInfo(name = "DEVICE_ID")
    public String deviceId;
    @ColumnInfo(name = "SESSION_ID")
    public long sessionId;
    @ColumnInfo(name = "VERSION_NAME")
    public String versionName;
    @ColumnInfo(name = "PLATFORM")
    public String platform;
    @ColumnInfo(name = "OS_NAME")
    public String osName;
    @ColumnInfo(name = "OS_VERSION")
    public String osVersion;
    @ColumnInfo(name = "DEVICE_BRAND")
    public String deviceBrand;
    @ColumnInfo(name = "DEVICE_MANUFACTURER")
    public String deviceManufacturer;
    @ColumnInfo(name = "DEVICE_MODEL")
    public String deviceModel;
    @ColumnInfo(name = "DEVICE_FAMILY")
    public String deviceFamily;
    @ColumnInfo(name = "DEVICE_TYPE")
    public String deviceType;
    @ColumnInfo(name = "DEVICE_CARRIER")
    public String deviceCarrier;
    @ColumnInfo(name = "latitude")
    public double latitude;
    @ColumnInfo(name = "longitude")
    public double longitude;
    @ColumnInfo(name = "IPADDRESS")
    public String ipAddress;
    @ColumnInfo(name = "country")
    public String country;
    @ColumnInfo(name = "language")
    public String language;
    @ColumnInfo(name = "library")
    public String library;
    @ColumnInfo(name = "city")
    public String city;
    @ColumnInfo(name = "region")
    public String region;
    @ColumnInfo(name = "EVENT_TIME")
    public String eventTime;
    @ColumnInfo(name = "SERVER_UPLOAD_TIME")
    public String serverUploadTime;
    @ColumnInfo(name = "SERVER_RECEIVED_TIME")
    public String serverReceivedTime;
    @ColumnInfo(name = "IDFA")
    public String idfa;
    @ColumnInfo(name = "ADID")
    public String adid;
    @ColumnInfo(name = "START_VERSION")
    public String startVersion;
    @ColumnInfo(name = "CLIENT_EVENT_TIME")
    public String clientEventTime;
    @ColumnInfo(name = "USER_CREATION_TIME")
    public String userCreationTime;
    @ColumnInfo(name = "CLIENT_UPLOAD_TIME")
    public String clientUploadTime;
    @ColumnInfo(name = "PROCESSED_TIME")
    public String processedTime;
}