import type { ButtonHTMLAttributes } from 'react'
import { buttonClasses, type Variant } from './buttonClasses'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
}

// type="button" by default: a bare <button> inside a <form> is a submit button,
// a classic source of accidental submits.
export function Button({ variant = 'primary', className = '', type = 'button', ...props }: ButtonProps) {
  return <button type={type} className={buttonClasses(variant, className)} {...props} />
}
