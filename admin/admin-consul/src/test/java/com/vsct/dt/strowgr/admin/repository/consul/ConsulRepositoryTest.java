/*
 *  Copyright (C) 2016 VSCT
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

package com.vsct.dt.strowgr.admin.repository.consul;

import com.vsct.dt.strowgr.admin.core.EntryPointKeyDefaultImpl;
import org.apache.http.HttpResponse;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;
import org.mockito.Mockito;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;
import static java.util.Optional.of;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class ConsulRepositoryTest {
    @Test
    public void should_lock_consul_resource() throws Exception {
        // given
        ConsulReader consulReader = mock(ConsulReader.class);
        CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
        ConsulRepository consulRepository = new ConsulRepository("localhost", 50080, 32000, 64_000, null, consulReader, closeableHttpClient);
        consulRepository = Mockito.spy(consulRepository);
        EntryPointKeyDefaultImpl entryPointKey = new EntryPointKeyDefaultImpl("UNIT/TEST");
        doReturn(of(new ConsulRepository.Session("a_session")))
                .when(consulRepository)
                .createSession(entryPointKey);
        when(consulReader.parseHttpResponse(any(HttpResponse.class), anyObject()))
                .thenReturn(of(TRUE));
        when(closeableHttpClient.execute(any(HttpPut.class), any(ResponseHandler.class))).thenReturn(of(TRUE));

        // test
        boolean locked = consulRepository.lock(entryPointKey);

        // check
        assertThat(locked).isTrue();
        verify(consulRepository).createSession(entryPointKey);
        verify(closeableHttpClient, times(1)).execute(any(HttpPut.class), any(ResponseHandler.class));
    }

    @Test
    public void should_return_not_locked_when_consul_call_fails_after_10_retries() throws Exception {
        // given
        ConsulReader consulReader = mock(ConsulReader.class);
        CloseableHttpClient closeableHttpClient = mock(CloseableHttpClient.class);
        ConsulRepository consulRepository = new ConsulRepository("localhost", 50080, 32000, 64_000, null, consulReader, closeableHttpClient);
        consulRepository = Mockito.spy(consulRepository);
        EntryPointKeyDefaultImpl entryPointKey = new EntryPointKeyDefaultImpl("UNIT/TEST");
        doReturn(of(new ConsulRepository.Session("a_session")))
                .when(consulRepository)
                .createSession(entryPointKey);
        when(consulReader.parseHttpResponse(any(HttpResponse.class), anyObject()))
                .thenReturn(of(TRUE));
        when(closeableHttpClient.execute(any(HttpPut.class), any(ResponseHandler.class))).thenReturn(of(FALSE));

        // test
        boolean locked = consulRepository.lock(entryPointKey);

        // check
        assertThat(locked).isFalse();
        verify(consulRepository).createSession(entryPointKey);
        verify(closeableHttpClient, times(10)).execute(any(HttpPut.class), any(ResponseHandler.class));
    }


}