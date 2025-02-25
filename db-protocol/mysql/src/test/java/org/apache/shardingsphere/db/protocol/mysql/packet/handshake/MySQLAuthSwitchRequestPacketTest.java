/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.shardingsphere.db.protocol.mysql.packet.handshake;

import org.apache.shardingsphere.db.protocol.mysql.payload.MySQLPacketPayload;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public final class MySQLAuthSwitchRequestPacketTest {
    
    @Mock
    private MySQLAuthenticationPluginData authPluginData;
    
    @Mock
    private MySQLPacketPayload payload;
    
    @Test
    public void assertWrite() {
        when(authPluginData.getAuthenticationPluginData()).thenReturn(new byte[]{0x11, 0x22});
        MySQLAuthSwitchRequestPacket authSwitchRequestPacket = new MySQLAuthSwitchRequestPacket("plugin", authPluginData);
        authSwitchRequestPacket.write(payload);
        verify(payload).writeInt1(0xfe);
        verify(payload, times(2)).writeStringNul(anyString());
    }
}
