CREATE TABLE staff_users (
  id UUID PRIMARY KEY,
  username VARCHAR(140) NOT NULL UNIQUE,
  password_hash VARCHAR(100) NOT NULL,
  role VARCHAR(32) NOT NULL CHECK (role IN ('SALES', 'OPERATOR')),
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

ALTER TABLE enquiries ADD COLUMN assigned_staff_id UUID REFERENCES staff_users(id);
ALTER TABLE enquiries ADD COLUMN source VARCHAR(24) NOT NULL DEFAULT 'WEBSITE';
ALTER TABLE enquiries ADD COLUMN priority VARCHAR(12) NOT NULL DEFAULT 'NORMAL';
ALTER TABLE enquiries ADD COLUMN next_action VARCHAR(500);
ALTER TABLE enquiries ADD COLUMN follow_up_at TIMESTAMPTZ;
ALTER TABLE enquiries ADD COLUMN revision BIGINT NOT NULL DEFAULT 0;
CREATE INDEX idx_enquiries_staff_followup ON enquiries(assigned_staff_id, follow_up_at);

CREATE TABLE lead_activities (
  id UUID PRIMARY KEY,
  enquiry_id UUID NOT NULL REFERENCES enquiries(id),
  actor_id UUID NOT NULL REFERENCES staff_users(id),
  action VARCHAR(40) NOT NULL,
  details VARCHAR(1200) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_lead_activities_history ON lead_activities(enquiry_id, created_at);
