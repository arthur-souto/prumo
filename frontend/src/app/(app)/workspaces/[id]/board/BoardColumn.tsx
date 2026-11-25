"use client";

import { useDroppable } from "@dnd-kit/core";
import { SortableContext, verticalListSortingStrategy } from "@dnd-kit/sortable";
import { ArrowLeft, ArrowRight, MoreHorizontal, Pencil, Plus, Trash2 } from "lucide-react";
import { Button } from "@/components/ui/Button";
import { DropdownContent, DropdownItem, DropdownMenu, DropdownSeparator, DropdownTrigger } from "@/components/ui/DropdownMenu";
import { IconButton } from "@/components/ui/IconButton";
import { useT } from "@/i18n/client";
import type { Column, Member, Sprint, Story } from "@/lib/board-types";
import { StoryCard } from "./StoryCard";
import styles from "./board.module.css";

type BoardColumnProps = {
  column: Column;
  members: Member[];
  sprints: Map<string, Sprint>;
  stories: Story[];
  allCount: number;
  filtering: boolean;
  landedId: string | null;
  dragDisabled: boolean;
  canManage: boolean;
  isFirst: boolean;
  isLast: boolean;
  onOpen: (story: Story) => void;
  onCreate: (columnId: string) => void;
  onRename: (column: Column) => void;
  onDelete: (column: Column) => void;
  onShift: (column: Column, direction: -1 | 1) => void;
};

export function BoardColumn({
  column,
  members,
  sprints,
  stories,
  allCount,
  filtering,
  landedId,
  dragDisabled,
  canManage,
  isFirst,
  isLast,
  onOpen,
  onCreate,
  onRename,
  onDelete,
  onShift,
}: BoardColumnProps) {
  const t = useT();
  const { setNodeRef, isOver } = useDroppable({ id: column.id });
  const icon = 16;

  return (
    <section className={`${styles.column} ${isOver ? styles.over : ""}`} aria-label={column.name}>
      <header className={styles.columnHeader}>
        <h2 className={styles.columnName}>
          {column.name}
          <span className={styles.count} aria-label={t("board.cardCount", { count: allCount })}>
            {filtering ? `${stories.length}/${allCount}` : allCount}
          </span>
        </h2>
        {canManage ? (
          <DropdownMenu>
            <DropdownTrigger>
              <IconButton size="sm" label={t("board.columnMenu")} icon={<MoreHorizontal size={icon} aria-hidden="true" />} />
            </DropdownTrigger>
            <DropdownContent>
              <DropdownItem icon={<Pencil size={icon} aria-hidden="true" />} onSelect={() => onRename(column)}>
                {t("board.rename")}
              </DropdownItem>
              <DropdownItem icon={<ArrowLeft size={icon} aria-hidden="true" />} disabled={isFirst} onSelect={() => onShift(column, -1)}>
                {t("board.moveLeft")}
              </DropdownItem>
              <DropdownItem icon={<ArrowRight size={icon} aria-hidden="true" />} disabled={isLast} onSelect={() => onShift(column, 1)}>
                {t("board.moveRight")}
              </DropdownItem>
              <DropdownSeparator />
              <DropdownItem icon={<Trash2 size={icon} aria-hidden="true" />} danger onSelect={() => onDelete(column)}>
                {t("board.deleteColumn")}
              </DropdownItem>
            </DropdownContent>
          </DropdownMenu>
        ) : null}
      </header>

      <SortableContext items={stories.map((story) => story.id)} strategy={verticalListSortingStrategy}>
        <ul ref={setNodeRef} className={styles.cards}>
          {stories.map((story) => (
            <StoryCard key={story.id} story={story} sprint={story.sprintId ? sprints.get(story.sprintId) : undefined} assignee={members.find((member) => member.id === story.assigneeId)} status={column.name} disabled={dragDisabled} landed={landedId === story.id} onOpen={onOpen} />
          ))}
          {stories.length === 0 ? <li className={styles.emptyColumn}>{filtering ? t("board.noMatchColumn") : t("board.emptyColumn")}</li> : null}
        </ul>
      </SortableContext>

      <div className={styles.columnFooter}>
        <Button appearance="subtle" size="sm" fullWidth icon={<Plus size={icon} aria-hidden="true" />} onClick={() => onCreate(column.id)}>
          {t("board.createStory")}
        </Button>
      </div>
    </section>
  );
}
