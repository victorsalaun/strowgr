package com.vsct.dt.haas;

import com.google.common.eventbus.EventBus;
import com.vsct.dt.haas.events.AddNewEntryPointEvent;
import com.vsct.dt.haas.events.AddNewServerEvent;
import com.vsct.dt.haas.state.*;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.fest.assertions.Assertions.assertThat;

/**
 * Created by william_montaz on 02/02/2016.
 */
public class ScenarioAddServer {


    EventBus eventBus = new EventBus();
    AdminState adminState;

    @Before
    public void setUp(){
        adminState = new AdminState();
        eventBus.register(adminState);
    }

    @Test
    public void scenario_add_new_server_should_add_pending_ep(){

        Haproxy haproxy = new Haproxy("ip_master", "ip_slave");
        EntryPoint entryPoint = new EntryPoint(haproxy, "OCE", "REC1", "hapocer1", "54250", EntryPointStatus.DEPLOYED);
        adminState.putEntryPoint(entryPoint);

        Server server = new Server("instance_name", "server_name", "ip", "port");

        AddNewServerEvent addNewServerEvent = new AddNewServerEvent("OCE", "REC1", "BACKEND", server);

        eventBus.post(addNewServerEvent);

        Optional<PendingEntryPoint> ep = adminState.getPendingEntryPoint("OCE", "REC1");
        assertThat(ep.isPresent()).isTrue();
        assertThat(ep.get().getStatus().equals(EntryPointStatus.DEPLOYING));

    }



}
