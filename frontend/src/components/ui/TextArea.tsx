import { useId, type TextareaHTMLAttributes } from "react";
import styles from "./TextField.module.css";

type TextAreaProps = TextareaHTMLAttributes<HTMLTextAreaElement> & {
  label: string;
  error?: string;
  hint?: string;
};

export function TextArea({ label, error, hint, id, required, ...rest }: TextAreaProps) {
  const generated = useId();
  const inputId = id ?? generated;
  const messageId = `${inputId}-message`;

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={inputId}>
        {label}
        {required ? <span className={styles.required} aria-hidden="true"> *</span> : null}
      </label>
      <textarea
        id={inputId}
        className={`${styles.input} ${styles.multiline} ${error ? styles.invalid : ""}`}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || hint ? messageId : undefined}
        rows={3}
        {...rest}
      />
      {error ? (
        <p id={messageId} className={styles.error} role="alert">
          {error}
        </p>
      ) : hint ? (
        <p id={messageId} className={styles.hint}>
          {hint}
        </p>
      ) : null}
    </div>
  );
}
