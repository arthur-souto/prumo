"use client";

import { UserX, X } from "lucide-react";
import type { ReactNode } from "react";
import { Avatar } from "@/components/ui/Avatar";
import { Button } from "@/components/ui/Button";
import { FilterMenu } from "@/components/ui/FilterMenu";
import { useT } from "@/i18n/client";
import { PRIORITIES, type Column, type Member, type Priority, type Story } from "@/lib/board-types";
import { EMPTY_STORY_FILTERS, hasStoryFilters, toggle, type StoryFilters } from "@/lib/story-filters";
import { StorySearchInput } from "./StorySearchInput";
import styles from "./story-filter-bar.module.css";

const UNASSIGNED = "__unassigned";

type StoryFilterBarProps = {
  filters: StoryFilters;
  onChange: (filters: StoryFilters) => void;
  labels: string[];
  members: Member[];
  columns?: Column[];
  workspaceId?: string;
  onPick?: (story: Story) => void;
  children?: ReactNode;
};

export function StoryFilterBar({ filters, onChange, labels, members, columns, workspaceId, onPick, children }: StoryFilterBarProps) {
  const t = useT();
  const clear = t("filters.clear");
  const assignees = [...filters.assigneeIds, ...(filters.unassigned ? [UNASSIGNED] : [])];

  function toggleAssignee(value: string) {
    if (value === UNASSIGNED) {
      onChange({ ...filters, unassigned: !filters.unassigned });
    } else {
      onChange({ ...filters, assigneeIds: toggle(filters.assigneeIds, value) });
    }
  }

  return (
    <div className={styles.bar} role="search" aria-label={t("filters.label")}>
      <StorySearchInput
        value={filters.text}
        onChange={(text) => onChange({ ...filters, text })}
        workspaceId={workspaceId}
        onPick={onPick}
      />

      {columns ? (
        <FilterMenu
          label={t("backlog.status")}
          options={columns.map((column) => ({ value: column.id, label: column.name }))}
          selected={filters.columnIds}
          onToggle={(value) => onChange({ ...filters, columnIds: toggle(filters.columnIds, value) })}
          onClear={() => onChange({ ...filters, columnIds: [] })}
          clearLabel={clear}
        />
      ) : null}

      <FilterMenu
        label={t("backlog.priority")}
        options={PRIORITIES.map((priority) => ({ value: priority, label: t(`priority.${priority}`) }))}
        selected={filters.priorities}
        onToggle={(value) => onChange({ ...filters, priorities: toggle(filters.priorities, value as Priority) })}
        onClear={() => onChange({ ...filters, priorities: [] })}
        clearLabel={clear}
      />

      <FilterMenu
        label={t("story.labels")}
        options={labels.map((label) => ({ value: label, label }))}
        selected={filters.labels}
        onToggle={(value) => onChange({ ...filters, labels: toggle(filters.labels, value) })}
        onClear={() => onChange({ ...filters, labels: [] })}
        clearLabel={clear}
        emptyLabel={t("filters.noLabels")}
      />

      <FilterMenu
        label={t("filters.assignee")}
        options={[
          { value: UNASSIGNED, label: t("filters.unassigned"), icon: <UserX size={16} aria-hidden="true" /> },
          ...members.map((member) => ({
            value: member.id,
            label: member.name,
            icon: <Avatar name={member.name} seed={member.id} src={member.avatarUrl} size="sm" />,
          })),
        ]}
        selected={assignees}
        onToggle={toggleAssignee}
        onClear={() => onChange({ ...filters, assigneeIds: [], unassigned: false })}
        clearLabel={clear}
      />

      {hasStoryFilters(filters) ? (
        <Button appearance="subtle" size="sm" icon={<X size={14} aria-hidden="true" />} onClick={() => onChange(EMPTY_STORY_FILTERS)}>
          {t("board.clearFilters")}
        </Button>
      ) : null}

      {children ? <div className={styles.extra}>{children}</div> : null}
    </div>
  );
}
