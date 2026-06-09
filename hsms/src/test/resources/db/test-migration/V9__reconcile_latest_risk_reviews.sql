UPDATE mission
SET risk_snapshot_id = (
    SELECT r.id
    FROM risk_snapshot r
    WHERE r.mission_id = mission.id
      AND r.stale = FALSE
    ORDER BY r.calculated_at DESC, r.id DESC
    LIMIT 1
)
WHERE EXISTS (
    SELECT 1
    FROM risk_snapshot r
    WHERE r.mission_id = mission.id
      AND r.stale = FALSE
);

UPDATE mission
SET risk_review_required_at = NULL,
    risk_review_reason = NULL,
    updated_at = (
        SELECT CASE
            WHEN mission.updated_at < r.calculated_at THEN r.calculated_at
            ELSE mission.updated_at
        END
        FROM risk_snapshot r
        WHERE r.mission_id = mission.id
          AND r.stale = FALSE
          AND r.calculated_at >= mission.risk_review_required_at
        ORDER BY r.calculated_at DESC, r.id DESC
        LIMIT 1
    )
WHERE risk_review_required_at IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM risk_snapshot r
      WHERE r.mission_id = mission.id
        AND r.stale = FALSE
        AND r.calculated_at >= mission.risk_review_required_at
  );
