CREATE TABLE enquiries (
  id UUID PRIMARY KEY,
  public_id UUID NOT NULL UNIQUE,
  tracking_token_sha256 CHAR(64) NOT NULL,
  customer_name VARCHAR(140) NOT NULL,
  email VARCHAR(254) NOT NULL,
  phone VARCHAR(32) NOT NULL,
  product_code VARCHAR(32) NOT NULL,
  quantity INTEGER NOT NULL CHECK (quantity > 0),
  size VARCHAR(60),
  paper_gsm INTEGER,
  print_sides VARCHAR(24),
  finish VARCHAR(80),
  notes VARCHAR(2000),
  status VARCHAR(24) NOT NULL DEFAULT 'NEW',
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_enquiries_created_at ON enquiries(created_at DESC);
CREATE INDEX idx_enquiries_status ON enquiries(status);

CREATE TABLE enquiry_events (
  id UUID PRIMARY KEY,
  enquiry_id UUID NOT NULL REFERENCES enquiries(id) ON DELETE CASCADE,
  event_type VARCHAR(40) NOT NULL,
  from_status VARCHAR(24),
  to_status VARCHAR(24) NOT NULL,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
CREATE INDEX idx_enquiry_events_enquiry_created ON enquiry_events(enquiry_id, created_at);
