/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.zwavejs.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Objects;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.openhab.binding.zwavejs.internal.api.dto.messages.BaseMessage;
import org.openhab.binding.zwavejs.internal.api.dto.messages.EventMessage;
import org.openhab.binding.zwavejs.internal.api.dto.messages.ResultMessage;
import org.openhab.binding.zwavejs.internal.api.dto.messages.VersionMessage;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.typeadapters.RuntimeTypeAdapterFactory;

/**
 * @author Leo siepel - Initial contribution
 */
@NonNullByDefault
public class BaseTest {

    /**
     * If a scene already has a group ID, it should applicable to the group with the given ID.
     */
    @Disabled
    @Test
    public void testJsonParsing() {
        RuntimeTypeAdapterFactory<BaseMessage> typeAdapterFactory = RuntimeTypeAdapterFactory.of(BaseMessage.class,
                "type", true);
        typeAdapterFactory.registerSubtype(VersionMessage.class, "version") //
                .registerSubtype(ResultMessage.class, "result") //
                .registerSubtype(EventMessage.class, "event"); //
        Gson gson = new GsonBuilder().registerTypeAdapterFactory(typeAdapterFactory).create();
        String json = "{\"type\":\"result\",\"success\":true,\"result\":{}}";

        BaseMessage baseEvent = Objects.requireNonNull(gson.fromJson(json, BaseMessage.class));

        assertThat(baseEvent.type, is("result"));
    }
}
