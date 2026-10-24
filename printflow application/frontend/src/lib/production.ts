export type ProductionAttentionState = {
  stage: string;
  hold_reason: string | null;
  active_rework?: boolean;
};

/** Current attention excludes held work and historical rework alone. */
export function needsQcReworkAttention(job: ProductionAttentionState): boolean {
  return !job.hold_reason && (job.stage === 'QC' || job.active_rework === true);
}
