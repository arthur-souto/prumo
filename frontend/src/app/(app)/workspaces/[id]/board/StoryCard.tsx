"use client";

import { useSortable } from "@dnd-kit/sortable";
import { CSS } from "@dnd-kit/utilities";
import { GitBranch, GitMerge, GitPullRequest } from "lucide-react";
import { useT } from "@/i18n/client";
import type { Story } from "@/lib/board-types";
import { PriorityBadge } from "./PriorityBadge";
import styles from "./board.module.css";

type StoryCardProps = {
  story: Story;
  disabled: boolean;
  landed: boolean;
  onOpen: (story: Story) => void;
};

export function StoryCard({ story, disabled, landed, onOpen }: StoryCardProps) {
  const t = useT();
  const { attributes, listeners, setNodeRef, transform, transition, isDragging } = useSortable({
    id: story.id,
    disabled,
  });

  return (
    <li
      ref={setNodeRef}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      data-priority={story.priority}
      className={`${styles.card} ${isDragging ? styles.dragging : ""} ${landed ? styles.landed : ""}`}
      {...attributes}
      {...listeners}
    >
      <CardContent story={story} onOpen={onOpen} t={t} />
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

export function CardContent({
  story,
  onOpen,
  t,
}: {
  story: Story;
  onOpen?: (story: Story) => void;
  t: ReturnType<typeof useT>;
}) {
  const pullRequest = story.pullRequests.length > 0 ? pullRequestIcon(story) : null;

  return (
    <>
      <button type="button" className={styles.cardTitle} onClick={() => onOpen?.(story)}>
        {story.title}
      </button>

      {story.labels.length > 0 ? (
        <ul className={styles.labels}>
          {story.labels.map((label) => (
            <li key={label} className={styles.label}>
              {label}
            </li>
          ))}
        </ul>
      ) : null}

      <div className={styles.cardFooter}>
        <span className={styles.key}>{story.key}</span>
        <span className={styles.spacer} />
        {story.repositoryId ? (
          <span className={styles.icon} title={t("board.hasRepository")} role="img" aria-label={t("board.hasRepository")}>
            <GitBranch size={14} aria-hidden="true" />
          </span>
        ) : null}
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
        <PriorityBadge priority={story.priority} label={t(`priority.${story.priority}`)} />
        {story.storyPoints !== null ? (
          <span className={styles.points} title={t("board.points", { count: story.storyPoints })}>
            {story.storyPoints}
          </span>
        ) : null}
      </div>
    </>
  );
}
