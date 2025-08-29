package org.openhab.binding.parkeergroningen.internal.api.controller;

import org.eclipse.jetty.client.HttpClient;
import org.openhab.binding.parkeergroningen.internal.api.dto.PermitDTO;

public class RestController {
    private final HttpClient httpClient;
    public PermitDTO data = new PermitDTO();

    public RestController(HttpClient httpClient) {
        this.httpClient = httpClient;
        data.getPermitMediaTypeID();
    }

    public void activate() {
        // TODO: Implement activation logic
        System.out.println("Activating permit: " + permit.getPermitMediaCode());
    }

    public void deactivate() {
        // TODO: Implement deactivation logic
        System.out.println("Deactivating permit: " + permit.getPermitMediaCode());
    }

    public void refresh() {
        // TODO: Implement refresh logic
        System.out.println("Refreshing permit: " + permit.getPermitMediaCode());
    }

    public boolean login(String cardNumber, String pin) {
        return true;
    }
}
