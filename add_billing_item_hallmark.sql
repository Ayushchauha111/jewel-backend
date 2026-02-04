-- Add hallmark flag to billing_items (per-item: if true, hallmark charges apply on GST invoice)
ALTER TABLE billing_items
  ADD COLUMN hallmark BOOLEAN DEFAULT FALSE;
