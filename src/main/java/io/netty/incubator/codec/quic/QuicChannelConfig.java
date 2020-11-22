/*
 * Copyright 2020 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.netty.incubator.codec.quic;

import io.netty.channel.ChannelConfig;
import io.netty.channel.ChannelOption;

/**
 * A QUIC {@link ChannelConfig}.
 */
public interface QuicChannelConfig extends ChannelConfig {

    /**
     * Optional parameter to verify peer's certificate.
     * See <a href="https://docs.rs/quiche/0.6.0/quiche/fn.connect.html">server_name</a>.
     */
    ChannelOption<String> QUIC_PEER_CERT_SERVER_NAME = ChannelOption.valueOf("QUIC_PEER_CERT_SERVER_NAME");

    String getPeerCertServerName();

    ChannelConfig setPeerCertServerName(String peerCertServerName);
}
