-- ============================================================
--  Notification Service — V1: Base Schema
-- ============================================================

CREATE TABLE notifications (
    id                UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id           UUID NOT NULL,
    title             VARCHAR(255) NOT NULL,
    content           TEXT NOT NULL,
    type              VARCHAR(50) NOT NULL,
    is_read           BOOLEAN NOT NULL DEFAULT FALSE,
    related_entity_id UUID,
    created_at        TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_notifications_user_id ON notifications(user_id);
CREATE INDEX idx_notifications_is_read ON notifications(is_read);
