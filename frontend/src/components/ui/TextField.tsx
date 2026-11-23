import { useId, type InputHTMLAttributes } from "react";
import styles from "./TextField.module.css";

type TextFieldProps = InputHTMLAttributes<HTMLInputElement> & {
  label: string;
  error?: string;
  hint?: string;
};

export function TextField({ label, error, hint, id, required, ...rest }: TextFieldProps) {
  const generated = useId();
  const inputId = id ?? generated;
  const messageId = `${inputId}-message`;

  return (
    <div className={styles.field}>
      <label className={styles.label} htmlFor={inputId}>
        {label}
        {required ? <span className={styles.required} aria-hidden="true"> *</span> : null}
      </label>
      <input
        id={inputId}
        className={`${styles.input} ${error ? styles.invalid : ""}`}
        required={required}
        aria-invalid={error ? true : undefined}
        aria-describedby={error || hint ? messageId : undefined}
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
