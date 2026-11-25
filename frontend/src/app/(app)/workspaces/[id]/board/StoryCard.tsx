"use client";

import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { Bookmark, GitBranch, GitMerge, GitPullRequest, ListChecks, User } from "lucide-react";
import { useT } from "@/i18n/client";
import { Avatar } from "@/components/ui/Avatar";
import type { Member, Story } from "@/lib/board-types";
import { GithubOrigin } from "./GithubOrigin";
import { PriorityBadge } from "./PriorityBadge";
import styles from "./board.module.css";

type StoryCardProps = {
  story: Story;
  disabled: boolean;
  landed: boolean;
  status?: string;
  assignee?: Member;
  onOpen: (story: Story) => void;
};

export function StoryCard({ story, disabled, landed, status, assignee, onOpen }: StoryCardProps) {
  const t = useT();
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: story.id,
    disabled,
  });

  return (
    <li
      ref={setNodeRef}
      data-priority={story.priority}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={`${styles.card} ${isDragging ? styles.dragging : ""} ${landed ? styles.landed : ""}`}
      {...attributes}
      {...listeners}
    >
      <CardContent story={story} status={status} assignee={assignee} onOpen={onOpen} t={t} />
    </li>
  );
}

function pullRequestIcon(story: Story) {
  if (story.pullRequests.some((pullRequest) => pullRequest.state === "OPEN")) {
    return { Icon: GitPullRequest, className: styles.prOpen };
  }

  if (story.pullRequests.some((pullRequest) => pullRequest.state === "MERGED")) {
    return { Icon: GitMerge, className: styles.prMerged };
  }

  return { Icon: GitPullRequest, className: "" };
}

/** The description as plain text for the card: markdown marks dropped, whitespace collapsed. */
function excerpt(markdown: string | null) {
  if (!markdown) return "";

  return markdown
    .replace(/```[\s\S]*?```/g, " ")
    .replace(/!?\[([^\]]*)\]\([^)]*\)/g, "$1")
    .replace(/[#>*_`~-]+/g, " ")
    .replace(/\s+/g, " ")
    .trim();
}

/** Stable colour per label, so the same label always looks the same. */
function labelTone(label: string) {
  let hash = 0;
  for (const char of label) hash = (hash * 31 + char.charCodeAt(0)) % 6;
  return hash + 1;
}

export function CardContent({
  story,
  status,
  assignee,
  onOpen,
  t,
}: {
  story: Story;
  status?: string;
  assignee?: Member;
  onOpen?: (story: Story) => void;
  t: ReturnType<typeof useT>;
}) {
  const pullRequest = story.pullRequests.length > 0 ? pullRequestIcon(story) : null;
  const description = excerpt(story.description);

  return (
    <>
      <button type="button" className={styles.cardTitle} onClick={() => onOpen?.(story)}>
        {story.title}
      </button>

      {description ? <p className={styles.description}>{description}</p> : null}

      {story.labels.length > 0 ? (
        <ul className={styles.labels}>
          {story.labels.map((label) => (
            <li key={label} className={styles.label} style={{ background: `var(--avatar-${labelTone(label)})` }}>
              {label}
            </li>
          ))}
        </ul>
      ) : null}

      {status ? <span className={styles.status}>{status}</span> : null}

      <div className={styles.cardFooter}>
        <Bookmark size={14} aria-hidden="true" className={styles.typeIcon} />
        <span className={styles.key}>{story.key}</span>
        <span className={styles.cardMeta}>
          {story.repositoryId ? (
            <span className={styles.icon} title={t("board.hasRepository")} role="img" aria-label={t("board.hasRepository")}>
              <GitBranch size={14} aria-hidden="true" />
            </span>
          ) : null}
          {story.subtasks && story.subtasks.total > 0 ? (
            <span
              className={`${styles.subtaskCount} ${story.subtasks.done === story.subtasks.total ? styles.subtasksDone : ""}`}
              title={t("board.subtaskProgress", { done: story.subtasks.done, total: story.subtasks.total })}
              role="img"
              aria-label={t("board.subtaskProgress", { done: story.subtasks.done, total: story.subtasks.total })}
            >
              <ListChecks size={14} aria-hidden="true" />
              {story.subtasks.done}/{story.subtasks.total}
            </span>
          ) : null}
          {story.columnSource === "GITHUB" ? <GithubOrigin compact action="column" /> : null}
          {pullRequest ? (
            <span
              className={`${styles.icon} ${pullRequest.className}`}
              title={t("board.hasPullRequest")}
              role="img"
              aria-label={t("board.hasPullRequest")}
            >
              <pullRequest.Icon size={14} aria-hidden="true" />
            </span>
          ) : null}
          {story.storyPoints !== null ? (
            <span className={styles.points} title={t("board.points", { count: story.storyPoints })}>
              {story.storyPoints}
            </span>
          ) : null}
          <PriorityBadge priority={story.priority} label={t(`priority.${story.priority}`)} />
          {assignee ? (
            <span className={styles.assignee} title={assignee.name}>
              <Avatar name={assignee.name} seed={assignee.id} src={assignee.avatarUrl} size="sm" />
            </span>
          ) : (
            <span className={`${styles.assignee} ${styles.unassigned}`} title={t("story.unassigned")} role="img" aria-label={t("story.unassigned")}>
              <User size={14} aria-hidden="true" />
            </span>
          )}
        </span>
      </div>
    </>
  );
}
