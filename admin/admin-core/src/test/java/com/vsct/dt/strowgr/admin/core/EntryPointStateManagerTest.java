/*
 * Copyright (C) 2016 VSCT
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.vsct.dt.strowgr.admin.core;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPoint;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointBackend;
import com.vsct.dt.strowgr.admin.core.configuration.EntryPointFrontend;
import com.vsct.dt.strowgr.admin.core.event.CorrelationId;
import com.vsct.dt.strowgr.admin.core.repository.EntryPointRepository;
import org.junit.Before;
import org.junit.Test;

import java.util.Optional;

import static org.mockito.Mockito.*;

public class EntryPointStateManagerTest {

    EntryPointStateManager entryPointStateManager;
    EntryPointRepository repositoryMock;

    @Before
    public void setUp() {
        repositoryMock = mock(EntryPointRepository.class);
        entryPointStateManager = new EntryPointStateManager(10, repositoryMock);
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_committing_one__should_add_pending_configuration_if_it_is_different_than_committing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint differentNewConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint committingConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser2")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(committingConfiguration)
        );

        entryPointStateManager.prepare(key, differentNewConfiguration);

        verify(repositoryMock).setPendingConfiguration(key, differentNewConfiguration);
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_committing_one__should_not_add_pending_configuration_if_it_is_same_as_committing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint sameNewConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint committingConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(committingConfiguration)
        );

        entryPointStateManager.prepare(key, sameNewConfiguration);

        verify(repositoryMock, never()).setPendingConfiguration(any(), any());
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_nothing_is_committing__should_add_pending_configuration_if_it_is_different_than_current() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint differentNewConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint currentConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser2")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );
        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.of(currentConfiguration)
        );

        entryPointStateManager.prepare(key, differentNewConfiguration);

        verify(repositoryMock).setPendingConfiguration(key, differentNewConfiguration);
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_nothing_is_committing__should_not_add_pending_configuration_if_it_is_same_as_current() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint sameNewConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint currentConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );
        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.of(currentConfiguration)
        );

        entryPointStateManager.prepare(key, sameNewConfiguration);

        verify(repositoryMock, never()).setPendingConfiguration(any(), any());
    }

    /* This test relies on equals method based on state of the object rather than entity */
    @Test
    public void prepare_configuration_when_nothing_is_committing_or_current__should_add_current_configuration() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint newConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );
        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.prepare(key, newConfiguration);

        verify(repositoryMock).setCurrentConfiguration(key, newConfiguration);
    }

    @Test
    public void try_commit_pending_configuration__with_pending_without_committing_should_create_committing_from_pending() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint pendingConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();


        when(repositoryMock.getPendingConfiguration(key)).thenReturn(
                Optional.of(pendingConfiguration)
        );
        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );

        String correlationId = CorrelationId.newCorrelationId();
        when(repositoryMock.setCommittingConfiguration(correlationId, key, pendingConfiguration, 10)).thenReturn(true);

        entryPointStateManager.tryCommitPending(correlationId, key);


        verify(repositoryMock).setCommittingConfiguration(correlationId, key, pendingConfiguration, 10);
        verify(repositoryMock).removePendingConfiguration(key);
    }

    @Test
    public void try_commit_pending_configuration__with_pending_with_committing_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint pendingConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint existingCommittingConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getPendingConfiguration(key)).thenReturn(
                Optional.of(pendingConfiguration)
        );
        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(existingCommittingConfiguration)
        );

        entryPointStateManager.tryCommitPending(CorrelationId.newCorrelationId(), key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any(), any(), anyInt());
        verify(repositoryMock, never()).removePendingConfiguration(any());
    }

    @Test
    public void try_commit_pending_configuration__without_pending_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        when(repositoryMock.getPendingConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.tryCommitPending(CorrelationId.newCorrelationId(), key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any(), any(), anyInt());
        verify(repositoryMock, never()).removePendingConfiguration(any());
    }

    @Test
    public void try_commit_current_configuration__with_current_without_committing_should_create_committing_from_current() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint currentConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.of(currentConfiguration)
        );
        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );

        String correlationId = CorrelationId.newCorrelationId();
        entryPointStateManager.tryCommitCurrent(correlationId, key);

        verify(repositoryMock).setCommittingConfiguration(eq(correlationId), eq(key), eq(currentConfiguration), eq(10));
    }

    @Test
    public void try_commit_current_configuration__with_current_with_committing_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint currentConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        EntryPoint existingCommittingConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.of(currentConfiguration)
        );
        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(existingCommittingConfiguration)
        );

        entryPointStateManager.tryCommitCurrent(CorrelationId.newCorrelationId(), key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any(), any(), anyInt());
    }

    @Test
    public void try_commit_current_configuration__without_current_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        when(repositoryMock.getCurrentConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.tryCommitCurrent(CorrelationId.newCorrelationId(), key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any(), any(), anyInt());
    }

    @Test
    public void commit_configuration__without_committing_should_do_nothing() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.empty()
        );

        entryPointStateManager.commit(key);

        verify(repositoryMock, never()).setCommittingConfiguration(any(), any(), any(), anyInt());
    }

    @Test
    public void commit_configuration__with_committing_should_replace_current_by_committing_and_leave_no_committing_and_no_pending() {
        EntryPointKey key = new EntryPointKeyDefaultImpl("some_key");

        EntryPoint committingConfiguration = EntryPoint
                .onHaproxy("haproxy", 0)
                .withUser("hapuser")
                .withVersion("hapVersion")
                .definesFrontends(ImmutableSet.<EntryPointFrontend>of())
                .definesBackends(ImmutableSet.<EntryPointBackend>of())
                .withGlobalContext(ImmutableMap.<String, String>of())
                .build();

        when(repositoryMock.getCommittingConfiguration(key)).thenReturn(
                Optional.of(committingConfiguration)
        );

        entryPointStateManager.commit(key);

        verify(repositoryMock).setCurrentConfiguration(eq(key), eq(committingConfiguration));
        verify(repositoryMock).removeCommittingConfiguration(key);
        verify(repositoryMock, never()).removePendingConfiguration(key);
    }

}
