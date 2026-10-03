import { useMutation } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate, type Location } from 'react-router'
import { api, ApiError, unwrap } from '../api/client'
import { useAuth } from '../auth/useAuth'
import { Alert } from '../components/ui/Alert'
import { Button } from '../components/ui/Button'
import { TextField } from '../components/ui/Field'

type Mode = 'login' | 'register'

export function AuthPage({ mode }: { mode: Mode }) {
  const { isAuthenticated, login } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')

  // RequireAuth stored the page the user originally asked for.
  const from = (location.state as { from?: Location } | null)?.from?.pathname ?? '/'

  const submit = useMutation({
    mutationFn: () => {
      const body = { username, password }
      return mode === 'login'
        ? unwrap(api.POST('/api/v1/auth/login', { body }))
        : unwrap(api.POST('/api/v1/auth/register', { body }))
    },
    onSuccess: (token) => {
      login(token.accessToken)
      navigate(from, { replace: true })
    },
  })

  if (isAuthenticated) {
    return <Navigate to={from} replace />
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault() // stop the browser's full-page form POST; we send JSON instead
    submit.mutate()
  }

  const error = submit.error instanceof ApiError ? submit.error : null
  const isLogin = mode === 'login'

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm">
        <div className="mb-8 text-center">
          <img src="/favicon.svg" alt="" className="mx-auto h-12 w-12" />
          <h1 className="mt-4 text-2xl font-semibold tracking-tight">
            {isLogin ? 'Sign in to Coffee Tracker' : 'Create your account'}
          </h1>
          <p className="mt-1 text-sm text-stone-600">Log every brew. Find your best cup.</p>
        </div>

        <form onSubmit={handleSubmit} className="space-y-4 rounded-xl bg-white p-6 shadow-sm ring-1 ring-stone-200" noValidate>
          {error && Object.keys(error.fieldErrors).length === 0 && <Alert>{error.message}</Alert>}
          <TextField
            label="Username"
            name="username"
            autoComplete="username"
            required
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            error={error?.fieldErrors.username}
          />
          <TextField
            label="Password"
            name="password"
            type="password"
            autoComplete={isLogin ? 'current-password' : 'new-password'}
            required
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            error={error?.fieldErrors.password}
            hint={isLogin ? undefined : 'At least 8 characters.'}
          />
          <Button type="submit" className="w-full" disabled={submit.isPending}>
            {submit.isPending ? 'Please wait…' : isLogin ? 'Sign in' : 'Create account'}
          </Button>
        </form>

        <p className="mt-6 text-center text-sm text-stone-600">
          {isLogin ? 'New here? ' : 'Already have an account? '}
          <Link to={isLogin ? '/register' : '/login'} state={location.state} className="font-medium text-brand-700 hover:underline">
            {isLogin ? 'Create an account' : 'Sign in'}
          </Link>
        </p>
      </div>
    </div>
  )
}
