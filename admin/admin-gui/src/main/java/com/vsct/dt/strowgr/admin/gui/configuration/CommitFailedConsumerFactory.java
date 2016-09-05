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

package com.vsct.dt.strowgr.admin.gui.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.brainlag.nsq.NSQConsumer;
import com.github.brainlag.nsq.lookup.NSQLookup;
import com.vsct.dt.strowgr.admin.core.event.in.CommitFailureEvent;
import com.vsct.dt.strowgr.admin.nsq.consumer.EntryPointKeyVsctImpl;
import com.vsct.dt.strowgr.admin.nsq.payload.CommitFailed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Configuration factory from Dropwizard for CommitFailedConsumer NSQ.
 */
public class CommitFailedConsumerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommitFailedConsumerFactory.class);

    private static final String TOPIC_PREFIX = "commit_failed_";

    private final NSQLookup                    nsqLookup;
    private final ObjectMapper                 objectMapper;
    private final Consumer<CommitFailureEvent> consumer;

    public CommitFailedConsumerFactory(NSQLookup nsqLookup, ObjectMapper objectMapper, Consumer<CommitFailureEvent> consumer) {
        this.nsqLookup = nsqLookup;
        this.objectMapper = objectMapper;
        this.consumer = consumer;
    }

    public NSQConsumer build(String haproxy) {
        return new NSQConsumer(nsqLookup, TOPIC_PREFIX + haproxy, "admin", (message) -> {
            CommitFailed commitFailed = null;
            try {
                commitFailed = objectMapper.readValue(message.getMessage(), CommitFailed.class);
            } catch (IOException e) {
                LOGGER.error("can't deserialize the commitFailed:" + new String(message.getMessage()), e);
                //Avoid republishing message and stop processing
                message.finished();
                return;
            }

            CommitFailureEvent event = new CommitFailureEvent(commitFailed.getHeader().getCorrelationId(), new EntryPointKeyVsctImpl(commitFailed.getHeader().getApplication(), commitFailed.getHeader().getPlatform()));
            consumer.accept(event);
            message.finished();
        });
    }

}
