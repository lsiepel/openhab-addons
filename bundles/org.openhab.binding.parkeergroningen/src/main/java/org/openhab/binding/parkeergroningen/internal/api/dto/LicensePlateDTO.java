package org.openhab.binding.parkeergroningen.internal.api.dto;

public class LicensePlateDTO {
    private String value;
    private String name;

    public LicensePlateDTO() {
    }

    public LicensePlateDTO(String value, String name) {
        this.value = value;
        this.name = name;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
