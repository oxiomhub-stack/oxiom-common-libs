package com.oxiomhub.common.event;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Default publisher: logs and drops events. Runs locally and in tests without AWS. */
public class NoOpDomainEventPublisher implements DomainEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpDomainEventPublisher.class);

    @Override
    public void publish(String type, String subjectId, Object data) {
        log.debug("Domain event (no-op): type={} subject={} data={}", type, subjectId, data);
    }
}
