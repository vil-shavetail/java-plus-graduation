-- Таблица заявок на участие
CREATE TABLE IF NOT EXISTS participation_requests (
    id BIGSERIAL PRIMARY KEY,
    created TIMESTAMP NOT NULL,
    event_id BIGINT NOT NULL,
    requester_id BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    UNIQUE (event_id, requester_id)
);

CREATE INDEX IF NOT EXISTS idx_participation_requests_event_id ON participation_requests(event_id);
CREATE INDEX IF NOT EXISTS idx_participation_requests_requester_id ON participation_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_participation_requests_status ON participation_requests(status);