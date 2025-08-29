package org.openhab.binding.parkeergroningen.internal.api.dto;

import java.time.ZonedDateTime;

public class PermitDTO {
    private ZonedDateTime dateFrom;
    private ZonedDateTime dateUntil;
    private LicensePlateDTO licensePlate;
    private int permitMediaTypeID;
    private String permitMediaCode;

    public PermitDTO() {
    }

    public PermitDTO(ZonedDateTime dateFrom, ZonedDateTime dateUntil, LicensePlateDTO licensePlate,
            int permitMediaTypeID, String permitMediaCode) {
        this.dateFrom = dateFrom;
        this.dateUntil = dateUntil;
        this.licensePlate = licensePlate;
        this.permitMediaTypeID = permitMediaTypeID;
        this.permitMediaCode = permitMediaCode;
    }

    public ZonedDateTime getDateFrom() {
        return dateFrom;
    }

    public void setDateFrom(ZonedDateTime dateFrom) {
        this.dateFrom = dateFrom;
    }

    public ZonedDateTime getDateUntil() {
        return dateUntil;
    }

    public void setDateUntil(ZonedDateTime dateUntil) {
        this.dateUntil = dateUntil;
    }

    public String getLicensePlate() {
        return this.licensePlate.getName();
    }

    public void setLicensePlate(String licensePlate) {
        this.licensePlate = new LicensePlateDTO();
        this.licensePlate.setName("openHAB automation");
        this.licensePlate.setValue(licensePlate);
    }

    public int getPermitMediaTypeID() {
        return permitMediaTypeID;
    }

    public void setPermitMediaTypeID(int permitMediaTypeID) {
        this.permitMediaTypeID = permitMediaTypeID;
    }

    public String getPermitMediaCode() {
        return permitMediaCode;
    }

    public void setPermitMediaCode(String permitMediaCode) {
        this.permitMediaCode = permitMediaCode;
    }
}
