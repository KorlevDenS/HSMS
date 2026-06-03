CREATE TABLE event_publication (
                                   id UUID PRIMARY KEY,
                                   event_type VARCHAR(255) NOT NULL,
                                   listener_id VARCHAR(255) NOT NULL,
                                   publication_date TIMESTAMP NOT NULL,
                                   completion_date TIMESTAMP NULL,
                                   last_resubmission_date TIMESTAMP NULL,
                                   completion_attempts INT NOT NULL,
                                   serialized_event TEXT NOT NULL,
                                   status VARCHAR(32) NOT NULL
);
