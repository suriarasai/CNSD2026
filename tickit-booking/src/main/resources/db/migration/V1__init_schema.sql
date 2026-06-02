-- US1/US2/US3 schema. Table definitions mirror the JPA entities.

CREATE TABLE app_user (
    id    BIGSERIAL PRIMARY KEY,
    name  VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

CREATE TABLE event (
    id              BIGSERIAL PRIMARY KEY,
    name            VARCHAR(255) NOT NULL,
    venue           VARCHAR(255) NOT NULL,
    event_date      TIMESTAMP    NOT NULL,
    total_seats     INTEGER      NOT NULL CHECK (total_seats >= 0),
    available_seats INTEGER      NOT NULL CHECK (available_seats >= 0),
    version         BIGINT       NOT NULL DEFAULT 0
);

CREATE TABLE booking (
    id         BIGSERIAL PRIMARY KEY,
    user_id    BIGINT       NOT NULL REFERENCES app_user (id),
    event_id   BIGINT       NOT NULL REFERENCES event (id),
    quantity   INTEGER      NOT NULL CHECK (quantity > 0),
    status     VARCHAR(20)  NOT NULL,
    created_at TIMESTAMP    NOT NULL
);

CREATE INDEX idx_booking_user ON booking (user_id);
CREATE INDEX idx_booking_event ON booking (event_id);
CREATE INDEX idx_event_date ON event (event_date);
