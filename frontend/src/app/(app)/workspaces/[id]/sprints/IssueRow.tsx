"use client";

import { useDraggable } from "@dnd-kit/core";
import { ArrowRightLeft, Bookmark, MoreHorizontal, User } from "lucide-react";
import type { Ref } from "react";
import { Avatar } from "@/components/ui/Avatar";
import { DropdownContent, DropdownItem, DropdownLabel, DropdownMenu, DropdownTrigger } from "@/components/ui/DropdownMenu";
import { IconButton } from "@/components/ui/IconButton";
import { useT } from "@/i18n/client";
import type { Column, Member, Sprint, Story } from "@/lib/board-types";
import { PriorityBadge } from "../board/PriorityBadge";
import { groupOf } from "./planning";
import styles from "./planning.module.css";

type IssueRowProps = {
  story: Story;
  column: Column | undefined;
  assignee: Member | undefined;
  targets: Sprint[];
  inBacklog: boolean;
  onOpen: (story: Story) => void;
  onMove: (story: Story, sprintId: string | null) => void;
};

export function IssueRow({ story, column, assignee, targets, inBacklog, onOpen, onMove }: IssueRowProps) {
  const t = useT();
  const { attributes, listeners, setNodeRef, isDragging } = useDraggable({ id: story.id });

  return (
    <li
      ref={setNodeRef as Ref<HTMLLIElement>}
      className={styles.row}
      data-dragging={isDragging ? "true" : undefined}
      {...attributes}
      {...listeners}
      onClick={() => onOpen(story)}
    >
      <IssueContent story={story} column={column} assignee={assignee} />
      <span className={styles.rowMenu} onClick={(event) => event.stopPropagation()} onPointerDown={(event) => event.stopPropagation()}>
        <DropdownMenu>
          <DropdownTrigger>
            <IconButton size="sm" label={t("story.moreActions")} icon={<MoreHorizontal size={16} aria-hidden="true" />} />
          </DropdownTrigger>
          <DropdownContent>
            <DropdownLabel>{t("sprints.moveTo")}</DropdownLabel>
            {targets.map((sprint) => (
              <DropdownItem key={sprint.id} icon={<ArrowRightLeft size={16} aria-hidden="true" />} onSelect={() => onMove(story, sprint.id)}>
                {sprint.name}
              </DropdownItem>
            ))}
            {inBacklog ? null : (
              <DropdownItem icon={<ArrowRightLeft size={16} aria-hidden="true" />} onSelect={() => onMove(story, null)}>
                {t("sprints.backlog")}
              </DropdownItem>
            )}
          </DropdownContent>
        </DropdownMenu>
      </span>
    </li>
  );
}

export function IssueContent({ story, column, assignee }: { story: Story; column: Column | undefined; assignee: Member | undefined }) {
  const t = useT();

  return (
    <>
      <Bookmark size={14} aria-hidden="true" className={styles.typeIcon} />
      <span className={styles.rowKey} data-done={groupOf(column?.kind) === "done" ? "true" : undefined}>
        {story.key}
      </span>
      <span className={styles.rowTitle}>{story.title}</span>
      {story.labels.slice(0, 2).map((label) => (
        <span key={label} className={styles.rowLabel}>
          {label}
        </span>
      ))}
      <span className={styles.status} data-group={groupOf(column?.kind)}>
        {column?.name ?? ""}
      </span>
      <PriorityBadge priority={story.priority} label={t(`priority.${story.priority}`)} />
      <span className={styles.rowPoints} title={t("story.points")}>
        {story.storyPoints ?? "–"}
      </span>
      {assignee ? (
        <span title={assignee.name} className={styles.rowAvatar}>
          <Avatar name={assignee.name} seed={assignee.id} src={assignee.avatarUrl} size="sm" />
        </span>
      ) : (
        <span className={styles.nobody} title={t("story.unassigned")} role="img" aria-label={t("story.unassigned")}>
          <User size={12} aria-hidden="true" />
        </span>
      )}
    </>
  );
}
