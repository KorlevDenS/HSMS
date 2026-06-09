WITH ranked_current_risk AS (
    SELECT
        id,
        mission_id,
        calculated_at,
        ROW_NUMBER() OVER (PARTITION BY mission_id ORDER BY calculated_at DESC, id DESC) AS row_number
    FROM risk_snapshot
    WHERE stale = FALSE
),
latest_current_risk AS (
    SELECT id, mission_id, calculated_at
    FROM ranked_current_risk
    WHERE row_number = 1
)
UPDATE mission m
SET risk_snapshot_id = latest_current_risk.id,
    risk_review_required_at = CASE
        WHEN m.risk_review_required_at IS NOT NULL
            AND latest_current_risk.calculated_at >= m.risk_review_required_at
            THEN NULL
        ELSE m.risk_review_required_at
    END,
    risk_review_reason = CASE
        WHEN m.risk_review_required_at IS NOT NULL
            AND latest_current_risk.calculated_at >= m.risk_review_required_at
            THEN NULL
        ELSE m.risk_review_reason
    END,
    updated_at = GREATEST(m.updated_at, latest_current_risk.calculated_at)
FROM latest_current_risk
WHERE m.id = latest_current_risk.mission_id
  AND (
      m.risk_snapshot_id IS DISTINCT FROM latest_current_risk.id
      OR (
          m.risk_review_required_at IS NOT NULL
          AND latest_current_risk.calculated_at >= m.risk_review_required_at
      )
  );
